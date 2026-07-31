package biscuitius.blueprints.client;

import net.minecraft.client.gui.ButtonElement;
import net.minecraft.client.gui.SliderElement;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;

public final class DesignTextures {
   public static final String BUTTON = "blueprints:gui/widgets/button/button";
   public static final String BUTTON_HIGHLIGHTED = "blueprints:gui/widgets/button/button_highlighted";
   public static final String BUTTON_DISABLED = "blueprints:gui/widgets/button/button_disabled";
   public static final String CLEAR = "blueprints:gui/screen/design/clear";
   public static final String CLEAR_HIGHLIGHTED = "blueprints:gui/screen/design/clear_highlighted";
   public static final String SLIDER = "blueprints:gui/widgets/slider/slider";
   public static final String SLIDER_HIGHLIGHTED = "blueprints:gui/widgets/slider/slider_highlighted";
   public static final String SLIDER_DISABLED = "blueprints:gui/widgets/slider/slider_disabled";
   public static final String SLIDER_BACKGROUND = "blueprints:gui/widgets/slider/slider_background";

   private DesignTextures() {
   }

   public static void applyButton(ButtonElement button) {
      if (button != null) {
         button.setTextures(
            "blueprints:gui/widgets/button/button", "blueprints:gui/widgets/button/button_highlighted", "blueprints:gui/widgets/button/button_disabled"
         );
      }
   }

   public static void applyClear(ButtonElement button) {
      if (button != null) {
         button.setTextures("blueprints:gui/screen/design/clear", "blueprints:gui/screen/design/clear_highlighted", null);
      }
   }

   public static void applySlider(SliderElement slider) {
      if (slider != null) {
         slider.setTextures(
            "blueprints:gui/widgets/slider/slider", "blueprints:gui/widgets/slider/slider_highlighted", "blueprints:gui/widgets/slider/slider_disabled"
         );
         slider.backgroundTexture = TextureRegistry.getTexture("blueprints:gui/widgets/slider/slider_background");
      }
   }
}
