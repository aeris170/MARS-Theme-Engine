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

import java.awt.Component;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javax.swing.AbstractListModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ListSelectionEvent;

import themeengine.include.com.formdev.flatlaf.FlatDarculaLaf;
import themeengine.include.com.formdev.flatlaf.FlatDarkLaf;
import themeengine.include.com.formdev.flatlaf.FlatIntelliJLaf;
import themeengine.include.com.formdev.flatlaf.FlatLaf;
import themeengine.include.com.formdev.flatlaf.FlatLightLaf;
import themeengine.include.com.formdev.flatlaf.FlatPropertiesLaf;
import themeengine.include.com.formdev.flatlaf.IntelliJTheme;
import themeengine.include.com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import themeengine.include.com.formdev.flatlaf.util.StringUtils;
import themeengine.include.net.miginfocom.swing.MigLayout;

/**
 * @author Karl Tauber
 */
public class IJThemesPanel extends JPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1015932645086976264L;

	public static final String THEMES_PACKAGE = "include/com/formdev/flatlaf/intellijthemes/themes/";

	private final IJThemesManager themesManager = new IJThemesManager();
	private final List<IJThemeInfo> themes = new ArrayList<>();
	private final HashMap<Integer, String> categories = new HashMap<>();
	private final PropertyChangeListener lafListener = this::lafChanged;
	private final WindowListener windowListener = new WindowAdapter() {

		@Override
		public void windowActivated(final WindowEvent e) {
			IJThemesPanel.this.windowActivated();
		}
	};
	private Window window;

	private File lastDirectory;
	private boolean isAdjustingThemesList;
	private boolean areThemesEnabled;

	public IJThemesPanel(boolean areThemesEnabled) {
		initComponents();
		setThemesEnabled(areThemesEnabled);
		enableThemesCheckBox.addItemListener(e -> checkBoxChanged(e)); //special treatment for this guy.

		// create renderer
		themesList.setCellRenderer(new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				final String title = categories.get(index);
				String name = ((IJThemeInfo) value).name;
				final int sep = name.indexOf('/');
				if (sep >= 0) { name = name.substring(sep + 1).trim(); }

				final JComponent c = (JComponent) super.getListCellRendererComponent(list, name, index, isSelected,
						cellHasFocus);
				c.setToolTipText(buildToolTip((IJThemeInfo) value));
				if (title != null) {
					c.setBorder(new CompoundBorder(new ListCellTitledBorder(themesList, title), c.getBorder()));
				}
				return c;
			}

			private String buildToolTip(final IJThemeInfo ti) {
				if (ti.themeFile != null) { return ti.themeFile.getPath(); }
				if (ti.resourceName == null) { return ti.name; }

				return "Name: " + ti.name + "\nLicense: " + ti.license + "\nSource Code: " + ti.sourceCodeUrl;
			}
		});

		updateThemesList();
	}

	private void updateThemesList() {
		final int filterLightDark = filterComboBox.getSelectedIndex();
		final boolean showLight = filterLightDark != 2;
		final boolean showDark = filterLightDark != 1;

		// load theme infos
		themesManager.loadBundledThemes();
		themesManager.loadThemesFromDirectory();

		// sort themes by name
		final Comparator<? super IJThemeInfo> comparator = (t1, t2) -> t1.name.compareToIgnoreCase(t2.name);
		themesManager.bundledThemes.sort(comparator);
		themesManager.moreThemes.sort(comparator);

		// remember selection (must be invoked before clearing themes field)
		IJThemeInfo oldSel = themesList.getSelectedValue();

		themes.clear();
		categories.clear();

		// add core themes at beginning
		categories.put(themes.size(), "Core Themes");
		if (showLight) {
			themes.add(new IJThemeInfo("Flat Light", null, false, null, null, null, null, null, FlatLightLaf.class
					.getName()));
		}
		if (showDark) {
			themes.add(new IJThemeInfo("Flat Dark", null, true, null, null, null, null, null, FlatDarkLaf.class
					.getName()));
		}
		if (showLight) {
			themes.add(new IJThemeInfo("Flat IntelliJ", null, false, null, null, null, null, null, FlatIntelliJLaf.class
					.getName()));
		}
		if (showDark) {
			themes.add(new IJThemeInfo("Flat Darcula", null, true, null, null, null, null, null, FlatDarculaLaf.class
					.getName()));
		}

		// add themes from directory
		categories.put(themes.size(), "Current Directory");
		themes.addAll(themesManager.moreThemes);

		// add uncategorized bundled themes
		categories.put(themes.size(), "IntelliJ Themes");
		for (final IJThemeInfo ti : themesManager.bundledThemes) {
			final boolean show = showLight && !ti.dark || showDark && ti.dark;
			if (show && !ti.name.contains("/")) { themes.add(ti); }
		}

		// add categorized bundled themes
		String lastCategory = null;
		for (final IJThemeInfo ti : themesManager.bundledThemes) {
			final boolean show = showLight && !ti.dark || showDark && ti.dark;
			final int sep = ti.name.indexOf('/');
			if (!show || sep < 0) { continue; }

			final String category = ti.name.substring(0, sep).trim();
			if (!Objects.equals(lastCategory, category)) {
				lastCategory = category;
				categories.put(themes.size(), category);
			}

			themes.add(ti);
		}

		// fill themes list
		themesList.setModel(new AbstractListModel<IJThemeInfo>() {

			@Override
			public int getSize() { return themes.size(); }

			@Override
			public IJThemeInfo getElementAt(final int index) {
				return themes.get(index);
			}
		});

		// restore selection
		if (oldSel != null) {
			for (int i = 0; i < themes.size(); i++) {
				final IJThemeInfo theme = themes.get(i);
				if (oldSel.name.equals(theme.name) && Objects.equals(oldSel.resourceName, theme.resourceName) && Objects
						.equals(oldSel.themeFile, theme.themeFile) && Objects.equals(oldSel.lafClassName,
								theme.lafClassName)) {
					themesList.setSelectedIndex(i);
					break;
				}
			}
		} else {
			if (UIManager.getLookAndFeel() instanceof FlatLaf) {
				themesList.setSelectedIndex(DemoPrefs.getSelectedLafIndex());
			}
		}

		// select first theme if none selected
		if (themesList.getSelectedIndex() < 0) { themesList.setSelectedIndex(0); }

		// scroll selection into visible area
		final int sel = themesList.getSelectedIndex();
		if (sel >= 0) {
			final Rectangle bounds = themesList.getCellBounds(sel, sel);
			if (bounds != null) { themesList.scrollRectToVisible(bounds); }
		}
	}

	private void themesListValueChanged(final ListSelectionEvent e) {
		final IJThemeInfo themeInfo = themesList.getSelectedValue();
		DemoPrefs.setSelectedLafIndex(themesList.getSelectedIndex());
		if (e.getValueIsAdjusting() || isAdjustingThemesList || !areThemesEnabled) { return; }

		EventQueue.invokeLater(() -> { setTheme(themeInfo); });
	}

	private void setTheme(final IJThemeInfo themeInfo) {
		if (themeInfo == null) { return; }

		// change look and feel
		if (themeInfo.lafClassName != null) {
			if (themeInfo.lafClassName.equals(UIManager.getLookAndFeel().getClass().getName())) { return; }

			FlatAnimatedLafChange.showSnapshot();

			try {
				UIManager.setLookAndFeel(themeInfo.lafClassName);
			} catch (final Exception ex) {
				ex.printStackTrace();
				showInformationDialog("Failed to create '" + themeInfo.lafClassName + "'.", ex);
			}
		} else if (themeInfo.themeFile != null) {
			FlatAnimatedLafChange.showSnapshot();
			//cannot test, if errs, can't do anything :(
			try {
				if (themeInfo.themeFile.getName().endsWith(".properties")) {
					FlatLaf.install(new FlatPropertiesLaf(themeInfo.name, themeInfo.themeFile));
				} else {
					FlatLaf.install(IntelliJTheme.createLaf(new FileInputStream(themeInfo.themeFile)));
				}

				DemoPrefs.getState().put(DemoPrefs.KEY_LAF_THEME, DemoPrefs.FILE_PREFIX + themeInfo.themeFile);
			} catch (final Exception ex) {
				ex.printStackTrace();
				showInformationDialog("Failed to load '" + themeInfo.themeFile + "'.", ex);
			}
		} else {
			FlatAnimatedLafChange.showSnapshot();

			IntelliJTheme.install(getClass().getResourceAsStream(THEMES_PACKAGE + themeInfo.resourceName));
			DemoPrefs.getState().put(DemoPrefs.KEY_LAF_THEME, DemoPrefs.RESOURCE_PREFIX + themeInfo.resourceName);
		}

		// update all components
		FlatLaf.updateUI();
		FlatAnimatedLafChange.hideSnapshotWithAnimation();
	}

	private void saveTheme() {
		final IJThemeInfo themeInfo = themesList.getSelectedValue();
		if (themeInfo == null || themeInfo.resourceName == null) { return; }

		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setSelectedFile(new File(lastDirectory, themeInfo.resourceName));
		if (fileChooser.showSaveDialog(SwingUtilities.windowForComponent(this)) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		final File file = fileChooser.getSelectedFile();
		lastDirectory = file.getParentFile();

		// save theme
		try {
			Files.copy(getClass().getResourceAsStream(THEMES_PACKAGE + themeInfo.resourceName), file.toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (final IOException ex) {
			showInformationDialog("Failed to save theme to '" + file + "'.", ex);
			return;
		}

		// save license
		if (themeInfo.licenseFile != null) {
			try {
				final File licenseFile = new File(file.getParentFile(), StringUtils.removeTrailing(file.getName(),
						".theme.json") + themeInfo.licenseFile.substring(themeInfo.licenseFile.indexOf('.')));
				Files.copy(getClass().getResourceAsStream(THEMES_PACKAGE + themeInfo.licenseFile), licenseFile.toPath(),
						StandardCopyOption.REPLACE_EXISTING);
			} catch (final IOException ex) {
				showInformationDialog("Failed to save theme license to '" + file + "'.", ex);
				return;
			}
		}
	}

	private void browseSourceCode() {
		final IJThemeInfo themeInfo = themesList.getSelectedValue();
		if (themeInfo == null || themeInfo.resourceName == null) { return; }

		final String themeUrl = (themeInfo.sourceCodeUrl + '/' + themeInfo.sourceCodePath).replace(" ", "%20");
		try {
			Desktop.getDesktop().browse(new URI(themeUrl));
		} catch (IOException | URISyntaxException ex) {
			showInformationDialog("Failed to browse '" + themeUrl + "'.", ex);
		}
	}

	private void showInformationDialog(final String message, final Exception ex) {
		JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(this), message + "\n\n" + ex.getMessage(),
				"FlatLaf", JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public void addNotify() {
		super.addNotify();

		selectedCurrentLookAndFeel();
		UIManager.addPropertyChangeListener(lafListener);

		window = SwingUtilities.windowForComponent(this);
		if (window != null) { window.addWindowListener(windowListener); }
	}

	@Override
	public void removeNotify() {
		super.removeNotify();

		UIManager.removePropertyChangeListener(lafListener);

		if (window != null) {
			window.removeWindowListener(windowListener);
			window = null;
		}
	}

	private void lafChanged(final PropertyChangeEvent e) {
		if ("lookAndFeel".equals(e.getPropertyName())) { selectedCurrentLookAndFeel(); }
	}

	private void windowActivated() {
		// refresh themes list on window activation
		if (themesManager.hasThemesFromDirectoryChanged()) { updateThemesList(); }
	}

	private void selectedCurrentLookAndFeel() {
		final LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
		final String theme = UIManager.getLookAndFeelDefaults().getString(DemoPrefs.THEME_UI_KEY);

		if (theme == null && (lookAndFeel instanceof IntelliJTheme.ThemeLaf
				|| lookAndFeel instanceof FlatPropertiesLaf)) {
			return;
		}

		Predicate<IJThemeInfo> test;
		if (theme != null && theme.startsWith(DemoPrefs.RESOURCE_PREFIX)) {
			final String resourceName = theme.substring(DemoPrefs.RESOURCE_PREFIX.length());
			test = ti -> Objects.equals(ti.resourceName, resourceName);
		} else if (theme != null && theme.startsWith(DemoPrefs.FILE_PREFIX)) {
			final File themeFile = new File(theme.substring(DemoPrefs.FILE_PREFIX.length()));
			test = ti -> Objects.equals(ti.themeFile, themeFile);
		} else {
			final String lafClassName = lookAndFeel.getClass().getName();
			test = ti -> Objects.equals(ti.lafClassName, lafClassName);
		}

		int newSel = -1;
		for (int i = 0; i < themes.size(); i++) {
			if (test.test(themes.get(i))) {
				newSel = i;
				break;
			}
		}

		isAdjustingThemesList = true;
		if (newSel >= 0) {
			if (newSel != themesList.getSelectedIndex()) { themesList.setSelectedIndex(newSel); }
		} else {
			themesList.clearSelection();
		}
		isAdjustingThemesList = false;
	}

	private void checkBoxChanged(ItemEvent e) {
		boolean newState = e.getStateChange() == ItemEvent.SELECTED;
		setThemesEnabled(newState);
		DemoPrefs.setLafState(newState);
		if (newState) {
			Object[] options = { "I understand" };
			JOptionPane.showOptionDialog(this,
					"Due to how Java's L&F system works, title bars will not be themed until MARS is restarted.",
					"MARS Theme Engine", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options,
					options[0]);
		}
	}

	private void filterChanged() {
		updateThemesList();
	}

	private void setThemesEnabled(boolean state) {
		areThemesEnabled = state;
		enableThemesCheckBox.setSelected(state);
		filterComboBox.setEnabled(state);
		themesList.setEnabled(state);
		if (state) { //apply theme
			updateThemesList(); //i'm a simple man, i'm lazy
		} else { //apply java default laf
			try {
				UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException ex) {
				ex.printStackTrace();
			}

			// update all components
			FlatLaf.updateUI();
			FlatAnimatedLafChange.hideSnapshotWithAnimation();
		}
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		final JLabel themesLabel = new JLabel();
		enableThemesCheckBox = new JCheckBox("Enable Themes");
		filterComboBox = new JComboBox<>();
		themesScrollPane = new JScrollPane();
		themesList = new JList<>();

		//======== this ========
		setLayout(new MigLayout("insets dialog,hidemode 3",
				// columns
				"[grow,fill]",
				// rows
				"[]3" + "[grow,fill]"));

		//---- enableThemesCheckBox ----
		enableThemesCheckBox.setFocusable(false);
		add(enableThemesCheckBox, "cell 0 0");

		//---- themesLabel ----
		themesLabel.setText("Themes:");
		add(themesLabel, "cell 0 1");

		//---- filterComboBox ----
		filterComboBox.setModel(new DefaultComboBoxModel<>(new String[] { "all", "light", "dark" }));
		filterComboBox.putClientProperty("JComponent.minimumWidth", 0);
		filterComboBox.setFocusable(false);
		filterComboBox.addActionListener(e -> filterChanged());
		add(filterComboBox, "cell 1 0,alignx right,growx 0");

		//======== themesScrollPane ========
		{

			//---- themesList ----
			themesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			themesList.addListSelectionListener(this::themesListValueChanged);
			themesScrollPane.setViewportView(themesList);
		}
		add(themesScrollPane, "cell 0 2,spanx 2");
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JCheckBox enableThemesCheckBox;
	private JComboBox<String> filterComboBox;
	private JScrollPane themesScrollPane;
	private JList<IJThemeInfo> themesList;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
