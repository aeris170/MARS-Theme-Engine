package mars.venus;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import mars.Globals;

/*
 * Copyright (c) 2003-2006, Pete Sanderson and Kenneth Vollmar
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
 * Action class for the Settings menu item for optionally loading a MIPS
 * exception handler.
 */
public class SettingsExceptionHandlerAction extends GuiAction {

	/**
	 *
	 */
	private static final long serialVersionUID = 8321314886544115279L;
	JDialog exceptionHandlerDialog;
	JCheckBox exceptionHandlerSetting;
	JButton exceptionHandlerSelectionButton;
	JTextField exceptionHandlerDisplay;

	boolean initialSelected; // state of check box when dialog initiated.
	String initialPathname;  // selected exception handler when dialog initiated.

	public SettingsExceptionHandlerAction(final String name, final Icon icon, final String descrip,
			final Integer mnemonic, final KeyStroke accel, final VenusUI gui) {
		super(name, icon, descrip, mnemonic, accel, gui);
	}

	// launch dialog for setting and filename specification
	@Override
	public void actionPerformed(final ActionEvent e) {
		initialSelected = Globals.getSettings().getExceptionHandlerEnabled();
		initialPathname = Globals.getSettings().getExceptionHandler();
		exceptionHandlerDialog = new JDialog(Globals.getGui(), "Exception Handler", true);
		exceptionHandlerDialog.setContentPane(buildDialogPanel());
		exceptionHandlerDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		exceptionHandlerDialog.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent we) {
				closeDialog();
			}
		});
		exceptionHandlerDialog.pack();
		exceptionHandlerDialog.setLocationRelativeTo(Globals.getGui());
		exceptionHandlerDialog.setVisible(true);
	}

	// The dialog box that appears when menu item is selected.
	private JPanel buildDialogPanel() {
		final JPanel contents = new JPanel(new BorderLayout(20, 20));
		contents.setBorder(new EmptyBorder(10, 10, 10, 10));
		// Top row - the check box for setting...
		exceptionHandlerSetting = new JCheckBox("Include this exception handler file in all assemble operations");
		exceptionHandlerSetting.setSelected(Globals.getSettings().getExceptionHandlerEnabled());
		exceptionHandlerSetting.addActionListener(new ExceptionHandlerSettingAction());
		contents.add(exceptionHandlerSetting, BorderLayout.NORTH);
		// Middle row - the button and text field for exception handler file selection
		final JPanel specifyHandlerFile = new JPanel();
		exceptionHandlerSelectionButton = new JButton("Browse");
		exceptionHandlerSelectionButton.setEnabled(exceptionHandlerSetting.isSelected());
		exceptionHandlerSelectionButton.addActionListener(new ExceptionHandlerSelectionAction());
		exceptionHandlerDisplay = new JTextField(Globals.getSettings().getExceptionHandler(), 30);
		exceptionHandlerDisplay.setEditable(false);
		exceptionHandlerDisplay.setEnabled(exceptionHandlerSetting.isSelected());
		specifyHandlerFile.add(exceptionHandlerSelectionButton);
		specifyHandlerFile.add(exceptionHandlerDisplay);
		contents.add(specifyHandlerFile, BorderLayout.CENTER);
		// Bottom row - the control buttons for OK and Cancel
		final Box controlPanel = Box.createHorizontalBox();
		final JButton okButton = new JButton("OK");
		okButton.addActionListener(e -> {
			performOK();
			closeDialog();
		});
		final JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> closeDialog());
		controlPanel.add(Box.createHorizontalGlue());
		controlPanel.add(okButton);
		controlPanel.add(Box.createHorizontalGlue());
		controlPanel.add(cancelButton);
		controlPanel.add(Box.createHorizontalGlue());
		contents.add(controlPanel, BorderLayout.SOUTH);
		return contents;
	}

	// User has clicked "OK" button, so record status of the checkbox and text field.
	private void performOK() {
		final boolean finalSelected = exceptionHandlerSetting.isSelected();
		final String finalPathname = exceptionHandlerDisplay.getText();
		// If nothing has changed then don't modify setting variables or properties file.
		if (initialSelected != finalSelected || initialPathname == null && finalPathname != null
				|| initialPathname != null && !initialPathname.equals(finalPathname)) {
			Globals.getSettings().setExceptionHandlerEnabled(finalSelected);
			if (finalSelected) { Globals.getSettings().setExceptionHandler(finalPathname); }
		}
	}

	// We're finished with this modal dialog.
	private void closeDialog() {
		exceptionHandlerDialog.setVisible(false);
		exceptionHandlerDialog.dispose();
	}

	/////////////////////////////////////////////////////////////////////////////////
	// Associated action class: exception handler setting.  Attached to check box.
	private class ExceptionHandlerSettingAction implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {
			final boolean selected = ((JCheckBox) e.getSource()).isSelected();
			exceptionHandlerSelectionButton.setEnabled(selected);
			exceptionHandlerDisplay.setEnabled(selected);
		}
	}

	/////////////////////////////////////////////////////////////////////////////////
	// Associated action class: selecting exception handler file.  Attached to handler selector.
	private class ExceptionHandlerSelectionAction implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {
			final JFileChooser chooser = new JFileChooser();
			String pathname = Globals.getSettings().getExceptionHandler();
			if (pathname != null) {
				final File file = new File(pathname);
				if (file.exists()) { chooser.setSelectedFile(file); }
			}
			final int result = chooser.showOpenDialog(Globals.getGui());
			if (result == JFileChooser.APPROVE_OPTION) {
				pathname = chooser.getSelectedFile().getPath();//.replaceAll("\\\\","/");
				exceptionHandlerDisplay.setText(pathname);
			}
		}
	}

}
