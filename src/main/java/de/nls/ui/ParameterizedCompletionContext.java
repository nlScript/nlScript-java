package de.nls.ui;

import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

public class ParameterizedCompletionContext implements KeyListener {

	private final JTextComponent tc;

	public interface ParameterChangeListener {
		void parameterChanged(ParameterizedCompletionContext source, int i, boolean wasLast);
	}

	private final ArrayList<ParameterChangeListener> parameterChangeListeners = new ArrayList<>();

	public void addParameterChangeListener(ParameterChangeListener l) {
		parameterChangeListeners.add(l);
	}

	public void removeParameterChangeListener(ParameterChangeListener l) {
		parameterChangeListeners.remove(l);
	}

	private void fireParameterChanged(int i, boolean wasLast) {
		List<ParameterChangeListener> copy = new ArrayList<>(parameterChangeListeners);
		for(ParameterChangeListener l : copy)
			l.parameterChanged(this, i, wasLast);
	}

	public ParameterizedCompletionContext(JTextComponent tc) {
		this.tc = tc;
	}

	public final HighlightPainter highlightPainter = new HighlightPainter.Outline(Color.GRAY);
	public final HighlightPainter cursorHighlightPainter = new HighlightPainter.Outline(new Color(0x00b400));

	private final ArrayList<Param> parameters = new ArrayList<>();

	private Param addHighlight(String name, int i0, int i1) {
		return addHighlight(name, i0, i1, highlightPainter);
	}

	private Param addHighlight(String name, int i0, int i1, Highlighter.HighlightPainter highlightPainter) {
		try {
			int start = i0 == 0 ? 0 : i0 - 1;
			Object tag = tc.getHighlighter().addHighlight(start, i1, highlightPainter);
			Highlighter.Highlight hl = findHighlight(start, i1);
			return new Param(name, hl, tag);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private Highlighter.Highlight findHighlight(int offs0, int offs1) {
		Highlighter.Highlight[] highlights = tc.getHighlighter().getHighlights();
		for(Highlighter.Highlight hl : highlights) {
			if(hl.getStartOffset() == offs0 && hl.getEndOffset() == offs1) {
				return hl;
			}
		}
		return null;
	}

	private int getParamIndexForCursorPosition(int pos) {
		for(int i = 0; i < parameters.size(); i++) {
			Param p = parameters.get(i);
			Highlighter.Highlight highlight = p.highlight;
			if(pos >= highlight.getStartOffset() + 1 && pos <= highlight.getEndOffset())
				return i;
		}
		return -1;
	}

	private int getNextParamIndexForCursorPosition(int pos) {
		for(int i = 0; i < parameters.size(); i++) {
			Param p = parameters.get(i);
			Highlighter.Highlight highlight = p.highlight;
			if(pos < highlight.getStartOffset() + 1)
				return i;
		}
		return -1;
	}

	private int getPreviousParamIndexForCursorPosition(int pos) {
		for(int i = parameters.size() - 1; i >= 0; i--) {
			Param p = parameters.get(i);
			Highlighter.Highlight highlight = p.highlight;
			if(pos > highlight.getEndOffset())
				return i;
		}
		return -1;
	}

	public void insertCompletion(int offset, String autocompletion) throws BadLocationException {
		ArrayList<ParsedParam> parsedParams = new ArrayList<>();
		String insertionString = parseParameters(autocompletion, parsedParams);
		tc.getDocument().insertString(offset, insertionString, null);
		parameters.clear();
		for(ParsedParam pp : parsedParams)
			parameters.add(addHighlight(pp.name, offset + pp.i0, offset + pp.i1));
		int cursor = offset + insertionString.length();
		parameters.add(addHighlight("", cursor, cursor, cursorHighlightPainter));
		cycle(0);
		tc.addKeyListener(this);
	}

	public void replaceSelection(String autocompletion) {
		int offset = tc.getSelectionStart();
		ArrayList<ParsedParam> parsedParams = new ArrayList<>();
		String insertionString = parseParameters(autocompletion, parsedParams);
		tc.replaceSelection(insertionString);
		parameters.clear();
		for(ParsedParam pp : parsedParams)
			parameters.add(addHighlight(pp.name, offset + pp.i0, offset + pp.i1));
		int cursor = offset + insertionString.length();
		parameters.add(addHighlight("", cursor, cursor, cursorHighlightPainter));
		cycle(0);
		tc.addKeyListener(this);
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if(e.isConsumed())
			return;

		int kc = e.getKeyCode();
		if (kc == KeyEvent.VK_TAB) {
			if(e.isShiftDown())
				previous();
			else
				next();
			e.consume();
		}
		else if(kc == KeyEvent.VK_ENTER) {
			next();
			e.consume();
		}
		else if(kc == KeyEvent.VK_ESCAPE) {
			cancel();
			e.consume();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	public int getParametersSize() {
		return parameters.size();
	}

	/**
	 * Assumes <code>currentParameterIndex</code> is already set.
	 */
	public void cycle(int currentParameterIndex) {
		int nParameters = parameters.size();
		if(nParameters == 0)
			return;

		if(currentParameterIndex == -1) {
			for(Param param : parameters)
				tc.getHighlighter().removeHighlight(param.highlightTag);
			tc.removeKeyListener(this);
			fireParameterChanged(-1, false);
			parameterChangeListeners.clear();
			return;
		}

		Highlighter.Highlight hl = parameters.get(currentParameterIndex).highlight;
		boolean last = currentParameterIndex == nParameters - 1;
		if(!last) {
			tc.setCaretPosition(hl.getEndOffset());
			tc.moveCaretPosition(hl.getStartOffset() + 1);
			fireParameterChanged(currentParameterIndex, false);
		}
		else {
			finish(true);
		}
	}

	public void next() {
		int caret = tc.getCaretPosition();
		int idx = getNextParamIndexForCursorPosition(caret);
		cycle(idx);
	}

	public void previous() {
		int caret = tc.getCaretPosition();
		int idx = getPreviousParamIndexForCursorPosition(caret);
		cycle(idx);
	}

	public void finish(boolean moveCaret) {
		if(moveCaret) {
			Highlighter.Highlight hl = parameters.get(parameters.size() - 1).highlight;
			tc.setCaretPosition(hl.getEndOffset());
		}
		for(Param param : parameters)
			tc.getHighlighter().removeHighlight(param.highlightTag);
		tc.removeKeyListener(this);
		fireParameterChanged(parameters.size() - 1, true);
		parameterChangeListeners.clear();
	}

	public void cancel() {
		for(Param param : parameters)
			tc.getHighlighter().removeHighlight(param.highlightTag);
		tc.removeKeyListener(this);
		parameterChangeListeners.clear();
	}

	public static String parseParameters(String paramString, List<ParsedParam> ret) {
		StringBuilder varName = null;
		StringBuilder insertString = new StringBuilder();
		final int l = paramString.length();
		int hlStart = -1;
		for(int i = 0; i < l; i++) {
			char cha = paramString.charAt(i);
			if(cha == '$' && i < l - 1 && paramString.charAt(i + 1) == '{') {
				if(varName == null) {
					varName = new StringBuilder();
					hlStart = insertString.length();
					i++;
				}
				else
					throw new RuntimeException("Expected '}' before next '${'");
			}
			else if(varName != null && cha == '}') {
				int hlEnd = insertString.length();
				ret.add(new ParsedParam(varName.toString(), hlStart, hlEnd));
				varName = null;
			}
			else if(varName != null) {
				varName.append(cha);
				insertString.append(cha);
			}
			else {
				insertString.append(cha);
			}
		}
		return insertString.toString();
	}

	public static class ParsedParam {
		public final String name;
		public final int i0;
		public final int i1;

		public ParsedParam(String name, int i0, int i1) {
			this.name = name;
			this.i0 = i0;
			this.i1 = i1;
		}
	}

	private static class Param {
		private final String name;
		private final Highlighter.Highlight highlight;
		private final Object highlightTag;

		public Param(String name, Highlighter.Highlight highlight, Object highlightTag) {
			this.name = name;
			this.highlight = highlight;
			this.highlightTag = highlightTag;
		}

		public String toString() {
			return name + ": [" + highlight.getStartOffset() + ", " + highlight.getEndOffset() + "[";
		}
	}
}
