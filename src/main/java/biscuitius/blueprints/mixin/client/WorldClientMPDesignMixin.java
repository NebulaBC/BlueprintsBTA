package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.HologramPlacementContext;
import biscuitius.blueprints.client.hologram.HologramStore;
import net.minecraft.client.world.WorldClientMP;
import net.minecraft.core.block.Block;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePosc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldClientMP.class)
public abstract class WorldClientMPDesignMixin {
   @Inject(method = "setBlockData", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipPredictionForData(TilePosc pos, int data, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         int x = pos.x();
         int y = pos.y();
         int z = pos.z();
         if (HologramPlacementContext.isDryRun(self)) {
            int[] existing = HologramPlacementContext.captureRead(x, y, z);
            int id = existing != null ? existing[0] : 0;
            HologramPlacementContext.captureWrite(x, y, z, id, data);
            cir.setReturnValue(true);
         } else {
            HologramBlock current = HologramStore.get(self, x, y, z);
            if (current != null) {
               HologramStore.put(self, x, y, z, current.withMetadata(data));
            }

            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "setBlockTypeData", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipPredictionForTypeData(TilePosc pos, Block<?> block, int data, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         int id = block == null ? 0 : block.id();
         int x = pos.x();
         int y = pos.y();
         int z = pos.z();
         if (HologramPlacementContext.isDryRun(self)) {
            HologramPlacementContext.captureWrite(x, y, z, id, data);
            cir.setReturnValue(id != 0);
         } else {
            if (id == 0) {
               HologramStore.remove(self, x, y, z);
            } else {
               HologramStore.put(self, x, y, z, new HologramBlock(id, data));
            }

            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "setBlockType", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipPredictionForType(TilePosc pos, Block<?> block, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         int id = block == null ? 0 : block.id();
         int x = pos.x();
         int y = pos.y();
         int z = pos.z();
         if (HologramPlacementContext.isDryRun(self)) {
            int[] existing = HologramPlacementContext.captureRead(x, y, z);
            int meta = existing != null ? existing[1] : 0;
            HologramPlacementContext.captureWrite(x, y, z, id, meta);
            cir.setReturnValue(id != 0);
         } else {
            if (id == 0) {
               HologramStore.remove(self, x, y, z);
            } else {
               HologramBlock prev = HologramStore.get(self, x, y, z);
               int meta = prev != null ? prev.metadata : 0;
               HologramStore.put(self, x, y, z, new HologramBlock(id, meta));
            }

            cir.setReturnValue(true);
         }
      }
   }
}
