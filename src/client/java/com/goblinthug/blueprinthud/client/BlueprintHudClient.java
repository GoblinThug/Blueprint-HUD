package com.goblinthug.blueprinthud.client;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWScrollCallback;

import com.goblinthug.blueprinthud.BlueprintHudMod;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class BlueprintHudClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(BlueprintHudMod.id("main"));

	private static BlueprintHudClient instance;
	private static volatile boolean moveModeActive;

	private final KeyMapping openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.blueprint-hud.open",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_R,
			CATEGORY
	));

	private final KeyMapping toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.blueprint-hud.toggle",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_H,
			CATEGORY
	));

	private final KeyMapping moveKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.blueprint-hud.move",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_LALT,
			CATEGORY
	));

	private boolean dragging;
	private boolean leftWasDown;
	private double dragOffsetX;
	private double dragOffsetY;
	private ReferenceImage dragTarget;

	public static boolean isMoveModeActive() {
		return moveModeActive;
	}

	/** Called every rendered frame so dragging stays smooth (not limited to 20 TPS). */
	public static void updateDragEveryFrame(Minecraft client) {
		if (instance != null) {
			instance.handleDragFrame(client);
		}
	}

	@Override
	public void onInitializeClient() {
		instance = this;

		HudElementRegistry.attachElementAfter(
				VanillaHudElements.MISC_OVERLAYS,
				BlueprintHudMod.id("reference_overlay"),
				ReferenceOverlay::render
		);

		ClientTickEvents.START_CLIENT_TICK.register(this::onClientTick);

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			long handle = client.getWindow().handle();
			GLFWScrollCallback previous = GLFW.glfwSetScrollCallback(handle, null);
			GLFW.glfwSetScrollCallback(handle, (window, horizontal, vertical) -> {
				if (this.moveKey.isDown() && client.gui.screen() == null && handleScaleScroll(client, vertical)) {
					return;
				}
				if (previous != null) {
					previous.invoke(window, horizontal, vertical);
				}
			});

			ReferenceImageManager.INSTANCE.tryReloadSavedImages();
		});

		BlueprintHudMod.LOGGER.info("Blueprint-HUD ready");
	}

	private void onClientTick(Minecraft client) {
		while (this.openMenuKey.consumeClick()) {
			client.gui.setScreen(new ReferenceScreen(client.gui.screen()));
		}

		while (this.toggleKey.consumeClick()) {
			ReferenceConfig config = ReferenceImageManager.INSTANCE.config();
			config.visible = !config.visible;
			config.save();
			if (client.player != null) {
				client.player.sendOverlayMessage(
						Component.translatable(config.visible ? "message.blueprint-hud.toggled_on" : "message.blueprint-hud.toggled_off")
				);
			}
		}

		if (client.getWindow() == null || client.player == null) {
			exitMoveMode(client, false);
			return;
		}

		boolean wantMoveMode = this.moveKey.isDown()
				&& client.gui.screen() == null
				&& ReferenceImageManager.INSTANCE.hasImage()
				&& ReferenceImageManager.INSTANCE.config().visible;

		if (wantMoveMode) {
			enterMoveMode(client);
		} else {
			exitMoveMode(client, true);
		}
	}

	private void enterMoveMode(Minecraft client) {
		moveModeActive = true;
		if (client.mouseHandler.isMouseGrabbed()) {
			client.mouseHandler.releaseMouse();
		}
	}

	private void exitMoveMode(Minecraft client, boolean grabMouse) {
		if (!moveModeActive && !this.dragging) {
			return;
		}

		if (this.dragging) {
			this.dragging = false;
			this.dragTarget = null;
			ReferenceImageManager.INSTANCE.persistImages();
		}
		this.leftWasDown = false;

		boolean wasActive = moveModeActive;
		moveModeActive = false;

		if (wasActive && grabMouse && client.player != null && client.gui.screen() == null && !client.mouseHandler.isMouseGrabbed()) {
			client.mouseHandler.grabMouse();
		}
	}

	private void handleDragFrame(Minecraft client) {
		if (!moveModeActive || client.getWindow() == null || client.gui.screen() != null) {
			return;
		}

		Window window = client.getWindow();
		boolean leftDown = GLFW.glfwGetMouseButton(window.handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		ReferenceImageManager manager = ReferenceImageManager.INSTANCE;

		double mouseX = readScaledMouseX(window);
		double mouseY = readScaledMouseY(window);

		if (leftDown) {
			if (!this.dragging) {
				if (!this.leftWasDown) {
					ReferenceImage hit = manager.findImageAt(mouseX, mouseY);
					if (hit != null) {
						manager.bringToFront(hit);
						this.dragging = true;
						this.dragTarget = hit;
						this.dragOffsetX = mouseX - hit.x;
						this.dragOffsetY = mouseY - hit.y;
					}
				}
			}

			if (this.dragging && this.dragTarget != null) {
				this.dragTarget.x = (float) (mouseX - this.dragOffsetX);
				this.dragTarget.y = (float) (mouseY - this.dragOffsetY);
				clampToScreen(client, this.dragTarget);
			}
		} else if (this.dragging) {
			this.dragging = false;
			if (this.dragTarget != null) {
				clampToScreen(client, this.dragTarget);
				manager.persistImages();
			}
			this.dragTarget = null;
		}

		this.leftWasDown = leftDown;
	}

	private static double readScaledMouseX(Window window) {
		double[] xpos = new double[1];
		double[] ypos = new double[1];
		GLFW.glfwGetCursorPos(window.handle(), xpos, ypos);
		return xpos[0] * (double) window.getGuiScaledWidth() / (double) Math.max(1, window.getWidth());
	}

	private static double readScaledMouseY(Window window) {
		double[] xpos = new double[1];
		double[] ypos = new double[1];
		GLFW.glfwGetCursorPos(window.handle(), xpos, ypos);
		return ypos[0] * (double) window.getGuiScaledHeight() / (double) Math.max(1, window.getHeight());
	}

	static void clampToScreen(Minecraft client, ReferenceImage image) {
		Window window = client.getWindow();
		image.clampToScreen(window.getGuiScaledWidth(), window.getGuiScaledHeight());
	}

	private boolean handleScaleScroll(Minecraft client, double vertical) {
		ReferenceImageManager manager = ReferenceImageManager.INSTANCE;
		if (!manager.hasImage()) {
			return false;
		}

		Window window = client.getWindow();
		double mouseX = readScaledMouseX(window);
		double mouseY = readScaledMouseY(window);

		ReferenceImage image = manager.findImageAt(mouseX, mouseY);
		if (image == null) {
			image = manager.selected();
		}
		if (image == null) {
			return false;
		}

		manager.selectImage(image);

		float oldX = image.x;
		float oldY = image.y;
		int oldW = image.drawWidth();
		int oldH = image.drawHeight();

		image.scale += (float) (vertical * 0.05f);
		image.clamp();
		image.applyScaleKeepNearestCorner(
				window.getGuiScaledWidth(),
				window.getGuiScaledHeight(),
				oldX,
				oldY,
				oldW,
				oldH
		);
		manager.persistImages();
		return true;
	}
}
