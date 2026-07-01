package biscuitius.blueprints.client.hologram;

import biscuitius.blueprints.mixin.client.MinecraftAccessor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HologramCache implements HologramListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("blueprints-cache");
   private static final byte[] MAGIC = new byte[]{66, 80, 67};
   private static final byte VERSION = 1;
   private static final int FLUSH_INTERVAL_TICKS = 20;
   private static final HologramCache INSTANCE = new HologramCache();
   private final Map<World, Boolean> dirty = new IdentityHashMap<>();
   private int tickCounter;
   private final Map<World, String> scopeAtFirstWrite = new IdentityHashMap<>();
   private final Map<World, String> loadedScopes = new IdentityHashMap<>();

   private HologramCache() {
   }

   public static HologramCache get() {
      return INSTANCE;
   }

   public static void install() {
      HologramStore.addListener(INSTANCE);
   }

   @Override
   public void onHologramChanged(World world, int x, int y, int z, HologramBlock previous, HologramBlock current) {
      if (world != null) {
         this.dirty.put(world, Boolean.TRUE);
      }
   }

   @Override
   public void onRegionChanged(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      if (world != null) {
         this.dirty.put(world, Boolean.TRUE);
      }
   }

   @Override
   public void onWorldCleared(World world) {
      if (world != null) {
         this.dirty.put(world, Boolean.TRUE);
      }
   }

   public static void tick(Minecraft mc) {
      if (mc != null) {
         World world = mc.currentWorld;
         if (world != null) {
            INSTANCE.maybeLoad(mc, world);
         }

         INSTANCE.tickCounter++;
         if (INSTANCE.tickCounter >= 20) {
            INSTANCE.tickCounter = 0;
            if (!INSTANCE.dirty.isEmpty()) {
               World[] toSave = INSTANCE.dirty.keySet().toArray(new World[0]);
               INSTANCE.dirty.clear();

               for (World w : toSave) {
                  INSTANCE.save(mc, w);
               }
            }
         }
      }
   }

   private void maybeLoad(Minecraft mc, World world) {
      String scope = scopeFor(mc);
      if (scope != null) {
         String previous = this.loadedScopes.get(world);
         if (!scope.equals(previous)) {
            this.loadedScopes.put(world, scope);
            if (!HologramStore.hasEntries(world)) {
               this.load(scope, world);
            }
         }
      }
   }

   private void save(Minecraft mc, World world) {
      String scope = this.scopeAtFirstWrite.get(world);
      if (scope == null) {
         scope = scopeFor(mc);
      }

      if (scope != null) {
         int dim = world.dimension == null ? 0 : world.dimension.id;
         File file = resolveCacheFile(scope, dim);
         if (file != null) {
            int size = HologramStore.size(world);
            if (size == 0) {
               if (file.isFile()) {
                  file.delete();
               }

               this.scopeAtFirstWrite.remove(world);
            } else {
               this.scopeAtFirstWrite.put(world, scope);
               File parent = file.getParentFile();
               if (parent != null && !parent.exists() && !parent.mkdirs()) {
                  LOGGER.warn("Failed to create cache directory {}", parent);
               } else {
                  try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
                     out.write(MAGIC);
                     out.writeByte(1);
                     out.writeInt(size);
                     HologramStore.forEach(world, (x, y, z, h) -> {
                        try {
                           out.writeInt(x);
                           out.writeInt(y);
                           out.writeInt(z);
                           out.writeShort((short)h.blockId);
                           out.writeShort((short)h.metadata);
                        } catch (Exception var6x) {
                           throw new RuntimeException(var6x);
                        }
                     });
                  } catch (Exception var21) {
                     LOGGER.warn("Failed to write hologram cache {}: {}", file, var21.getMessage());
                     file.delete();
                  }
               }
            }
         }
      }
   }

   private void load(String scope, World world) {
      int dim = world.dimension == null ? 0 : world.dimension.id;
      File file = resolveCacheFile(scope, dim);
      if (file != null && file.isFile()) {
         try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            byte[] magic = new byte[3];
            in.readFully(magic);
            if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1] || magic[2] != MAGIC[2]) {
               LOGGER.warn("Bad hologram cache magic in {}", file);
            } else {
               int version = in.readByte() & 255;
               if (version != 1) {
                  LOGGER.warn("Unsupported hologram cache version {} in {}", version, file);
               } else {
                  int count = in.readInt();
                  if (count >= 0 && count <= 16000000) {
                     for (int i = 0; i < count; i++) {
                        int x = in.readInt();
                        int y = in.readInt();
                        int z = in.readInt();
                        int blockId = in.readShort() & '\uffff';
                        int meta = in.readShort() & '\uffff';
                        if (y >= 0 && y < 256) {
                           HologramStore.put(world, x, y, z, new HologramBlock(blockId, meta));
                        }
                     }

                     HologramStore.recomputeBounds(world);
                     this.scopeAtFirstWrite.put(world, scope);
                     this.dirty.remove(world);
                  }
               }
            }
         } catch (Exception var29) {
            LOGGER.warn("Failed to load hologram cache {}: {}", file, var29.getMessage());
         }
      }
   }

   private static String scopeFor(Minecraft mc) {
      if (mc == null) {
         return null;
      } else {
         if (mc.currentWorld != null && mc.currentWorld.saveHandler != null) {
            String dir = readWorldDirName(mc.currentWorld.saveHandler);
            if (dir != null && !dir.isEmpty()) {
               return "sp_" + sanitise(dir);
            }
         }

         MinecraftAccessor acc = (MinecraftAccessor)mc;
         String server = acc.getServerName();
         return server != null && !server.isEmpty() ? "mp_" + sanitise(server) + "_" + acc.getServerPort() : null;
      }
   }

   private static String readWorldDirName(Object saveHandler) {
      Class<?> c = saveHandler.getClass();

      while (c != null) {
         try {
            Field f = c.getDeclaredField("worldDirName");
            f.setAccessible(true);
            Object v = f.get(saveHandler);
            return v == null ? null : v.toString();
         } catch (NoSuchFieldException var4) {
            c = c.getSuperclass();
         } catch (Exception var5) {
            return null;
         }
      }

      return null;
   }

   private static String sanitise(String raw) {
      return raw.replaceAll("[\\\\/:*?\"<>|]", "_");
   }

   private static File resolveCacheFile(String scope, int dimensionId) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc == null) {
         return null;
      } else {
         File root = new File(mc.getMinecraftDir(), "blueprints");
         File cacheDir = new File(new File(root, "cache"), scope);
         return new File(cacheDir, "dim_" + dimensionId + ".cache");
      }
   }
}
