package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.core.net.packet.PacketSetHealth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketHandlerClient.class)
public abstract class PacketHandlerClientMixin {
   @Shadow
   @Final
   private Minecraft mc;

   @Inject(method = "handleUpdateHealth", at = @At("HEAD"))
   private void blueprints$exitOnDamage(PacketSetHealth packet, CallbackInfo ci) {
      if (DesignModeState.isActive() && this.mc != null && this.mc.thePlayer != null && packet.healthMP < this.mc.thePlayer.getHealth()) {
         DesignModeState.handleDamageExit(this.mc);
      }
   }
}
