package biscuitius.blueprints.client;

import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.HologramController;
import biscuitius.blueprints.client.hologram.HologramStore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.gui.container.ScreenContainerAbstract;
import net.minecraft.core.InventoryAction;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.lang.I18n;
import net.minecraft.core.player.inventory.container.ContainerInventory;
import net.minecraft.core.world.World;
import org.lwjgl.input.Keyboard;

public final class PrinterMode {
   private static final double RADIUS = 7.5;
   private static final double RADIUS_SQ = 56.25;
   private static volatile boolean active;

   private PrinterMode() {
   }

   public static boolean isActive() {
      return active;
   }

   public static void handleKeyEvent(Minecraft mc) {
      if (Keyboard.getEventKeyState()) {
         if (Keyboard.getEventKey() == Keyboard.KEY_NUMPAD0) {
            boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
            boolean alt = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            if (ctrl && alt) {
               toggle(mc);
            }
         }
      }
   }

   public static void toggle(Minecraft mc) {
      if (!active && DesignModeState.isActive()) {
         showMessage("blueprints.printer_mode.blocked_design", "Printer Mode unavailable in Design Mode", 16733525);
      } else {
         active = !active;
         showMessage(
            active ? "blueprints.printer_mode.enabled" : "blueprints.printer_mode.disabled",
            active ? "Printer Mode enabled" : "Printer Mode disabled",
            active ? 5635925 : 16733525
         );
      }
   }

   public static void tick(Minecraft mc) {
      if (active) {
         if (mc != null && mc.currentWorld != null && mc.thePlayer != null) {
            if (DesignModeState.isActive()) {
               active = false;
            } else if (mc.currentScreen == null || mc.currentScreen instanceof ScreenContainerAbstract) {
               if (HologramStore.hasEntries(mc.currentWorld)) {
                  attemptOnePlacement(mc, mc.thePlayer);
               }
            }
         }
      }
   }

   public static void disable() {
      active = false;
   }

   private static void attemptOnePlacement(Minecraft mc, PlayerLocal player) {
      World world = mc.currentWorld;
      double px = player.x;
      double py = player.y;
      double pz = player.z;
      ContainerInventory inv = player.inventory;
      List<PrinterMode.Candidate> candidates = new ArrayList<>();
      HologramStore.forEach(world, (x, y, z, h) -> {
         double dx = x + 0.5 - px;
         double dy = y + 0.5 - py;
         double dz = z + 0.5 - pz;
         double distSq = dx * dx + dy * dy + dz * dz;
         if (!(distSq > 56.25)) {
            int realId = world.getBlockId(x, y, z);
            int realMeta = world.getBlockMetadata(x, y, z);
            if (realId != h.blockId || realMeta != h.metadata) {
               int slot = findInventorySlotFor(inv, world, h, x, y, z);
               if (slot >= 0) {
                  candidates.add(new PrinterMode.Candidate(x, y, z, slot, distSq));
               }
            }
         }
      });
      if (!candidates.isEmpty()) {
         candidates.sort((a, b) -> Double.compare(a.distSq, b.distSq));

         for (PrinterMode.Candidate c : candidates) {
            if (tryPlaceFromInventorySlot(mc, player, c.slot, c.x, c.y, c.z)) {
               return;
            }
         }
      }
   }

   private static boolean tryPlaceFromInventorySlot(Minecraft mc, PlayerLocal player, int invSlot, int x, int y, int z) {
      ContainerInventory inv = player.inventory;
      int originalSelected = inv.getCurrentItemIndex();
      if (invSlot >= 0 && invSlot <= 8) {
         inv.setCurrentItemIndex(invSlot, true);
         ItemStack held = inv.getCurrentItem();

         boolean var23;
         try {
            var23 = held != null && HologramController.tryFulfill(mc, player, held, x, y, z);
         } finally {
            inv.setCurrentItemIndex(originalSelected, true);
         }

         return var23;
      } else {
         int hotbarTarget = -1;

         for (int i = 0; i < 9; i++) {
            if (inv.mainInventory[i] == null) {
               hotbarTarget = i;
               break;
            }
         }

         if (hotbarTarget < 0) {
            hotbarTarget = originalSelected >= 0 && originalSelected <= 8 ? originalSelected : 0;
         }

         if (mc.playerController == null) {
            return false;
         } else {
            int containerId = player.inventorySlots.containerId;
            int hotbarNumber = hotbarTarget + 1;
            boolean swapped = false;

            boolean var13;
            try {
               mc.playerController.handleInventoryMouseClick(containerId, InventoryAction.HOTBAR_ITEM_SWAP, new int[]{invSlot, hotbarNumber}, player);
               swapped = true;
               inv.setCurrentItemIndex(hotbarTarget, true);
               ItemStack held = inv.getCurrentItem();
               var13 = held != null && HologramController.tryFulfill(mc, player, held, x, y, z);
            } finally {
               if (swapped) {
                  mc.playerController.handleInventoryMouseClick(containerId, InventoryAction.HOTBAR_ITEM_SWAP, new int[]{invSlot, hotbarNumber}, player);
               }

               inv.setCurrentItemIndex(originalSelected, true);
            }

            return var13;
         }
      }
   }

   private static int findInventorySlotFor(ContainerInventory inv, World world, HologramBlock h, int hx, int hy, int hz) {
      Block<?> block = h.blockId > 0 && h.blockId < Blocks.blocksList.length ? Blocks.blocksList[h.blockId] : null;
      if (block == null) {
         return -1;
      } else {
         ItemStack want;
         try {
            ItemStack[] pick = block.getBreakResult(world, EnumDropCause.PICK_BLOCK, hx, hy, hz, h.metadata, null);
            if (pick == null || pick.length == 0 || pick[0] == null) {
               return -1;
            }

            want = pick[0];
         } catch (Throwable var12) {
            return -1;
         }

         int backpackMatch = -1;
         int limit = Math.min(36, inv.mainInventory.length);

         for (int i = 0; i < limit; i++) {
            ItemStack stack = inv.mainInventory[i];
            if (stack != null && stack.itemID == want.itemID && stack.getMetadata() == want.getMetadata()) {
               if (i <= 8) {
                  return i;
               }

               if (backpackMatch < 0) {
                  backpackMatch = i;
               }
            }
         }

         return backpackMatch;
      }
   }

   private static void showMessage(String key, String fallback, int colour) {
      I18n i18n = I18n.getInstance();
      String message = i18n != null ? i18n.translateKey(key) : fallback;
      if (message == null || message.equals(key)) {
         message = fallback;
      }

      DesignModeOverlay.show(message, colour);
   }

   private static final class Candidate {
      final int x;
      final int y;
      final int z;
      final int slot;
      final double distSq;

      Candidate(int x, int y, int z, int slot, double distSq) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.slot = slot;
         this.distSq = distSq;
      }
   }
}
