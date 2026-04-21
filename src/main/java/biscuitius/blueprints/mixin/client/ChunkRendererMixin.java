package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.block.model.BlockModel;
import net.minecraft.client.render.block.model.BlockModelDispatcher;
import net.minecraft.client.render.terrain.ChunkRenderer;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkRenderer.class)
public abstract class ChunkRendererMixin {
   @Shadow
   public World world;
   @Unique
   private boolean blueprints$renderingGhostOverlay;

   @Shadow
   public abstract boolean renderBlock(Tessellator var1, RenderBlocks var2, BlockModel<?> var3, int var4, int var5, int var6);

   @Inject(method = "renderBlock", at = @At("HEAD"))
   private void blueprints$setGhostRenderFlag(
      Tessellator tessellator, RenderBlocks renderBlocks, BlockModel model, int x, int y, int z, CallbackInfoReturnable<Boolean> cir
   ) {
      if (!this.blueprints$renderingGhostOverlay) {
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
   }

   @Inject(method = "renderBlock", at = @At("RETURN"))
   private void blueprints$clearGhostRenderFlag(
      Tessellator tessellator, RenderBlocks renderBlocks, BlockModel model, int x, int y, int z, CallbackInfoReturnable<Boolean> cir
   ) {
      if (!this.blueprints$renderingGhostOverlay) {
         int prevMode = GhostBlockState.isRenderingReplaceableWrongBlock() ? 3 : 0;
         GhostBlockState.setGhostRenderMode(0);
         if (prevMode == 3 && this.world != null) {
            int ghostId = GhostBlockState.getGhostBlockId(this.world, x, y, z);
            int ghostMeta = GhostBlockState.getGhostMetadata(this.world, x, y, z);
            if (ghostId > 0) {
               Block<?> ghostBlock = Blocks.blocksList[ghostId];
               if (ghostBlock != null) {
                  int serverId = GhostBlockState.getServerBlockId(this.world, x, y, z);
                  int serverMeta = this.world.getBlockMetadata(x, y, z);
                  GhostBlockState.setBlockNoLighting(this.world, x, y, z, ghostId, ghostMeta);
                  GhostBlockState.setGhostRenderMode(2);
                  GhostBlockState.setWrongBlockOverlay(true);
                  this.blueprints$renderingGhostOverlay = true;

                  try {
                     BlockModel<?> ghostModel = (BlockModel<?>)BlockModelDispatcher.getInstance().getDispatch(ghostBlock);
                     this.renderBlock(tessellator, renderBlocks, ghostModel, x, y, z);
                  } finally {
                     this.blueprints$renderingGhostOverlay = false;
                     GhostBlockState.setWrongBlockOverlay(false);
                     GhostBlockState.setGhostRenderMode(0);
                     GhostBlockState.setBlockNoLighting(this.world, x, y, z, serverId, serverMeta);
                  }
               }
            }
         }
      }
   }
}
