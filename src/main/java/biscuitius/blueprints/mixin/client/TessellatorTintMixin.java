package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.hologram.HologramAppearance;
import biscuitius.blueprints.client.hologram.HologramRenderer;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.tessellator.TessellatorBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TessellatorBase.class)
public abstract class TessellatorTintMixin extends Tessellator {
   @Inject(method = "setColorOpaque", at = @At("HEAD"), cancellable = true)
   private void blueprints$tintHologramColorOpaque(int r, int g, int b, CallbackInfo ci) {
      if (HologramRenderer.WRONG_BLOCK_PASS_ACTIVE) {
         this.setColorRGBA(255, 48, 48, 192);
         ci.cancel();
      } else if (HologramRenderer.HOLOGRAM_PASS_ACTIVE) {
         if (!DesignModeState.isActive()) {
            this.applyTint(r, g, b);
            ci.cancel();
         }
      }
   }

   @Inject(method = "setColorRGBA_F", at = @At("HEAD"), cancellable = true)
   private void blueprints$tintHologramColorRGBA_F(float r, float g, float b, float a, CallbackInfo ci) {
      if (HologramRenderer.WRONG_BLOCK_PASS_ACTIVE) {
         this.setColorRGBA(255, 48, 48, 192);
         ci.cancel();
      } else if (HologramRenderer.HOLOGRAM_PASS_ACTIVE) {
         if (!DesignModeState.isActive()) {
            this.applyTint((int)(r * 255.0F), (int)(g * 255.0F), (int)(b * 255.0F));
            ci.cancel();
         }
      }
   }

   private void applyTint(int r, int g, int b) {
      float sat = HologramAppearance.getSaturation();
      int hr = HologramAppearance.getR();
      int hg = HologramAppearance.getG();
      int hb = HologramAppearance.getB();
      int fr = (int)(r * (1.0F - sat) + hr * sat);
      int fg = (int)(g * (1.0F - sat) + hg * sat);
      int fb = (int)(b * (1.0F - sat) + hb * sat);
      this.setColorRGBA(fr > 255 ? 255 : fr, fg > 255 ? 255 : fg, fb > 255 ? 255 : fb, HologramAppearance.getA());
   }
}
