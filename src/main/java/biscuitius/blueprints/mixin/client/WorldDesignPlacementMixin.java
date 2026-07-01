package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.HologramPlacementContext;
import biscuitius.blueprints.client.hologram.HologramRenderer;
import biscuitius.blueprints.client.hologram.HologramStore;
import net.minecraft.core.block.Block;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.enums.EnumBlockSoundEffectType;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldDesignPlacementMixin {
   @Inject(method = "setBlockAndMetadataWithNotify", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureSetBlock(int x, int y, int z, int id, int meta, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
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

   @Inject(method = "setBlockWithNotify", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureSetBlockOnly(int x, int y, int z, int id, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         if (HologramPlacementContext.isDryRun(self)) {
            HologramPlacementContext.captureWrite(x, y, z, id, 0);
            cir.setReturnValue(id != 0);
         } else {
            if (id == 0) {
               HologramStore.remove(self, x, y, z);
            } else {
               HologramStore.put(self, x, y, z, new HologramBlock(id, 0));
            }

            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "setBlockMetadataWithNotify", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureSetMeta(int x, int y, int z, int meta, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         if (HologramPlacementContext.isDryRun(self)) {
            int[] existing = HologramPlacementContext.captureRead(x, y, z);
            int id = existing != null ? existing[0] : 0;
            HologramPlacementContext.captureWrite(x, y, z, id, meta);
            ci.cancel();
         } else {
            HologramBlock current = HologramStore.get(self, x, y, z);
            if (current != null) {
               HologramStore.put(self, x, y, z, current.withMetadata(meta));
            }

            ci.cancel();
         }
      }
   }

   @Inject(method = "setBlockMetadata", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureSetMetaRaw(int x, int y, int z, int meta, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         if (HologramPlacementContext.isDryRun(self)) {
            int[] existing = HologramPlacementContext.captureRead(x, y, z);
            int id = existing != null ? existing[0] : 0;
            HologramPlacementContext.captureWrite(x, y, z, id, meta);
            cir.setReturnValue(true);
         } else {
            HologramBlock current = HologramStore.get(self, x, y, z);
            if (current != null) {
               HologramStore.put(self, x, y, z, current.withMetadata(meta));
            }

            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "setBlockAndMetadata", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureSetBlockAndMetaRaw(int x, int y, int z, int id, int meta, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
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

   @Inject(method = "setBlockAndMetadataRaw", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureSetBlockAndMetaRawer(int x, int y, int z, int id, int meta, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
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

   @Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureSetBlockRaw(int x, int y, int z, int id, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
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

   @Inject(method = "setBlockRaw", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureSetBlockRawer(int x, int y, int z, int id, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
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

   @Inject(method = "getBlockId", at = @At("HEAD"), cancellable = true)
   private void blueprints$readBlockId(int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
      if (HologramPlacementContext.isActive()) {
         World self = (World)(Object)this;
         if (self == null || HologramPlacementContext.isActive(self)) {
            if (HologramPlacementContext.isDryRun(self)) {
               int[] captured = HologramPlacementContext.captureRead(x, y, z);
               if (captured != null) {
                  cir.setReturnValue(captured[0]);
               }
            } else {
               HologramBlock h = HologramStore.get(self, x, y, z);
               if (h != null) {
                  cir.setReturnValue(h.blockId);
               }
            }
         }
      }
   }

   @Inject(method = "getBlockMetadata", at = @At("HEAD"), cancellable = true)
   private void blueprints$readBlockMeta(int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
      if (HologramPlacementContext.isActive()) {
         World self = (World)(Object)this;
         if (HologramPlacementContext.isActive(self)) {
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
   }

   @Inject(method = "notifyBlocksOfNeighborChange(IIII)V", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipNeighborNotify(int x, int y, int z, int id, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(method = "markBlockNeedsUpdate", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipMarkNeedsUpdate(int x, int y, int z, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(method = "checkIfAABBIsClear", at = @At("HEAD"), cancellable = true)
   private void blueprints$bypassEntityCheck(AABB aabb, CallbackInfoReturnable<Boolean> cir) {
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
   private void blueprints$notifyRealChange(int x, int y, int z, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (!HologramPlacementContext.isActive(self)) {
         HologramRenderer.notifyRealBlockChanged(self, x, y, z);
      }
   }

   @Inject(method = "markBlocksDirty", at = @At("RETURN"))
   private void blueprints$notifyRealRegionChange(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (!HologramPlacementContext.isActive(self)) {
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
