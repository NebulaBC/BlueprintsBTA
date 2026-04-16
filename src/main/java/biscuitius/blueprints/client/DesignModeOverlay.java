package biscuitius.blueprints.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Font;
import net.minecraft.core.util.helper.Time;
import org.lwjgl.opengl.GL11;

public final class DesignModeOverlay {
   private static String message;
   private static int color;
   private static double time;
   private static final double DISPLAY_TIME = 2.5;
   private static final double FADE_TIME = 0.5;

   private DesignModeOverlay() {
   }

   public static void show(String text, int rgb) {
      message = text;
      color = rgb;
      time = 2.5;
   }

   public static void renderIfActive(Minecraft mc, int hotbarCentreX, int hotbarTopY) {
      if (message != null && !(time < 0.0)) {
         double alpha = time < 0.5 ? time / 0.5 : 1.0;
         if (!mc.isGamePaused) {
            time = time - Time.delta;
         }

         int a = (int)(alpha * 255.0);
         if (a >= 1) {
            int argb = color & 16777215 | a << 24;
            if (a < 255) {
               GL11.glEnable(3042);
               GL11.glDisable(3008);
               GL11.glBlendFunc(770, 771);
            }

            Font font = mc.font;
            int textWidth = font.getStringWidth(message);
            int y = hotbarTopY - 10;
            font.drawStringWithShadow(message, hotbarCentreX - textWidth / 2, y, argb);
            GL11.glDisable(3042);
            GL11.glEnable(3008);
         }
      }
   }
}
