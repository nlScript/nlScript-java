package de.nls.ui;

import de.nls.Autocompleter;
import de.nls.ParseException;
import de.nls.ParsedNode;
import de.nls.Parser;
import de.nls.core.Autocompletion;

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

	private ActionListener onRun = e -> run();

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

		JPanel buttons = new JPanel(new FlowLayout());
		runButton = new JButton("Run");
		runButton.addActionListener(onRun);
		buttons.add(runButton);
		frame.getContentPane().add(buttons, BorderLayout.SOUTH);

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

	public void setOnRun(ActionListener l) {
		runButton.removeActionListener(onRun);
		onRun = l;
		runButton.addActionListener(onRun);
	}

	public void run() {
		outputArea.setText("");
		try {
			ParsedNode pn = parser.parse(getText(), null);
			pn.evaluate();
		} catch(ParseException e) {
			outputArea.setText(e.getMessage());
		}
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
