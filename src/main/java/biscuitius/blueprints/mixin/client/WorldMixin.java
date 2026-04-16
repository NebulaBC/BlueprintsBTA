package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.GhostBlockState;
import java.util.List;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
         if (GhostBlockState.isGhostBlock(world, x, y, z) && !GhostBlockState.isRenderingGhostBlock()) {
            int originalId = GhostBlockState.getServerBlockId(world, x, y, z);
            Block<?> block = Blocks.blocksList[originalId];
            cir.setReturnValue(block != null && block.isSolidRender());
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
   private void blueprints$preventReplaceInDesignMode(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
      if (DesignModeState.isActive()) {
         int id = ((World)(Object)this).getBlockId(x, y, z);
         if (id != 0) {
            cir.setReturnValue(false);
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

   @Inject(method = "notifyBlocksOfNeighborChange", at = @At("HEAD"), cancellable = true)
   private void blueprints$suppressNeighborNotifications(int x, int y, int z, int blockId, CallbackInfo ci) {
      if (GhostBlockState.isSuppressingLighting()) {
         ci.cancel();
      }
   }
}
