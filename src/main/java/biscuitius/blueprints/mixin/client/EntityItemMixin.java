package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityItem.class)
public abstract class EntityItemMixin {
   @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelPickupInDesignMode(Player player, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         ci.cancel();
      }
   }
}
