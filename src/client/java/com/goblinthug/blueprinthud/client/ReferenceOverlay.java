package com.goblinthug.blueprinthud.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class ReferenceOverlay {
	private ReferenceOverlay() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		ReferenceImageManager manager = ReferenceImageManager.INSTANCE;
		ReferenceConfig config = manager.config();

		if (!manager.hasImage() || !config.visible) {
			return;
		}

		Minecraft client = Minecraft.getInstance();

		// Update drag every frame (smooth), before drawing — even if HUD chrome is hidden
		BlueprintHudClient.updateDragEveryFrame(client);

		if (client.gui.hud.isHidden()) {
			return;
		}

		for (ReferenceImage image : manager.images()) {
			if (!image.hasTexture()) {
				continue;
			}

			int color = ARGB.color(Mth.floor(image.opacity * 255.0f), 255, 255, 255);
			graphics.blit(
					RenderPipelines.GUI_TEXTURED,
					image.textureId(),
					Math.round(image.x),
					Math.round(image.y),
					image.cropLeftPx(),
					image.cropTopPx(),
					image.drawWidth(),
					image.drawHeight(),
					image.sourceWidth(),
					image.sourceHeight(),
					image.imageWidth(),
					image.imageHeight(),
					color
			);
		}
	}
}
