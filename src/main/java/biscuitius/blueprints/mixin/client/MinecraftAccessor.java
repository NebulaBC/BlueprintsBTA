package biscuitius.blueprints.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
   @Accessor("toggleFlyPressed")
   void setToggleFlyPressed(boolean var1);

   @Accessor("mouseTicksRan")
   void setMouseTicksRan(int var1);

   @Accessor("ticksRan")
   int getTicksRan();

   @Accessor("serverName")
   String getServerName();

   @Accessor("serverPort")
   int getServerPort();
}
