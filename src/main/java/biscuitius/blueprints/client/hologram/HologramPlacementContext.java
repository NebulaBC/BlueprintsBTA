package biscuitius.blueprints.client.hologram;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.world.World;

public final class HologramPlacementContext {
   private static volatile HologramPlacementContext.Mode mode = HologramPlacementContext.Mode.NONE;
   private static volatile World activeWorld;
   private static final HashMap<Long, int[]> captureMap = new HashMap<>();
   private static final HashSet<Long> touchedPositions = new HashSet<>();

   private HologramPlacementContext() {
   }

   public static void begin(World world) {
      activeWorld = world;
      touchedPositions.clear();
      mode = HologramPlacementContext.Mode.NORMAL;
   }

   public static void beginDryRun(World world) {
      activeWorld = world;
      captureMap.clear();
      touchedPositions.clear();
      mode = HologramPlacementContext.Mode.DRY_RUN;
   }

   public static void end() {
      mode = HologramPlacementContext.Mode.NONE;
      activeWorld = null;
      captureMap.clear();
      touchedPositions.clear();
   }

   public static boolean isActive() {
      return mode != HologramPlacementContext.Mode.NONE;
   }

   public static boolean isActive(World world) {
      return mode != HologramPlacementContext.Mode.NONE && world == activeWorld;
   }

   public static boolean isNormal(World world) {
      return mode == HologramPlacementContext.Mode.NORMAL && world == activeWorld;
   }

   public static boolean isDryRun() {
      return mode == HologramPlacementContext.Mode.DRY_RUN;
   }

   public static boolean isDryRun(World world) {
      return mode == HologramPlacementContext.Mode.DRY_RUN && world == activeWorld;
   }

   public static void captureWrite(int x, int y, int z, int id, int meta) {
      captureMap.put(key(x, y, z), new int[]{id, meta});
   }

   public static int[] captureRead(int x, int y, int z) {
      return captureMap.get(key(x, y, z));
   }

   public static void recordTouch(int x, int y, int z) {
      if (mode == HologramPlacementContext.Mode.NORMAL) {
         touchedPositions.add(key(x, y, z));
      }
   }

   public static Set<Long> snapshotTouchedPositions() {
      return new HashSet<>(touchedPositions);
   }

   private static long key(int x, int y, int z) {
      return (long)(x & 67108863) << 38 | (long)(z & 67108863) << 12 | y & 4095;
   }

   public enum Mode {
      NONE,
      NORMAL,
      DRY_RUN;
   }
}
