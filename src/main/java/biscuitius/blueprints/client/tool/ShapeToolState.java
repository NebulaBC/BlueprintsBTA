package biscuitius.blueprints.client.tool;

import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.HologramStore;
import biscuitius.blueprints.client.preview.PreviewStore;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.world.World;

public final class ShapeToolState {
   private static final Map<World, ShapeToolState.State> STATES = new IdentityHashMap<>();

   private ShapeToolState() {
   }

   private static ShapeToolState.State state(World world, boolean create) {
      if (world == null) {
         return null;
      }

      ShapeToolState.State s = STATES.get(world);
      if (s == null && create) {
         s = new ShapeToolState.State();
         STATES.put(world, s);
      }

      return s;
   }

   public static void setInk(World world, int blockId, int meta) {
      ShapeToolState.State s = state(world, true);
      if (s != null) {
         s.inkBlockId = blockId;
         s.inkMeta = meta;
         recompute(world, s.shape);
      }
   }

   public static boolean hasInk(World world) {
      ShapeToolState.State s = state(world, false);
      return s != null && s.inkBlockId > 0;
   }

   public static int getInkBlockId(World world) {
      ShapeToolState.State s = state(world, false);
      return s == null ? -1 : s.inkBlockId;
   }

   public static int getInkMeta(World world) {
      ShapeToolState.State s = state(world, false);
      return s == null ? 0 : s.inkMeta;
   }

   public static void setPointA(World world, int x, int y, int z, ShapeToolState.Shape shape) {
      ShapeToolState.State s = state(world, true);
      if (s != null) {
         s.pointA = new int[]{x, y, z};
         s.shape = shape;
      }

      recompute(world, shape);
   }

   public static void setPointB(World world, int x, int y, int z, ShapeToolState.Shape shape) {
      ShapeToolState.State s = state(world, true);
      if (s != null) {
         s.pointB = new int[]{x, y, z};
         s.shape = shape;
      }

      recompute(world, shape);
   }

   public static ShapeToolState.Shape getActiveShape(World world) {
      ShapeToolState.State s = state(world, false);
      return s == null ? ShapeToolState.Shape.LINE : s.shape;
   }

   public static int[] getPointA(World world) {
      ShapeToolState.State s = state(world, false);
      return s == null ? null : s.pointA;
   }

   public static int[] getPointB(World world) {
      ShapeToolState.State s = state(world, false);
      return s == null ? null : s.pointB;
   }

   public static boolean hasBothPoints(World world) {
      ShapeToolState.State s = state(world, false);
      return s != null && s.pointA != null && s.pointB != null;
   }

   public static boolean hasAnyPoint(World world) {
      ShapeToolState.State s = state(world, false);
      return s != null && (s.pointA != null || s.pointB != null);
   }

   public static void recompute(World world, ShapeToolState.Shape shape) {
      ShapeToolState.State s = state(world, false);
      if (s != null && s.pointA != null && s.pointB != null && s.inkBlockId > 0) {
         List<int[]> positions = switch (shape) {
            case FILL -> ShapeGenerator.fill(s.pointA, s.pointB);
            case RECTANGLE -> ShapeGenerator.rectangle(s.pointA, s.pointB);
            case OVAL -> ShapeGenerator.oval(s.pointA, s.pointB);
            default -> ShapeGenerator.line(s.pointA, s.pointB);
         };
         Map<Long, HologramBlock> preview = new LinkedHashMap<>(positions.size() * 2);
         HologramBlock block = new HologramBlock(s.inkBlockId, s.inkMeta);

         for (int[] p : positions) {
            preview.put(HologramStore.packPos(p[0], p[1], p[2]), block);
         }

         PreviewStore.setPreview(world, preview);
      } else {
         PreviewStore.clear(world);
      }
   }

   public static int commit(World world) {
      ShapeToolState.State s = state(world, false);
      if (s == null) {
         return 0;
      }

      Map<Long, HologramBlock> preview = PreviewStore.rawView(world);
      if (preview.isEmpty()) {
         return 0;
      }

      int count = 0;

      for (Entry<Long, HologramBlock> e : preview.entrySet()) {
         long packed = e.getKey();
         HologramStore.put(world, HologramStore.unpackX(packed), HologramStore.unpackY(packed), HologramStore.unpackZ(packed), e.getValue());
         count++;
      }

      PreviewStore.clear(world);
      s.pointA = null;
      s.pointB = null;
      return count;
   }

   public static void clearPoints(World world) {
      ShapeToolState.State s = state(world, false);
      if (s != null) {
         s.pointA = null;
         s.pointB = null;
      }

      PreviewStore.clear(world);
   }

   public static void clear(World world) {
      STATES.remove(world);
      PreviewStore.clear(world);
   }

   public static void clearAll() {
      STATES.clear();
      PreviewStore.clearAll();
   }

   public enum Shape {
      LINE,
      FILL,
      RECTANGLE,
      OVAL;
   }

   private static final class State {
      int[] pointA;
      int[] pointB;
      int inkBlockId = -1;
      int inkMeta;
      ShapeToolState.Shape shape = ShapeToolState.Shape.LINE;
   }
}
