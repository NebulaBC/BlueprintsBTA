package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.tessellator.TessellatorBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TessellatorBase.class)
public abstract class TessellatorBaseMixin extends Tessellator {
   @Unique
   private static final float WRONG_TINT = 0.35F;

   @Inject(method = "setColorOpaque", at = @At("HEAD"), cancellable = true)
   private void blueprints$ghostColorOpaque(int r, int g, int b, CallbackInfo ci) {
      if (GhostBlockState.isRenderingGhostBlock()) {
         if (GhostBlockState.isRenderingWrongBlock()) {
            this.setColorRGBA((int)(r * 0.65F + 89.25F), (int)(g * 0.65F), (int)(b * 0.65F), 255);
         } else {
            int alpha = GhostBlockState.isLastBlockNonSolid() ? 255 : GhostBlockState.getHologramA();
            this.setColorRGBA(GhostBlockState.getHologramR(), GhostBlockState.getHologramG(), GhostBlockState.getHologramB(), alpha);
         }

         ci.cancel();
      }
   }

   @Inject(method = "setColorRGBA_F", at = @At("HEAD"), cancellable = true)
   private void blueprints$ghostColorRGBA_F(float r, float g, float b, float a, CallbackInfo ci) {
      if (GhostBlockState.isRenderingGhostBlock()) {
         if (GhostBlockState.isRenderingWrongBlock()) {
            int ri = (int)(r * 255.0F * 0.65F + 89.25F);
            int gi = (int)(g * 255.0F * 0.65F);
            int bi = (int)(b * 255.0F * 0.65F);
            this.setColorRGBA(Math.min(ri, 255), Math.min(gi, 255), Math.min(bi, 255), 255);
         } else {
            int alpha = GhostBlockState.isLastBlockNonSolid() ? 255 : GhostBlockState.getHologramA();
            this.setColorRGBA(GhostBlockState.getHologramR(), GhostBlockState.getHologramG(), GhostBlockState.getHologramB(), alpha);
         }

         ci.cancel();
      }
   }
}
