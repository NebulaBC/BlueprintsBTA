package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.HologramController;
import biscuitius.blueprints.client.hologram.HologramStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.core.Timer;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.HitResult.Tile;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class PickBlockHologramMixin {
   @Inject(method = "clickMiddleMouseButton", at = @At("HEAD"), cancellable = true)
   private void blueprints$pickHologram(boolean shift, boolean control, CallbackInfo ci) {
      Minecraft mc = (Minecraft)(Object)this;
      if (mc.currentWorld != null) {
         if (HologramStore.hasEntries(mc.currentWorld)) {
            PlayerLocal player = DesignModeState.isActive() ? DesignModeState.getControlPlayer(mc) : mc.thePlayer;
            if (player != null) {
               Timer timer = ((MinecraftAccessor)mc).getTimer();
               float partial = timer != null ? timer.partialTicks : 1.0F;
               HitResult realHit = player.rayCast(256.0, partial, false, false, true);
               double reach = 256.0;
               Vector3dc camPos = player.getPosition(partial, false);
               Vector3dc look = player.getViewVector(partial);
               Vector3dc start = new Vector3d(camPos);
               Vector3dc end = new Vector3d(camPos).add(look.x() * reach, look.y() * reach, look.z() * reach);
               if (HologramController.pickHologramOverlay(mc.currentWorld, start, end, realHit) instanceof Tile finalTile) {
                  int fx = finalTile.tilePos.x();
                  int fy = finalTile.tilePos.y();
                  int fz = finalTile.tilePos.z();
                  HologramBlock h = HologramStore.get(mc.currentWorld, fx, fy, fz);
                  if (h != null) {
                     HologramController.pickHologramBlock(player, mc.currentWorld, h, fx, fy, fz);
                     ci.cancel();
                  }
               }
            }
         }
      }
   }
}
