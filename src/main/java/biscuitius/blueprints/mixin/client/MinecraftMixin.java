package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.BlueprintsCacheManager;
import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.GhostBlockState;
import biscuitius.blueprints.client.ScreenDesignTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.input.InputDevice;
import net.minecraft.client.input.PlayerInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.SoundEngine;
import net.minecraft.client.world.WorldClient;
import net.minecraft.core.InventoryAction;
import net.minecraft.core.block.Block;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.gamemode.Gamemode;
import net.minecraft.core.player.inventory.container.ContainerInventory;
import net.minecraft.core.player.inventory.menu.MenuInventoryCreative;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.HitResult.HitType;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
   @Inject(method = "runTick", at = @At("HEAD"))
   private void blueprints$syncDesignModePlayer(CallbackInfo ci) {
      DesignModeState.syncPlayer((Minecraft)(Object)this);
   }

   @Inject(method = "runTick", at = @At("RETURN"))
   private void blueprints$tickDesignModePlayer(CallbackInfo ci) {
      DesignModeState.tickDesignPlayer((Minecraft)(Object)this);
   }

   @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/PlayerInput;keyEvent(IZ)V"))
   private void blueprints$routeKeyEventsToDesignPlayer(PlayerInput input, int keyCode, boolean pressed) {
      Minecraft minecraft = (Minecraft)(Object)this;
      PlayerLocal controlPlayer = DesignModeState.getControlPlayer(minecraft);
      if (DesignModeState.isActive() && controlPlayer != null && controlPlayer.input != null) {
         controlPlayer.input.keyEvent(keyCode, pressed);
      } else {
         input.keyEvent(keyCode, pressed);
      }
   }

   @Redirect(
      method = "runTick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/core/player/inventory/container/ContainerInventory;changeCurrentItem(I)V")
   )
   private void blueprints$routeScrollToDesignPlayer(ContainerInventory inventory, int scrollDelta) {
      Minecraft minecraft = (Minecraft)(Object)this;
      PlayerLocal controlPlayer = DesignModeState.getControlPlayer(minecraft);
      if (DesignModeState.isActive() && controlPlayer != null) {
         controlPlayer.inventory.changeCurrentItem(scrollDelta);
      } else {
         inventory.changeCurrentItem(scrollDelta);
      }
   }

   @Redirect(
      method = {"clickMouse", "mineBlocks", "clickMiddleMouseButton"},
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeMouseActionsToDesignPlayer(Minecraft minecraft) {
      return DesignModeState.isActive() ? DesignModeState.getControlPlayer(minecraft) : minecraft.thePlayer;
   }

   @Redirect(
      method = "getInventoryScreen",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeInventoryScreenPlayer(Minecraft minecraft) {
      return DesignModeState.getControlPlayer(minecraft);
   }

   @Redirect(
      method = "checkBoundInputs",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeBoundInputPlayer(Minecraft minecraft) {
      return DesignModeState.isActive() ? DesignModeState.getControlPlayer(minecraft) : minecraft.thePlayer;
   }

   @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/player/PlayerLocal;setNoclip(Z)V"))
   private void blueprints$routeSetNoclip(PlayerLocal player, boolean noclip) {
      if (DesignModeState.isActive()) {
         PlayerLocal dp = DesignModeState.getDesignPlayer();
         if (dp != null) {
            dp.setNoclip(noclip);
            return;
         }
      }

      player.setNoclip(noclip);
   }

   @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/player/PlayerLocal;setFlySpeed(F)V"))
   private void blueprints$routeSetFlySpeed(PlayerLocal player, float speed) {
      if (DesignModeState.isActive()) {
         PlayerLocal dp = DesignModeState.getDesignPlayer();
         if (dp != null) {
            dp.setFlySpeed(speed);
            return;
         }
      }

      player.setFlySpeed(speed);
   }

   @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/player/PlayerLocal;setFlightSmoothness(F)V"))
   private void blueprints$routeSetFlightSmoothness(PlayerLocal player, float smoothness) {
      if (DesignModeState.isActive()) {
         PlayerLocal dp = DesignModeState.getDesignPlayer();
         if (dp != null) {
            dp.setFlightSmoothness(smoothness);
            return;
         }
      }

      player.setFlightSmoothness(smoothness);
   }

   @Redirect(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/player/PlayerLocal;noPhysics:Z", opcode = 180))
   private boolean blueprints$routeNoPhysicsRead(PlayerLocal player) {
      if (DesignModeState.isActive()) {
         PlayerLocal dp = DesignModeState.getDesignPlayer();
         if (dp != null) {
            return dp.noPhysics;
         }
      }

      return player.noPhysics;
   }

   @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/PlayerInput;keyEvent(IZ)V", shift = Shift.AFTER))
   private void blueprints$handleDesignModeToggle(CallbackInfo ci) {
      Minecraft mc = (Minecraft)(Object)this;
      DesignModeState.handleKeyPress(mc);
      if (DesignModeState.TOOLS_KEY.isPressEvent(InputDevice.keyboard) && Keyboard.getEventKeyState()) {
         if (mc.currentScreen instanceof ScreenDesignTools) {
            mc.displayScreen(null);
         } else if (mc.currentScreen == null) {
            mc.displayScreen(new ScreenDesignTools());
         }
      }
   }

   @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/SoundEngine;updateListener(Lnet/minecraft/core/entity/Mob;F)V"))
   private void blueprints$routeSoundListener(SoundEngine engine, Mob player, float partialTick) {
      Minecraft mc = (Minecraft)(Object)this;
      Mob listener = (Mob)(DesignModeState.isActive() ? DesignModeState.getDesignPlayer() : player);
      engine.updateListener(listener != null ? listener : player, partialTick);
   }

   @Redirect(
      method = "checkBoundInputs",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressEvent(Lnet/minecraft/client/input/InputDevice;)Z", ordinal = 5)
   )
   private boolean blueprints$blockPerspectiveSwitch(KeyBinding key, InputDevice device) {
      return !DesignModeState.isActive() && key.isPressEvent(device);
   }

   @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelClickOnAir(int clickType, boolean attack, boolean repeat, CallbackInfo ci) {
      if (DesignModeState.isActive() && clickType == 1) {
         Minecraft mc = (Minecraft)(Object)this;
         if (mc.objectMouseOver == null) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "clickMiddleMouseButton", at = @At("HEAD"), cancellable = true)
   private void blueprints$pickGhostBlock(CallbackInfo ci) {
      if (!DesignModeState.isActive()) {
         Minecraft mc = (Minecraft)(Object)this;
         if (mc.thePlayer != null && mc.currentWorld != null) {
            HitResult mouseOver = mc.objectMouseOver;
            if (mouseOver != null && mouseOver.hitType == HitType.TILE) {
               int x = mouseOver.x;
               int y = mouseOver.y;
               int z = mouseOver.z;
               if (GhostBlockState.isGhostBlock(mc.currentWorld, x, y, z)) {
                  Block<?> block = mc.currentWorld.getBlock(x, y, z);
                  if (block == null) {
                     ci.cancel();
                  } else {
                     int meta = mc.currentWorld.getBlockMetadata(x, y, z);
                     ItemStack[] result = block.getBreakResult(mc.currentWorld, EnumDropCause.PICK_BLOCK, x, y, z, meta, null);
                     ItemStack selectItem = result != null && result.length > 0 ? result[0] : null;
                     if (selectItem == null) {
                        ci.cancel();
                     } else {
                        int hotbarOffset = mc.thePlayer.inventory.getHotbarOffset();

                        for (int i = 0; i < 9; i++) {
                           ItemStack stack = mc.thePlayer.inventory.getItem(i + hotbarOffset);
                           if (stack != null && stack.itemID == selectItem.itemID && stack.getMetadata() == selectItem.getMetadata()) {
                              mc.thePlayer.setCurrentItem(i + hotbarOffset);
                              ci.cancel();
                              return;
                           }
                        }

                        int slot = mc.thePlayer.inventory.getCurrentItemIndex();
                        int emptyHotbarSlot = -1;

                        for (int ix = 0; ix < 9; ix++) {
                           if (mc.thePlayer.inventory.getItem(ix + hotbarOffset) == null) {
                              emptyHotbarSlot = ix + hotbarOffset;
                              slot = emptyHotbarSlot;
                              break;
                           }
                        }

                        int itemSlot = -1;
                        int stackSize = -1;

                        for (int ixx = 0; ixx < 36; ixx++) {
                           ItemStack stack = mc.thePlayer.inventory.getItem(ixx);
                           if (stack != null
                              && stack.itemID == selectItem.itemID
                              && stack.getMetadata() == selectItem.getMetadata()
                              && (stackSize == -1 || stack.stackSize < stackSize)) {
                              itemSlot = ixx;
                              stackSize = stack.stackSize;
                           }
                        }

                        if (itemSlot != -1) {
                           mc.thePlayer.swapItems(slot, itemSlot);
                           mc.thePlayer.setCurrentItem(slot);
                        } else if (mc.thePlayer.getGamemode() == Gamemode.creative) {
                           if (!(mc.thePlayer.inventorySlots instanceof MenuInventoryCreative)) {
                              ci.cancel();
                              return;
                           }

                           MenuInventoryCreative creativeMenu = (MenuInventoryCreative)mc.thePlayer.inventorySlots;
                           int creativeIndex = -1;

                           for (int cIdx = 0; cIdx < MenuInventoryCreative.creativeItemsCount; cIdx++) {
                              ItemStack cs = (ItemStack)MenuInventoryCreative.creativeItems.get(cIdx);
                              if (cs.itemID == selectItem.itemID && cs.getMetadata() == selectItem.getMetadata()) {
                                 creativeIndex = cIdx;
                                 break;
                              }
                           }

                           if (creativeIndex == -1) {
                              ci.cancel();
                              return;
                           }

                           int savedPage = creativeMenu.page;
                           String savedSearch = creativeMenu.searchText;
                           if (!savedSearch.isEmpty()) {
                              creativeMenu.searchPage("");
                           }

                           int targetPage = creativeIndex / 36;
                           if (creativeMenu.page != targetPage) {
                              creativeMenu.setInventoryStatus(targetPage, "");
                           }

                           int creativeSlotsStart = creativeMenu.slots.size() - 36;
                           int slotInPage = creativeIndex % 36;
                           int creativeSlotIdx = creativeSlotsStart + slotInPage;
                           int targetInvSlot = emptyHotbarSlot != -1 ? emptyHotbarSlot : mc.thePlayer.inventory.getCurrentItemIndex();
                           int targetMenuSlot = targetInvSlot < 9 ? targetInvSlot + 36 : targetInvSlot;
                           mc.playerController
                              .handleInventoryMouseClick(creativeMenu.containerId, InventoryAction.CREATIVE_GRAB, new int[]{creativeSlotIdx, 1}, mc.thePlayer);
                           mc.playerController
                              .handleInventoryMouseClick(creativeMenu.containerId, InventoryAction.CLICK_LEFT, new int[]{targetMenuSlot}, mc.thePlayer);
                           if (mc.thePlayer.inventory.getHeldItemStack() != null) {
                              for (int s = 0; s < 36; s++) {
                                 if (mc.thePlayer.inventory.getItem(s) == null) {
                                    int emptyMenuSlot = s < 9 ? s + 36 : s;
                                    mc.playerController
                                       .handleInventoryMouseClick(creativeMenu.containerId, InventoryAction.CLICK_LEFT, new int[]{emptyMenuSlot}, mc.thePlayer);
                                    break;
                                 }
                              }
                           }

                           if (!savedSearch.isEmpty()) {
                              creativeMenu.searchPage(savedSearch);
                              if (savedPage != 0) {
                                 creativeMenu.setInventoryStatus(savedPage, savedSearch);
                              }
                           } else if (creativeMenu.page != savedPage) {
                              creativeMenu.setInventoryStatus(savedPage, "");
                           }

                           mc.thePlayer.setCurrentItem(targetInvSlot);
                        }

                        ci.cancel();
                     }
                  }
               }
            }
         }
      }
   }

   @Inject(method = "displayScreen", at = @At("HEAD"))
   private void blueprints$clearMovementOnScreenOpen(Screen screen, CallbackInfo ci) {
      if (screen != null && DesignModeState.isActive()) {
         PlayerLocal dp = DesignModeState.getDesignPlayer();
         if (dp != null) {
            if (dp.input != null) {
               dp.input.onGameUnfocused();
            }

            dp.xd = 0.0;
            dp.yd = 0.0;
            dp.zd = 0.0;
         }
      }
   }

   @Inject(method = "changeWorld(Lnet/minecraft/client/world/WorldClient;Ljava/lang/String;Lnet/minecraft/core/entity/player/Player;)V", at = @At("HEAD"))
   private void blueprints$saveOnWorldLeave(WorldClient world, String loadingTitle, Player player, CallbackInfo ci) {
      Minecraft mc = (Minecraft)(Object)this;
      if (mc.currentWorld != null) {
         if (DesignModeState.isActive()) {
            DesignModeState.handleWorldLeave();
         }

         BlueprintsCacheManager.save(mc);
         GhostBlockState.clear();
      }
   }

   @Inject(method = "changeWorld(Lnet/minecraft/client/world/WorldClient;Ljava/lang/String;Lnet/minecraft/core/entity/player/Player;)V", at = @At("RETURN"))
   private void blueprints$loadOnWorldJoin(WorldClient world, String loadingTitle, Player player, CallbackInfo ci) {
      if (world != null) {
         BlueprintsCacheManager.load((Minecraft)(Object)this);
      }
   }
}
