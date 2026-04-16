package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.GhostBlockState;
import java.util.LinkedList;
import net.minecraft.client.world.WorldClientMP;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldClientMP.class)
public abstract class WorldClientMPMixin {
   @Redirect(
      method = {"setBlockMetadata", "setBlockAndMetadata", "setBlock"},
      at = @At(value = "INVOKE", target = "Ljava/util/LinkedList;add(Ljava/lang/Object;)Z")
   )
   private boolean blueprints$skipTemporaryBlockHistory(LinkedList<Object> list, Object entry) {
      return !DesignModeState.isActive() && !GhostBlockState.isFulfillmentInProgress() && list.add(entry);
   }

   @Inject(method = "setBlockAndMetadata", at = @At("HEAD"))
   private void blueprints$captureBeforeSetBlockAndMeta(int x, int y, int z, int id, int meta, CallbackInfoReturnable<Boolean> cir) {
      if (GhostBlockState.isSuppressingLighting()) {
         GhostBlockState.capture((World)(Object)this, x, y, z);
      }
   }

   @Inject(method = "setBlockAndMetadata", at = @At("RETURN"))
   private void blueprints$syncAfterSetBlockAndMeta(int x, int y, int z, int id, int meta, CallbackInfoReturnable<Boolean> cir) {
      if (GhostBlockState.isSuppressingLighting()) {
         GhostBlockState.sync((World)(Object)this, x, y, z);
      }
   }

   @Inject(method = "setBlock", at = @At("HEAD"))
   private void blueprints$captureBeforeSetBlock(int x, int y, int z, int id, CallbackInfoReturnable<Boolean> cir) {
      if (GhostBlockState.isSuppressingLighting()) {
         GhostBlockState.capture((World)(Object)this, x, y, z);
      }
   }

   @Inject(method = "setBlock", at = @At("RETURN"))
   private void blueprints$syncAfterSetBlock(int x, int y, int z, int id, CallbackInfoReturnable<Boolean> cir) {
      if (GhostBlockState.isSuppressingLighting()) {
         GhostBlockState.sync((World)(Object)this, x, y, z);
      }
   }

   @Inject(method = "setBlockMetadata", at = @At("HEAD"))
   private void blueprints$captureBeforeSetBlockMeta(int x, int y, int z, int meta, CallbackInfoReturnable<Boolean> cir) {
      if (GhostBlockState.isSuppressingLighting()) {
         GhostBlockState.capture((World)(Object)this, x, y, z);
      }
   }

   @Inject(method = "setBlockMetadata", at = @At("RETURN"))
   private void blueprints$syncAfterSetBlockMeta(int x, int y, int z, int meta, CallbackInfoReturnable<Boolean> cir) {
      if (GhostBlockState.isSuppressingLighting()) {
         GhostBlockState.sync((World)(Object)this, x, y, z);
      }
   }

   @Inject(
      method = "blockChange",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/WorldClientMP;removePositionTypesInBounds(IIIIII)V", shift = Shift.AFTER),
      cancellable = true
   )
   private void blueprints$protectGhostBlock(int x, int y, int z, int id, int meta, CallbackInfoReturnable<Boolean> cir) {
      World world = (World)(Object)this;
      if (GhostBlockState.isTracked(world, x, y, z)) {
         GhostBlockState.updateServer(world, x, y, z, id, meta);
         int ghostId = GhostBlockState.getGhostBlockId(world, x, y, z);
         int ghostMeta = GhostBlockState.getGhostMetadata(world, x, y, z);
         if ((id != ghostId || meta != ghostMeta) && id == 0 && !GhostBlockState.isHidden() && y <= GhostBlockState.getLayerCutoffY()) {
            GhostBlockState.setBlockNoLighting(world, x, y, z, ghostId, ghostMeta);
            world.markBlocksDirty(x, y, z, x, y, z);
            cir.setReturnValue(true);
         } else {
            world.markBlocksDirty(x, y, z, x, y, z);
         }
      }
   }

   @Inject(method = "blockChange", at = @At("RETURN"))
   private void blueprints$correctFulfillmentMeta(int x, int y, int z, int id, int meta, CallbackInfoReturnable<Boolean> cir) {
      int[] desired = GhostBlockState.getRecentFulfillment(x, y, z);
      if (desired != null && id == desired[0] && meta != desired[1]) {
         World world = (World)(Object)this;
         GhostBlockState.setBlockNoLighting(world, x, y, z, id, desired[1]);
         world.markBlocksDirty(x, y, z, x, y, z);
      }
   }
}
