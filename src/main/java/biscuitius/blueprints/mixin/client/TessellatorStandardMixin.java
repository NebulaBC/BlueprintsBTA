package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.client.render.tessellator.TessellatorStandard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TessellatorStandard.class)
public abstract class TessellatorStandardMixin {
   @Unique
   private static final int FULL_BRIGHT_LIGHTMAP = 15728880;

   @ModifyVariable(method = "setLightmapCoord", at = @At("HEAD"), argsOnly = true)
   private int blueprints$ghostFullBrightLightmap(int lightmapCoord) {
      return GhostBlockState.isRenderingGhostBlock() && !GhostBlockState.isRenderingWrongBlock() && !GhostBlockState.isRenderingReplaceableWrongBlock()
         ? 15728880
         : lightmapCoord;
   }
}
