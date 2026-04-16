package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.MobRendererPlayer;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobRendererPlayer.class)
public abstract class MobRendererPlayerMixin {
   @Inject(
      method = "renderSpecials(Lnet/minecraft/client/render/tessellator/Tessellator;Lnet/minecraft/core/entity/player/Player;DDD)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void blueprints$hideRealPlayerNametag(Tessellator tessellator, Player entity, double d, double d1, double d2, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc != null && entity == mc.thePlayer) {
            ci.cancel();
         }
      }
   }
}
