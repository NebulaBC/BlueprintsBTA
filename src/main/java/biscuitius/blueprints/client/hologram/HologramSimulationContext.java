package biscuitius.blueprints.client.hologram;

import java.util.function.Supplier;
import net.minecraft.core.world.World;

public final class HologramSimulationContext {
   private static volatile World activeWorld;
   private static volatile int depth;
   private static volatile boolean previousClientSide;

   private HologramSimulationContext() {
   }

   public static boolean isActive() {
      return depth > 0;
   }

   public static boolean isActive(World world) {
      return depth > 0 && world == activeWorld;
   }

   public static void begin(World world) {
      if (world != null) {
         if (depth > 0) {
            if (world != activeWorld) {
               throw new IllegalStateException("Nested hologram simulation for a different world");
            }

            depth++;
         } else {
            activeWorld = world;
            previousClientSide = world.isClientSide;
            world.isClientSide = false;
            depth = 1;
         }
      }
   }

   public static void end() {
      if (depth > 0) {
         if (--depth <= 0) {
            World world = activeWorld;
            activeWorld = null;
            if (world != null) {
               world.isClientSide = previousClientSide;
            }
         }
      }
   }

   public static void run(World world, Runnable action) {
      if (world != null && action != null) {
         begin(world);

         try {
            action.run();
         } finally {
            end();
         }
      }
   }

   public static <T> T call(World world, Supplier<T> action) {
      if (world != null && action != null) {
         begin(world);

         try {
            return action.get();
         } finally {
            end();
         }
      } else {
         return null;
      }
   }
}
