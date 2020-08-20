package mars.venus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import mars.util.EditorFont;

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
 * Abstract class for a font selection dialog.
 */
public abstract class AbstractFontSettingDialog extends JDialog {

	/**
	 *
	 */
	private static final long serialVersionUID = 5421164881357311885L;
	JDialog editorDialog;
	JComboBox fontFamilySelector, fontStyleSelector;
	JSlider fontSizeSelector;
	JSpinner fontSizeSpinSelector;
	JLabel fontSample;
	protected Font currentFont;

	// Used to determine upon OK, whether or not anything has changed.
	String initialFontFamily, initialFontStyle, initialFontSize;

	/**
	 * Create a new font chooser. Has pertinent JDialog parameters. Will do
	 * everything except make it visible.
	 */
	public AbstractFontSettingDialog(final Frame owner, final String title, final boolean modality,
			final Font currentFont) {
		super(owner, title, modality);
		this.currentFont = currentFont;
		final JPanel overallPanel = new JPanel(new BorderLayout());
		overallPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		overallPanel.add(buildDialogPanel(), BorderLayout.CENTER);
		overallPanel.add(buildControlPanel(), BorderLayout.SOUTH);
		setContentPane(overallPanel);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent we) {
				closeDialog();
			}
		});
		pack();
		setLocationRelativeTo(owner);
	}

	// The dialog area, not including control buttons at bottom
	protected JPanel buildDialogPanel() {
		final JPanel contents = new JPanel(new BorderLayout(20, 20));
		contents.setBorder(new EmptyBorder(10, 10, 10, 10));

		//Font currentFont = Globals.getSettings().getEditorFont();
		initialFontFamily = currentFont.getFamily();
		initialFontStyle = EditorFont.styleIntToStyleString(currentFont.getStyle());
		initialFontSize = EditorFont.sizeIntToSizeString(currentFont.getSize());
		final String[] commonFontFamilies = EditorFont.getCommonFamilies();
		final String[] allFontFamilies = EditorFont.getAllFamilies();
		// The makeVectorData() method will combine these two into one Vector
		// with a horizontal line separating the two groups.
		final String[][] fullList = { commonFontFamilies, allFontFamilies };

		fontFamilySelector = new JComboBox(makeVectorData(fullList));
		fontFamilySelector.setRenderer(new ComboBoxRenderer());
		fontFamilySelector.addActionListener(new BlockComboListener(fontFamilySelector));
		fontFamilySelector.setSelectedItem(currentFont.getFamily());
		fontFamilySelector.setEditable(false);
		fontFamilySelector.setMaximumRowCount(commonFontFamilies.length);
		fontFamilySelector.setToolTipText("Short list of common font families followed by complete list.");

		final String[] fontStyles = EditorFont.getFontStyleStrings();
		fontStyleSelector = new JComboBox(fontStyles);
		fontStyleSelector.setSelectedItem(EditorFont.styleIntToStyleString(currentFont.getStyle()));
		fontStyleSelector.setEditable(false);
		fontStyleSelector.setToolTipText("List of available font styles.");

		fontSizeSelector = new JSlider(EditorFont.MIN_SIZE, EditorFont.MAX_SIZE, currentFont.getSize());
		fontSizeSelector.setToolTipText("Use slider to select font size from " + EditorFont.MIN_SIZE + " to "
				+ EditorFont.MAX_SIZE + ".");
		fontSizeSelector.addChangeListener(e -> {
			final Integer value = new Integer(((JSlider) e.getSource()).getValue());
			fontSizeSpinSelector.setValue(value);
			fontSample.setFont(getFont());
		});
		final SpinnerNumberModel fontSizeSpinnerModel = new SpinnerNumberModel(currentFont.getSize(),
				EditorFont.MIN_SIZE, EditorFont.MAX_SIZE, 1);
		fontSizeSpinSelector = new JSpinner(fontSizeSpinnerModel);
		fontSizeSpinSelector.setToolTipText("Current font size in points.");
		fontSizeSpinSelector.addChangeListener(e -> {
			final Object value = ((JSpinner) e.getSource()).getValue();
			fontSizeSelector.setValue(((Integer) value));
			fontSample.setFont(getFont());
		});
		// Action listener to update sample when family or style selected
		final ActionListener updateSample = e -> fontSample.setFont(getFont());
		fontFamilySelector.addActionListener(updateSample);
		fontStyleSelector.addActionListener(updateSample);

		final JPanel familyStyleComponents = new JPanel(new GridLayout(2, 2, 4, 4));
		familyStyleComponents.add(new JLabel("Font Family"));
		familyStyleComponents.add(new JLabel("Font Style"));
		familyStyleComponents.add(fontFamilySelector);
		familyStyleComponents.add(fontStyleSelector);

		fontSample = new JLabel("Sample of this font", SwingConstants.CENTER);
		fontSample.setBorder(new LineBorder(Color.BLACK));
		fontSample.setFont(getFont());
		fontSample.setToolTipText("Dynamically updated font sample based on current settings");
		final JPanel sizeComponents = new JPanel();
		sizeComponents.add(new JLabel("Font Size "));
		sizeComponents.add(fontSizeSelector);
		sizeComponents.add(fontSizeSpinSelector);
		final JPanel sizeAndSample = new JPanel(new GridLayout(2, 1, 4, 8));
		sizeAndSample.add(sizeComponents);
		sizeAndSample.add(fontSample);
		contents.add(familyStyleComponents, BorderLayout.NORTH);
		contents.add(sizeAndSample, BorderLayout.CENTER);
		return contents;
	}

	// Build component containing the buttons for dialog control
	// Such as OK, Cancel, Reset, Apply, etc.  These may vary
	// by application
	protected abstract Component buildControlPanel();

	@Override
	public Font getFont() {
		return EditorFont.createFontFromStringValues((String) fontFamilySelector.getSelectedItem(),
				(String) fontStyleSelector.getSelectedItem(), fontSizeSpinSelector.getValue().toString());
	}

	// User has clicked "Apply" or "Apply and Close" button.
	protected void performApply() {
		apply(getFont());
	}

	// We're finished with this modal dialog.
	protected void closeDialog() {
		setVisible(false);
		dispose();
	}

	// Reset font to its initial setting
	protected void reset() {
		fontFamilySelector.setSelectedItem(initialFontFamily);
		fontStyleSelector.setSelectedItem(initialFontStyle);
		fontSizeSelector.setValue(EditorFont.sizeStringToSizeInt(initialFontSize));
		fontSizeSpinSelector.setValue(new Integer(EditorFont.sizeStringToSizeInt(initialFontSize)));
	}

	/**
	 * Apply the given font. Left for the client to define.
	 *
	 * @param font a font to be applied by the client.
	 */
	protected abstract void apply(Font font);

	/////////////////////////////////////////////////////////////////////
	//
	// Method and two classes to permit one or more horizontal separators
	// within a combo box list.  I obtained this code on 13 July 2007
	// from http://www.codeguru.com/java/articles/164.shtml.  Author
	// is listed: Nobuo Tamemasa.  Code is old, 1999, but fine for this.
	// I will use it to separate the short list of "common" font
	// families from the very long list of all font families.  No attempt
	// to keep a list of recently-used fonts like Word does.  The list
	// of common font families is static.
	//
	/////////////////////////////////////////////////////////////////////

	private static String SEPARATOR = "___SEPARATOR____";

	// Given an array of string arrays, will produce a Vector contenating
	// the arrays with a separator between each.
	private Vector makeVectorData(final String[][] str) {
		boolean needSeparator = false;
		final Vector data = new Vector();
		for (int i = 0; i < str.length; i++) {
			if (needSeparator) { data.addElement(SEPARATOR); }
			for (int j = 0; j < str[i].length; j++) {
				data.addElement(str[i][j]);
				needSeparator = true;
			}
		}
		return data;
	}

	// Required renderer for handling the separator bar.
	private class ComboBoxRenderer extends JLabel implements ListCellRenderer {

		/**
		 *
		 */
		private static final long serialVersionUID = 4874595289175340279L;
		JSeparator separator;

		public ComboBoxRenderer() {
			setOpaque(true);
			setBorder(new EmptyBorder(1, 1, 1, 1));
			separator = new JSeparator(SwingConstants.HORIZONTAL);
		}

		@Override
		public Component getListCellRendererComponent(final JList list, final Object value, final int index,
				final boolean isSelected, final boolean cellHasFocus) {
			final String str = value == null ? "" : value.toString();
			if (SEPARATOR.equals(str)) { return separator; }
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			setFont(list.getFont());
			setText(str);
			return this;
		}
	}

	// Required listener to handle the separator bar.
	private class BlockComboListener implements ActionListener {

		JComboBox combo;
		Object currentItem;

		BlockComboListener(final JComboBox combo) {
			this.combo = combo;
			combo.setSelectedIndex(0);
			currentItem = combo.getSelectedItem();
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final String tempItem = (String) combo.getSelectedItem();
			if (SEPARATOR.equals(tempItem)) {
				combo.setSelectedItem(currentItem);
			} else {
				currentItem = tempItem;
			}
		}
	}

}
