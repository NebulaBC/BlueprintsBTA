package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.client.render.LightmapHelper;
import net.minecraft.client.render.TileEntityRenderDispatcher;
import net.minecraft.client.render.camera.ICamera;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.world.World;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityRenderDispatcher.class)
public abstract class TileEntityRenderDispatcherMixin {
   @Shadow
   public World worldObj;
   @Unique
   private int blueprints$teGhostMode = 0;
   @Unique
   private static final float WRONG_TINT = 0.35F;

   @Inject(
      method = "renderTileEntity(Lnet/minecraft/client/render/tessellator/Tessellator;Lnet/minecraft/client/render/camera/ICamera;Lnet/minecraft/core/block/entity/TileEntity;F)V",
      at = @At("HEAD")
   )
   private void blueprints$detectGhostTE(Tessellator t, ICamera cam, TileEntity te, float pt, CallbackInfo ci) {
      this.blueprints$teGhostMode = !DesignModeState.isActive() && !GhostBlockState.isHidden() && this.worldObj != null
         ? GhostBlockState.getGhostRenderMode(this.worldObj, te.x, te.y, te.z)
         : 0;
   }

   @Redirect(
      method = "renderTileEntity(Lnet/minecraft/client/render/tessellator/Tessellator;Lnet/minecraft/client/render/camera/ICamera;Lnet/minecraft/core/block/entity/TileEntity;F)V",
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor3f(FFF)V")
   )
   private void blueprints$ghostTEColor(float r, float g, float b) {
      if (this.blueprints$teGhostMode == 1) {
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glColor4f(
            GhostBlockState.getHologramR() / 255.0F,
            GhostBlockState.getHologramG() / 255.0F,
            GhostBlockState.getHologramB() / 255.0F,
            GhostBlockState.getHologramA() / 255.0F
         );
         GhostBlockState.setGhostRenderMode(1);
      } else if (this.blueprints$teGhostMode == 2) {
         GL11.glColor3f(r * 0.65F + 0.35F, g * 0.65F, b * 0.65F);
         GhostBlockState.setGhostRenderMode(2);
      } else {
         GL11.glColor3f(r, g, b);
      }
   }

   @Redirect(
      method = "renderTileEntity(Lnet/minecraft/client/render/tessellator/Tessellator;Lnet/minecraft/client/render/camera/ICamera;Lnet/minecraft/core/block/entity/TileEntity;F)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/LightmapHelper;setLightmapCoord(I)V")
   )
   private void blueprints$ghostFullBrightLightmap(int coord) {
      LightmapHelper.setLightmapCoord(this.blueprints$teGhostMode == 1 ? 15728880 : coord);
   }

   @Inject(
      method = "renderTileEntity(Lnet/minecraft/client/render/tessellator/Tessellator;Lnet/minecraft/client/render/camera/ICamera;Lnet/minecraft/core/block/entity/TileEntity;F)V",
      at = @At("RETURN")
   )
   private void blueprints$clearGhostTE(Tessellator t, ICamera cam, TileEntity te, float pt, CallbackInfo ci) {
      if (this.blueprints$teGhostMode > 0) {
         if (this.blueprints$teGhostMode == 1) {
            GL11.glDisable(3042);
         }

         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GhostBlockState.setGhostRenderMode(0);
         this.blueprints$teGhostMode = 0;
      }
   }
}
