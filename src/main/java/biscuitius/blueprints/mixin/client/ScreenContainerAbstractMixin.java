package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.item.SlotBlueprintTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.gui.container.ScreenContainerAbstract;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.slot.Slot;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenContainerAbstract.class)
public abstract class ScreenContainerAbstractMixin {
   @Redirect(
      method = {"render", "init", "keyPressed", "clickInventory"},
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeContainerScreenPlayer(Minecraft minecraft) {
      return DesignModeState.getControlPlayer(minecraft);
   }

   @Inject(method = "clickInventory", at = @At("HEAD"), cancellable = true)
   private void blueprints$handleBlueprintToolSlotClick(int x, int y, int mouseButton, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         ScreenContainerAbstract self = (ScreenContainerAbstract)(Object)this;
         Slot slot = self.getSlotAtPosition(x, y);
         if (slot instanceof SlotBlueprintTool) {
            PlayerLocal player = DesignModeState.getControlPlayer(Minecraft.getMinecraft());
            if (player != null) {
               ItemStack source = slot.getItemStack();
               if (source == null) {
                  player.inventory.setHeldItemStack(null);
                  ci.cancel();
               } else {
                  ItemStack grabbed = source.copy();
                  boolean maxStack = mouseButton == 2
                     || Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                     || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
                     || Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                     || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
                  grabbed.stackSize = maxStack ? grabbed.getMaxStackSize() : 1;
                  player.inventory.setHeldItemStack(grabbed);
                  ci.cancel();
               }
            }
         }
      }
   }
}
