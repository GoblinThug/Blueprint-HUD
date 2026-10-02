package com.goblinthug.blueprinthud.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

import com.goblinthug.blueprinthud.BlueprintHudMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.platform.NativeImage;

/**
 * One on-screen reference layer: texture + transform + crop.
 */
public final class ReferenceImage {
	private static final int MAX_SIZE = 4096;
	private static final AtomicInteger NEXT_ID = new AtomicInteger();

	private final Identifier textureId;
	private DynamicTexture texture;
	private int imageWidth;
	private int imageHeight;
	private String path = "";
	private String loadedName = "";

	public float opacity = 0.55f;
	public float scale = 0.45f;
	public float x = 20;
	public float y = 20;
	public float cropLeft = 0.0f;
	public float cropRight = 0.0f;
	public float cropTop = 0.0f;
	public float cropBottom = 0.0f;

	public ReferenceImage() {
		this.textureId = BlueprintHudMod.id("dynamic/reference_" + NEXT_ID.getAndIncrement());
	}

	public Identifier textureId() {
		return textureId;
	}

	public boolean hasTexture() {
		return texture != null;
	}

	public int imageWidth() {
		return imageWidth;
	}

	public int imageHeight() {
		return imageHeight;
	}

	public String path() {
		return path;
	}

	public String loadedName() {
		return loadedName;
	}

	public int cropLeftPx() {
		return Math.clamp(Math.round(imageWidth * cropLeft), 0, Math.max(0, imageWidth - 1));
	}

	public int cropRightPx() {
		return Math.clamp(Math.round(imageWidth * cropRight), 0, Math.max(0, imageWidth - 1));
	}

	public int cropTopPx() {
		return Math.clamp(Math.round(imageHeight * cropTop), 0, Math.max(0, imageHeight - 1));
	}

	public int cropBottomPx() {
		return Math.clamp(Math.round(imageHeight * cropBottom), 0, Math.max(0, imageHeight - 1));
	}

	public int sourceWidth() {
		return Math.max(1, imageWidth - cropLeftPx() - cropRightPx());
	}

	public int sourceHeight() {
		return Math.max(1, imageHeight - cropTopPx() - cropBottomPx());
	}

	public int drawWidth() {
		return Math.max(1, Math.round(sourceWidth() * scale));
	}

	public int drawHeight() {
		return Math.max(1, Math.round(sourceHeight() * scale));
	}

	public boolean contains(double mouseX, double mouseY) {
		int drawW = drawWidth();
		int drawH = drawHeight();
		return mouseX >= x && mouseY >= y && mouseX < x + drawW && mouseY < y + drawH;
	}

	public void clamp() {
		opacity = Math.clamp(opacity, 0.05f, 1.0f);
		scale = Math.clamp(scale, 0.05f, 3.0f);
		clampCrop();
	}

	public void clampCrop() {
		float minRemain = ReferenceConfig.MIN_CROP_REMAINING;
		cropLeft = Math.clamp(cropLeft, 0.0f, 1.0f - minRemain);
		cropRight = Math.clamp(cropRight, 0.0f, 1.0f - minRemain);
		cropTop = Math.clamp(cropTop, 0.0f, 1.0f - minRemain);
		cropBottom = Math.clamp(cropBottom, 0.0f, 1.0f - minRemain);

		float maxHorizontal = 1.0f - minRemain;
		if (cropLeft + cropRight > maxHorizontal) {
			float scaleDown = maxHorizontal / (cropLeft + cropRight);
			cropLeft *= scaleDown;
			cropRight *= scaleDown;
		}

		float maxVertical = 1.0f - minRemain;
		if (cropTop + cropBottom > maxVertical) {
			float scaleDown = maxVertical / (cropTop + cropBottom);
			cropTop *= scaleDown;
			cropBottom *= scaleDown;
		}
	}

	public void resetCrop() {
		cropLeft = 0.0f;
		cropRight = 0.0f;
		cropTop = 0.0f;
		cropBottom = 0.0f;
	}

	public void copyTransformFrom(ReferenceConfig.SavedImage saved) {
		opacity = saved.opacity;
		scale = saved.scale;
		x = saved.x;
		y = saved.y;
		cropLeft = saved.cropLeft;
		cropRight = saved.cropRight;
		cropTop = saved.cropTop;
		cropBottom = saved.cropBottom;
		clamp();
	}

	public ReferenceConfig.SavedImage toSaved() {
		ReferenceConfig.SavedImage saved = new ReferenceConfig.SavedImage();
		saved.path = path;
		saved.opacity = opacity;
		saved.scale = scale;
		saved.x = x;
		saved.y = y;
		saved.cropLeft = cropLeft;
		saved.cropRight = cropRight;
		saved.cropTop = cropTop;
		saved.cropBottom = cropBottom;
		return saved;
	}

	public void loadFromPath(Path filePath, boolean keepCrop) throws IOException {
		BufferedImage buffered = ImageIO.read(filePath.toFile());
		if (buffered == null) {
			throw new IOException("Unsupported or unreadable image: " + filePath);
		}

		int width = buffered.getWidth();
		int height = buffered.getHeight();
		if (width <= 0 || height <= 0) {
			throw new IOException("Invalid image size");
		}
		if (width > MAX_SIZE || height > MAX_SIZE) {
			buffered = scaleDown(buffered, MAX_SIZE);
			width = buffered.getWidth();
			height = buffered.getHeight();
		}

		NativeImage nativeImage = new NativeImage(width, height, false);
		for (int yPx = 0; yPx < height; yPx++) {
			for (int xPx = 0; xPx < width; xPx++) {
				nativeImage.setPixel(xPx, yPx, buffered.getRGB(xPx, yPx));
			}
		}

		clearTexture();

		Minecraft client = Minecraft.getInstance();
		texture = new DynamicTexture(() -> "blueprint-hud/" + textureId.getPath(), nativeImage);
		client.getTextureManager().register(textureId, texture);

		imageWidth = width;
		imageHeight = height;
		path = filePath.toAbsolutePath().normalize().toString();
		loadedName = filePath.getFileName().toString();
		if (!keepCrop) {
			resetCrop();
		}
		clamp();
	}

	public void clearTexture() {
		if (texture == null) {
			return;
		}
		Minecraft.getInstance().getTextureManager().release(textureId);
		texture.close();
		texture = null;
	}

	/**
	 * Keeps the corner closest to a screen corner fixed while size changes.
	 */
	public void applyScaleKeepNearestCorner(int screenW, int screenH, float oldX, float oldY, int oldW, int oldH) {
		int newW = drawWidth();
		int newH = drawHeight();

		float distLeft = oldX;
		float distRight = screenW - (oldX + oldW);
		float distTop = oldY;
		float distBottom = screenH - (oldY + oldH);

		boolean anchorRight = distRight < distLeft;
		boolean anchorBottom = distBottom < distTop;

		if (anchorRight) {
			x = oldX + oldW - newW;
		} else {
			x = oldX;
		}
		if (anchorBottom) {
			y = oldY + oldH - newH;
		} else {
			y = oldY;
		}

		clampToScreen(screenW, screenH);
	}

	public void clampToScreen(int screenW, int screenH) {
		int drawW = drawWidth();
		int drawH = drawHeight();
		float minX = Math.min(0, screenW - drawW);
		float maxX = Math.max(0, screenW - drawW);
		float minY = Math.min(0, screenH - drawH);
		float maxY = Math.max(0, screenH - drawH);
		x = Mth.clamp(x, minX, maxX);
		y = Mth.clamp(y, minY, maxY);
	}

	private static BufferedImage scaleDown(BufferedImage source, int maxSize) {
		int width = source.getWidth();
		int height = source.getHeight();
		float factor = Math.min(maxSize / (float) width, maxSize / (float) height);
		int newWidth = Math.max(1, Math.round(width * factor));
		int newHeight = Math.max(1, Math.round(height * factor));

		BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g = scaled.createGraphics();
		g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(source, 0, 0, newWidth, newHeight, null);
		g.dispose();
		return scaled;
	}
}
