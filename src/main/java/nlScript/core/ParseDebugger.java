package nlScript.core;

import nlScript.Evaluator;
import nlScript.ParseException;
import nlScript.Parser;
import nlScript.ebnf.EBNF;
import nlScript.ebnf.Rule;
import nlScript.util.Range;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.HashMap;

import static nlScript.core.RDParser.SymbolSequence;

public class ParseDebugger implements IParseDebugger {

	private DynamicJTree tree;

	private String text;

	private JFrame frame = null;
	private JSplitPane split = null;
	private JEditorPane parsedTextArea;
	private JLabel productionLabel;

	private JButton waitButton;

	private final HashMap<SymbolSequence, DefaultMutableTreeNode> sequence2node = new HashMap<>();

	@Override
	public void reset(SymbolSequence start, String text) {
		this.text = text;
		sequence2node.clear();
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(start);
		sequence2node.put(start, root);
		tree = new DynamicJTree(root);
		show();
	}

	@Override
	public void nextTerminal(SymbolSequence current, Matcher matcher, SymbolSequence tip) {
		if(tip != null)
			tree.nextNode = sequence2node.get(tip);
		DefaultMutableTreeNode node = sequence2node.get(current);
		tree.treeModel.reload(node);
		SwingUtilities.invokeLater(() -> {
			tree.tree.scrollPathToVisible(new TreePath(node.getPath()));
			showDetailsFor(node);
		});
		pause();
	}

	@Override
	public void nextNonTerminal(SymbolSequence current, SymbolSequence next, SymbolSequence tip) {
		DefaultMutableTreeNode parent = sequence2node.get(current);
		if(parent == null)
			throw new RuntimeException("Cannot find node for " + current);

		DefaultMutableTreeNode tmp = sequence2node.get(next);
		if(tmp != null)
			throw new RuntimeException("Node for " + tmp + " already exists");

		final DefaultMutableTreeNode child = new DefaultMutableTreeNode(next);
		sequence2node.put(next, child);
		tree.add(parent, child);
		if(tip != null)
			tree.nextNode = sequence2node.get(tip);

		SwingUtilities.invokeLater(() -> {
			tree.tree.scrollPathToVisible(new TreePath(child.getPath()));
			showDetailsFor(child);
		});


		pause();

	}

	private void pause() {
		waitButton.setEnabled(true);
		final Object lock = new Object();
		waitButton.addActionListener(l -> {
			synchronized (lock) {
				lock.notify();
			}
		});

		synchronized (lock) {
			try {
				lock.wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void show() {
		frame = new JFrame("Parse Debugger");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setSize(400, 300);
		frame.setLayout(new BorderLayout());

		JPanel treePanel = new JPanel(new BorderLayout());
		JPanel detailsPanel = new JPanel();

		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treePanel, detailsPanel);
		frame.add(split, BorderLayout.CENTER);


		JScrollPane treeScrollPane = new JScrollPane(tree.tree);
		treePanel.add(treeScrollPane, BorderLayout.CENTER);

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5, 5, 5, 5);
		detailsPanel.setLayout(gridbag);

		c.gridx = 0; c.gridy = 0; c.gridwidth = GridBagConstraints.REMAINDER; c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0; c.weighty = 0;
		productionLabel = new JLabel();
		detailsPanel.add(productionLabel, c);

		c.gridx = 0; c.gridy = 1; c.gridwidth = GridBagConstraints.REMAINDER; c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0; c.weighty = 1.0;
		parsedTextArea = new JEditorPane("text/html", this.text);
		parsedTextArea.setEditable(false);
		detailsPanel.add(new JScrollPane(parsedTextArea), c);


		// buttons
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new FlowLayout());
		waitButton = new JButton("Continue");
		waitButton.setEnabled(false);
		inputPanel.add(waitButton);
		frame.add(inputPanel, BorderLayout.SOUTH);

		// Show the frame
		frame.setVisible(true);

		tree.tree.addTreeSelectionListener(e -> {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPaths()[0].getLastPathComponent();
			if(node == null)
				return;
			showDetailsFor(node);
		});
	}

	public void showDetailsFor(DefaultMutableTreeNode node) {
		SymbolSequence seq = (SymbolSequence) node.getUserObject();
		productionLabel.setText("Production: " + seq.getProduction().toString());
		Matcher matcher = seq.getLastMatcher();
		int parsedUntil = seq.getParsedUntil();
		parsedUntil = Math.min(parsedUntil, this.text.length());
		String text =
				"<html>" +
				"<u><b color=\"blue\">" + escapeHTML(this.text.substring(0, parsedUntil)) + "</b></u>" +
				escapeHTML(this.text.substring(parsedUntil)) +
				"</html>";
		parsedTextArea.setText(text);
	}

	public static void main(String[] args) throws ParseException {
		Parser parser = new Parser();
//		 parser.defineType("t", "{t:[a-z]:+}", e -> e.getParsedString());
//		 parser.defineSentence("T {t:t}z.", e -> null);

		 parser.defineSentence("T {x:digit:+} z.", e -> null);


		 String text = "T abc z.";

//		parser.defineSentence("H {bla:int}.", e -> null);
//		String text = "Ha.";
		parser.parse(text, null);
	}

	public static void main2(String[] args) throws ParseException {
		EBNF grammar = new EBNF();

		Rule IDENTIFIER = grammar.sequence("identifier",
				Terminal.characterClass("[A-Za-z_]").withName(),
				grammar.optional(null,
						grammar.sequence(null,
								grammar.star(null,
										Terminal.characterClass("[A-Za-z0-9_-]").withName()
								).withName("star"),
								Terminal.characterClass("[A-Za-z0-9_]").withName()
						).withName("seq")
				).withName("opt")
		);

		Rule TYPE = grammar.or("type",
				grammar.sequence(null,
						IDENTIFIER.withName("identifier")
				).withName()
		);

		Rule VARIABLE_NAME = grammar.plus("var-name",
				Terminal.characterClass("[^:{}]").withName()).setEvaluator(Evaluator.DEFAULT_EVALUATOR);

		Rule QUANTIFIER = grammar.or("quantifier",
				grammar.sequence(null, Terminal.literal("?").withName()).       setEvaluator(pn -> Range.OPTIONAL).withName("optional"),
				grammar.sequence(null, Terminal.literal("+").withName()).       setEvaluator(pn -> Range.PLUS).withName("plus"),
				grammar.sequence(null, Terminal.literal("*").withName()).       setEvaluator(pn -> Range.STAR).withName("star"),
				grammar.sequence(null, grammar.INTEGER_RANGE.withName("range")).setEvaluator(pn -> pn.evaluate(0)).withName("range"),
				grammar.sequence(null,       grammar.INTEGER.withName("int")).  setEvaluator(pn -> new Range((int)pn.evaluate(0))).withName("fixed")
		);

		Rule r = grammar.sequence("variable",
				Terminal.literal("{").withName(),
				VARIABLE_NAME.withName("variable-name"),
				grammar.optional(null,
						grammar.sequence(null,
								Terminal.literal(":").withName(),
								TYPE.withName("type")
						).withName("seq-type")
				).withName("opt-type"),
				grammar.optional(null,
						grammar.sequence(null,
								Terminal.literal(":").withName(),
								QUANTIFIER.withName("quantifier")
						).withName("seq-quantifier")
				).withName("opt-quantifier"),
				Terminal.literal("}").withName()
		);

		grammar.compile(r.getTarget());

		BNF bnf = grammar.getBNF();

		RDParser parser = new RDParser(bnf, new Lexer("{H:int:+}"), ParsedNodeFactory.DEFAULT);
		// parser.setParseDebugger(new ParseDebugger());
		DefaultParsedNode parsed = parser.parse();
		if(ParsingState.SUCCESSFUL != parsed.getMatcher().state)
			throw new RuntimeException();
	}

	private static class RoundedBorder implements Border {
		private final int radius;
		private Color color = null;
		private int stroke = 1;

		RoundedBorder(int radius) {
			this.radius = radius;
		}

		public void setColor(Color color) {
			this.color = color;
		}

		public void setStroke(int stroke) {
			this.stroke = stroke;
		}

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Color prev = g.getColor();
			if(color != null)
				g.setColor(color);
			((Graphics2D) g).setStroke(new BasicStroke(stroke));
			g.drawRoundRect(x, y, width-1, height-1, radius, radius);
			g.setColor(prev);
		}

		@Override
		public Insets getBorderInsets(Component c) {
			return new Insets(radius, radius, radius, radius);
		}

		@Override
		public boolean isBorderOpaque() {
			return true;
		}
	}

	private static class DynamicJTree {
		private final JTree tree;
		private final DefaultTreeModel treeModel;

		private DefaultMutableTreeNode nextNode = null;

		public DynamicJTree(DefaultMutableTreeNode root) {
			treeModel = new DefaultTreeModel(root);
			tree = new JTree(treeModel);
			DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
				@Override
				public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
					SymbolSequence seq = (SymbolSequence) node.getUserObject();
					JPanel panel = new JPanel();
					if(selected) {
						RoundedBorder b = new RoundedBorder(0);
						b.setStroke(1);
						b.setColor(Color.DARK_GRAY);
						panel.setBorder(b);
					}
					FlowLayout layout = new FlowLayout();
					layout.setHgap(5);
					panel.setLayout(layout);
					panel.setBackground(Color.white);
					int newSymbolOffset = 0;
					int newSymbolLength = 0;
					if(seq.getParent() != null) {
						newSymbolOffset = seq.getParent().getPos();
						newSymbolLength = seq.getProduction().getRight().length;
					}

					if(node == nextNode) {
						JLabel x = new JLabel(" -> ");
						x.setForeground(Color.WHITE);
						x.setBackground(Color.LIGHT_GRAY);
						x.setOpaque(true);
						panel.add(x);
					}


					Matcher matcher = seq.getLastMatcher();
					if(matcher != null && matcher.state == ParsingState.FAILED) {
						JLabel x = new JLabel("X  Failed");
						x.setForeground(Color.RED);
						panel.add(x);
						panel.setBackground(new Color(255, 210, 210));
					}
					if(matcher != null && matcher.state == ParsingState.END_OF_INPUT) {
						JLabel x = new JLabel("X  EOI");
						x.setForeground(new Color(255, 128, 0));
						panel.add(x);
						panel.setBackground(new Color(255, 230, 200));
					}

					for(int i = 0; i < seq.size(); i++) {
						Symbol sym = seq.getSymbol(i);
						JLabel label = new JLabel(sym.toString());
						Color bg = Color.white;
						if(i >= newSymbolOffset && i < newSymbolOffset + newSymbolLength)
							bg = new Color(180, 255, 180);
						label.setBackground(bg);
						RoundedBorder border = new RoundedBorder(3);
						if(i == seq.getPos()) {
							border.setStroke(4);
							border.setColor(new Color(150, 100, 255));
						}
						label.setBorder(border);
						label.setOpaque(true);
						panel.add(label);
					}
					return panel;
				}
			};
			tree.setCellRenderer(renderer);
		}

		public void add(DefaultMutableTreeNode parent, DefaultMutableTreeNode child) {
			// parent.add(child);
			parent.insert(child, 0);
			treeModel.reload(parent);
		}
	}

	private static String escapeHTML(String html) {
		return html
				.replace("&", "&amp;")
				.replace("\\", "&#39;")
				.replace("\"", "&quot;")
				// Note: "&apos;" is not defined in HTML 4.01.
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}
}
