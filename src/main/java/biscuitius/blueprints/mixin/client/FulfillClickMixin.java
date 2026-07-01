package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.hologram.HologramController;
import biscuitius.blueprints.client.hologram.HologramStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.Vec3;
import net.minecraft.core.util.phys.HitResult.HitType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class FulfillClickMixin {
   @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
   private void blueprints$handleFulfillClick(int clickType, boolean attack, boolean repeat, CallbackInfo ci) {
      if (clickType == 1) {
         if (!DesignModeState.isActive()) {
            if (!DesignModeState.isPassthroughMode()) {
               Minecraft mc = Minecraft.getMinecraft();
               if (mc.currentWorld != null && mc.thePlayer != null) {
                  if (HologramStore.hasEntries(mc.currentWorld)) {
                     PlayerLocal player = mc.thePlayer;
                     ItemStack held = player.getCurrentEquippedItem();
                     if (held != null) {
                        double reach = mc.playerController.getBlockReachDistance();
                        Vec3 start = player.getPosition(1.0F, false);
                        Vec3 look = player.getViewVector(1.0F);
                        Vec3 end = start.add(look.x * reach, look.y * reach, look.z * reach);
                        HitResult realHit = mc.objectMouseOver;
                        HitResult overlaid = HologramController.pickHologramOverlay(mc.currentWorld, start, end, realHit);
                        if (overlaid != realHit && overlaid != null) {
                           if (overlaid.hitType == HitType.TILE) {
                              if (HologramController.tryFulfill(mc, player, held, overlaid.x, overlaid.y, overlaid.z)) {
                                 player.swingItem();
                                 MinecraftAccessor accessor = (MinecraftAccessor)mc;
                                 accessor.setMouseTicksRan(accessor.getTicksRan());
                                 ci.cancel();
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
