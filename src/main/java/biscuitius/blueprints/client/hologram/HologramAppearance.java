package biscuitius.blueprints.client.hologram;

import net.minecraft.core.world.World;

public final class HologramAppearance {
   private static float opacity = 0.85F;
   private static float hue = 0.5F;
   private static float saturation = 0.0F;
   private static int cachedR = 255;
   private static int cachedG = 255;
   private static int cachedB = 255;
   private static int cachedA = 216;
   private static boolean hidden;
   private static int layerCutoffY = Integer.MAX_VALUE;
   private static boolean layerAtMax = true;

   private HologramAppearance() {
   }

   public static float getOpacity() {
      return opacity;
   }

   public static float getHue() {
      return hue;
   }

   public static float getSaturation() {
      return saturation;
   }

   public static int getR() {
      return cachedR;
   }

   public static int getG() {
      return cachedG;
   }

   public static int getB() {
      return cachedB;
   }

   public static int getA() {
      return cachedA;
   }

   public static void setOpacity(float v) {
      opacity = clamp01(v);
      recomputeCache();
   }

   public static void setHue(float v) {
      hue = clamp01(v);
      recomputeCache();
   }

   public static void setSaturation(float v) {
      saturation = clamp01(v);
      recomputeCache();
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static void recomputeCache() {
      float h = (hue - (float)Math.floor(hue)) * 6.0F;
      int hi = (int)h;
      float f = h - hi;
      float p = 1.0F - saturation;
      float q = 1.0F - f * saturation;
      float t = 1.0F - (1.0F - f) * saturation;
      float r;
      float g;
      float b;
      switch (hi) {
         case 0:
            r = 1.0F;
            g = t;
            b = p;
            break;
         case 1:
            r = q;
            g = 1.0F;
            b = p;
            break;
         case 2:
            r = p;
            g = 1.0F;
            b = t;
            break;
         case 3:
            r = p;
            g = q;
            b = 1.0F;
            break;
         case 4:
            r = t;
            g = p;
            b = 1.0F;
            break;
         default:
            r = 1.0F;
            g = p;
            b = q;
      }

      cachedR = (int)(r * 255.0F);
      cachedG = (int)(g * 255.0F);
      cachedB = (int)(b * 255.0F);
      cachedA = (int)(opacity * 255.0F);
   }

   public static boolean isHidden() {
      return hidden;
   }

   public static boolean isLayerAtMax() {
      return layerAtMax;
   }

   public static int getLayerCutoffY() {
      return layerCutoffY;
   }

   public static void setHidden(boolean hide) {
      if (hidden != hide) {
         hidden = hide;
         HologramRenderer.markAllDirty();
      }
   }

   public static void setLayerCount(World world, int count, boolean atMax) {
      layerAtMax = atMax;
      int newCutoff;
      if (!atMax && world != null) {
         int[] range = HologramStore.getYRange(world);
         newCutoff = range == null ? Integer.MAX_VALUE : range[0] + count - 1;
      } else {
         newCutoff = Integer.MAX_VALUE;
      }

      if (newCutoff != layerCutoffY) {
         layerCutoffY = newCutoff;
         HologramRenderer.markAllDirty();
      }
   }

   public static boolean isYVisible(int y) {
      return !hidden && y <= layerCutoffY;
   }

   static {
      recomputeCache();
   }
}
