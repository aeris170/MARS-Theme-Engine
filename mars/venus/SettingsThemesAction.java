package mars.venus;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import mars.Globals;
import themeengine.DemoPrefs;
import themeengine.IJThemesPanel;

public class SettingsThemesAction extends GuiAction {

	JDialog themesDialog;

	protected SettingsThemesAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel,
			VenusUI gui) {
		super(name, icon, descrip, mnemonic, accel, gui);
	}

	/**
	 * When this action is triggered, launch a dialog to view and modify editor
	 * settings.
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		themesDialog = new JDialog(Globals.getGui(), "Mars Themes", true);
		themesDialog.setContentPane(buildDialogPanel());
		themesDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		themesDialog.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent we) {
				closeDialog();
			}
		});
		themesDialog.pack();
		themesDialog.setLocationRelativeTo(Globals.getGui());
		themesDialog.setVisible(true);
	}

	private Container buildDialogPanel() {
		return new IJThemesPanel(DemoPrefs.getLafState());
	}

	// We're finished with this modal dialog.
	private void closeDialog() {
		themesDialog.setVisible(false);
		themesDialog.dispose();
	}
}
