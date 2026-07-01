package biscuitius.blueprints.client;

import biscuitius.blueprints.client.hologram.HologramStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ButtonElement;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.gui.ScrolledSelectionList;
import net.minecraft.client.input.InputDevice;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.ItemModelDispatcher;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import org.lwjgl.opengl.GL11;

public final class ScreenBlueprintMaterials extends Screen {
   private static final int ID_DONE = 0;
   private static final int LIST_TOP = 32;
   private static final int FOOTER_AREA = 36;
   private static final int ITEM_HEIGHT = 22;
   private final Screen returnScreen;
   private List<ScreenBlueprintMaterials.Material> materials;
   private ScreenBlueprintMaterials.MaterialList list;

   public ScreenBlueprintMaterials(Screen returnScreen) {
      super(returnScreen);
      this.returnScreen = returnScreen;
   }

   public void init() {
      this.materials = this.computeMaterials();
      this.list = new ScreenBlueprintMaterials.MaterialList();
      int btnWidth = 100;
      this.buttons.add(new ButtonElement(0, this.width / 2 - btnWidth / 2, this.height - 28, btnWidth, 20, "Done"));
   }

   protected void buttonClicked(ButtonElement button) {
      if (button.id == 0) {
         this.mc.displayScreen(this.returnScreen);
      }
   }

   public void render(int mx, int my, float partialTick) {
      if (this.list != null) {
         this.list.render(mx, my, partialTick);
      }

      this.drawStringCentered(this.font, "Blueprint Materials", this.width / 2, 12, 16777215);
      super.render(mx, my, partialTick);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void keyPressed(char eventCharacter, int eventKey, int mx, int my) {
      if (DesignModeState.MENU_KEY.isPressEvent(InputDevice.keyboard)) {
         this.mc.displayScreen(null);
      } else {
         super.keyPressed(eventCharacter, eventKey, mx, my);
      }
   }

   private List<ScreenBlueprintMaterials.Material> computeMaterials() {
      Map<Long, ScreenBlueprintMaterials.Material> byKey = new HashMap<>();
      HologramStore.forEach(this.mc.currentWorld, (x, y, z, h) -> {
         if (h != null) {
            if (h.blockId > 0 && h.blockId < Blocks.blocksList.length) {
               Block<?> block = Blocks.blocksList[h.blockId];
               if (block != null) {
                  ItemStack pick = pickStackFor(block, x, y, z, h.metadata);
                  if (pick != null) {
                     int itemId = pick.itemID;
                     int meta = pick.getMetadata();
                     long key = (long)itemId << 32 | meta & 4294967295L;
                     ScreenBlueprintMaterials.Material m = byKey.get(key);
                     if (m == null) {
                        ItemStack display = pick.copy();
                        display.stackSize = 1;
                        byKey.put(key, new ScreenBlueprintMaterials.Material(display, 1));
                     } else {
                        m.count++;
                     }
                  }
               }
            }
         }
      });
      List<ScreenBlueprintMaterials.Material> list = new ArrayList<>(byKey.values());
      list.sort(
         Comparator.<ScreenBlueprintMaterials.Material>comparingInt(m -> -m.count).thenComparing(m -> m.stack.getDisplayName(), String.CASE_INSENSITIVE_ORDER)
      );
      return list;
   }

   private static ItemStack pickStackFor(Block<?> block, int x, int y, int z, int metadata) {
      ItemStack[] drops;
      try {
         drops = block.getBreakResult(this_world(), EnumDropCause.PICK_BLOCK, x, y, z, metadata, null);
      } catch (Throwable var8) {
         drops = null;
      }

      if (drops != null && drops.length > 0 && drops[0] != null) {
         return drops[0];
      } else {
         try {
            return new ItemStack(block, 1, metadata);
         } catch (Throwable var7) {
            return null;
         }
      }
   }

   private static World this_world() {
      Minecraft mc = Minecraft.getMinecraft();
      return mc == null ? null : mc.currentWorld;
   }

   private static String formatCount(int n) {
      if (n < 1000) {
         return Integer.toString(n);
      } else {
         StringBuilder sb = new StringBuilder();
         String s = Integer.toString(n);
         int firstGroup = s.length() % 3;
         if (firstGroup == 0) {
            firstGroup = 3;
         }

         sb.append(s, 0, firstGroup);

         for (int i = firstGroup; i < s.length(); i += 3) {
            sb.append(',').append(s, i, i + 3);
         }

         return sb.toString();
      }
   }

   private static final class Material {
      final ItemStack stack;
      int count;

      Material(ItemStack stack, int count) {
         this.stack = stack;
         this.count = count;
      }
   }

   private final class MaterialList extends ScrolledSelectionList {
      MaterialList() {
         super(
            ScreenBlueprintMaterials.this.mc,
            ScreenBlueprintMaterials.this.width,
            ScreenBlueprintMaterials.this.height,
            32,
            ScreenBlueprintMaterials.this.height - 36,
            22
         );
      }

      protected int getItemCount() {
         return ScreenBlueprintMaterials.this.materials.size();
      }

      protected void selectItem(int itemIndex, boolean doubleClicked) {
      }

      protected boolean isSelectedItem(int itemIndex) {
         return false;
      }

      protected void renderHoleBackground() {
         ScreenBlueprintMaterials.this.renderBackground();
      }

      protected void renderItem(int index, int x, int y, int height, Tessellator tessellator) {
         if (index >= 0 && index < ScreenBlueprintMaterials.this.materials.size()) {
            ScreenBlueprintMaterials.Material m = ScreenBlueprintMaterials.this.materials.get(index);
            int iconX = x + 2;
            int iconY = y + 2;

            try {
               ItemModel model = (ItemModel)ItemModelDispatcher.getInstance().getDispatch(m.stack.getItem());
               if (model != null) {
                  GL11.glPushAttrib(286785);
                  GL11.glPushMatrix();
                  GL11.glEnable(2929);
                  model.renderItemIntoGui(
                     Tessellator.instance, ScreenBlueprintMaterials.this.font, ScreenBlueprintMaterials.this.mc.textureManager, m.stack, iconX, iconY, 1.0F
                  );
                  GL11.glPopMatrix();
                  GL11.glPopAttrib();
                  GL11.glDisable(2896);
                  GL11.glDisable(2929);
                  GL11.glEnable(3553);
                  GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
               }
            } catch (Throwable var11) {
            }

            String name = this.safeName(m.stack);
            String line = ScreenBlueprintMaterials.formatCount(m.count) + "x " + name;
            ScreenBlueprintMaterials.this.drawString(ScreenBlueprintMaterials.this.font, line, iconX + 20, y + 6, 16777215);
         }
      }

      private String safeName(ItemStack stack) {
         try {
            String n = stack.getDisplayName();
            return n == null ? "Unknown" : n;
         } catch (Throwable var3) {
            return "Unknown";
         }
      }
   }
}
