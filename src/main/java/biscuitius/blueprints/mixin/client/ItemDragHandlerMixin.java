package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.util.helper.ItemDragHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemDragHandler.class)
public abstract class ItemDragHandlerMixin {
   @Redirect(
      method = {"click", "spreadItemsAcrossDraggedSlots", "pickupSimilarItems", "getGrabbedItem"},
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeDraggedInventoryPlayer(Minecraft minecraft) {
      return DesignModeState.getControlPlayer(minecraft);
   }
}
