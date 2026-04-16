package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.gui.HeldItemTooltipElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HeldItemTooltipElement.class)
public abstract class HeldItemTooltipElementMixin {
   @Redirect(
      method = {"updateAndRender(Lnet/minecraft/client/Minecraft;III)V", "updateAndRender(Lnet/minecraft/client/Minecraft;II)V"},
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeTooltipPlayer(Minecraft minecraft) {
      return DesignModeState.getControlPlayer(minecraft);
   }
}
