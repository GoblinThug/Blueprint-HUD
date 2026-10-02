package com.goblinthug.blueprinthud.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.goblinthug.blueprinthud.BlueprintHudMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ReferenceImageManager {
	public static final ReferenceImageManager INSTANCE = new ReferenceImageManager();

	private final ReferenceConfig config = ReferenceConfig.load();
	private final List<ReferenceImage> images = new ArrayList<>();
	private int selectedIndex = -1;

	private ReferenceImageManager() {
	}

	public ReferenceConfig config() {
		return config;
	}

	public List<ReferenceImage> images() {
		return images;
	}

	public boolean hasImage() {
		return !images.isEmpty();
	}

	public int imageCount() {
		return images.size();
	}

	public ReferenceImage selected() {
		if (selectedIndex < 0 || selectedIndex >= images.size()) {
			return null;
		}
		return images.get(selectedIndex);
	}

	public int selectedIndex() {
		return selectedIndex;
	}

	public void select(int index) {
		if (images.isEmpty()) {
			selectedIndex = -1;
			return;
		}
		selectedIndex = Math.clamp(index, 0, images.size() - 1);
	}

	public void selectNext() {
		if (images.isEmpty()) {
			return;
		}
		select((selectedIndex + 1) % images.size());
	}

	public void selectPrevious() {
		if (images.isEmpty()) {
			return;
		}
		select((selectedIndex - 1 + images.size()) % images.size());
	}

	public void selectImage(ReferenceImage image) {
		int index = images.indexOf(image);
		if (index >= 0) {
			selectedIndex = index;
		}
	}

	/** Topmost image under the cursor (later list entries draw above earlier ones). */
	public ReferenceImage findImageAt(double mouseX, double mouseY) {
		for (int i = images.size() - 1; i >= 0; i--) {
			ReferenceImage image = images.get(i);
			if (image.contains(mouseX, mouseY)) {
				return image;
			}
		}
		return null;
	}

	public void bringToFront(ReferenceImage image) {
		int index = images.indexOf(image);
		if (index < 0 || index == images.size() - 1) {
			if (index >= 0) {
				selectedIndex = index;
			}
			return;
		}
		images.remove(index);
		images.add(image);
		selectedIndex = images.size() - 1;
		persistImages();
	}

	public void openFilePicker(Screen parent) {
		Minecraft.getInstance().gui.setScreen(new FileBrowserScreen(parent));
	}

	public void tryReloadSavedImages() {
		config.migrateLegacy();
		images.clear();
		selectedIndex = -1;

		if (config.images == null || config.images.isEmpty()) {
			return;
		}

		List<ReferenceConfig.SavedImage> savedList = new ArrayList<>(config.images);
		for (ReferenceConfig.SavedImage saved : savedList) {
			if (saved == null || saved.path == null || saved.path.isBlank()) {
				continue;
			}
			Path path = Path.of(saved.path);
			if (!Files.isRegularFile(path)) {
				BlueprintHudMod.LOGGER.warn("Skipping missing reference image: {}", path);
				continue;
			}
			try {
				ReferenceImage image = new ReferenceImage();
				image.copyTransformFrom(saved);
				image.loadFromPath(path, true);
				images.add(image);
			} catch (Exception e) {
				BlueprintHudMod.LOGGER.warn("Could not reload reference image: {}", path, e);
			}
		}

		if (!images.isEmpty()) {
			selectedIndex = images.size() - 1;
		}
		persistImages();
	}

	/**
	 * Adds a new layer, or refreshes an existing one with the same path.
	 */
	public synchronized void loadImage(Path path) throws IOException {
		String absolute = path.toAbsolutePath().normalize().toString();

		for (int i = 0; i < images.size(); i++) {
			ReferenceImage existing = images.get(i);
			if (absolute.equals(existing.path())) {
				existing.loadFromPath(path, true);
				bringToFront(existing);
				config.visible = true;
				config.rememberPath(path);
				persistImages();
				return;
			}
		}

		if (images.size() >= ReferenceConfig.MAX_IMAGES) {
			throw new IOException("Maximum reference images reached (" + ReferenceConfig.MAX_IMAGES + ")");
		}

		ReferenceImage image = new ReferenceImage();
		int offset = images.size() * 28;
		image.x = 20 + offset;
		image.y = 20 + offset;
		image.loadFromPath(path, false);

		images.add(image);
		selectedIndex = images.size() - 1;
		config.visible = true;
		config.rememberPath(path);
		persistImages();
	}

	public synchronized void removeSelected() {
		if (selectedIndex < 0 || selectedIndex >= images.size()) {
			return;
		}
		ReferenceImage removed = images.remove(selectedIndex);
		removed.clearTexture();
		if (images.isEmpty()) {
			selectedIndex = -1;
		} else {
			selectedIndex = Math.min(selectedIndex, images.size() - 1);
		}
		persistImages();
	}

	public synchronized void clearAll() {
		for (ReferenceImage image : images) {
			image.clearTexture();
		}
		images.clear();
		selectedIndex = -1;
		persistImages();
	}

	public void persistImages() {
		List<ReferenceConfig.SavedImage> saved = new ArrayList<>();
		for (ReferenceImage image : images) {
			saved.add(image.toSaved());
		}
		config.images = saved;
		config.save();
	}
}
