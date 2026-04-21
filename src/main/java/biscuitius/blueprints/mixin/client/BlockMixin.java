package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockMixin {
   @Inject(method = "dropBlockWithCause", at = @At("HEAD"), cancellable = true)
   private void blueprints$preventGhostDrop(
      World world, EnumDropCause cause, int x, int y, int z, int meta, TileEntity tileEntity, Player player, CallbackInfo ci
   ) {
      if (world != null && GhostBlockState.isGhostBlock(world, x, y, z)) {
         ci.cancel();
      }
   }

   @Inject(method = "onBlockDestroyedByExplosion", at = @At("HEAD"), cancellable = true)
   private void blueprints$preventGhostExplosion(World world, int x, int y, int z, CallbackInfo ci) {
      if (world != null && GhostBlockState.isGhostBlock(world, x, y, z)) {
         ci.cancel();
      }
   }
}
