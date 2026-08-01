package biscuitius.blueprints.client.hologram;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePosc;

public final class HologramChunkCache extends OverlayChunkCache {
   public HologramChunkCache(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      super(world, minX, minY, minZ, maxX, maxY, maxZ);
   }

   @Override
   protected HologramBlock overlay(int x, int y, int z) {
      return !HologramAppearance.isYVisible(y) ? null : HologramStore.get(this.world, x, y, z);
   }

   @Override
   public Block<?> getBlockType(TilePosc pos) {
      if (this.overlay(pos.x(), pos.y(), pos.z()) == null) {
         Block<?> real = super.getBlockType(pos);
         return isRealRedstoneConnector(real) ? Blocks.AIR : real;
      } else {
         return super.getBlockType(pos);
      }
   }

   @Override
   public int getBlockData(TilePosc pos) {
      return this.overlay(pos.x(), pos.y(), pos.z()) == null && isRealRedstoneConnector(super.getBlockType(pos)) ? 0 : super.getBlockData(pos);
   }

   private static boolean isRealRedstoneConnector(Block<?> b) {
      if (b == null || b == Blocks.AIR) {
         return false;
      } else {
         return b.isSignalSource()
            ? true
            : b == Blocks.WIRE_REDSTONE
               || b == Blocks.CONDUIT
               || b == Blocks.REPEATER_IDLE
               || b == Blocks.REPEATER_ACTIVE
               || b == Blocks.MATCHER
               || b == Blocks.MATCHER_ACTIVE
               || b == Blocks.PUMPKIN_REDSTONE
               || b == Blocks.TIMER;
      }
   }
}
