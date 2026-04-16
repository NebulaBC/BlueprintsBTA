package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.gui.ButtonElement;
import net.minecraft.client.gui.container.ScreenInventory;
import net.minecraft.client.gui.container.ScreenInventoryCreative;
import net.minecraft.client.render.EntityRenderDispatcher;
import net.minecraft.client.render.TextureManager;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.texture.Texture;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.player.inventory.menu.MenuAbstract;
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
   @Unique
   private static final String DESIGN_TEXTURE = "/assets/blueprints/textures/gui/container/design.png";
   @Unique
   private static final int SURVIVAL_SLOT_COUNT = 9;

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
         target = "Lnet/minecraft/client/render/EntityRenderDispatcher;renderEntityWithPosYaw(Lnet/minecraft/client/render/tessellator/Tessellator;Lnet/minecraft/core/entity/Entity;DDDFF)V"
      )
   )
   private void blueprints$skipPlayerPreview(
      EntityRenderDispatcher dispatcher, Tessellator tessellator, Entity entity, double x, double y, double z, float yaw, float partialTick
   ) {
      if (!DesignModeState.isActive()) {
         dispatcher.renderEntityWithPosYaw(tessellator, entity, x, y, z, yaw, partialTick);
      }
   }

   @Inject(method = "drawGuiContainerForegroundLayer", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipCraftingLabel(CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         ScreenInventoryCreative self = (ScreenInventoryCreative)(Object)this;
         Minecraft mc = Minecraft.getMinecraft();
         self.drawStringCenteredNoShadow(mc.font, this.pageString, 238, 146, 4210752);
         ci.cancel();
      }
   }

   @Inject(method = "init", at = @At("RETURN"))
   private void blueprints$initDesignModeUI(CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         ScreenInventoryCreative self = (ScreenInventoryCreative)(Object)this;
         MenuAbstract menu = self.inventorySlots;

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
