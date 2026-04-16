package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.BlueprintsCacheManager;
import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.player.controller.PlayerController;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerController.class)
public abstract class PlayerControllerMixin {
   @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelRealBlockStart(int x, int y, int z, Side side, double xHit, double yHit, boolean repeat, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc != null && mc.currentWorld != null && GhostBlockState.isTracked(mc.currentWorld, x, y, z)) {
            int blockId = mc.currentWorld.getBlockId(x, y, z);
            if (blockId > 0) {
               mc.currentWorld.playBlockEvent(2001, x, y, z, blockId);
            }

            GhostBlockState.revertToServer(mc.currentWorld, x, y, z);
            BlueprintsCacheManager.markDirty();
         }

         ci.cancel();
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc != null && mc.currentWorld != null && GhostBlockState.isGhostBlock(mc.currentWorld, x, y, z)) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelRealBlockContinue(int x, int y, int z, Side side, double xHit, double yHit, CallbackInfo ci) {
      if (DesignModeState.isActive()) {
         ci.cancel();
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc != null && mc.currentWorld != null && GhostBlockState.isGhostBlock(mc.currentWorld, x, y, z)) {
            ci.cancel();
         }
      }
   }

   @Redirect(
      method = "swingItem",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeSwingItemPlayer(Minecraft minecraft) {
      return DesignModeState.getControlPlayer(minecraft);
   }

   @Redirect(
      method = "getBlockReachDistance",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeBlockReach(Minecraft minecraft) {
      return DesignModeState.getControlPlayer(minecraft);
   }

   @Redirect(
      method = "getEntityReachDistance",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeEntityReach(Minecraft minecraft) {
      return DesignModeState.getControlPlayer(minecraft);
   }

   @Redirect(
      method = "useOrPlaceItemStackOnTile",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/block/Block;onBlockRightClicked(Lnet/minecraft/core/world/World;IIILnet/minecraft/core/entity/player/Player;Lnet/minecraft/core/util/helper/Side;DD)Z"
      )
   )
   private boolean blueprints$skipRealBlockInteraction(Block<?> block, World world, int x, int y, int z, Player player, Side side, double xHit, double yHit) {
      return !DesignModeState.isActive() && block.onBlockRightClicked(world, x, y, z, player, side, xHit, yHit);
   }

   @Redirect(
      method = "useOrPlaceItemStackOnTile",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/block/entity/TileEntity;canBeCarried(Lnet/minecraft/core/world/World;Lnet/minecraft/core/entity/Entity;)Z"
      )
   )
   private boolean blueprints$skipTileEntityCarry(TileEntity tileEntity, World world, Entity entity) {
      return !DesignModeState.isActive() && tileEntity.canBeCarried(world, entity);
   }

   @Inject(method = "useOrPlaceItemStackOnTile", at = @At("HEAD"))
   private void blueprints$captureGhostUseOrPlaceBase(
      Player player,
      World world,
      ItemStack itemstack,
      int blockX,
      int blockY,
      int blockZ,
      Side side,
      double xPlaced,
      double yPlaced,
      CallbackInfoReturnable<Boolean> cir
   ) {
      Minecraft minecraft = Minecraft.getMinecraft();
      if (DesignModeState.isActive() && minecraft != null && player == DesignModeState.getControlPlayer(minecraft)) {
         GhostBlockState.setSuppressLighting(true);
      }
   }

   @Inject(method = "useOrPlaceItemStackOnTile", at = @At("RETURN"))
   private void blueprints$syncGhostUseOrPlaceBase(
      Player player,
      World world,
      ItemStack itemstack,
      int blockX,
      int blockY,
      int blockZ,
      Side side,
      double xPlaced,
      double yPlaced,
      CallbackInfoReturnable<Boolean> cir
   ) {
      if (DesignModeState.isActive()) {
         BlueprintsCacheManager.markDirty();
      }

      GhostBlockState.setSuppressLighting(false);
   }

   @Inject(method = "placeItemStackOnTile", at = @At("HEAD"))
   private void blueprints$captureGhostPlaceBase(
      Player player,
      World world,
      ItemStack itemstack,
      int blockX,
      int blockY,
      int blockZ,
      Side side,
      double xPlaced,
      double yPlaced,
      CallbackInfoReturnable<Boolean> cir
   ) {
      Minecraft minecraft = Minecraft.getMinecraft();
      if (DesignModeState.isActive() && minecraft != null && player == DesignModeState.getControlPlayer(minecraft)) {
         GhostBlockState.setSuppressLighting(true);
      }
   }

   @Inject(method = "placeItemStackOnTile", at = @At("RETURN"))
   private void blueprints$syncGhostPlaceBase(
      Player player,
      World world,
      ItemStack itemstack,
      int blockX,
      int blockY,
      int blockZ,
      Side side,
      double xPlaced,
      double yPlaced,
      CallbackInfoReturnable<Boolean> cir
   ) {
      if (DesignModeState.isActive()) {
         BlueprintsCacheManager.markDirty();
      }

      GhostBlockState.setSuppressLighting(false);
   }
}
