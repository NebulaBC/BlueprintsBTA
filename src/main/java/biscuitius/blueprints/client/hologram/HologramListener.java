package biscuitius.blueprints.client.hologram;

import net.minecraft.core.world.World;

public interface HologramListener {
   void onHologramChanged(World var1, int var2, int var3, int var4, HologramBlock var5, HologramBlock var6);

   void onRegionChanged(World var1, int var2, int var3, int var4, int var5, int var6, int var7);

   void onWorldCleared(World var1);
}
