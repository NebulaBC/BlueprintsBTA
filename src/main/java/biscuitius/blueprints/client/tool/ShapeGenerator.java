package biscuitius.blueprints.client.tool;

import java.util.ArrayList;
import java.util.List;

public final class ShapeGenerator {
   private ShapeGenerator() {
   }

   public static List<int[]> line(int[] a, int[] b) {
      List<int[]> out = new ArrayList<>();
      if (a != null && b != null) {
         int x0 = a[0];
         int y0 = a[1];
         int z0 = a[2];
         int x1 = b[0];
         int y1 = b[1];
         int z1 = b[2];
         int dx = Math.abs(x1 - x0);
         int dy = Math.abs(y1 - y0);
         int dz = Math.abs(z1 - z0);
         int sx = x0 < x1 ? 1 : -1;
         int sy = y0 < y1 ? 1 : -1;
         int sz = z0 < z1 ? 1 : -1;
         int x = x0;
         int y = y0;
         int z = z0;
         if (dx >= dy && dx >= dz) {
            int errY = 2 * dy - dx;
            int errZ = 2 * dz - dx;

            for (int i = 0; i <= dx; i++) {
               out.add(new int[]{x, y, z});
               if (errY > 0) {
                  y += sy;
                  errY -= 2 * dx;
               }

               if (errZ > 0) {
                  z += sz;
                  errZ -= 2 * dx;
               }

               errY += 2 * dy;
               errZ += 2 * dz;
               x += sx;
            }
         } else if (dy >= dx && dy >= dz) {
            int errX = 2 * dx - dy;
            int errZ = 2 * dz - dy;

            for (int i = 0; i <= dy; i++) {
               out.add(new int[]{x, y, z});
               if (errX > 0) {
                  x += sx;
                  errX -= 2 * dy;
               }

               if (errZ > 0) {
                  z += sz;
                  errZ -= 2 * dy;
               }

               errX += 2 * dx;
               errZ += 2 * dz;
               y += sy;
            }
         } else {
            int errX = 2 * dx - dz;
            int errY = 2 * dy - dz;

            for (int i = 0; i <= dz; i++) {
               out.add(new int[]{x, y, z});
               if (errX > 0) {
                  x += sx;
                  errX -= 2 * dz;
               }

               if (errY > 0) {
                  y += sy;
                  errY -= 2 * dz;
               }

               errX += 2 * dx;
               errY += 2 * dy;
               z += sz;
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public static List<int[]> fill(int[] a, int[] b) {
      List<int[]> out = new ArrayList<>();
      if (a != null && b != null) {
         int minX = Math.min(a[0], b[0]);
         int maxX = Math.max(a[0], b[0]);
         int minY = Math.min(a[1], b[1]);
         int maxY = Math.max(a[1], b[1]);
         int minZ = Math.min(a[2], b[2]);
         int maxZ = Math.max(a[2], b[2]);

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  out.add(new int[]{x, y, z});
               }
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public static List<int[]> rectangle(int[] a, int[] b) {
      List<int[]> out = new ArrayList<>();
      if (a != null && b != null) {
         int minX = Math.min(a[0], b[0]);
         int maxX = Math.max(a[0], b[0]);
         int minY = Math.min(a[1], b[1]);
         int maxY = Math.max(a[1], b[1]);
         int minZ = Math.min(a[2], b[2]);
         int maxZ = Math.max(a[2], b[2]);

         for (int y = minY; y <= maxY; y++) {
            if (minX == maxX && minZ == maxZ) {
               out.add(new int[]{minX, y, minZ});
            } else if (minX == maxX) {
               for (int z = minZ; z <= maxZ; z++) {
                  out.add(new int[]{minX, y, z});
               }
            } else if (minZ == maxZ) {
               for (int x = minX; x <= maxX; x++) {
                  out.add(new int[]{x, y, minZ});
               }
            } else {
               for (int x = minX; x <= maxX; x++) {
                  out.add(new int[]{x, y, minZ});
                  out.add(new int[]{x, y, maxZ});
               }

               for (int z = minZ + 1; z < maxZ; z++) {
                  out.add(new int[]{minX, y, z});
                  out.add(new int[]{maxX, y, z});
               }
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public static List<int[]> oval(int[] a, int[] b) {
      List<int[]> out = new ArrayList<>();
      if (a != null && b != null) {
         int minX = Math.min(a[0], b[0]);
         int maxX = Math.max(a[0], b[0]);
         int minY = Math.min(a[1], b[1]);
         int maxY = Math.max(a[1], b[1]);
         int minZ = Math.min(a[2], b[2]);
         int maxZ = Math.max(a[2], b[2]);
         double radiusX = (maxX - minX + 1) * 0.5;
         double radiusZ = (maxZ - minZ + 1) * 0.5;
         double centerX = minX + radiusX - 0.5;
         double centerZ = minZ + radiusZ - 0.5;
         double invRx2 = 1.0 / Math.max(0.25, radiusX * radiusX);
         double invRz2 = 1.0 / Math.max(0.25, radiusZ * radiusZ);

         for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
               for (int z = minZ; z <= maxZ; z++) {
                  if (isInsideOval(x, z, centerX, centerZ, invRx2, invRz2)
                     && (
                        !isInsideOval(x - 1, z, centerX, centerZ, invRx2, invRz2)
                           || !isInsideOval(x + 1, z, centerX, centerZ, invRx2, invRz2)
                           || !isInsideOval(x, z - 1, centerX, centerZ, invRx2, invRz2)
                           || !isInsideOval(x, z + 1, centerX, centerZ, invRx2, invRz2)
                     )) {
                     out.add(new int[]{x, y, z});
                  }
               }
            }
         }

         return out;
      } else {
         return out;
      }
   }

   private static boolean isInsideOval(int x, int z, double centerX, double centerZ, double invRx2, double invRz2) {
      double dx = x - centerX;
      double dz = z - centerZ;
      return dx * dx * invRx2 + dz * dz * invRz2 <= 1.0;
   }
}
