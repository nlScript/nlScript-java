package de.nls.ui;

import de.nls.Autocompleter;
import de.nls.ParsedNode;
import de.nls.Parser;
import de.nls.core.Autocompletion;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.LutLoader;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;

public class ACEditor {

	private final JFrame frame;
	// private final JTextArea textArea;
	private final JTextComponent textArea;
	private final AutocompletionContext autocompletionContext;
	private final Parser parser;
	private final JButton runButton;

	public ACEditor(Parser parser) {
		this.parser = parser;
		textArea = new JTextArea(20, 60);
		textArea.setFont(new Font("monospaced", Font.BOLD, 12));
		String text = "";
		for(int i = 0; i < 100; i++) {
			text += (i + 1) + "\n";
		}
		textArea.setText(text);

		JScrollPane textAreaScrollPane = new JScrollPane(textArea);

		TextLineNumber textLineNumber = new TextLineNumber(textArea);
		textAreaScrollPane.setRowHeaderView(textLineNumber);

		frame = new JFrame();
		frame.getContentPane().add(textAreaScrollPane);

		JPanel buttons = new JPanel(new FlowLayout());
		runButton = new JButton("Run");
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

	private static int getDistance2(int[] rgb1, int[] rgb2) {
		int dr = rgb1[0] - rgb2[0];
		int dg = rgb1[1] - rgb2[1];
		int db = rgb1[2] - rgb2[2];
		return dr * dr + dg * dg + db * db;
	}

	private static int getClosest(IndexColorModel cm, int[] rgb) {
		int l = cm.getMapSize();
		int[] rgbCM = new int[] {255, 255, 255};
		int minDistance = getDistance2(rgb, rgbCM);
		int minI = 255;
		for(int i = 0; i < l - 1; i++) {
			rgbCM[0] = cm.getRed(i);
			rgbCM[1] = cm.getGreen(i);
			rgbCM[2] = cm.getBlue(i);
			int distance = getDistance2(rgb,rgbCM);
			if(distance < minDistance) {
				minDistance = distance;
				minI = i;
			}
		}
		return minI;
	}

	public static void main2(String... args) {
		new ij.ImageJ();
		ImagePlus imp = IJ.openImage("d:/rpalmisano/2023-07-12/OICE-Logo-ohne Schrift.tif");
		imp.show();
		ColorProcessor ip = (ColorProcessor) imp.getProcessor();
		IndexColorModel cm = LutLoader.openLut("D:\\3Dscript_Releases\\Release_2022-04-06\\Fiji.app\\luts\\physics.lut").getColorModel();
		// "D:\3Dscript_Releases\Release_2022-04-06\Fiji.app\luts\physics.lut"
		ImageProcessor out = ip.convertToByte(false);

		int[] rgb = new int[3];
		for(int y = 0; y < ip.getHeight(); y++) {
			for(int x = 0; x < ip.getWidth(); x++) {
				rgb = ip.getPixel(x, y, rgb);
				out.set(x, y, getClosest(cm, rgb));
			}
		}

		new ImagePlus("gray", out).show();
	}

	public static void main(String[] args) {
		new ACEditor(initParser2()).setVisible(true);
		if(true)
			return;
		Parser parser = initParser2();
		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		String input =
				"Define channel 'dapi':\n" +
				"  excite with 5% at 470nm";

		long start = System.currentTimeMillis();
		ParsedNode root = parser.parse(input, autocompletions);
		long end = System.currentTimeMillis();
		System.out.println("took " + (end - start) + " ms.");
//		System.out.println(GraphViz.toVizDotLink(root));
	}

	public enum Binning {
		ONE(  1, "1 x 1"),
		TWO(  2, "2 x 2"),
		THREE(3, "3 x 3"),
		FOUR( 4, "4 x 4"),
		FIVE( 5, "5 x 5");

		public final int binning;
		public final String label;

		Binning(int binning, String label) {
			this.binning = binning;
			this.label = label;
		}

		public String toString() {
			return label;
		}
	}

	private static Parser initParser2() {

		final ArrayList<String> definedChannels = new ArrayList<>();
		final ArrayList<String> definedRegions  = new ArrayList<>();

		Parser parser = new Parser();
		parser.addParseStartListener(() -> {
			System.out.println("Clear defined channels");
			definedChannels.clear();
			definedRegions.clear();
		});

		parser.defineType("led", "385nm", e -> null);
		parser.defineType("led", "470nm", e -> null);
		parser.defineType("led", "567nm", e -> null);
		parser.defineType("led", "625nm", e -> null);

		parser.defineType("led-power", "{<led-power>:int}%", e -> null, true);
		parser.defineType("exposure-time", "{<exposure-time>:int}ms", e -> null, true);
		parser.defineType("led-setting", "{led-power:led-power} at {wavelength:led}", e -> null, true);
		parser.defineType("another-led-setting", ", {led-setting:led-setting}", e -> null, true);

		parser.defineType("channel-name", "'{<name>:[A-Za-z0-9]:+}'", e -> null, new Autocompleter.IfNothingYetEnteredAutocompleter("'${name}'"));

		parser.defineSentence(
				"Define channel {channel-name:channel-name}:" +
				"{\n  }excite with {led-setting:led-setting}{another-led-setting:another-led-setting:0-3}" +
				"{\n  }use an exposure time of {exposure-time:exposure-time}.",
				e -> null
		).onSuccessfulParsed(n -> {
			System.out.println("Successfully parsed " + n.getParsedString("channel-name"));
			definedChannels.add(n.getParsedString("channel-name"));
		});

		// Define "Tile Scan 1" as a (w x h x d) region centered at (x, y, z)
		parser.defineType("region-name", "'{<region-name>:[a-zA-Z0-9]:+}'", e -> null, new Autocompleter.IfNothingYetEnteredAutocompleter("'${region-name}'"));
		parser.defineType("region-dimensions", "{<width>:float} x {<height>:float} x {<depth>:float} microns", e -> null, true);
		parser.defineType("region-center", "{<center>:tuple<float,x,y,z>} microns", e-> null, true);
		parser.defineType("sentence",
				"Define a position {region-name:region-name}:" +
						"{\n  }{region-dimensions:region-dimensions}" +
						"{\n  }centered at {region-center:region-center}.",
				e -> null
		).onSuccessfulParsed(n -> {
			System.out.println("Successfully parsed " + n.getParsedString("region-name"));
			definedRegions.add(n.getParsedString("region-name"));
		});

		parser.defineSentence(
				"Define the output folder at {folder:path}.",
				e -> null);

		parser.defineType("defined-channels", "'{channel:[A-Za-z0-9]:+}'",
				e -> null,
				e -> {
					String ret = String.join(";;;", definedChannels);
					System.out.println("defined-channels: autocomplete: " + ret);
					return ret;
				});

		parser.defineType("defined-positions", "'{position:[A-Za-z0-9]:+}'",
				e -> e.getParsedString("position"),
				e -> String.join(";;;", definedRegions));

		parser.defineType("time-unit", "second(s)", e -> 1);
		parser.defineType("time-unit", "minute(s)", e -> 60);
		parser.defineType("time-unit", "hour(s)",   e -> 3600);

		parser.defineType("time-interval", "{n:float} {time-unit:time-unit}", e -> {
			float n = (Float)e.evaluate("n");
			int unit = (Integer)e.evaluate("time-unit");
			long seconds = Math.round(n * unit);
			return seconds;
		}, true);

		parser.defineType("z-distance", "{z-distance:float} microns", e -> null, true);

		parser.defineType("lens",  "5x lens", e -> null);
		parser.defineType("lens", "20x lens", e -> null);

		parser.defineType("mag", "0.5x magnification changer", e -> null);
		parser.defineType("mag", "1.0x magnification changer", e -> null);
		parser.defineType("mag", "2.0x magnification changer", e -> null);

		parser.defineType("binning", "1 x 1", e -> 1);
		parser.defineType("binning", "2 x 2", e -> 2);
		parser.defineType("binning", "4 x 4", e -> 4);
		parser.defineType("binning", "8 x 8", e -> 8);

		parser.defineType("temperature", "{temperature:float}\u00B0C", e -> null, true);
		parser.defineType("co2-concentration", "{CO2 concentration:float}%", e -> null, true);

		parser.defineType("incubation",
				"set the temperature to {temperature:temperature}",
				e -> null);

		parser.defineType("incubation",
				"set the CO2 concentration to {co2-concentration:co2-concentration}",
				e -> null);

		parser.defineType("acquisition",
				"acquire..." +
						"{\n  }every {interval:time-interval} for {duration:time-interval}" +
						"{\n  }position(s) {positions:list<defined-positions>}" +
						"{\n  }channel(s) {channels:list<defined-channels>}" +
//				"{\n  }with a resolution of {dx:float} x {dy:float} x {dz:float} microns.",
						"{\n  }with a plane distance of {dz:z-distance}" +
						"{\n  }using the {lens:lens} with the {magnification:mag} and a binning of {binning:binning}",
				e -> null);


		parser.defineType("start", "At the beginning",            e -> null);
		parser.defineType("start", "At {time:time}",              e -> null, true);
		parser.defineType("start", "After {delay:time-interval}", e -> null, true);

		parser.defineSentence("{start:start}, {acquisition:acquisition}.", e -> null);

		parser.defineSentence("{start:start}, {incubation:incubation}.", e -> null);
		return parser;
	}

	private static Parser initParser() {
		Parser parser = new Parser();
		parser.defineType("color", "blue",  e -> null);
		parser.defineType("color", "green", e -> null);

		parser.defineSentence("My favorite color is {color:color}.", e -> null);

		parser.defineSentence("Please come {now:int} and {then:int}.", e -> null, true);
		parser.defineSentence("Add {sum1:int} and {sum2:int}.", e -> null, true);
		parser.defineSentence("{color:color} is my favorite.", e -> null, true);
		return parser;
	}
}
