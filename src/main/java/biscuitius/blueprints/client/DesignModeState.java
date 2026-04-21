package biscuitius.blueprints.client;

import biscuitius.blueprints.mixin.client.MinecraftAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.gui.ScreenSignEditor;
import net.minecraft.client.gui.container.ScreenContainerAbstract;
import net.minecraft.client.input.InputDevice;
import net.minecraft.client.input.PlayerInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.camera.EntityCameraFirstPerson;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.block.ItemBlock;
import net.minecraft.core.lang.I18n;
import net.minecraft.core.player.gamemode.Gamemode;
import net.minecraft.core.player.inventory.container.ContainerInventory;
import org.lwjgl.input.Keyboard;

public final class DesignModeState {
   public static final KeyBinding TOGGLE_KEY = new KeyBinding("key.blueprints.design_mode").setDefault(InputDevice.keyboard, Keyboard.KEY_V);
   public static final KeyBinding TOOLS_KEY = new KeyBinding("key.blueprints.design_tools").setDefault(InputDevice.keyboard, Keyboard.KEY_G);
   public static final KeyBinding SHUFFLE_KEY = new KeyBinding("key.blueprints.shuffle").setDefault(InputDevice.keyboard, Keyboard.KEY_H);
   public static final KeyBinding INTERACTION_KEY = new KeyBinding("key.blueprints.interaction_mode").setDefault(InputDevice.keyboard, Keyboard.KEY_X);
   private static volatile boolean active;
   private static boolean shuffleEnabled;
   private static boolean passthroughMode;
   private static final Random shuffleRandom = new Random();
   private static PlayerLocal realPlayer;
   private static PlayerLocal designPlayer;
   private static boolean designPlayerNoclipOverride;
   private static ItemStack[] pendingMainInventory;
   private static ItemStack[] pendingArmorInventory;
   private static int pendingCurrentItemIndex = -1;
   private static int pendingHotbarOffset;

   public static boolean isDesignPlayerNoclipOverride() {
      return designPlayerNoclipOverride;
   }

   public static void setDesignPlayerNoclipOverride(boolean v) {
      designPlayerNoclipOverride = v;
   }

   private DesignModeState() {
   }

   public static void bootstrap() {
   }

   public static boolean isActive() {
      return active;
   }

   public static PlayerLocal getDesignPlayer() {
      return designPlayer;
   }

   public static void setPendingInventory(ItemStack[] main, ItemStack[] armor, int currentItemIndex, int hotbarOffset) {
      pendingMainInventory = main;
      pendingArmorInventory = armor;
      pendingCurrentItemIndex = currentItemIndex;
      pendingHotbarOffset = hotbarOffset;
   }

   public static PlayerLocal getControlPlayer(Minecraft minecraft) {
      if (active && designPlayer != null) {
         return designPlayer;
      } else {
         return minecraft != null ? minecraft.thePlayer : null;
      }
   }

   public static boolean isShuffleEnabled() {
      return shuffleEnabled;
   }

   public static boolean isPassthroughMode() {
      return passthroughMode;
   }

   public static void setPassthroughMode(boolean value) {
      passthroughMode = value;
   }

   public static void toggleInteractionMode(Minecraft minecraft) {
      passthroughMode = !passthroughMode;
      BlueprintsConfig.save();
      showInteractionModeMessage(minecraft);
   }

   public static void toggleShuffle(Minecraft minecraft) {
      shuffleEnabled = !shuffleEnabled;
      showShuffleMessage(minecraft);
   }

   public static ItemStack shuffleAndGetItem(ContainerInventory inventory) {
      if (active && shuffleEnabled && designPlayer != null) {
         int hotbarOffset = inventory.getHotbarOffset();
         List<Integer> candidates = new ArrayList<>();

         for (int i = 0; i < 9; i++) {
            int slot = i + hotbarOffset;
            ItemStack stack = inventory.mainInventory[slot];
            if (stack != null && stack.getItem() instanceof ItemBlock) {
               candidates.add(slot);
            }
         }

         if (candidates.isEmpty()) {
            return inventory.getCurrentItem();
         } else {
            int chosen = candidates.get(shuffleRandom.nextInt(candidates.size()));
            inventory.setCurrentItemIndex(chosen, true);
            return inventory.mainInventory[chosen];
         }
      } else {
         return inventory.getCurrentItem();
      }
   }

   public static void syncPlayer(Minecraft minecraft) {
      if (minecraft != null && minecraft.currentWorld != null) {
         if (realPlayer != null && minecraft.thePlayer != null && minecraft.thePlayer != realPlayer) {
            realPlayer = minecraft.thePlayer;
         }

         GhostBlockState.tickPendingApplication(minecraft.currentWorld);
         GhostBlockState.tickFulfillments();
         BlueprintsCacheManager.tickAutoSave(minecraft);
         if (active) {
            if (realPlayer != null) {
               if (realPlayer.getHealth() <= 0) {
                  exitDesignMode(minecraft);
               } else {
                  if (designPlayer == null || designPlayer.world != minecraft.currentWorld) {
                     GhostBlockState.clear();
                     designPlayer = createDesignPlayer(minecraft, realPlayer, designPlayer);
                  }

                  attachDesignCamera(minecraft);
               }
            }
         }
      }
   }

   public static void tickDesignPlayer(Minecraft minecraft) {
      if (active && minecraft != null && designPlayer != null) {
         designPlayer.syncPlacementMode();
         designPlayer.tick();
      }
   }

   public static void handleKeyPress(Minecraft minecraft) {
      if (TOGGLE_KEY.isPressEvent(InputDevice.keyboard) && Keyboard.getEventKeyState()) {
         if (active) {
            exitDesignMode(minecraft);
         } else {
            enterDesignMode(minecraft);
         }
      } else {
         if (active && SHUFFLE_KEY.isPressEvent(InputDevice.keyboard) && Keyboard.getEventKeyState()) {
            toggleShuffle(minecraft);
         }

         if (INTERACTION_KEY.isPressEvent(InputDevice.keyboard) && Keyboard.getEventKeyState()) {
            toggleInteractionMode(minecraft);
         }
      }
   }

   public static void handleDamageExit(Minecraft minecraft) {
      if (active) {
         exitDesignMode(minecraft);
      }
   }

   private static void enterDesignMode(Minecraft minecraft) {
      if (minecraft != null && minecraft.currentWorld != null && minecraft.thePlayer != null) {
         if (realPlayer == null) {
            realPlayer = minecraft.thePlayer;
         }

         if (designPlayer != null && designPlayer.world == minecraft.currentWorld) {
            copyPlayerState(realPlayer, designPlayer);
            designPlayer.setGamemode(Gamemode.creative);
            designPlayer.setNoclip(true);
            designPlayer.syncPlacementMode();
            designPlayer.removed = false;
         } else {
            designPlayer = createDesignPlayer(minecraft, realPlayer, designPlayer);
         }

         active = true;
         GhostBlockState.setHidden(false, minecraft.currentWorld);
         clearMovement(realPlayer);
         clearMovement(designPlayer);
         minecraft.gameSettings.thirdPersonView.value = 0;
         GhostBlockState.swapGhostChunkData(minecraft.currentWorld, true);
         GhostBlockState.markAllDirty(minecraft.currentWorld);
         attachDesignCamera(minecraft);
         showStatusMessage(minecraft, true);
      }
   }

   private static void exitDesignMode(Minecraft minecraft) {
      active = false;
      if (minecraft != null && minecraft.currentWorld != null) {
         GhostBlockState.swapGhostChunkData(minecraft.currentWorld, false);
         GhostBlockState.markAllDirty(minecraft.currentWorld);
      }

      if (realPlayer != null) {
         realPlayer.removed = false;
         if (!realPlayer.getGamemode().canPlayerFly()) {
            realPlayer.setNoclip(false);
         }

         if (minecraft != null) {
            ((MinecraftAccessor)minecraft).setToggleFlyPressed(false);
         }

         if (minecraft != null) {
            attachRealPlayerCamera(minecraft);
            if (minecraft.currentScreen instanceof ScreenContainerAbstract
               || minecraft.currentScreen instanceof ScreenDesignTools
               || minecraft.currentScreen instanceof ScreenSignEditor) {
               minecraft.displayScreen(null);
            }
         }
      }

      if (designPlayer != null) {
         clearMovement(designPlayer);
         designPlayer.removed = true;
      }

      BlueprintsCacheManager.markDirty();
      showStatusMessage(minecraft, false);
   }

   public static void handleWorldLeave() {
      if (active) {
         active = false;
         if (designPlayer != null) {
            designPlayer.removed = true;
         }

         realPlayer = null;
      }
   }

   private static void clearMovement(PlayerLocal player) {
      if (player != null) {
         if (player.input != null) {
            player.input.onGameUnfocused();
         }

         player.xd = 0.0;
         player.yd = 0.0;
         player.zd = 0.0;
      }
   }

   private static void attachDesignCamera(Minecraft minecraft) {
      if (minecraft != null && designPlayer != null) {
         if (designPlayer.input == null) {
            designPlayer.input = new PlayerInput(minecraft);
         }

         minecraft.activeCamera = new EntityCameraFirstPerson(minecraft, designPlayer);
      }
   }

   private static void attachRealPlayerCamera(Minecraft minecraft) {
      if (minecraft != null && realPlayer != null) {
         realPlayer.input = new PlayerInput(minecraft);
         minecraft.activeCamera = new EntityCameraFirstPerson(minecraft, realPlayer);
      }
   }

   private static PlayerLocal createDesignPlayer(Minecraft minecraft, PlayerLocal source, PlayerLocal preservedState) {
      PlayerLocal ghost = new PlayerLocal(minecraft, minecraft.currentWorld, minecraft.session, source.dimension);
      copyPlayerState(source, ghost);
      if (pendingMainInventory != null) {
         copyInventory(pendingMainInventory, ghost.inventory.mainInventory);
         if (pendingArmorInventory != null) {
            copyInventory(pendingArmorInventory, ghost.inventory.armorInventory);
         }

         if (pendingCurrentItemIndex >= 0) {
            ghost.inventory.setCurrentItemIndex(pendingCurrentItemIndex, true);
            ghost.inventory.setHotbarOffset(pendingHotbarOffset, true);
         }

         pendingMainInventory = null;
         pendingArmorInventory = null;
         pendingCurrentItemIndex = -1;
      } else if (preservedState != null) {
         copyInventory(preservedState.inventory.mainInventory, ghost.inventory.mainInventory);
         copyInventory(preservedState.inventory.armorInventory, ghost.inventory.armorInventory);
         ghost.inventory.setCurrentItemIndex(preservedState.inventory.getCurrentItemIndex(), true);
         ghost.inventory.setHotbarOffset(preservedState.inventory.getHotbarOffset(), true);
         ghost.score = preservedState.score;
      }

      ghost.setGamemode(Gamemode.creative);
      ghost.setNoclip(true);
      ghost.syncPlacementMode();
      return ghost;
   }

   private static void copyPlayerState(Player source, PlayerLocal target) {
      target.setPos(source.x, source.y, source.z);
      target.setRot(source.yRot, source.xRot);
      target.xd = source.xd;
      target.yd = source.yd;
      target.zd = source.zd;
      target.onGround = source.onGround;
      target.dimension = source.dimension;
      target.removed = false;
      target.score = source.score;
   }

   private static void copyInventory(ItemStack[] source, ItemStack[] target) {
      for (int i = 0; i < source.length && i < target.length; i++) {
         target[i] = source[i] == null ? null : source[i].copy();
      }
   }

   private static void showStatusMessage(Minecraft minecraft, boolean enabled) {
      if (minecraft != null) {
         I18n i18n = I18n.getInstance();
         String message = i18n != null
            ? i18n.translateKey(enabled ? "blueprints.design_mode.enabled" : "blueprints.design_mode.disabled")
            : (enabled ? "Design Mode enabled" : "Design Mode disabled");
         DesignModeOverlay.show(message, enabled ? 5635925 : 16733525);
      }
   }

   private static void showShuffleMessage(Minecraft minecraft) {
      if (minecraft != null) {
         I18n i18n = I18n.getInstance();
         String message = i18n != null
            ? i18n.translateKey(shuffleEnabled ? "blueprints.shuffle.enabled" : "blueprints.shuffle.disabled")
            : (shuffleEnabled ? "Shuffle enabled" : "Shuffle disabled");
         DesignModeOverlay.show(message, shuffleEnabled ? 5635925 : 16733525);
      }
   }

   private static void showInteractionModeMessage(Minecraft minecraft) {
      if (minecraft != null) {
         I18n i18n = I18n.getInstance();
         String key = passthroughMode ? "blueprints.interaction_mode.passthrough" : "blueprints.interaction_mode.fulfill";
         String message = i18n != null ? i18n.translateKey(key) : (passthroughMode ? "Interaction Mode: Passthrough" : "Interaction Mode: Fulfill");
         DesignModeOverlay.show(message, passthroughMode ? 16762880 : 5635925);
      }
   }
}
