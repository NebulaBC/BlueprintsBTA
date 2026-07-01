package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.BlueprintSelection;
import biscuitius.blueprints.client.DesignModeOverlay;
import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.ScreenBlueprintBrowser;
import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.HologramController;
import biscuitius.blueprints.client.hologram.HologramStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.enums.EnumBlockSoundEffectType;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.Axis;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.HitResult.HitType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class DesignModeClickMixin {
   @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
   private void blueprints$handleDesignModeClick(int clickType, boolean attack, boolean repeat, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc.currentWorld != null) {
            PlayerLocal designPlayer = DesignModeState.getDesignPlayer();
            if (designPlayer != null) {
               MinecraftAccessor accessor = (MinecraftAccessor)mc;
               accessor.setMouseTicksRan(accessor.getTicksRan());
               HitResult hit = mc.objectMouseOver;
               boolean emptyHand = designPlayer.inventory.getCurrentItem() == null;
               boolean sneaking = mc.gameSettings.keySneak.isPressed();
               if (hit == null) {
                  if (emptyHand && sneaking) {
                     if (clickType == 1 && BlueprintSelection.getBox(mc.currentWorld) != null) {
                        mc.displayScreen(new ScreenBlueprintBrowser(null, ScreenBlueprintBrowser.Mode.CREATE_FROM_SELECTION));
                     } else if (clickType == 0 && BlueprintSelection.hasAny(mc.currentWorld)) {
                        BlueprintSelection.clear(mc.currentWorld);
                        DesignModeOverlay.show("Selection cleared", 16733525);
                     }
                  }

                  ci.cancel();
               } else if (hit.hitType == HitType.TILE) {
                  int x = hit.x;
                  int y = hit.y;
                  int z = hit.z;
                  Side side = hit.side;
                  if (emptyHand && HologramStore.get(mc.currentWorld, x, y, z) == null) {
                     if (clickType == 0) {
                        BlueprintSelection.setCornerA(mc.currentWorld, x, y, z);
                        DesignModeOverlay.show("Corner A: " + x + ", " + y + ", " + z, 16755285);
                        designPlayer.swingItem();
                        ci.cancel();
                        return;
                     }

                     if (clickType == 1) {
                        BlueprintSelection.setCornerB(mc.currentWorld, x, y, z);
                        DesignModeOverlay.show("Corner B: " + x + ", " + y + ", " + z, 5614335);
                        designPlayer.swingItem();
                        ci.cancel();
                        return;
                     }
                  }

                  if (clickType != 0) {
                     if (clickType == 1) {
                        ItemStack stack = designPlayer.inventory.getCurrentItem();
                        if (stack == null) {
                           ci.cancel();
                           return;
                        }

                        double yPlaced = hit.location.y - hit.y;
                        double xPlaced;
                        if (side.getAxis() == Axis.X) {
                           xPlaced = hit.location.x - hit.x;
                        } else if (side.getAxis() == Axis.Z) {
                           xPlaced = hit.location.z - hit.z;
                        } else {
                           xPlaced = hit.location.x - hit.x;
                        }

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
                              mc.particleEngine.destroy(x, y, z, removed.blockId, removed.metadata);
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
}
