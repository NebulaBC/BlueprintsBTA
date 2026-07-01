package biscuitius.blueprints.client.hologram;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocalMultiplayer;
import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicBed;
import net.minecraft.core.block.BlockLogicDoor;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemBed;
import net.minecraft.core.item.ItemDoor;
import net.minecraft.core.item.ItemFireStriker;
import net.minecraft.core.item.ItemPlaceable;
import net.minecraft.core.item.ItemSign;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.block.ItemBlock;
import net.minecraft.core.net.packet.PacketMovePlayer.Rot;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.Vec3;
import net.minecraft.core.world.World;

public final class HologramController {
   private static final float[] YAW_CANDIDATES = new float[]{0.0F, 90.0F, 180.0F, -90.0F};
   private static final float[] PITCH_CANDIDATES = new float[]{0.0F, -89.0F, 89.0F};
   private static final double[] HIT_CANDIDATES = new double[]{0.1, 0.9};

   private HologramController() {
   }

   public static boolean tryPlace(World world, Player player, ItemStack stack, int x, int y, int z, Side side, double xHit, double yHit) {
      if (world != null && stack != null && player != null) {
         Item item = stack.getItem();
         if (item == null) {
            return false;
         } else if (!isPlacementItem(item)) {
            return false;
         } else {
            HologramPlacementContext.begin(world);

            try {
               return item.onUseItemOnBlock(stack, player, world, x, y, z, side, xHit, yHit);
            } catch (Throwable var21) {
               if (item instanceof ItemBlock) {
                  Block<?> block = ((ItemBlock)item).getBlock();
                  if (block != null) {
                     int tx = x;
                     int ty = y;
                     int tz = z;
                     if (!isReplaceable(world, x, y, z)) {
                        tx = x + side.getOffsetX();
                        ty = y + side.getOffsetY();
                        tz = z + side.getOffsetZ();
                     }

                     if (ty >= 0 && ty < world.getHeightBlocks() && isReplaceable(world, tx, ty, tz)) {
                        HologramStore.put(world, tx, ty, tz, new HologramBlock(block.id(), stack.getMetadata()));
                        return true;
                     }
                  }
               }

               return false;
            } finally {
               HologramPlacementContext.end();
            }
         }
      } else {
         return false;
      }
   }

   public static boolean isPlacementItem(Item item) {
      return item == null
         ? false
         : item instanceof ItemBlock
            || item instanceof ItemPlaceable
            || item instanceof ItemDoor
            || item instanceof ItemBed
            || item instanceof ItemSign
            || item instanceof ItemFireStriker;
   }

   public static boolean isReplaceable(World world, int x, int y, int z) {
      HologramBlock h = HologramStore.get(world, x, y, z);
      if (h == null) {
         return world.canPlaceInsideBlock(x, y, z);
      } else {
         Block<?> b = Blocks.blocksList[h.blockId];
         return b == null || b.getMaterial().isReplaceable();
      }
   }

   public static HologramBlock tryBreak(World world, int x, int y, int z) {
      if (world == null) {
         return null;
      } else {
         HologramBlock removed = HologramStore.remove(world, x, y, z);
         if (removed == null) {
            return null;
         } else {
            removeLinkedPart(world, x, y, z, removed);
            HologramStore.recomputeBounds(world);
            return removed;
         }
      }
   }

   private static void removeLinkedPart(World world, int x, int y, int z, HologramBlock removed) {
      Block<?> block = Blocks.blocksList[removed.blockId];
      if (block != null) {
         if (block.getLogic() instanceof BlockLogicDoor) {
            BlockLogicDoor logic = (BlockLogicDoor)block.getLogic();
            int partnerY = logic.isTop ? y - 1 : y + 1;
            HologramBlock partner = HologramStore.get(world, x, partnerY, z);
            if (partner != null) {
               Block<?> partnerBlock = Blocks.blocksList[partner.blockId];
               if (partnerBlock != null && partnerBlock.getLogic() instanceof BlockLogicDoor) {
                  HologramStore.remove(world, x, partnerY, z);
               }
            }
         } else {
            if (block.getLogic() instanceof BlockLogicBed) {
               int[][] horiz = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};

               for (int[] d : horiz) {
                  HologramBlock partner = HologramStore.get(world, x + d[0], y + d[1], z + d[2]);
                  if (partner != null && partner.blockId == removed.blockId) {
                     HologramStore.remove(world, x + d[0], y + d[1], z + d[2]);
                     return;
                  }
               }
            }
         }
      }
   }

   public static boolean tryFulfill(Minecraft mc, Player player, ItemStack held, int hx, int hy, int hz) {
      if (mc != null && mc.currentWorld != null && mc.playerController != null && player != null && held != null) {
         HologramBlock h = HologramStore.get(mc.currentWorld, hx, hy, hz);
         if (h == null) {
            return false;
         } else {
            Block<?> block = Blocks.blocksList[h.blockId];
            if (block == null) {
               return false;
            } else {
               ItemStack[] pick;
               try {
                  pick = block.getBreakResult(mc.currentWorld, EnumDropCause.PICK_BLOCK, hx, hy, hz, h.metadata, null);
               } catch (Throwable var22) {
                  pick = null;
               }

               if (pick != null && pick.length != 0 && pick[0] != null) {
                  ItemStack want = pick[0];
                  if (held.itemID == want.itemID && held.getMetadata() == want.getMetadata()) {
                     Side[] sides = Side.values();

                     for (Side s : sides) {
                        if (s != Side.NONE) {
                           int nx = hx - s.getOffsetX();
                           int ny = hy - s.getOffsetY();
                           int nz = hz - s.getOffsetZ();
                           if (ny >= 0 && ny < mc.currentWorld.getHeightBlocks()) {
                              int id = mc.currentWorld.getBlockId(nx, ny, nz);
                              if (id != 0) {
                                 Block<?> neighbour = Blocks.blocksList[id];
                                 if (neighbour != null && !neighbour.getMaterial().isReplaceable()) {
                                    HologramController.ForcedRotation forced = findRotationForMeta(
                                       player, held, mc.currentWorld, nx, ny, nz, s, hx, hy, hz, h.metadata
                                    );
                                    if (forced != null) {
                                       boolean placed = placeWithRotation(mc, player, held, nx, ny, nz, s, forced.xHit, forced.yHit, forced.yaw, forced.pitch);
                                       if (placed) {
                                          HologramStore.remove(mc.currentWorld, hx, hy, hz);
                                       }

                                       return placed;
                                    }
                                 }
                              }
                           }
                        }
                     }

                     return false;
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   private static HologramController.ForcedRotation findRotationForMeta(
      Player player, ItemStack stack, World world, int anchorX, int anchorY, int anchorZ, Side side, int tx, int ty, int tz, int targetMeta
   ) {
      float origYaw = player.yRot;
      float origPitch = player.xRot;

      try {
         for (float yaw : YAW_CANDIDATES) {
            for (float pitch : PITCH_CANDIDATES) {
               for (double yHit : HIT_CANDIDATES) {
                  player.yRot = yaw;
                  player.xRot = pitch;
                  Integer meta = dryRunMetadata(player, stack, world, anchorX, anchorY, anchorZ, side, 0.5, yHit, tx, ty, tz);
                  if (meta != null && meta == targetMeta) {
                     return new HologramController.ForcedRotation(yaw, pitch, 0.5, yHit);
                  }
               }
            }
         }

         return null;
      } finally {
         player.yRot = origYaw;
         player.xRot = origPitch;
      }
   }

   private static Integer dryRunMetadata(
      Player player, ItemStack stack, World world, int anchorX, int anchorY, int anchorZ, Side side, double xHit, double yHit, int tx, int ty, int tz
   ) {
      Item item = stack.getItem();
      if (item == null) {
         return null;
      } else {
         ItemStack copy = stack.copy();
         HologramPlacementContext.beginDryRun(world);

         Object captured;
         try {
            boolean ok;
            try {
               ok = item.onUseItemOnBlock(copy, player, world, anchorX, anchorY, anchorZ, side, xHit, yHit);
            } catch (Throwable var22) {
               return null;
            }

            if (ok) {
               int[] capturedx = HologramPlacementContext.captureRead(tx, ty, tz);
               return capturedx != null ? capturedx[1] : null;
            }

            captured = null;
         } finally {
            HologramPlacementContext.end();
         }

         return (Integer)captured;
      }
   }

   private static boolean placeWithRotation(
      Minecraft mc,
      Player player,
      ItemStack held,
      int anchorX,
      int anchorY,
      int anchorZ,
      Side side,
      double xHit,
      double yHit,
      float forcedYaw,
      float forcedPitch
   ) {
      PacketHandlerClient sendQueue = player instanceof PlayerLocalMultiplayer ? ((PlayerLocalMultiplayer)player).sendQueue : null;
      float origYaw = player.yRot;
      float origPitch = player.xRot;
      boolean onGround = player.onGround;

      boolean var18;
      try {
         if (sendQueue != null) {
            sendQueue.addToSendQueue(new Rot(forcedYaw, forcedPitch, onGround));
         }

         player.yRot = forcedYaw;
         player.xRot = forcedPitch;
         boolean placed = mc.playerController.placeItemStackOnTile(player, mc.currentWorld, held, anchorX, anchorY, anchorZ, side, xHit, yHit);
         var18 = placed;
      } finally {
         player.yRot = origYaw;
         player.xRot = origPitch;
         if (sendQueue != null) {
            sendQueue.addToSendQueue(new Rot(origYaw, origPitch, onGround));
         }
      }

      return var18;
   }

   public static HitResult pickHologramOverlay(World world, Vec3 start, Vec3 end, HitResult realHit) {
      if (world == null || start == null || end == null) {
         return realHit;
      } else if (!HologramStore.hasEntries(world)) {
         return realHit;
      } else {
         double dx = end.x - start.x;
         double dy = end.y - start.y;
         double dz = end.z - start.z;
         double segLenSq = dx * dx + dy * dy + dz * dz;
         if (segLenSq <= 0.0) {
            return realHit;
         } else {
            double maxDistSq = realHit == null ? segLenSq : sqDist(start, realHit.location);
            int bx = floor(start.x);
            int by = floor(start.y);
            int bz = floor(start.z);
            int stepX = dx > 0.0 ? 1 : (dx < 0.0 ? -1 : 0);
            int stepY = dy > 0.0 ? 1 : (dy < 0.0 ? -1 : 0);
            int stepZ = dz > 0.0 ? 1 : (dz < 0.0 ? -1 : 0);
            double tMaxX = stepX == 0 ? Double.POSITIVE_INFINITY : ((stepX > 0 ? bx + 1 : bx) - start.x) / dx;
            double tMaxY = stepY == 0 ? Double.POSITIVE_INFINITY : ((stepY > 0 ? by + 1 : by) - start.y) / dy;
            double tMaxZ = stepZ == 0 ? Double.POSITIVE_INFINITY : ((stepZ > 0 ? bz + 1 : bz) - start.z) / dz;
            double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dx);
            double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dy);
            double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dz);
            HitResult best = null;
            double bestSqDist = maxDistSq;
            int maxSteps = 256;

            for (int i = 0; i <= maxSteps; i++) {
               if (HologramStore.hasSectionHolograms(world, bx, by, bz)) {
                  HologramBlock h = HologramStore.get(world, bx, by, bz);
                  if (h != null && HologramAppearance.isYVisible(by)) {
                     HitResult candidate = rayTraceHologram(world, bx, by, bz, h, start, end);
                     if (candidate != null) {
                        double sqd = sqDist(start, candidate.location);
                        if (sqd < bestSqDist) {
                           return candidate;
                        }
                     }
                  }
               }

               double tNext;
               if (tMaxX < tMaxY) {
                  if (tMaxX < tMaxZ) {
                     tNext = tMaxX;
                     bx += stepX;
                     tMaxX += tDeltaX;
                  } else {
                     tNext = tMaxZ;
                     bz += stepZ;
                     tMaxZ += tDeltaZ;
                  }
               } else if (tMaxY < tMaxZ) {
                  tNext = tMaxY;
                  by += stepY;
                  tMaxY += tDeltaY;
               } else {
                  tNext = tMaxZ;
                  bz += stepZ;
                  tMaxZ += tDeltaZ;
               }

               if (tNext > 1.0) {
                  break;
               }

               double walkedSq = dx * tNext * (dx * tNext) + dy * tNext * (dy * tNext) + dz * tNext * (dz * tNext);
               if (walkedSq > bestSqDist) {
                  break;
               }
            }

            return best != null ? best : realHit;
         }
      }
   }

   private static HitResult rayTraceHologram(World world, int x, int y, int z, HologramBlock h, Vec3 start, Vec3 end) {
      Block<?> block = Blocks.blocksList[h.blockId];
      if (block == null) {
         double dx = end.x - start.x;
         double dy = end.y - start.y;
         double dz = end.z - start.z;
         return rayVsUnitCube(start, dx, dy, dz, x, y, z);
      } else {
         HologramPlacementContext.begin(world);

         HitResult var15;
         try {
            return block.collisionRayTrace(world, x, y, z, start, end, true);
         } catch (Throwable var19) {
            double dx = end.x - start.x;
            double dy = end.y - start.y;
            double dz = end.z - start.z;
            var15 = rayVsUnitCube(start, dx, dy, dz, x, y, z);
         } finally {
            HologramPlacementContext.end();
         }

         return var15;
      }
   }

   public static AABB getHologramSelectionBox(World world, int x, int y, int z) {
      if (world == null) {
         return null;
      } else {
         HologramBlock h = HologramStore.get(world, x, y, z);
         if (h == null) {
            return null;
         } else {
            Block<?> block = Blocks.blocksList[h.blockId];
            if (block == null) {
               return null;
            } else {
               HologramPlacementContext.begin(world);

               Object var7;
               try {
                  return block.getSelectedBoundingBoxFromPool(world, x, y, z);
               } catch (Throwable var11) {
                  var7 = null;
               } finally {
                  HologramPlacementContext.end();
               }

               return (AABB)var7;
            }
         }
      }
   }

   private static int floor(double v) {
      int fi = (int)v;
      return v < fi ? fi - 1 : fi;
   }

   private static double sqDist(Vec3 a, Vec3 b) {
      double dx = a.x - b.x;
      double dy = a.y - b.y;
      double dz = a.z - b.z;
      return dx * dx + dy * dy + dz * dz;
   }

   private static HitResult rayVsUnitCube(Vec3 start, double dx, double dy, double dz, int bx, int by, int bz) {
      double tMin = Double.NEGATIVE_INFINITY;
      double tMax = Double.POSITIVE_INFINITY;
      int enterAxis = -1;
      boolean enterPositiveDir = false;
      if (dx != 0.0) {
         double t1 = (bx - start.x) / dx;
         double t2 = (bx + 1.0 - start.x) / dx;
         double near;
         double far;
         if (t1 < t2) {
            near = t1;
            far = t2;
         } else {
            near = t2;
            far = t1;
         }

         if (near > tMin) {
            tMin = near;
            enterAxis = 0;
            enterPositiveDir = dx > 0.0;
         }

         if (far < tMax) {
            tMax = far;
         }
      } else if (start.x < bx || start.x > bx + 1.0) {
         return null;
      }

      if (dy != 0.0) {
         double t1x = (by - start.y) / dy;
         double t2x = (by + 1.0 - start.y) / dy;
         double nearx;
         double farx;
         if (t1x < t2x) {
            nearx = t1x;
            farx = t2x;
         } else {
            nearx = t2x;
            farx = t1x;
         }

         if (nearx > tMin) {
            tMin = nearx;
            enterAxis = 1;
            enterPositiveDir = dy > 0.0;
         }

         if (farx < tMax) {
            tMax = farx;
         }
      } else if (start.y < by || start.y > by + 1.0) {
         return null;
      }

      if (dz != 0.0) {
         double t1xx = (bz - start.z) / dz;
         double t2xx = (bz + 1.0 - start.z) / dz;
         double nearxx;
         double farxx;
         if (t1xx < t2xx) {
            nearxx = t1xx;
            farxx = t2xx;
         } else {
            nearxx = t2xx;
            farxx = t1xx;
         }

         if (nearxx > tMin) {
            tMin = nearxx;
            enterAxis = 2;
            enterPositiveDir = dz > 0.0;
         }

         if (farxx < tMax) {
            tMax = farxx;
         }
      } else if (start.z < bz || start.z > bz + 1.0) {
         return null;
      }

      if (tMin > tMax || tMax < 0.0) {
         return null;
      } else if (tMin < 0.0) {
         Side side = inferSide(dx, dy, dz);
         return new HitResult(bx, by, bz, side, Vec3.getTempVec3(start.x, start.y, start.z));
      } else {
         double hx = start.x + dx * tMin;
         double hy = start.y + dy * tMin;
         double hz = start.z + dz * tMin;
         Side side;
         switch (enterAxis) {
            case 0:
               side = enterPositiveDir ? Side.WEST : Side.EAST;
               break;
            case 1:
               side = enterPositiveDir ? Side.BOTTOM : Side.TOP;
               break;
            case 2:
               side = enterPositiveDir ? Side.NORTH : Side.SOUTH;
               break;
            default:
               side = inferSide(dx, dy, dz);
         }

         return new HitResult(bx, by, bz, side, Vec3.getTempVec3(hx, hy, hz));
      }
   }

   private static Side inferSide(double dx, double dy, double dz) {
      double ax = Math.abs(dx);
      double ay = Math.abs(dy);
      double az = Math.abs(dz);
      if (ay >= ax && ay >= az) {
         return dy < 0.0 ? Side.TOP : Side.BOTTOM;
      } else if (ax >= az) {
         return dx < 0.0 ? Side.EAST : Side.WEST;
      } else {
         return dz < 0.0 ? Side.SOUTH : Side.NORTH;
      }
   }

   private static final class ForcedRotation {
      final float yaw;
      final float pitch;
      final double xHit;
      final double yHit;

      ForcedRotation(float yaw, float pitch, double xHit, double yHit) {
         this.yaw = yaw;
         this.pitch = pitch;
         this.xHit = xHit;
         this.yHit = yHit;
      }
   }
}
