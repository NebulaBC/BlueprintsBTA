package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.GhostBlockState;
import net.minecraft.core.world.chunk.Chunk;
import net.minecraft.core.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Chunk.class)
public abstract class ChunkMixin {
   @Shadow
   public boolean isModified;

   @Shadow
   public abstract ChunkSection getSection(int var1);

   @Inject(method = "setBlockIDWithMetadataRaw", at = @At("HEAD"), cancellable = true)
   private void blueprints$suppressLightingRaw(int x, int y, int z, int id, int data, CallbackInfoReturnable<Boolean> cir) {
      if (GhostBlockState.isSuppressingLighting()) {
         if (x >= 0 && x < 16 && y >= 0 && y < 256 && z >= 0 && z < 16) {
            ChunkSection section = this.getSection(y / 16);
            if (section != null) {
               int currentId = section.getBlock(x, y % 16, z);
               int currentData = section.getData(x, y % 16, z);
               if (currentId == id && currentData == data) {
                  cir.setReturnValue(false);
               } else {
                  section.setBlock(x, y % 16, z, id);
                  section.setData(x, y % 16, z, data);
                  this.isModified = true;
                  cir.setReturnValue(true);
               }
            } else {
               cir.setReturnValue(false);
            }
         } else {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(method = "setBlockIDWithMetadata", at = @At("HEAD"), cancellable = true)
   private void blueprints$suppressLighting(int x, int y, int z, int id, int data, CallbackInfoReturnable<Boolean> cir) {
      if (GhostBlockState.isSuppressingLighting()) {
         if (x >= 0 && x < 16 && y >= 0 && y < 256 && z >= 0 && z < 16) {
            ChunkSection section = this.getSection(y / 16);
            if (section != null) {
               int currentId = section.getBlock(x, y % 16, z);
               int currentData = section.getData(x, y % 16, z);
               if (currentId == id && currentData == data) {
                  cir.setReturnValue(false);
               } else {
                  section.setBlock(x, y % 16, z, id);
                  section.setData(x, y % 16, z, data);
                  this.isModified = true;
                  cir.setReturnValue(true);
               }
            } else {
               cir.setReturnValue(false);
            }
         } else {
            cir.setReturnValue(false);
         }
      }
   }
}
