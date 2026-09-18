package nlScript.ai;

import nlScript.ui.ACEditor;
import nlScript.util.Json;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class InstallAI {
	public static void installAI(
			final ACEditor editor,
			final String modelName,
			final String modelResourcePath,
			final BiFunction<String, String, String> createPrompt) {

		if(!checkOllamaOK(modelName, modelResourcePath))
			return;

		JComponent glassPane = (JComponent) editor.getFrame().getGlassPane();
		glassPane.setLayout(null);
		glassPane.setOpaque(false);

		JLabel hint = new JLabel("Ctrl-click for AI-assisted input");
		hint.setForeground(Color.GRAY);
		hint.setBackground(Color.WHITE);
		hint.setOpaque(true);
		hint.setFont(new Font("Arial", Font.BOLD, 12));
		glassPane.add(hint);
		glassPane.setVisible(true);


		final ActionListener hideListener = e -> glassPane.setVisible(false);
		final Timer timer = new Timer(500, hideListener);
		timer.setRepeats(false);
		editor.getTextArea().addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				timer.restart();
				Point p = SwingUtilities.convertPoint(
						e.getComponent(), e.getPoint(), glassPane
				);
				Dimension labelSize = hint.getPreferredSize();

				// Offset to keep label fully visible
				int x = p.x + 10;
				int y = p.y + 10;
				hint.setBounds(x, y, labelSize.width, labelSize.height);
				if(!glassPane.isVisible())
					glassPane.setVisible(true);
			}
		});

		editor.getTextArea().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(!e.isControlDown())
					return;
				Point p = e.getPoint();
				SwingUtilities.convertPointToScreen(p, editor.getTextArea());
				showAIAutocompletion(editor, p.x, p.y, modelName, modelResourcePath, createPrompt);
			}
		});
	}

	private static boolean checkOllamaOK(final String modelName, final String modelResourcePath) {
		Ollama ollama = new Ollama(modelName, modelResourcePath);
		try {
			if (ollama.isOllamaRunning(null, null, null) && ollama.isModelAvailable(modelName, null, null, null))
				return true;

			new ConfigureOllama(ollama).configure();

			return ollama.isOllamaRunning(null, null, null) && ollama.isModelAvailable(modelName, null, null, null);
		} catch(Exception e) {
			throw new RuntimeException("Error communicating with Ollama", e);
		}
	}

	private static void showAIAutocompletion(
			final ACEditor editor,
			final int x,
			final int y,
			final String modelName,
			final String modelResourcePath,
			final BiFunction<String, String, String> createPrompt) {
		JDialog dialog = new JDialog(editor.getFrame());
		JTextArea ta = new JTextArea(2, 30);
		Font taFont = UIManager.getFont("Label.font");
		if(taFont != null)
			ta.setFont(taFont.deriveFont((float) ta.getFont().getSize()));

		ta.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown()) {
					final AtomicInteger caret = new AtomicInteger(editor.getTextArea().getCaretPosition());
					String context = editor.getText().substring(0, caret.get());
					String sentence = ta.getText();
					dialog.dispose();
					new Thread(() -> {
						Ollama ollama = new Ollama(modelName, modelResourcePath);
						editor.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						try {
							boolean prev = editor.isAutocompletionEnabled();
							editor.setAutocompletionEnabled(false);
							String prompt = createPrompt.apply(context, sentence);
//							try {
								StringBuffer json = new StringBuffer();
								ollama.query(prompt, s -> {
									System.out.print(s);
									json.append(s);
									try {
										editor.getTextArea().getDocument().insertString(caret.getAndAdd(s.length()), s, null);
									} catch(BadLocationException ex) {
										throw new RuntimeException(ex);
									}
								});

//								String script = editor.getParser().fromJson(Json.parse(json.toString()));
//								editor.getTextArea().getDocument().insertString(caret.getAndAdd(script.length()), script, null);
//							} catch (BadLocationException ex) {
//								throw new RuntimeException(ex);
//							}
							editor.setAutocompletionEnabled(prev);
						} catch(Exception ex) {
							throw new RuntimeException("Error querying Ollama", ex);
						} finally {
							editor.getFrame().setCursor(Cursor.getDefaultCursor());
						}
					}).start();
				}
				else if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					dialog.dispose();
				}
			}
		});
		ta.setBorder(null);
		ta.setLineWrap(true);
		JScrollPane scroll = new JScrollPane(ta);
		ResizableUndecoratedFrame.makeUndecoratedResizable(dialog);
		dialog.setModal(true);
		dialog.getContentPane().add(scroll);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		buttons.setBackground(Color.GRAY);
		buttons.setBorder(ta.getBorder());
		JLabel label = new JLabel("Press Ctrl-Enter to confirm or Esc to cancel");
		label.setForeground(Color.WHITE);
		buttons.add(label);

		dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
		dialog.pack();
		dialog.setLocation(x, y);
		dialog.setVisible(true);
	}
}
