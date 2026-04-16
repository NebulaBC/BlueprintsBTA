package biscuitius.blueprints.mixin.client;

import java.util.List;
import net.minecraft.client.render.TextureManager;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public abstract class TextureManagerMixin {
   @Inject(method = "refreshTextures", at = @At("HEAD"))
   private void blueprints$registerDesignTextures(List errors, CallbackInfo ci) {
      TextureRegistry.getTexture("blueprints:gui/hud/design_hotbar");
      TextureRegistry.getTexture("blueprints:gui/hud/design_hotbar_selection");
      TextureRegistry.getTexture("blueprints:gui/hud/design_hotbar_selector");
      TextureRegistry.getTexture("blueprints:gui/hud/design_hotbar_selector0");
      TextureRegistry.getTexture("blueprints:gui/hud/design_hotbar_selector1");
      TextureRegistry.getTexture("blueprints:gui/hud/design_hotbar_selector2");
      TextureRegistry.getTexture("blueprints:gui/hud/design_hotbar_selector3");
   }
}
