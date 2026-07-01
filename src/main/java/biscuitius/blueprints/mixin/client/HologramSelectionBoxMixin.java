package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.hologram.HologramAppearance;
import biscuitius.blueprints.client.hologram.HologramController;
import biscuitius.blueprints.client.hologram.HologramStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.RenderGlobal;
import net.minecraft.client.render.camera.ICamera;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.HitResult.HitType;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public abstract class HologramSelectionBoxMixin {
   @Inject(method = "drawSelectionBox", at = @At("HEAD"))
   private void blueprints$drawHologramOutline(ICamera camera, HitResult hitResult, float partialTick, CallbackInfo ci) {
      if (hitResult != null && hitResult.hitType == HitType.TILE) {
         if (DesignModeState.isActive()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.currentWorld != null) {
               if (mc.gameSettings.immersiveMode.drawOutline()) {
                  float widthSetting = (Float)mc.gameSettings.blockOutlineWidth.value;
                  if (!(widthSetting <= 0.01F)) {
                     if (mc.currentWorld.getBlockId(hitResult.x, hitResult.y, hitResult.z) == 0) {
                        if (HologramStore.get(mc.currentWorld, hitResult.x, hitResult.y, hitResult.z) != null) {
                           AABB box = HologramController.getHologramSelectionBox(mc.currentWorld, hitResult.x, hitResult.y, hitResult.z);
                           if (box != null) {
                              double offsetX = camera.getX(partialTick);
                              double offsetY = camera.getY(partialTick);
                              double offsetZ = camera.getZ(partialTick);
                              float r = HologramAppearance.getR() / 255.0F;
                              float g = HologramAppearance.getG() / 255.0F;
                              float b = HologramAppearance.getB() / 255.0F;
                              float w = mc.getOutlineWidth();
                              GL11.glEnable(3042);
                              GL11.glBlendFunc(770, 771);
                              GL11.glColor4f(r, g, b, 0.6F + w * 0.3F);
                              GL11.glLineWidth(Math.max(4.0F * w * 2.0F, 1.0F));
                              GL11.glDisable(3553);
                              GL11.glDepthMask(false);
                              float expand = 0.002F;
                              ((RenderGlobal)(Object)this).drawOutlinedBoundingBox(box.grow(expand, expand, expand).cloneMove(-offsetX, -offsetY, -offsetZ));
                              GL11.glDepthMask(true);
                              GL11.glEnable(3553);
                              GL11.glDisable(3042);
                              GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
