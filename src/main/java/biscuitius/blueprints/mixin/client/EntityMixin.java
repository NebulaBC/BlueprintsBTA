package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.core.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
   @Shadow
   public boolean noPhysics;
   @Unique
   private boolean blueprints$wasNoclip;

   @Inject(method = "move", at = @At("HEAD"))
   private void blueprints$forceCollisionHead(double xd, double yd, double zd, CallbackInfo ci) {
      if (this.noPhysics && DesignModeState.isActive() && Minecraft.getMinecraft().thePlayer == DesignModeState.getDesignPlayer()) {
         this.blueprints$wasNoclip = true;
         this.noPhysics = false;
         DesignModeState.setDesignPlayerNoclipOverride(true);
      } else {
         this.blueprints$wasNoclip = false;
         if (DesignModeState.isActive() && Minecraft.getMinecraft().thePlayer == DesignModeState.getDesignPlayer()) {
            DesignModeState.setDesignPlayerNoclipOverride(false);
         }
      }
   }

   @Inject(method = "move", at = @At("RETURN"))
   private void blueprints$forceCollisionReturn(double xd, double yd, double zd, CallbackInfo ci) {
      if (this.blueprints$wasNoclip) {
         this.noPhysics = true;
         this.blueprints$wasNoclip = false;
      }
   }
}
