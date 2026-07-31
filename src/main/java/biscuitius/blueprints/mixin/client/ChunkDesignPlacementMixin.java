package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.hologram.HologramPlacementContext;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.world.World;
import net.minecraft.core.world.chunk.Chunk;
import net.minecraft.core.world.pos.ChunkTilePosc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Chunk.class)
public abstract class ChunkDesignPlacementMixin {
   @Shadow
   @Final
   public World world;

   @Inject(
      method = "setTileEntity(Lnet/minecraft/core/world/pos/ChunkTilePosc;Lnet/minecraft/core/block/entity/TileEntity;)Z",
      at = @At("HEAD"),
      cancellable = true
   )
   private void blueprints$skipDesignPlacementTileEntity(ChunkTilePosc tilePos, TileEntity tileEntity, CallbackInfoReturnable<Boolean> cir) {
      if (HologramPlacementContext.isActive(this.world)) {
         cir.setReturnValue(true);
      }
   }
}
