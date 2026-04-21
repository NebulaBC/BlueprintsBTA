package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.core.block.BlockLogicMud;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.tag.BlockTags;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockLogicMud.class)
public abstract class BlockLogicMudMixin {
   @Inject(method = "canFallBelow", at = @At("HEAD"), cancellable = true)
   private static void blueprints$ghostCanFallBelow(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
      if (GhostBlockState.hasSectionGhosts(world, x, y, z)) {
         if (GhostBlockState.isGhostBlock(world, x, y, z)) {
            int serverId = GhostBlockState.getServerBlockId(world, x, y, z);
            if (serverId == 0) {
               cir.setReturnValue(true);
            } else if (serverId == Blocks.FIRE.id()) {
               cir.setReturnValue(true);
            } else {
               cir.setReturnValue(Blocks.hasTag(serverId, BlockTags.IS_WATER) || Blocks.hasTag(serverId, BlockTags.IS_LAVA));
            }
         }
      }
   }
}
