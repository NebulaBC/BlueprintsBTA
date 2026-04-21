package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.RenderBlockCache;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.world.World;
import net.minecraft.core.world.WorldSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderBlockCache.class)
public abstract class RenderBlockCacheMixin {
   @Shadow
   private WorldSource access;
   @Shadow
   private int offsetX;
   @Shadow
   private int offsetY;
   @Shadow
   private int offsetZ;

   @Redirect(method = "getOpacity", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/world/WorldSource;getBlockId(III)I"))
   private int blueprints$serverBlockIdForOpacity(WorldSource source, int x, int y, int z) {
      World world = getWorld();
      if (world != null
         && GhostBlockState.hasSectionGhosts(world, x, y, z)
         && GhostBlockState.isGhostBlock(world, x, y, z)
         && !GhostBlockState.isRenderingGhostBlock()) {
         int ghostId = source.getBlockId(x, y, z);
         boolean ghostSolid = ghostId > 0 && Blocks.blocksList[ghostId] != null && Blocks.blocksList[ghostId].isSolidRender();
         return !ghostSolid ? ghostId : GhostBlockState.getServerBlockId(world, x, y, z);
      } else {
         return source.getBlockId(x, y, z);
      }
   }

   @Redirect(method = "calcBrightness", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/world/WorldSource;getBlockId(III)I"))
   private int blueprints$serverBlockIdForBrightness(WorldSource source, int x, int y, int z) {
      World world = getWorld();
      if (world != null
         && GhostBlockState.hasSectionGhosts(world, x, y, z)
         && GhostBlockState.isGhostBlock(world, x, y, z)
         && !GhostBlockState.isRenderingGhostBlock()) {
         int ghostId = source.getBlockId(x, y, z);
         boolean ghostSolid = ghostId > 0 && Blocks.blocksList[ghostId] != null && Blocks.blocksList[ghostId].isSolidRender();
         return !ghostSolid ? ghostId : GhostBlockState.getServerBlockId(world, x, y, z);
      } else {
         return source.getBlockId(x, y, z);
      }
   }

   private static World getWorld() {
      Minecraft mc = Minecraft.getMinecraft();
      return mc != null ? mc.currentWorld : null;
   }
}
