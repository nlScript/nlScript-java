package de.nls.ui;

import de.nls.Autocompleter;
import de.nls.ParseException;
import de.nls.ParsedNode;
import de.nls.Parser;
import de.nls.core.Autocompletion;
import de.nls.core.GraphViz;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ACEditor {

	private final JFrame frame;
	// private final JTextArea textArea;
	private final JTextComponent textArea;
	private final JTextComponent outputArea;
	private final AutocompletionContext autocompletionContext;
	private final Parser parser;
	private final JButton runButton;
	private final JPanel buttonsPanel;

	private Thread runThread = null;

	private ActionListener onRun = e -> run();

	private Runnable beforeRun = () -> {};

	private Runnable afterRun = () -> {};

	public ACEditor(Parser parser) {
		this.parser = parser;
		textArea = new JTextArea(20, 60);
		textArea.setFont(new Font("monospaced", Font.BOLD, 12));

		JScrollPane textAreaScrollPane = new JScrollPane(textArea);

		TextLineNumber textLineNumber = new TextLineNumber(textArea);
		textAreaScrollPane.setRowHeaderView(textLineNumber);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.add(textAreaScrollPane);

		outputArea = new JTextArea(5, 60);
		outputArea.setFont(new Font("monospaced", Font.PLAIN, 12));

		JScrollPane outputAreaScrollPane = new JScrollPane(outputArea);
//		outputAreaScrollPane.setRowHeaderView(new JLabel("x"));
		splitPane.add(outputAreaScrollPane);

		frame = new JFrame();
		frame.getContentPane().add(splitPane);

		buttonsPanel = new JPanel(new FlowLayout());
		runButton = new JButton("Run");
		runButton.addActionListener(onRun);
		buttonsPanel.add(runButton);
		frame.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		frame.pack();
		autocompletionContext = new AutocompletionContext(textArea, new ACProvider(parser));

//		textArea.addMouseListener(new MouseAdapter() {
//			public void mouseClicked(MouseEvent e) {
//				// TODO this method will not work in case a pre-processor is active
//				if(e.getClickCount() == 2) {
//					int pos = textArea.viewToModel(e.getPoint());
//					String text = textArea.getText();
//					List<ParsedLeafNode> bestRoute = parser.getBestParseRoute(text);
//					ParsedLeafNode leaf = null;
//					for(ParsedLeafNode pln : bestRoute) {
//						ParseResult pr = pln.getParseResult();
//						int l = pr.getParsed().length();
//						if(pos >= pr.getPosition() - l && pos < pr.getPosition()) {
//							leaf = pln;
//							break;
//						}
//					}
//					ArrayList<ParsedNode> pathToRoot = new ArrayList<>();
//					leaf.getPathToRoot(pathToRoot);
//					for(ParsedNode p : pathToRoot) {
//						if(p.getNode().getGuiEditor() != null) {
//							textArea.setCaretPosition(p.getParseResult().getPosition());
//							textArea.moveCaretPosition(p.getParseResult().getPosition() - p.getParseResult().getParsed().length());
//							p.getNode().getGuiEditor().edit(textArea, p);
//						}
//					}
//					e.consume();
//				}
//			}
//		});
	}

	public JFrame getFrame() {
		return frame;
	}

	public void setMenuBar(JMenuBar menuBar) {
		frame.setJMenuBar(menuBar);
	}

	public JPanel getButtonsPanel() {
		return buttonsPanel;
	}

	public void setOnRun(ActionListener l) {
		runButton.removeActionListener(onRun);
		onRun = l;
		runButton.addActionListener(onRun);
	}

	public void setBeforeRun(Runnable r) {
		this.beforeRun = r;
	}

	public void setAfterRun(Runnable r) {
		this.afterRun = r;
	}

	public void run() {
		run(false);
	}

	public void run(boolean selectedLines) {
		outputArea.setText("");
		runThread = new Thread(new Runnable() {
			public void run() {
				try {
					beforeRun.run();
					String textToEvaluate = selectedLines ? getSelectedLines() : getText();
					ParsedNode pn = parser.parse(textToEvaluate, null);
					System.out.println(GraphViz.toVizDotLink(pn));
					pn.evaluate();
					afterRun.run();
				} catch(ParseException e) {
					outputArea.setText(e.getMessage());
				} catch(Throwable e) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					outputArea.setText(pw.toString());
					e.printStackTrace();
				}
			}
		});
		runThread.start();
	}

	public Thread getRunThread() {
		return runThread;
	}

	public void setVisible(boolean b) {
		frame.setVisible(b);
	}

	public JTextComponent getTextArea() {
		return textArea;
	}

	public JButton getRunButton() {
		return runButton;
	}

	public String getText() {
		return textArea.getText();
	}

	public int getSelectedLinesStart() {
		JTextArea jta = (JTextArea) textArea;
		int start = jta.getSelectionStart();
		try {
			start = jta.getLineStartOffset(jta.getLineOfOffset(start));
		} catch(BadLocationException ignored) {
		}
		return start;
	}

	public String getSelectedLines() {
		if(!(textArea instanceof JTextArea))
			return textArea.getSelectedText();
		JTextArea jta = (JTextArea) textArea;
		int start = jta.getSelectionStart();
		int end = jta.getSelectionEnd();
		try {
			start = jta.getLineStartOffset(jta.getLineOfOffset(start));
			end = jta.getLineEndOffset(jta.getLineOfOffset(end));
		} catch(BadLocationException ignored) {
		}
		try {
			return jta.getText(start, end - start);
		} catch(BadLocationException e) {
			return textArea.getSelectedText();
		}
	}
}
