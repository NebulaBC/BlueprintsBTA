package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.hologram.HologramRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.camera.ICamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class HologramRenderMixin {
   @Inject(
      method = "renderWorld",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/terrain/TerrainRenderer;renderSolidTerrain(F)V", shift = Shift.AFTER)
   )
   private void blueprints$renderHologramsSolid(float partialTicks, long updateRenderersUntil, CallbackInfo ci) {
      renderHologramPass(0, partialTicks);
   }

   @Inject(
      method = "renderWorld",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/terrain/TerrainRenderer;renderTranslucentTerrain(F)V", shift = Shift.AFTER)
   )
   private void blueprints$renderHologramsTranslucent(float partialTicks, long updateRenderersUntil, CallbackInfo ci) {
      renderHologramPass(1, partialTicks);
   }

   @Unique
   private static void renderHologramPass(int renderPass, float partialTicks) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc != null && mc.currentWorld != null) {
         ICamera camera = mc.activeCamera;
         if (camera != null) {
            double cx = camera.getX(partialTicks);
            double cy = camera.getY(partialTicks);
            double cz = camera.getZ(partialTicks);
            HologramRenderer.render(mc.currentWorld, renderPass, cx, cy, cz);
         }
      }
   }
}
