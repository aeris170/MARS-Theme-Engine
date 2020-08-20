package mars.venus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import mars.Globals;
import mars.Settings;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterAccessNotice;
import mars.mips.hardware.RegisterFile;
import mars.simulator.Simulator;
import mars.simulator.SimulatorNotice;
import mars.util.Binary;

/*
 * Copyright (c) 2003-2009, Pete Sanderson and Kenneth Vollmar
 *
 * Developed by Pete Sanderson (psanderson@otterbein.edu) and Kenneth Vollmar
 * (kenvollmar@missouristate.edu)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * (MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Sets up a window to display registers in the UI.
 *
 * @author Sanderson, Bumgarner
 **/

public class RegistersWindow extends JPanel implements Observer {

	/**
	 *
	 */
	private static final long serialVersionUID = -1062568029810971377L;
	private static JTable table;
	private static Register[] registers;
	private Object[][] tableData;
	private boolean highlighting;
	private int highlightRow;
	private ExecutePane executePane;
	private static final int NAME_COLUMN = 0;
	private static final int NUMBER_COLUMN = 1;
	private static final int VALUE_COLUMN = 2;
	private static Settings settings;

	/**
	 * Constructor which sets up a fresh window with a table that contains the
	 * register values.
	 **/

	public RegistersWindow() {
		Simulator.getInstance().addObserver(this);
		settings = Globals.getSettings();
		highlighting = false;
		table = new MyTippedJTable(new RegTableModel(setupWindow()));
		table.getColumnModel().getColumn(NAME_COLUMN).setPreferredWidth(25);
		table.getColumnModel().getColumn(NUMBER_COLUMN).setPreferredWidth(25);
		table.getColumnModel().getColumn(VALUE_COLUMN).setPreferredWidth(60);
		// Display register values (String-ified) right-justified in mono font
		table.getColumnModel().getColumn(NAME_COLUMN).setCellRenderer(new RegisterCellRenderer(
				MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.LEFT));
		table.getColumnModel().getColumn(NUMBER_COLUMN).setCellRenderer(new RegisterCellRenderer(
				MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.RIGHT));
		table.getColumnModel().getColumn(VALUE_COLUMN).setCellRenderer(new RegisterCellRenderer(
				MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.RIGHT));
		table.setPreferredScrollableViewportSize(new Dimension(200, 700));
		setLayout(new BorderLayout()); // table display will occupy entire width if widened
		this.add(new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
	}

	/**
	 * Sets up the data for the window.
	 *
	 * @return The array object with the data for the window.
	 **/

	public Object[][] setupWindow() {
		final int valueBase = NumberDisplayBaseChooser.getBase(settings.getDisplayValuesInHex());
		tableData = new Object[35][3];
		registers = RegisterFile.getRegisters();
		for (int i = 0; i < registers.length; i++) {
			tableData[i][0] = registers[i].getName();
			tableData[i][1] = new Integer(registers[i].getNumber());
			tableData[i][2] = NumberDisplayBaseChooser.formatNumber(registers[i].getValue(), valueBase);
		}
		tableData[32][0] = "pc";
		tableData[32][1] = "";//new Integer(32);
		tableData[32][2] = NumberDisplayBaseChooser.formatUnsignedInteger(RegisterFile.getProgramCounter(), valueBase);

		tableData[33][0] = "hi";
		tableData[33][1] = "";//new Integer(33);
		tableData[33][2] = NumberDisplayBaseChooser.formatNumber(RegisterFile.getValue(33), valueBase);

		tableData[34][0] = "lo";
		tableData[34][1] = "";//new Integer(34);
		tableData[34][2] = NumberDisplayBaseChooser.formatNumber(RegisterFile.getValue(34), valueBase);

		return tableData;
	}

	/**
	 * clear and redisplay registers
	 */
	public void clearWindow() {
		clearHighlighting();
		RegisterFile.resetRegisters();
		this.updateRegisters(Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase());
	}

	/**
	 * Clear highlight background color from any cell currently highlighted.
	 */
	public void clearHighlighting() {
		highlighting = false;
		if (table != null) { table.tableChanged(new TableModelEvent(table.getModel())); }
		highlightRow = -1; // assure highlight will not occur upon re-assemble.
	}

	/**
	 * Refresh the table, triggering re-rendering.
	 */
	public void refresh() {
		if (table != null) { table.tableChanged(new TableModelEvent(table.getModel())); }
	}

	/**
	 * update register display using current number base (10 or 16)
	 */
	public void updateRegisters() {
		updateRegisters(Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase());
	}

	/**
	 * update register display using specified number base (10 or 16)
	 *
	 * @param base desired number base
	 */
	public void updateRegisters(final int base) {
		registers = RegisterFile.getRegisters();
		for (int i = 0; i < registers.length; i++) {
			updateRegisterValue(registers[i].getNumber(), registers[i].getValue(), base);
		}
		updateRegisterUnsignedValue(32, RegisterFile.getProgramCounter(), base);
		updateRegisterValue(33, RegisterFile.getValue(33), base);
		updateRegisterValue(34, RegisterFile.getValue(34), base);
	}

	/**
	 * This method handles the updating of the GUI.
	 *
	 * @param number The number of the register to update.
	 * @param val    New value.
	 **/

	public void updateRegisterValue(final int number, final int val, final int base) {
		((RegTableModel) table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(val, base),
				number, 2);
	}

	private void updateRegisterUnsignedValue(final int number, final int val, final int base) {
		((RegTableModel) table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatUnsignedInteger(val,
				base), number, 2);
	}

	/**
	 * Required by Observer interface. Called when notified by an Observable that we
	 * are registered with. Observables include: The Simulator object, which lets us
	 * know when it starts and stops running A register object, which lets us know
	 * of register operations The Simulator keeps us informed of when simulated MIPS
	 * execution is active. This is the only time we care about register operations.
	 *
	 * @param observable The Observable object who is notifying us
	 * @param obj        Auxiliary object with additional information.
	 */
	@Override
	public void update(final Observable observable, final Object obj) {
		if (observable == mars.simulator.Simulator.getInstance()) {
			final SimulatorNotice notice = (SimulatorNotice) obj;
			if (notice.getAction() == SimulatorNotice.SIMULATOR_START) {
				// Simulated MIPS execution starts.  Respond to memory changes if running in timed
				// or stepped mode.
				if (notice.getRunSpeed() != RunSpeedPanel.UNLIMITED_SPEED || notice.getMaxSteps() == 1) {
					RegisterFile.addRegistersObserver(this);
					highlighting = true;
				}
			} else {
				// Simulated MIPS execution stops.  Stop responding.
				RegisterFile.deleteRegistersObserver(this);
			}
		} else if (obj instanceof RegisterAccessNotice) {
			// NOTE: each register is a separate Observable
			final RegisterAccessNotice access = (RegisterAccessNotice) obj;
			if (access.getAccessType() == AccessNotice.WRITE) {
				// Uses the same highlighting technique as for Text Segment -- see
				// AddressCellRenderer class in DataSegmentWindow.java.
				highlighting = true;
				highlightCellForRegister((Register) observable);
				Globals.getGui().getRegistersPane().setSelectedComponent(this);
			}
		}
	}

	/**
	 * Highlight the row corresponding to the given register.
	 *
	 * @param register Register object corresponding to row to be selected.
	 */
	void highlightCellForRegister(final Register register) {
		highlightRow = register.getNumber();
		// Tell the system that table contents have changed.  This will trigger re-rendering
		// during which cell renderers are obtained.  The row of interest (identified by
		// instance variabls this.registerRow) will get a renderer
		// with highlight background color and all others get renderer with default background.
		table.tableChanged(new TableModelEvent(table.getModel()));
	}

	/*
	* Cell renderer for displaying register entries.  This does highlighting, so if you
	* don't want highlighting for a given column, don't use this.  Currently we highlight
	* all columns.
	*/
	private class RegisterCellRenderer extends DefaultTableCellRenderer {

		/**
		 *
		 */
		private static final long serialVersionUID = 3326458046306618533L;
		private final Font font;
		private final int alignment;

		public RegisterCellRenderer(final Font font, final int alignment) {
			super();
			this.font = font;
			this.alignment = alignment;
		}

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column) {
			final JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
					column);
			cell.setFont(font);
			cell.setHorizontalAlignment(alignment);
			if (settings.getRegistersHighlighting() && highlighting && row == highlightRow) {
				cell.setBackground(settings.getColorSettingByPosition(Settings.REGISTER_HIGHLIGHT_BACKGROUND));
				cell.setForeground(settings.getColorSettingByPosition(Settings.REGISTER_HIGHLIGHT_FOREGROUND));
				cell.setFont(settings.getFontByPosition(Settings.REGISTER_HIGHLIGHT_FONT));
			} else if (row % 2 == 0) {
				cell.setBackground(settings.getColorSettingByPosition(Settings.EVEN_ROW_BACKGROUND));
				cell.setForeground(settings.getColorSettingByPosition(Settings.EVEN_ROW_FOREGROUND));
				cell.setFont(settings.getFontByPosition(Settings.EVEN_ROW_FONT));
			} else {
				cell.setBackground(settings.getColorSettingByPosition(Settings.ODD_ROW_BACKGROUND));
				cell.setForeground(settings.getColorSettingByPosition(Settings.ODD_ROW_FOREGROUND));
				cell.setFont(settings.getFontByPosition(Settings.ODD_ROW_FONT));
			}
			return cell;
		}
	}

	////////////////////////////////////////////////////////////////////////////

	class RegTableModel extends AbstractTableModel {

		/**
		 *
		 */
		private static final long serialVersionUID = -1232936166438401626L;
		final String[] columnNames = { "Name", "Number", "Value" };
		Object[][] data;

		public RegTableModel(final Object[][] d) {
			data = d;
		}

		@Override
		public int getColumnCount() { return columnNames.length; }

		@Override
		public int getRowCount() { return data.length; }

		@Override
		public String getColumnName(final int col) {
			return columnNames[col];
		}

		@Override
		public Object getValueAt(final int row, final int col) {
			return data[row][col];
		}

		/*
		 * JTable uses this method to determine the default renderer/
		 * editor for each cell.
		*/
		@Override
		public Class getColumnClass(final int c) {
			return getValueAt(0, c).getClass();
		}

		/*
		* Don't need to implement this method unless your table's
		* editable.
		*/
		@Override
		public boolean isCellEditable(final int row, final int col) {
			//Note that the data/cell address is constant,
			//no matter where the cell appears onscreen.
			// these registers are not editable: $zero (0), $pc (32), $ra (31)
			if (col == VALUE_COLUMN && row != 0 && row != 32 && row != 31) {
				return true;
			} else {
				return false;
			}
		}

		/*
		 * Update cell contents in table model.  This method should be called
		* only when user edits cell, so input validation has to be done.  If
		* value is valid, MIPS register is updated.
		 */
		@Override
		public void setValueAt(final Object value, final int row, final int col) {
			int val = 0;
			try {
				val = Binary.stringToInt((String) value);
			} catch (final NumberFormatException nfe) {
				data[row][col] = "INVALID";
				fireTableCellUpdated(row, col);
				return;
			}
			//  Assures that if changed during MIPS program execution, the update will
			//  occur only between MIPS instructions.
			synchronized (Globals.memoryAndRegistersLock) {
				RegisterFile.updateRegister(row, val);
			}
			final int valueBase = Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase();
			data[row][col] = NumberDisplayBaseChooser.formatNumber(val, valueBase);
			fireTableCellUpdated(row, col);
			return;
		}

		/**
		 * Update cell contents in table model. Does not affect MIPS register.
		 */
		private void setDisplayAndModelValueAt(final Object value, final int row, final int col) {
			data[row][col] = value;
			fireTableCellUpdated(row, col);
		}

		// handy for debugging....
		private void printDebugData() {
			final int numRows = getRowCount();
			final int numCols = getColumnCount();

			for (int i = 0; i < numRows; i++) {
				System.out.print("    row " + i + ":");
				for (int j = 0; j < numCols; j++) {
					System.out.print("  " + data[i][j]);
				}
				System.out.println();
			}
			System.out.println("--------------------------");
		}
	}

	///////////////////////////////////////////////////////////////////
	//
	// JTable subclass to provide custom tool tips for each of the
	// register table column headers and for each register name in
	// the first column. From Sun's JTable tutorial.
	// http://java.sun.com/docs/books/tutorial/uiswing/components/table.html
	//
	private class MyTippedJTable extends JTable {

		/**
		 *
		 */
		private static final long serialVersionUID = 513907327826617134L;

		MyTippedJTable(final RegTableModel m) {
			super(m);
			setRowSelectionAllowed(true); // highlights background color of entire row
			setSelectionBackground(Color.GREEN);
		}

		private final String[] regToolTips = { /* $zero */ "constant 0", /* $at   */ "reserved for assembler",
				/* $v0   */ "expression evaluation and results of a function",
				/* $v1   */ "expression evaluation and results of a function", /* $a0   */ "argument 1",
				/* $a1   */ "argument 2", /* $a2   */ "argument 3", /* $a3   */ "argument 4",
				/* $t0   */ "temporary (not preserved across call)",
				/* $t1   */ "temporary (not preserved across call)",
				/* $t2   */ "temporary (not preserved across call)",
				/* $t3   */ "temporary (not preserved across call)",
				/* $t4   */ "temporary (not preserved across call)",
				/* $t5   */ "temporary (not preserved across call)",
				/* $t6   */ "temporary (not preserved across call)",
				/* $t7   */ "temporary (not preserved across call)",
				/* $s0   */ "saved temporary (preserved across call)",
				/* $s1   */ "saved temporary (preserved across call)",
				/* $s2   */ "saved temporary (preserved across call)",
				/* $s3   */ "saved temporary (preserved across call)",
				/* $s4   */ "saved temporary (preserved across call)",
				/* $s5   */ "saved temporary (preserved across call)",
				/* $s6   */ "saved temporary (preserved across call)",
				/* $s7   */ "saved temporary (preserved across call)",
				/* $t8   */ "temporary (not preserved across call)",
				/* $t9   */ "temporary (not preserved across call)", /* $k0   */ "reserved for OS kernel",
				/* $k1   */ "reserved for OS kernel", /* $gp   */ "pointer to global area", /* $sp   */ "stack pointer",
				/* $fp   */ "frame pointer", /* $ra   */ "return address (used by function call)",
				/* pc    */ "program counter", /* hi    */ "high-order word of multiply product, or divide remainder",
				/* lo    */ "low-order word of multiply product, or divide quotient" };

		//Implement table cell tool tips.
		@Override
		public String getToolTipText(final MouseEvent e) {
			String tip = null;
			final java.awt.Point p = e.getPoint();
			final int rowIndex = rowAtPoint(p);
			final int colIndex = columnAtPoint(p);
			final int realColumnIndex = convertColumnIndexToModel(colIndex);
			if (realColumnIndex == NAME_COLUMN) { //Register name column
				tip = regToolTips[rowIndex];
				/* You can customize each tip to encorporiate cell contents if you like:
				   TableModel model = getModel();
				   String regName = (String)model.getValueAt(rowIndex,0);
					....... etc .......
				*/
			} else {
				//You can omit this part if you know you don't have any
				//renderers that supply their own tool tips.
				tip = super.getToolTipText(e);
			}
			return tip;
		}

		private final String[] columnToolTips = {
				/* name */ "Each register has a tool tip describing its usage convention",
				/* number */ "Corresponding register number", /* value */ "Current 32 bit value" };

		//Implement table header tool tips.
		@Override
		protected JTableHeader createDefaultTableHeader() {
			return new JTableHeader(columnModel) {

				@Override
				public String getToolTipText(final MouseEvent e) {
					final java.awt.Point p = e.getPoint();
					final int index = columnModel.getColumnIndexAtX(p.x);
					final int realIndex = columnModel.getColumn(index).getModelIndex();
					return columnToolTips[realIndex];
				}
			};
		}
	}

}
