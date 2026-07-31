package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.hologram.HologramController;
import biscuitius.blueprints.client.hologram.HologramStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.HitResult.Entity;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class DesignModeRaytraceMixin {
   @Inject(method = "getMouseOver", at = @At("RETURN"))
   private void blueprints$overlayHologramHit(float partialTicks, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc != null && mc.currentWorld != null) {
            HitResult existing = mc.objectMouseOver;
            if (!(existing instanceof Entity)) {
               if (HologramStore.hasEntries(mc.currentWorld)) {
                  PlayerLocal controlPlayer = DesignModeState.getControlPlayer(mc);
                  if (controlPlayer != null) {
                     double reach = controlPlayer.getGamemode().getBlockReachDistance();
                     Vector3dc pos = controlPlayer.getPosition(partialTicks, true);
                     Vector3dc look = controlPlayer.getViewVector(partialTicks);
                     Vector3dc start = new Vector3d(pos);
                     Vector3dc end = new Vector3d(pos).add(look.x() * reach, look.y() * reach, look.z() * reach);
                     HitResult overlaid = HologramController.pickHologramOverlay(mc.currentWorld, start, end, existing);
                     if (overlaid != existing) {
                        mc.objectMouseOver = overlaid;
                     }
                  }
               }
            }
         }
      }
   }
}
