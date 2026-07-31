package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.HologramController;
import biscuitius.blueprints.client.hologram.HologramStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.core.Timer;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.Vec3;
import net.minecraft.core.util.phys.HitResult.HitType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class PickBlockHologramMixin {
   @Inject(method = "clickMiddleMouseButton", at = @At("HEAD"), cancellable = true)
   private void blueprints$pickHologram(CallbackInfo ci) {
      Minecraft mc = (Minecraft)(Object)this;
      if (mc.currentWorld != null) {
         if (HologramStore.hasEntries(mc.currentWorld)) {
            PlayerLocal player = DesignModeState.isActive() ? DesignModeState.getControlPlayer(mc) : mc.thePlayer;
            if (player != null) {
               Timer timer = ((MinecraftAccessor)mc).getTimer();
               float partial = timer != null ? timer.partialTicks : 1.0F;
               HitResult realHit = player.rayTrace(256.0, partial, false, false);
               double reach = 256.0;
               Vec3 start = player.getPosition(partial, false);
               Vec3 look = player.getViewVector(partial);
               Vec3 end = start.add(look.x * reach, look.y * reach, look.z * reach);
               HitResult finalHit = HologramController.pickHologramOverlay(mc.currentWorld, start, end, realHit);
               if (finalHit != null && finalHit.hitType == HitType.TILE) {
                  HologramBlock h = HologramStore.get(mc.currentWorld, finalHit.x, finalHit.y, finalHit.z);
                  if (h != null) {
                     HologramController.pickHologramBlock(player, mc.currentWorld, h, finalHit.x, finalHit.y, finalHit.z);
                     ci.cancel();
                  }
               }
            }
         }
      }
   }
}
