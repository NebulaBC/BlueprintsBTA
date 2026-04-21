package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
   @Inject(method = "dropPlayerItemWithRandomChoice", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelDesignPlayerDrop(ItemStack itemStack, boolean random, CallbackInfo ci) {
      if (DesignModeState.isActive() && (PlayerLocal)(Object)this == DesignModeState.getDesignPlayer()) {
         ci.cancel();
      }
   }

   @Inject(method = "push(Lnet/minecraft/core/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelDesignRealPush(Entity entity, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         Player self = (Player)(Object)this;
         Player designPlayer = DesignModeState.getDesignPlayer();
         if (designPlayer != null) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
               if (self == designPlayer && entity == mc.thePlayer || self == mc.thePlayer && entity == designPlayer) {
                  ci.cancel();
               }
            }
         }
      }
   }

   @Inject(method = "handlePortal", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelDesignPlayerPortal(int portalBlockId, DyeColor portalColor, CallbackInfo ci) {
      if (DesignModeState.isActive() && (Player)(Object)this == DesignModeState.getDesignPlayer()) {
         ci.cancel();
      }
   }

   @Inject(method = "collideWithPlayer", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelItemPickupInDesignMode(Entity entity, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         if ((Player)(Object)this != DesignModeState.getDesignPlayer()) {
            ci.cancel();
         }
      }
   }
}
