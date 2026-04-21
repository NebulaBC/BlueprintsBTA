package biscuitius.blueprints.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.render.block.model.BlockModel;
import net.minecraft.client.render.block.model.BlockModelDispatcher;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.BlockLogicBed;
import net.minecraft.core.block.BlockLogicDoor;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.enums.LightLayer;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import net.minecraft.core.world.chunk.Chunk;
import net.minecraft.core.world.chunk.ChunkSection;

public final class GhostBlockState {
   private static final Map<World, Map<Long, GhostBlockState.BlockState>> BLOCKS_BY_WORLD = new IdentityHashMap<>();
   private static int physicsDepth;
   private static final List<int[]> patchedPositions = new ArrayList<>();
   private static boolean suppressLighting;
   private static boolean suppressEntitySpawn;
   private static int lightPatchDepth;
   private static final List<int[]> lightPatchedPositions = new ArrayList<>();
   private static World chunkRebuildWorld;
   private static int lastBlockX;
   private static int lastBlockY;
   private static int lastBlockZ;
   private static int ghostRenderMode;
   private static int lastBlockGhostMode;
   private static boolean lastBlockNonSolid;
   private static boolean wrongBlockOverlay;
   private static final Map<World, Set<Long>> GHOST_SECTIONS = new IdentityHashMap<>();
   private static final Map<World, Map<Long, int[]>> SECTION_COUNTS = new IdentityHashMap<>();
   private static boolean hidden;
   private static int layerCutoffY = Integer.MAX_VALUE;
   private static boolean layerAtMax = true;
   private static float hologramOpacity = 0.85F;
   private static float hologramHue = 0.5F;
   private static float hologramSaturation = 0.0F;
   private static int cachedR;
   private static int cachedG;
   private static int cachedB;
   private static int cachedA;
   private static boolean hasPendingFulfillment;
   private static int pendingFulfillX;
   private static int pendingFulfillY;
   private static int pendingFulfillZ;
   private static int pendingFulfillBlockId;
   private static int pendingFulfillMeta;
   private static boolean fulfillmentInProgress;
   private static final Map<Long, int[]> recentFulfillments = new LinkedHashMap<>();
   private static List<int[]> pendingEntries;
   private static int pendingApplyTicksRemaining;

   private GhostBlockState() {
   }

   private static long packPos(int x, int y, int z) {
      return (x & 67108863L) << 38 | (z & 67108863L) << 12 | y & 4095L;
   }

   private static long packSection(int x, int y, int z) {
      return packPos(x >> 4, y >> 4, z >> 4);
   }

   private static int unpackX(long packed) {
      int x = (int)(packed >> 38);
      if ((x & 33554432) != 0) {
         x |= -67108864;
      }

      return x;
   }

   private static int unpackY(long packed) {
      return (int)(packed & 4095L);
   }

   private static int unpackZ(long packed) {
      int z = (int)(packed >> 12 & 67108863L);
      if ((z & 33554432) != 0) {
         z |= -67108864;
      }

      return z;
   }

   private static void addSectionEntry(World world, int x, int y, int z) {
      long sectionKey = packSection(x, y, z);
      GHOST_SECTIONS.computeIfAbsent(world, w -> new HashSet<>()).add(sectionKey);
      Map<Long, int[]> counts = SECTION_COUNTS.computeIfAbsent(world, w -> new HashMap<>());
      int[] count = counts.get(sectionKey);
      if (count == null) {
         counts.put(sectionKey, new int[]{1});
      } else {
         count[0]++;
      }
   }

   private static void removeSectionEntry(World world, int x, int y, int z) {
      Map<Long, int[]> counts = SECTION_COUNTS.get(world);
      if (counts != null) {
         long sectionKey = packSection(x, y, z);
         int[] count = counts.get(sectionKey);
         if (count != null) {
            if (--count[0] <= 0) {
               counts.remove(sectionKey);
               Set<Long> sections = GHOST_SECTIONS.get(world);
               if (sections != null) {
                  sections.remove(sectionKey);
                  if (sections.isEmpty()) {
                     GHOST_SECTIONS.remove(world);
                     SECTION_COUNTS.remove(world);
                  }
               }
            }
         }
      }
   }

   public static boolean hasSectionGhosts(World world, int x, int y, int z) {
      Set<Long> sections = GHOST_SECTIONS.get(world);
      return sections != null && !sections.isEmpty() ? sections.contains(packSection(x, y, z)) : false;
   }

   public static void setChunkRebuildWorld(World world) {
      chunkRebuildWorld = world;
   }

   public static void clearChunkRebuildWorld() {
      chunkRebuildWorld = null;
   }

   public static void recordBlockPos(int x, int y, int z) {
      lastBlockX = x;
      lastBlockY = y;
      lastBlockZ = z;
      lastBlockGhostMode = 0;
      lastBlockNonSolid = false;
   }

   public static boolean shouldRenderAsTranslucent() {
      if (hidden || DesignModeState.isActive() || chunkRebuildWorld == null) {
         return false;
      } else if (!hasSectionGhosts(chunkRebuildWorld, lastBlockX, lastBlockY, lastBlockZ)) {
         lastBlockGhostMode = 0;
         lastBlockNonSolid = false;
         return false;
      } else {
         int mode = getGhostRenderModeInternal(chunkRebuildWorld, lastBlockX, lastBlockY, lastBlockZ);
         lastBlockGhostMode = mode;
         if (mode == 1) {
            int id = chunkRebuildWorld.getBlockId(lastBlockX, lastBlockY, lastBlockZ);
            Block<?> block = Blocks.blocksList[id];
            boolean nativelyTranslucent = false;
            if (block != null) {
               BlockModel<?> model = (BlockModel<?>)BlockModelDispatcher.getInstance().getDispatch(block);
               nativelyTranslucent = model != null && model.renderLayer() == 1;
            }

            lastBlockNonSolid = nativelyTranslucent;
            return !nativelyTranslucent && cachedA < 255;
         } else if (mode == 3) {
            lastBlockNonSolid = false;
            return cachedA < 255;
         } else {
            lastBlockNonSolid = false;
            return false;
         }
      }
   }

   public static int getLastBlockGhostMode() {
      return lastBlockGhostMode;
   }

   public static boolean isLastBlockNonSolid() {
      return lastBlockNonSolid;
   }

   public static boolean isRenderingGhostBlock() {
      return ghostRenderMode > 0;
   }

   public static boolean isRenderingWrongBlock() {
      return ghostRenderMode == 2;
   }

   public static boolean isRenderingReplaceableWrongBlock() {
      return ghostRenderMode == 3;
   }

   public static boolean isWrongBlockOverlay() {
      return wrongBlockOverlay;
   }

   public static void setWrongBlockOverlay(boolean v) {
      wrongBlockOverlay = v;
   }

   public static void setGhostRenderMode(int mode) {
      ghostRenderMode = mode;
   }

   public static boolean isHidden() {
      return hidden;
   }

   public static void setHidden(boolean hide, World world) {
      if (hidden != hide) {
         hidden = hide;
         if (world != null) {
            Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
            if (worldBlocks != null && !worldBlocks.isEmpty()) {
               for (Entry<Long, GhostBlockState.BlockState> entry : worldBlocks.entrySet()) {
                  long packed = entry.getKey();
                  int px = unpackX(packed);
                  int py = unpackY(packed);
                  int pz = unpackZ(packed);
                  GhostBlockState.BlockState state = entry.getValue();
                  if (hide) {
                     setBlockNoLighting(world, px, py, pz, state.serverBlockId, state.serverMetadata);
                  } else if (py <= layerCutoffY && state.serverBlockId == 0) {
                     setBlockNoLighting(world, px, py, pz, state.ghostBlockId, state.ghostMetadata);
                  }

                  world.markBlocksDirty(px, py, pz, px, py, pz);
               }
            }
         }
      }
   }

   public static boolean isLayerAtMax() {
      return layerAtMax;
   }

   public static int getLayerCutoffY() {
      return layerCutoffY;
   }

   public static int[] getYRange(World world) {
      if (world == null) {
         return null;
      } else {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
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

   public static int getTotalLayers(World world) {
      int[] range = getYRange(world);
      return range == null ? 0 : range[1] - range[0] + 1;
   }

   public static void setLayerCount(World world, int count, boolean atMax) {
      layerAtMax = atMax;
      int oldCutoff = layerCutoffY;
      if (!atMax && world != null) {
         int[] range = getYRange(world);
         layerCutoffY = range == null ? Integer.MAX_VALUE : range[0] + count - 1;
      } else {
         layerCutoffY = Integer.MAX_VALUE;
      }

      if (oldCutoff != layerCutoffY) {
         applyLayerVisibility(world, layerCutoffY);
      }
   }

   private static void applyLayerVisibility(World world, int newCutoff) {
      if (!hidden && world != null) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null && !worldBlocks.isEmpty()) {
            for (Entry<Long, GhostBlockState.BlockState> entry : worldBlocks.entrySet()) {
               long packed = entry.getKey();
               int px = unpackX(packed);
               int py = unpackY(packed);
               int pz = unpackZ(packed);
               GhostBlockState.BlockState state = entry.getValue();
               boolean shouldBeVisible = py <= newCutoff;
               int wantedId = shouldBeVisible && state.serverBlockId == 0 ? state.ghostBlockId : state.serverBlockId;
               int wantedMeta = shouldBeVisible && state.serverBlockId == 0 ? state.ghostMetadata : state.serverMetadata;
               int currentId = world.getBlockId(px, py, pz);
               int currentMeta = world.getBlockMetadata(px, py, pz);
               if (currentId != wantedId || currentMeta != wantedMeta) {
                  setBlockNoLighting(world, px, py, pz, wantedId, wantedMeta);
                  world.markBlocksDirty(px, py, pz, px, py, pz);
               }
            }
         }
      }
   }

   public static float getHologramOpacity() {
      return hologramOpacity;
   }

   public static float getHologramHue() {
      return hologramHue;
   }

   public static float getHologramSaturation() {
      return hologramSaturation;
   }

   public static void setHologramOpacity(float v) {
      hologramOpacity = Math.max(0.0F, Math.min(1.0F, v));
      recomputeHologramCache();
   }

   public static void setHologramHue(float v) {
      hologramHue = Math.max(0.0F, Math.min(1.0F, v));
      recomputeHologramCache();
   }

   public static void setHologramSaturation(float v) {
      hologramSaturation = Math.max(0.0F, Math.min(1.0F, v));
      recomputeHologramCache();
   }

   private static void recomputeHologramCache() {
      float h = (hologramHue - (float)Math.floor(hologramHue)) * 6.0F;
      int hi = (int)h;
      float f = h - hi;
      float p = 1.0F - hologramSaturation;
      float q = 1.0F - f * hologramSaturation;
      float t = 1.0F - (1.0F - f) * hologramSaturation;
      float r;
      float g;
      float b;
      switch (hi) {
         case 0:
            r = 1.0F;
            g = t;
            b = p;
            break;
         case 1:
            r = q;
            g = 1.0F;
            b = p;
            break;
         case 2:
            r = p;
            g = 1.0F;
            b = t;
            break;
         case 3:
            r = p;
            g = q;
            b = 1.0F;
            break;
         case 4:
            r = t;
            g = p;
            b = 1.0F;
            break;
         default:
            r = 1.0F;
            g = p;
            b = q;
      }

      cachedR = (int)(r * 255.0F);
      cachedG = (int)(g * 255.0F);
      cachedB = (int)(b * 255.0F);
      cachedA = (int)(hologramOpacity * 255.0F);
   }

   public static int getHologramR() {
      return cachedR;
   }

   public static int getHologramG() {
      return cachedG;
   }

   public static int getHologramB() {
      return cachedB;
   }

   public static int getHologramA() {
      return cachedA;
   }

   public static boolean isSuppressingLighting() {
      return suppressLighting;
   }

   public static void setSuppressLighting(boolean suppress) {
      suppressLighting = suppress;
   }

   public static boolean isSuppressingEntitySpawn() {
      return suppressEntitySpawn;
   }

   public static void setSuppressEntitySpawn(boolean suppress) {
      suppressEntitySpawn = suppress;
   }

   public static void setBlockNoLighting(World world, int x, int y, int z, int id, int meta) {
      if (y >= 0 && y < 256) {
         Chunk chunk = world.getChunkFromBlockCoords(x, z);
         ChunkSection section = chunk.getSection(y / 16);
         if (section != null) {
            section.setBlock(x & 15, y % 16, z & 15, id);
            section.setData(x & 15, y % 16, z & 15, meta);
         }
      }
   }

   public static void beginLightPatchScoped(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      if (world != null) {
         if (lightPatchDepth++ <= 0) {
            Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
            if (worldBlocks != null && !worldBlocks.isEmpty()) {
               for (Entry<Long, GhostBlockState.BlockState> entry : worldBlocks.entrySet()) {
                  long packed = entry.getKey();
                  int px = unpackX(packed);
                  int py = unpackY(packed);
                  int pz = unpackZ(packed);
                  if (px >= minX && px <= maxX && py >= minY && py <= maxY && pz >= minZ && pz <= maxZ) {
                     GhostBlockState.BlockState state = entry.getValue();
                     int liveId = world.getBlockId(px, py, pz);
                     int liveMeta = world.getBlockMetadata(px, py, pz);
                     if (liveId != state.serverBlockId || liveMeta != state.serverMetadata) {
                        lightPatchedPositions.add(new int[]{px, py, pz, liveId, liveMeta});
                        setBlockNoLighting(world, px, py, pz, state.serverBlockId, state.serverMetadata);
                     }
                  }
               }
            }
         }
      }
   }

   public static void endLightPatch(World world) {
      if (world != null) {
         if (--lightPatchDepth <= 0) {
            lightPatchDepth = 0;

            for (int[] saved : lightPatchedPositions) {
               setBlockNoLighting(world, saved[0], saved[1], saved[2], saved[3], saved[4]);
            }

            lightPatchedPositions.clear();
         }
      }
   }

   public static void updateServer(World world, int x, int y, int z, int newServerId, int newServerMeta) {
      if (world != null) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null) {
            long key = packPos(x, y, z);
            GhostBlockState.BlockState state = worldBlocks.get(key);
            if (state != null) {
               worldBlocks.put(key, new GhostBlockState.BlockState(newServerId, newServerMeta, state.ghostBlockId, state.ghostMetadata));
            }
         }
      }
   }

   public static boolean handleTrackedBlockChange(World world, int x, int y, int z, int newId, int newMeta) {
      if (world == null) {
         return false;
      } else {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks == null) {
            return false;
         } else {
            long key = packPos(x, y, z);
            GhostBlockState.BlockState state = worldBlocks.get(key);
            if (state == null) {
               return false;
            } else {
               worldBlocks.put(key, new GhostBlockState.BlockState(newId, newMeta, state.ghostBlockId, state.ghostMetadata));
               if (newId == 0 && !hidden && y <= layerCutoffY) {
                  setBlockNoLighting(world, x, y, z, state.ghostBlockId, state.ghostMetadata);
                  world.scheduleLightingUpdate(LightLayer.Sky, x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
                  world.scheduleLightingUpdate(LightLayer.Block, x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
                  world.markBlocksDirty(x, y, z, x, y, z);
                  return true;
               } else {
                  if (newId != 0) {
                     setBlockNoLighting(world, x, y, z, newId, newMeta);
                  }

                  world.markBlocksDirty(x, y, z, x, y, z);
                  return false;
               }
            }
         }
      }
   }

   public static void beginPhysicsPatchScoped(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      if (world != null) {
         if (physicsDepth++ <= 0) {
            Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
            if (worldBlocks != null && !worldBlocks.isEmpty()) {
               for (Entry<Long, GhostBlockState.BlockState> entry : worldBlocks.entrySet()) {
                  long packed = entry.getKey();
                  int px = unpackX(packed);
                  int py = unpackY(packed);
                  int pz = unpackZ(packed);
                  if (px >= minX && px <= maxX && py >= minY && py <= maxY && pz >= minZ && pz <= maxZ) {
                     GhostBlockState.BlockState state = entry.getValue();
                     int liveId = world.getBlockId(px, py, pz);
                     int liveMeta = world.getBlockMetadata(px, py, pz);
                     if (liveId != state.serverBlockId || liveMeta != state.serverMetadata) {
                        patchedPositions.add(new int[]{px, py, pz, liveId, liveMeta});
                        setBlockNoLighting(world, px, py, pz, state.serverBlockId, state.serverMetadata);
                     }
                  }
               }
            }
         }
      }
   }

   public static void endPhysicsPatch(World world) {
      if (world != null) {
         if (--physicsDepth <= 0) {
            physicsDepth = 0;

            for (int[] saved : patchedPositions) {
               setBlockNoLighting(world, saved[0], saved[1], saved[2], saved[3], saved[4]);
            }

            patchedPositions.clear();
         }
      }
   }

   public static void capture(World world, int x, int y, int z) {
      if (world != null) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.computeIfAbsent(world, w -> new HashMap<>());
         long key = packPos(x, y, z);
         if (!worldBlocks.containsKey(key)) {
            int id = world.getBlockId(x, y, z);
            int meta = world.getBlockMetadata(x, y, z);
            worldBlocks.put(key, new GhostBlockState.BlockState(id, meta, id, meta));
            addSectionEntry(world, x, y, z);
         }
      }
   }

   public static void sync(World world, int x, int y, int z) {
      if (world != null) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null) {
            long key = packPos(x, y, z);
            GhostBlockState.BlockState state = worldBlocks.get(key);
            if (state != null) {
               int currentId = world.getBlockId(x, y, z);
               int currentMeta = world.getBlockMetadata(x, y, z);
               if (currentId == state.serverBlockId && currentMeta == state.serverMetadata) {
                  worldBlocks.remove(key);
                  if (worldBlocks.isEmpty()) {
                     BLOCKS_BY_WORLD.remove(world);
                     GHOST_SECTIONS.remove(world);
                     SECTION_COUNTS.remove(world);
                  } else {
                     removeSectionEntry(world, x, y, z);
                  }
               } else {
                  worldBlocks.put(key, new GhostBlockState.BlockState(state.serverBlockId, state.serverMetadata, currentId, currentMeta));
                  if (state.serverBlockId != 0 && !DesignModeState.isActive()) {
                     setBlockNoLighting(world, x, y, z, state.serverBlockId, state.serverMetadata);
                  }

                  if (DesignModeState.isActive() && !layerAtMax) {
                     int oldCutoff = layerCutoffY;
                     layerAtMax = true;
                     layerCutoffY = Integer.MAX_VALUE;
                     if (oldCutoff != Integer.MAX_VALUE) {
                        applyLayerVisibility(world, Integer.MAX_VALUE);
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean isGhostBlock(World world, int x, int y, int z) {
      if (hidden || world == null || y > layerCutoffY) {
         return false;
      } else if (!hasSectionGhosts(world, x, y, z)) {
         return false;
      } else {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks == null) {
            return false;
         } else {
            GhostBlockState.BlockState state = worldBlocks.get(packPos(x, y, z));
            if (state == null) {
               return false;
            } else {
               int currentId = world.getBlockId(x, y, z);
               int currentMeta = world.getBlockMetadata(x, y, z);
               return currentId == state.ghostBlockId
                  && currentMeta == state.ghostMetadata
                  && (currentId != state.serverBlockId || currentMeta != state.serverMetadata);
            }
         }
      }
   }

   public static boolean isFulfillableGhost(World world, int x, int y, int z) {
      if (isGhostBlock(world, x, y, z)) {
         return true;
      } else if (hidden || world == null || y > layerCutoffY) {
         return false;
      } else if (!hasSectionGhosts(world, x, y, z)) {
         return false;
      } else {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks == null) {
            return false;
         } else {
            GhostBlockState.BlockState state = worldBlocks.get(packPos(x, y, z));
            if (state == null) {
               return false;
            } else if (state.serverBlockId != 0 && (state.serverBlockId != state.ghostBlockId || state.serverMetadata != state.ghostMetadata)) {
               Block<?> serverBlock = Blocks.blocksList[state.serverBlockId];
               return serverBlock != null && serverBlock.getMaterial().isReplaceable();
            } else {
               return false;
            }
         }
      }
   }

   public static int getGhostRenderMode(World world, int x, int y, int z) {
      return !hasSectionGhosts(world, x, y, z) ? 0 : getGhostRenderModeInternal(world, x, y, z);
   }

   private static int getGhostRenderModeInternal(World world, int x, int y, int z) {
      if (!hidden && world != null && y <= layerCutoffY) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks == null) {
            return 0;
         } else {
            GhostBlockState.BlockState state = worldBlocks.get(packPos(x, y, z));
            if (state == null) {
               return 0;
            } else {
               int currentId = world.getBlockId(x, y, z);
               int currentMeta = world.getBlockMetadata(x, y, z);
               if (currentId != state.ghostBlockId
                  || currentMeta != state.ghostMetadata
                  || currentId == state.serverBlockId && currentMeta == state.serverMetadata) {
                  if (state.serverBlockId != 0 && (state.serverBlockId != state.ghostBlockId || state.serverMetadata != state.ghostMetadata)) {
                     Block<?> serverBlock = Blocks.blocksList[state.serverBlockId];
                     return serverBlock != null && serverBlock.getMaterial().isReplaceable() ? 3 : 2;
                  } else {
                     return 0;
                  }
               } else {
                  return 1;
               }
            }
         }
      } else {
         return 0;
      }
   }

   public static int getServerBlockId(World world, int x, int y, int z) {
      if (world == null) {
         return 0;
      } else {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null) {
            GhostBlockState.BlockState state = worldBlocks.get(packPos(x, y, z));
            if (state != null) {
               return state.serverBlockId;
            }
         }

         return world.getBlockId(x, y, z);
      }
   }

   public static int isServerBlockNormalCube(World world, int x, int y, int z) {
      if (world == null) {
         return -1;
      } else if (!hasSectionGhosts(world, x, y, z)) {
         return -1;
      } else {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks == null) {
            return -1;
         } else {
            GhostBlockState.BlockState state = worldBlocks.get(packPos(x, y, z));
            if (state == null) {
               return -1;
            } else {
               Block<?> block = Blocks.blocksList[state.serverBlockId];
               if (block == null) {
                  return 0;
               } else {
                  return block.getMaterial().isSolidBlocking() && block.renderAsNormalBlockOnCondition(world, x, y, z) ? 1 : 0;
               }
            }
         }
      }
   }

   public static int getGhostBlockId(World world, int x, int y, int z) {
      if (world == null) {
         return 0;
      } else {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null) {
            GhostBlockState.BlockState state = worldBlocks.get(packPos(x, y, z));
            if (state != null) {
               return state.ghostBlockId;
            }
         }

         return world.getBlockId(x, y, z);
      }
   }

   public static int getGhostMetadata(World world, int x, int y, int z) {
      if (world == null) {
         return 0;
      } else {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null) {
            GhostBlockState.BlockState state = worldBlocks.get(packPos(x, y, z));
            if (state != null) {
               return state.ghostMetadata;
            }
         }

         return world.getBlockMetadata(x, y, z);
      }
   }

   public static void revertToServer(World world, int x, int y, int z) {
      if (world != null) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null) {
            long key = packPos(x, y, z);
            GhostBlockState.BlockState state = worldBlocks.get(key);
            if (state != null) {
               setBlockNoLighting(world, x, y, z, state.serverBlockId, state.serverMetadata);
               worldBlocks.remove(key);
               if (worldBlocks.isEmpty()) {
                  BLOCKS_BY_WORLD.remove(world);
                  GHOST_SECTIONS.remove(world);
                  SECTION_COUNTS.remove(world);
               } else {
                  removeSectionEntry(world, x, y, z);
               }

               SignTextCache.remove(x, y, z);
               world.markBlocksDirty(x, y, z, x, y, z);
            }
         }
      }
   }

   public static void revertMultiPart(World world, int x, int y, int z) {
      if (world != null) {
         int blockId = world.getBlockId(x, y, z);
         Block<?> block = Blocks.blocksList[blockId];
         if (block != null) {
            BlockLogic logic = block.getLogic();
            if (logic instanceof BlockLogicDoor) {
               BlockLogicDoor doorLogic = (BlockLogicDoor)logic;
               int otherY = doorLogic.isTop ? y - 1 : y + 1;
               revertToServer(world, x, otherY, z);
            } else if (logic instanceof BlockLogicBed) {
               int meta = world.getBlockMetadata(x, y, z);
               int dir = BlockLogicBed.getDirection(meta);
               Side[] map = BlockLogicBed.headBlockToFootBlockMap;
               if (BlockLogicBed.isBlockFootOfBed(meta)) {
                  revertToServer(world, x - map[dir].getOffsetX(), y, z - map[dir].getOffsetZ());
               } else {
                  revertToServer(world, x + map[dir].getOffsetX(), y, z + map[dir].getOffsetZ());
               }
            }
         }

         revertToServer(world, x, y, z);
      }
   }

   public static void clear() {
      BLOCKS_BY_WORLD.clear();
      GHOST_SECTIONS.clear();
      SECTION_COUNTS.clear();
      SignTextCache.clear();
      layerCutoffY = Integer.MAX_VALUE;
      layerAtMax = true;
   }

   public static void clearAll(World world) {
      if (world != null) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.remove(world);
         GHOST_SECTIONS.remove(world);
         SECTION_COUNTS.remove(world);
         if (worldBlocks != null && !worldBlocks.isEmpty()) {
            for (Entry<Long, GhostBlockState.BlockState> entry : worldBlocks.entrySet()) {
               long packed = entry.getKey();
               int px = unpackX(packed);
               int py = unpackY(packed);
               int pz = unpackZ(packed);
               GhostBlockState.BlockState state = entry.getValue();
               setBlockNoLighting(world, px, py, pz, state.serverBlockId, state.serverMetadata);
               world.markBlocksDirty(px, py, pz, px, py, pz);
            }

            SignTextCache.clear();
            layerCutoffY = Integer.MAX_VALUE;
            layerAtMax = true;
         }
      }
   }

   public static boolean isTracked(World world, int x, int y, int z) {
      if (world == null) {
         return false;
      } else {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         return worldBlocks != null && worldBlocks.containsKey(packPos(x, y, z));
      }
   }

   public static boolean isWrongBlock(World world, int x, int y, int z) {
      if (hidden || world == null || y > layerCutoffY) {
         return false;
      } else if (!hasSectionGhosts(world, x, y, z)) {
         return false;
      } else {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks == null) {
            return false;
         } else {
            GhostBlockState.BlockState state = worldBlocks.get(packPos(x, y, z));
            return state == null
               ? false
               : state.serverBlockId != 0 && (state.serverBlockId != state.ghostBlockId || state.serverMetadata != state.ghostMetadata);
         }
      }
   }

   public static void shiftAll(World world, int dx, int dy, int dz) {
      if (world != null) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null && !worldBlocks.isEmpty()) {
            List<int[]> entries = new ArrayList<>();

            for (Entry<Long, GhostBlockState.BlockState> entry : worldBlocks.entrySet()) {
               long packed = entry.getKey();
               int px = unpackX(packed);
               int py = unpackY(packed);
               int pz = unpackZ(packed);
               GhostBlockState.BlockState state = entry.getValue();
               setBlockNoLighting(world, px, py, pz, state.serverBlockId, state.serverMetadata);
               world.markBlocksDirty(px, py, pz, px, py, pz);
               int ny = py + dy;
               if (ny >= 0 && ny < 256) {
                  entries.add(new int[]{px + dx, ny, pz + dz, state.ghostBlockId, state.ghostMetadata});
               }
            }

            worldBlocks.clear();
            GHOST_SECTIONS.remove(world);
            SECTION_COUNTS.remove(world);

            for (int[] e : entries) {
               int nx = e[0];
               int ny = e[1];
               int nz = e[2];
               int ghostId = e[3];
               int ghostMeta = e[4];
               int serverId = world.getBlockId(nx, ny, nz);
               int serverMeta = world.getBlockMetadata(nx, ny, nz);
               long key = packPos(nx, ny, nz);
               worldBlocks.put(key, new GhostBlockState.BlockState(serverId, serverMeta, ghostId, ghostMeta));
               addSectionEntry(world, nx, ny, nz);
               if (serverId == 0) {
                  setBlockNoLighting(world, nx, ny, nz, ghostId, ghostMeta);
               }

               world.markBlocksDirty(nx, ny, nz, nx, ny, nz);
            }

            if (worldBlocks.isEmpty()) {
               BLOCKS_BY_WORLD.remove(world);
            }

            SignTextCache.shiftAll(dx, dy, dz);
         }
      }
   }

   public static void markAllDirty(World world) {
      if (world != null) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null && !worldBlocks.isEmpty()) {
            for (long packed : worldBlocks.keySet()) {
               int px = unpackX(packed);
               int py = unpackY(packed);
               int pz = unpackZ(packed);
               world.markBlocksDirty(px, py, pz, px, py, pz);
            }
         }
      }
   }

   public static void swapGhostChunkData(World world, boolean enteringDesignMode) {
      if (world != null) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null && !worldBlocks.isEmpty()) {
            for (Entry<Long, GhostBlockState.BlockState> entry : worldBlocks.entrySet()) {
               GhostBlockState.BlockState state = entry.getValue();
               if (state.serverBlockId != 0) {
                  long packed = entry.getKey();
                  int px = unpackX(packed);
                  int py = unpackY(packed);
                  int pz = unpackZ(packed);
                  if (enteringDesignMode) {
                     setBlockNoLighting(world, px, py, pz, state.ghostBlockId, state.ghostMetadata);
                  } else {
                     setBlockNoLighting(world, px, py, pz, state.serverBlockId, state.serverMetadata);
                  }
               }
            }
         }
      }
   }

   public static boolean hasRealAdjacentBlock(World world, int x, int y, int z) {
      return getServerBlockId(world, x - 1, y, z) != 0
         || getServerBlockId(world, x + 1, y, z) != 0
         || getServerBlockId(world, x, y - 1, z) != 0
         || getServerBlockId(world, x, y + 1, z) != 0
         || getServerBlockId(world, x, y, z - 1) != 0
         || getServerBlockId(world, x, y, z + 1) != 0;
   }

   public static boolean isFulfillmentInProgress() {
      return fulfillmentInProgress;
   }

   public static void setFulfillmentInProgress(boolean active) {
      fulfillmentInProgress = active;
   }

   public static void setPendingFulfillment(int x, int y, int z, int blockId, int meta) {
      hasPendingFulfillment = true;
      pendingFulfillX = x;
      pendingFulfillY = y;
      pendingFulfillZ = z;
      pendingFulfillBlockId = blockId;
      pendingFulfillMeta = meta;
   }

   public static int[] consumePendingFulfillment() {
      if (!hasPendingFulfillment) {
         return null;
      } else {
         hasPendingFulfillment = false;
         return new int[]{pendingFulfillX, pendingFulfillY, pendingFulfillZ, pendingFulfillBlockId, pendingFulfillMeta};
      }
   }

   public static void registerRecentFulfillment(int x, int y, int z, int blockId, int meta) {
      recentFulfillments.put(packPos(x, y, z), new int[]{blockId, meta, 100});
   }

   public static void trackFulfilled(World world, int x, int y, int z, int serverBlockId, int serverMeta, int ghostBlockId, int ghostMeta) {
      if (world != null) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.computeIfAbsent(world, w -> new HashMap<>());
         long key = packPos(x, y, z);
         worldBlocks.put(key, new GhostBlockState.BlockState(serverBlockId, serverMeta, ghostBlockId, ghostMeta));
         addSectionEntry(world, x, y, z);
      }
   }

   public static void restoreGhost(World world, int x, int y, int z, int ghostId, int ghostMeta) {
      if (world != null) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.computeIfAbsent(world, w -> new HashMap<>());
         worldBlocks.put(packPos(x, y, z), new GhostBlockState.BlockState(0, 0, ghostId, ghostMeta));
         addSectionEntry(world, x, y, z);
         setBlockNoLighting(world, x, y, z, ghostId, ghostMeta);
         world.markBlocksDirty(x, y, z, x, y, z);
      }
   }

   public static int[] getRecentFulfillment(int x, int y, int z) {
      int[] data = recentFulfillments.get(packPos(x, y, z));
      return data != null ? new int[]{data[0], data[1]} : null;
   }

   public static void tickFulfillments() {
      if (!recentFulfillments.isEmpty()) {
         Iterator<Entry<Long, int[]>> it = recentFulfillments.entrySet().iterator();

         while (it.hasNext()) {
            int[] data = it.next().getValue();
            if (--data[2] <= 0) {
               it.remove();
            }
         }
      }
   }

   public static void correctFulfillmentsInRegion(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      if (world != null && !recentFulfillments.isEmpty()) {
         for (Entry<Long, int[]> entry : recentFulfillments.entrySet()) {
            long packed = entry.getKey();
            int px = unpackX(packed);
            int py = unpackY(packed);
            int pz = unpackZ(packed);
            if (px >= minX && px <= maxX && py >= minY && py <= maxY && pz >= minZ && pz <= maxZ) {
               int[] data = entry.getValue();
               int currentId = world.getBlockId(px, py, pz);
               int currentMeta = world.getBlockMetadata(px, py, pz);
               if (currentId == data[0] && currentMeta != data[1]) {
                  setBlockNoLighting(world, px, py, pz, data[0], data[1]);
                  world.markBlocksDirty(px, py, pz, px, py, pz);
               }
            }
         }
      }
   }

   public static void restoreGhostsInRegion(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      if (world != null) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null && !worldBlocks.isEmpty()) {
            for (Entry<Long, GhostBlockState.BlockState> entry : worldBlocks.entrySet()) {
               long packed = entry.getKey();
               int px = unpackX(packed);
               int py = unpackY(packed);
               int pz = unpackZ(packed);
               if (px >= minX && px <= maxX && py >= minY && py <= maxY && pz >= minZ && pz <= maxZ) {
                  GhostBlockState.BlockState state = entry.getValue();
                  int newServerId = world.getBlockId(px, py, pz);
                  int newServerMeta = world.getBlockMetadata(px, py, pz);
                  worldBlocks.put(packed, new GhostBlockState.BlockState(newServerId, newServerMeta, state.ghostBlockId, state.ghostMetadata));
                  if (!hidden && py <= layerCutoffY && newServerId == 0 && (state.ghostBlockId != 0 || state.ghostMetadata != 0)) {
                     setBlockNoLighting(world, px, py, pz, state.ghostBlockId, state.ghostMetadata);
                     world.markBlocksDirty(px, py, pz, px, py, pz);
                  }
               }
            }
         }
      }
   }

   public static void forEachEntry(World world, GhostBlockState.GhostBlockConsumer consumer) {
      if (world != null) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         if (worldBlocks != null) {
            for (Entry<Long, GhostBlockState.BlockState> entry : worldBlocks.entrySet()) {
               long packed = entry.getKey();
               GhostBlockState.BlockState state = entry.getValue();
               consumer.accept(
                  unpackX(packed), unpackY(packed), unpackZ(packed), state.serverBlockId, state.serverMetadata, state.ghostBlockId, state.ghostMetadata
               );
            }
         }
      }
   }

   public static boolean hasEntries(World world) {
      if (world == null) {
         return false;
      } else {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
         return worldBlocks != null && !worldBlocks.isEmpty();
      }
   }

   public static void setPendingEntries(List<int[]> entries) {
      pendingEntries = entries;
      pendingApplyTicksRemaining = 0;
   }

   public static void applyPendingEntries(World world) {
      if (world != null && pendingEntries != null) {
         Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.computeIfAbsent(world, w -> new HashMap<>());

         for (int[] e : pendingEntries) {
            long key = packPos(e[0], e[1], e[2]);
            worldBlocks.put(key, new GhostBlockState.BlockState(e[3], e[4], e[5], e[6]));
            addSectionEntry(world, e[0], e[1], e[2]);
         }

         pendingEntries = null;
         pendingApplyTicksRemaining = 200;
         applyGhostBlocksToChunks(world);
      }
   }

   public static void tickPendingApplication(World world) {
      if (pendingApplyTicksRemaining > 0 && world != null) {
         pendingApplyTicksRemaining--;
         if (pendingApplyTicksRemaining % 20 == 0) {
            applyGhostBlocksToChunks(world);
         }
      }
   }

   private static void applyGhostBlocksToChunks(World world) {
      Map<Long, GhostBlockState.BlockState> worldBlocks = BLOCKS_BY_WORLD.get(world);
      if (worldBlocks != null && !worldBlocks.isEmpty()) {
         boolean anyApplied = false;

         for (Entry<Long, GhostBlockState.BlockState> entry : worldBlocks.entrySet()) {
            long packed = entry.getKey();
            int px = unpackX(packed);
            int py = unpackY(packed);
            int pz = unpackZ(packed);
            GhostBlockState.BlockState state = entry.getValue();
            if ((state.ghostBlockId != state.serverBlockId || state.ghostMetadata != state.serverMetadata) && world.isChunkLoaded(px >> 4, pz >> 4)) {
               int currentId = world.getBlockId(px, py, pz);
               int currentMeta = world.getBlockMetadata(px, py, pz);
               if (currentId == state.serverBlockId && currentMeta == state.serverMetadata && state.serverBlockId == 0) {
                  setBlockNoLighting(world, px, py, pz, state.ghostBlockId, state.ghostMetadata);
                  anyApplied = true;
               }
            }
         }

         if (anyApplied) {
            markAllDirty(world);
         }

         SignTextCache.applySignTileEntities(world);
      }
   }

   static {
      recomputeHologramCache();
   }

   private static final class BlockState {
      final int serverBlockId;
      final int serverMetadata;
      final int ghostBlockId;
      final int ghostMetadata;

      BlockState(int serverBlockId, int serverMetadata, int ghostBlockId, int ghostMetadata) {
         this.serverBlockId = serverBlockId;
         this.serverMetadata = serverMetadata;
         this.ghostBlockId = ghostBlockId;
         this.ghostMetadata = ghostMetadata;
      }
   }

   @FunctionalInterface
   public interface GhostBlockConsumer {
      void accept(int var1, int var2, int var3, int var4, int var5, int var6, int var7);
   }
}
