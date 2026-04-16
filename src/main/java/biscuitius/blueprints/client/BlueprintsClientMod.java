package biscuitius.blueprints.client;

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
      LOGGER.info("Blueprints client features initialized.");
   }

   private static void loadTranslations() {
      try (InputStream in = BlueprintsClientMod.class.getResourceAsStream("/assets/blueprints/lang/en_US.lang")) {
         if (in == null) {
            LOGGER.warn("Could not find /assets/blueprints/lang/en_US.lang on classpath");
            return;
         }

         Properties modEntries = new Properties();
         modEntries.load(new InputStreamReader(in, StandardCharsets.UTF_8));
         Field entriesField = Language.class.getDeclaredField("entries");
         entriesField.setAccessible(true);
         Properties defaultEntries = (Properties)entriesField.get(Default.INSTANCE);
         defaultEntries.putAll(modEntries);
         LOGGER.info("Loaded {} translation key(s) from blueprints.lang", modEntries.size());
      } catch (Exception var16) {
         LOGGER.error("Failed to load Blueprints translations", var16);
      }
   }
}
