package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.core.world.World;
import net.minecraft.core.world.chunk.LightUpdate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightUpdate.class)
public abstract class LightUpdateMixin {
   @Shadow
   public int minX;
   @Shadow
   public int minY;
   @Shadow
   public int minZ;
   @Shadow
   public int maxX;
   @Shadow
   public int maxY;
   @Shadow
   public int maxZ;

   @Inject(method = "performLightUpdate", at = @At("HEAD"))
   private void blueprints$patchBeforeLightUpdate(World world, CallbackInfo ci) {
      GhostBlockState.beginLightPatchScoped(world, this.minX - 1, this.minY - 1, this.minZ - 1, this.maxX + 1, this.maxY + 1, this.maxZ + 1);
   }

   @Inject(method = "performLightUpdate", at = @At("RETURN"))
   private void blueprints$unpatchAfterLightUpdate(World world, CallbackInfo ci) {
      GhostBlockState.endLightPatch(world);
   }
}
