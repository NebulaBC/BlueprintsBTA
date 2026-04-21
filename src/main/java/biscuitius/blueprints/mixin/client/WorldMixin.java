package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.GhostBlockState;
import java.util.List;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.block.tag.BlockTags;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.Vec3;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldMixin {
   @Inject(method = "getCubes", at = @At("HEAD"))
   private void blueprints$patchBeforeCubes(Entity entity, AABB aabb, CallbackInfoReturnable<List> cir) {
      if (!DesignModeState.isActive() || entity != DesignModeState.getDesignPlayer() || DesignModeState.isDesignPlayerNoclipOverride()) {
         int minX = MathHelper.floor(aabb.minX) - 1;
         int maxX = MathHelper.floor(aabb.maxX + 1.0);
         int minY = MathHelper.floor(aabb.minY) - 1;
         int maxY = MathHelper.floor(aabb.maxY + 1.0);
         int minZ = MathHelper.floor(aabb.minZ) - 1;
         int maxZ = MathHelper.floor(aabb.maxZ + 1.0);
         GhostBlockState.beginPhysicsPatchScoped((World)(Object)this, minX, minY, minZ, maxX, maxY, maxZ);
      }
   }

   @Inject(method = "getCubes", at = @At("RETURN"))
   private void blueprints$unpatchAfterCubes(Entity entity, AABB aabb, CallbackInfoReturnable<List> cir) {
      if (!DesignModeState.isActive() || entity != DesignModeState.getDesignPlayer() || DesignModeState.isDesignPlayerNoclipOverride()) {
         GhostBlockState.endPhysicsPatch((World)(Object)this);
      }
   }

   @Inject(method = "isBlockNormalCube", at = @At("HEAD"), cancellable = true)
   private void blueprints$ghostNormalCubeCheck(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
      if (!DesignModeState.isActive()) {
         int result = GhostBlockState.isServerBlockNormalCube((World)(Object)this, x, y, z);
         if (result >= 0) {
            cir.setReturnValue(result == 1);
         }
      }
   }

   @Inject(method = "isBlockOpaqueCube", at = @At("HEAD"), cancellable = true)
   private void blueprints$ghostNotOpaque(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
      World world = (World)(Object)this;
      if (GhostBlockState.hasSectionGhosts(world, x, y, z)) {
         if (GhostBlockState.isGhostBlock(world, x, y, z)) {
            if (GhostBlockState.isRenderingGhostBlock() && !GhostBlockState.isRenderingWrongBlock()) {
               int ghostId = world.getBlockId(x, y, z);
               Block<?> ghostBlock = Blocks.blocksList[ghostId];
               cir.setReturnValue(ghostBlock != null && ghostBlock.isSolidRender());
            } else {
               int ghostId = world.getBlockId(x, y, z);
               Block<?> ghostBlock = Blocks.blocksList[ghostId];
               boolean ghostOpaque = ghostBlock != null && ghostBlock.isSolidRender();
               int serverId = GhostBlockState.getServerBlockId(world, x, y, z);
               Block<?> serverBlock = serverId > 0 ? Blocks.blocksList[serverId] : null;
               boolean serverOpaque = serverBlock != null && serverBlock.isSolidRender();
               cir.setReturnValue(ghostOpaque && serverOpaque);
            }
         }
      }
   }

   @Inject(method = "checkIfAABBIsClear", at = @At("HEAD"), cancellable = true)
   private void blueprints$allowPlaceInsideEntities(AABB axisalignedbb, CallbackInfoReturnable<Boolean> cir) {
      if (DesignModeState.isActive()) {
         cir.setReturnValue(true);
      }
   }

   @Inject(method = "canPlaceInsideBlock", at = @At("HEAD"), cancellable = true)
   private void blueprints$ghostCanPlaceInsideBlock(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
      if (DesignModeState.isActive()) {
         World world = (World)(Object)this;
         if (GhostBlockState.isGhostBlock(world, x, y, z)) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(method = "canBlockBePlacedAt", at = @At("HEAD"), cancellable = true)
   private void blueprints$ghostCanBlockBePlacedAt(int blockId, int x, int y, int z, boolean flag, Side side, CallbackInfoReturnable<Boolean> cir) {
      World world = (World)(Object)this;
      if (GhostBlockState.isGhostBlock(world, x, y, z)) {
         int serverId = GhostBlockState.getServerBlockId(world, x, y, z);
         Block<?> serverBlock = serverId > 0 ? Blocks.blocksList[serverId] : null;
         if (serverBlock != null && serverBlock.hasTag(BlockTags.PLACE_OVERWRITES)) {
            serverBlock = null;
         }

         if (!DesignModeState.isActive()) {
            cir.setReturnValue(blockId > 0 && serverBlock == null);
         } else {
            Block<?> newBlock = Blocks.blocksList[blockId];
            if (newBlock == null) {
               cir.setReturnValue(false);
            } else {
               AABB bb = newBlock.getCollisionBoundingBoxFromPool(world, x, y, z);
               if (flag) {
                  bb = null;
               }

               if (bb != null && !world.checkIfAABBIsClear(bb)) {
                  cir.setReturnValue(false);
               } else {
                  cir.setReturnValue(blockId > 0 && serverBlock == null && newBlock.canPlaceBlockOnSide(world, x, y, z, side));
               }
            }
         }
      }
   }

   @Inject(method = "setBlockAndMetadata", at = @At("HEAD"), cancellable = true)
   private void blueprints$ghostSetBlockAndMeta(int x, int y, int z, int id, int meta, CallbackInfoReturnable<Boolean> cir) {
      if (GhostBlockState.isSuppressingLighting()) {
         World world = (World)(Object)this;
         GhostBlockState.capture(world, x, y, z);
         GhostBlockState.setBlockNoLighting(world, x, y, z, id, meta);
         GhostBlockState.sync(world, x, y, z);
         cir.setReturnValue(true);
      } else {
         World world = (World)(Object)this;
         if (GhostBlockState.isTracked(world, x, y, z)) {
            GhostBlockState.handleTrackedBlockChange(world, x, y, z, id, meta);
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
   private void blueprints$ghostSetBlock(int x, int y, int z, int id, CallbackInfoReturnable<Boolean> cir) {
      if (GhostBlockState.isSuppressingLighting()) {
         World world = (World)(Object)this;
         GhostBlockState.capture(world, x, y, z);
         GhostBlockState.setBlockNoLighting(world, x, y, z, id, 0);
         GhostBlockState.sync(world, x, y, z);
         cir.setReturnValue(true);
      } else {
         World world = (World)(Object)this;
         if (GhostBlockState.isTracked(world, x, y, z)) {
            GhostBlockState.handleTrackedBlockChange(world, x, y, z, id, 0);
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "setBlockMetadata", at = @At("HEAD"), cancellable = true)
   private void blueprints$ghostSetBlockMeta(int x, int y, int z, int meta, CallbackInfoReturnable<Boolean> cir) {
      if (GhostBlockState.isSuppressingLighting()) {
         World world = (World)(Object)this;
         GhostBlockState.capture(world, x, y, z);
         int currentId = world.getBlockId(x, y, z);
         GhostBlockState.setBlockNoLighting(world, x, y, z, currentId, meta);
         GhostBlockState.sync(world, x, y, z);
         cir.setReturnValue(true);
      }
   }

   @Inject(method = "setBlockAndMetadataRaw", at = @At("HEAD"), cancellable = true)
   private void blueprints$protectGhostSetBlockAndMetaRaw(int x, int y, int z, int id, int meta, CallbackInfoReturnable<Boolean> cir) {
      if (!GhostBlockState.isSuppressingLighting()) {
         World world = (World)(Object)this;
         if (GhostBlockState.isTracked(world, x, y, z)) {
            GhostBlockState.handleTrackedBlockChange(world, x, y, z, id, meta);
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "setBlockWithNotify", at = @At("HEAD"), cancellable = true)
   private void blueprints$protectGhostSetBlockWithNotify(int x, int y, int z, int id, CallbackInfoReturnable<Boolean> cir) {
      if (!GhostBlockState.isSuppressingLighting()) {
         World world = (World)(Object)this;
         if (GhostBlockState.isTracked(world, x, y, z)) {
            GhostBlockState.handleTrackedBlockChange(world, x, y, z, id, 0);
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "setBlockAndMetadataWithNotify", at = @At("HEAD"), cancellable = true)
   private void blueprints$protectGhostSetBlockAndMetaWithNotify(int x, int y, int z, int id, int meta, CallbackInfoReturnable<Boolean> cir) {
      if (!GhostBlockState.isSuppressingLighting()) {
         World world = (World)(Object)this;
         if (GhostBlockState.isTracked(world, x, y, z)) {
            GhostBlockState.handleTrackedBlockChange(world, x, y, z, id, meta);
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "notifyBlocksOfNeighborChange", at = @At("HEAD"), cancellable = true)
   private void blueprints$suppressNeighborNotifications(int x, int y, int z, int blockId, CallbackInfo ci) {
      if (GhostBlockState.isSuppressingLighting()) {
         ci.cancel();
      } else {
         World world = (World)(Object)this;
         if (GhostBlockState.isGhostBlock(world, x, y, z)) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "notifyBlockOfNeighborChange", at = @At("HEAD"), cancellable = true)
   private void blueprints$suppressNeighborToGhost(int x, int y, int z, int blockId, CallbackInfo ci) {
      World world = (World)(Object)this;
      if (GhostBlockState.isGhostBlock(world, x, y, z)) {
         ci.cancel();
      }
   }

   @Inject(method = "isMaterialInBB", at = @At("HEAD"))
   private void blueprints$patchBeforeMaterialCheck(AABB aabb, Material[] materials, CallbackInfoReturnable<Boolean> cir) {
      World world = (World)(Object)this;
      int minX = MathHelper.floor(aabb.minX);
      int maxX = MathHelper.floor(aabb.maxX + 1.0);
      int minY = MathHelper.floor(aabb.minY);
      int maxY = MathHelper.floor(aabb.maxY + 1.0);
      int minZ = MathHelper.floor(aabb.minZ);
      int maxZ = MathHelper.floor(aabb.maxZ + 1.0);
      GhostBlockState.beginPhysicsPatchScoped(world, minX, minY, minZ, maxX, maxY, maxZ);
   }

   @Inject(method = "isMaterialInBB", at = @At("RETURN"))
   private void blueprints$unpatchAfterMaterialCheck(AABB aabb, Material[] materials, CallbackInfoReturnable<Boolean> cir) {
      GhostBlockState.endPhysicsPatch((World)(Object)this);
   }

   @Inject(method = "entityJoinedWorld", at = @At("HEAD"), cancellable = true)
   private void blueprints$suppressEntitySpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (GhostBlockState.isSuppressingEntitySpawn()) {
         cir.setReturnValue(false);
      }
   }

   @Redirect(
      method = "checkBlockCollisionBetweenPoints(Lnet/minecraft/core/util/phys/Vec3;Lnet/minecraft/core/util/phys/Vec3;ZZZ)Lnet/minecraft/core/util/phys/HitResult;",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/block/Block;collisionRayTrace(Lnet/minecraft/core/world/World;IIILnet/minecraft/core/util/phys/Vec3;Lnet/minecraft/core/util/phys/Vec3;Z)Lnet/minecraft/core/util/phys/HitResult;"
      ),
      expect = 2
   )
   private HitResult blueprints$skipGhostRayTrace(Block<?> instance, World world, int x, int y, int z, Vec3 start, Vec3 end, boolean useSelectorBoxes) {
      return !DesignModeState.isActive() && DesignModeState.isPassthroughMode() && GhostBlockState.isGhostBlock(world, x, y, z)
         ? null
         : instance.collisionRayTrace(world, x, y, z, start, end, useSelectorBoxes);
   }
}
