package com.goblinthug.blueprinthud.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * In-game file explorer: walk folders like a normal file manager, or paste a full path.
 */
public class FileBrowserScreen extends Screen {
	private static final int PAGE_SIZE = 8;

	private final Screen parent;
	private Path currentDir;
	private int page;
	private final List<Entry> entries = new ArrayList<>();
	private String statusMessage = "";
	private boolean statusError;
	private String pathDraft = "";
	private EditBox pathBox;

	private record Entry(Path path, boolean directory, boolean parentLink, String label) {
	}

	public FileBrowserScreen(Screen parent) {
		super(Component.translatable("screen.blueprint-hud.browser.title"));
		this.parent = parent;
		this.currentDir = resolveStartDirectory();
		this.pathDraft = this.currentDir != null ? this.currentDir.toString() : "";
	}

	private static Path resolveStartDirectory() {
		ReferenceConfig config = ReferenceImageManager.INSTANCE.config();
		if (config.lastBrowsePath != null && !config.lastBrowsePath.isBlank()) {
			Path browse = Path.of(config.lastBrowsePath);
			if (Files.isDirectory(browse)) {
				return browse.toAbsolutePath().normalize();
			}
			if (Files.isRegularFile(browse) && browse.getParent() != null) {
				return browse.getParent().toAbsolutePath().normalize();
			}
		}
		if (config.lastImagePath != null && !config.lastImagePath.isBlank()) {
			Path last = Path.of(config.lastImagePath);
			if (last.getParent() != null && Files.isDirectory(last.getParent())) {
				return last.getParent().toAbsolutePath().normalize();
			}
		}

		Path pictures = Path.of(System.getProperty("user.home"), "Pictures");
		if (Files.isDirectory(pictures)) {
			return pictures.toAbsolutePath().normalize();
		}

		Path desktop = Path.of(System.getProperty("user.home"), "Desktop");
		if (Files.isDirectory(desktop)) {
			return desktop.toAbsolutePath().normalize();
		}

		return Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
	}

	@Override
	protected void init() {
		refreshEntries();

		int centerX = this.width / 2;
		int listWidth = Math.min(380, this.width - 40);
		int left = centerX - listWidth / 2;

		// Optional: paste full path
		this.pathBox = new EditBox(this.font, left, 36, listWidth - 74, 18, Component.translatable("screen.blueprint-hud.browser.path"));
		this.pathBox.setMaxLength(2048);
		this.pathBox.setHint(Component.translatable("screen.blueprint-hud.browser.path_hint"));
		this.pathBox.setValue(this.pathDraft);
		this.pathBox.setResponder(value -> this.pathDraft = value);
		this.addRenderableWidget(this.pathBox);

		this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.browser.open_path"), b -> openPathInput())
				.bounds(left + listWidth - 70, 36, 70, 18)
				.build());

		// Shortcuts for common places + all drives
		int sy = 58;
		int gap = 4;
		int bw = (listWidth - gap * 4) / 5;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.browser.up"), b -> goUp())
				.bounds(left, sy, bw, 18).build());
		this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.browser.drives"), b -> showDrives())
				.bounds(left + (bw + gap), sy, bw, 18).build());
		this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.browser.home"), b -> goTo(Path.of(System.getProperty("user.home"))))
				.bounds(left + (bw + gap) * 2, sy, bw, 18).build());
		this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.browser.desktop"), b -> goTo(Path.of(System.getProperty("user.home"), "Desktop")))
				.bounds(left + (bw + gap) * 3, sy, bw, 18).build());
		this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.browser.pictures"), b -> goTo(Path.of(System.getProperty("user.home"), "Pictures")))
				.bounds(left + (bw + gap) * 4, sy, bw, 18).build());

		int top = 82;

		if (!this.entries.isEmpty()) {
			int totalPages = Math.max(1, (this.entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
			this.page = Math.min(Math.max(this.page, 0), totalPages - 1);
			int start = this.page * PAGE_SIZE;
			int end = Math.min(this.entries.size(), start + PAGE_SIZE);

			for (int i = start; i < end; i++) {
				Entry entry = this.entries.get(i);
				int row = i - start;
				int y = top + row * 22;

				Component label;
				if (entry.parentLink()) {
					label = Component.translatable("screen.blueprint-hud.browser.parent");
				} else if (entry.directory()) {
					label = Component.translatable("screen.blueprint-hud.browser.folder", truncate(entry.label(), listWidth - 28));
				} else {
					label = Component.translatable("screen.blueprint-hud.browser.file", truncate(entry.label(), listWidth - 28));
				}

				this.addRenderableWidget(Button.builder(label, b -> onEntryClick(entry))
						.bounds(left, y, listWidth, 20)
						.build());
			}

			int navY = top + PAGE_SIZE * 22 + 4;
			if (this.page > 0) {
				this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.history.prev"), b -> {
					this.page--;
					this.rebuildWidgets();
				}).bounds(left, navY, 100, 20).build());
			}
			if (this.page < totalPages - 1) {
				this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.history.next"), b -> {
					this.page++;
					this.rebuildWidgets();
				}).bounds(left + listWidth - 100, navY, 100, 20).build());
			}
		}

		this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose())
				.bounds(centerX - 100, this.height - 28, 200, 20)
				.build());
	}

	private void onEntryClick(Entry entry) {
		if (entry.parentLink()) {
			goUp();
			return;
		}
		if (entry.directory()) {
			goTo(entry.path());
			return;
		}
		selectFile(entry.path());
	}

	private void openPathInput() {
		String raw = this.pathDraft == null ? "" : this.pathDraft.trim();
		if (raw.isEmpty() && this.pathBox != null) {
			raw = this.pathBox.getValue().trim();
		}
		if (raw.isEmpty()) {
			setStatus(Component.translatable("screen.blueprint-hud.browser.path_empty").getString(), true);
			return;
		}

		if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
			raw = raw.substring(1, raw.length() - 1);
		}

		Path path;
		try {
			path = Path.of(raw).toAbsolutePath().normalize();
		} catch (Exception e) {
			setStatus(Component.translatable("screen.blueprint-hud.browser.path_invalid").getString(), true);
			return;
		}

		if (Files.isRegularFile(path)) {
			if (!isImageFile(path)) {
				setStatus(Component.translatable("screen.blueprint-hud.browser.not_image").getString(), true);
				return;
			}
			selectFile(path);
			return;
		}

		if (Files.isDirectory(path)) {
			goTo(path);
			return;
		}

		setStatus(Component.translatable("screen.blueprint-hud.browser.path_missing").getString(), true);
	}

	private void setStatus(String message, boolean error) {
		this.statusMessage = message;
		this.statusError = error;
	}

	private void refreshEntries() {
		this.entries.clear();

		try {
			if (this.currentDir == null) {
				for (File root : File.listRoots()) {
					Path rootPath = root.toPath();
					this.entries.add(new Entry(rootPath, true, false, rootPath.toString()));
				}
			} else if (Files.isDirectory(this.currentDir)) {
				// Always offer going up one level (like ".." in Explorer)
				this.entries.add(new Entry(this.currentDir, true, true, ".."));

				try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.currentDir)) {
					for (Path path : stream) {
						try {
							if (Files.isHidden(path)) {
								continue;
							}
						} catch (IOException ignored) {
							// keep
						}
						if (Files.isDirectory(path)) {
							String name = path.getFileName() != null ? path.getFileName().toString() : path.toString();
							this.entries.add(new Entry(path, true, false, name));
						} else if (isImageFile(path)) {
							this.entries.add(new Entry(path, false, false, path.getFileName().toString()));
						}
					}
				}
			} else {
				this.currentDir = Path.of(System.getProperty("user.home"));
				this.pathDraft = this.currentDir.toString();
				refreshEntries();
				return;
			}
		} catch (Exception e) {
			setStatus(e.getMessage() != null ? e.getMessage() : "error", true);
			this.entries.clear();
			return;
		}

		// Keep ".." first, then folders, then files
		this.entries.sort(Comparator
				.comparing((Entry e) -> !e.parentLink())
				.thenComparing(e -> !e.directory())
				.thenComparing(e -> e.label().toLowerCase(Locale.ROOT)));
	}

	private static boolean isImageFile(Path path) {
		if (!Files.isRegularFile(path)) {
			return false;
		}
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		return name.endsWith(".png")
				|| name.endsWith(".jpg")
				|| name.endsWith(".jpeg")
				|| name.endsWith(".bmp")
				|| name.endsWith(".gif")
				|| name.endsWith(".webp");
	}

	private void showDrives() {
		this.currentDir = null;
		this.page = 0;
		this.pathDraft = "";
		this.statusMessage = "";
		this.rebuildWidgets();
	}

	private void goTo(Path path) {
		if (path == null) {
			return;
		}
		Path target = path.toAbsolutePath().normalize();
		if (!Files.isDirectory(target)) {
			setStatus(Component.translatable("screen.blueprint-hud.browser.path_missing").getString(), true);
			return;
		}
		this.currentDir = target;
		this.pathDraft = target.toString();
		this.page = 0;
		this.statusMessage = "";
		rememberBrowseDir();
		this.rebuildWidgets();
	}

	private void goUp() {
		if (this.currentDir == null) {
			return;
		}
		Path parentPath = this.currentDir.getParent();
		if (parentPath == null) {
			showDrives();
			return;
		}
		goTo(parentPath);
	}

	private void rememberBrowseDir() {
		ReferenceConfig config = ReferenceImageManager.INSTANCE.config();
		if (this.currentDir != null) {
			config.lastBrowsePath = this.currentDir.toAbsolutePath().normalize().toString();
			config.save();
		}
	}

	private void selectFile(Path path) {
		try {
			ReferenceImageManager manager = ReferenceImageManager.INSTANCE;
			boolean alreadyLoaded = manager.images().stream()
					.anyMatch(image -> path.toAbsolutePath().normalize().toString().equals(image.path()));
			if (!alreadyLoaded && manager.imageCount() >= ReferenceConfig.MAX_IMAGES) {
				setStatus(Component.translatable("message.blueprint-hud.max_images", ReferenceConfig.MAX_IMAGES).getString(), true);
				if (this.minecraft.player != null) {
					this.minecraft.player.sendOverlayMessage(Component.translatable("message.blueprint-hud.max_images", ReferenceConfig.MAX_IMAGES));
				}
				return;
			}

			manager.loadImage(path);
			ReferenceConfig config = manager.config();
			if (path.getParent() != null) {
				config.lastBrowsePath = path.getParent().toAbsolutePath().normalize().toString();
				config.save();
			}
			if (this.minecraft.player != null) {
				this.minecraft.player.sendOverlayMessage(Component.translatable(
						alreadyLoaded ? "message.blueprint-hud.reloaded" : "message.blueprint-hud.loaded"
				));
			}
			this.minecraft.gui.setScreen(this.parent);
		} catch (Exception e) {
			setStatus(Component.translatable("message.blueprint-hud.failed").getString(), true);
			if (this.minecraft.player != null) {
				this.minecraft.player.sendOverlayMessage(Component.translatable("message.blueprint-hud.failed"));
			}
		}
	}

	private String truncate(String text, int maxWidth) {
		if (this.font.width(text) <= maxWidth) {
			return text;
		}
		String ellipsis = "...";
		int ellipsisWidth = this.font.width(ellipsis);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			builder.append(text.charAt(i));
			if (this.font.width(builder.toString()) + ellipsisWidth > maxWidth) {
				builder.setLength(Math.max(0, builder.length() - 1));
				break;
			}
		}
		return builder + ellipsis;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.pathBox != null && this.pathBox.isFocused() && event.key() == InputConstants.KEY_RETURN) {
			openPathInput();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractTransparentBackground(graphics);
		graphics.centeredText(this.font, this.title, this.width / 2, 6, 0xFFFFFFFF);

		String location = this.currentDir == null
				? Component.translatable("screen.blueprint-hud.browser.roots").getString()
				: this.currentDir.toAbsolutePath().normalize().toString();
		graphics.centeredText(this.font, truncate(location, this.width - 40), this.width / 2, 18, 0xFFCCCCCC);
		graphics.centeredText(this.font, Component.translatable("screen.blueprint-hud.browser.hint"), this.width / 2, 28, 0xFF888888);

		if (!this.statusMessage.isEmpty()) {
			graphics.centeredText(
					this.font,
					Component.literal(this.statusMessage),
					this.width / 2,
					this.height - 48,
					this.statusError ? 0xFFFF5555 : 0xFF88FF88
			);
		} else if (this.entries.isEmpty()) {
			graphics.centeredText(this.font, Component.translatable("screen.blueprint-hud.browser.empty"), this.width / 2, this.height / 2, 0xFFAAAAAA);
		} else {
			int totalPages = Math.max(1, (this.entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
			graphics.centeredText(
					this.font,
					Component.translatable("screen.blueprint-hud.browser.page", this.page + 1, totalPages, this.entries.size()),
					this.width / 2,
					this.height - 48,
					0xFF888888
			);
		}

		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
