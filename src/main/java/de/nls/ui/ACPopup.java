package de.nls.ui;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

public class ACPopup extends JWindow {

	private final ACListModel model;

	private final JList<IAutocompletion> jList;

	public ACPopup(Window parent) {
		super(parent);
		jList = new JList<>();
		model = new ACListModel();
		jList.setModel(model);
		jList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				String text = ((IAutocompletion) value).getCompletion();
				List<ParameterizedCompletionContext.ParsedParam> parsedParams = new ArrayList<>();
				String insertionString = ParameterizedCompletionContext.parseParameters(text, parsedParams);
				if(parsedParams.size() > 0) {
					StringBuilder sb = new StringBuilder(insertionString);
					for (int i = parsedParams.size() - 1; i >= 0; i--) {
						ParameterizedCompletionContext.ParsedParam param = parsedParams.get(i);
						sb.insert(param.i1, "</b>");
						sb.insert(param.i0, "<b>");
					}
					sb.insert(0, "<html>");
					sb.append("</html>");
					text = sb.toString();
				}
				if(text.startsWith("\n"))
					text = "<new line>";
				if(text.equals(""))
					text = "<empty>";

				System.out.println("label text = " + text);
				JLabel label = (JLabel) super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
				Font font = label.getFont();
				font = font.deriveFont(Font.PLAIN);
				label.setFont(font);
				return label;
			}
		});
		JScrollPane scrollPane = new JScrollPane(jList);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		getContentPane().add(scrollPane);
		// setUndecorated(true);
		setLocation(200, 200);
		setSize(350, 250);
		setFocusableWindowState(false);

		jList.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int idx = jList.locationToIndex(e.getPoint());
				System.out.println("popup: clicked " + model.getElementAt(idx));
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}
		});
	}

	public IAutocompletion getSelected() {
		return jList.getSelectedValue();
	}

	public void setSelectedIndex(int idx) {
		jList.setSelectedIndex(idx);
	}

	public ACListModel getModel() {
		return model;
	}

	public void next() {
		int selectedIdx = (jList.getSelectedIndex() + 1) % model.getSize();
		jList.setSelectedIndex(selectedIdx);
	}

	public void previous() {
		int selectedIdx = (jList.getSelectedIndex() - 1 + model.getSize()) % model.getSize();
		jList.setSelectedIndex(selectedIdx);
	}


	/**
	 * The space between the caret and the completion popup.
	 */
	private static final int VERTICAL_SPACE			= 1;
	public void setLocationRelativeTo(Rectangle r) {

		// Multi-monitor support - make sure the completion window (and
		// description window, if applicable) both fit in the same window in
		// a multi-monitor environment.  To do this, we decide which monitor
		// the rectangle "r" is in, and use that one (just pick top-left corner
		// as the defining point).
		Rectangle screenBounds = getScreenBoundsForPoint(r.x, r.y);
		//Dimension screenSize = getToolkit().getScreenSize();

		int totalH = getHeight();

		// Try putting our stuff "below" the caret first.  We assume that the
		// entire height of our stuff fits on the screen one way or the other.
		int y = r.y + r.height + VERTICAL_SPACE;
		if (y+totalH>screenBounds.height) {
			y = r.y - VERTICAL_SPACE - getHeight();
		}

		// Get x-coordinate of completions.  Try to align left edge with the
		// caret first.
		int x = r.x;
		if (x<screenBounds.x) {
			x = screenBounds.x;
		}
		else if (x+getWidth()>screenBounds.x+screenBounds.width) { // completions don't fit
			x = screenBounds.x + screenBounds.width - getWidth();
		}

		setLocation(x, y);
	}


	/**
	 * Returns the screen coordinates for the monitor that contains the
	 * specified point.  This is useful for setups with multiple monitors,
	 * to ensure that popup windows are positioned properly.
	 *
	 * @param x The x-coordinate, in screen coordinates.
	 * @param y The y-coordinate, in screen coordinates.
	 * @return The bounds of the monitor that contains the specified point.
	 */
	public static Rectangle getScreenBoundsForPoint(int x, int y) {
		GraphicsEnvironment env = GraphicsEnvironment.
				getLocalGraphicsEnvironment();
		GraphicsDevice[] devices = env.getScreenDevices();
		for (GraphicsDevice device : devices) {
			GraphicsConfiguration config = device.getDefaultConfiguration();
			Rectangle gcBounds = config.getBounds();
			if (gcBounds.contains(x, y)) {
				return gcBounds;
			}
		}
		// If point is outside all monitors, default to default monitor (?)
		return env.getMaximumWindowBounds();
	}
}
