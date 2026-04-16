package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.GhostBlockState;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.client.world.WorldClientMP;
import net.minecraft.core.net.packet.PacketBlockRegionUpdate;
import net.minecraft.core.net.packet.PacketSetHealth;
import net.minecraft.core.world.Explosion;
import net.minecraft.core.world.chunk.ChunkPosition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketHandlerClient.class)
public abstract class PacketHandlerClientMixin {
   @Shadow
   private WorldClientMP worldClientMP;
   @Shadow
   @Final
   private Minecraft mc;

   @Inject(method = "handleUpdateHealth", at = @At("HEAD"))
   private void blueprints$exitOnDamage(PacketSetHealth packet, CallbackInfo ci) {
      if (DesignModeState.isActive() && this.mc != null && this.mc.thePlayer != null && packet.healthMP < this.mc.thePlayer.getHealth()) {
         DesignModeState.handleDamageExit(this.mc);
      }
   }

   @Redirect(
      method = "handleMultiBlockChange",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/WorldClientMP;removePositionTypesInBounds(IIIIII)V")
   )
   private void blueprints$protectGhostInMultiBlock(WorldClientMP worldMP, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      worldMP.removePositionTypesInBounds(minX, minY, minZ, maxX, maxY, maxZ);
      int serverId = worldMP.getBlockId(minX, minY, minZ);
      int serverMeta = worldMP.getBlockMetadata(minX, minY, minZ);
      int[] desired = GhostBlockState.getRecentFulfillment(minX, minY, minZ);
      if (desired != null && serverId == desired[0] && serverMeta != desired[1]) {
         GhostBlockState.setBlockNoLighting(worldMP, minX, minY, minZ, serverId, desired[1]);
         worldMP.markBlocksDirty(minX, minY, minZ, minX, minY, minZ);
      }

      if (GhostBlockState.isTracked(worldMP, minX, minY, minZ)) {
         int ghostServerId = worldMP.getBlockId(minX, minY, minZ);
         int ghostServerMeta = worldMP.getBlockMetadata(minX, minY, minZ);
         GhostBlockState.updateServer(worldMP, minX, minY, minZ, ghostServerId, ghostServerMeta);
         int ghostId = GhostBlockState.getGhostBlockId(worldMP, minX, minY, minZ);
         int ghostMeta = GhostBlockState.getGhostMetadata(worldMP, minX, minY, minZ);
         if (!GhostBlockState.isHidden() && minY <= GhostBlockState.getLayerCutoffY() && ghostServerId == 0 && (ghostId != 0 || ghostMeta != 0)) {
            GhostBlockState.setBlockNoLighting(worldMP, minX, minY, minZ, ghostId, ghostMeta);
         }

         worldMP.markBlocksDirty(minX, minY, minZ, minX, minY, minZ);
      }
   }

   @Inject(method = "handleMapChunk", at = @At("RETURN"))
   private void blueprints$correctAfterMapChunk(PacketBlockRegionUpdate packet, CallbackInfo ci) {
      if (this.worldClientMP != null) {
         int minX = packet.xPosition;
         int minY = packet.yPosition;
         int minZ = packet.zPosition;
         int maxX = minX + packet.xSize - 1;
         int maxY = minY + packet.ySize - 1;
         int maxZ = minZ + packet.zSize - 1;
         GhostBlockState.correctFulfillmentsInRegion(this.worldClientMP, minX, minY, minZ, maxX, maxY, maxZ);
         GhostBlockState.restoreGhostsInRegion(this.worldClientMP, minX, minY, minZ, maxX, maxY, maxZ);
      }
   }

   @Redirect(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/world/Explosion;addEffects(Z)V"))
   private void blueprints$protectGhostsFromExplosion(Explosion explosion, boolean particles) {
      if (this.worldClientMP != null) {
         Set<ChunkPosition> original = explosion.destroyedBlockPositions;
         HashSet<ChunkPosition> filtered = new HashSet<>(original);
         Iterator<ChunkPosition> it = filtered.iterator();

         while (it.hasNext()) {
            ChunkPosition pos = it.next();
            if (GhostBlockState.isTracked(this.worldClientMP, pos.x, pos.y, pos.z)) {
               it.remove();
            }
         }

         explosion.destroyedBlockPositions = filtered;
         explosion.addEffects(particles);
         explosion.destroyedBlockPositions = original;
      } else {
         explosion.addEffects(particles);
      }
   }
}
