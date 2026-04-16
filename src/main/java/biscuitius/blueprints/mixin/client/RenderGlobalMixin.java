package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.modelviewer.ScreenModelViewer;
import net.minecraft.client.render.RenderGlobal;
import net.minecraft.client.render.camera.ICamera;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.util.phys.BoundingVolume;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.HitResult.HitType;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public abstract class RenderGlobalMixin {
   @Inject(method = "drawSelectionBox", at = @At("HEAD"), cancellable = true)
   private void blueprints$drawGhostSelectionBox(ICamera camera, HitResult hitResult, float partialTick, CallbackInfo ci) {
      Minecraft minecraft = Minecraft.getMinecraft();
      if (DesignModeState.isActive() && minecraft != null && minecraft.currentWorld != null && hitResult != null && hitResult.hitType == HitType.TILE) {
         if (minecraft.gameSettings.immersiveMode.drawOutline()
            && !((Float)minecraft.gameSettings.blockOutlineWidth.value <= 0.01F)
            && !(minecraft.currentScreen instanceof ScreenModelViewer)) {
            int blockId = minecraft.currentWorld.getBlockId(hitResult.x, hitResult.y, hitResult.z);
            if (blockId > 0) {
               Block<?> block = Blocks.blocksList[blockId];
               if (block != null) {
                  boolean isGhost = GhostBlockState.isGhostBlock(minecraft.currentWorld, hitResult.x, hitResult.y, hitResult.z);
                  GL11.glEnable(3042);
                  GL11.glBlendFunc(770, 771);
                  float w = minecraft.getOutlineWidth();
                  if (isGhost) {
                     GL11.glColor4f(0.0F, 0.25F, 1.0F, 0.45F + w * 0.35F);
                  } else {
                     GL11.glColor4f(1.0F, 0.0F, 0.0F, 0.45F + w * 0.35F);
                  }

                  GL11.glLineWidth(Math.max(4.0F * w * 2.0F, 1.0F));
                  GL11.glDisable(3553);
                  GL11.glDisable(2884);
                  GL11.glDepthMask(false);
                  GL11.glDisable(2929);
                  double offsetX = camera.getX(partialTick);
                  double offsetY = camera.getY(partialTick);
                  double offsetZ = camera.getZ(partialTick);
                  AABB fillBox = block.getSelectedBoundingBoxFromPool(minecraft.currentWorld, hitResult.x, hitResult.y, hitResult.z)
                     .cloneMove(-offsetX, -offsetY, -offsetZ);
                  if (isGhost) {
                     this.blueprints$drawFilledBoundingBox(fillBox, 0.14F + w * 0.18F, 0.0F, 0.25F, 1.0F);
                  } else {
                     this.blueprints$drawFilledBoundingBox(fillBox, 0.14F + w * 0.18F, 1.0F, 0.0F, 0.0F);
                  }

                  GL11.glEnable(2929);
                  BoundingVolume volume = block.getBoundingVolume(minecraft.currentWorld, hitResult.x, hitResult.y, hitResult.z);
                  if (volume != null) {
                     ((RenderGlobal)(Object)this).drawOutlinedVolume(volume, hitResult.x - offsetX, hitResult.y - offsetY, hitResult.z - offsetZ, 0.002F);
                  } else {
                     AABB box = fillBox.grow(0.002F, 0.002F, 0.002F);
                     ((RenderGlobal)(Object)this).drawOutlinedBoundingBox(box);
                  }

                  GL11.glDepthMask(true);
                  GL11.glEnable(2884);
                  GL11.glEnable(3553);
                  GL11.glDisable(3042);
                  ci.cancel();
               }
            }
         }
      }
   }

   @Unique
   private void blueprints$drawFilledBoundingBox(AABB box, float alpha, float r, float g, float b) {
      Tessellator tessellator = Tessellator.instance;
      tessellator.startDrawingQuads();
      tessellator.setColorRGBA_F(r, g, b, alpha);
      double minX = box.minX;
      double minY = box.minY;
      double minZ = box.minZ;
      double maxX = box.maxX;
      double maxY = box.maxY;
      double maxZ = box.maxZ;
      tessellator.addVertex(minX, minY, minZ);
      tessellator.addVertex(maxX, minY, minZ);
      tessellator.addVertex(maxX, minY, maxZ);
      tessellator.addVertex(minX, minY, maxZ);
      tessellator.addVertex(minX, maxY, maxZ);
      tessellator.addVertex(maxX, maxY, maxZ);
      tessellator.addVertex(maxX, maxY, minZ);
      tessellator.addVertex(minX, maxY, minZ);
      tessellator.addVertex(minX, minY, minZ);
      tessellator.addVertex(minX, maxY, minZ);
      tessellator.addVertex(maxX, maxY, minZ);
      tessellator.addVertex(maxX, minY, minZ);
      tessellator.addVertex(maxX, minY, maxZ);
      tessellator.addVertex(maxX, maxY, maxZ);
      tessellator.addVertex(minX, maxY, maxZ);
      tessellator.addVertex(minX, minY, maxZ);
      tessellator.addVertex(minX, minY, maxZ);
      tessellator.addVertex(minX, maxY, maxZ);
      tessellator.addVertex(minX, maxY, minZ);
      tessellator.addVertex(minX, minY, minZ);
      tessellator.addVertex(maxX, minY, minZ);
      tessellator.addVertex(maxX, maxY, minZ);
      tessellator.addVertex(maxX, maxY, maxZ);
      tessellator.addVertex(maxX, minY, maxZ);
      tessellator.draw();
   }
}
