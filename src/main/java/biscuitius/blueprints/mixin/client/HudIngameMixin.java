package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.HudIngame;
import net.minecraft.client.gui.hud.component.HudComponent;
import net.minecraft.client.gui.hud.component.HudComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HudIngame.class)
public abstract class HudIngameMixin {
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
}
