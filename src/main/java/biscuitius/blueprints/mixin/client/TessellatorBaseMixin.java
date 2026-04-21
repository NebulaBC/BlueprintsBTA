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
         if (!GhostBlockState.isRenderingReplaceableWrongBlock()) {
            if (GhostBlockState.isRenderingWrongBlock()) {
               int alpha = GhostBlockState.isWrongBlockOverlay() ? GhostBlockState.getHologramA() : 255;
               this.setColorRGBA((int)(r * 0.65F + 89.25F), (int)(g * 0.65F), (int)(b * 0.65F), alpha);
            } else {
               int alpha = GhostBlockState.isLastBlockNonSolid() ? 255 : GhostBlockState.getHologramA();
               float sat = GhostBlockState.getHologramSaturation();
               int hr = GhostBlockState.getHologramR();
               int hg = GhostBlockState.getHologramG();
               int hb = GhostBlockState.getHologramB();
               int fr = (int)(r * (1.0F - sat) + hr * sat);
               int fg = (int)(g * (1.0F - sat) + hg * sat);
               int fb = (int)(b * (1.0F - sat) + hb * sat);
               this.setColorRGBA(Math.min(fr, 255), Math.min(fg, 255), Math.min(fb, 255), alpha);
            }

            ci.cancel();
         }
      }
   }

   @Inject(method = "setColorRGBA_F", at = @At("HEAD"), cancellable = true)
   private void blueprints$ghostColorRGBA_F(float r, float g, float b, float a, CallbackInfo ci) {
      if (GhostBlockState.isRenderingGhostBlock()) {
         if (!GhostBlockState.isRenderingReplaceableWrongBlock()) {
            if (GhostBlockState.isRenderingWrongBlock()) {
               int alpha = GhostBlockState.isWrongBlockOverlay() ? GhostBlockState.getHologramA() : 255;
               int ri = (int)(r * 255.0F * 0.65F + 89.25F);
               int gi = (int)(g * 255.0F * 0.65F);
               int bi = (int)(b * 255.0F * 0.65F);
               this.setColorRGBA(Math.min(ri, 255), Math.min(gi, 255), Math.min(bi, 255), alpha);
            } else {
               int alpha = GhostBlockState.isLastBlockNonSolid() ? 255 : GhostBlockState.getHologramA();
               float sat = GhostBlockState.getHologramSaturation();
               int hr = GhostBlockState.getHologramR();
               int hg = GhostBlockState.getHologramG();
               int hb = GhostBlockState.getHologramB();
               int fr = (int)(r * 255.0F * (1.0F - sat) + hr * sat);
               int fg = (int)(g * 255.0F * (1.0F - sat) + hg * sat);
               int fb = (int)(b * 255.0F * (1.0F - sat) + hb * sat);
               this.setColorRGBA(Math.min(fr, 255), Math.min(fg, 255), Math.min(fb, 255), alpha);
            }

            ci.cancel();
         }
      }
   }
}
