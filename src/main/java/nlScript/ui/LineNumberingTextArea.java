package nlScript.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.Element;
import java.awt.*;

public class LineNumberingTextArea extends JTextArea
{
	private JTextArea textArea;

	public LineNumberingTextArea(JTextArea textArea) {
		this.textArea = textArea;
		this.setBorder(new CompoundBorder(new MatteBorder(0, 0, 0, 1, new Color(221, 221, 221)), new EmptyBorder(new Insets(0, 5, 0, 5))));
//		this.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		this.setFont(textArea.getFont());
		setBackground(new Color(245, 245, 245));
		setForeground(new Color(108, 108, 108));
		setEditable(false);
	}

	public Dimension getPreferredSize() {
		Dimension d = super.getPreferredSize();
		d.width = getFontMetrics(getFont()).stringWidth("9") * 5;
		return d;
	}

	public void updateLineNumbers() {
		String lineNumbersText = getLineNumbersText();
		System.out.println("updateLineNumbers: " + lineNumbersText);
		setText(lineNumbersText);
	}

	private String getLineNumbersText() {
		int caretPosition = textArea.getDocument().getLength();
		Element root = textArea.getDocument().getDefaultRootElement();
		StringBuilder lineNumbersTextBuilder = new StringBuilder();
		lineNumbersTextBuilder.append("1").append(System.lineSeparator());

		for (int elementIndex = 2; elementIndex < root.getElementIndex(caretPosition) + 2; elementIndex++)
			lineNumbersTextBuilder.append(elementIndex).append(System.lineSeparator());

		return lineNumbersTextBuilder.toString();
	}
}