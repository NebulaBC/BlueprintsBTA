package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.BlueprintSelection;
import biscuitius.blueprints.client.DesignModeOverlay;
import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.MoveToolController;
import biscuitius.blueprints.client.ScreenBlueprintBrowser;
import biscuitius.blueprints.client.hologram.BlueprintTransform;
import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.HologramController;
import biscuitius.blueprints.client.hologram.HologramStore;
import biscuitius.blueprints.client.hologram.HologramTileEntities;
import biscuitius.blueprints.client.item.BlueprintItem;
import biscuitius.blueprints.client.item.ClipboardToolItem;
import biscuitius.blueprints.client.item.FillToolItem;
import biscuitius.blueprints.client.item.LineToolItem;
import biscuitius.blueprints.client.item.MoveToolItem;
import biscuitius.blueprints.client.item.OvalToolItem;
import biscuitius.blueprints.client.item.RectangleToolItem;
import biscuitius.blueprints.client.item.ReplaceToolItem;
import biscuitius.blueprints.client.item.RotateToolItem;
import biscuitius.blueprints.client.tool.ShapeToolState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.option.GameSettings;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.enums.EnumBlockSoundEffectType;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.Axis;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.HitResult.Tile;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class DesignModeClickMixin {
   @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
   private void blueprints$handleDesignModeClick(int clickType, boolean attack, boolean repeat, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         Minecraft mc = (Minecraft)(Object)this;
         if (mc.currentWorld == null) {
            ci.cancel();
         } else {
            PlayerLocal designPlayer = DesignModeState.getDesignPlayer();
            if (designPlayer == null) {
               ci.cancel();
            } else {
               MinecraftAccessor accessor = (MinecraftAccessor)mc;
               accessor.setMouseTicksRan(accessor.getTicksRan());
               HitResult hit = mc.objectMouseOver;
               ItemStack heldStack = designPlayer.inventory.getCurrentItem();
               boolean holdingBlueprint = BlueprintItem.is(heldStack);
               boolean sneaking = GameSettings.KEY_SNEAK.isPressed();
               if (MoveToolItem.is(heldStack)) {
                  if (clickType == 1 && !repeat) {
                     MoveToolController.toggleArmed(mc);
                  }

                  ci.cancel();
               } else if (RotateToolItem.is(heldStack)) {
                  if (!repeat && mc.currentWorld != null) {
                     if (clickType == 0) {
                        BlueprintTransform.rotate(mc.currentWorld, BlueprintTransform.Rotation.CCW);
                     } else if (clickType == 1) {
                        if (sneaking) {
                           BlueprintTransform.flip(mc.currentWorld, BlueprintTransform.FlipAxis.X);
                        } else {
                           BlueprintTransform.rotate(mc.currentWorld, BlueprintTransform.Rotation.CW);
                        }
                     }
                  }

                  ci.cancel();
               } else if (LineToolItem.is(heldStack)) {
                  this.blueprints$handleShapeTool(mc, designPlayer, hit, clickType, repeat, sneaking, ShapeToolState.Shape.LINE, "Line");
                  ci.cancel();
               } else if (FillToolItem.is(heldStack)) {
                  this.blueprints$handleShapeTool(mc, designPlayer, hit, clickType, repeat, sneaking, ShapeToolState.Shape.FILL, "Fill");
                  ci.cancel();
               } else if (RectangleToolItem.is(heldStack)) {
                  this.blueprints$handleShapeTool(mc, designPlayer, hit, clickType, repeat, sneaking, ShapeToolState.Shape.RECTANGLE, "Rectangle");
                  ci.cancel();
               } else if (OvalToolItem.is(heldStack)) {
                  this.blueprints$handleShapeTool(mc, designPlayer, hit, clickType, repeat, sneaking, ShapeToolState.Shape.OVAL, "Oval");
                  ci.cancel();
               } else if (ClipboardToolItem.is(heldStack) || ReplaceToolItem.is(heldStack)) {
                  ci.cancel();
               } else if (hit == null) {
                  if (holdingBlueprint) {
                     if (clickType == 1 && BlueprintSelection.getBox(mc.currentWorld) != null) {
                        mc.displayScreen(new ScreenBlueprintBrowser(null, ScreenBlueprintBrowser.Mode.CREATE_FROM_SELECTION));
                     } else if (clickType == 0 && sneaking && BlueprintSelection.hasAny(mc.currentWorld)) {
                        BlueprintSelection.clear(mc.currentWorld);
                        DesignModeOverlay.show("Selection cleared", 16733525);
                     }
                  }

                  ci.cancel();
               } else if (!(hit instanceof Tile tile)) {
                  ci.cancel();
               } else {
                  int x = tile.tilePos.x();
                  int y = tile.tilePos.y();
                  int z = tile.tilePos.z();
                  Side side = tile.side;
                  if (holdingBlueprint) {
                     if (HologramStore.get(mc.currentWorld, x, y, z) == null) {
                        if (clickType == 0) {
                           BlueprintSelection.setCornerA(mc.currentWorld, x, y, z);
                           DesignModeOverlay.show("Corner A: " + x + ", " + y + ", " + z, 16755285);
                           designPlayer.swingItem();
                        } else if (clickType == 1) {
                           BlueprintSelection.setCornerB(mc.currentWorld, x, y, z);
                           DesignModeOverlay.show("Corner B: " + x + ", " + y + ", " + z, 5614335);
                           designPlayer.swingItem();
                        }
                     }

                     ci.cancel();
                  } else if (clickType != 0) {
                     if (clickType == 1) {
                        HologramBlock hostBlock = HologramStore.get(mc.currentWorld, x, y, z);
                        if (!sneaking && hostBlock != null && HologramTileEntities.tryOpenEditor(mc.currentWorld, designPlayer, x, y, z, hostBlock)) {
                           designPlayer.swingItem();
                           ci.cancel();
                           return;
                        }

                        if (HologramStore.get(mc.currentWorld, x, y, z) != null
                           && HologramController.tryInteract(mc.currentWorld, designPlayer, x, y, z, side, xPlacedFor(side, hit, x, y, z), hit.location.y() - y)
                           )
                         {
                           designPlayer.swingItem();
                           ci.cancel();
                           return;
                        }

                        ItemStack stack = designPlayer.inventory.getCurrentItem();
                        if (stack == null) {
                           ci.cancel();
                           return;
                        }

                        double yPlaced = hit.location.y() - y;
                        double xPlaced = xPlacedFor(side, hit, x, y, z);
                        if (HologramController.tryPlace(mc.currentWorld, designPlayer, stack, x, y, z, side, xPlaced, yPlaced)) {
                           designPlayer.swingItem();
                           DesignModeState.shuffleAndGetItem(designPlayer.inventory);
                        }

                        ci.cancel();
                     }
                  } else {
                     HologramBlock removed = HologramController.tryBreak(mc.currentWorld, x, y, z);
                     if (removed != null) {
                        designPlayer.swingItem();
                        Block<?> block = removed.blockId > 0 && removed.blockId < Blocks.blocksList.length ? Blocks.blocksList[removed.blockId] : null;
                        if (block != null) {
                           mc.currentWorld.playBlockSoundEffect(null, x + 0.5, y + 0.5, z + 0.5, block, EnumBlockSoundEffectType.MINE);
                           if (mc.particleEngine != null) {
                              mc.particleEngine.destroy(new TilePos(x, y, z), removed.blockId, removed.metadata);
                           }
                        }
                     }

                     ci.cancel();
                  }
               }
            }
         }
      }
   }

   private static double xPlacedFor(Side side, HitResult hit, int x, int y, int z) {
      if (side.axis() == Axis.X) {
         return hit.location.x() - x;
      } else {
         return side.axis() == Axis.Z ? hit.location.z() - z : hit.location.x() - x;
      }
   }

   @Inject(method = "mineBlocks", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelVanillaHoldMining(boolean leftClickDown, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         ci.cancel();
      }
   }

   private void blueprints$handleShapeTool(
      Minecraft mc, PlayerLocal designPlayer, HitResult hit, int clickType, boolean repeat, boolean sneaking, ShapeToolState.Shape shape, String label
   ) {
      World world = mc.currentWorld;
      if (world != null) {
         if (hit == null) {
            if (!repeat) {
               if (clickType == 1) {
                  int placed = ShapeToolState.commit(world);
                  if (placed > 0) {
                     DesignModeOverlay.show(label + " placed (" + placed + " blocks)", 5635925);
                     designPlayer.swingItem();
                  }
               } else if (clickType == 0 && sneaking && ShapeToolState.hasAnyPoint(world)) {
                  ShapeToolState.clearPoints(world);
                  DesignModeOverlay.show(label + " cleared", 16733525);
               }
            }
         } else if (hit instanceof Tile) {
            if (!repeat) {
               Tile tile = (Tile)hit;
               int x = tile.tilePos.x();
               int y = tile.tilePos.y();
               int z = tile.tilePos.z();
               Side side = tile.side;
               if (clickType == 1 && sneaking) {
                  HologramBlock h = HologramStore.get(world, x, y, z);
                  int inkId;
                  int inkMeta;
                  if (h != null) {
                     inkId = h.blockId;
                     inkMeta = h.metadata;
                  } else {
                     inkId = world.getBlockId(x, y, z);
                     inkMeta = world.getBlockMetadata(x, y, z);
                  }

                  if (inkId > 0) {
                     ShapeToolState.setInk(world, inkId, inkMeta);
                     DesignModeOverlay.show("Ink set", 16777045);
                     designPlayer.swingItem();
                  }
               } else {
                  int px = x + side.offsetX();
                  int py = y + side.offsetY();
                  int pz = z + side.offsetZ();
                  if (clickType == 1) {
                     ShapeToolState.setPointA(world, px, py, pz, shape);
                     DesignModeOverlay.show("Point A: " + px + ", " + py + ", " + pz, 16755285);
                     designPlayer.swingItem();
                  } else if (clickType == 0) {
                     ShapeToolState.setPointB(world, px, py, pz, shape);
                     DesignModeOverlay.show("Point B: " + px + ", " + py + ", " + pz, 5614335);
                     designPlayer.swingItem();
                  }
               }
            }
         }
      }
   }
}
