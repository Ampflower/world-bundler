/* Copyright 2022 Ampflower
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package gay.ampflower.bundler.data.ini;// Created 2022-14-07T06:56:10

import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Ampflower
 * @since ${version}
 **/
@ApiStatus.Experimental
public final class Ini {
	private static final Logger logger = LoggerFactory.getLogger(Ini.class);

	// LinkedHashMap helps preserve
	private final Map<String, Map<String, String>> map = new LinkedHashMap<>();

	public Ini() {
	}

	public static Ini read(Path path, Charset charset) throws IOException {
		try (final var reader = Files.newBufferedReader(path, charset)) {
			return read(reader);
		}
	}

	public static Ini read(File file, Charset charset) throws IOException {
		try (final var reader = new FileReader(file, charset); final var bufferedReader = new BufferedReader(reader)) {
			return read(bufferedReader);
		}
	}

	public static Ini read(Reader reader) throws IOException {
		if (reader instanceof LineNumberReader lineNumberReader) {
			return read(lineNumberReader);
		}
		try (final var lineNumberReader = new LineNumberReader(reader)) {
			return read(lineNumberReader);
		}
	}

	public static Ini read(LineNumberReader reader) throws IOException {
		final var ini = new Ini();
		read(reader, ini::put);
		return ini;
	}

	public static void read(LineNumberReader reader, IniStream stream) throws IOException {
		String section = null, line;
		boolean skipSection = !stream.shouldReadSection(null);
		logger.debug("Skipping section: {}", skipSection);
		while ((line = reader.readLine()) != null) {
			// Skip comments & blank lines
			if (line.startsWith(";") || line.isBlank()) {
				continue;
			}
			// Parse out sections.
			if (line.startsWith("[") && line.endsWith("]")) {
				skipSection = !stream.shouldReadSection(section = line.substring(1, line.length() - 1));
				logger.debug("Skipping section {}: {}", section, skipSection);
			} else if (!skipSection) {
				// Parse out each entry.
				int equalIndex = line.indexOf('=');
//				int colonIndex = line.indexOf(':');
//				if (equalIndex < 0 || (colonIndex >= 0 && colonIndex < equalIndex)) {
//					equalIndex = colonIndex;
//				}
				if (equalIndex < 0) {
					logger.warn("Unable to parse line @ {}: {} in section {}", reader.getLineNumber(), line, section);
					continue;
				}
				stream.ofEntry(section, line.substring(0, equalIndex), line.substring(equalIndex + 1));
			}
		}
	}

	public Map<String, String> getSection(final @Nullable String section) {
		return this.map.get(section);
	}

	public Map<String, String> getOrCreateSection(final @Nullable String section) {
		return this.map.computeIfAbsent(section, k -> new LinkedHashMap<>());
	}

	public @Nullable String put(
		final @Nullable String section,
		final @Nonnull String key,
		final @Nullable String value
	) {
		final var map = this.getOrCreateSection(section);

		return map.put(key, value);
	}

	public @Nullable String get(final @Nullable String section, final @Nonnull String key) {
		final var map = this.getSection(section);

		if (map == null) {
			return null;
		}

		return map.get(key);
	}
}
