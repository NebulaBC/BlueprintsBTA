package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.menu.MenuInventoryCreative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MenuInventoryCreative.class)
public class MenuInventoryCreativeMixin {
   @Shadow
   protected List searchedItems;
   @Unique
   private static final Set<String> ALLOWED_ITEM_PREFIXES = new HashSet<>();

   @Unique
   private static boolean blueprints$isAllowedInDesignMode(ItemStack stack) {
      Item item = Item.itemsList[stack.itemID];
      if (item == null) {
         return false;
      } else {
         String nsId = item.namespaceID.toString();
         if (!nsId.startsWith("minecraft:item/")) {
            return true;
         } else {
            for (String prefix : ALLOWED_ITEM_PREFIXES) {
               if (nsId.startsWith(prefix)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   @Inject(method = "searchPage", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/player/inventory/menu/MenuInventoryCreative;updatePage()V"))
   private void blueprints$filterDesignModeItems(String search, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         Iterator<ItemStack> it = this.searchedItems.iterator();

         while (it.hasNext()) {
            if (!blueprints$isAllowedInDesignMode(it.next())) {
               it.remove();
            }
         }
      }
   }

   static {
      ALLOWED_ITEM_PREFIXES.add("minecraft:item/door_");
      ALLOWED_ITEM_PREFIXES.add("minecraft:item/sign");
      ALLOWED_ITEM_PREFIXES.add("minecraft:item/lantern_firefly_");
      ALLOWED_ITEM_PREFIXES.add("minecraft:item/jar");
      ALLOWED_ITEM_PREFIXES.add("minecraft:item/seat");
      ALLOWED_ITEM_PREFIXES.add("minecraft:item/flag");
      ALLOWED_ITEM_PREFIXES.add("minecraft:item/bed");
      ALLOWED_ITEM_PREFIXES.add("minecraft:item/repeater");
      ALLOWED_ITEM_PREFIXES.add("minecraft:item/food_pumpkin_pie");
      ALLOWED_ITEM_PREFIXES.add("minecraft:item/food_cake");
      ALLOWED_ITEM_PREFIXES.add("minecraft:item/rope");
      ALLOWED_ITEM_PREFIXES.add("minecraft:item/basket");
      ALLOWED_ITEM_PREFIXES.add("minecraft:item/dust_redstone");
   }
}
