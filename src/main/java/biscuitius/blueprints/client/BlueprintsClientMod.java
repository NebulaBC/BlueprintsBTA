package biscuitius.blueprints.client;

import biscuitius.blueprints.client.hologram.HologramCache;
import biscuitius.blueprints.client.hologram.HologramRenderer;
import biscuitius.blueprints.client.item.BlueprintItem;
import biscuitius.blueprints.client.item.ClipboardToolItem;
import biscuitius.blueprints.client.item.FillToolItem;
import biscuitius.blueprints.client.item.LineToolItem;
import biscuitius.blueprints.client.item.MoveToolItem;
import biscuitius.blueprints.client.item.OvalToolItem;
import biscuitius.blueprints.client.item.RectangleToolItem;
import biscuitius.blueprints.client.item.ReplaceToolItem;
import biscuitius.blueprints.client.item.RotateToolItem;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.core.lang.Language;
import net.minecraft.core.lang.Language.Default;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlueprintsClientMod implements ClientModInitializer {
   private static final Logger LOGGER = LoggerFactory.getLogger("blueprints-client");

   public void onInitializeClient() {
      loadTranslations();
      BlueprintsConfig.load();
      BlueprintItem.register();
      MoveToolItem.register();
      RotateToolItem.register();
      LineToolItem.register();
      ClipboardToolItem.register();
      FillToolItem.register();
      ReplaceToolItem.register();
      RectangleToolItem.register();
      OvalToolItem.register();
      HologramRenderer.install();
      HologramCache.install();
      LOGGER.info("Blueprints client features initialized.");
   }

   private static void loadTranslations() {
      try (InputStream in = BlueprintsClientMod.class.getResourceAsStream("/assets/blueprints/lang/en_US.lang")) {
         if (in != null) {
            Properties modEntries = new Properties();
            modEntries.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            Field entriesField = Language.class.getDeclaredField("entries");
            entriesField.setAccessible(true);
            Properties defaultEntries = (Properties)entriesField.get(Default.INSTANCE);
            defaultEntries.putAll(modEntries);
            LOGGER.info("Loaded {} translation key(s) from blueprints.lang", modEntries.size());
         } else {
            LOGGER.warn("Could not find /assets/blueprints/lang/en_US.lang on classpath");
         }
      } catch (Exception e) {
         LOGGER.error("Failed to load Blueprints translations", e);
      }
   }
}
