package com.goblinthug.blueprinthud.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HistoryScreen extends Screen {
	private static final int PAGE_SIZE = 7;

	private final Screen parent;
	private int page;

	public HistoryScreen(Screen parent) {
		super(Component.translatable("screen.blueprint-hud.history.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ReferenceImageManager manager = ReferenceImageManager.INSTANCE;
		ReferenceConfig config = manager.config();
		config.normalizeHistory();

		List<String> entries = new ArrayList<>(config.history);
		int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		this.page = Math.min(this.page, totalPages - 1);

		int centerX = this.width / 2;
		int top = 40;

		if (entries.isEmpty()) {
			this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
					.bounds(centerX - 100, this.height - 28, 200, 20)
					.build());
			return;
		}

		int start = this.page * PAGE_SIZE;
		int end = Math.min(entries.size(), start + PAGE_SIZE);

		for (int i = start; i < end; i++) {
			String path = entries.get(i);
			int row = i - start;
			int y = top + row * 24;
			String fileName = Path.of(path).getFileName().toString();
			boolean exists = Files.isRegularFile(Path.of(path));
			boolean current = manager.images().stream().anyMatch(image -> path.equals(image.path()));

			Component label = Component.literal(truncate(fileName, 140));
			if (current) {
				label = Component.translatable("screen.blueprint-hud.history.current", truncate(fileName, 120));
			} else if (!exists) {
				label = Component.translatable("screen.blueprint-hud.history.missing", truncate(fileName, 120));
			}

			this.addRenderableWidget(Button.builder(label, button -> loadEntry(path))
					.bounds(centerX - 160, y, 260, 20)
					.build());

			this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.history.delete"), button -> {
				config.removeFromHistory(path);
				config.save();
				this.rebuildWidgets();
			}).bounds(centerX + 105, y, 55, 20).build());
		}

		int navY = top + PAGE_SIZE * 24 + 8;
		if (this.page > 0) {
			this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.history.prev"), button -> {
				this.page--;
				this.rebuildWidgets();
			}).bounds(centerX - 160, navY, 100, 20).build());
		}

		if (this.page < totalPages - 1) {
			this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.history.next"), button -> {
				this.page++;
				this.rebuildWidgets();
			}).bounds(centerX + 60, navY, 100, 20).build());
		}

		this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.history.prune"), button -> {
			int removed = config.pruneMissingFiles();
			config.save();
			if (this.minecraft.player != null) {
				this.minecraft.player.sendOverlayMessage(Component.translatable("message.blueprint-hud.pruned", removed));
			}
			this.rebuildWidgets();
		}).bounds(centerX - 100, this.height - 52, 200, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX - 100, this.height - 28, 200, 20)
				.build());
	}

	private void loadEntry(String pathString) {
		Path path = Path.of(pathString);
		ReferenceImageManager manager = ReferenceImageManager.INSTANCE;
		ReferenceConfig config = manager.config();

		if (!Files.isRegularFile(path)) {
			config.removeFromHistory(pathString);
			config.save();
			if (this.minecraft.player != null) {
				this.minecraft.player.sendOverlayMessage(Component.translatable("message.blueprint-hud.failed"));
			}
			this.rebuildWidgets();
			return;
		}

		boolean alreadyLoaded = manager.images().stream().anyMatch(image -> pathString.equals(image.path()));
		if (!alreadyLoaded && manager.imageCount() >= ReferenceConfig.MAX_IMAGES) {
			if (this.minecraft.player != null) {
				this.minecraft.player.sendOverlayMessage(Component.translatable("message.blueprint-hud.max_images", ReferenceConfig.MAX_IMAGES));
			}
			return;
		}

		try {
			manager.loadImage(path);
			if (this.minecraft.player != null) {
				this.minecraft.player.sendOverlayMessage(Component.translatable(
						alreadyLoaded ? "message.blueprint-hud.reloaded" : "message.blueprint-hud.loaded"
				));
			}
			this.rebuildWidgets();
		} catch (Exception e) {
			if (this.minecraft.player != null) {
				this.minecraft.player.sendOverlayMessage(Component.translatable("message.blueprint-hud.failed"));
			}
			this.rebuildWidgets();
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
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractTransparentBackground(graphics);
		graphics.centeredText(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);

		ReferenceConfig config = ReferenceImageManager.INSTANCE.config();
		int count = config.history == null ? 0 : config.history.size();
		if (count == 0) {
			graphics.centeredText(this.font, Component.translatable("screen.blueprint-hud.history.empty"), this.width / 2, this.height / 2 - 10, 0xFFAAAAAA);
		} else {
			int totalPages = Math.max(1, (count + PAGE_SIZE - 1) / PAGE_SIZE);
			graphics.centeredText(
					this.font,
					Component.translatable("screen.blueprint-hud.history.page", this.page + 1, totalPages, count),
					this.width / 2,
					28,
					0xFFAAAAAA
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
