package com.goblinthug.blueprinthud.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.goblinthug.blueprinthud.BlueprintHudMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

public final class ReferenceConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("blueprint-hud.json");
	private static final Path LEGACY_PATH = FabricLoader.getInstance().getConfigDir().resolve("buildref.json");
	public static final int MAX_HISTORY = 40;
	public static final int MAX_IMAGES = 8;

	public static final float MIN_CROP_REMAINING = 0.05f;

	/** Master HUD toggle (H key). */
	public boolean visible = true;
	public String lastBrowsePath = "";
	public List<String> history = new ArrayList<>();
	public List<SavedImage> images = new ArrayList<>();

	// Legacy single-image fields — read for migration only
	public float opacity = 0.55f;
	public float scale = 0.45f;
	public float x = 20;
	public float y = 20;
	public float cropLeft = 0.0f;
	public float cropRight = 0.0f;
	public float cropTop = 0.0f;
	public float cropBottom = 0.0f;
	public String lastImagePath = "";

	public static final class SavedImage {
		public String path = "";
		public float opacity = 0.55f;
		public float scale = 0.45f;
		public float x = 20;
		public float y = 20;
		public float cropLeft = 0.0f;
		public float cropRight = 0.0f;
		public float cropTop = 0.0f;
		public float cropBottom = 0.0f;
	}

	public static ReferenceConfig load() {
		Path source = Files.isRegularFile(PATH) ? PATH : (Files.isRegularFile(LEGACY_PATH) ? LEGACY_PATH : null);
		if (source == null) {
			return new ReferenceConfig();
		}

		try (Reader reader = Files.newBufferedReader(source)) {
			ReferenceConfig config = GSON.fromJson(reader, ReferenceConfig.class);
			if (config == null) {
				return new ReferenceConfig();
			}
			config.migrateLegacy();
			config.normalizeHistory();
			if (source.equals(LEGACY_PATH)) {
				config.save();
				BlueprintHudMod.LOGGER.info("Migrated config from buildref.json to blueprint-hud.json");
			}
			return config;
		} catch (Exception e) {
			BlueprintHudMod.LOGGER.warn("Failed to read config, using defaults", e);
			return new ReferenceConfig();
		}
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			syncLegacyMirror();
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			BlueprintHudMod.LOGGER.warn("Failed to save config", e);
		}
	}

	/** Older builds stored one image in flat fields — fold into {@link #images}. */
	public void migrateLegacy() {
		if (this.images == null) {
			this.images = new ArrayList<>();
		}
		if (!this.images.isEmpty()) {
			return;
		}
		if (this.lastImagePath == null || this.lastImagePath.isBlank()) {
			return;
		}

		SavedImage saved = new SavedImage();
		saved.path = Path.of(this.lastImagePath).toAbsolutePath().normalize().toString();
		saved.opacity = this.opacity;
		saved.scale = this.scale;
		saved.x = this.x;
		saved.y = this.y;
		saved.cropLeft = this.cropLeft;
		saved.cropRight = this.cropRight;
		saved.cropTop = this.cropTop;
		saved.cropBottom = this.cropBottom;
		this.images.add(saved);
	}

	/** Keep flat fields in sync so older tools still see a sensible last path. */
	private void syncLegacyMirror() {
		if (this.images == null) {
			this.images = new ArrayList<>();
		}
		if (this.images.isEmpty()) {
			this.lastImagePath = "";
			return;
		}
		SavedImage first = this.images.getFirst();
		this.lastImagePath = first.path;
		this.opacity = first.opacity;
		this.scale = first.scale;
		this.x = first.x;
		this.y = first.y;
		this.cropLeft = first.cropLeft;
		this.cropRight = first.cropRight;
		this.cropTop = first.cropTop;
		this.cropBottom = first.cropBottom;
	}

	public void normalizeHistory() {
		if (this.history == null) {
			this.history = new ArrayList<>();
		}
		if (this.images == null) {
			this.images = new ArrayList<>();
		}

		List<String> cleaned = new ArrayList<>();
		for (String entry : this.history) {
			if (entry == null || entry.isBlank()) {
				continue;
			}
			String normalized = Path.of(entry).toAbsolutePath().normalize().toString();
			if (!cleaned.contains(normalized)) {
				cleaned.add(normalized);
			}
		}

		for (SavedImage image : this.images) {
			if (image == null || image.path == null || image.path.isBlank()) {
				continue;
			}
			String absolute = Path.of(image.path).toAbsolutePath().normalize().toString();
			image.path = absolute;
			cleaned.remove(absolute);
			cleaned.add(0, absolute);
		}

		if (cleaned.size() > MAX_HISTORY) {
			cleaned = new ArrayList<>(cleaned.subList(0, MAX_HISTORY));
		}

		this.history = cleaned;
	}

	public void rememberPath(Path path) {
		normalizeHistory();
		String absolute = path.toAbsolutePath().normalize().toString();
		this.history.remove(absolute);
		this.history.add(0, absolute);
		while (this.history.size() > MAX_HISTORY) {
			this.history.remove(this.history.size() - 1);
		}
		this.lastImagePath = absolute;
	}

	public boolean removeFromHistory(String path) {
		normalizeHistory();
		String absolute = Path.of(path).toAbsolutePath().normalize().toString();
		return this.history.remove(absolute);
	}

	public int pruneMissingFiles() {
		normalizeHistory();
		int removed = 0;
		Iterator<String> iterator = this.history.iterator();
		while (iterator.hasNext()) {
			String entry = iterator.next();
			if (!Files.isRegularFile(Path.of(entry))) {
				iterator.remove();
				removed++;
			}
		}
		return removed;
	}
}
