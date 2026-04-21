package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityFallingBlock;
import net.minecraft.core.util.helper.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityFallingBlock.class)
public abstract class EntityFallingBlockMixin {
   @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
   private void blueprints$preventGhostDrop(CallbackInfo ci) {
      Entity self = (Entity)(Object)this;
      if (self.world != null) {
         int x = MathHelper.round(self.x - 0.5);
         int y = MathHelper.round(self.y);
         int z = MathHelper.round(self.z - 0.5);
         if (GhostBlockState.isTracked(self.world, x, y, z)) {
            ci.cancel();
         }
      }
   }
}
