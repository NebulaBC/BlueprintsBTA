package biscuitius.blueprints.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
   @Accessor("toggleFlyPressed")
   void setToggleFlyPressed(boolean var1);
}
