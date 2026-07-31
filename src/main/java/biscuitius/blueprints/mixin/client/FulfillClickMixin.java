package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.hologram.HologramController;
import biscuitius.blueprints.client.hologram.HologramStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.HitResult.Tile;
import org.joml.Vector3d;
import org.joml.Vector3dc;
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
               Minecraft mc = (Minecraft)(Object)this;
               if (mc.currentWorld != null && mc.thePlayer != null) {
                  if (HologramStore.hasEntries(mc.currentWorld)) {
                     PlayerLocal player = mc.thePlayer;
                     ItemStack held = player.getCurrentEquippedItem();
                     if (held != null) {
                        double reach = player.getGamemode().getBlockReachDistance();
                        Vector3dc pos = player.getPosition(1.0F, true);
                        Vector3dc look = player.getViewVector(1.0F);
                        Vector3dc start = new Vector3d(pos);
                        Vector3dc end = new Vector3d(pos).add(look.x() * reach, look.y() * reach, look.z() * reach);
                        HitResult realHit = mc.objectMouseOver;
                        HitResult overlaid = HologramController.pickHologramOverlay(mc.currentWorld, start, end, realHit);
                        if (overlaid != realHit && overlaid != null) {
                           if (overlaid instanceof Tile overlaidTile) {
                              if (HologramController.tryFulfill(mc, player, held, overlaidTile.tilePos.x(), overlaidTile.tilePos.y(), overlaidTile.tilePos.z())
                                 )
                               {
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
