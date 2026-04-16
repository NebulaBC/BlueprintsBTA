package biscuitius.blueprints.mixin.client;

import biscuitius.blueprints.client.DesignModeState;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.WorldClient;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
   @Redirect(
      method = {"updateCameraAndRender", "getMouseOver", "updateSpeedFov"},
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;thePlayer:Lnet/minecraft/client/entity/player/PlayerLocal;", opcode = 180)
   )
   private PlayerLocal blueprints$routeCameraAndMousePlayer(Minecraft minecraft) {
      return DesignModeState.getControlPlayer(minecraft);
   }

   @Redirect(
      method = "getMouseOver",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/world/WorldClient;getEntitiesWithinAABBExcludingEntity(Lnet/minecraft/core/entity/Entity;Lnet/minecraft/core/util/phys/AABB;)Ljava/util/List;"
      )
   )
   private List<Entity> blueprints$skipEntityPicking(WorldClient world, Entity exclude, AABB box) {
      return DesignModeState.isActive() ? Collections.emptyList() : world.getEntitiesWithinAABBExcludingEntity(exclude, box);
   }
}
