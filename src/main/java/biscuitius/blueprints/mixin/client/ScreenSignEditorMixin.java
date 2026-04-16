package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.BlueprintsCacheManager;
import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.SignTextCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenSignEditor;
import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicSign;
import net.minecraft.core.block.entity.TileEntitySign;
import net.minecraft.core.net.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenSignEditor.class)
public abstract class ScreenSignEditorMixin {
   @Shadow
   private TileEntitySign entitySign;

   @Inject(method = "removed", at = @At("HEAD"))
   private void blueprints$cacheSignText(CallbackInfo ci) {
      if (DesignModeState.isActive() && this.entitySign != null) {
         String[] text = new String[4];

         for (int i = 0; i < 4; i++) {
            text[i] = this.entitySign.signText[i] != null ? this.entitySign.signText[i] : "";
         }

         int picture = this.entitySign.getPicture() != null ? this.entitySign.getPicture().getId() : 0;
         int color = this.entitySign.getColor() != null ? this.entitySign.getColor().id : 15;
         SignTextCache.put(this.entitySign.x, this.entitySign.y, this.entitySign.z, text, picture, color);
         BlueprintsCacheManager.markDirty();
      }
   }

   @Redirect(
      method = "removed",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/net/handler/PacketHandlerClient;addToSendQueue(Lnet/minecraft/core/net/packet/Packet;)V")
   )
   private void blueprints$suppressSignPacketInDesignMode(PacketHandlerClient handler, Packet packet) {
      if (!DesignModeState.isActive()) {
         handler.addToSendQueue(packet);
      }
   }

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void blueprints$guardSignCast(int mx, int my, float partialTick, CallbackInfo ci) {
      if (this.entitySign != null) {
         Block<?> block = this.entitySign.getBlock();
         if (block == null || !(block.getLogic() instanceof BlockLogicSign)) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null) {
               mc.displayScreen(null);
            }

            ci.cancel();
         }
      }
   }
}
