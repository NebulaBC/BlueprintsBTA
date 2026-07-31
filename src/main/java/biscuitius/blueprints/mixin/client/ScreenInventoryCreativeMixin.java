package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.DesignTextures;
import biscuitius.blueprints.client.item.BlueprintItem;
import biscuitius.blueprints.client.item.FillToolItem;
import biscuitius.blueprints.client.item.LineToolItem;
import biscuitius.blueprints.client.item.MoveToolItem;
import biscuitius.blueprints.client.item.OvalToolItem;
import biscuitius.blueprints.client.item.RectangleToolItem;
import biscuitius.blueprints.client.item.RotateToolItem;
import biscuitius.blueprints.client.item.SlotBlueprintTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.gui.ButtonElement;
import net.minecraft.client.gui.container.ScreenInventory;
import net.minecraft.client.gui.container.ScreenInventoryCreative;
import net.minecraft.client.render.EntityRendererDispatcher;
import net.minecraft.client.render.TextureManager;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import net.minecraft.client.render.texture.Texture;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.menu.MenuAbstract;
import net.minecraft.core.player.inventory.menu.MenuInventoryCreative;
import net.minecraft.core.player.inventory.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenInventoryCreative.class)
public abstract class ScreenInventoryCreativeMixin extends ScreenInventory {
   @Shadow
   protected String pageString;
   @Shadow
   protected ButtonElement lastPageButton;
   @Shadow
   protected ButtonElement nextPageButton;
   @Shadow
   protected ButtonElement clearSearchButton;
   @Unique
   private static final String DESIGN_TEXTURE = "/assets/blueprints/textures/gui/container/design.png";
   @Unique
   private static final int SURVIVAL_SLOT_COUNT = 9;
   @Unique
   private static final int TOOL_PANEL_X0 = 8;
   @Unique
   private static final int TOOL_PANEL_Y0 = 34;
   @Unique
   private static final int TOOL_SLOT_PITCH = 18;
   @Unique
   private static final int TOOL_PANEL_COLS = 9;
   @Unique
   private static final int TOOL_PANEL_ROWS = 2;

   protected ScreenInventoryCreativeMixin(Player player) {
      super(player);
   }

   @Redirect(
      method = {"drawGuiContainerBackgroundLayer", "buttonClicked"},
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeCreativeInventoryPlayer(Minecraft minecraft) {
      return DesignModeState.getControlPlayer(minecraft);
   }

   @Redirect(
      method = "drawGuiContainerBackgroundLayer",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/TextureManager;loadTexture(Ljava/lang/String;)Lnet/minecraft/client/render/texture/Texture;"
      )
   )
   private Texture blueprints$swapInventoryTexture(TextureManager textureManager, String path) {
      return textureManager.loadTexture(DesignModeState.isActive() ? "/assets/blueprints/textures/gui/container/design.png" : path);
   }

   @Redirect(
      method = "drawGuiContainerBackgroundLayer",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/EntityRendererDispatcher;renderEntityPreviewWithPosYaw(Lnet/minecraft/client/render/tessellator/TessellatorGeneral;Lnet/minecraft/core/entity/Entity;DDDFF)V"
      )
   )
   private void blueprints$skipPlayerPreview(
      EntityRendererDispatcher dispatcher, TessellatorGeneral tessellator, Entity entity, double x, double y, double z, float yaw, float partialTick
   ) {
      if (!DesignModeState.isActive()) {
         dispatcher.renderEntityPreviewWithPosYaw(tessellator, entity, x, y, z, yaw, partialTick);
      }
   }

   @Inject(method = "drawGuiContainerForegroundLayer", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipCraftingLabel(CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         ScreenInventoryCreative self = (ScreenInventoryCreative)(Object)this;
         Minecraft mc = Minecraft.getMinecraft();
         self.drawStringCenteredNoShadow(mc.font, this.pageString, 238, 146, 16777215);
         ci.cancel();
      }
   }

   @Inject(method = "init", at = @At("RETURN"))
   private void blueprints$initDesignModeUI(CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         ScreenInventoryCreative self = (ScreenInventoryCreative)(Object)this;
         MenuAbstract menu = self.inventorySlots;
         if (menu instanceof MenuInventoryCreative cm) {
            cm.searchPage(cm.getSearchText());
         }

         this.blueprints$addBlueprintToolSlots(menu);
         DesignTextures.applyButton(this.lastPageButton);
         DesignTextures.applyButton(this.nextPageButton);
         DesignTextures.applyClear(this.clearSearchButton);

         for (int i = 0; i < 9; i++) {
            Slot slot = (Slot)menu.slots.get(i);
            slot.x = -9999;
            slot.y = -9999;
         }

         for (Object element : this.overlayButtonsLayout.elements) {
            self.buttons.remove(element);
         }

         this.overlayButtonsLayout.elements.clear();

         for (Object btn : self.buttons) {
            if (((ButtonElement)btn).id == 100) {
               ((ButtonElement)btn).enabled = false;
               break;
            }
         }
      }
   }

   @Unique
   private void blueprints$addBlueprintToolSlots(MenuAbstract menu) {
      if (BlueprintItem.get() != null) {
         for (Object existing : menu.slots) {
            if (existing instanceof SlotBlueprintTool) {
               return;
            }
         }

         for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
               int x = 8 + col * 18;
               int y = 34 + row * 18;
               ItemStack item = null;
               if (row == 0 && col == 0) {
                  item = BlueprintItem.newStack();
               } else if (row == 0 && col == 1) {
                  item = MoveToolItem.newStack();
               } else if (row == 0 && col == 2) {
                  item = RotateToolItem.newStack();
               } else if (row == 0 && col == 3) {
                  item = FillToolItem.newStack();
               } else if (row == 0 && col == 4) {
                  item = LineToolItem.newStack();
               } else if (row == 0 && col == 5) {
                  item = RectangleToolItem.newStack();
               } else if (row == 0 && col == 6) {
                  item = OvalToolItem.newStack();
               }

               int index = menu.slots.size();
               SlotBlueprintTool slot = new SlotBlueprintTool(index, x, y, item);
               slot.index = index;
               menu.slots.add(slot);
               menu.lastSlots.add(null);
            }
         }
      }
   }

   @Inject(method = "tick", at = @At("RETURN"))
   private void blueprints$tickDesignModeUI(CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         ScreenInventoryCreative self = (ScreenInventoryCreative)(Object)this;

         for (Object btn : self.buttons) {
            if (((ButtonElement)btn).id == 100) {
               ((ButtonElement)btn).enabled = false;
               break;
            }
         }
      }
   }
}
