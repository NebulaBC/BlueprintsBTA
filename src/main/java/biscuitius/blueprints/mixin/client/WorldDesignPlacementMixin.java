package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.HologramPlacementContext;
import biscuitius.blueprints.client.hologram.HologramRenderer;
import biscuitius.blueprints.client.hologram.HologramStore;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.enums.EnumBlockSoundEffectType;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePosc;
import org.joml.primitives.AABBdc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldDesignPlacementMixin {
   @Inject(method = "setBlockTypeDataRaw", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureSetTypeDataRaw(TilePosc pos, Block<?> block, int data, CallbackInfoReturnable<Boolean> cir) {
      this.blueprints$writeIdMeta(pos, block == null ? 0 : block.id(), data, cir);
   }

   @Inject(method = "setBlockTypeData", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureSetTypeData(TilePosc pos, Block<?> block, int data, CallbackInfoReturnable<Boolean> cir) {
      this.blueprints$writeIdMeta(pos, block == null ? 0 : block.id(), data, cir);
   }

   @Inject(method = "setBlockType", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureSetType(TilePosc pos, Block<?> block, CallbackInfoReturnable<Boolean> cir) {
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

   @Inject(method = "setBlockData", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureSetData(TilePosc pos, int data, CallbackInfoReturnable<Boolean> cir) {
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

   private void blueprints$writeIdMeta(TilePosc pos, int id, int meta, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         int x = pos.x();
         int y = pos.y();
         int z = pos.z();
         if (HologramPlacementContext.isDryRun(self)) {
            HologramPlacementContext.captureWrite(x, y, z, id, meta);
            cir.setReturnValue(id != 0);
         } else {
            if (id == 0) {
               HologramStore.remove(self, x, y, z);
            } else {
               HologramStore.put(self, x, y, z, new HologramBlock(id, meta));
            }

            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "getBlockType", at = @At("HEAD"), cancellable = true)
   private void blueprints$readBlockType(TilePosc pos, CallbackInfoReturnable<Block<?>> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         int x = pos.x();
         int y = pos.y();
         int z = pos.z();
         if (HologramPlacementContext.isDryRun(self)) {
            int[] captured = HologramPlacementContext.captureRead(x, y, z);
            if (captured != null) {
               cir.setReturnValue(captured[0] == 0 ? Blocks.AIR : Blocks.getBlock(captured[0]));
            }
         } else {
            HologramBlock h = HologramStore.get(self, x, y, z);
            if (h != null) {
               cir.setReturnValue(h.blockId == 0 ? Blocks.AIR : Blocks.getBlock(h.blockId));
            }
         }
      }
   }

   @Inject(method = "getBlockData", at = @At("HEAD"), cancellable = true)
   private void blueprints$readBlockData(TilePosc pos, CallbackInfoReturnable<Integer> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         int x = pos.x();
         int y = pos.y();
         int z = pos.z();
         if (HologramPlacementContext.isDryRun(self)) {
            int[] captured = HologramPlacementContext.captureRead(x, y, z);
            if (captured != null) {
               cir.setReturnValue(captured[1]);
            }
         } else {
            HologramBlock h = HologramStore.get(self, x, y, z);
            if (h != null) {
               cir.setReturnValue(h.metadata);
            }
         }
      }
   }

   @Inject(method = "setTileEntity", at = @At("HEAD"), cancellable = true)
   private void blueprints$suppressTileEntityWrite(TilePosc pos, TileEntity tileEntity, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(method = "notifyBlockChange", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipBlockChange(TilePosc pos, Block<?> block, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(method = "notifyBlocksOfNeighborChange", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipNeighborNotify(TilePosc pos, Block<?> block, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(method = "markBlockNeedsUpdate", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipMarkNeedsUpdate(TilePosc pos, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(method = "checkIfAABBIsClear", at = @At("HEAD"), cancellable = true)
   private void blueprints$bypassEntityCheck(AABBdc aabb, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(method = "playBlockSoundEffect", at = @At("HEAD"), cancellable = true)
   private void blueprints$suppressDryRunSound(Entity player, double x, double y, double z, Block<?> block, EnumBlockSoundEffectType type, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isDryRun(self)) {
         ci.cancel();
      }
   }

   @Inject(method = "markBlockNeedsUpdate", at = @At("RETURN"))
   private void blueprints$notifyRealChange(TilePosc pos, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (!HologramPlacementContext.isActive(self)) {
         HologramRenderer.notifyRealBlockChanged(self, pos.x(), pos.y(), pos.z());
      }
   }

   @Inject(method = "markBlocksDirty", at = @At("RETURN"))
   private void blueprints$notifyRealRegionChange(TilePosc min, TilePosc max, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (!HologramPlacementContext.isActive(self)) {
         int minX = min.x();
         int minY = min.y();
         int minZ = min.z();
         int maxX = max.x();
         int maxY = max.y();
         int maxZ = max.z();

         for (int sx = minX >> 4; sx <= maxX >> 4; sx++) {
            for (int sy = Math.max(0, minY) >> 4; sy <= Math.min(255, maxY) >> 4; sy++) {
               for (int sz = minZ >> 4; sz <= maxZ >> 4; sz++) {
                  HologramRenderer.notifyRealBlockChanged(self, sx << 4, sy << 4, sz << 4);
               }
            }
         }
      }
   }
}
