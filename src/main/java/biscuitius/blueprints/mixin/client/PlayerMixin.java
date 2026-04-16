package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
   @Inject(method = "dropPlayerItemWithRandomChoice", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelDesignPlayerDrop(ItemStack itemStack, boolean random, CallbackInfo ci) {
      if (DesignModeState.isActive() && Minecraft.getMinecraft().thePlayer == DesignModeState.getDesignPlayer()) {
         ci.cancel();
      }
   }
}
