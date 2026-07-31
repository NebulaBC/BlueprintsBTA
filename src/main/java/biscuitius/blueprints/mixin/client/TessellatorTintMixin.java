package biscuitius.blueprints.mixin.client;

import net.minecraft.client.render.tessellator.TessellatorShader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TessellatorShader.class)
public abstract class TessellatorTintMixin {
   @ModifyVariable(method = "setColor1i", at = @At("HEAD"), argsOnly = true, index = 1)
   private int blueprints$tintColor(int argb) {
      return argb;
   }
}
