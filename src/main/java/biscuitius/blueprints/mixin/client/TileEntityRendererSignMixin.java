package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.client.render.tileentity.TileEntityRendererSign;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TileEntityRendererSign.class)
public abstract class TileEntityRendererSignMixin {
   @Redirect(
      method = "doRender(Lnet/minecraft/client/render/tessellator/Tessellator;Lnet/minecraft/core/block/entity/TileEntitySign;DDDF)V",
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V")
   )
   private void blueprints$keepBlendEnabled(int cap) {
      if (cap != 3042 || !GhostBlockState.isRenderingGhostBlock() || GhostBlockState.isRenderingWrongBlock()) {
         GL11.glDisable(cap);
      }
   }

   @Redirect(
      method = "doRender(Lnet/minecraft/client/render/tessellator/Tessellator;Lnet/minecraft/core/block/entity/TileEntitySign;DDDF)V",
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V")
   )
   private void blueprints$maintainGhostColor(float r, float g, float b, float a) {
      if (GhostBlockState.isRenderingGhostBlock() && !GhostBlockState.isRenderingWrongBlock()) {
         GL11.glColor4f(
            GhostBlockState.getHologramR() / 255.0F,
            GhostBlockState.getHologramG() / 255.0F,
            GhostBlockState.getHologramB() / 255.0F,
            GhostBlockState.getHologramA() / 255.0F
         );
      } else {
         GL11.glColor4f(r, g, b, a);
      }
   }
}
