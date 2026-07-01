package biscuitius.blueprints.client;

import biscuitius.blueprints.client.hologram.BlueprintTransform;
import biscuitius.blueprints.client.hologram.HologramAppearance;
import biscuitius.blueprints.client.hologram.HologramRenderer;
import biscuitius.blueprints.client.hologram.HologramStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.gui.ButtonElement;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.gui.SliderElement;
import net.minecraft.client.input.InputDevice;
import net.minecraft.core.world.World;

public final class ScreenDesignTools extends Screen {
   private static final int ID_CLEAR = 0;
   private static final int ID_HIDE = 1;
   private static final int ID_LAYERS = 2;
   private static final int ID_COLOUR = 3;
   private static final int ID_OPACITY = 4;
   private static final int ID_SATURATION = 5;
   private static final int ID_SHUFFLE = 6;
   private static final int ID_INTERACTION = 7;
   private static final int ID_SAVE = 8;
   private static final int ID_LOAD = 9;
   private static final int ID_MOVE_UP = 10;
   private static final int ID_MOVE_DOWN = 11;
   private static final int ID_MOVE_FORWARD = 12;
   private static final int ID_MOVE_BACKWARD = 13;
   private static final int ID_MOVE_LEFT = 14;
   private static final int ID_MOVE_RIGHT = 15;
   private static final int ID_ROTATE_CCW = 16;
   private static final int ID_ROTATE_CW = 17;
   private static final int ID_FLIP = 18;
   private static final int ID_MATERIALS = 19;
   private static final int BTN_WIDTH = 240;
   private static final int BTN_HEIGHT = 20;
   private static final int GAP = 4;
   private static final int SMALL_BTN_WIDTH = 77;
   private static final int HALF_BTN_WIDTH = 118;
   private SliderElement layersSlider;
   private SliderElement colourSlider;
   private SliderElement opacitySlider;
   private SliderElement saturationSlider;
   private float prevHue;
   private float prevOpacity;
   private float prevSaturation;
   private int titleY;

   private static int getTotalLayers(World world) {
      if (world == null) {
         return 0;
      } else {
         int[] range = HologramStore.getYRange(world);
         return range == null ? 0 : range[1] - range[0] + 1;
      }
   }

   public void init() {
      int cx = this.width / 2;
      int rowCount = 8;
      int totalHeight = rowCount * 20 + (rowCount - 1) * 4;
      int top = this.height / 2 - (totalHeight + 12) / 2 + 12;
      this.titleY = top - 12;
      int leftCol = cx - 120;
      int rightCol = leftCol + 118 + 4;
      int midCol3 = leftCol + 77 + 4;
      int rightCol3 = midCol3 + 77 + 4;
      this.buttons.add(new ButtonElement(0, leftCol, top, 77, 20, "Clear"));
      this.buttons.add(new ButtonElement(1, midCol3, top, 77, 20, HologramAppearance.isHidden() ? "Show" : "Hide"));
      this.buttons.add(new ButtonElement(19, rightCol3, top, 77, 20, "Materials"));
      top += 24;
      this.buttons.add(new ButtonElement(8, leftCol, top, 118, 20, "Save Blueprint"));
      this.buttons.add(new ButtonElement(9, rightCol, top, 118, 20, "Load Blueprint"));
      top += 24;
      Minecraft mc = Minecraft.getMinecraft();
      int totalLayers = getTotalLayers(mc.currentWorld);
      float initialSlider = 1.0F;
      if (!HologramAppearance.isLayerAtMax() && totalLayers > 1) {
         int[] range = HologramStore.getYRange(mc.currentWorld);
         if (range != null) {
            int currentCutoff = HologramAppearance.getLayerCutoffY();
            int currentLayer = Math.min(currentCutoff - range[0] + 1, totalLayers);
            initialSlider = (float)(currentLayer - 1) / (totalLayers - 1);
         }
      }

      String layerLabel = totalLayers > 1 && !HologramAppearance.isLayerAtMax() ? "Layers" : "Layer: All";
      this.layersSlider = new SliderElement(2, leftCol, top, 118, 20, layerLabel, initialSlider);
      this.buttons.add(this.layersSlider);
      float opacity = HologramAppearance.getOpacity();
      this.opacitySlider = new SliderElement(4, rightCol, top, 118, 20, "Opacity: " + Math.round(opacity * 100.0F) + "%", opacity);
      this.buttons.add(this.opacitySlider);
      top += 24;
      float hue = HologramAppearance.getHue();
      this.colourSlider = new SliderElement(3, leftCol, top, 118, 20, "Colour: " + Math.round(hue * 360.0F) + "°", hue);
      this.buttons.add(this.colourSlider);
      float sat = HologramAppearance.getSaturation();
      this.saturationSlider = new SliderElement(5, rightCol, top, 118, 20, "Saturation: " + Math.round(sat * 100.0F) + "%", sat);
      this.buttons.add(this.saturationSlider);
      top += 24;
      this.buttons.add(new ButtonElement(6, leftCol, top, 118, 20, DesignModeState.isShuffleEnabled() ? "Shuffle: ON" : "Shuffle: OFF"));
      this.buttons.add(new ButtonElement(7, rightCol, top, 118, 20, DesignModeState.isPassthroughMode() ? "Interact: Passthrough" : "Interact: Fulfill"));
      top += 24;
      this.buttons.add(new ButtonElement(11, rightCol3, top, 77, 20, "Down"));
      this.buttons.add(new ButtonElement(12, midCol3, top, 77, 20, "Forward"));
      this.buttons.add(new ButtonElement(10, leftCol, top, 77, 20, "Up"));
      top += 24;
      this.buttons.add(new ButtonElement(14, leftCol, top, 77, 20, "Left"));
      this.buttons.add(new ButtonElement(13, midCol3, top, 77, 20, "Back"));
      this.buttons.add(new ButtonElement(15, rightCol3, top, 77, 20, "Right"));
      top += 24;
      this.buttons.add(new ButtonElement(16, leftCol, top, 77, 20, "← Rotate"));
      this.buttons.add(new ButtonElement(18, midCol3, top, 77, 20, "Flip"));
      this.buttons.add(new ButtonElement(17, rightCol3, top, 77, 20, "Rotate →"));
      this.prevHue = hue;
      this.prevOpacity = opacity;
      this.prevSaturation = sat;
   }

   protected void buttonClicked(ButtonElement button) {
      Minecraft mc = Minecraft.getMinecraft();
      if (button.id == 0) {
         if (mc != null && mc.currentWorld != null) {
            HologramStore.clearWorld(mc.currentWorld);
         }
      } else if (button.id == 8) {
         this.mc.displayScreen(new ScreenBlueprintBrowser(this, ScreenBlueprintBrowser.Mode.SAVE));
      } else if (button.id == 9) {
         this.mc.displayScreen(new ScreenBlueprintBrowser(this, ScreenBlueprintBrowser.Mode.LOAD));
      } else if (button.id == 1) {
         boolean nowHidden = !HologramAppearance.isHidden();
         HologramAppearance.setHidden(nowHidden);
         button.displayString = nowHidden ? "Show Blueprint" : "Hide Blueprint";
      } else if (button.id == 6) {
         DesignModeState.toggleShuffle(mc);
         button.displayString = DesignModeState.isShuffleEnabled() ? "Shuffle: ON" : "Shuffle: OFF";
      } else if (button.id == 7) {
         DesignModeState.toggleInteractionMode(mc);
         button.displayString = DesignModeState.isPassthroughMode() ? "Interact: Passthrough" : "Interact: Fulfill";
      } else if (button.id == 19) {
         this.mc.displayScreen(new ScreenBlueprintMaterials(this));
      } else if (button.id == 17) {
         if (mc != null && mc.currentWorld != null) {
            BlueprintTransform.rotate(mc.currentWorld, BlueprintTransform.Rotation.CW);
         }
      } else if (button.id == 16) {
         if (mc != null && mc.currentWorld != null) {
            BlueprintTransform.rotate(mc.currentWorld, BlueprintTransform.Rotation.CCW);
         }
      } else if (button.id == 18) {
         if (mc != null && mc.currentWorld != null) {
            BlueprintTransform.flip(mc.currentWorld, BlueprintTransform.FlipAxis.X);
         }
      } else if (button.id >= 10 && button.id <= 15 && mc != null && mc.currentWorld != null) {
         int dx = 0;
         int dy = 0;
         int dz = 0;
         if (button.id == 10) {
            dy = 1;
         } else if (button.id == 11) {
            dy = -1;
         } else {
            int[] fwd = forwardCardinal(mc);
            int fx = fwd[0];
            int fz = fwd[1];
            switch (button.id) {
               case 12:
                  dx = fx;
                  dz = fz;
                  break;
               case 13:
                  dx = -fx;
                  dz = -fz;
                  break;
               case 14:
                  dx = fz;
                  dz = -fx;
                  break;
               case 15:
                  dx = -fz;
                  dz = fx;
            }
         }

         HologramStore.shiftAll(mc.currentWorld, dx, dy, dz);
      }
   }

   private static int[] forwardCardinal(Minecraft mc) {
      PlayerLocal player = DesignModeState.getControlPlayer(mc);
      float yaw = player != null ? player.yRot : 0.0F;
      yaw = (yaw % 360.0F + 360.0F) % 360.0F;
      int facing = (int)Math.floor((yaw + 45.0) / 90.0) & 3;
      switch (facing) {
         case 0:
            return new int[]{0, 1};
         case 1:
            return new int[]{-1, 0};
         case 2:
            return new int[]{0, -1};
         default:
            return new int[]{1, 0};
      }
   }

   public void tick() {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc != null && mc.currentWorld != null) {
         if (this.layersSlider != null) {
            int totalLayers = getTotalLayers(mc.currentWorld);
            if (totalLayers <= 1) {
               this.layersSlider.displayString = "Layer: All";
               HologramAppearance.setLayerCount(mc.currentWorld, Integer.MAX_VALUE, true);
            } else {
               if (HologramAppearance.isLayerAtMax() && !this.layersSlider.dragging) {
                  this.layersSlider.sliderValue = 1.0;
               }

               int layer = (int)Math.round(this.layersSlider.sliderValue * (totalLayers - 1)) + 1;
               if (layer > totalLayers) {
                  layer = totalLayers;
               }

               boolean atMax = layer >= totalLayers;
               this.layersSlider.displayString = atMax ? "Layer: All" : "Layer: " + layer;
               HologramAppearance.setLayerCount(mc.currentWorld, atMax ? Integer.MAX_VALUE : layer, atMax);
            }
         }

         boolean appearanceChanged = false;
         if (this.colourSlider != null) {
            float hue = (float)this.colourSlider.sliderValue;
            this.colourSlider.displayString = "Colour: " + Math.round(hue * 360.0F) + "°";
            HologramAppearance.setHue(hue);
            if (hue != this.prevHue) {
               this.prevHue = hue;
               appearanceChanged = true;
            }
         }

         if (this.opacitySlider != null) {
            float opacity = (float)this.opacitySlider.sliderValue;
            this.opacitySlider.displayString = "Opacity: " + Math.round(opacity * 100.0F) + "%";
            HologramAppearance.setOpacity(opacity);
            if (opacity != this.prevOpacity) {
               this.prevOpacity = opacity;
               appearanceChanged = true;
            }
         }

         if (this.saturationSlider != null) {
            float sat = (float)this.saturationSlider.sliderValue;
            this.saturationSlider.displayString = "Saturation: " + Math.round(sat * 100.0F) + "%";
            HologramAppearance.setSaturation(sat);
            if (sat != this.prevSaturation) {
               this.prevSaturation = sat;
               appearanceChanged = true;
            }
         }

         if (appearanceChanged) {
            HologramRenderer.markAllDirty();
            BlueprintsConfig.save();
         }
      }
   }

   public void render(int mx, int my, float partialTick) {
      this.renderBackground();
      this.drawStringCentered(this.font, "Blueprints Menu", this.width / 2, this.titleY, 16777215);
      super.render(mx, my, partialTick);
   }

   public void keyPressed(char eventCharacter, int eventKey, int mx, int my) {
      if (DesignModeState.MENU_KEY.isPressEvent(InputDevice.keyboard)) {
         this.mc.displayScreen(null);
      } else {
         super.keyPressed(eventCharacter, eventKey, mx, my);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
