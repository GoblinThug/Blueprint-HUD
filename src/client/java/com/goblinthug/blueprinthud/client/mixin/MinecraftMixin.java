package com.goblinthug.blueprinthud.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.goblinthug.blueprinthud.client.BlueprintHudClient;

import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void blueprint_hud$blockAttackInMoveMode(CallbackInfoReturnable<Boolean> cir) {
		if (BlueprintHudClient.isMoveModeActive()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void blueprint_hud$blockContinueAttackInMoveMode(boolean leftClick, CallbackInfo ci) {
		if (BlueprintHudClient.isMoveModeActive()) {
			ci.cancel();
		}
	}
}
