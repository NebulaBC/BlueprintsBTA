package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.hologram.HologramAppearance;
import biscuitius.blueprints.client.hologram.HologramController;
import biscuitius.blueprints.client.hologram.HologramStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.GameSettings;
import net.minecraft.client.render.RenderGlobal;
import net.minecraft.client.render.camera.ICamera;
import net.minecraft.client.render.renderer.BlendFactor;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.renderer.Shaders;
import net.minecraft.client.render.renderer.State;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.HitResult.Tile;
import org.joml.Math;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public abstract class HologramSelectionBoxMixin {
   @Inject(method = "drawSelectionBox", at = @At("HEAD"))
   private void blueprints$drawHologramOutline(ICamera camera, HitResult hitResult, float partialTick, CallbackInfo ci) {
      if (hitResult instanceof Tile) {
         if (DesignModeState.isActive()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.currentWorld != null) {
               if (GameSettings.IMMERSIVE_MODE.drawOutline()) {
                  float w = GameSettings.BLOCK_OUTLINE_WIDTH.get();
                  if (!(w <= 0.01F)) {
                     Tile tile = (Tile)hitResult;
                     int x = tile.tilePos.x();
                     int y = tile.tilePos.y();
                     int z = tile.tilePos.z();
                     if (mc.currentWorld.getBlockId(x, y, z) == 0) {
                        if (HologramStore.get(mc.currentWorld, x, y, z) != null) {
                           AABBdc box = HologramController.getHologramSelectionBox(mc.currentWorld, x, y, z);
                           if (box != null) {
                              double offsetX = camera.getX(partialTick);
                              double offsetY = camera.getY(partialTick);
                              double offsetZ = camera.getZ(partialTick);
                              float r = HologramAppearance.getR() / 255.0F;
                              float g = HologramAppearance.getG() / 255.0F;
                              float b = HologramAppearance.getB() / 255.0F;
                              float expand = 0.002F;
                              AABBd draw = MathHelper.aabbGrow(box, expand, expand, expand, new AABBd());
                              draw.translate(-offsetX, -offsetY, -offsetZ);
                              GLRenderer.pushFrame();
                              GLRenderer.enableState(State.BLEND);
                              GLRenderer.setBlendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
                              GLRenderer.setColor4f(r, g, b, 0.6F + w * 0.3F);
                              GLRenderer.setLineWidth(Math.max(4.0F * w * 2.0F, 1.0F));
                              GLRenderer.setShader(Shaders.LINES);
                              GLRenderer.setDepthMask(false);
                              ((RenderGlobal)(Object)this).drawOutlinedBoundingBox(draw);
                              GLRenderer.popFrame();
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
