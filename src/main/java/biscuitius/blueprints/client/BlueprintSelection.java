package biscuitius.blueprints.client;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.world.World;

public final class BlueprintSelection {
   private static final Map<World, BlueprintSelection.State> STATES = new IdentityHashMap<>();

   private BlueprintSelection() {
   }

   private static BlueprintSelection.State state(World world, boolean create) {
      if (world == null) {
         return null;
      } else {
         BlueprintSelection.State s = STATES.get(world);
         if (s == null && create) {
            s = new BlueprintSelection.State();
            STATES.put(world, s);
         }

         return s;
      }
   }

   public static void setCornerA(World world, int x, int y, int z) {
      BlueprintSelection.State s = state(world, true);
      if (s != null) {
         s.cornerA = new int[]{x, y, z};
      }
   }

   public static void setCornerB(World world, int x, int y, int z) {
      BlueprintSelection.State s = state(world, true);
      if (s != null) {
         s.cornerB = new int[]{x, y, z};
      }
   }

   public static int[] getCornerA(World world) {
      BlueprintSelection.State s = state(world, false);
      return s == null ? null : s.cornerA;
   }

   public static int[] getCornerB(World world) {
      BlueprintSelection.State s = state(world, false);
      return s == null ? null : s.cornerB;
   }

   public static BlueprintSelection.Box getBox(World world) {
      BlueprintSelection.State s = state(world, false);
      if (s != null && s.cornerA != null && s.cornerB != null) {
         int[] a = s.cornerA;
         int[] b = s.cornerB;
         return new BlueprintSelection.Box(
            Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]), Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])
         );
      } else {
         return null;
      }
   }

   public static boolean hasAny(World world) {
      BlueprintSelection.State s = state(world, false);
      return s != null && (s.cornerA != null || s.cornerB != null);
   }

   public static void clear(World world) {
      STATES.remove(world);
   }

   public static void clearAll() {
      STATES.clear();
   }

   public static final class Box {
      public final int minX;
      public final int minY;
      public final int minZ;
      public final int maxX;
      public final int maxY;
      public final int maxZ;

      public Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
         this.minX = minX;
         this.minY = minY;
         this.minZ = minZ;
         this.maxX = maxX;
         this.maxY = maxY;
         this.maxZ = maxZ;
      }
   }

   private static final class State {
      int[] cornerA;
      int[] cornerB;

      private State() {
      }
   }
}
