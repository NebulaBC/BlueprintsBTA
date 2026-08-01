package biscuitius.blueprints.mixin.client;

import net.minecraft.client.gui.ScreenSignEditor;
import net.minecraft.core.block.entity.TileEntitySign;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenSignEditor.class)
public abstract class ScreenSignEditorMixin {
   @Shadow
   @Final
   private TileEntitySign entitySign;

   @Inject(method = "removed", at = @At("HEAD"), cancellable = true)
   private void blueprints$skipWorldFinalizeForBlueprintSign(CallbackInfo ci) {
      if (this.entitySign != null && this.entitySign.worldObj == null) {
         Keyboard.enableRepeatEvents(false);
         ci.cancel();
      }
   }
}
