package biscuitius.blueprints.client.preview;

import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.hologram.HologramAppearance;
import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.HologramRenderer;
import biscuitius.blueprints.client.hologram.HologramStore;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.render.block.model.BlockModel;
import net.minecraft.client.render.block.model.BlockModelDispatcher;
import net.minecraft.client.render.renderer.BlendFactor;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.renderer.Shaders;
import net.minecraft.client.render.renderer.State;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;

public final class PreviewRenderer {
   private static final TilePos SCRATCH = new TilePos();

   private PreviewRenderer() {
   }

   public static void render(World world, int renderPass, double cameraX, double cameraY, double cameraZ) {
      if (world != null) {
         if (DesignModeState.isActive()) {
            if (!HologramAppearance.isHidden()) {
               if (PreviewStore.hasEntries(world)) {
                  int[] bounds = PreviewStore.getBounds(world);
                  if (bounds != null) {
                     PreviewChunkCache cache = new PreviewChunkCache(
                        world, bounds[0] - 1, Math.max(0, bounds[1] - 1), bounds[2] - 1, bounds[3] + 1, Math.min(255, bounds[4] + 1), bounds[5] + 1
                     );
                     GLRenderer.pushFrame();
                     GLRenderer.setShader(Shaders.ITEM);
                     TextureRegistry.worldAtlas.bind();
                     GLRenderer.enableState(State.BLEND);
                     GLRenderer.setBlendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
                     GLRenderer.setDepthMask(renderPass == 0);
                     GLRenderer.setAlphaTest(0.0F);
                     GLRenderer.setColor4f(
                        HologramAppearance.getR() / 255.0F,
                        HologramAppearance.getG() / 255.0F,
                        HologramAppearance.getB() / 255.0F,
                        HologramAppearance.getOpacity()
                     );
                     GLRenderer.modelM4f().translate((float)(-cameraX), (float)(-cameraY), (float)(-cameraZ));
                     TessellatorGeneral tess = GLRenderer.getTessellator();
                     HologramRenderer.HOLOGRAM_PASS_ACTIVE = true;
                     HologramRenderer.PREVIEW_PASS_ACTIVE = true;

                     try {
                        tess.startDrawingQuads();
                        Map<Long, HologramBlock> preview = PreviewStore.rawView(world);

                        for (Entry<Long, HologramBlock> e : preview.entrySet()) {
                           long key = e.getKey();
                           int y = HologramStore.unpackY(key);
                           if (HologramAppearance.isYVisible(y)) {
                              HologramBlock h = e.getValue();
                              int blockId = h.blockId;
                              if (blockId > 0 && blockId < Blocks.blocksList.length) {
                                 Block<?> block = Blocks.blocksList[blockId];
                                 if (block != null) {
                                    BlockModel<?> model = (BlockModel<?>)BlockModelDispatcher.getInstance().getDispatch(block);
                                    if (model != null && model.renderLayer() == renderPass) {
                                       SCRATCH.x = HologramStore.unpackX(key);
                                       SCRATCH.y = y;
                                       SCRATCH.z = HologramStore.unpackZ(key);
                                       model.render(tess, cache, SCRATCH);
                                    }
                                 }
                              }
                           }
                        }

                        tess.draw();
                     } finally {
                        HologramRenderer.HOLOGRAM_PASS_ACTIVE = false;
                        HologramRenderer.PREVIEW_PASS_ACTIVE = false;
                        GLRenderer.popFrame();
                     }
                  }
               }
            }
         }
      }
   }
}
