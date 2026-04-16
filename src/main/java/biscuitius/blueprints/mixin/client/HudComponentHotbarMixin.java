package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeOverlay;
import biscuitius.blueprints.client.DesignModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.gui.hud.HudIngame;
import net.minecraft.client.gui.hud.component.HudComponentHotbar;
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
   private void blueprints$swapHotbarPlayer(Minecraft mc, HudIngame hud, int xSizeScreen, int ySizeScreen, float partialTick, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         PlayerLocal controlPlayer = DesignModeState.getControlPlayer(mc);
         if (controlPlayer != null) {
            this.blueprints$previousHudPlayer = mc.thePlayer;
            mc.thePlayer = controlPlayer;
         }
      }
   }

   @Inject(method = "render", at = @At("RETURN"))
   private void blueprints$restoreHotbarPlayer(Minecraft mc, HudIngame hud, int xSizeScreen, int ySizeScreen, float partialTick, CallbackInfo ci) {
      if (this.blueprints$previousHudPlayer != null) {
         mc.thePlayer = this.blueprints$previousHudPlayer;
         this.blueprints$previousHudPlayer = null;
      }

      DesignModeOverlay.renderIfActive(mc, xSizeScreen / 2, ySizeScreen - 52);
   }

   @Redirect(
      method = {"render", "renderInventorySlot"},
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeHotbarPlayer(Minecraft minecraft) {
      return DesignModeState.getControlPlayer(minecraft);
   }

   @ModifyArg(
      method = "render",
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
