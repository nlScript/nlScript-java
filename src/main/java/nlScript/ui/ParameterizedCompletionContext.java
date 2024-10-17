package nlScript.ui;

import nlScript.core.Autocompletion;
import nlScript.ebnf.Rule;

import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

	private Param addHighlight(String name, Autocompletion.Parameterized autocompletion, List<Autocompletion> allOptions, int i0, int i1) {
		return addHighlight(name, autocompletion, allOptions, i0, i1, highlightPainter);
	}

	private Param addHighlight(String name, Autocompletion.Parameterized autocompletion, List<Autocompletion> allOptions, int i0, int i1, Highlighter.HighlightPainter highlightPainter) {
		try {
			int start = i0 == 0 ? 0 : i0 - 1;
			Object tag = tc.getHighlighter().addHighlight(start, i1, highlightPainter);
			Highlighter.Highlight hl = findHighlight(start, i1);
			return new Param(name, autocompletion, allOptions, hl, tag);
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

	public int getCurrentParamIndex() {
		return getParamIndexForCursorPosition(tc.getCaretPosition());
	}

	public Param getParameter(int idx) {
		return parameters.get(idx);
	}

	public Param getCurrentParameter() {
		return parameters.get(getCurrentParamIndex());
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

	private Autocompletion forAutocompletion = null;

	public Autocompletion getForAutocompletion() {
		return forAutocompletion;
	}

	public void replaceSelection(Autocompletion autocompletion) {
		this.forAutocompletion = autocompletion;
		int offset = tc.getSelectionStart();
		ArrayList<ParsedParam> parsedParams = new ArrayList<>();
		String insertionString = parseParameters(autocompletion, parsedParams);
		tc.replaceSelection(insertionString);
		parameters.clear();
		for(ParsedParam pp : parsedParams)
			parameters.add(addHighlight(pp.name, pp.parameterizedCompletion, pp.allOptions, offset + pp.i0, offset + pp.i1));
		int cursor = offset + insertionString.length();
		parameters.add(addHighlight("", null, null, cursor, cursor, cursorHighlightPainter));
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

	public static String parseParameters(Autocompletion autocompletion, List<ParsedParam> ret) {
		if(autocompletion instanceof Autocompletion.Literal)
			return parseParameters((Autocompletion.Literal) autocompletion, ret);
		if(autocompletion instanceof Autocompletion.Parameterized)
			return parseParameters((Autocompletion.Parameterized) autocompletion, ret);
		if(autocompletion instanceof Autocompletion.EntireSequence)
			return parseParameters((Autocompletion.EntireSequence) autocompletion, ret, 0);
		throw new RuntimeException("Unexpected autocompletion type: " + autocompletion.getClass());
	}

	public static String parseParameters(Autocompletion.Literal autocompletion, List<ParsedParam> ret) {
		return autocompletion.getCompletion(Autocompletion.Purpose.FOR_INSERTION);
	}

	public static String parseParameters(Autocompletion.Parameterized autocompletion, List<ParsedParam> ret) {
		String s = autocompletion.paramName;
		List<Autocompletion> allOptions = Collections.singletonList(autocompletion);
		ret.add(new ParsedParam(s, 0, s.length(), autocompletion, allOptions));
		return s;
	}

	public static String parseParameters(Autocompletion.EntireSequence autocompletion, List<ParsedParam> ret, int offset) {
		List<List<Autocompletion>> sequenceOfCompletions = autocompletion.getSequenceOfCompletions();
		Rule sequence = autocompletion.getSequence();
		StringBuilder insertionString = new StringBuilder();
		int i = 0;
		for(List<Autocompletion> autocompletions : sequenceOfCompletions) {
			int n = autocompletions.size();
			if(n > 1) {
				String name = sequence.getNameForChild(i);
				Autocompletion.Parameterized p = new Autocompletion.Parameterized(sequence.getChildren()[i], name, name);
				int i0 = offset + insertionString.length();
				int i1 = i0 + name.length();
				ret.add(new ParsedParam(name, i0, i1, p, autocompletions));
				insertionString.append(name);
			}
			else if(n == 1) {
				Autocompletion single = autocompletions.get(0);
				if(single instanceof Autocompletion.Literal) {
					insertionString.append(single.getCompletion(Autocompletion.Purpose.FOR_INSERTION));
				}
				else if(single instanceof Autocompletion.Parameterized) {
					Autocompletion.Parameterized parameterized = (Autocompletion.Parameterized) single;
					String s = parameterized.paramName;
					int i0 = offset + insertionString.length();
					int i1 = i0 + s.length();
					ret.add(new ParsedParam(s, i0, i1, parameterized, Collections.singletonList(parameterized)));
					insertionString.append(s);
				}
				else if(single instanceof Autocompletion.EntireSequence) {
					Autocompletion.EntireSequence entire = (Autocompletion.EntireSequence) single;
					int offs = offset + insertionString.length();
					String s = parseParameters(entire, ret, offs);
					insertionString.append(s);
				}
				else {
					System.err.println("Unknown/unexpected autocompletion");
				}
			}
			i++;
		}
		return insertionString.toString();
	}

	public static class ParsedParam {
		public final String name;
		public final int i0;
		public final int i1;

		public final Autocompletion.Parameterized parameterizedCompletion;

		public final List<Autocompletion> allOptions;

		public ParsedParam(String name, int i0, int i1, Autocompletion.Parameterized parameterizedCompletion, List<Autocompletion> allOptions) {
			this.name = name;
			this.i0 = i0;
			this.i1 = i1;
			this.parameterizedCompletion = parameterizedCompletion;
			this.allOptions = allOptions;
		}
	}

	public static class Param {
		private final String name;

		public final Autocompletion.Parameterized parameterizedCompletion;

		public final List<Autocompletion> allOptions;
		private final Highlighter.Highlight highlight;
		private final Object highlightTag;

		public Param(String name, Autocompletion.Parameterized parameterizedCompletion, List<Autocompletion> allOptions, Highlighter.Highlight highlight, Object highlightTag) {
			this.name = name;
			this.parameterizedCompletion = parameterizedCompletion;
			this.allOptions = allOptions;
			this.highlight = highlight;
			this.highlightTag = highlightTag;
		}

		public String toString() {
			return name + ": [" + highlight.getStartOffset() + ", " + highlight.getEndOffset() + "[";
		}
	}
}
