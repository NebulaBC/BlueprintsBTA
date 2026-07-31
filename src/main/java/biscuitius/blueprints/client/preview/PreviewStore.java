package biscuitius.blueprints.client.preview;

import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.HologramStore;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.world.World;

public final class PreviewStore {
   private static final Map<World, Map<Long, HologramBlock>> BLOCKS_BY_WORLD = new IdentityHashMap<>();

   private PreviewStore() {
   }

   public static void setPreview(World world, Map<Long, HologramBlock> blocks) {
      if (world != null) {
         if (blocks != null && !blocks.isEmpty()) {
            BLOCKS_BY_WORLD.put(world, new LinkedHashMap<>(blocks));
         } else {
            BLOCKS_BY_WORLD.remove(world);
         }
      }
   }

   public static void clear(World world) {
      if (world != null) {
         BLOCKS_BY_WORLD.remove(world);
      }
   }

   public static void clearAll() {
      BLOCKS_BY_WORLD.clear();
   }

   public static boolean hasEntries(World world) {
      Map<Long, HologramBlock> m = BLOCKS_BY_WORLD.get(world);
      return m != null && !m.isEmpty();
   }

   public static int size(World world) {
      Map<Long, HologramBlock> m = BLOCKS_BY_WORLD.get(world);
      return m == null ? 0 : m.size();
   }

   public static HologramBlock get(World world, int x, int y, int z) {
      Map<Long, HologramBlock> m = BLOCKS_BY_WORLD.get(world);
      return m == null ? null : m.get(HologramStore.packPos(x, y, z));
   }

   public static Map<Long, HologramBlock> rawView(World world) {
      Map<Long, HologramBlock> m = BLOCKS_BY_WORLD.get(world);
      return m == null ? Collections.emptyMap() : Collections.unmodifiableMap(m);
   }

   public static int[] getBounds(World world) {
      Map<Long, HologramBlock> m = BLOCKS_BY_WORLD.get(world);
      if (m != null && !m.isEmpty()) {
         int minX = Integer.MAX_VALUE;
         int minY = Integer.MAX_VALUE;
         int minZ = Integer.MAX_VALUE;
         int maxX = Integer.MIN_VALUE;
         int maxY = Integer.MIN_VALUE;
         int maxZ = Integer.MIN_VALUE;

         for (long packed : m.keySet()) {
            int x = HologramStore.unpackX(packed);
            int y = HologramStore.unpackY(packed);
            int z = HologramStore.unpackZ(packed);
            if (x < minX) {
               minX = x;
            }

            if (y < minY) {
               minY = y;
            }

            if (z < minZ) {
               minZ = z;
            }

            if (x > maxX) {
               maxX = x;
            }

            if (y > maxY) {
               maxY = y;
            }

            if (z > maxZ) {
               maxZ = z;
            }
         }

         return new int[]{minX, minY, minZ, maxX, maxY, maxZ};
      } else {
         return null;
      }
   }
}
