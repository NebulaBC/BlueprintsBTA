package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.MoveToolController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.PlayerInput;
import net.minecraft.core.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInput.class)
public abstract class PlayerInputMoveToolMixin {
   @Shadow
   public float moveStrafe;
   @Shadow
   public float moveForward;
   @Shadow
   public boolean jump;
   @Shadow
   public boolean sneak;

   @Inject(method = "tick", at = @At("TAIL"))
   private void blueprints$moveToolTick(Player entityplayer, CallbackInfo ci) {
      Minecraft mc = Minecraft.getMinecraft();
      if (!MoveToolController.isHeld(mc)) {
         MoveToolController.reset();
      } else if (entityplayer == MoveToolController.getControlPlayer(mc)) {
         if (MoveToolController.isArmed()) {
            MoveToolController.tickHeld(mc);
            this.moveForward = 0.0F;
            this.moveStrafe = 0.0F;
            this.jump = false;
            this.sneak = false;
         }
      }
   }
}
