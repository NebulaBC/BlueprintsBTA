package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.core.world.Explosion;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
   @Shadow
   protected World worldObj;
   @Shadow
   public double explosionX;
   @Shadow
   public double explosionY;
   @Shadow
   public double explosionZ;
   @Shadow
   public float explosionSize;

   @Inject(method = "explode", at = @At("HEAD"))
   private void blueprints$patchBeforeExplode(CallbackInfo ci) {
      if (this.worldObj != null) {
         int radius = (int)Math.ceil(this.explosionSize * 2.0) + 2;
         int minX = (int)Math.floor(this.explosionX) - radius;
         int maxX = (int)Math.ceil(this.explosionX) + radius;
         int minY = (int)Math.floor(this.explosionY) - radius;
         int maxY = (int)Math.ceil(this.explosionY) + radius;
         int minZ = (int)Math.floor(this.explosionZ) - radius;
         int maxZ = (int)Math.ceil(this.explosionZ) + radius;
         GhostBlockState.beginPhysicsPatchScoped(this.worldObj, minX, minY, minZ, maxX, maxY, maxZ);
      }
   }

   @Inject(method = "explode", at = @At("RETURN"))
   private void blueprints$unpatchAfterExplode(CallbackInfo ci) {
      if (this.worldObj != null) {
         GhostBlockState.endPhysicsPatch(this.worldObj);
      }
   }

   @Inject(method = "addEffects", at = @At("HEAD"))
   private void blueprints$patchBeforeEffects(boolean particles, CallbackInfo ci) {
      if (this.worldObj != null) {
         int radius = (int)Math.ceil(this.explosionSize * 2.0) + 2;
         int minX = (int)Math.floor(this.explosionX) - radius;
         int maxX = (int)Math.ceil(this.explosionX) + radius;
         int minY = (int)Math.floor(this.explosionY) - radius;
         int maxY = (int)Math.ceil(this.explosionY) + radius;
         int minZ = (int)Math.floor(this.explosionZ) - radius;
         int maxZ = (int)Math.ceil(this.explosionZ) + radius;
         GhostBlockState.beginPhysicsPatchScoped(this.worldObj, minX, minY, minZ, maxX, maxY, maxZ);
      }
   }

   @Inject(method = "addEffects", at = @At("RETURN"))
   private void blueprints$unpatchAfterEffects(boolean particles, CallbackInfo ci) {
      if (this.worldObj != null) {
         GhostBlockState.endPhysicsPatch(this.worldObj);
      }
   }
}
