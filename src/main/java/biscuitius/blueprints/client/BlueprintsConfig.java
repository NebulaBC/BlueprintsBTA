package biscuitius.blueprints.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlueprintsConfig {
   private static final Logger LOGGER = LoggerFactory.getLogger("blueprints-config");
   private static final String CONFIG_PATH = "config/blueprints.json";

   private BlueprintsConfig() {
   }

   public static void load() {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc != null) {
         File file = new File(mc.getMinecraftDir(), "config/blueprints.json");
         if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
               JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
               if (root.has("hologramHue")) {
                  GhostBlockState.setHologramHue(root.get("hologramHue").getAsFloat());
               }

               if (root.has("hologramOpacity")) {
                  GhostBlockState.setHologramOpacity(root.get("hologramOpacity").getAsFloat());
               }

               if (root.has("hologramSaturation")) {
                  GhostBlockState.setHologramSaturation(root.get("hologramSaturation").getAsFloat());
               }

               if (root.has("hologramPassthrough")) {
                  DesignModeState.setPassthroughMode(root.get("hologramPassthrough").getAsBoolean());
               }
            } catch (Exception var15) {
               LOGGER.warn("Failed to load config from {}: {}", file.getPath(), var15.getMessage());
            }
         }
      }
   }

   public static void save() {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc != null) {
         File file = new File(mc.getMinecraftDir(), "config/blueprints.json");
         File dir = file.getParentFile();
         if (!dir.exists() && !dir.mkdirs()) {
            LOGGER.warn("Failed to create config directory: {}", dir);
         } else {
            JsonObject root;
            if (file.exists()) {
               try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                  root = new JsonParser().parse(reader).getAsJsonObject();
               } catch (Exception var37) {
                  root = new JsonObject();
               }
            } else {
               root = new JsonObject();
            }

            root.addProperty("hologramHue", GhostBlockState.getHologramHue());
            root.addProperty("hologramOpacity", GhostBlockState.getHologramOpacity());
            root.addProperty("hologramSaturation", GhostBlockState.getHologramSaturation());
            root.addProperty("hologramPassthrough", DesignModeState.isPassthroughMode());

            try (Writer writer = new BufferedWriter(new FileWriter(file))) {
               writer.write("{\n");
               String[] keys = root.keySet().toArray(new String[0]);

               for (int i = 0; i < keys.length; i++) {
                  String key = keys[i];
                  writer.write("  \"" + key + "\": " + root.get(key));
                  if (i < keys.length - 1) {
                     writer.write(",");
                  }

                  writer.write("\n");
               }

               writer.write("}\n");
            } catch (Exception var35) {
               LOGGER.warn("Failed to save config to {}: {}", file.getPath(), var35.getMessage());
            }
         }
      }
   }
}
