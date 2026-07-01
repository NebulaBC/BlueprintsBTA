package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.player.controller.PlayerController;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerController.class)
public abstract class PlayerControllerMixin {
   @Redirect(
      method = "swingItem",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeSwingItemPlayer(Minecraft minecraft) {
      return DesignModeState.getControlPlayer(minecraft);
   }

   @Redirect(
      method = "getBlockReachDistance",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeBlockReach(Minecraft minecraft) {
      return DesignModeState.getControlPlayer(minecraft);
   }

   @Redirect(
      method = "getEntityReachDistance",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeEntityReach(Minecraft minecraft) {
      return DesignModeState.getControlPlayer(minecraft);
   }

   @Inject(method = "useItemStackOnNothing", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelUseOnNothing(Player player, World world, ItemStack itemstack, CallbackInfoReturnable<Boolean> cir) {
      if (DesignModeState.isActive()) {
         cir.setReturnValue(false);
      }
   }
}
