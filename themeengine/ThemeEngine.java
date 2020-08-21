package themeengine;

import javax.swing.JDialog;
import javax.swing.JFrame;

import themeengine.include.com.formdev.flatlaf.util.SystemInfo;

public final class ThemeEngine {

	private ThemeEngine() {}

	public static void setup(final String[] args) {
		if (SystemInfo.isMacOS && System.getProperty("apple.laf.useScreenMenuBar") == null) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}
		DemoPrefs.init("mars");
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
		DemoPrefs.initLaf(args);
	}
}
