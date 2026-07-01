package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.hologram.HologramController;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.menu.MenuInventoryCreative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MenuInventoryCreative.class)
public abstract class MenuInventoryCreativeMixin {
   @Shadow
   protected List searchedItems;

   @Shadow
   protected abstract void updatePage();

   @Inject(method = "searchPage", at = @At("RETURN"))
   private void blueprints$filterDesignModeItems(String search, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         if (this.searchedItems != null && !this.searchedItems.isEmpty()) {
            int before = this.searchedItems.size();
            Iterator<ItemStack> it = this.searchedItems.iterator();

            while (it.hasNext()) {
               ItemStack stack = it.next();
               Item item = stack == null ? null : Item.itemsList[stack.itemID];
               if (item == null || !HologramController.isPlacementItem(item)) {
                  it.remove();
               }
            }

            if (this.searchedItems.size() != before) {
               this.updatePage();
            }
         }
      }
   }
}
