package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.item.BlueprintItem;
import biscuitius.blueprints.client.item.ClipboardToolItem;
import biscuitius.blueprints.client.item.FillToolItem;
import biscuitius.blueprints.client.item.LineToolItem;
import biscuitius.blueprints.client.item.MoveToolItem;
import biscuitius.blueprints.client.item.OvalToolItem;
import biscuitius.blueprints.client.item.RectangleToolItem;
import biscuitius.blueprints.client.item.ReplaceToolItem;
import biscuitius.blueprints.client.item.RotateToolItem;
import java.util.List;
import net.minecraft.client.render.TextureManager;
import net.minecraft.client.render.item.model.ItemModelDispatcher;
import net.minecraft.client.render.item.model.ItemModelStandard;
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
      TextureRegistry.getTexture("blueprints:gui/hud/ink_preview");
      TextureRegistry.getTexture("blueprints:gui/widgets/button/button");
      TextureRegistry.getTexture("blueprints:gui/widgets/button/button_highlighted");
      TextureRegistry.getTexture("blueprints:gui/widgets/button/button_disabled");
      TextureRegistry.getTexture("blueprints:gui/widgets/slider/slider");
      TextureRegistry.getTexture("blueprints:gui/widgets/slider/slider_highlighted");
      TextureRegistry.getTexture("blueprints:gui/widgets/slider/slider_disabled");
      TextureRegistry.getTexture("blueprints:gui/widgets/slider/slider_background");
      TextureRegistry.getTexture("blueprints:gui/screen/design/clear");
      TextureRegistry.getTexture("blueprints:gui/screen/design/clear_highlighted");
      BlueprintItem.register();
      TextureRegistry.getTexture("blueprints:item/blueprint");
      ItemModelDispatcher.getInstance().addDispatch(new ItemModelStandard(BlueprintItem.get(), "blueprints"));
      MoveToolItem.register();
      TextureRegistry.getTexture("blueprints:item/move");
      ItemModelDispatcher.getInstance().addDispatch(new ItemModelStandard(MoveToolItem.get(), null).setIcon("blueprints:item/move"));
      RotateToolItem.register();
      TextureRegistry.getTexture("blueprints:item/rotate");
      ItemModelDispatcher.getInstance().addDispatch(new ItemModelStandard(RotateToolItem.get(), null).setIcon("blueprints:item/rotate"));
      LineToolItem.register();
      TextureRegistry.getTexture("blueprints:item/line");
      ItemModelDispatcher.getInstance().addDispatch(new ItemModelStandard(LineToolItem.get(), null).setIcon("blueprints:item/line"));
      ClipboardToolItem.register();
      TextureRegistry.getTexture("blueprints:item/clipboard");
      ItemModelDispatcher.getInstance().addDispatch(new ItemModelStandard(ClipboardToolItem.get(), null).setIcon("blueprints:item/clipboard"));
      FillToolItem.register();
      TextureRegistry.getTexture("blueprints:item/fill");
      ItemModelDispatcher.getInstance().addDispatch(new ItemModelStandard(FillToolItem.get(), null).setIcon("blueprints:item/fill"));
      ReplaceToolItem.register();
      TextureRegistry.getTexture("blueprints:item/replace");
      ItemModelDispatcher.getInstance().addDispatch(new ItemModelStandard(ReplaceToolItem.get(), null).setIcon("blueprints:item/replace"));
      RectangleToolItem.register();
      TextureRegistry.getTexture("blueprints:item/rectangle");
      ItemModelDispatcher.getInstance().addDispatch(new ItemModelStandard(RectangleToolItem.get(), null).setIcon("blueprints:item/rectangle"));
      OvalToolItem.register();
      TextureRegistry.getTexture("blueprints:item/oval");
      ItemModelDispatcher.getInstance().addDispatch(new ItemModelStandard(OvalToolItem.get(), null).setIcon("blueprints:item/oval"));
   }
}
