package com.goblinthug.blueprinthud.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.goblinthug.blueprinthud.client.BlueprintHudClient;

import net.minecraft.client.MouseHandler;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
	private void blueprint_hud$blockGrabDuringMoveMode(CallbackInfo ci) {
		if (BlueprintHudClient.isMoveModeActive()) {
			ci.cancel();
		}
	}
}
