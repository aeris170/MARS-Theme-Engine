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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import themeengine.include.com.formdev.flatlaf.json.Json;
import themeengine.include.com.formdev.flatlaf.util.StringUtils;

/**
 * @author Karl Tauber
 */
class IJThemesManager {

	final List<IJThemeInfo> bundledThemes = new ArrayList<>();
	final List<IJThemeInfo> moreThemes = new ArrayList<>();
	private final Map<File, Long> lastModifiedMap = new HashMap<>();

	@SuppressWarnings("unchecked")
	void loadBundledThemes() {
		bundledThemes.clear();

		// load themes.json
		Map<String, Object> json;
		try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("themes.json"),
				StandardCharsets.UTF_8)) {
			json = (Map<String, Object>) Json.parse(reader);
		} catch (final IOException ex) {
			ex.printStackTrace();
			return;
		}

		// add info about bundled themes
		for (final Map.Entry<String, Object> e : json.entrySet()) {
			final String resourceName = e.getKey();
			final Map<String, String> value = (Map<String, String>) e.getValue();
			final String name = value.get("name");
			final boolean dark = Boolean.parseBoolean(value.get("dark"));
			final String license = value.get("license");
			final String licenseFile = value.get("licenseFile");
			final String sourceCodeUrl = value.get("sourceCodeUrl");
			final String sourceCodePath = value.get("sourceCodePath");

			bundledThemes.add(new IJThemeInfo(name, resourceName, dark, license, licenseFile, sourceCodeUrl,
					sourceCodePath, null, null));
		}
	}

	void loadThemesFromDirectory() {
		// get current working directory
		final File directory = new File("").getAbsoluteFile();

		final File[] themeFiles = directory.listFiles((dir, name) -> name.endsWith(".theme.json"));
		if (themeFiles == null) { return; }

		lastModifiedMap.clear();
		lastModifiedMap.put(directory, directory.lastModified());

		moreThemes.clear();
		for (final File f : themeFiles) {
			final String fname = f.getName();
			final String name = fname.endsWith(".properties") ? StringUtils.removeTrailing(fname, ".properties")
					: StringUtils.removeTrailing(fname, ".theme.json");
			moreThemes.add(new IJThemeInfo(name, null, false, null, null, null, null, f, null));
			lastModifiedMap.put(f, f.lastModified());
		}
	}

	boolean hasThemesFromDirectoryChanged() {
		for (final Map.Entry<File, Long> e : lastModifiedMap.entrySet()) {
			if (e.getKey().lastModified() != e.getValue().longValue()) { return true; }
		}
		return false;
	}
}
