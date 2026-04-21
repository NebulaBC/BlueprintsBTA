package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
   @Shadow
   public boolean noPhysics;
   @Shadow
   public World world;
   @Unique
   private boolean blueprints$wasNoclip;

   @Inject(method = "move", at = @At("HEAD"))
   private void blueprints$forceCollisionHead(double xd, double yd, double zd, CallbackInfo ci) {
      if (this.noPhysics && DesignModeState.isActive() && (Entity)(Object)this == DesignModeState.getDesignPlayer()) {
         this.blueprints$wasNoclip = true;
         this.noPhysics = false;
         DesignModeState.setDesignPlayerNoclipOverride(true);
      } else {
         this.blueprints$wasNoclip = false;
         if (DesignModeState.isActive() && (Entity)(Object)this == DesignModeState.getDesignPlayer()) {
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

   @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
   private void blueprints$preventGhostSuffocation(CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity)(Object)this;
      if (self.world != null) {
         for (int i = 0; i < 8; i++) {
            float f = ((i >> 0) % 2 - 0.5F) * self.bbWidth * 0.9F;
            float f1 = ((i >> 1) % 2 - 0.5F) * 0.1F;
            float f2 = ((i >> 2) % 2 - 0.5F) * self.bbWidth * 0.9F;
            int x = MathHelper.floor(self.x + f);
            int y = MathHelper.floor(self.y + self.getHeadHeight() + f1);
            int z = MathHelper.floor(self.z + f2);
            int blockId = GhostBlockState.getServerBlockId(self.world, x, y, z);
            Block<?> block = Blocks.blocksList[blockId];
            if (block != null && block.getMaterial().isSolidBlocking() && block.renderAsNormalBlockOnCondition(self.world, x, y, z)) {
               cir.setReturnValue(true);
               return;
            }
         }

         cir.setReturnValue(false);
      }
   }

   @Redirect(
      method = "move",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/block/Block;onEntityCollidedWithBlock(Lnet/minecraft/core/world/World;IIILnet/minecraft/core/entity/Entity;)V"
      )
   )
   private void blueprints$skipGhostCollision(Block<?> block, World world, int x, int y, int z, Entity entity) {
      if (!GhostBlockState.isGhostBlock(world, x, y, z)) {
         block.onEntityCollidedWithBlock(world, x, y, z, entity);
      }
   }
}
