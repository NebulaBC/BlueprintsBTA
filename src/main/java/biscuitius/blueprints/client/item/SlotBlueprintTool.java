package biscuitius.blueprints.client.item;

import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.slot.Slot;
import org.jetbrains.annotations.Nullable;

public final class SlotBlueprintTool extends Slot {
   public ItemStack item;

   public SlotBlueprintTool(int index, int x, int y, ItemStack item) {
      super(null, index, x, y);
      this.item = item;
   }

   @Nullable
   public ItemStack remove(int i) {
      return this.item == null ? null : this.item.copy();
   }

   public boolean hasItem() {
      return this.item != null;
   }

   public int getMaxStackSize() {
      return 64;
   }

   @Nullable
   public ItemStack getItemStack() {
      return this.item == null ? null : this.item.copy();
   }

   public void onTake(ItemStack itemstack) {
   }

   public void setChanged() {
   }

   public void set(@Nullable ItemStack itemstack) {
   }

   public boolean enableDragAndPickup() {
      return false;
   }

   public boolean allowItemInteraction() {
      return false;
   }
}
