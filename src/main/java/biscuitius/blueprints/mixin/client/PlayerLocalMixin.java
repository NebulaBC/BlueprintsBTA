package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.GhostBlockState;
import biscuitius.blueprints.client.SignTextCache;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicSign;
import net.minecraft.core.block.entity.TileEntitySign;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerLocal.class)
public abstract class PlayerLocalMixin {
   @Inject(method = "displaySignEditorScreen", at = @At("HEAD"), cancellable = true)
   private void blueprints$suppressOrGuardSignEditor(TileEntitySign tileEntity, CallbackInfo ci) {
      if (SignTextCache.isSuppressingSignEditor()) {
         ci.cancel();
      } else {
         if (tileEntity != null) {
            Block<?> block = tileEntity.getBlock();
            if (block == null || !(block.getLogic() instanceof BlockLogicSign)) {
               ci.cancel();
            }
         }
      }
   }

   @Inject(method = "checkInTile", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipGhostPushOut(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
      if (DesignModeState.isActive()) {
         if ((PlayerLocal)(Object)this != DesignModeState.getDesignPlayer()) {
            Player self = (Player)(Object)this;
            World world = self.world;
            if (world != null) {
               int bx = MathHelper.floor(x);
               int by = MathHelper.floor(y);
               int bz = MathHelper.floor(z);
               if (GhostBlockState.isGhostBlock(world, bx, by, bz) || GhostBlockState.isGhostBlock(world, bx, by + 1, bz)) {
                  cir.setReturnValue(false);
               }
            }
         }
      }
   }
}
