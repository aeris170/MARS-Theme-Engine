package mars.venus;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import mars.Globals;
import mars.MIPSprogram;
import mars.assembler.Symbol;
import mars.assembler.SymbolTable;
import mars.mips.hardware.Memory;
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
 * Represents the Labels window, which is a type of JInternalFrame. Venus user
 * can view MIPS program labels.
 *
 * @author Sanderson and Team JSpim
 **/

public class LabelsWindow extends JInternalFrame {

	/**
	 *
	 */
	private static final long serialVersionUID = -6556430479954986911L;
	private final Container contentPane;
	private final JPanel labelPanel;      // holds J
	private final JCheckBox dataLabels, textLabels;
	private ArrayList listOfLabelsForSymbolTable;
	private final LabelsWindow labelsWindow;
	private static final int MAX_DISPLAYED_CHARS = 24;
	private static final int PREFERRED_NAME_COLUMN_WIDTH = 60;
	private static final int PREFERRED_ADDRESS_COLUMN_WIDTH = 60;
	private static final int LABEL_COLUMN = 0;
	private static final int ADDRESS_COLUMN = 1;
	private static final String[] columnToolTips = { /* LABEL_COLUMN */ "Programmer-defined label (identifier).",
			/* ADDRESS_COLUMN */ "Text or data segment address at which label is defined." };
	private static String[] columnNames;
	private Comparator tableSortComparator;

	/////////////////////////////////////////////////////////////////////////////////////
	// Use 8-state machine to track sort status for displaying tables
	// State    Sort Column     Name sort order   Address sort order  Click Name   Click Addr
	//   0         Addr              ascend             ascend            4            1
	//   1         Addr              ascend             descend           5            0
	//   2         Addr              descend            ascend            6            3
	//   3         Addr              descend            descend           7            2
	//   4         Name              ascend             ascend            6            0
	//   5         Name              ascend             descend           7            1
	//   6         Name              descend            ascend            4            2
	//   7         Name              descend            descend           5            3
	// "Click Name" column shows which state to go to when Name column is clicked.
	// "Click Addr" column shows which state to go to when Addr column is clicked.
	//////////////////////////////////////////////////////////////////////////////////////
	// The array of comparators; index corresponds to state in table above.
	private final Comparator[] tableSortingComparators = { /*  0  */ new LabelAddressAscendingComparator(),
			/*  1  */ new DescendingComparator(new LabelAddressAscendingComparator()),
			/*  2  */ new LabelAddressAscendingComparator(), /*  3  */ new DescendingComparator(
					new LabelAddressAscendingComparator()), /*  4  */ new LabelNameAscendingComparator(),
			/*  5  */ new LabelNameAscendingComparator(), /*  6  */ new DescendingComparator(
					new LabelNameAscendingComparator()), /*  7  */ new DescendingComparator(
							new LabelNameAscendingComparator()) };
	// The array of state transitions; primary index corresponds to state in table above,
	// secondary index corresponds to table columns (0==label name, 1==address).
	private static final int[][] sortStateTransitions = { /*  0  */ { 4, 1 }, /*  1  */ { 5, 0 }, /*  2  */ { 6, 3 },
			/*  3  */ { 7, 2 }, /*  4  */ { 6, 0 }, /*  5  */ { 7, 1 }, /*  6  */ { 4, 2 }, /*  7  */ { 5, 3 } };
	// The array of column headings; index corresponds to state in table above.
	private static final char ASCENDING_SYMBOL = '\u25b2'; //triangle with base at bottom ("points" up, to indicate ascending sort)
	private static final char DESCENDING_SYMBOL = '\u25bc';//triangle with base at top ("points" down, to indicate descending sort)
	private static final String[][] sortColumnHeadings = { /*  0  */ { "Label", "Address  " + ASCENDING_SYMBOL },
			/*  1  */ { "Label", "Address  " + DESCENDING_SYMBOL }, /*  2  */ { "Label", "Address  "
					+ ASCENDING_SYMBOL }, /*  3  */ { "Label", "Address  " + DESCENDING_SYMBOL }, /*  4  */ { "Label  "
							+ ASCENDING_SYMBOL, "Address" }, /*  5  */ { "Label  " + ASCENDING_SYMBOL, "Address" },
			/*  6  */ { "Label  " + DESCENDING_SYMBOL, "Address" }, /*  7  */ { "Label  " + DESCENDING_SYMBOL,
					"Address" } };

	// Current sort state (0-7, see table above).  Will be set from saved Settings in construtor.
	private int sortState = 0;

	/**
	 * Constructor for the Labels (symbol table) window.
	 **/

	public LabelsWindow() {
		super("Labels", true, false, true, true);
		try {
			sortState = Integer.parseInt(Globals.getSettings().getLabelSortState());
		} catch (final NumberFormatException nfe) {
			sortState = 0;
		}
		columnNames = sortColumnHeadings[sortState];
		tableSortComparator = tableSortingComparators[sortState];
		labelsWindow = this;
		contentPane = getContentPane();
		labelPanel = new JPanel(new GridLayout(1, 2, 10, 0));
		final JPanel features = new JPanel();
		dataLabels = new JCheckBox("Data", true);
		textLabels = new JCheckBox("Text", true);
		dataLabels.addItemListener(new LabelItemListener());
		textLabels.addItemListener(new LabelItemListener());
		dataLabels.setToolTipText("If checked, will display labels defined in data segment");
		textLabels.setToolTipText("If checked, will display labels defined in text segment");
		features.add(dataLabels);
		features.add(textLabels);
		contentPane.add(features, BorderLayout.SOUTH);
		contentPane.add(labelPanel);
	}

	/**
	 * Initialize table of labels (symbol table)
	 */
	public void setupTable() {
		labelPanel.removeAll();
		labelPanel.add(generateLabelScrollPane());
	}

	/**
	 * Clear the window
	 */
	public void clearWindow() {
		labelPanel.removeAll();
	}

	//
	private JScrollPane generateLabelScrollPane() {
		listOfLabelsForSymbolTable = new ArrayList();
		listOfLabelsForSymbolTable.add(new LabelsForSymbolTable(null));// global symtab
		final ArrayList MIPSprogramsAssembled = RunAssembleAction.getMIPSprogramsToAssemble();
		final Box allSymtabTables = Box.createVerticalBox();
		for (int i = 0; i < MIPSprogramsAssembled.size(); i++) {
			listOfLabelsForSymbolTable.add(new LabelsForSymbolTable((MIPSprogram) MIPSprogramsAssembled.get(i)));
		}
		final ArrayList tableNames = new ArrayList();
		JTableHeader tableHeader = null;
		for (int i = 0; i < listOfLabelsForSymbolTable.size(); i++) {
			final LabelsForSymbolTable symtab = (LabelsForSymbolTable) listOfLabelsForSymbolTable.get(i);
			if (symtab.hasSymbols()) {
				String name = symtab.getSymbolTableName();
				if (name.length() > MAX_DISPLAYED_CHARS) { name = name.substring(0, MAX_DISPLAYED_CHARS - 3) + "..."; }
				// To get left-justified, put file name into first slot of horizontal Box, then glue.
				final JLabel nameLab = new JLabel(name, SwingConstants.LEFT);
				final Box nameLabel = Box.createHorizontalBox();
				nameLabel.add(nameLab);
				nameLabel.add(Box.createHorizontalGlue());
				nameLabel.add(Box.createHorizontalStrut(1));
				tableNames.add(nameLabel);
				allSymtabTables.add(nameLabel);
				final JTable table = symtab.generateLabelTable();
				tableHeader = table.getTableHeader();
				// The following is selfish on my part.  Column re-ordering doesn't work correctly when
				// displaying multiple symbol tables; the headers re-order but the columns do not.
				// Given the low perceived benefit of reordering displayed symbol table information
				// versus the perceived effort to make reordering work for multiple symbol tables,
				// I am taking the easy way out here.  PS 19 July 2007.
				tableHeader.setReorderingAllowed(false);
				table.setSelectionBackground(table.getBackground());
				// Sense click on label/address and scroll Text/Data segment display to it.
				table.addMouseListener(new LabelDisplayMouseListener());
				allSymtabTables.add(table);
			}
		}
		final JScrollPane labelScrollPane = new JScrollPane(allSymtabTables,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		// Set file name label's max width to scrollpane's viewport width, max height to small.
		// Does it do any good?  Addressing problem that occurs when label (filename) is wider than
		// the table beneath it -- the table column widths are stretched to attain the same width and
		// the address information requires scrolling to see.  All because of a long file name.
		for (int i = 0; i < tableNames.size(); i++) {
			final JComponent nameLabel = (JComponent) tableNames.get(i);
			nameLabel.setMaximumSize(new Dimension(labelScrollPane.getViewport().getViewSize().width, (int) (1.5
					* nameLabel.getFontMetrics(nameLabel.getFont()).getHeight())));
		}
		labelScrollPane.setColumnHeaderView(tableHeader);
		return labelScrollPane;
	}

	/**
	 * Method to update display of label addresses. Since label information doesn't
	 * change, this should only be done when address base is changed. (e.g. between
	 * base 16 hex and base 10 dec).
	 */
	public void updateLabelAddresses() {
		if (listOfLabelsForSymbolTable != null) {
			for (int i = 0; i < listOfLabelsForSymbolTable.size(); i++) {
				((LabelsForSymbolTable) listOfLabelsForSymbolTable.get(i)).updateLabelAddresses();
			}
		}
	}

	///////////////////////////////////////////////////////////////
	//   Listener class to respond to "Text" or "Data" checkbox click
	private class LabelItemListener implements ItemListener {

		@Override
		public void itemStateChanged(final ItemEvent ie) {
			for (int i = 0; i < listOfLabelsForSymbolTable.size(); i++) {
				((LabelsForSymbolTable) listOfLabelsForSymbolTable.get(i)).generateLabelTable();
			}
		}
	}

	/////////////////////////////////////////////////////////////////
	//  Private listener class to sense clicks on a table entry's
	//  Label or Address.  This will trigger action by Text or Data
	//  segment to scroll to the corresponding label/address.
	//  Suggested by Ken Vollmar, implemented by Pete Sanderson
	//  July 2007.

	private class LabelDisplayMouseListener extends MouseAdapter {

		@Override
		public void mouseClicked(final MouseEvent e) {
			final JTable table = (JTable) e.getSource();
			final int row = table.rowAtPoint(e.getPoint());
			final int column = table.columnAtPoint(e.getPoint());
			Object data = table.getValueAt(row, column);
			if (table.getColumnName(column).equals(columnNames[LABEL_COLUMN])) {
				// Selected a Label name, so get its address.
				data = table.getModel().getValueAt(row, ADDRESS_COLUMN);
			}
			int address = 0;
			try {
				address = Binary.stringToInt((String) data);
			} catch (final NumberFormatException nfe) {
				// Cannot happen because address is generated internally.
			} catch (final ClassCastException cce) {
				// Cannot happen because table contains only strings.
			}
			// Scroll to this address, either in Text Segment display or Data Segment display
			if (Memory.inTextSegment(address) || Memory.inKernelTextSegment(address)) {
				Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow().selectStepAtAddress(address);
			} else {
				Globals.getGui().getMainPane().getExecutePane().getDataSegmentWindow().selectCellForAddress(address);
			}
		}
	}

	///////////////////////////////////////////////////////////////////
	// Represents one symbol table for the display.
	private class LabelsForSymbolTable {

		private final MIPSprogram myMIPSprogram;
		private Object[][] labelData;
		private JTable labelTable;
		private ArrayList symbols;
		private final SymbolTable symbolTable;
		private final String tableName;

		// Associated MIPSprogram object.  If null, this represents global symbol table.
		public LabelsForSymbolTable(final MIPSprogram myMIPSprogram) {
			this.myMIPSprogram = myMIPSprogram;
			symbolTable = myMIPSprogram == null ? Globals.symbolTable : myMIPSprogram.getLocalSymbolTable();
			tableName = myMIPSprogram == null ? "(global)" : new File(myMIPSprogram.getFilename()).getName();
		}

		// Returns file name of associated file for local symbol table or "(global)"
		public String getSymbolTableName() { return tableName; }

		public boolean hasSymbols() {
			return symbolTable.getSize() != 0;
		}

		// builds the Table containing labels and addresses for this symbol table.
		private JTable generateLabelTable() {
			final SymbolTable symbolTable = myMIPSprogram == null ? Globals.symbolTable
					: myMIPSprogram.getLocalSymbolTable();
			final int addressBase = Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBase();
			if (textLabels.isSelected() && dataLabels.isSelected()) {
				symbols = symbolTable.getAllSymbols();
			} else if (textLabels.isSelected() && !dataLabels.isSelected()) {
				symbols = symbolTable.getTextSymbols();
			} else if (!textLabels.isSelected() && dataLabels.isSelected()) {
				symbols = symbolTable.getDataSymbols();
			} else {
				symbols = new ArrayList();
			}
			Collections.sort(symbols, tableSortComparator); // DPS 25 Dec 2008
			labelData = new Object[symbols.size()][2];

			for (int i = 0; i < symbols.size(); i++) {//sets up the label table
				final Symbol s = (Symbol) symbols.get(i);
				labelData[i][LABEL_COLUMN] = s.getName();
				labelData[i][ADDRESS_COLUMN] = NumberDisplayBaseChooser.formatNumber(s.getAddress(), addressBase);
			}
			final LabelTableModel m = new LabelTableModel(labelData, LabelsWindow.columnNames);
			if (labelTable == null) {
				labelTable = new MyTippedJTable(m);
			} else {
				labelTable.setModel(m);
			}
			labelTable.getColumnModel().getColumn(ADDRESS_COLUMN).setCellRenderer(new MonoRightCellRenderer());
			return labelTable;
		}

		public void updateLabelAddresses() {
			if (labelPanel.getComponentCount() == 0) {
				return; // ignore if no content to change
			}
			final int addressBase = Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBase();
			int address;
			String formattedAddress;
			final int numSymbols = labelData == null ? 0 : labelData.length;
			for (int i = 0; i < numSymbols; i++) {
				address = ((Symbol) symbols.get(i)).getAddress();
				formattedAddress = NumberDisplayBaseChooser.formatNumber(address, addressBase);
				labelTable.getModel().setValueAt(formattedAddress, i, ADDRESS_COLUMN);
			}
		}
	}
	//////////////////////  end of LabelsForOneSymbolTable class //////////////////

	///////////////////////////////////////////////////////////////
	// Class representing label table data
	class LabelTableModel extends AbstractTableModel {

		/**
		 *
		 */
		private static final long serialVersionUID = -7342283880585385711L;
		String[] columns;
		Object[][] data;

		public LabelTableModel(final Object[][] d, final String[] n) {
			data = d;
			columns = n;
		}

		@Override
		public int getColumnCount() { return columns.length; }

		@Override
		public int getRowCount() { return data.length; }

		@Override
		public String getColumnName(final int col) {
			return columns[col];
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
		* data can change.
		*/
		@Override
		public void setValueAt(final Object value, final int row, final int col) {
			data[row][col] = value;
			fireTableCellUpdated(row, col);
		}

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
	// label table column headers. From Sun's JTable tutorial.
	// http://java.sun.com/docs/books/tutorial/uiswing/components/table.html
	//
	private class MyTippedJTable extends JTable {

		/**
		 *
		 */
		private static final long serialVersionUID = -1957448010780941936L;

		MyTippedJTable(final LabelTableModel m) {
			super(m);
		}

		//Implement table header tool tips.
		@Override
		protected JTableHeader createDefaultTableHeader() {
			return new SymbolTableHeader(columnModel);
		}

		// Implement cell tool tips.  All of them are the same (although they could be customized).
		@Override
		public Component prepareRenderer(final TableCellRenderer renderer, final int rowIndex, final int vColIndex) {
			final Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
			if (c instanceof JComponent) {
				final JComponent jc = (JComponent) c;
				jc.setToolTipText("Click on label or address to view it in Text/Data Segment");
			}
			return c;
		}

		/////////////////////////////////////////////////////////////////
		//
		// Customized table header that will both display tool tip when
		// mouse hovers over each column, and also sort the table when
		// mouse is clicked on each column.  The tool tip and sort are
		// customized based on the column under the mouse.

		private class SymbolTableHeader extends JTableHeader {

			/**
			 *
			 */
			private static final long serialVersionUID = -66449043177468213L;

			public SymbolTableHeader(final TableColumnModel cm) {
				super(cm);
				addMouseListener(new SymbolTableHeaderMouseListener());
			}

			@Override
			public String getToolTipText(final MouseEvent e) {
				final Point p = e.getPoint();
				final int index = columnModel.getColumnIndexAtX(p.x);
				final int realIndex = columnModel.getColumn(index).getModelIndex();
				return columnToolTips[realIndex];
			}

			/////////////////////////////////////////////////////////////////////
			// When user clicks on table column header, system will sort the
			// table based on that column then redraw it.
			private class SymbolTableHeaderMouseListener implements MouseListener {

				@Override
				public void mouseClicked(final MouseEvent e) {
					final Point p = e.getPoint();
					final int index = columnModel.getColumnIndexAtX(p.x);
					final int realIndex = columnModel.getColumn(index).getModelIndex();
					sortState = sortStateTransitions[sortState][realIndex];
					tableSortComparator = tableSortingComparators[sortState];
					columnNames = sortColumnHeadings[sortState];
					Globals.getSettings().setLabelSortState(new Integer(sortState).toString());
					setupTable();
					Globals.getGui().getMainPane().getExecutePane().setLabelWindowVisibility(false);
					Globals.getGui().getMainPane().getExecutePane().setLabelWindowVisibility(true);
				}

				@Override
				public void mouseEntered(final MouseEvent e) {}

				@Override
				public void mouseExited(final MouseEvent e) {}

				@Override
				public void mousePressed(final MouseEvent e) {}

				@Override
				public void mouseReleased(final MouseEvent e) {}
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////
	//
	//  Comparator class used to sort in ascending order a List of symbols alphabetically by name
	private class LabelNameAscendingComparator implements java.util.Comparator {

		@Override
		public int compare(final Object a, final Object b) {
			return ((Symbol) a).getName().toLowerCase().compareTo(((Symbol) b).getName().toLowerCase());
		}
	}

	////////////////////////////////////////////////////////////////////////////
	//
	//  Comparator class used to sort in ascending order a List of symbols numerically
	//  by address. The kernel address space is all negative integers, so we need some
	//  special processing to treat int address as unsigned 32 bit value.
	//  Note: Integer.signum() is Java 1.5 and MARS is 1.4 so I can't use it.
	//  Remember, if not equal then any value with correct sign will work.
	//  If both have same sign, a-b will yield correct result.
	//  If signs differ, b will yield correct result (think about it).
	private class LabelAddressAscendingComparator implements java.util.Comparator {

		@Override
		public int compare(final Object a, final Object b) {
			final int addrA = ((Symbol) a).getAddress();
			final int addrB = ((Symbol) b).getAddress();
			return addrA >= 0 && addrB >= 0 || addrA < 0 && addrB < 0 ? addrA - addrB : addrB;
		}
	}

	////////////////////////////////////////////////////////////////////////////
	//
	//  Comparator class used to sort in descending order a List of symbols.  It will
	//  sort either alphabetically by name or numerically by address, depending on the
	//  Comparator object provided as the argument constructor.  This works because it
	//  is implemented by returning the result of the Ascending comparator when
	//  arguments are reversed.
	private class DescendingComparator implements java.util.Comparator {

		private final Comparator opposite;

		private DescendingComparator(final Comparator opposite) {
			this.opposite = opposite;
		}

		@Override
		public int compare(final Object a, final Object b) {
			return opposite.compare(b, a);
		}
	}

}
