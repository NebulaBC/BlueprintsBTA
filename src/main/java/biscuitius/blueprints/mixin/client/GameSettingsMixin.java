package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import java.io.File;
import net.minecraft.client.option.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameSettings.class)
public abstract class GameSettingsMixin {
   @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameSettings;loadOptions()V"))
   private static void blueprints$registerDesignModeKeybind(File mcDirectory, CallbackInfo ci) {
      DesignModeState.bootstrap();
   }
}
