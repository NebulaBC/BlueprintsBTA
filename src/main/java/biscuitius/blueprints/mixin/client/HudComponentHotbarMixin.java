package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.BlueprintSelection;
import biscuitius.blueprints.client.DesignModeOverlay;
import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.item.BlueprintItem;
import biscuitius.blueprints.client.item.FillToolItem;
import biscuitius.blueprints.client.item.LineToolItem;
import biscuitius.blueprints.client.item.OvalToolItem;
import biscuitius.blueprints.client.item.RectangleToolItem;
import biscuitius.blueprints.client.tool.ShapeToolState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.gui.hud.HudIngame;
import net.minecraft.client.gui.hud.component.HudComponentHotbar;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.ItemModelDispatcher;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.LightIndexHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HudComponentHotbar.class)
public abstract class HudComponentHotbarMixin {
   @Unique
   private PlayerLocal blueprints$previousHudPlayer;

   @Inject(method = "render", at = @At("HEAD"))
   private void blueprints$swapHotbarPlayer(HudIngame hud, int xSizeScreen, int ySizeScreen, float partialTick, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         Minecraft mc = Minecraft.getMinecraft();
         PlayerLocal controlPlayer = DesignModeState.getControlPlayer(mc);
         if (controlPlayer != null) {
            this.blueprints$previousHudPlayer = mc.thePlayer;
            mc.thePlayer = controlPlayer;
            blueprints$clearToolSelections(mc);
         }
      }
   }

   @Inject(method = "render", at = @At("RETURN"))
   private void blueprints$restoreHotbarPlayer(HudIngame hud, int xSizeScreen, int ySizeScreen, float partialTick, CallbackInfo ci) {
      Minecraft mc = Minecraft.getMinecraft();
      if (this.blueprints$previousHudPlayer != null) {
         mc.thePlayer = this.blueprints$previousHudPlayer;
         this.blueprints$previousHudPlayer = null;
      }

      DesignModeOverlay.renderIfActive(mc, xSizeScreen / 2, ySizeScreen - 52);
      this.blueprints$renderInkPreview(mc, hud, xSizeScreen, ySizeScreen);
   }

   @Unique
   private void blueprints$renderInkPreview(Minecraft mc, HudIngame hud, int xSizeScreen, int ySizeScreen) {
      if (DesignModeState.isActive()) {
         PlayerLocal designPlayer = DesignModeState.getDesignPlayer();
         if (designPlayer != null && mc.currentWorld != null) {
            ItemStack held = designPlayer.inventory.getCurrentItem();
            if (held != null) {
               boolean shapeTool = LineToolItem.is(held) || FillToolItem.is(held) || RectangleToolItem.is(held) || OvalToolItem.is(held);
               if (shapeTool) {
                  int iconX = xSizeScreen / 2 + 91 + 6;
                  int iconY = ySizeScreen - 21;
                  hud.drawGuiIcon(iconX - 4, iconY - 4, 24, 24, TextureRegistry.getTexture("blueprints:gui/hud/ink_preview"));
                  if (ShapeToolState.hasInk(mc.currentWorld)) {
                     int inkId = ShapeToolState.getInkBlockId(mc.currentWorld);
                     int inkMeta = ShapeToolState.getInkMeta(mc.currentWorld);
                     if (inkId > 0 && inkId < Blocks.blocksList.length) {
                        Block<?> block = Blocks.blocksList[inkId];
                        if (block != null) {
                           try {
                              ItemStack inkStack = new ItemStack(block, 1, inkMeta);
                              ItemModel model = (ItemModel)ItemModelDispatcher.getInstance().getDispatch(inkStack.getItem());
                              if (model == null) {
                                 return;
                              }

                              model.renderGui(GLRenderer.getTessellator(), null, inkStack, iconX, iconY, LightIndexHelper.lightIndex2i(15, 15), 1.0F);
                           } catch (Throwable var15) {
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Redirect(
      method = {"render", "renderInventorySlot"},
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeHotbarPlayer(Minecraft minecraft) {
      return DesignModeState.getControlPlayer(minecraft);
   }

   @Unique
   private static void blueprints$clearToolSelections(Minecraft mc) {
      if (mc != null && mc.currentWorld != null) {
         PlayerLocal player = DesignModeState.getControlPlayer(mc);
         if (player != null) {
            ItemStack held = player.getCurrentEquippedItem();
            if (!BlueprintItem.is(held)) {
               BlueprintSelection.clear(mc.currentWorld);
            }

            if (!LineToolItem.is(held) && !FillToolItem.is(held) && !RectangleToolItem.is(held) && !OvalToolItem.is(held)) {
               ShapeToolState.clearPoints(mc.currentWorld);
            }
         }
      }
   }

   @ModifyArg(
      method = "drawOrientedBackgrounds",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/texture/stitcher/TextureRegistry;getTexture(Ljava/lang/String;)Lnet/minecraft/client/render/texture/stitcher/IconCoordinate;"
      ),
      index = 0
   )
   private String blueprints$swapHotbarTextureKey(String key) {
      if (!DesignModeState.isActive()) {
         return key;
      } else if ("minecraft:gui/hud/hotbar".equals(key)) {
         return "blueprints:gui/hud/design_hotbar";
      } else if (key.startsWith("minecraft:gui/hud/hotbar_selector")) {
         return "blueprints:gui/hud/design_" + key.substring("minecraft:gui/hud/".length());
      } else {
         return !"minecraft:gui/hud/hotbar_selection".equals(key) && !"minecraft:gui/hud/hotbar_selection_locked".equals(key)
            ? key
            : "blueprints:gui/hud/design_hotbar_selection";
      }
   }
}
