package de.nls.ui;

import de.nls.ParseException;
import de.nls.core.DefaultParsedNode;
import de.nls.core.Matcher;

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
import java.util.concurrent.atomic.AtomicBoolean;

public class AutocompletionContext implements ParameterizedCompletionContext.ParameterChangeListener {

	private final JTextComponent tc;
	private final ACProvider provider;
	private final ACPopup popup;

	private final ErrorHighlight errorHighlight;

	private final boolean insertAsFarAsPossible = true;

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
		this.tc.addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {
			}

			@Override public void keyPressed(KeyEvent e) {
				if(e.isConsumed()) {
					System.out.println("AutocompletionContext.keyPressed: consumed=true");
					return;
				}
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
						System.out.println("Enter");
						final IAutocompletion completion = popup.getSelected();
						if(completion != null) {
							hidePopup(); // need to hide it before changing the document
							insertCompletion(tc.getCaretPosition(), completion);
//							// if we made a choice in the popup menu, insert it and cycle to next parameter
//							if(parameterizedCompletion != null)
//								parameterizedCompletion.next();
						}
						e.consume();
					}
					if (kc == KeyEvent.VK_ESCAPE) {
						e.consume();
						hidePopup();
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
				System.out.println("insertUpdate: " + tc.getText());
				int caret = e.getOffset() + e.getLength();
				justInserted.set(true);
				if(!insertedByAutocompletion || insertAsFarAsPossible) {
					if(insertedParameterizedAutocompletion) {
						// Don't autocomplete if the insert was a parameterized completion. This is important if
						// e.g. '${name}' was inserted. If we autocompleted here, we'd autocomplete after 'name' and
						// ignore the parameterization.
						// If it was indeed a parameterized completion, autocomplete will be called in parameterChanged().
						System.out.println("Don't autocomplete because it a parameterize autocompletion was inserted");
						return;
					}
					System.out.println("insertUpdate: doAutocompletion caret = " + caret + " (after " + tc.getText().charAt(caret - 1) + ")");
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

	private String lastInserted = null;
	private int lastInsertionPosition = -1;

	public void insertCompletion(int caret, IAutocompletion completion) {
		final String repl = completion.getCompletion();
		final String alreadyEntered = completion.getAlreadyEnteredText();
		/* If a parameterized completion was inserted before, with the first parameter starting right at
		 * the beginning of the insertion (e.g. "${percentage} %"), we would insert the very same completion again
		 * when trying to auto-complete the first parameter.
		 */
		if(caret == lastInsertionPosition && completion.getCompletion().equals(lastInserted)) {
			System.out.println("Skip insertion");
			return;
		}

		lastInserted = repl;
		lastInsertionPosition = caret;
		System.out.println("insertCompletion: caret = " + caret + ", compl = " + repl);

		tc.moveCaretPosition(caret - alreadyEntered.length());

		insertedByAutocompletion = true;
		if(repl.contains("${")) {
			cancelParameterizedCompletion();
			insertedParameterizedAutocompletion = true;
			parameterizedCompletion = new ParameterizedCompletionContext(tc);
			parameterizedCompletion.addParameterChangeListener(this);
			parameterizedCompletion.replaceSelection(repl);
			insertedParameterizedAutocompletion = false;
		}
		else {
			tc.replaceSelection(repl);
		}
		System.out.println("insert completion caret is now " + tc.getCaretPosition());
		insertedByAutocompletion = false;
	}

	@Override
	public void parameterChanged(ParameterizedCompletionContext source, int pIdx, boolean wasLast) {
		System.out.println("parameterChanged: " + pIdx);
		if(source != parameterizedCompletion) {
			// This should not happen, because we cancel the current parameterization before making a new one.
			System.out.println("skipping");
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
		System.out.println(parameterizedCompletion.getParametersSize());
		// if (parameterizedCompletion.getParametersSize() > 2) // 1 parameter + the end cursor parameter
			doAutocompletion(tc.getCaretPosition(), false);
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
		if(caret < entireText.trim().length())
			autoInsertSingleOption = false;
		String text = entireText.substring(0, caret);

		errorHighlight.clearError();

		IAutocompletion[] completions;
		try {
			completions = provider.getAutocompletions(text);
		} catch (ParseException e) {
			Matcher f = e.getFirstAutocompletingAncestorThatFailed().getMatcher();
			errorHighlight.setError(f.pos, f.pos + f.parsed.length());
			return;
		}
		popup.getModel().set(completions);
		if (completions.length < 2) {
			if (popup.getModel().getSize() == 1 && autoInsertSingleOption) {
				final IAutocompletion completion = popup.getModel().getElementAt(0);
				SwingUtilities.invokeLater(() -> {
					System.out.println("Only one option left [" + completion.getCompletion() + "], insert it automatically.");
					insertCompletion(caret, completion);
				});
			}
			hidePopup();
			return;
		}

		String remainingText = entireText.substring(caret);
		int matchingLength = 0;
		for(IAutocompletion ac : completions) {
			String remainingCompletion = ac.getCompletion().substring(ac.getAlreadyEnteredText().length());
			if(remainingText.startsWith(remainingCompletion)) {
				matchingLength = remainingCompletion.length();
				break;
			}
		}
		if(matchingLength > 0)
			tc.select(caret, caret + matchingLength);

		popup.setSelectedIndex(0);
		showPopup(caret);
	}
}
