package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.block.model.BlockModel;
import net.minecraft.client.render.terrain.ChunkRenderer;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkRenderer.class)
public abstract class ChunkRendererMixin {
   @Shadow
   public World world;

   @Inject(method = "renderBlock", at = @At("HEAD"))
   private void blueprints$setGhostRenderFlag(
      Tessellator tessellator, RenderBlocks renderBlocks, BlockModel model, int x, int y, int z, CallbackInfoReturnable<Boolean> cir
   ) {
      if (!DesignModeState.isActive()) {
         int mode = GhostBlockState.getLastBlockGhostMode();
         if (mode == 0) {
            mode = GhostBlockState.getGhostRenderMode(this.world, x, y, z);
         }

         if (mode > 0) {
            GhostBlockState.setGhostRenderMode(mode);
         }
      }
   }

   @Inject(method = "renderBlock", at = @At("RETURN"))
   private void blueprints$clearGhostRenderFlag(
      Tessellator tessellator, RenderBlocks renderBlocks, BlockModel model, int x, int y, int z, CallbackInfoReturnable<Boolean> cir
   ) {
      GhostBlockState.setGhostRenderMode(0);
   }
}
