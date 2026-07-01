package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.hologram.HologramRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.RenderGlobal;
import net.minecraft.client.render.camera.ICamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public abstract class RenderGlobalEntitiesMixin {
   @Inject(method = "renderEntities", at = @At("RETURN"))
   private void blueprints$renderHologramOverlays(ICamera camera, float partialTick, CallbackInfo ci) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc != null && mc.currentWorld != null) {
         double cx = camera.getX(partialTick);
         double cy = camera.getY(partialTick);
         double cz = camera.getZ(partialTick);
         HologramRenderer.renderPostEntities(mc.currentWorld, cx, cy, cz, partialTick);
      }
   }
}
