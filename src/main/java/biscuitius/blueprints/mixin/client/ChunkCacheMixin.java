package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.world.World;
import net.minecraft.core.world.chunk.ChunkCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkCache.class)
public abstract class ChunkCacheMixin {
   @Shadow
   @Final
   private World worldObj;

   @Inject(method = "isBlockOpaqueCube", at = @At("HEAD"), cancellable = true)
   private void blueprints$ghostNotOpaque(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
      if (GhostBlockState.hasSectionGhosts(this.worldObj, x, y, z)) {
         if (GhostBlockState.isGhostBlock(this.worldObj, x, y, z)) {
            if (GhostBlockState.isRenderingGhostBlock() && !GhostBlockState.isRenderingWrongBlock()) {
               int ghostId = this.worldObj.getBlockId(x, y, z);
               Block<?> ghostBlock = Blocks.blocksList[ghostId];
               cir.setReturnValue(ghostBlock != null && ghostBlock.isSolidRender());
            } else {
               int ghostId = this.worldObj.getBlockId(x, y, z);
               Block<?> ghostBlock = Blocks.blocksList[ghostId];
               boolean ghostOpaque = ghostBlock != null && ghostBlock.isSolidRender();
               int serverId = GhostBlockState.getServerBlockId(this.worldObj, x, y, z);
               Block<?> serverBlock = serverId > 0 ? Blocks.blocksList[serverId] : null;
               boolean serverOpaque = serverBlock != null && serverBlock.isSolidRender();
               cir.setReturnValue(ghostOpaque && serverOpaque);
            }
         }
      }
   }
}
