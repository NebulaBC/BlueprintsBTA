package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.hologram.HologramRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.RenderGlobal;
import net.minecraft.client.render.camera.ICamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderGlobal.class)
public abstract class HologramRenderMixin {
   @Inject(method = "sortAndRender", at = @At("RETURN"))
   private void blueprints$renderHolograms(ICamera camera, int renderPass, double partialTick, CallbackInfoReturnable<Integer> cir) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc != null && mc.currentWorld != null) {
         double cx = camera.getX((float)partialTick);
         double cy = camera.getY((float)partialTick);
         double cz = camera.getZ((float)partialTick);
         HologramRenderer.render(mc.currentWorld, renderPass, cx, cy, cz);
      }
   }
}
