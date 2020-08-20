package mars.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Observable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import mars.Globals;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;

@SuppressWarnings("serial")
/* Add these two lines in exceptions.java file
 * public static final int EXTERNAL_INTERRUPT_TIMER = 0x00000100; //Add for digital Lab Sim
 * public static final int EXTERNAL_INTERRUPT_HEXA_KEYBOARD = 0x00000200;// Add for digital Lab Sim
*/

/*
 * Didier Teifreto LIFC Université de franche-Comté www.lifc.univ-fcomte.fr/~teifreto
 * didier.teifreto@univ-fcomte.fr
 */
public class DigitalLabSim extends AbstractMarsToolAndApplication {

	/**
	 *
	 */
	private static final long serialVersionUID = -6287022620154742886L;
	private static String heading = "Digital Lab Sim";
	private static String version = " Version 1.0 (Didier Teifreto)";
	private static final int IN_ADRESS_DISPLAY_1 = Memory.memoryMapBaseAddress + 0x10;
	private static final int IN_ADRESS_DISPLAY_2 = Memory.memoryMapBaseAddress + 0x11;
	private static final int IN_ADRESS_HEXA_KEYBOARD = Memory.memoryMapBaseAddress + 0x12;
	private static final int IN_ADRESS_COUNTER = Memory.memoryMapBaseAddress + 0x13;
	private static final int OUT_ADRESS_HEXA_KEYBOARD = Memory.memoryMapBaseAddress + 0x14;

	public static final int EXTERNAL_INTERRUPT_TIMER = 0x00000100; //Add for digital Lab Sim
	public static final int EXTERNAL_INTERRUPT_HEXA_KEYBOARD = 0x00000200;// Add for digital Lab Sim

	// GUI Interface.
	private static JPanel panelTools;
	// Seven Segment display
	private SevenSegmentPanel sevenSegPanel;
	// Keyboard
	private static int KeyBoardValueButtonClick = -1; // -1 no button click
	private HexaKeyboard hexaKeyPanel;
	private static boolean KeyboardInterruptOnOff = false;
	// Counter
	private static int CounterValueMax = 30;
	private static int CounterValue = CounterValueMax;
	private static boolean CounterInterruptOnOff = false;
	private static OneSecondCounter SecondCounter;

	public DigitalLabSim(final String title, final String heading) {
		super(title, heading);
	}

	public DigitalLabSim() {
		super(heading + ", " + version, heading);
	}

	public static void main(final String[] args) {
		new DigitalLabSim(heading + ", " + version, heading).go();
	}

	@Override
	public String getName() { return "Digital Lab Sim"; }

	@Override
	protected void addAsObserver() {
		addAsObserver(IN_ADRESS_DISPLAY_1, IN_ADRESS_DISPLAY_1);
		addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
	}

	@Override
	public void update(final Observable ressource, final Object accessNotice) {
		final MemoryAccessNotice notice = (MemoryAccessNotice) accessNotice;
		final int address = notice.getAddress();
		final char value = (char) notice.getValue();
		if (address == IN_ADRESS_DISPLAY_1) {
			updateSevenSegment(1, value);
		} else if (address == IN_ADRESS_DISPLAY_2) {
			updateSevenSegment(0, value);
		} else if (address == IN_ADRESS_HEXA_KEYBOARD) {
			updateHexaKeyboard(value);
		} else if (address == IN_ADRESS_COUNTER) { updateOneSecondCounter(value); }
		if (CounterInterruptOnOff) {
			if (CounterValue > 0) {
				CounterValue--;
			} else {
				CounterValue = CounterValueMax;
				if ((Coprocessor0.getValue(Coprocessor0.STATUS) & 2) == 0) {
					mars.simulator.Simulator.externalInterruptingDevice = /*Exceptions.*/EXTERNAL_INTERRUPT_TIMER;
				}
			}
		}
	}

	@Override
	protected void reset() {
		sevenSegPanel.resetSevenSegment();
		hexaKeyPanel.resetHexaKeyboard();
		SecondCounter.resetOneSecondCounter();
	}

	@Override
	protected JComponent buildMainDisplayArea() {
		panelTools = new JPanel(new GridLayout(1, 2));
		sevenSegPanel = new SevenSegmentPanel();
		panelTools.add(sevenSegPanel);
		hexaKeyPanel = new HexaKeyboard();
		panelTools.add(hexaKeyPanel);
		SecondCounter = new OneSecondCounter();
		return panelTools;
	}

	private synchronized void updateMMIOControlAndData(final int dataAddr, final int dataValue) {
		if (!isBeingUsedAsAMarsTool || isBeingUsedAsAMarsTool && connectButton.isConnected()) {
			synchronized (Globals.memoryAndRegistersLock) {
				try {
					Globals.memory.setByte(dataAddr, dataValue);
				} catch (final AddressErrorException aee) {
					System.out.println("Tool author specified incorrect MMIO address!" + aee);
					System.exit(0);
				}
			}
			if (Globals.getGui() != null && Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow()
					.getCodeHighlighting()) {
				Globals.getGui().getMainPane().getExecutePane().getDataSegmentWindow().updateValues();
			}
		}
	}

	@Override
	protected JComponent getHelpComponent() {
		final String helpContent = " This tool is composed of 3 parts : two seven-segment displays, an hexadecimal keyboard and counter \n"
				+ "Seven segment display\n"
				+ " Byte value at address 0xFFFF0010 : command right seven segment display \n "
				+ " Byte value at address 0xFFFF0011 : command left seven segment display \n "
				+ " Each bit of these two bytes are connected to segments (bit 0 for a segment, 1 for b segment and 7 for point \n \n"
				+ "Hexadecimal keyboard\n"
				+ " Byte value at address 0xFFFF0012 : command row number of hexadecimal keyboard (bit 0 to 3) and enable keyboard interrupt (bit 7) \n"
				+ " Byte value at address 0xFFFF0014 : receive row and column of the key pressed, 0 if not key pressed \n"
				+ " The mips program have to scan, one by one, each row (send 1,2,4,8...)"
				+ " and then observe if a key is pressed (that mean byte value at adresse 0xFFFF0014 is different from zero). "
				+ " This byte value is composed of row number (4 left bits) and column number (4 right bits)"
				+ " Here you'll find the code for each key : 0x11,0x21,0x41,0x81,0x12,0x22,0x42,0x82,0x14,0x24,0x44,0x84,0x18,0x28,0x48,0x88. \n"
				+ " For exemple key number 2 return 0x41, that mean the key is on column 3 and row 1. \n"
				+ " If keyboard interruption is enable, an exception is started, with cause register bit number 11 set.\n \n"
				+ "Counter\n"
				+ " Byte value at address 0xFFFF0013 : If one bit of this byte is set, the counter interruption is enable.\n"
				+ " If counter interruption is enable, every 30 instructions, an exception is started with cause register bit number 10.\n"
				+ "   (contributed by Didier Teifreto, dteifreto@lifc.univ-fcomte.fr)";
		final JButton help = new JButton("Help");
		help.addActionListener(e -> {
			final JTextArea ja = new JTextArea(helpContent);
			ja.setRows(20);
			ja.setColumns(60);
			ja.setLineWrap(true);
			ja.setWrapStyleWord(true);
			JOptionPane.showMessageDialog(theWindow, new JScrollPane(ja),
					"Simulating the Hexa Keyboard and Seven segment display", JOptionPane.INFORMATION_MESSAGE);
		});
		return help;
	}/* ....................Seven Segment display start here................................... */
	/* ...........................Seven segment display start here ..............................*/

	public void updateSevenSegment(final int number, final char value) {
		sevenSegPanel.display[number].modifyDisplay(value);
	}

	public class SevenSegmentDisplay extends JComponent {

		/**
		 *
		 */
		private static final long serialVersionUID = 7317442202378697704L;
		public char aff;

		public SevenSegmentDisplay(final char aff) {
			this.aff = aff;
			setPreferredSize(new Dimension(60, 80));
		}

		public void modifyDisplay(final char val) {
			aff = val;
			this.repaint();
		}

		public void SwitchSegment(final Graphics g, final char segment) {
			switch (segment) {
			case 'a': //a segment
				final int[] pxa1 = { 12, 9, 12 };
				final int[] pxa2 = { 36, 39, 36 };
				final int[] pya = { 5, 8, 11 };
				g.fillPolygon(pxa1, pya, 3);
				g.fillPolygon(pxa2, pya, 3);
				g.fillRect(12, 5, 24, 6);
				break;
			case 'b': //b segment	
				final int[] pxb = { 37, 40, 43 };
				final int[] pyb1 = { 12, 9, 12 };
				final int[] pyb2 = { 36, 39, 36 };
				g.fillPolygon(pxb, pyb1, 3);
				g.fillPolygon(pxb, pyb2, 3);
				g.fillRect(37, 12, 6, 24);
				break;
			case 'c': // c segment
				final int[] pxc = { 37, 40, 43 };
				final int[] pyc1 = { 44, 41, 44 };
				final int[] pyc2 = { 68, 71, 68 };
				g.fillPolygon(pxc, pyc1, 3);
				g.fillPolygon(pxc, pyc2, 3);
				g.fillRect(37, 44, 6, 24);
				break;
			case 'd': // d segment
				final int[] pxd1 = { 12, 9, 12 };
				final int[] pxd2 = { 36, 39, 36 };
				final int[] pyd = { 69, 72, 75 };
				g.fillPolygon(pxd1, pyd, 3);
				g.fillPolygon(pxd2, pyd, 3);
				g.fillRect(12, 69, 24, 6);
				break;
			case 'e': // e segment
				final int[] pxe = { 5, 8, 11 };
				final int[] pye1 = { 44, 41, 44 };
				final int[] pye2 = { 68, 71, 68 };
				g.fillPolygon(pxe, pye1, 3);
				g.fillPolygon(pxe, pye2, 3);
				g.fillRect(5, 44, 6, 24);
				break;
			case 'f': // f segment
				final int[] pxf = { 5, 8, 11 };
				final int[] pyf1 = { 12, 9, 12 };
				final int[] pyf2 = { 36, 39, 36 };
				g.fillPolygon(pxf, pyf1, 3);
				g.fillPolygon(pxf, pyf2, 3);
				g.fillRect(5, 12, 6, 24);
				break;
			case 'g': // g segment 
				final int[] pxg1 = { 12, 9, 12 };
				final int[] pxg2 = { 36, 39, 36 };
				final int[] pyg = { 37, 40, 43 };
				g.fillPolygon(pxg1, pyg, 3);
				g.fillPolygon(pxg2, pyg, 3);
				g.fillRect(12, 37, 24, 6);
				break;
			case 'h': // decimal point
				g.fillOval(49, 68, 8, 8);
				break;
			}
		}

		@Override
		public void paint(final Graphics g) {
			char c = 'a';
			while (c <= 'h') {
				if ((aff & 0x1) == 1) {
					g.setColor(Color.RED);
				} else {
					g.setColor(Color.LIGHT_GRAY);
				}
				SwitchSegment(g, c);
				aff = (char) (aff >>> 1);
				c++;
			}
		}
	}

	public class SevenSegmentPanel extends JPanel {

		/**
		 *
		 */
		private static final long serialVersionUID = 8643368219585362191L;
		public SevenSegmentDisplay[] display;

		public SevenSegmentPanel() {
			int i;
			final FlowLayout fl = new FlowLayout();
			setLayout(fl);
			display = new SevenSegmentDisplay[2];
			for (i = 0; i < 2; i++) {
				display[i] = new SevenSegmentDisplay((char) 0);
				this.add(display[i]);
			}
		}

		public void modifyDisplay(final int num, final char val) {
			display[num].modifyDisplay(val);
			display[num].repaint();
		}

		public void resetSevenSegment() {
			int i;
			for (i = 0; i < 2; i++) {
				modifyDisplay(i, (char) 0);
			}
		}
	}

	/* ...........................Seven segment display end here ..............................*/
	/* ....................Hexa Keyboard start here................................... */
	public void updateHexaKeyboard(final char row) {
		final int key = KeyBoardValueButtonClick;
		if (key != -1 && 1 << key / 4 == (row & 0xF)) {
			updateMMIOControlAndData(OUT_ADRESS_HEXA_KEYBOARD, (char) (1 << key / 4) | 1 << 4 + key % 4);
		} else {
			updateMMIOControlAndData(OUT_ADRESS_HEXA_KEYBOARD, 0);
		}
		if ((row & 0xF0) != 0) {
			KeyboardInterruptOnOff = true;
		} else {
			KeyboardInterruptOnOff = false;
		}
	}

	public class HexaKeyboard extends JPanel {

		/**
		 *
		 */
		private static final long serialVersionUID = -4940205404503576092L;
		public JButton[] button;

		public HexaKeyboard() {
			int i;
			final GridLayout layout = new GridLayout(4, 4);
			setLayout(layout);
			button = new JButton[16];
			for (i = 0; i < 16; i++) {
				button[i] = new JButton(Integer.toHexString(i));
				button[i].setBackground(Color.WHITE);
				button[i].setMargin(new Insets(10, 10, 10, 10));
				button[i].addMouseListener(new EcouteurClick(i));
				this.add(button[i]);
			}
		}

		public void resetHexaKeyboard() {
			int i;
			KeyBoardValueButtonClick = -1;
			for (i = 0; i < 16; i++) {
				button[i].setBackground(Color.WHITE);
			}
		}

		public class EcouteurClick implements MouseListener {

			private final int buttonValue;

			public EcouteurClick(final int val) {
				buttonValue = val;
			}

			@Override
			public void mouseEntered(final MouseEvent arg0) {}

			@Override
			public void mouseExited(final MouseEvent arg0) {}

			@Override
			public void mousePressed(final MouseEvent arg0) {}

			@Override
			public void mouseReleased(final MouseEvent arg0) {}

			@Override
			public void mouseClicked(final MouseEvent arg0) {
				int i;
				if (KeyBoardValueButtonClick != -1) {//Button already pressed -> now realease
					KeyBoardValueButtonClick = -1;
					updateMMIOControlAndData(OUT_ADRESS_HEXA_KEYBOARD, 0);
					for (i = 0; i < 16; i++) {
						button[i].setBackground(Color.WHITE);
					}
				} else { // new button pressed
					KeyBoardValueButtonClick = buttonValue;
					button[KeyBoardValueButtonClick].setBackground(Color.GREEN);
					if (KeyboardInterruptOnOff && (Coprocessor0.getValue(Coprocessor0.STATUS) & 2) == 0) {
						mars.simulator.Simulator.externalInterruptingDevice = /*Exceptions.*/EXTERNAL_INTERRUPT_HEXA_KEYBOARD;
					}
				}
			}
		}
	}

	/* ....................Hexa Keyboard end here................................... */
	/* ....................Timer start here................................... */
	public void updateOneSecondCounter(final char value) {
		if (value != 0) {
			CounterInterruptOnOff = true;
			CounterValue = CounterValueMax;
		} else {
			CounterInterruptOnOff = false;
		}
	}

	public class OneSecondCounter {

		public OneSecondCounter() {
			CounterInterruptOnOff = false;
		}

		public void resetOneSecondCounter() {
			CounterInterruptOnOff = false;
			CounterValue = CounterValueMax;
		}
	}
}
