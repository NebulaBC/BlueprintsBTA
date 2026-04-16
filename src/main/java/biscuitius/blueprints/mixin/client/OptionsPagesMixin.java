package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import net.minecraft.client.gui.options.components.KeyBindingComponent;
import net.minecraft.client.gui.options.components.OptionsCategory;
import net.minecraft.client.gui.options.data.OptionsPages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsPages.class)
public abstract class OptionsPagesMixin {
   @Inject(method = "init", at = @At("RETURN"))
   private static void blueprints$addKeybindCategory(CallbackInfo ci) {
      if (OptionsPages.CONTROLS != null) {
         OptionsPages.CONTROLS
            .withComponent(
               new OptionsCategory("gui.options.page.controls.category.blueprints")
                  .withComponent(new KeyBindingComponent(DesignModeState.TOGGLE_KEY))
                  .withComponent(new KeyBindingComponent(DesignModeState.TOOLS_KEY))
                  .withComponent(new KeyBindingComponent(DesignModeState.SHUFFLE_KEY))
            );
      }
   }
}
