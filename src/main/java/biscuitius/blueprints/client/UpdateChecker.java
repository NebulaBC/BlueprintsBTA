package biscuitius.blueprints.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.net.command.TextFormatting;
import net.minecraft.core.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UpdateChecker {
   private static final Logger LOGGER = LoggerFactory.getLogger("blueprints-update");
   private static final String API_URL = "https://api.modrinth.com/v2/project/blueprints-bta/version";
   private static final String PROJECT_URL = "https://modrinth.com/mod/blueprints-bta";
   private static final AtomicBoolean FETCH_STARTED = new AtomicBoolean(false);
   private static volatile String pendingMessage;
   private static World lastWorld;
   private static boolean notifiedForWorld;

   private UpdateChecker() {
   }

   public static void tick(Minecraft mc) {
      if (mc != null) {
         World world = mc.currentWorld;
         if (world == null) {
            lastWorld = null;
            notifiedForWorld = false;
         } else {
            if (world != lastWorld) {
               lastWorld = world;
               notifiedForWorld = false;
            }

            if (FETCH_STARTED.compareAndSet(false, true)) {
               startAsyncCheck();
            }

            if (!notifiedForWorld && pendingMessage != null && mc.thePlayer != null && mc.hudIngame != null) {
               mc.hudIngame.addChatMessage(pendingMessage);
               notifiedForWorld = true;
            }
         }
      }
   }

   private static void startAsyncCheck() {
      Thread thread = new Thread(UpdateChecker::runCheck, "blueprints-update-check");
      thread.setDaemon(true);
      thread.start();
   }

   private static void runCheck() {
      try {
         String installed = installedModVersion();
         if (installed == null) {
            LOGGER.warn("Could not determine installed mod version; skipping update check.");
            return;
         }

         int[] installedMod = parseParts(modPart(installed));
         int[] runningBta = parseParts("8.0");
         String json = fetch("https://api.modrinth.com/v2/project/blueprints-bta/version");
         if (json == null) {
            return;
         }

         JsonElement root = new JsonParser().parse(json);
         if (!root.isJsonArray()) {
            return;
         }

         JsonArray versions = root.getAsJsonArray();
         String bestVersionNumber = null;
         int[] bestMod = null;

         for (JsonElement element : versions) {
            if (element.isJsonObject()) {
               JsonObject entry = element.getAsJsonObject();
               if ((!entry.has("version_type") || "release".equals(entry.get("version_type").getAsString()))
                  && (!entry.has("status") || "listed".equals(entry.get("status").getAsString()))
                  && entry.has("version_number")) {
                  String versionNumber = entry.get("version_number").getAsString();
                  int[] entryBta = parseParts(btaPart(versionNumber));
                  if (compare(entryBta, runningBta) <= 0) {
                     int[] entryMod = parseParts(modPart(versionNumber));
                     if (bestMod == null || compare(entryMod, bestMod) > 0) {
                        bestMod = entryMod;
                        bestVersionNumber = versionNumber;
                     }
                  }
               }
            }
         }

         if (bestMod != null && compare(bestMod, installedMod) > 0) {
            pendingMessage = buildMessage(installed, bestVersionNumber);
            LOGGER.info("Update available: {} (installed {})", bestVersionNumber, installed);
         } else {
            LOGGER.info("Blueprints is up to date (installed {}).", installed);
         }
      } catch (Exception e) {
         LOGGER.warn("Update check failed: {}", e.toString());
      }
   }

   private static String buildMessage(String installed, String latest) {
      return TextFormatting.LIGHT_BLUE + "A new version of Blueprints is available: " + latest;
   }

   private static String installedModVersion() {
      try {
         return FabricLoader.getInstance()
            .getModContainer("blueprints")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse(null);
      } catch (Throwable t) {
         return null;
      }
   }

   private static String fetch(String urlString) {
      HttpURLConnection connection = null;

      try {
         URL url = new URL(urlString);
         connection = (HttpURLConnection)url.openConnection();
         connection.setRequestMethod("GET");
         connection.setConnectTimeout(8000);
         connection.setReadTimeout(8000);
         connection.setRequestProperty("Accept", "application/json");
         connection.setRequestProperty("User-Agent", "Biscuitius/blueprints-bta (Modrinth update check)");
         int code = connection.getResponseCode();
         if (code != 200) {
            LOGGER.warn("Modrinth returned HTTP {} for update check.", code);
            return null;
         }

         try (
            InputStream in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
         ) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[4096];

            int read;
            while ((read = reader.read(buffer)) != -1) {
               sb.append(buffer, 0, read);
            }

            return sb.toString();
         }
      } catch (Exception e) {
         LOGGER.warn("Could not reach Modrinth for update check: {}", e.toString());
         return null;
      } finally {
         if (connection != null) {
            connection.disconnect();
         }
      }
   }

   static String modPart(String version) {
      int dash = version.indexOf(45);
      return dash >= 0 ? version.substring(0, dash) : version;
   }

   static String btaPart(String version) {
      int dash = version.indexOf(45);
      return dash >= 0 ? version.substring(dash + 1) : "";
   }

   static int[] parseParts(String version) {
      if (version == null) {
         return new int[0];
      }

      List<Integer> parts = new ArrayList<>();

      for (String token : version.split("[^0-9]+")) {
         if (!token.isEmpty()) {
            try {
               parts.add(Integer.parseInt(token));
            } catch (NumberFormatException var7) {
            }
         }
      }

      int[] result = new int[parts.size()];

      for (int i = 0; i < result.length; i++) {
         result[i] = parts.get(i);
      }

      return result;
   }

   static int compare(int[] a, int[] b) {
      int length = Math.max(a.length, b.length);

      for (int i = 0; i < length; i++) {
         int left = i < a.length ? a[i] : 0;
         int right = i < b.length ? b[i] : 0;
         if (left != right) {
            return Integer.compare(left, right);
         }
      }

      return 0;
   }
}
