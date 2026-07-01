package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.client.player.controller.PlayerControllerMP;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.net.packet.Packet;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public abstract class PlayerControllerMPMixin {
   @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelDestroyBlock(int x, int y, int z, Side side, Player player, CallbackInfoReturnable<Boolean> cir) {
      if (DesignModeState.isActive()) {
         cir.setReturnValue(false);
      }
   }

   @ModifyVariable(method = "useOrPlaceItemStackOnTile", at = @At("HEAD"), argsOnly = true, ordinal = 0)
   private ItemStack blueprints$shuffleItemStack(ItemStack itemstack) {
      if (DesignModeState.isActive() && DesignModeState.isShuffleEnabled()) {
         PlayerLocal dp = DesignModeState.getDesignPlayer();
         return dp == null ? itemstack : DesignModeState.shuffleAndGetItem(dp.inventory);
      } else {
         return itemstack;
      }
   }

   @Redirect(
      method = {"destroyBlock", "syncCurrentPlayItem"},
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeControllerPlayer(Minecraft minecraft) {
      return DesignModeState.isActive() ? DesignModeState.getControlPlayer(minecraft) : minecraft.thePlayer;
   }

   @Redirect(
      method = {
            "hitBlock",
            "sendStartDigPacket",
            "sendDestroyBlockPacket",
            "syncCurrentPlayItem",
            "useOrPlaceItemStackOnTile",
            "useItemStackOnNothing",
            "placeItemStackOnTile",
            "attack",
            "interact",
            "handleInventoryMouseClick",
            "setPaintingType",
            "dropCurrentItem",
            "pickBlock",
            "sendSpecialVehiclePacket"
      },
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/net/handler/PacketHandlerClient;addToSendQueue(Lnet/minecraft/core/net/packet/Packet;)V")
   )
   private void blueprints$skipControllerPacket(PacketHandlerClient handler, Packet packet) {
      if (!DesignModeState.isActive()) {
         handler.addToSendQueue(packet);
      }
   }

   @Inject(method = "useItemStackOnNothing", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelMPUseOnNothing(Player player, World world, ItemStack itemstack, CallbackInfoReturnable<Boolean> cir) {
      if (DesignModeState.isActive()) {
         cir.setReturnValue(false);
      }
   }
}
