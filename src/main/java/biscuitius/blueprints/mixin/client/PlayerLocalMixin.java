package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.SignTextCache;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicSign;
import net.minecraft.core.block.entity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
}
