package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.util.helper.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {
   @Inject(method = "canClimb", at = @At("RETURN"), cancellable = true)
   private void blueprints$noClimbGhostBlocks(CallbackInfoReturnable<Boolean> cir) {
      if (Boolean.TRUE.equals(cir.getReturnValue())) {
         Mob self = (Mob)(Object)this;
         int x = MathHelper.floor(self.x);
         int y = MathHelper.floor(self.bb.minY);
         int z = MathHelper.floor(self.z);
         if (self.world != null && GhostBlockState.isGhostBlock(self.world, x, y, z)) {
            cir.setReturnValue(false);
         }
      }
   }
}
