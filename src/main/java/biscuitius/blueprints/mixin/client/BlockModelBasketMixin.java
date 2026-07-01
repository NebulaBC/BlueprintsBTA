package biscuitius.blueprints.mixin.client;

import net.minecraft.client.render.block.model.BlockModelBasket;
import net.minecraft.core.block.BlockLogicBasket;
import net.minecraft.core.world.WorldSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockModelBasket.class)
public abstract class BlockModelBasketMixin {
   @Redirect(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/core/block/BlockLogicBasket;getFillLevel(Lnet/minecraft/core/world/WorldSource;III)I")
   )
   private int blueprints$avoidNullBasketFill(BlockLogicBasket logic, WorldSource world, int x, int y, int z) {
      if (logic != null && world != null) {
         try {
            if (world.getTileEntity(x, y, z) == null) {
               return 0;
            }
         } catch (Throwable var7) {
            return 0;
         }

         return logic.getFillLevel(world, x, y, z);
      } else {
         return 0;
      }
   }
}
