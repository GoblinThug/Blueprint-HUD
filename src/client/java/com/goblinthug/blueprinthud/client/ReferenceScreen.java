package com.goblinthug.blueprinthud.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ReferenceScreen extends Screen {
	private final Screen parent;

	public ReferenceScreen(Screen parent) {
		super(Component.translatable("screen.blueprint-hud.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ReferenceImageManager manager = ReferenceImageManager.INSTANCE;
		ReferenceConfig config = manager.config();
		ReferenceImage selected = manager.selected();
		if (selected != null) {
			selected.clamp();
		}

		int centerX = this.width / 2;
		int y = this.height / 4 - 8;

		this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.load"), button -> manager.openFilePicker(this))
				.bounds(centerX - 100, y, 200, 20)
				.build());
		y += 24;

		this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.history.open"), button -> {
			this.minecraft.gui.setScreen(new HistoryScreen(this));
		}).bounds(centerX - 100, y, 200, 20).build());
		y += 24;

		if (manager.hasImage()) {
			this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.prev"), button -> {
				manager.selectPrevious();
				this.rebuildWidgets();
			}).bounds(centerX - 100, y, 96, 20).build());
			this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.next"), button -> {
				manager.selectNext();
				this.rebuildWidgets();
			}).bounds(centerX + 4, y, 96, 20).build());
			y += 24;
		}

		this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.crop.open"), button -> {
			if (manager.selected() == null) {
				if (this.minecraft.player != null) {
					this.minecraft.player.sendOverlayMessage(Component.translatable("screen.blueprint-hud.none"));
				}
				return;
			}
			this.minecraft.gui.setScreen(new CropScreen(this));
		}).bounds(centerX - 100, y, 200, 20).build());
		y += 24;

		this.addRenderableWidget(Button.builder(visibleLabel(config), button -> {
			config.visible = !config.visible;
			config.save();
			button.setMessage(visibleLabel(config));
		}).bounds(centerX - 100, y, 200, 20).build());
		y += 28;

		if (selected != null) {
			final ReferenceImage image = selected;

			this.addRenderableWidget(new AbstractSliderButton(centerX - 100, y, 200, 20, opacityLabel(image), image.opacity) {
				{
					this.updateMessage();
				}

				@Override
				protected void updateMessage() {
					this.setMessage(opacityLabel(image));
				}

				@Override
				protected void applyValue() {
					image.opacity = (float) this.value;
					image.clamp();
					manager.persistImages();
				}
			});
			y += 24;

			double scaleSlider = Mth.inverseLerp(image.scale, 0.05f, 3.0f);
			this.addRenderableWidget(new AbstractSliderButton(centerX - 100, y, 200, 20, scaleLabel(image), scaleSlider) {
				{
					this.updateMessage();
				}

				@Override
				protected void updateMessage() {
					this.setMessage(scaleLabel(image));
				}

				@Override
				protected void applyValue() {
					float oldX = image.x;
					float oldY = image.y;
					int oldW = image.drawWidth();
					int oldH = image.drawHeight();
					image.scale = Mth.lerp((float) this.value, 0.05f, 3.0f);
					image.clamp();
					if (ReferenceScreen.this.minecraft != null) {
						var window = ReferenceScreen.this.minecraft.getWindow();
						image.applyScaleKeepNearestCorner(
								window.getGuiScaledWidth(),
								window.getGuiScaledHeight(),
								oldX,
								oldY,
								oldW,
								oldH
						);
					}
					manager.persistImages();
				}
			});
			y += 28;

			this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.reset"), button -> {
				image.x = 20;
				image.y = 20;
				manager.persistImages();
			}).bounds(centerX - 100, y, 200, 20).build());
			y += 24;

			this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.remove"), button -> {
				manager.removeSelected();
				if (this.minecraft.player != null) {
					this.minecraft.player.sendOverlayMessage(Component.translatable("message.blueprint-hud.removed"));
				}
				this.rebuildWidgets();
			}).bounds(centerX - 100, y, 200, 20).build());
			y += 24;
		}

		this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.clear"), button -> {
			manager.clearAll();
			if (this.minecraft.player != null) {
				this.minecraft.player.sendOverlayMessage(Component.translatable("message.blueprint-hud.cleared"));
			}
			this.rebuildWidgets();
		}).bounds(centerX - 100, y, 200, 20).build());
		y += 28;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX - 100, y, 200, 20)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractTransparentBackground(graphics);
		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 4 - 38, 0xFFFFFFFF);

		ReferenceImageManager manager = ReferenceImageManager.INSTANCE;
		Component status;
		if (!manager.hasImage()) {
			status = Component.translatable("screen.blueprint-hud.none");
		} else {
			ReferenceImage selected = manager.selected();
			String name = selected != null ? selected.loadedName() : "?";
			status = Component.translatable(
					"screen.blueprint-hud.loaded_multi",
					manager.selectedIndex() + 1,
					manager.imageCount(),
					name
			);
		}
		graphics.centeredText(this.font, status, this.width / 2, this.height / 4 - 24, 0xFFAAAAAA);
		graphics.centeredText(this.font, Component.translatable("screen.blueprint-hud.hint"), this.width / 2, this.height - 28, 0xFF888888);

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

	private static Component visibleLabel(ReferenceConfig config) {
		return Component.translatable("screen.blueprint-hud.visible", config.visible ? "ON" : "OFF");
	}

	private static Component opacityLabel(ReferenceImage image) {
		return Component.translatable("screen.blueprint-hud.opacity", Math.round(image.opacity * 100.0f));
	}

	private static Component scaleLabel(ReferenceImage image) {
		return Component.translatable("screen.blueprint-hud.scale", Math.round(image.scale * 100.0f));
	}
}
