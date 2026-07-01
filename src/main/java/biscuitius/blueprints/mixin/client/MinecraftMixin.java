package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.PrinterMode;
import biscuitius.blueprints.client.ScreenDesignTools;
import biscuitius.blueprints.client.hologram.HologramCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.input.InputDevice;
import net.minecraft.client.input.PlayerInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.SoundEngine;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.player.inventory.container.ContainerInventory;
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
      DesignModeState.syncPlayer(Minecraft.getMinecraft());
      Minecraft mc = Minecraft.getMinecraft();
      HologramCache.tick(mc);
      PrinterMode.tick(mc);
   }

   @Inject(method = "runTick", at = @At("RETURN"))
   private void blueprints$tickDesignModePlayer(CallbackInfo ci) {
      DesignModeState.tickDesignPlayer(Minecraft.getMinecraft());
   }

   @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/PlayerInput;keyEvent(IZ)V"))
   private void blueprints$routeKeyEventsToDesignPlayer(PlayerInput input, int keyCode, boolean pressed) {
      Minecraft minecraft = Minecraft.getMinecraft();
      if (!DesignModeState.isActive() || minecraft.currentScreen == null) {
         PlayerLocal controlPlayer = DesignModeState.getControlPlayer(minecraft);
         if (DesignModeState.isActive() && controlPlayer != null && controlPlayer.input != null) {
            controlPlayer.input.keyEvent(keyCode, pressed);
         } else {
            input.keyEvent(keyCode, pressed);
         }
      }
   }

   @Redirect(
      method = "runTick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/core/player/inventory/container/ContainerInventory;changeCurrentItem(I)V")
   )
   private void blueprints$routeScrollToDesignPlayer(ContainerInventory inventory, int scrollDelta) {
      Minecraft minecraft = Minecraft.getMinecraft();
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
      Minecraft mc = Minecraft.getMinecraft();
      DesignModeState.handleKeyPress(mc);
      PrinterMode.handleKeyEvent(mc);
      if (DesignModeState.MENU_KEY.isPressEvent(InputDevice.keyboard) && Keyboard.getEventKeyState()) {
         if (mc.currentScreen instanceof ScreenDesignTools) {
            mc.displayScreen(null);
         } else if (mc.currentScreen == null) {
            mc.displayScreen(new ScreenDesignTools());
         }
      }
   }

   @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/SoundEngine;updateListener(Lnet/minecraft/core/entity/Mob;F)V"))
   private void blueprints$routeSoundListener(SoundEngine engine, Mob player, float partialTick) {
      Minecraft mc = Minecraft.getMinecraft();
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
}
