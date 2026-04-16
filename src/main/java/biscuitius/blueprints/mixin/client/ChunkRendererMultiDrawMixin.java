package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.client.render.block.model.BlockModel;
import net.minecraft.client.render.terrain.ChunkRenderer;
import net.minecraft.client.render.terrain.ChunkRendererMultiDraw;
import net.minecraft.core.world.chunk.ChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkRendererMultiDraw.class)
public abstract class ChunkRendererMultiDrawMixin extends ChunkRenderer {
   private ChunkRendererMultiDrawMixin() {
      super(null, null, 0, 0, 0, 0, 0);
   }

   @Inject(method = "rebuild", at = @At("HEAD"))
   private void blueprints$setRebuildContext(CallbackInfo ci) {
      GhostBlockState.setChunkRebuildWorld(this.world);
   }

   @Inject(method = "rebuild", at = @At("RETURN"))
   private void blueprints$clearRebuildContext(CallbackInfo ci) {
      GhostBlockState.clearChunkRebuildWorld();
   }

   @Redirect(method = "rebuild", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/world/chunk/ChunkCache;getBlockId(III)I"))
   private int blueprints$captureBlockPos(ChunkCache cache, int x, int y, int z) {
      GhostBlockState.recordBlockPos(x, y, z);
      return cache.getBlockId(x, y, z);
   }

   @Redirect(method = "rebuild", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/model/BlockModel;renderLayer()I"))
   private int blueprints$ghostRenderLayer(BlockModel model) {
      return GhostBlockState.shouldRenderAsTranslucent() ? 1 : model.renderLayer();
   }
}
