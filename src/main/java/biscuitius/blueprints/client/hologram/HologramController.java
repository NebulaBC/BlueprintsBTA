package biscuitius.blueprints.client.hologram;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocalMultiplayer;
import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicBed;
import net.minecraft.core.block.BlockLogicConduit;
import net.minecraft.core.block.BlockLogicDoor;
import net.minecraft.core.block.BlockLogicFenceGate;
import net.minecraft.core.block.BlockLogicFire;
import net.minecraft.core.block.BlockLogicLamp;
import net.minecraft.core.block.BlockLogicRepeater;
import net.minecraft.core.block.BlockLogicTimer;
import net.minecraft.core.block.BlockLogicTrapDoor;
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
import net.minecraft.core.player.gamemode.Gamemodes;
import net.minecraft.core.player.inventory.container.ContainerInventory;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.HitResult.Tile;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBdc;

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
         }

         if (!isPlacementItem(item)) {
            return false;
         }

         if (item instanceof ItemFireStriker && !isLegalFireStrike(world, x, y, z, side)) {
            return false;
         }

         boolean placed = false;
         HologramPlacementContext.begin(world);

         try {
            placed = item.onUseItemOnBlock(stack, player, world, x, y, z, side, xHit, yHit);
         } catch (Throwable t) {
            if (item instanceof ItemBlock) {
               Block<?> block = ((ItemBlock)item).getBlock();
               if (block != null) {
                  int tx = x;
                  int ty = y;
                  int tz = z;
                  if (!isReplaceable(world, x, y, z)) {
                     tx += side.offsetX();
                     ty += side.offsetY();
                     tz += side.offsetZ();
                  }

                  if (ty >= 0 && ty < world.getHeightBlocks() && isReplaceable(world, tx, ty, tz)) {
                     HologramStore.put(world, tx, ty, tz, new HologramBlock(block.id(), stack.getMetadata()));
                     HologramPlacementContext.recordTouch(tx, ty, tz);
                     placed = true;
                  }
               }
            }
         } finally {
            HologramPlacementContext.end();
         }

         return placed;
      } else {
         return false;
      }
   }

   public static boolean tryInteract(World world, Player player, int x, int y, int z, Side side, double xHit, double yHit) {
      if (world != null && player != null) {
         HologramBlock state = HologramStore.get(world, x, y, z);
         if (state == null) {
            return false;
         } else {
            Block<?> block = state.blockId > 0 && state.blockId < Blocks.blocksList.length ? Blocks.blocksList[state.blockId] : null;
            if (block != null && isManuallyToggleable(block)) {
               Boolean result = HologramSimulationContext.call(world, () -> {
                  try {
                     return block.onBlockRightClicked(world, x, y, z, player, side, xHit, yHit);
                  } catch (Throwable t) {
                     return false;
                  }
               });
               return Boolean.TRUE.equals(result);
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean isManuallyToggleable(Block<?> block) {
      Object logic = block.getLogic();
      return logic instanceof BlockLogicDoor
         || logic instanceof BlockLogicTrapDoor
         || logic instanceof BlockLogicFenceGate
         || logic instanceof BlockLogicLamp
         || logic instanceof BlockLogicRepeater
         || logic instanceof BlockLogicTimer
         || logic instanceof BlockLogicConduit;
   }

   private static boolean isLegalFireStrike(World world, int x, int y, int z, Side side) {
      int fx = x + side.offsetX();
      int fy = y + side.offsetY();
      int fz = z + side.offsetZ();
      if (Blocks.FIRE.getLogic() instanceof BlockLogicFire fire) {
         Boolean ok = HologramSimulationContext.call(world, () -> {
            try {
               return fire.canPlaceAt(world, new TilePos(fx, fy, fz));
            } catch (Throwable t) {
               return false;
            }
         });
         return Boolean.TRUE.equals(ok);
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
      }

      Block<?> b = Blocks.blocksList[h.blockId];
      return b == null || b.getMaterial().isReplaceable();
   }

   public static HologramBlock tryBreak(World world, int x, int y, int z) {
      if (world == null) {
         return null;
      }

      HologramBlock removed = HologramStore.remove(world, x, y, z);
      if (removed == null) {
         return null;
      }

      removeLinkedPart(world, x, y, z, removed);
      HologramStore.recomputeBounds(world);
      return removed;
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
         }

         Block<?> block = Blocks.blocksList[h.blockId];
         if (block == null) {
            return false;
         }

         ItemStack[] pick;
         try {
            pick = block.getBreakResult(mc.currentWorld, EnumDropCause.PICK_BLOCK, hx, hy, hz, h.metadata, null);
         } catch (Throwable t) {
            pick = null;
         }

         if (pick != null && pick.length != 0 && pick[0] != null) {
            ItemStack want = pick[0];
            if (held.itemID == want.itemID && held.getMetadata() == want.getMetadata()) {
               Side[] sides = Side.values();

               for (Side s : sides) {
                  if (s != Side.NONE) {
                     int nx = hx - s.offsetX();
                     int ny = hy - s.offsetY();
                     int nz = hz - s.offsetZ();
                     if (ny >= 0 && ny < mc.currentWorld.getHeightBlocks()) {
                        int id = mc.currentWorld.getBlockId(nx, ny, nz);
                        if (id != 0) {
                           Block<?> neighbour = Blocks.blocksList[id];
                           if (neighbour != null && !neighbour.getMaterial().isReplaceable()) {
                              HologramController.ForcedRotation forced = findRotationForMeta(
                                 player, held, mc.currentWorld, nx, ny, nz, s, hx, hy, hz, h.metadata
                              );
                              if (forced != null) {
                                 return placeWithRotation(mc, player, held, nx, ny, nz, s, forced.xHit, forced.yHit, forced.yaw, forced.pitch);
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
      } finally {
         player.yRot = origYaw;
         player.xRot = origPitch;
      }

      return null;
   }

   private static Integer dryRunMetadata(
      Player player, ItemStack stack, World world, int anchorX, int anchorY, int anchorZ, Side side, double xHit, double yHit, int tx, int ty, int tz
   ) {
      Item item = stack.getItem();
      if (item == null) {
         return null;
      }

      ItemStack copy = stack.copy();
      HologramPlacementContext.beginDryRun(world);

      try {
         boolean ok;
         try {
            ok = item.onUseItemOnBlock(copy, player, world, anchorX, anchorY, anchorZ, side, xHit, yHit);
         } catch (Throwable t) {
            return null;
         }

         if (!ok) {
            return null;
         }

         int[] captured = HologramPlacementContext.captureRead(tx, ty, tz);
         return captured != null ? captured[1] : null;
      } finally {
         HologramPlacementContext.end();
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

      try {
         if (sendQueue != null) {
            sendQueue.addToSendQueue(new Rot(forcedYaw, forcedPitch, onGround));
         }

         player.yRot = forcedYaw;
         player.xRot = forcedPitch;
         return mc.playerController.placeItemStackOnTile(player, mc.currentWorld, held, new TilePos(anchorX, anchorY, anchorZ), side, xHit, yHit);
      } finally {
         player.yRot = origYaw;
         player.xRot = origPitch;
         if (sendQueue != null) {
            sendQueue.addToSendQueue(new Rot(origYaw, origPitch, onGround));
         }
      }
   }

   public static HitResult pickHologramOverlay(World world, Vector3dc start, Vector3dc end, HitResult realHit) {
      if (world == null || start == null || end == null) {
         return realHit;
      }

      if (!HologramStore.hasEntries(world)) {
         return realHit;
      }

      double dx = end.x() - start.x();
      double dy = end.y() - start.y();
      double dz = end.z() - start.z();
      double segLenSq = dx * dx + dy * dy + dz * dz;
      if (segLenSq <= 0.0) {
         return realHit;
      }

      double maxDistSq = realHit == null ? segLenSq : sqDist(start, realHit.location);
      int bx = floor(start.x());
      int by = floor(start.y());
      int bz = floor(start.z());
      int stepX = dx > 0.0 ? 1 : (dx < 0.0 ? -1 : 0);
      int stepY = dy > 0.0 ? 1 : (dy < 0.0 ? -1 : 0);
      int stepZ = dz > 0.0 ? 1 : (dz < 0.0 ? -1 : 0);
      double tMaxX = stepX == 0 ? Double.POSITIVE_INFINITY : ((stepX > 0 ? bx + 1 : bx) - start.x()) / dx;
      double tMaxY = stepY == 0 ? Double.POSITIVE_INFINITY : ((stepY > 0 ? by + 1 : by) - start.y()) / dy;
      double tMaxZ = stepZ == 0 ? Double.POSITIVE_INFINITY : ((stepZ > 0 ? bz + 1 : bz) - start.z()) / dz;
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

   private static HitResult rayTraceHologram(World world, int x, int y, int z, HologramBlock h, Vector3dc start, Vector3dc end) {
      Block<?> block = Blocks.blocksList[h.blockId];
      if (block == null) {
         double dx = end.x() - start.x();
         double dy = end.y() - start.y();
         double dz = end.z() - start.z();
         return rayVsUnitCube(start, dx, dy, dz, x, y, z);
      }

      HologramPlacementContext.begin(world);

      try {
         return block.collisionRayTrace(world, x, y, z, start, end, true);
      } catch (Throwable t) {
         double dx = end.x() - start.x();
         double dy = end.y() - start.y();
         double dz = end.z() - start.z();
         return rayVsUnitCube(start, dx, dy, dz, x, y, z);
      } finally {
         HologramPlacementContext.end();
      }
   }

   public static AABBdc getHologramSelectionBox(World world, int x, int y, int z) {
      if (world == null) {
         return null;
      }

      HologramBlock h = HologramStore.get(world, x, y, z);
      if (h == null) {
         return null;
      }

      Block<?> block = Blocks.blocksList[h.blockId];
      if (block == null) {
         return null;
      }

      HologramPlacementContext.begin(world);

      try {
         return block.getSelectedBoundingBoxFromPool(world, x, y, z);
      } catch (Throwable t) {
         return null;
      } finally {
         HologramPlacementContext.end();
      }
   }

   private static int floor(double v) {
      int fi = (int)v;
      return v < fi ? fi - 1 : fi;
   }

   private static double sqDist(Vector3dc a, Vector3dc b) {
      double dx = a.x() - b.x();
      double dy = a.y() - b.y();
      double dz = a.z() - b.z();
      return dx * dx + dy * dy + dz * dz;
   }

   private static HitResult rayVsUnitCube(Vector3dc start, double dx, double dy, double dz, int bx, int by, int bz) {
      double tMin = Double.NEGATIVE_INFINITY;
      double tMax = Double.POSITIVE_INFINITY;
      int enterAxis = -1;
      boolean enterPositiveDir = false;
      if (dx != 0.0) {
         double t1 = (bx - start.x()) / dx;
         double t2 = (bx + 1.0 - start.x()) / dx;
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
      } else if (start.x() < bx || start.x() > bx + 1.0) {
         return null;
      }

      if (dy != 0.0) {
         double t1 = (by - start.y()) / dy;
         double t2 = (by + 1.0 - start.y()) / dy;
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
            enterAxis = 1;
            enterPositiveDir = dy > 0.0;
         }

         if (far < tMax) {
            tMax = far;
         }
      } else if (start.y() < by || start.y() > by + 1.0) {
         return null;
      }

      if (dz != 0.0) {
         double t1 = (bz - start.z()) / dz;
         double t2 = (bz + 1.0 - start.z()) / dz;
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
            enterAxis = 2;
            enterPositiveDir = dz > 0.0;
         }

         if (far < tMax) {
            tMax = far;
         }
      } else if (start.z() < bz || start.z() > bz + 1.0) {
         return null;
      }

      if (tMin > tMax || tMax < 0.0) {
         return null;
      }

      if (tMin < 0.0) {
         Side side = inferSide(dx, dy, dz);
         return new Tile(new TilePos(bx, by, bz), side, new Vector3d(start.x(), start.y(), start.z()));
      }

      double hx = start.x() + dx * tMin;
      double hy = start.y() + dy * tMin;
      double hz = start.z() + dz * tMin;

      return new Tile(new TilePos(bx, by, bz), switch (enterAxis) {
         case 0 -> enterPositiveDir ? Side.WEST : Side.EAST;
         case 1 -> enterPositiveDir ? Side.BOTTOM : Side.TOP;
         case 2 -> enterPositiveDir ? Side.NORTH : Side.SOUTH;
         default -> inferSide(dx, dy, dz);
      }, new Vector3d(hx, hy, hz));
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

   public static void pickHologramBlock(Player player, World world, HologramBlock h, int x, int y, int z) {
      if (player != null && world != null && h != null) {
         Block<?> block = Blocks.blocksList[h.blockId];
         if (block != null) {
            ItemStack[] result;
            try {
               result = block.getBreakResult(world, EnumDropCause.PICK_BLOCK, x, y, z, h.metadata, null);
            } catch (Throwable t) {
               return;
            }

            if (result != null && result.length != 0 && result[0] != null) {
               ItemStack selectItem = result[0];
               ContainerInventory inv = player.inventory;
               int hotbarOffset = inv.getHotbarOffset();

               for (int i = 0; i < 9; i++) {
                  ItemStack s = inv.getItem(i + hotbarOffset);
                  if (s != null && s.itemID == selectItem.itemID && s.getMetadata() == selectItem.getMetadata()) {
                     player.setCurrentItem(i + hotbarOffset);
                     return;
                  }
               }

               int emptyHotbarSlot = -1;
               int destSlot = inv.getCurrentSlot();

               for (int i = 0; i < 9; i++) {
                  if (inv.getItem(i + hotbarOffset) == null) {
                     emptyHotbarSlot = i + hotbarOffset;
                     destSlot = emptyHotbarSlot;
                     break;
                  }
               }

               int itemSlot = -1;
               int stackSize = -1;

               for (int i = 0; i < 36; i++) {
                  ItemStack s = inv.getItem(i);
                  if (s != null && s.itemID == selectItem.itemID && s.getMetadata() == selectItem.getMetadata() && (stackSize == -1 || s.stackSize < stackSize)
                     )
                   {
                     itemSlot = i;
                     stackSize = s.stackSize;
                  }
               }

               if (itemSlot != -1) {
                  player.swapItems(destSlot, itemSlot);
                  player.setCurrentItem(destSlot);
               } else {
                  if (player.getGamemode() == Gamemodes.CREATIVE) {
                     int emptySlot = -1;

                     for (int i = 0; i < 36; i++) {
                        if (inv.getItem(i) == null) {
                           emptySlot = i;
                           break;
                        }
                     }

                     int insert = emptyHotbarSlot != -1 ? emptyHotbarSlot : inv.getCurrentSlot();
                     selectItem.stackSize = 1;
                     if (emptySlot != -1) {
                        player.swapItems(emptySlot, insert);
                     }

                     inv.setItem(insert, selectItem);
                     player.setCurrentItem(insert);
                  }
               }
            }
         }
      }
   }

   public static int pasteBlueprint(Minecraft mc) {
      if (mc != null && mc.currentWorld != null) {
         if (mc.isMultiplayerWorld()) {
            return 0;
         }

         World world = mc.currentWorld;
         if (!HologramStore.hasEntries(world)) {
            return 0;
         }

         List<int[]> coords = new ArrayList<>();
         HologramStore.forEach(world, (xx, yx, zx, h) -> coords.add(new int[]{xx, yx, zx, h.blockId, h.metadata}));
         if (coords.isEmpty()) {
            return 0;
         }

         coords.sort((a, b) -> Integer.compare(a[1], b[1]));
         int placed = 0;
         List<int[]> written = new ArrayList<>();

         for (int[] c : coords) {
            int x = c[0];
            int y = c[1];
            int z = c[2];
            int id = c[3];
            int meta = c[4];
            if (y >= 0 && y < world.getHeightBlocks()) {
               try {
                  if (world.getTileEntity(x, y, z) != null) {
                     world.removeBlockTileEntity(x, y, z);
                  }

                  if (world.setBlockAndMetadataRaw(x, y, z, id, meta)) {
                     placed++;
                     written.add(c);
                  }
               } catch (Throwable var14) {
               }

               HologramStore.remove(world, x, y, z);
            } else {
               HologramStore.remove(world, x, y, z);
            }
         }

         for (int[] c : written) {
            int x = c[0];
            int y = c[1];
            int z = c[2];
            int id = c[3];

            try {
               Block<?> block = Blocks.blocksList[id & 16383];
               if (block != null) {
                  block.onBlockPlacedByWorld(world, x, y, z);
               }

               world.notifyBlockChange(x, y, z, id);
            } catch (Throwable var13) {
            }
         }

         HologramStore.recomputeBounds(world);
         return placed;
      } else {
         return 0;
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
