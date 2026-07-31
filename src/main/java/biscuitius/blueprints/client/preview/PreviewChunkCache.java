package biscuitius.blueprints.client.preview;

import biscuitius.blueprints.client.hologram.HologramAppearance;
import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.OverlayChunkCache;
import net.minecraft.core.world.World;

public final class PreviewChunkCache extends OverlayChunkCache {
   public PreviewChunkCache(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      super(world, minX, minY, minZ, maxX, maxY, maxZ);
   }

   @Override
   protected HologramBlock overlay(int x, int y, int z) {
      return !HologramAppearance.isYVisible(y) ? null : PreviewStore.get(this.world, x, y, z);
   }
}
