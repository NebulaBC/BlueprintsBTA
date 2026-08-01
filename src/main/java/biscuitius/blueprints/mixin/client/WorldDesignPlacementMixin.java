package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.HologramPlacementContext;
import biscuitius.blueprints.client.hologram.HologramRenderer;
import biscuitius.blueprints.client.hologram.HologramSimulationContext;
import biscuitius.blueprints.client.hologram.HologramStore;
import java.util.Optional;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.enums.EnumBlockSoundEffectType;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.Direction;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePosc;
import org.joml.primitives.AABBdc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldDesignPlacementMixin {
   @Unique
   private static boolean blueprints$realReadFallthrough;

   @Inject(method = "setBlockTypeDataRaw", at = @At("HEAD"), cancellable = true)
   @Unique
   private void blueprints$captureSetTypeDataRaw(TilePosc pos, Block<?> block, int data, CallbackInfoReturnable<Boolean> cir) {
      this.blueprints$writeIdMeta(pos, block == null ? 0 : block.id(), data, cir);
   }

   @Inject(method = "setBlockTypeData", at = @At("HEAD"), cancellable = true)
   @Unique
   private void blueprints$captureSetTypeData(TilePosc pos, Block<?> block, int data, CallbackInfoReturnable<Boolean> cir) {
      this.blueprints$writeIdMeta(pos, block == null ? 0 : block.id(), data, cir);
   }

   @Inject(method = "setBlockType", at = @At("HEAD"), cancellable = true)
   @Unique
   private void blueprints$captureSetType(TilePosc pos, Block<?> block, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self)) {
         int id = block == null ? 0 : block.id();
         this.blueprints$writeSimulationType(pos.x(), pos.y(), pos.z(), id, null, cir);
      } else if (HologramPlacementContext.isActive(self)) {
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

            HologramPlacementContext.recordTouch(x, y, z);
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "setBlockData", at = @At("HEAD"), cancellable = true)
   @Unique
   private void blueprints$captureSetData(TilePosc pos, int data, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self)) {
         HologramBlock current = HologramStore.get(self, pos.x(), pos.y(), pos.z());
         if (current != null) {
            HologramStore.putRaw(self, pos.x(), pos.y(), pos.z(), current.withMetadata(data));
         }

         cir.setReturnValue(true);
      } else if (HologramPlacementContext.isActive(self)) {
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

            HologramPlacementContext.recordTouch(x, y, z);
            cir.setReturnValue(true);
         }
      }
   }

   @Unique
   private void blueprints$writeIdMeta(TilePosc pos, int id, int meta, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self)) {
         this.blueprints$writeSimulationType(pos.x(), pos.y(), pos.z(), id, meta, cir);
      } else if (HologramPlacementContext.isActive(self)) {
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

            HologramPlacementContext.recordTouch(x, y, z);
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "setBlockTypeDataEntity", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureSimulationEntityWrite(TilePosc pos, Block<?> block, int data, TileEntity entity, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self)) {
         int x = pos.x();
         int y = pos.y();
         int z = pos.z();
         if (block != null && block != Blocks.AIR) {
            HologramStore.putRaw(self, x, y, z, new HologramBlock(block.id(), data));
         } else {
            HologramStore.remove(self, x, y, z);
         }

         cir.setReturnValue(true);
      }
   }

   @Inject(method = "getBlockType", at = @At("HEAD"), cancellable = true)
   private void blueprints$readBlockType(TilePosc pos, CallbackInfoReturnable<Block<?>> cir) {
      World self = (World)(Object)this;
      if (!blueprints$realReadFallthrough) {
         if (HologramSimulationContext.isActive(self)) {
            HologramBlock h = HologramStore.get(self, pos.x(), pos.y(), pos.z());
            if (h != null) {
               cir.setReturnValue(h.blockId == 0 ? Blocks.AIR : Blocks.getBlock(h.blockId));
            } else {
               Block<?> conductor = this.blueprints$realConductor(self, pos);
               cir.setReturnValue(conductor != null ? conductor : Blocks.AIR);
            }
         } else if (HologramPlacementContext.isActive(self)) {
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
   }

   @Inject(method = "getBlockData", at = @At("HEAD"), cancellable = true)
   private void blueprints$readBlockData(TilePosc pos, CallbackInfoReturnable<Integer> cir) {
      World self = (World)(Object)this;
      if (!blueprints$realReadFallthrough) {
         if (HologramSimulationContext.isActive(self)) {
            HologramBlock h = HologramStore.get(self, pos.x(), pos.y(), pos.z());
            if (h != null) {
               cir.setReturnValue(h.metadata);
            } else {
               if (this.blueprints$realConductor(self, pos) != null) {
                  cir.setReturnValue(this.blueprints$readRealBlockData(self, pos));
               } else {
                  cir.setReturnValue(0);
               }
            }
         } else if (HologramPlacementContext.isActive(self)) {
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
   }

   @Unique
   private Block<?> blueprints$realConductor(World self, TilePosc pos) {
      Block<?> real = this.blueprints$readRealBlockType(self, pos);
      if (real != null && real != Blocks.AIR) {
         try {
            if (real.isSignalSource()) {
               return null;
            } else {
               return real.getMaterial().isSolidBlocking() && real.isSolidRender() ? real : null;
            }
         } catch (Throwable ignored) {
            return null;
         }
      } else {
         return null;
      }
   }

   @Unique
   private Block<?> blueprints$readRealBlockType(World self, TilePosc pos) {
      boolean prev = blueprints$realReadFallthrough;
      blueprints$realReadFallthrough = true;

      try {
         return self.getBlockType(pos);
      } finally {
         blueprints$realReadFallthrough = prev;
      }
   }

   @Unique
   private int blueprints$readRealBlockData(World self, TilePosc pos) {
      boolean prev = blueprints$realReadFallthrough;
      blueprints$realReadFallthrough = true;

      try {
         return self.getBlockData(pos);
      } finally {
         blueprints$realReadFallthrough = prev;
      }
   }

   @Inject(
      method = "setTileEntity(Lnet/minecraft/core/world/pos/TilePosc;Lnet/minecraft/core/block/entity/TileEntity;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void blueprints$suppressTileEntityWrite(TilePosc pos, TileEntity tileEntity, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self) || HologramSimulationContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(method = "getTileEntity(Lnet/minecraft/core/world/pos/TilePosc;)Lnet/minecraft/core/block/entity/TileEntity;", at = @At("HEAD"), cancellable = true)
   private void blueprints$readSimulationTileEntity(TilePosc pos, CallbackInfoReturnable<TileEntity> cir) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self)) {
         cir.setReturnValue(null);
      }
   }

   @Inject(method = "removeTileEntity(Lnet/minecraft/core/world/pos/TilePosc;)V", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipSimulationTileEntityRemove(TilePosc pos, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(method = "notifyBlockChange(Lnet/minecraft/core/world/pos/TilePosc;Lnet/minecraft/core/block/Block;)V", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipBlockChange(TilePosc pos, Block<?> block, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self) || HologramPlacementContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(
      method = "notifyBlocksOfNeighborChange(Lnet/minecraft/core/world/pos/TilePosc;Lnet/minecraft/core/block/Block;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void blueprints$skipNeighborNotify(TilePosc pos, Block<?> block, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self) || HologramPlacementContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(
      method = "notifyBlocksInRadiusOfNeighborChange(ILnet/minecraft/core/world/pos/TilePosc;Lnet/minecraft/core/block/Block;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void blueprints$skipRadiusNotify(int radius, TilePosc pos, Block<?> block, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self) || HologramPlacementContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(
      method = "notifyShellBlocksInRadiusOfNeighborChange(ILnet/minecraft/core/world/pos/TilePosc;Lnet/minecraft/core/block/Block;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void blueprints$skipShellNotify(int radius, TilePosc pos, Block<?> block, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self) || HologramPlacementContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(
      method = "notifyBlocksInCapsuleOfNeighborChange(Lnet/minecraft/core/util/helper/Direction;Lnet/minecraft/core/world/pos/TilePosc;Lnet/minecraft/core/block/Block;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void blueprints$skipCapsuleNotify(Direction direction, TilePosc pos, Block<?> block, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self) || HologramPlacementContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(method = "markBlockNeedsUpdate(Lnet/minecraft/core/world/pos/TilePosc;)V", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipMarkNeedsUpdate(TilePosc pos, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self) || HologramSimulationContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(method = "scheduleBlockUpdate(Lnet/minecraft/core/world/pos/TilePosc;Lnet/minecraft/core/block/Block;J)V", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureScheduledUpdate(TilePosc pos, Block<?> block, long delay, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self) || HologramSimulationContext.isActive(self)) {
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

   @Inject(
      method = "playBlockSoundEffect(Lnet/minecraft/core/entity/Entity;DDDLnet/minecraft/core/block/Block;Lnet/minecraft/core/enums/EnumBlockSoundEffectType;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void blueprints$suppressDryRunSound(Entity player, double x, double y, double z, Block<?> block, EnumBlockSoundEffectType type, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isDryRun(self)) {
         ci.cancel();
      }
   }

   @Inject(method = "hasSignal(Lnet/minecraft/core/world/pos/TilePosc;Lnet/minecraft/core/util/helper/Side;)Z", at = @At("HEAD"), cancellable = true)
   private void blueprints$redirectHasSignal(TilePosc pos, Side side, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "hasDirectSignal(Lnet/minecraft/core/world/pos/TilePosc;)Z", at = @At("HEAD"), cancellable = true)
   private void blueprints$redirectHasDirectSignal(TilePosc pos, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "hasNeighborSignal(Lnet/minecraft/core/world/pos/TilePosc;)Z", at = @At("HEAD"), cancellable = true)
   private void blueprints$redirectHasNeighborSignal(TilePosc pos, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "playBlockEvent(Lnet/minecraft/core/world/pos/TilePosc;II)V", at = @At("HEAD"), cancellable = true)
   private void blueprints$redirectPlayBlockEvent(TilePosc pos, int id, int data, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(method = "triggerEvent(Lnet/minecraft/core/world/pos/TilePosc;II)V", at = @At("HEAD"), cancellable = true)
   private void blueprints$redirectTriggerEvent(TilePosc pos, int index, int data, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (HologramSimulationContext.isActive(self)) {
         ci.cancel();
      }
   }

   @Inject(method = "entityJoinedWorld(Lnet/minecraft/core/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
   private void blueprints$suppressSimulationEntities(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self) || HologramSimulationContext.isActive(self)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = "dropItem(Lnet/minecraft/core/world/pos/TilePosc;Lnet/minecraft/core/item/ItemStack;)Lnet/minecraft/core/entity/EntityItem;",
      at = @At("HEAD"),
      cancellable = true
   )
   private void blueprints$suppressContextItemDrops(TilePosc pos, ItemStack stack, CallbackInfoReturnable<EntityItem> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self) || HologramSimulationContext.isActive(self)) {
         cir.setReturnValue(new EntityItem(self, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5, stack == null ? null : stack.copy()));
      }
   }

   @Inject(method = "dropItem(DDDLnet/minecraft/core/item/ItemStack;D)Lnet/minecraft/core/entity/EntityItem;", at = @At("HEAD"), cancellable = true)
   private void blueprints$suppressContextItemDropsDouble(double x, double y, double z, ItemStack stack, double radius, CallbackInfoReturnable<EntityItem> cir) {
      World self = (World)(Object)this;
      if (HologramPlacementContext.isActive(self) || HologramSimulationContext.isActive(self)) {
         cir.setReturnValue(new EntityItem(self, x, y, z, stack == null ? null : stack.copy()));
      }
   }

   @Unique
   private void blueprints$writeSimulationType(int x, int y, int z, int id, Integer explicitMeta, CallbackInfoReturnable<Boolean> cir) {
      World self = (World)(Object)this;
      if (id == 0) {
         HologramStore.remove(self, x, y, z);
      } else {
         int meta = explicitMeta != null ? explicitMeta : Optional.ofNullable(HologramStore.get(self, x, y, z)).map(h -> h.metadata).orElse(0);
         HologramStore.putRaw(self, x, y, z, new HologramBlock(id, meta));
      }

      cir.setReturnValue(true);
   }

   @Inject(method = "markBlockNeedsUpdate(Lnet/minecraft/core/world/pos/TilePosc;)V", at = @At("RETURN"))
   private void blueprints$notifyRealChange(TilePosc pos, CallbackInfo ci) {
      World self = (World)(Object)this;
      if (!HologramPlacementContext.isActive(self)) {
         HologramRenderer.notifyRealBlockChanged(self, pos.x(), pos.y(), pos.z());
      }
   }

   @Inject(method = "markBlocksDirty(Lnet/minecraft/core/world/pos/TilePosc;Lnet/minecraft/core/world/pos/TilePosc;)V", at = @At("RETURN"))
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
