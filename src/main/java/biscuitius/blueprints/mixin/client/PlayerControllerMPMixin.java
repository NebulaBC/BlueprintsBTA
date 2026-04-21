package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.BlueprintsCacheManager;
import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.GhostBlockState;
import biscuitius.blueprints.client.SignTextCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.client.player.controller.PlayerControllerMP;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicSign;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.entity.TileEntitySign;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.net.packet.Packet;
import net.minecraft.core.net.packet.PacketSignUpdate;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public abstract class PlayerControllerMPMixin {
   @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelDestroyBlock(int x, int y, int z, Side side, Player player, CallbackInfoReturnable<Boolean> cir) {
      if (DesignModeState.isActive()) {
         Minecraft minecraft = Minecraft.getMinecraft();
         if (minecraft != null && minecraft.currentWorld != null && GhostBlockState.isTracked(minecraft.currentWorld, x, y, z)) {
            GhostBlockState.revertToServer(minecraft.currentWorld, x, y, z);
            BlueprintsCacheManager.markDirty();
         }

         cir.setReturnValue(false);
      } else {
         Minecraft minecraft = Minecraft.getMinecraft();
         if (minecraft != null
            && minecraft.currentWorld != null
            && !DesignModeState.isPassthroughMode()
            && GhostBlockState.isGhostBlock(minecraft.currentWorld, x, y, z)) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(method = "useOrPlaceItemStackOnTile", at = @At("HEAD"), cancellable = true)
   private void blueprints$fulfillGhostBlock(
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
      if (!DesignModeState.isActive() && !DesignModeState.isPassthroughMode() && world != null) {
         if (!GhostBlockState.isFulfillmentInProgress() && !GhostBlockState.isFulfillableGhost(world, blockX, blockY, blockZ)) {
            int tx = blockX + side.getOffsetX();
            int ty = blockY + side.getOffsetY();
            int tz = blockZ + side.getOffsetZ();
            if (GhostBlockState.isFulfillableGhost(world, tx, ty, tz)) {
               this.blueprints$fulfillAtReplaceable(player, world, itemstack, tx, ty, tz, side, xPlaced, yPlaced, cir);
            } else {
               if (GhostBlockState.isGhostBlock(world, tx, ty, tz)) {
                  cir.setReturnValue(false);
               }
            }
         } else if (!GhostBlockState.isFulfillmentInProgress()) {
            if (itemstack == null) {
               cir.setReturnValue(false);
            } else if (!GhostBlockState.hasRealAdjacentBlock(world, blockX, blockY, blockZ)) {
               cir.setReturnValue(false);
            } else {
               int ghostBlockId = GhostBlockState.getGhostBlockId(world, blockX, blockY, blockZ);
               Block<?> ghostBlock = Blocks.blocksList[ghostBlockId];
               if (ghostBlock == null) {
                  cir.setReturnValue(false);
               } else {
                  int ghostMeta = GhostBlockState.getGhostMetadata(world, blockX, blockY, blockZ);
                  ItemStack[] pickResult = ghostBlock.getBreakResult(world, EnumDropCause.PICK_BLOCK, blockX, blockY, blockZ, ghostMeta, null);
                  if (pickResult != null && pickResult.length != 0 && pickResult[0] != null) {
                     ItemStack pickItem = pickResult[0];
                     if (itemstack.itemID != pickItem.itemID || itemstack.getMetadata() != pickItem.getMetadata()) {
                        cir.setReturnValue(false);
                     } else if (ghostBlock.getLogic() instanceof BlockLogicSign) {
                        this.blueprints$fulfillSign(player, world, itemstack, blockX, blockY, blockZ, ghostMeta, ghostBlock, xPlaced, yPlaced, cir);
                     } else {
                        int ghostId = GhostBlockState.getGhostBlockId(world, blockX, blockY, blockZ);
                        int ghostMetadata = GhostBlockState.getGhostMetadata(world, blockX, blockY, blockZ);
                        GhostBlockState.setPendingFulfillment(blockX, blockY, blockZ, ghostId, ghostMetadata);
                        GhostBlockState.setFulfillmentInProgress(true);
                        GhostBlockState.revertToServer(world, blockX, blockY, blockZ);
                        BlueprintsCacheManager.markDirty();
                     }
                  } else {
                     cir.setReturnValue(false);
                  }
               }
            }
         }
      }
   }

   @Unique
   private void blueprints$fulfillAtReplaceable(
      Player player,
      World world,
      ItemStack itemstack,
      int ghostX,
      int ghostY,
      int ghostZ,
      Side side,
      double xPlaced,
      double yPlaced,
      CallbackInfoReturnable<Boolean> cir
   ) {
      if (itemstack == null) {
         cir.setReturnValue(false);
      } else if (!GhostBlockState.hasRealAdjacentBlock(world, ghostX, ghostY, ghostZ)) {
         cir.setReturnValue(false);
      } else {
         int ghostBlockId = GhostBlockState.getGhostBlockId(world, ghostX, ghostY, ghostZ);
         Block<?> ghostBlock = Blocks.blocksList[ghostBlockId];
         if (ghostBlock == null) {
            cir.setReturnValue(false);
         } else {
            int ghostMeta = GhostBlockState.getGhostMetadata(world, ghostX, ghostY, ghostZ);
            ItemStack[] pickResult = ghostBlock.getBreakResult(world, EnumDropCause.PICK_BLOCK, ghostX, ghostY, ghostZ, ghostMeta, null);
            if (pickResult != null && pickResult.length != 0 && pickResult[0] != null) {
               ItemStack pickItem = pickResult[0];
               if (itemstack.itemID != pickItem.itemID || itemstack.getMetadata() != pickItem.getMetadata()) {
                  cir.setReturnValue(false);
               } else if (ghostBlock.getLogic() instanceof BlockLogicSign) {
                  this.blueprints$fulfillSign(player, world, itemstack, ghostX, ghostY, ghostZ, ghostMeta, ghostBlock, xPlaced, yPlaced, cir);
               } else {
                  int ghostId = GhostBlockState.getGhostBlockId(world, ghostX, ghostY, ghostZ);
                  int ghostMetadata = GhostBlockState.getGhostMetadata(world, ghostX, ghostY, ghostZ);
                  GhostBlockState.setPendingFulfillment(ghostX, ghostY, ghostZ, ghostId, ghostMetadata);
                  GhostBlockState.setFulfillmentInProgress(true);
                  GhostBlockState.revertToServer(world, ghostX, ghostY, ghostZ);
                  BlueprintsCacheManager.markDirty();
                  Minecraft mc = Minecraft.getMinecraft();
                  boolean result = false;
                  if (mc != null && mc.playerController != null) {
                     result = mc.playerController.placeItemStackOnTile(player, world, itemstack, ghostX, ghostY, ghostZ, side, xPlaced, yPlaced);
                  }

                  int[] pending = GhostBlockState.consumePendingFulfillment();
                  if (pending != null) {
                     int px = pending[0];
                     int py = pending[1];
                     int pz = pending[2];
                     int desiredId = pending[3];
                     int desiredMeta = pending[4];
                     int currentId = world.getBlockId(px, py, pz);
                     if (result && currentId == desiredId) {
                        int currentMeta = world.getBlockMetadata(px, py, pz);
                        if (currentMeta != desiredMeta) {
                           GhostBlockState.setBlockNoLighting(world, px, py, pz, desiredId, desiredMeta);
                           world.markBlocksDirty(px, py, pz, px, py, pz);
                        }

                        GhostBlockState.registerRecentFulfillment(px, py, pz, desiredId, desiredMeta);
                        GhostBlockState.trackFulfilled(world, px, py, pz, desiredId, desiredMeta, desiredId, desiredMeta);
                        BlueprintsCacheManager.markDirty();
                     } else {
                        GhostBlockState.restoreGhost(world, px, py, pz, desiredId, desiredMeta);
                        BlueprintsCacheManager.markDirty();
                     }
                  }

                  GhostBlockState.setFulfillmentInProgress(false);
                  cir.setReturnValue(result);
               }
            } else {
               cir.setReturnValue(false);
            }
         }
      }
   }

   @Unique
   private void blueprints$fulfillSign(
      Player player,
      World world,
      ItemStack itemstack,
      int blockX,
      int blockY,
      int blockZ,
      int ghostMeta,
      Block<?> ghostBlock,
      double xPlaced,
      double yPlaced,
      CallbackInfoReturnable<Boolean> cir
   ) {
      BlockLogicSign signLogic = (BlockLogicSign)ghostBlock.getLogic();
      int meta = ghostMeta & 15;
      int supportX;
      int supportY;
      int supportZ;
      Side clickSide;
      if (signLogic.isFreeStanding) {
         supportX = blockX;
         supportY = blockY - 1;
         supportZ = blockZ;
         clickSide = Side.TOP;
      } else {
         switch (meta) {
            case 2:
               supportX = blockX;
               supportY = blockY;
               supportZ = blockZ + 1;
               clickSide = Side.NORTH;
               break;
            case 3:
               supportX = blockX;
               supportY = blockY;
               supportZ = blockZ - 1;
               clickSide = Side.SOUTH;
               break;
            case 4:
               supportX = blockX + 1;
               supportY = blockY;
               supportZ = blockZ;
               clickSide = Side.WEST;
               break;
            case 5:
               supportX = blockX - 1;
               supportY = blockY;
               supportZ = blockZ;
               clickSide = Side.EAST;
               break;
            default:
               cir.setReturnValue(false);
               return;
         }
      }

      if (!world.getBlockMaterial(supportX, supportY, supportZ).isSolid()) {
         cir.setReturnValue(false);
      } else {
         int ghostId = GhostBlockState.getGhostBlockId(world, blockX, blockY, blockZ);
         int ghostMetadata = GhostBlockState.getGhostMetadata(world, blockX, blockY, blockZ);
         SignTextCache.SignData cachedText = SignTextCache.get(blockX, blockY, blockZ);
         GhostBlockState.setPendingFulfillment(blockX, blockY, blockZ, ghostId, ghostMetadata);
         GhostBlockState.setFulfillmentInProgress(true);
         GhostBlockState.revertToServer(world, blockX, blockY, blockZ);
         BlueprintsCacheManager.markDirty();
         boolean hadCachedText = cachedText != null;
         if (hadCachedText) {
            SignTextCache.setSuppressSignEditor(true);
         }

         Minecraft mc = Minecraft.getMinecraft();
         boolean result = false;
         if (mc != null && mc.playerController != null) {
            result = mc.playerController.placeItemStackOnTile(player, world, itemstack, supportX, supportY, supportZ, clickSide, xPlaced, yPlaced);
         }

         if (hadCachedText) {
            SignTextCache.setSuppressSignEditor(false);
         }

         int[] pending = GhostBlockState.consumePendingFulfillment();
         if (pending != null) {
            int px = pending[0];
            int py = pending[1];
            int pz = pending[2];
            int desiredId = pending[3];
            int desiredMeta = pending[4];
            int currentId = world.getBlockId(px, py, pz);
            if (result && currentId == desiredId) {
               int currentMeta = world.getBlockMetadata(px, py, pz);
               if (currentMeta != desiredMeta) {
                  GhostBlockState.setBlockNoLighting(world, px, py, pz, desiredId, desiredMeta);
                  world.markBlocksDirty(px, py, pz, px, py, pz);
               }

               GhostBlockState.registerRecentFulfillment(px, py, pz, desiredId, desiredMeta);
               GhostBlockState.trackFulfilled(world, px, py, pz, desiredId, desiredMeta, desiredId, desiredMeta);
               BlueprintsCacheManager.markDirty();
               if (hadCachedText) {
                  this.blueprints$applySignText(world, mc, px, py, pz, cachedText);
               }
            } else {
               GhostBlockState.restoreGhost(world, px, py, pz, desiredId, desiredMeta);
               if (hadCachedText) {
                  SignTextCache.put(px, py, pz, cachedText.text, cachedText.picture, cachedText.color);
               }

               BlueprintsCacheManager.markDirty();
            }
         }

         GhostBlockState.setFulfillmentInProgress(false);
         cir.setReturnValue(result);
      }
   }

   @Unique
   private void blueprints$applySignText(World world, Minecraft mc, int x, int y, int z, SignTextCache.SignData data) {
      TileEntity rawTe = world.getTileEntity(x, y, z);
      if (rawTe instanceof TileEntitySign) {
         TileEntitySign te = (TileEntitySign)rawTe;
         SignTextCache.applySignData(te, data);
         if (mc != null && mc.getSendQueue() != null) {
            int pictureId = te.getPicture() != null ? te.getPicture().getId() : 0;
            int colorId = te.getColor() != null ? te.getColor().id : 15;
            mc.getSendQueue().addToSendQueue(new PacketSignUpdate(x, y, z, te.signText, pictureId, colorId));
         }
      }
   }

   @ModifyVariable(method = "useOrPlaceItemStackOnTile", at = @At("HEAD"), argsOnly = true, ordinal = 0)
   private ItemStack blueprints$shuffleItemStack(ItemStack itemstack) {
      if (DesignModeState.isActive() && DesignModeState.isShuffleEnabled()) {
         PlayerLocal dp = DesignModeState.getDesignPlayer();
         return dp == null ? itemstack : DesignModeState.shuffleAndGetItem(dp.inventory);
      } else {
         return itemstack;
      }
   }

   @Inject(method = "useOrPlaceItemStackOnTile", at = @At("HEAD"))
   private void blueprints$captureGhostUseOrPlace(
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
   private void blueprints$syncGhostUseOrPlace(
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
      } else {
         int[] pending = GhostBlockState.consumePendingFulfillment();
         if (pending != null && world != null) {
            int px = pending[0];
            int py = pending[1];
            int pz = pending[2];
            int desiredId = pending[3];
            int desiredMeta = pending[4];
            boolean placed = Boolean.TRUE.equals(cir.getReturnValue());
            int currentId = world.getBlockId(px, py, pz);
            if (placed && currentId == desiredId) {
               int currentMeta = world.getBlockMetadata(px, py, pz);
               if (currentMeta != desiredMeta) {
                  GhostBlockState.setBlockNoLighting(world, px, py, pz, desiredId, desiredMeta);
                  world.markBlocksDirty(px, py, pz, px, py, pz);
               }

               GhostBlockState.registerRecentFulfillment(px, py, pz, desiredId, desiredMeta);
               GhostBlockState.trackFulfilled(world, px, py, pz, desiredId, desiredMeta, desiredId, desiredMeta);
               BlueprintsCacheManager.markDirty();
            } else {
               GhostBlockState.restoreGhost(world, px, py, pz, desiredId, desiredMeta);
               BlueprintsCacheManager.markDirty();
            }
         }

         GhostBlockState.setFulfillmentInProgress(false);
      }

      GhostBlockState.setSuppressLighting(false);
   }

   @Inject(method = "placeItemStackOnTile", at = @At("HEAD"), cancellable = true)
   private void blueprints$captureGhostPlace(
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

      if (!DesignModeState.isActive() && !DesignModeState.isPassthroughMode() && !GhostBlockState.isFulfillmentInProgress() && world != null) {
         int tx = blockX + side.getOffsetX();
         int ty = blockY + side.getOffsetY();
         int tz = blockZ + side.getOffsetZ();
         if (GhostBlockState.isGhostBlock(world, tx, ty, tz) || GhostBlockState.isGhostBlock(world, blockX, blockY, blockZ)) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(method = "placeItemStackOnTile", at = @At("RETURN"))
   private void blueprints$syncGhostPlace(
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

   @Redirect(
      method = {"destroyBlock", "syncCurrentPlayItem"},
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeControllerPlayer(Minecraft minecraft) {
      return DesignModeState.isActive() ? DesignModeState.getControlPlayer(minecraft) : minecraft.thePlayer;
   }

   @Redirect(
      method = {
            "hitBlock",
            "sendStartDigPacket",
            "sendDestroyBlockPacket",
            "syncCurrentPlayItem",
            "useOrPlaceItemStackOnTile",
            "useItemStackOnNothing",
            "placeItemStackOnTile",
            "attack",
            "interact",
            "handleInventoryMouseClick",
            "setPaintingType",
            "dropCurrentItem",
            "pickBlock",
            "sendSpecialVehiclePacket"
      },
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/net/handler/PacketHandlerClient;addToSendQueue(Lnet/minecraft/core/net/packet/Packet;)V")
   )
   private void blueprints$skipControllerPacket(PacketHandlerClient handler, Packet packet) {
      if (!DesignModeState.isActive()) {
         handler.addToSendQueue(packet);
      }
   }

   @Inject(method = "useItemStackOnNothing", at = @At("HEAD"), cancellable = true)
   private void blueprints$cancelMPUseOnNothing(Player player, World world, ItemStack itemstack, CallbackInfoReturnable<Boolean> cir) {
      if (DesignModeState.isActive()) {
         cir.setReturnValue(false);
      }
   }
}
