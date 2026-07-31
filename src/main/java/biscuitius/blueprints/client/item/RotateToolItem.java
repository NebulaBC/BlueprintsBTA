package biscuitius.blueprints.client.item;

import net.minecraft.core.data.tag.Tag;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.tag.ItemTags;

public final class RotateToolItem {
   private static final String KEY = "rotate_tool";
   public static final String NAMESPACE = "blueprints";
   public static final String ICON = "blueprints:item/rotate";
   private static final String NAMESPACE_ID = "blueprints:rotate_tool";
   private static final int PREFERRED_ID = 30002;
   private static Item instance;

   private RotateToolItem() {
   }

   public static Item get() {
      return instance;
   }

   public static boolean is(ItemStack stack) {
      return stack != null && instance != null && stack.itemID == instance.id;
   }

   public static ItemStack newStack() {
      return instance == null ? null : new ItemStack(instance);
   }

   public static synchronized void register() {
      if (instance == null) {
         int id = findFreeId();
         instance = new Item("rotate_tool", "blueprints:rotate_tool", id).setMaxStackSize(1).withTags(new Tag[]{ItemTags.NOT_IN_CREATIVE_MENU});
      }
   }

   private static int findFreeId() {
      for (int id = 30002; id < Item.itemsList.length; id++) {
         if (Item.itemsList[id] == null) {
            return id;
         }
      }

      for (int id = 30001; id >= 0; id--) {
         if (Item.itemsList[id] == null) {
            return id;
         }
      }

      throw new IllegalStateException("No free item id available for the Rotate Tool item");
   }
}
