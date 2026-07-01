package biscuitius.blueprints.client.hologram;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.BlockLogicAxisAligned;
import net.minecraft.core.block.BlockLogicBed;
import net.minecraft.core.block.BlockLogicDoor;
import net.minecraft.core.block.BlockLogicLadder;
import net.minecraft.core.block.BlockLogicRotatable;
import net.minecraft.core.block.BlockLogicStairs;
import net.minecraft.core.block.BlockLogicTrapDoor;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.world.World;

public final class BlueprintTransform {
   private BlueprintTransform() {
   }

   public static void rotate(World world, BlueprintTransform.Rotation rotation) {
      if (world != null && rotation != null) {
         BlueprintTransform.Snapshot snap = snapshot(world);
         if (snap != null) {
            int width = snap.maxX - snap.minX + 1;
            int depth = snap.maxZ - snap.minZ + 1;
            boolean cw = rotation == BlueprintTransform.Rotation.CW;
            List<BlueprintTransform.Entry> rotated = new ArrayList<>(snap.entries.size());

            for (BlueprintTransform.Entry e : snap.entries) {
               int relX = e.x - snap.minX;
               int relZ = e.z - snap.minZ;
               int newRelX;
               int newRelZ;
               if (cw) {
                  newRelX = depth - 1 - relZ;
                  newRelZ = relX;
               } else {
                  newRelX = relZ;
                  newRelZ = width - 1 - relX;
               }

               int newMeta = rotateMetadata(e.block.blockId, e.block.metadata, cw);
               HologramBlock nb = newMeta == e.block.metadata ? e.block : e.block.withMetadata(newMeta);
               rotated.add(new BlueprintTransform.Entry(snap.minX + newRelX, e.y, snap.minZ + newRelZ, nb));
            }

            commit(world, rotated);
         }
      }
   }

   public static void flip(World world, BlueprintTransform.FlipAxis axis) {
      if (world != null && axis != null) {
         BlueprintTransform.Snapshot snap = snapshot(world);
         if (snap != null) {
            List<BlueprintTransform.Entry> flipped = new ArrayList<>(snap.entries.size());

            for (BlueprintTransform.Entry e : snap.entries) {
               int newX = e.x;
               int newZ = e.z;
               if (axis == BlueprintTransform.FlipAxis.X) {
                  int relX = e.x - snap.minX;
                  newX = snap.maxX - relX;
               } else {
                  int relZ = e.z - snap.minZ;
                  newZ = snap.maxZ - relZ;
               }

               int newMeta = flipMetadata(e.block.blockId, e.block.metadata, axis);
               HologramBlock nb = newMeta == e.block.metadata ? e.block : e.block.withMetadata(newMeta);
               flipped.add(new BlueprintTransform.Entry(newX, e.y, newZ, nb));
            }

            commit(world, flipped);
         }
      }
   }

   private static BlueprintTransform.Snapshot snapshot(World world) {
      if (!HologramStore.hasEntries(world)) {
         return null;
      } else {
         int[] b = HologramStore.getBounds(world);
         if (b == null) {
            return null;
         } else {
            List<BlueprintTransform.Entry> entries = new ArrayList<>(HologramStore.size(world));
            HologramStore.forEach(world, (x, y, z, h) -> entries.add(new BlueprintTransform.Entry(x, y, z, h)));
            return new BlueprintTransform.Snapshot(entries, b[0], b[1], b[2], b[3], b[4], b[5]);
         }
      }
   }

   private static void commit(World world, List<BlueprintTransform.Entry> entries) {
      HologramStore.clearWorld(world);

      for (BlueprintTransform.Entry e : entries) {
         HologramStore.put(world, e.x, e.y, e.z, e.block);
      }

      HologramStore.recomputeBounds(world);
   }

   private static int rotateMetadata(int blockId, int meta, boolean cw) {
      BlockLogic logic = logicFor(blockId);
      if (logic == null) {
         return meta;
      } else if (logic instanceof BlockLogicStairs) {
         int high = meta & -4;
         return high | rotateStairsDir(meta & 3, cw);
      } else if (logic instanceof BlockLogicDoor) {
         int high = meta & -4;
         int dir = meta & 3;
         int rotated = cw ? dir + 1 & 3 : dir + 3 & 3;
         return high | rotated;
      } else if (logic instanceof BlockLogicBed) {
         int high = meta & -4;
         int dir = meta & 3;
         int rotated = cw ? dir + 1 & 3 : dir + 3 & 3;
         return high | rotated;
      } else if (logic instanceof BlockLogicTrapDoor) {
         int high = meta & -4;
         return high | rotateTrapDoorDir(meta & 3, cw);
      } else if (logic instanceof BlockLogicAxisAligned) {
         int low = meta & 3;
         int high = meta & -4;
         if (low == 1) {
            return high | 2;
         } else {
            return low == 2 ? high | 1 : meta;
         }
      } else if (logic instanceof BlockLogicRotatable) {
         int high = meta & -8;
         return high | rotateDirectionId(meta & 7, cw);
      } else {
         return logic instanceof BlockLogicLadder ? rotateDirectionId(meta, cw) : meta;
      }
   }

   private static int flipMetadata(int blockId, int meta, BlueprintTransform.FlipAxis axis) {
      BlockLogic logic = logicFor(blockId);
      if (logic == null) {
         return meta;
      } else if (logic instanceof BlockLogicStairs) {
         int high = meta & -4;
         return high | flipStairsDir(meta & 3, axis);
      } else if (logic instanceof BlockLogicDoor) {
         int high = meta & -4;
         return high | flipDoorOrBedDir(meta & 3, axis, true);
      } else if (logic instanceof BlockLogicBed) {
         int high = meta & -4;
         return high | flipDoorOrBedDir(meta & 3, axis, false);
      } else if (logic instanceof BlockLogicTrapDoor) {
         int high = meta & -4;
         return high | flipTrapDoorDir(meta & 3, axis);
      } else if (logic instanceof BlockLogicAxisAligned) {
         return meta;
      } else if (logic instanceof BlockLogicRotatable) {
         int high = meta & -8;
         return high | flipDirectionId(meta & 7, axis);
      } else {
         return logic instanceof BlockLogicLadder ? flipDirectionId(meta, axis) : meta;
      }
   }

   private static BlockLogic logicFor(int blockId) {
      if (blockId > 0 && blockId < Blocks.blocksList.length) {
         Block<?> b = Blocks.blocksList[blockId];
         return b == null ? null : b.getLogic();
      } else {
         return null;
      }
   }

   private static int rotateStairsDir(int dir, boolean cw) {
      int[] cwMap = new int[]{2, 3, 1, 0};
      int[] ccwMap = new int[]{3, 2, 0, 1};
      return (cw ? cwMap : ccwMap)[dir & 3];
   }

   private static int flipStairsDir(int dir, BlueprintTransform.FlipAxis axis) {
      if (axis == BlueprintTransform.FlipAxis.X) {
         if (dir == 0) {
            return 1;
         } else {
            return dir == 1 ? 0 : dir;
         }
      } else if (dir == 2) {
         return 3;
      } else {
         return dir == 3 ? 2 : dir;
      }
   }

   private static int rotateTrapDoorDir(int dir, boolean cw) {
      int[] cwMap = new int[]{3, 2, 0, 1};
      int[] ccwMap = new int[]{2, 3, 1, 0};
      return (cw ? cwMap : ccwMap)[dir & 3];
   }

   private static int flipTrapDoorDir(int dir, BlueprintTransform.FlipAxis axis) {
      if (axis == BlueprintTransform.FlipAxis.X) {
         if (dir == 2) {
            return 3;
         } else {
            return dir == 3 ? 2 : dir;
         }
      } else if (dir == 0) {
         return 1;
      } else {
         return dir == 1 ? 0 : dir;
      }
   }

   private static int flipDoorOrBedDir(int dir, BlueprintTransform.FlipAxis axis, boolean doorOrigin) {
      if (doorOrigin) {
         if (axis == BlueprintTransform.FlipAxis.X) {
            if (dir == 1) {
               return 3;
            } else {
               return dir == 3 ? 1 : dir;
            }
         } else if (dir == 0) {
            return 2;
         } else {
            return dir == 2 ? 0 : dir;
         }
      } else if (axis == BlueprintTransform.FlipAxis.X) {
         if (dir == 1) {
            return 3;
         } else {
            return dir == 3 ? 1 : dir;
         }
      } else if (dir == 0) {
         return 2;
      } else {
         return dir == 2 ? 0 : dir;
      }
   }

   private static int rotateDirectionId(int id, boolean cw) {
      switch (id) {
         case 2:
            return cw ? 5 : 4;
         case 3:
            return cw ? 4 : 5;
         case 4:
            return cw ? 2 : 3;
         case 5:
            return cw ? 3 : 2;
         default:
            return id;
      }
   }

   private static int flipDirectionId(int id, BlueprintTransform.FlipAxis axis) {
      if (axis == BlueprintTransform.FlipAxis.X) {
         if (id == 4) {
            return 5;
         } else {
            return id == 5 ? 4 : id;
         }
      } else if (id == 2) {
         return 3;
      } else {
         return id == 3 ? 2 : id;
      }
   }

   private static final class Entry {
      final int x;
      final int y;
      final int z;
      final HologramBlock block;

      Entry(int x, int y, int z, HologramBlock block) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.block = block;
      }
   }

   public static enum FlipAxis {
      X,
      Z;
   }

   public static enum Rotation {
      CW,
      CCW;
   }

   private static final class Snapshot {
      final List<BlueprintTransform.Entry> entries;
      final int minX;
      final int minY;
      final int minZ;
      final int maxX;
      final int maxY;
      final int maxZ;

      Snapshot(List<BlueprintTransform.Entry> e, int mnx, int mny, int mnz, int mxx, int mxy, int mxz) {
         this.entries = e;
         this.minX = mnx;
         this.minY = mny;
         this.minZ = mnz;
         this.maxX = mxx;
         this.maxY = mxy;
         this.maxZ = mxz;
      }
   }
}
