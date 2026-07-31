package biscuitius.blueprints.client.hologram;

import net.minecraft.core.world.World;

public final class HologramChunkCache extends OverlayChunkCache {
   public HologramChunkCache(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      super(world, minX, minY, minZ, maxX, maxY, maxZ);
   }

   @Override
   protected HologramBlock overlay(int x, int y, int z) {
      return !HologramAppearance.isYVisible(y) ? null : HologramStore.get(this.world, x, y, z);
   }
}
