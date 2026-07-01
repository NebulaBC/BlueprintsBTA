package biscuitius.blueprints.client.hologram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.world.World;

public final class HologramStore {
   private static final Map<World, Map<Long, HologramBlock>> BLOCKS_BY_WORLD = new IdentityHashMap<>();
   private static final Map<World, Set<Long>> SECTIONS_BY_WORLD = new IdentityHashMap<>();
   private static final Map<World, Map<Long, int[]>> SECTION_COUNTS = new IdentityHashMap<>();
   private static final Map<World, int[]> STICKY_BOUNDS = new IdentityHashMap<>();
   private static final List<HologramListener> LISTENERS = new ArrayList<>(2);

   private HologramStore() {
   }

   public static long packPos(int x, int y, int z) {
      return (x & 67108863L) << 38 | (z & 67108863L) << 12 | y & 4095L;
   }

   public static long packSection(int x, int y, int z) {
      return packPos(x >> 4, y >> 4, z >> 4);
   }

   public static int unpackX(long packed) {
      int x = (int)(packed >> 38);
      if ((x & 33554432) != 0) {
         x |= -67108864;
      }

      return x;
   }

   public static int unpackY(long packed) {
      return (int)(packed & 4095L);
   }

   public static int unpackZ(long packed) {
      int z = (int)(packed >> 12 & 67108863L);
      if ((z & 33554432) != 0) {
         z |= -67108864;
      }

      return z;
   }

   public static void addListener(HologramListener listener) {
      if (listener != null && !LISTENERS.contains(listener)) {
         LISTENERS.add(listener);
      }
   }

   public static void removeListener(HologramListener listener) {
      LISTENERS.remove(listener);
   }

   private static void fireChanged(World world, int x, int y, int z, HologramBlock previous, HologramBlock current) {
      int i = 0;

      for (int n = LISTENERS.size(); i < n; i++) {
         LISTENERS.get(i).onHologramChanged(world, x, y, z, previous, current);
      }
   }

   private static void fireRegion(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      int i = 0;

      for (int n = LISTENERS.size(); i < n; i++) {
         LISTENERS.get(i).onRegionChanged(world, minX, minY, minZ, maxX, maxY, maxZ);
      }
   }

   private static void fireCleared(World world) {
      int i = 0;

      for (int n = LISTENERS.size(); i < n; i++) {
         LISTENERS.get(i).onWorldCleared(world);
      }
   }

   private static void addSectionRef(World world, int x, int y, int z) {
      long sectionKey = packSection(x, y, z);
      SECTIONS_BY_WORLD.computeIfAbsent(world, w -> new HashSet<>()).add(sectionKey);
      Map<Long, int[]> counts = SECTION_COUNTS.computeIfAbsent(world, w -> new HashMap<>());
      int[] count = counts.get(sectionKey);
      if (count == null) {
         counts.put(sectionKey, new int[]{1});
      } else {
         count[0]++;
      }
   }

   private static void removeSectionRef(World world, int x, int y, int z) {
      Map<Long, int[]> counts = SECTION_COUNTS.get(world);
      if (counts != null) {
         long sectionKey = packSection(x, y, z);
         int[] count = counts.get(sectionKey);
         if (count != null) {
            if (--count[0] <= 0) {
               counts.remove(sectionKey);
               Set<Long> sections = SECTIONS_BY_WORLD.get(world);
               if (sections != null) {
                  sections.remove(sectionKey);
                  if (sections.isEmpty()) {
                     SECTIONS_BY_WORLD.remove(world);
                     SECTION_COUNTS.remove(world);
                  }
               }
            }
         }
      }
   }

   public static boolean hasSectionHolograms(World world, int x, int y, int z) {
      Set<Long> sections = SECTIONS_BY_WORLD.get(world);
      return sections != null && !sections.isEmpty() ? sections.contains(packSection(x, y, z)) : false;
   }

   private static void expandStickyBounds(World world, int x, int y, int z) {
      int[] b = STICKY_BOUNDS.get(world);
      if (b == null) {
         STICKY_BOUNDS.put(world, new int[]{x, y, z, x, y, z});
      } else {
         if (x < b[0]) {
            b[0] = x;
         }

         if (y < b[1]) {
            b[1] = y;
         }

         if (z < b[2]) {
            b[2] = z;
         }

         if (x > b[3]) {
            b[3] = x;
         }

         if (y > b[4]) {
            b[4] = y;
         }

         if (z > b[5]) {
            b[5] = z;
         }
      }
   }

   public static void recomputeBounds(World world) {
      if (world != null) {
         Map<Long, HologramBlock> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null && !worldBlocks.isEmpty()) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (long packed : worldBlocks.keySet()) {
               int x = unpackX(packed);
               int y = unpackY(packed);
               int z = unpackZ(packed);
               if (x < minX) {
                  minX = x;
               }

               if (x > maxX) {
                  maxX = x;
               }

               if (y < minY) {
                  minY = y;
               }

               if (y > maxY) {
                  maxY = y;
               }

               if (z < minZ) {
                  minZ = z;
               }

               if (z > maxZ) {
                  maxZ = z;
               }
            }

            STICKY_BOUNDS.put(world, new int[]{minX, minY, minZ, maxX, maxY, maxZ});
         } else {
            STICKY_BOUNDS.remove(world);
         }
      }
   }

   public static Set<Long> getHologramSectionKeys(World world) {
      Set<Long> sections = SECTIONS_BY_WORLD.get(world);
      return sections == null ? Collections.emptySet() : Collections.unmodifiableSet(sections);
   }

   public static HologramBlock put(World world, int x, int y, int z, HologramBlock block) {
      if (world == null || y < 0 || y >= 256) {
         return null;
      } else if (block == null) {
         return remove(world, x, y, z);
      } else {
         Map<Long, HologramBlock> worldBlocks = BLOCKS_BY_WORLD.computeIfAbsent(world, w -> new HashMap<>());
         long key = packPos(x, y, z);
         HologramBlock previous = worldBlocks.put(key, block);
         if (previous == null) {
            addSectionRef(world, x, y, z);
         }

         expandStickyBounds(world, x, y, z);
         fireChanged(world, x, y, z, previous, block);
         return previous;
      }
   }

   public static HologramBlock remove(World world, int x, int y, int z) {
      if (world == null) {
         return null;
      } else {
         Map<Long, HologramBlock> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks == null) {
            return null;
         } else {
            long key = packPos(x, y, z);
            HologramBlock previous = worldBlocks.remove(key);
            if (previous == null) {
               return null;
            } else {
               removeSectionRef(world, x, y, z);
               if (worldBlocks.isEmpty()) {
                  BLOCKS_BY_WORLD.remove(world);
               }

               fireChanged(world, x, y, z, previous, null);
               return previous;
            }
         }
      }
   }

   public static HologramBlock get(World world, int x, int y, int z) {
      if (world == null) {
         return null;
      } else if (!hasSectionHolograms(world, x, y, z)) {
         return null;
      } else {
         Map<Long, HologramBlock> worldBlocks = BLOCKS_BY_WORLD.get(world);
         return worldBlocks == null ? null : worldBlocks.get(packPos(x, y, z));
      }
   }

   public static boolean contains(World world, int x, int y, int z) {
      return get(world, x, y, z) != null;
   }

   public static int getBlockId(World world, int x, int y, int z) {
      HologramBlock b = get(world, x, y, z);
      return b == null ? 0 : b.blockId;
   }

   public static int getMetadata(World world, int x, int y, int z) {
      HologramBlock b = get(world, x, y, z);
      return b == null ? 0 : b.metadata;
   }

   public static void bulkFill(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, HologramBlock block) {
      if (world != null) {
         minY = Math.max(0, minY);
         maxY = Math.min(255, maxY);
         if (minY <= maxY && minX <= maxX && minZ <= maxZ) {
            if (block == null) {
               Map<Long, HologramBlock> worldBlocks = BLOCKS_BY_WORLD.get(world);
               if (worldBlocks == null || worldBlocks.isEmpty()) {
                  return;
               }

               for (int x = minX; x <= maxX; x++) {
                  for (int z = minZ; z <= maxZ; z++) {
                     for (int y = minY; y <= maxY; y++) {
                        long key = packPos(x, y, z);
                        if (worldBlocks.remove(key) != null) {
                           removeSectionRef(world, x, y, z);
                        }
                     }
                  }
               }

               if (worldBlocks.isEmpty()) {
                  BLOCKS_BY_WORLD.remove(world);
               }
            } else {
               Map<Long, HologramBlock> worldBlocksx = BLOCKS_BY_WORLD.computeIfAbsent(world, w -> new HashMap<>());

               for (int x = minX; x <= maxX; x++) {
                  for (int z = minZ; z <= maxZ; z++) {
                     for (int yx = minY; yx <= maxY; yx++) {
                        long key = packPos(x, yx, z);
                        if (worldBlocksx.put(key, block) == null) {
                           addSectionRef(world, x, yx, z);
                        }
                     }
                  }
               }
            }

            recomputeBounds(world);
            fireRegion(world, minX, minY, minZ, maxX, maxY, maxZ);
         }
      }
   }

   public static void shiftAll(World world, int dx, int dy, int dz) {
      if (world != null && (dx != 0 || dy != 0 || dz != 0)) {
         Map<Long, HologramBlock> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null && !worldBlocks.isEmpty()) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            long[] keys = new long[worldBlocks.size()];
            HologramBlock[] values = new HologramBlock[worldBlocks.size()];
            int i = 0;

            for (Entry<Long, HologramBlock> e : worldBlocks.entrySet()) {
               keys[i] = e.getKey();
               values[i] = e.getValue();
               int x = unpackX(keys[i]);
               int y = unpackY(keys[i]);
               int z = unpackZ(keys[i]);
               if (x < minX) {
                  minX = x;
               }

               if (x > maxX) {
                  maxX = x;
               }

               if (y < minY) {
                  minY = y;
               }

               if (y > maxY) {
                  maxY = y;
               }

               if (z < minZ) {
                  minZ = z;
               }

               if (z > maxZ) {
                  maxZ = z;
               }

               i++;
            }

            worldBlocks.clear();
            SECTIONS_BY_WORLD.remove(world);
            SECTION_COUNTS.remove(world);

            for (int j = 0; j < keys.length; j++) {
               int nx = unpackX(keys[j]) + dx;
               int ny = unpackY(keys[j]) + dy;
               int nz = unpackZ(keys[j]) + dz;
               if (ny >= 0 && ny < 256) {
                  worldBlocks.put(packPos(nx, ny, nz), values[j]);
                  addSectionRef(world, nx, ny, nz);
               }
            }

            if (worldBlocks.isEmpty()) {
               BLOCKS_BY_WORLD.remove(world);
            }

            recomputeBounds(world);
            fireRegion(
               world,
               Math.min(minX, minX + dx),
               Math.min(minY, minY + dy),
               Math.min(minZ, minZ + dz),
               Math.max(maxX, maxX + dx),
               Math.max(maxY, maxY + dy),
               Math.max(maxZ, maxZ + dz)
            );
         }
      }
   }

   public static void clearWorld(World world) {
      if (world != null) {
         STICKY_BOUNDS.remove(world);
         if (BLOCKS_BY_WORLD.remove(world) == null) {
            SECTIONS_BY_WORLD.remove(world);
            SECTION_COUNTS.remove(world);
         } else {
            SECTIONS_BY_WORLD.remove(world);
            SECTION_COUNTS.remove(world);
            fireCleared(world);
         }
      }
   }

   public static void clearAll() {
      if (!BLOCKS_BY_WORLD.isEmpty()) {
         World[] worlds = BLOCKS_BY_WORLD.keySet().toArray(new World[0]);
         BLOCKS_BY_WORLD.clear();
         SECTIONS_BY_WORLD.clear();
         SECTION_COUNTS.clear();
         STICKY_BOUNDS.clear();

         for (World w : worlds) {
            fireCleared(w);
         }
      }
   }

   public static boolean hasEntries(World world) {
      if (world == null) {
         return false;
      } else {
         Map<Long, HologramBlock> worldBlocks = BLOCKS_BY_WORLD.get(world);
         return worldBlocks != null && !worldBlocks.isEmpty();
      }
   }

   public static int size(World world) {
      if (world == null) {
         return 0;
      } else {
         Map<Long, HologramBlock> worldBlocks = BLOCKS_BY_WORLD.get(world);
         return worldBlocks == null ? 0 : worldBlocks.size();
      }
   }

   public static int[] getBounds(World world) {
      if (world == null) {
         return null;
      } else {
         int[] b = STICKY_BOUNDS.get(world);
         return b == null ? null : new int[]{b[0], b[1], b[2], b[3], b[4], b[5]};
      }
   }

   public static int[] getYRange(World world) {
      if (world == null) {
         return null;
      } else {
         Map<Long, HologramBlock> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null && !worldBlocks.isEmpty()) {
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;

            for (long packed : worldBlocks.keySet()) {
               int py = unpackY(packed);
               if (py < minY) {
                  minY = py;
               }

               if (py > maxY) {
                  maxY = py;
               }
            }

            return new int[]{minY, maxY};
         } else {
            return null;
         }
      }
   }

   public static void forEach(World world, HologramStore.HologramConsumer consumer) {
      if (world != null && consumer != null) {
         Map<Long, HologramBlock> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null) {
            for (Entry<Long, HologramBlock> e : worldBlocks.entrySet()) {
               long packed = e.getKey();
               consumer.accept(unpackX(packed), unpackY(packed), unpackZ(packed), e.getValue());
            }
         }
      }
   }

   public static Map<Long, HologramBlock> rawView(World world) {
      if (world == null) {
         return Collections.emptyMap();
      } else {
         Map<Long, HologramBlock> worldBlocks = BLOCKS_BY_WORLD.get(world);
         return worldBlocks == null ? Collections.emptyMap() : Collections.unmodifiableMap(worldBlocks);
      }
   }

   @FunctionalInterface
   public interface HologramConsumer {
      void accept(int var1, int var2, int var3, HologramBlock var4);
   }
}
