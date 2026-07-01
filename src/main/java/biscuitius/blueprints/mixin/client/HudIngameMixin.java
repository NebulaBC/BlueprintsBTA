package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.compat.BTWailaCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.HudIngame;
import net.minecraft.client.gui.hud.component.HudComponent;
import net.minecraft.client.gui.hud.component.HudComponents;
import net.minecraft.core.util.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HudIngame.class)
public abstract class HudIngameMixin {
   private static HitResult blueprints$savedMouseOver;

   @Redirect(
      method = "renderGameOverlay",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/component/HudComponent;isVisible(Lnet/minecraft/client/Minecraft;)Z")
   )
   private boolean blueprints$hideSurvivalHud(HudComponent component, Minecraft mc) {
      return !DesignModeState.isActive()
            || component != HudComponents.HEALTH_BAR
               && component != HudComponents.ARMOR_BAR
               && component != HudComponents.OXYGEN_BAR
               && component != HudComponents.FIRE_BAR
         ? component.isVisible(mc)
         : false;
   }

   @Inject(method = "renderGameOverlay", at = @At("HEAD"))
   private void blueprints$hideHologramHitFromHud(float partialTick, boolean hasMouse, int mouseX, int mouseY, CallbackInfo ci) {
      Minecraft mc = Minecraft.getMinecraft();
      if (BTWailaCompat.shouldSkipForHologramOnlyTile(mc)) {
         blueprints$savedMouseOver = mc.objectMouseOver;
         mc.objectMouseOver = null;
      }
   }

   @Inject(method = "renderGameOverlay", at = @At("RETURN"))
   private void blueprints$restoreHologramHitForHud(float partialTick, boolean hasMouse, int mouseX, int mouseY, CallbackInfo ci) {
      if (blueprints$savedMouseOver != null) {
         Minecraft.getMinecraft().objectMouseOver = blueprints$savedMouseOver;
         blueprints$savedMouseOver = null;
      }
   }
}
