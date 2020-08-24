/*
 * Copyright 2019 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package themeengine;

import java.io.File;
import java.io.FileInputStream;
import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;

import themeengine.include.com.formdev.flatlaf.FlatLaf;
import themeengine.include.com.formdev.flatlaf.FlatLightLaf;
import themeengine.include.com.formdev.flatlaf.FlatPropertiesLaf;
import themeengine.include.com.formdev.flatlaf.IntelliJTheme;
import themeengine.include.com.formdev.flatlaf.util.StringUtils;

/**
 * @author Karl Tauber
 */
public class DemoPrefs {

	public static final String KEY_LAF = "laf";
	public static final String KEY_LAF_THEME = "lafTheme";

	public static final String RESOURCE_PREFIX = "res:";
	public static final String FILE_PREFIX = "file:";

	public static final String THEME_UI_KEY = "__FlatLaf.demo.theme";

	public static final String LAF_STATE_KEY = "areThemesActive";
	private static final String LAF_INDEX_KEY = "selectedLafIndex";

	private static Preferences state;

	public static Preferences getState() { return state; }

	public static void init(final String rootPath) {
		state = Preferences.userRoot().node(rootPath);
	}

	public static void initLaf(final String[] args) {
		// remember active look and feel
		UIManager.addPropertyChangeListener(e -> {
			if ("lookAndFeel".equals(e.getPropertyName())) {
				state.put(KEY_LAF, UIManager.getLookAndFeel().getClass().getName());
			}
		});

		if (!getLafState()) return;
		// set look and feel

		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
		try {
			if (args.length > 0) {
				UIManager.setLookAndFeel(args[0]);
			} else {
				final String lafClassName = state.get(KEY_LAF, FlatLightLaf.class.getName());
				if (IntelliJTheme.ThemeLaf.class.getName().equals(lafClassName)) {
					final String theme = state.get(KEY_LAF_THEME, "");
					if (theme.startsWith(RESOURCE_PREFIX)) {
						IntelliJTheme.install(IJThemesPanel.class.getResourceAsStream(IJThemesPanel.THEMES_PACKAGE
								+ theme.substring(RESOURCE_PREFIX.length())));
					} else if (theme.startsWith(FILE_PREFIX)) {
						FlatLaf.install(IntelliJTheme.createLaf(new FileInputStream(theme.substring(FILE_PREFIX
								.length()))));
					} else {
						FlatLightLaf.install();
					}

					if (!theme.isEmpty()) { UIManager.getLookAndFeelDefaults().put(THEME_UI_KEY, theme); }
				} else if (FlatPropertiesLaf.class.getName().equals(lafClassName)) {
					final String theme = state.get(KEY_LAF_THEME, "");
					if (theme.startsWith(FILE_PREFIX)) {
						final File themeFile = new File(theme.substring(FILE_PREFIX.length()));
						final String themeName = StringUtils.removeTrailing(themeFile.getName(), ".properties");
						FlatLaf.install(new FlatPropertiesLaf(themeName, themeFile));
					} else {
						FlatLightLaf.install();
					}

					if (!theme.isEmpty()) { UIManager.getLookAndFeelDefaults().put(THEME_UI_KEY, theme); }
				} else {
					UIManager.setLookAndFeel(lafClassName);
				}
			}
		} catch (final Throwable ex) {
			ex.printStackTrace();

			// fallback
			FlatLightLaf.install();
		}
	}

	public static void setLafState(boolean lafState) {
		state.putBoolean(LAF_STATE_KEY, lafState);
	}

	public static boolean getLafState() { return state.getBoolean(LAF_STATE_KEY, false); }

	public static void setSelectedLafIndex(int selectedIndex) {
		state.putInt(LAF_INDEX_KEY, selectedIndex);
	}

	public static int getSelectedLafIndex() { return state.getInt(LAF_INDEX_KEY, 0); }

}
