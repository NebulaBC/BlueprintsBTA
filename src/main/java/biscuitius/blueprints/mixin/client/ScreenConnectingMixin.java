package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.BlueprintsCacheManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenConnecting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenConnecting.class)
public abstract class ScreenConnectingMixin {
   @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/lang/String;I)V", at = @At("RETURN"))
   private void blueprints$captureServerAddress(Minecraft minecraft, String hostName, int port, CallbackInfo ci) {
      BlueprintsCacheManager.setLastServerAddress(hostName, port);
   }
}
