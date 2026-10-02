package com.goblinthug.blueprinthud.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public class CropScreen extends Screen {
	private static final int HANDLE_RADIUS = 6;
	private static final int HANDLE_HIT_PAD = 4;

	private final Screen parent;

	private int previewX;
	private int previewY;
	private int previewW;
	private int previewH;
	private Handle dragging;

	private enum Handle {
		LEFT, RIGHT, TOP, BOTTOM
	}

	public CropScreen(Screen parent) {
		super(Component.translatable("screen.blueprint-hud.crop.title"));
		this.parent = parent;
	}

	private ReferenceImage target() {
		return ReferenceImageManager.INSTANCE.selected();
	}

	@Override
	protected void init() {
		ReferenceImage image = target();
		if (image != null) {
			image.clampCrop();
		}

		int centerX = this.width / 2;
		int y = this.height - 28;

		this.addRenderableWidget(Button.builder(Component.translatable("screen.blueprint-hud.crop.reset"), button -> {
			ReferenceImage current = target();
			if (current == null) {
				return;
			}
			current.resetCrop();
			ReferenceImageManager.INSTANCE.persistImages();
		}).bounds(centerX - 110, y, 100, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX + 10, y, 100, 20)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractTransparentBackground(graphics);
		graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.translatable("screen.blueprint-hud.crop.hint"), this.width / 2, 26, 0xFFAAAAAA);

		ReferenceImage image = target();
		if (image != null && image.hasTexture()) {
			updatePreviewBounds(image);
			renderPreview(graphics, image, mouseX, mouseY);

			graphics.centeredText(
					this.font,
					Component.translatable(
							"screen.blueprint-hud.crop.stats",
							Math.round(image.cropLeft * 100.0f),
							Math.round(image.cropRight * 100.0f),
							Math.round(image.cropTop * 100.0f),
							Math.round(image.cropBottom * 100.0f)
					),
					this.width / 2,
					this.height - 48,
					0xFFCCCCCC
			);
		} else {
			graphics.centeredText(this.font, Component.translatable("screen.blueprint-hud.none"), this.width / 2, this.height / 2 - 20, 0xFFFF5555);
		}

		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private void updatePreviewBounds(ReferenceImage image) {
		int imageWidth = image.imageWidth();
		int imageHeight = image.imageHeight();
		int maxPreviewW = Math.min(520, this.width - 48);
		int maxPreviewH = Math.max(120, this.height - 100);
		float fit = Math.min(maxPreviewW / (float) imageWidth, maxPreviewH / (float) imageHeight);

		this.previewW = Math.max(1, Math.round(imageWidth * fit));
		this.previewH = Math.max(1, Math.round(imageHeight * fit));
		this.previewX = (this.width - this.previewW) / 2;
		this.previewY = 44 + Math.max(0, (maxPreviewH - this.previewH) / 2);
	}

	private void renderPreview(GuiGraphicsExtractor graphics, ReferenceImage image, int mouseX, int mouseY) {
		int imageWidth = image.imageWidth();
		int imageHeight = image.imageHeight();
		int srcX = image.cropLeftPx();
		int srcY = image.cropTopPx();
		int srcW = image.sourceWidth();
		int srcH = image.sourceHeight();

		float fitX = this.previewW / (float) imageWidth;
		float fitY = this.previewH / (float) imageHeight;

		graphics.fill(this.previewX - 2, this.previewY - 2, this.previewX + this.previewW + 2, this.previewY + this.previewH + 2, 0x88000000);

		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				image.textureId(),
				this.previewX,
				this.previewY,
				0.0f,
				0.0f,
				this.previewW,
				this.previewH,
				imageWidth,
				imageHeight,
				imageWidth,
				imageHeight,
				ARGB.color(60, 255, 255, 255)
		);

		int cropX = this.previewX + Math.round(srcX * fitX);
		int cropY = this.previewY + Math.round(srcY * fitY);
		int cropW = Math.max(1, Math.round(srcW * fitX));
		int cropH = Math.max(1, Math.round(srcH * fitY));

		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				image.textureId(),
				cropX,
				cropY,
				srcX,
				srcY,
				cropW,
				cropH,
				srcW,
				srcH,
				imageWidth,
				imageHeight,
				0xFFFFFFFF
		);

		graphics.outline(cropX, cropY, cropW, cropH, 0xFFFFFF55);

		Handle hovered = this.dragging != null ? this.dragging : hitTestHandle(mouseX, mouseY);
		drawHandle(graphics, cropX, cropY + cropH / 2, hovered == Handle.LEFT || this.dragging == Handle.LEFT);
		drawHandle(graphics, cropX + cropW, cropY + cropH / 2, hovered == Handle.RIGHT || this.dragging == Handle.RIGHT);
		drawHandle(graphics, cropX + cropW / 2, cropY, hovered == Handle.TOP || this.dragging == Handle.TOP);
		drawHandle(graphics, cropX + cropW / 2, cropY + cropH, hovered == Handle.BOTTOM || this.dragging == Handle.BOTTOM);
	}

	private void drawHandle(GuiGraphicsExtractor graphics, int cx, int cy, boolean active) {
		int fill = active ? 0xFFFFFF55 : 0xFFFFFFFF;
		int border = active ? 0xFFFFAA00 : 0xFF222222;
		drawCircle(graphics, cx, cy, HANDLE_RADIUS + 1, border);
		drawCircle(graphics, cx, cy, HANDLE_RADIUS, fill);
		drawCircle(graphics, cx, cy, 2, 0xFF222222);
	}

	private static void drawCircle(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			int dx = (int) Math.floor(Math.sqrt(radius * radius - dy * dy));
			graphics.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
		}
	}

	private Handle hitTestHandle(double mouseX, double mouseY) {
		ReferenceImage image = target();
		if (image == null || !image.hasTexture() || this.previewW <= 0 || this.previewH <= 0) {
			return null;
		}

		float fitX = this.previewW / (float) image.imageWidth();
		float fitY = this.previewH / (float) image.imageHeight();
		int cropX = this.previewX + Math.round(image.cropLeftPx() * fitX);
		int cropY = this.previewY + Math.round(image.cropTopPx() * fitY);
		int cropW = Math.max(1, Math.round(image.sourceWidth() * fitX));
		int cropH = Math.max(1, Math.round(image.sourceHeight() * fitY));

		double hitR = HANDLE_RADIUS + HANDLE_HIT_PAD;
		if (distance(mouseX, mouseY, cropX, cropY + cropH / 2.0) <= hitR) {
			return Handle.LEFT;
		}
		if (distance(mouseX, mouseY, cropX + cropW, cropY + cropH / 2.0) <= hitR) {
			return Handle.RIGHT;
		}
		if (distance(mouseX, mouseY, cropX + cropW / 2.0, cropY) <= hitR) {
			return Handle.TOP;
		}
		if (distance(mouseX, mouseY, cropX + cropW / 2.0, cropY + cropH) <= hitR) {
			return Handle.BOTTOM;
		}
		return null;
	}

	private static double distance(double x1, double y1, double x2, double y2) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		return Math.sqrt(dx * dx + dy * dy);
	}

	private void applyDrag(double mouseX, double mouseY) {
		ReferenceImage image = target();
		if (this.dragging == null || image == null || this.previewW <= 0 || this.previewH <= 0) {
			return;
		}

		float minRemain = ReferenceConfig.MIN_CROP_REMAINING;

		switch (this.dragging) {
			case LEFT -> {
				float left = (float) ((mouseX - this.previewX) / this.previewW);
				image.cropLeft = Mth.clamp(left, 0.0f, 1.0f - image.cropRight - minRemain);
			}
			case RIGHT -> {
				float right = 1.0f - (float) ((mouseX - this.previewX) / this.previewW);
				image.cropRight = Mth.clamp(right, 0.0f, 1.0f - image.cropLeft - minRemain);
			}
			case TOP -> {
				float top = (float) ((mouseY - this.previewY) / this.previewH);
				image.cropTop = Mth.clamp(top, 0.0f, 1.0f - image.cropBottom - minRemain);
			}
			case BOTTOM -> {
				float bottom = 1.0f - (float) ((mouseY - this.previewY) / this.previewH);
				image.cropBottom = Mth.clamp(bottom, 0.0f, 1.0f - image.cropTop - minRemain);
			}
		}

		image.clampCrop();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			ReferenceImage image = target();
			if (image != null) {
				updatePreviewBounds(image);
				Handle handle = hitTestHandle(event.x(), event.y());
				if (handle != null) {
					this.dragging = handle;
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (this.dragging != null && event.button() == 0) {
			applyDrag(event.x(), event.y());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.dragging != null && event.button() == 0) {
			this.dragging = null;
			ReferenceImageManager.INSTANCE.persistImages();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		ReferenceImageManager.INSTANCE.persistImages();
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
