package biscuitius.blueprints.compat;

import biscuitius.blueprints.client.hologram.HologramStore;
import net.minecraft.client.Minecraft;
import net.minecraft.core.util.phys.HitResult.Tile;

public final class BTWailaCompat {
   private BTWailaCompat() {
   }

   public static boolean shouldSkipForHologramOnlyTile(Minecraft minecraft) {
      if (minecraft != null && minecraft.currentWorld != null) {
         if (!(minecraft.objectMouseOver instanceof Tile tile)) {
            return false;
         } else {
            int x = tile.tilePos.x();
            int y = tile.tilePos.y();
            int z = tile.tilePos.z();
            return minecraft.currentWorld.getBlockId(x, y, z) != 0 ? false : HologramStore.get(minecraft.currentWorld, x, y, z) != null;
         }
      } else {
         return false;
      }
   }
}
