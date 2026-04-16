package biscuitius.blueprints.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ButtonElement;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.gui.SliderElement;
import net.minecraft.client.input.InputDevice;

public final class ScreenDesignTools extends Screen {
   private static final int ID_CLEAR = 0;
   private static final int ID_HIDE = 1;
   private static final int ID_LAYERS = 2;
   private static final int ID_COLOUR = 3;
   private static final int ID_OPACITY = 4;
   private static final int ID_SATURATION = 5;
   private static final int ID_SHUFFLE = 6;
   private static final int ID_MOVE_UP = 10;
   private static final int ID_MOVE_DOWN = 11;
   private static final int ID_MOVE_NORTH = 12;
   private static final int ID_MOVE_SOUTH = 13;
   private static final int ID_MOVE_EAST = 14;
   private static final int ID_MOVE_WEST = 15;
   private static final int BTN_WIDTH = 200;
   private static final int BTN_HEIGHT = 20;
   private static final int GAP = 4;
   private static final int SMALL_BTN_WIDTH = 64;
   private SliderElement layersSlider;
   private SliderElement colourSlider;
   private SliderElement opacitySlider;
   private SliderElement saturationSlider;
   private float prevHue;
   private float prevOpacity;
   private float prevSaturation;
   private int titleY;
   private int moveSubtitleY;

   public void init() {
      int cx = this.width / 2;
      boolean inDesignMode = DesignModeState.isActive();
      int rowCount = 8;
      if (!inDesignMode) {
         rowCount++;
      }

      if (inDesignMode) {
         rowCount++;
      }

      int totalHeight = rowCount * 20 + (rowCount - 1) * 4;
      int top = this.height / 2 - (totalHeight + 12) / 2 + 12;
      this.titleY = top - 12;
      this.buttons.add(new ButtonElement(0, cx - 100, top, 200, 20, "Clear All Blueprints"));
      top += 24;
      if (!inDesignMode) {
         ButtonElement hideShowButton = new ButtonElement(1, cx - 100, top, 200, 20, GhostBlockState.isHidden() ? "Show Blueprints" : "Hide Blueprints");
         this.buttons.add(hideShowButton);
         top += 24;
      }

      Minecraft mc = Minecraft.getMinecraft();
      int totalLayers = GhostBlockState.getTotalLayers(mc.currentWorld);
      float initialSlider = 1.0F;
      if (!GhostBlockState.isLayerAtMax() && totalLayers > 1) {
         int[] range = GhostBlockState.getYRange(mc.currentWorld);
         if (range != null) {
            int currentCutoff = GhostBlockState.getLayerCutoffY();
            int currentLayer = Math.min(currentCutoff - range[0] + 1, totalLayers);
            initialSlider = (float)(currentLayer - 1) / (totalLayers - 1);
         }
      }

      String label = totalLayers > 1 && !GhostBlockState.isLayerAtMax() ? "Layers" : "Layer: All";
      this.layersSlider = new SliderElement(2, cx - 100, top, 200, 20, label, initialSlider);
      this.buttons.add(this.layersSlider);
      top += 24;
      float hue = GhostBlockState.getHologramHue();
      this.colourSlider = new SliderElement(3, cx - 100, top, 200, 20, "Hologram Colour: " + Math.round(hue * 360.0F) + "°", hue);
      this.buttons.add(this.colourSlider);
      top += 24;
      float opacity = GhostBlockState.getHologramOpacity();
      this.opacitySlider = new SliderElement(4, cx - 100, top, 200, 20, "Hologram Opacity: " + Math.round(opacity * 100.0F) + "%", opacity);
      this.buttons.add(this.opacitySlider);
      top += 24;
      float sat = GhostBlockState.getHologramSaturation();
      this.saturationSlider = new SliderElement(5, cx - 100, top, 200, 20, "Hologram Saturation: " + Math.round(sat * 100.0F) + "%", sat);
      this.buttons.add(this.saturationSlider);
      top += 24;
      if (inDesignMode) {
         this.buttons.add(new ButtonElement(6, cx - 100, top, 200, 20, DesignModeState.isShuffleEnabled() ? "Shuffle Hotbar: ON" : "Shuffle Hotbar: OFF"));
         top += 24;
      }

      this.moveSubtitleY = top + 6;
      top += 24;
      int leftCol = cx - 100;
      int midCol = leftCol + 64 + 4;
      int rightCol = midCol + 64 + 4;
      this.buttons.add(new ButtonElement(10, leftCol, top, 64, 20, "Up (+Y)"));
      this.buttons.add(new ButtonElement(12, midCol, top, 64, 20, "North (-Z)"));
      this.buttons.add(new ButtonElement(14, rightCol, top, 64, 20, "East (+X)"));
      top += 24;
      this.buttons.add(new ButtonElement(11, leftCol, top, 64, 20, "Down (-Y)"));
      this.buttons.add(new ButtonElement(13, midCol, top, 64, 20, "South (+Z)"));
      this.buttons.add(new ButtonElement(15, rightCol, top, 64, 20, "West (-X)"));
      this.prevHue = hue;
      this.prevOpacity = opacity;
      this.prevSaturation = sat;
   }

   protected void buttonClicked(ButtonElement button) {
      Minecraft mc = Minecraft.getMinecraft();
      if (button.id == 0) {
         if (mc != null && mc.currentWorld != null) {
            GhostBlockState.clearAll(mc.currentWorld);
            BlueprintsCacheManager.markDirty();
         }
      } else if (button.id == 1) {
         boolean nowHidden = !GhostBlockState.isHidden();
         GhostBlockState.setHidden(nowHidden, mc != null ? mc.currentWorld : null);
         button.displayString = nowHidden ? "Show Blueprints" : "Hide Blueprints";
      } else if (button.id == 6) {
         DesignModeState.toggleShuffle(mc);
         button.displayString = DesignModeState.isShuffleEnabled() ? "Shuffle Hotbar: ON" : "Shuffle Hotbar: OFF";
      } else if (button.id >= 10 && button.id <= 15 && mc != null && mc.currentWorld != null) {
         int dx = 0;
         int dy = 0;
         int dz = 0;
         switch (button.id) {
            case 10:
               dy = 1;
               break;
            case 11:
               dy = -1;
               break;
            case 12:
               dz = -1;
               break;
            case 13:
               dz = 1;
               break;
            case 14:
               dx = 1;
               break;
            case 15:
               dx = -1;
         }

         GhostBlockState.shiftAll(mc.currentWorld, dx, dy, dz);
         BlueprintsCacheManager.markDirty();
      }
   }

   public void tick() {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc != null && mc.currentWorld != null) {
         if (this.layersSlider != null) {
            int totalLayers = GhostBlockState.getTotalLayers(mc.currentWorld);
            if (totalLayers <= 1) {
               this.layersSlider.displayString = "Layer: All";
               GhostBlockState.setLayerCount(mc.currentWorld, Integer.MAX_VALUE, true);
            } else {
               if (GhostBlockState.isLayerAtMax() && !this.layersSlider.dragging) {
                  this.layersSlider.sliderValue = 1.0;
               }

               int layer = (int)Math.round(this.layersSlider.sliderValue * (totalLayers - 1)) + 1;
               if (layer > totalLayers) {
                  layer = totalLayers;
               }

               boolean atMax = layer >= totalLayers;
               this.layersSlider.displayString = atMax ? "Layer: All" : "Layer: " + layer;
               GhostBlockState.setLayerCount(mc.currentWorld, atMax ? Integer.MAX_VALUE : layer, atMax);
            }
         }

         boolean appearanceChanged = false;
         if (this.colourSlider != null) {
            float hue = (float)this.colourSlider.sliderValue;
            this.colourSlider.displayString = "Colour: " + Math.round(hue * 360.0F) + "°";
            GhostBlockState.setHologramHue(hue);
            if (hue != this.prevHue) {
               this.prevHue = hue;
               appearanceChanged = true;
            }
         }

         if (this.opacitySlider != null) {
            float opacity = (float)this.opacitySlider.sliderValue;
            this.opacitySlider.displayString = "Opacity: " + Math.round(opacity * 100.0F) + "%";
            GhostBlockState.setHologramOpacity(opacity);
            if (opacity != this.prevOpacity) {
               this.prevOpacity = opacity;
               appearanceChanged = true;
            }
         }

         if (this.saturationSlider != null) {
            float sat = (float)this.saturationSlider.sliderValue;
            this.saturationSlider.displayString = "Saturation: " + Math.round(sat * 100.0F) + "%";
            GhostBlockState.setHologramSaturation(sat);
            if (sat != this.prevSaturation) {
               this.prevSaturation = sat;
               appearanceChanged = true;
            }
         }

         if (appearanceChanged) {
            GhostBlockState.markAllDirty(mc.currentWorld);
            BlueprintsConfig.save();
         }
      }
   }

   public void render(int mx, int my, float partialTick) {
      this.renderBackground();
      this.drawStringCentered(this.font, "Blueprints Utility Menu", this.width / 2, this.titleY, 16777215);
      this.drawStringCentered(this.font, "Move Blueprints", this.width / 2, this.moveSubtitleY, 11184810);
      super.render(mx, my, partialTick);
   }

   public void keyPressed(char eventCharacter, int eventKey, int mx, int my) {
      if (DesignModeState.TOOLS_KEY.isPressEvent(InputDevice.keyboard)) {
         this.mc.displayScreen(null);
      } else {
         super.keyPressed(eventCharacter, eventKey, mx, my);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
