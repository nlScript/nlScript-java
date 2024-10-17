package nlScript.ui;

import nlScript.core.BNF;
import nlScript.core.Matcher;
import nlScript.core.NonTerminal;
import nlScript.core.Symbol;
import nlScript.ParseException;
import nlScript.core.Autocompletion;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutocompletionContext implements ParameterizedCompletionContext.ParameterChangeListener {

	private final JTextComponent tc;
	private final ACProvider provider;
	private final ACPopup popup;

	private final ErrorHighlight errorHighlight;

	private boolean insertAsFarAsPossible = true;

	private static class ErrorHighlight {
		private final HighlightPainter errorHighlight = new HighlightPainter.Squiggle(new Color(255, 100, 100));

		private final JTextComponent tc;

		private Object highlightTag;

		public ErrorHighlight(JTextComponent tc) {
			this.tc = tc;
		}

		void setError(int i0, int i1) {
			clearError();
			try {
				int start = i0 == 0 ? 0 : i0 - 1;
				highlightTag = tc.getHighlighter().addHighlight(start, i1, errorHighlight);
			} catch(BadLocationException ignored) {}
		}

		void clearError() {
			if(highlightTag != null) {
				tc.getHighlighter().removeHighlight(highlightTag);
				highlightTag = null;
			}
		}
	}

	public AutocompletionContext(JTextComponent tc, ACProvider acProvider) {
		this.tc = tc;
		this.provider = acProvider;
		final Window parent = SwingUtilities.getWindowAncestor(tc);
		this.popup = new ACPopup(parent);
		this.errorHighlight = new ErrorHighlight(tc);

		this.popup.addMouseListenerToList(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2) {
					final Autocompletion completion = popup.getSelected();
					if(completion != null) {
						hidePopup(); // need to hide it before changing the document
						insertCompletion(tc.getCaretPosition(), completion);
					}
					e.consume();
				}
			}
		});
		this.tc.addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {
			}

			@Override public void keyPressed(KeyEvent e) {
				if(e.isConsumed())
					return;

				if (popup.isVisible()) {
					int kc = e.getKeyCode();
					if (kc == KeyEvent.VK_DOWN || kc == KeyEvent.VK_RIGHT) {
						popup.next();
						e.consume();
					}
					if (kc == KeyEvent.VK_UP || kc == KeyEvent.VK_LEFT) {
						popup.previous();
						e.consume();
					}
					if (kc == KeyEvent.VK_ENTER) {
						final Autocompletion completion = popup.getSelected();
						if(completion != null) {
							hidePopup(); // need to hide it before changing the document
							insertCompletion(tc.getCaretPosition(), completion, true);
						}
						e.consume();
					}
					if (kc == KeyEvent.VK_ESCAPE) {
						e.consume();
						hidePopup();
					}
				}
				else if(parameterizedCompletion != null) {
					if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						cancelParameterizedCompletion();
					}
				}
				else {
					if(e.getKeyCode() == KeyEvent.VK_SPACE && e.isControlDown()) {
						doAutocompletion(tc.getCaretPosition(), true);
					}
				}
			}

			@Override public void keyReleased(KeyEvent e) {
			}
		});

		tc.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				int caret = e.getOffset() + e.getLength();
				justInserted.set(true);
				if(!insertedByAutocompletion || insertAsFarAsPossible) {
					if(insertedParameterizedAutocompletion) {
						// Don't autocomplete if the insert was a parameterized completion. This is important if
						// e.g. '${name}' was inserted. If we autocompleted here, we'd autocomplete after 'name' and
						// ignore the parameterization.
						// If it was indeed a parameterized completion, autocomplete will be called in parameterChanged().
						return;
					}
					doAutocompletion(caret, true);
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				hidePopup();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		});

		tc.addCaretListener(e -> {
			// if the caret change is caused by an insert, we don't want to hide the popup window,
			// but we'll still reset the flag.
			if(!justInserted.get()) {
				hidePopup();
			}
			else
				justInserted.set(false);
		});

		parent.addComponentListener(new ComponentListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				hidePopup();
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				hidePopup();
			}

			@Override
			public void componentShown(ComponentEvent e) {

			}

			@Override
			public void componentHidden(ComponentEvent e) {
				hidePopup();
			}
		});
	}

	private ParameterizedCompletionContext parameterizedCompletion = null;
	private boolean insertedByAutocompletion = false;
	private boolean insertedParameterizedAutocompletion = false;
	final AtomicBoolean justInserted = new AtomicBoolean(false);

	// private String lastInserted = null;
	private int lastInsertionPosition = -1;

	public void insertCompletion(int caret, Autocompletion completion) {
		insertCompletion(caret, completion, false);
	}

	public void insertCompletion(int caret, Autocompletion completion, boolean force) {
		final String repl = completion.getCompletion(Autocompletion.Purpose.FOR_INSERTION);
		final String alreadyEntered = completion.getAlreadyEntered();
		/* If a parameterized completion was inserted before, with the first parameter starting right at
		 * the beginning of the insertion (e.g. "${percentage} %"), we would insert the very same completion again
		 * when trying to auto-complete the first parameter.
		 */
		if(!force && caret == lastInsertionPosition) // TODO 2024-04-11 is this still needed: && repl.equals(lastInserted))
			return;


		// lastInserted = repl;
		lastInsertionPosition = caret;

		tc.moveCaretPosition(caret - alreadyEntered.length());

		insertedByAutocompletion = true;
		if(repl.contains("${")) {
			cancelParameterizedCompletion();
			insertedParameterizedAutocompletion = true;
			parameterizedCompletion = new ParameterizedCompletionContext(tc);
			parameterizedCompletion.addParameterChangeListener(this);
			parameterizedCompletion.replaceSelection(completion);
			insertedParameterizedAutocompletion = false;
		}
		else {
			tc.replaceSelection(repl);
		}
		insertedByAutocompletion = false;
	}

	@Override
	public void parameterChanged(ParameterizedCompletionContext source, int pIdx, boolean wasLast) {
		if(source != parameterizedCompletion) {
			// This should not happen, because we cancel the current parameterization before making a new one.
			cancelParameterizedCompletion();
			new Exception().printStackTrace();
			return;
		}
		justInserted.set(true);
		if (wasLast) {
			parameterizedCompletion = null;
			doAutocompletion(tc.getCaretPosition(), true);
			return;
		}
		if (pIdx == -1) {
			parameterizedCompletion = null;
			doAutocompletion(tc.getCaretPosition(), true);
			return;
		}
		Autocompletion autocompletion = parameterizedCompletion.getCurrentParameter().parameterizedCompletion;

		List<Autocompletion> completions = parameterizedCompletion.getParameter(pIdx).allOptions;
		popup.getModel().set(completions);
		if (completions.size() < 2)
			hidePopup();
		else {
			popup.setSelectedIndex(0);
			showPopup(tc.getCaretPosition());
		}
	}

	public void hidePopup() {
		if(popup.isVisible())
			popup.setVisible(false);
	}

	public void cancelParameterizedCompletion() {
		// do not remove (poll) here, that's done in the listener
		if(parameterizedCompletion != null)
			parameterizedCompletion.finish(false);
		parameterizedCompletion = null;
	}

	public void showPopup(int caret) {
		if (popup.isVisible())
			return;
		Rectangle r;
		try {
			r = tc.modelToView(caret);
		} catch (BadLocationException ble) {
			ble.printStackTrace();
			return;
		}
		Point p = new Point(r.x, r.y);
		SwingUtilities.convertPointToScreen(p, tc);
		r.x = p.x;
		r.y = p.y;
		popup.setLocationRelativeTo(r);
		popup.setVisible(true);
	}

	public void doAutocompletion(int caret, boolean autoInsertSingleOption) {
		String entireText = tc.getText();
//		if(caret < entireText.trim().length())
//			autoInsertSingleOption = false;
		String text = entireText.substring(0, caret);

		errorHighlight.clearError();

		List<Autocompletion> completions;
		try {
			completions = provider.getAutocompletions(text);
		} catch (ParseException e) {
			Matcher f = e.getFirstAutocompletingAncestorThatFailed().getMatcher();
			errorHighlight.setError(f.pos, f.pos + f.parsed.length());
			return;
		} catch(Exception e) {
			e.printStackTrace();
			completions = new ArrayList<>();
		}

		// we are in a parameterized completion context.
		// we still want to autocomplete, but not beyond the end of the current parameter
		BNF bnf = provider.getParser().getTargetGrammar().getBNF();
		if(parameterizedCompletion != null) {
			if(!completions.isEmpty()) {
				boolean atLeastOnCompletionForCurrentParameter = false;
				for (Autocompletion comp : completions) {
					Symbol symbol = comp.forSymbol;

					// if comp is an EntireSequence completion, we should just check the first
					// we can do that using ParameterizedCompletionContext.parseParameters
					if(comp instanceof Autocompletion.EntireSequence) {
						ArrayList<ParameterizedCompletionContext.ParsedParam> tmp = new ArrayList<>();
						ParameterizedCompletionContext.parseParameters((Autocompletion.EntireSequence) comp, tmp, 0);
						comp = tmp.get(0).parameterizedCompletion;
						symbol = comp.forSymbol;
					}

					if(symbol.equals(parameterizedCompletion.getForAutocompletion().forSymbol)) {
						atLeastOnCompletionForCurrentParameter = true;
						break;
					}

					// check if symbol is a descendent of the parameters autocompletion symbol
					Symbol parameterSymbol = parameterizedCompletion.getCurrentParameter().parameterizedCompletion.forSymbol;
					// symbol == parameterSymbol? -> fine
					if(symbol.equals(parameterSymbol)) {
						atLeastOnCompletionForCurrentParameter = true;
						break;
					}

					if(parameterSymbol instanceof NonTerminal) {
						// check recursively if symbol is in the list of child symbols
						if(((NonTerminal) parameterSymbol).uses(symbol, bnf)) {
							atLeastOnCompletionForCurrentParameter = true;
							break;
						}
					}
				}
				if(!atLeastOnCompletionForCurrentParameter) {
					SwingUtilities.invokeLater(() -> parameterizedCompletion.next());
					// parameterizedCompletion.next();
					return;
				}
			}
		}

		popup.getModel().set(completions);
		if (completions.size() < 2) {
			if (popup.getModel().getSize() == 1) {
				final Autocompletion completion = popup.getModel().getElementAt(0);
				if(autoInsertSingleOption || (completion instanceof Autocompletion.Literal)) {
					SwingUtilities.invokeLater(() -> {
						boolean tmp = insertAsFarAsPossible;
						if (caret < entireText.trim().length())
							insertAsFarAsPossible = false;
						insertCompletion(caret, completion);
						insertAsFarAsPossible = tmp;
					});
				}
			}
			hidePopup();
			return;
		}

//		if(false) {
//			String remainingText = entireText.substring(caret);
//			int matchingLength = 0;
//			for (Autocompletion ac : completions) {
//				String remainingCompletion = ac.getCompletion().substring(ac.getAlreadyEntered().length());
//				if (remainingText.startsWith(remainingCompletion)) {
//					matchingLength = remainingCompletion.length();
//					break;
//				}
//			}
//			if (matchingLength > 0)
//				tc.select(caret, caret + matchingLength);
//		}

		popup.setSelectedIndex(0);
		showPopup(caret);
	}
}
