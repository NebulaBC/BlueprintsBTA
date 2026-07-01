package biscuitius.blueprints.compat;

import biscuitius.blueprints.client.hologram.HologramStore;
import net.minecraft.client.Minecraft;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.HitResult.HitType;

public final class BTWailaCompat {
   private BTWailaCompat() {
   }

   public static boolean shouldSkipForHologramOnlyTile(Minecraft minecraft) {
      if (minecraft != null && minecraft.currentWorld != null) {
         HitResult hit = minecraft.objectMouseOver;
         if (hit == null || hit.hitType != HitType.TILE) {
            return false;
         } else {
            return minecraft.currentWorld.getBlockId(hit.x, hit.y, hit.z) != 0 ? false : HologramStore.get(minecraft.currentWorld, hit.x, hit.y, hit.z) != null;
         }
      } else {
         return false;
      }
   }
}
