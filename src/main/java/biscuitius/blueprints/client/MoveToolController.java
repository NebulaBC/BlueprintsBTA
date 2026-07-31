package biscuitius.blueprints.client;

import biscuitius.blueprints.client.hologram.HologramStore;
import biscuitius.blueprints.client.item.MoveToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.option.GameSettings;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.lang.I18n;
import net.minecraft.core.world.World;

public final class MoveToolController {
   private static final int REPEAT_DELAY_TICKS = 4;
   private static int cooldown;
   private static boolean armed;

   private MoveToolController() {
   }

   public static boolean isHeld(Minecraft mc) {
      if (mc != null && DesignModeState.isActive()) {
         PlayerLocal control = DesignModeState.getControlPlayer(mc);
         if (control == null) {
            return false;
         }

         ItemStack held = control.inventory.getCurrentItem();
         return MoveToolItem.is(held);
      } else {
         return false;
      }
   }

   public static boolean isArmed() {
      return armed;
   }

   public static boolean isEngaged(Minecraft mc) {
      return armed && isHeld(mc);
   }

   public static void toggleArmed(Minecraft mc) {
      armed = !armed;
      cooldown = 0;
      I18n i18n = I18n.getInstance();
      String msg;
      if (i18n != null) {
         msg = i18n.translateKey(armed ? "blueprints.move_tool.enabled" : "blueprints.move_tool.disabled");
      } else {
         msg = armed ? "Move Tool: ON" : "Move Tool: OFF";
      }

      DesignModeOverlay.show(msg, armed ? 5635925 : 16733525);
   }

   public static PlayerLocal getControlPlayer(Minecraft mc) {
      return DesignModeState.getControlPlayer(mc);
   }

   public static void reset() {
      cooldown = 0;
      armed = false;
   }

   public static void tickHeld(Minecraft mc) {
      if (mc != null && mc.currentWorld != null) {
         int dx = 0;
         int dy = 0;
         int dz = 0;
         int[] fwd = forwardCardinal(mc);
         int fx = fwd[0];
         int fz = fwd[1];
         if (GameSettings.KEY_FORWARD.isPressed()) {
            dx += fx;
            dz += fz;
         }

         if (GameSettings.KEY_BACK.isPressed()) {
            dx -= fx;
            dz -= fz;
         }

         if (GameSettings.KEY_RIGHT.isPressed()) {
            dx -= fz;
            dz += fx;
         }

         if (GameSettings.KEY_LEFT.isPressed()) {
            dx += fz;
            dz -= fx;
         }

         if (GameSettings.KEY_JUMP.isPressed()) {
            dy++;
         }

         if (GameSettings.KEY_SNEAK.isPressed()) {
            dy--;
         }

         if (dx == 0 && dy == 0 && dz == 0) {
            cooldown = 0;
         } else if (cooldown > 0) {
            cooldown--;
         } else {
            World world = mc.currentWorld;
            HologramStore.shiftAll(world, dx, dy, dz);
            cooldown = 4;
         }
      } else {
         cooldown = 0;
      }
   }

   public static int[] forwardCardinal(Minecraft mc) {
      PlayerLocal player = DesignModeState.getControlPlayer(mc);
      float yaw = player != null ? player.yRot : 0.0F;
      yaw = (yaw % 360.0F + 360.0F) % 360.0F;
      int facing = (int)Math.floor((yaw + 45.0) / 90.0) & 3;
      switch (facing) {
         case 0:
            return new int[]{0, 1};
         case 1:
            return new int[]{-1, 0};
         case 2:
            return new int[]{0, -1};
         default:
            return new int[]{1, 0};
      }
   }
}
