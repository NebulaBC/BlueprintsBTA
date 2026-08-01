package biscuitius.blueprints.client.hologram;

import biscuitius.blueprints.client.BlueprintSelection;
import biscuitius.blueprints.client.DesignModeState;
import biscuitius.blueprints.client.tool.ShapeToolState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.render.TileEntityRenderDispatcher;
import net.minecraft.client.render.block.model.BlockModel;
import net.minecraft.client.render.block.model.BlockModelDispatcher;
import net.minecraft.client.render.camera.ICamera;
import net.minecraft.client.render.renderer.BlendFactor;
import net.minecraft.client.render.renderer.DrawMode;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.renderer.Shaders;
import net.minecraft.client.render.renderer.State;
import net.minecraft.client.render.tessellator.RenderBuffer;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import net.minecraft.client.render.tessellator.TessellatorShader;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.world.World;
import net.minecraft.core.world.chunk.ChunkCache;
import net.minecraft.core.world.pos.TilePos;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL41;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HologramRenderer implements HologramListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("blueprints-client");
   public static final int RENDER_PASS_SOLID = 0;
   public static final int RENDER_PASS_TRANSLUCENT = 1;
   public static final int RENDER_PASS_WRONG = 2;
   private static final int RENDER_PASSES = 3;
   private static final int MAX_REBUILDS_PER_FRAME = 8;
   private static final float HOLOGRAM_ALPHA_TEST = 0.1F;
   public static volatile boolean HOLOGRAM_PASS_ACTIVE;
   public static volatile boolean WRONG_BLOCK_PASS_ACTIVE;
   public static volatile boolean PREVIEW_PASS_ACTIVE;
   private static final int MODE_HOLOGRAM = 0;
   private static final int MODE_WRONG_ONLY = 1;
   private static final int MODE_HOLOGRAM_WRONG = 2;
   private static final int MODE_FULFILLED = 3;
   private static final HologramRenderer INSTANCE = new HologramRenderer();
   private final Map<World, Map<Long, HologramRenderer.Section>> sections = new IdentityHashMap<>();
   private final Map<World, Map<Long, TileEntity>> dummyTileEntities = new IdentityHashMap<>();
   private final Deque<HologramRenderer.Section> dirtyQueue = new ArrayDeque<>();
   private final List<Object[]> pendingFulfilledRemovals = new ArrayList<>();
   private final TilePos scratchPos = new TilePos();
   private static volatile boolean glInfoLogged;
   private static volatile boolean firstDrawLogged;
   private static volatile boolean firstRebuildLogged;

   private HologramRenderer() {
   }

   public static HologramRenderer get() {
      return INSTANCE;
   }

   private static void logGlInfoOnce() {
      if (!glInfoLogged) {
         glInfoLogged = true;

         try {
            String vendor = GL11.glGetString(7936);
            String renderer = GL11.glGetString(7937);
            String version = GL11.glGetString(7938);
            LoggerFactory.getLogger("blueprints-client")
               .info("Hologram GL context — vendor='{}' renderer='{}' version='{}'", new Object[]{vendor, renderer, version});
         } catch (Throwable var3) {
         }
      }
   }

   private static void logFirstDrawOnce(World world, Map<Long, HologramRenderer.Section> worldSections) {
      if (!firstDrawLogged) {
         firstDrawLogged = true;

         try {
            int storeEntries = HologramStore.rawView(world).size();
            int sectionCount = worldSections.size();
            int withBuffer = 0;
            int[] nonEmpty = new int[3];

            for (HologramRenderer.Section s : worldSections.values()) {
               boolean any = false;

               for (int p = 0; p < 3; p++) {
                  if (s.buffers[p] != null) {
                     nonEmpty[p]++;
                     any = true;
                  }
               }

               if (any) {
                  withBuffer++;
               }
            }

            LoggerFactory.getLogger("blueprints-client")
               .info(
                  "First hologram draw — storeEntries={} sections={} sectionsWithBuffer={} nonEmpty[solid={}, translucent={}, wrong={}] designMode={} hidden={} layerCutoffY={}",
                  new Object[]{
                     storeEntries,
                     sectionCount,
                     withBuffer,
                     nonEmpty[0],
                     nonEmpty[1],
                     nonEmpty[2],
                     DesignModeState.isActive(),
                     HologramAppearance.isHidden(),
                     HologramAppearance.getLayerCutoffY()
                  }
               );
         } catch (Throwable var10) {
         }
      }
   }

   private static void logFirstRebuildOnce(int[] vertsPerPass, boolean[] anyRenderedPerPass) {
      if (!firstRebuildLogged) {
         firstRebuildLogged = true;

         try {
            LoggerFactory.getLogger("blueprints-client")
               .info(
                  "First hologram rebuild — verts[solid={}, translucent={}, wrong={}] anyRendered[solid={}, translucent={}, wrong={}]",
                  new Object[]{vertsPerPass[0], vertsPerPass[1], vertsPerPass[2], anyRenderedPerPass[0], anyRenderedPerPass[1], anyRenderedPerPass[2]}
               );
         } catch (Throwable var3) {
         }
      }
   }

   public static void install() {
      HologramStore.addListener(INSTANCE);
   }

   private HologramRenderer.Section getOrCreate(World world, long sectionKey) {
      Map<Long, HologramRenderer.Section> worldSections = this.sections.computeIfAbsent(world, w -> new HashMap<>());
      HologramRenderer.Section s = worldSections.get(sectionKey);
      if (s == null) {
         s = new HologramRenderer.Section(world, sectionKey);
         worldSections.put(sectionKey, s);
      }

      return s;
   }

   private void enqueueDirty(HologramRenderer.Section section) {
      section.dirty = true;
      if (!section.queued) {
         section.queued = true;
         this.dirtyQueue.add(section);
      }
   }

   @Override
   public void onHologramChanged(World world, int x, int y, int z, HologramBlock previous, HologramBlock current) {
      long sectionKey = HologramStore.packSection(x, y, z);
      this.enqueueDirty(this.getOrCreate(world, sectionKey));
      this.markNeighbourDirty(world, x, y, z);
      Map<Long, TileEntity> teCache = this.dummyTileEntities.get(world);
      if (teCache != null) {
         teCache.remove(HologramStore.packPos(x, y, z));
      }
   }

   @Override
   public void onRegionChanged(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      int sxMin = minX - 1 >> 4;
      int sxMax = maxX + 1 >> 4;
      int syMin = Math.max(0, minY - 1 >> 4);
      int syMax = Math.min(15, maxY + 1 >> 4);
      int szMin = minZ - 1 >> 4;
      int szMax = maxZ + 1 >> 4;
      Map<Long, HologramRenderer.Section> worldSections = this.sections.get(world);

      for (int sx = sxMin; sx <= sxMax; sx++) {
         for (int sy = syMin; sy <= syMax; sy++) {
            for (int sz = szMin; sz <= szMax; sz++) {
               long key = HologramStore.packSection(sx << 4, sy << 4, sz << 4);
               if (worldSections != null && worldSections.containsKey(key)) {
                  this.enqueueDirty(worldSections.get(key));
               } else if (HologramStore.hasSectionHolograms(world, sx << 4, sy << 4, sz << 4)) {
                  this.enqueueDirty(this.getOrCreate(world, key));
               }
            }
         }
      }

      Map<Long, TileEntity> teCache = this.dummyTileEntities.get(world);
      if (teCache != null && !teCache.isEmpty()) {
         Iterator<Long> it = teCache.keySet().iterator();

         while (it.hasNext()) {
            long k = it.next();
            int x = HologramStore.unpackX(k);
            int y = HologramStore.unpackY(k);
            int z = HologramStore.unpackZ(k);
            if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
               it.remove();
            }
         }
      }
   }

   @Override
   public void onWorldCleared(World world) {
      Map<Long, HologramRenderer.Section> worldSections = this.sections.remove(world);
      this.dummyTileEntities.remove(world);
      if (worldSections != null) {
         for (HologramRenderer.Section s : worldSections.values()) {
            freeBuffers(s);
            s.queued = false;
         }
      }
   }

   private void markNeighbourDirty(World world, int x, int y, int z) {
      Map<Long, HologramRenderer.Section> worldSections = this.sections.get(world);
      if (worldSections != null && !worldSections.isEmpty()) {
         int lx = x & 15;
         int ly = y & 15;
         int lz = z & 15;
         if (lx == 0) {
            this.markIfExists(worldSections, x - 1, y, z);
         }

         if (lx == 15) {
            this.markIfExists(worldSections, x + 1, y, z);
         }

         if (ly == 0) {
            this.markIfExists(worldSections, x, y - 1, z);
         }

         if (ly == 15) {
            this.markIfExists(worldSections, x, y + 1, z);
         }

         if (lz == 0) {
            this.markIfExists(worldSections, x, y, z - 1);
         }

         if (lz == 15) {
            this.markIfExists(worldSections, x, y, z + 1);
         }
      }
   }

   private void markIfExists(Map<Long, HologramRenderer.Section> worldSections, int x, int y, int z) {
      HologramRenderer.Section s = worldSections.get(HologramStore.packSection(x, y, z));
      if (s != null) {
         this.enqueueDirty(s);
      }
   }

   public static void markAllDirty() {
      for (Map<Long, HologramRenderer.Section> worldSections : INSTANCE.sections.values()) {
         for (HologramRenderer.Section s : worldSections.values()) {
            INSTANCE.enqueueDirty(s);
         }
      }
   }

   public static void notifyRealBlockChanged(World world, int x, int y, int z) {
      if (world != null) {
         Map<Long, HologramRenderer.Section> worldSections = INSTANCE.sections.get(world);
         if (worldSections != null && !worldSections.isEmpty()) {
            markIfPresent(worldSections, x, y, z);
            markIfPresent(worldSections, x - 1, y, z);
            markIfPresent(worldSections, x + 1, y, z);
            markIfPresent(worldSections, x, y - 1, z);
            markIfPresent(worldSections, x, y + 1, z);
            markIfPresent(worldSections, x, y, z - 1);
            markIfPresent(worldSections, x, y, z + 1);
         }
      }
   }

   private static void markIfPresent(Map<Long, HologramRenderer.Section> worldSections, int x, int y, int z) {
      if (y >= 0 && y <= 255) {
         HologramRenderer.Section s = worldSections.get(HologramStore.packSection(x, y, z));
         if (s != null) {
            INSTANCE.enqueueDirty(s);
         }
      }
   }

   private static void freeBuffers(HologramRenderer.Section s) {
      for (int p = 0; p < 3; p++) {
         if (s.buffers[p] != null) {
            s.buffers[p].delete();
            s.buffers[p] = null;
         }
      }
   }

   public static void render(World world, int renderPass, double cameraX, double cameraY, double cameraZ) {
      if (world != null) {
         if (!HologramAppearance.isHidden()) {
            Map<Long, HologramRenderer.Section> worldSections = INSTANCE.sections.get(world);
            boolean hasSections = worldSections != null && !worldSections.isEmpty();
            if (hasSections) {
               logGlInfoOnce();
               if (renderPass == 0) {
                  INSTANCE.flushDirty();
               }

               GLRenderer.pushFrame();
               GLRenderer.setShader(Shaders.ITEM);
               TextureRegistry.worldAtlas.bind();
               GLRenderer.enableState(State.BLEND);
               GLRenderer.setBlendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
               GLRenderer.setDepthMask(renderPass == 0);
               GLRenderer.setAlphaTest(0.1F);
               if (DesignModeState.isActive()) {
                  GLRenderer.setColor4f(1.0F, 1.0F, 1.0F, 1.0F);
               } else {
                  GLRenderer.setColor4f(
                     HologramAppearance.getR() / 255.0F,
                     HologramAppearance.getG() / 255.0F,
                     HologramAppearance.getB() / 255.0F,
                     HologramAppearance.getOpacity()
                  );
               }

               GLRenderer.modelM4f().translate((float)(-cameraX), (float)(-cameraY), (float)(-cameraZ));
               if (renderPass == 0) {
                  logFirstDrawOnce(world, worldSections);
               }

               for (HologramRenderer.Section s : worldSections.values()) {
                  RenderBuffer buf = s.buffers[renderPass];
                  if (buf != null) {
                     GLRenderer.render(buf);
                  }
               }

               GLRenderer.popFrame();
               if (renderPass == 0) {
                  drawBoundsWireframe(world, cameraX, cameraY, cameraZ);
                  drawWrongBlockPass(world, worldSections, cameraX, cameraY, cameraZ);
               }
            }
         }
      }
   }

   private static void drawWrongBlockPass(World world, Map<Long, HologramRenderer.Section> worldSections, double cameraX, double cameraY, double cameraZ) {
      boolean anyWrong = false;

      for (HologramRenderer.Section s : worldSections.values()) {
         if (s.buffers[2] != null) {
            anyWrong = true;
            break;
         }
      }

      if (anyWrong) {
         GLRenderer.pushFrame();
         GLRenderer.setShader(Shaders.ITEM);
         TextureRegistry.worldAtlas.bind();
         GLRenderer.enableState(State.BLEND);
         GLRenderer.setBlendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
         GLRenderer.enableState(State.POLYGON_OFFSET_FILL);
         GLRenderer.setPolygonOffset(-1.0F, -1.0F);
         GLRenderer.setDepthMask(false);
         GLRenderer.setAlphaTest(0.1F);
         GLRenderer.setColor4f(1.0F, 0.19F, 0.19F, 0.75F);
         GLRenderer.modelM4f().translate((float)(-cameraX), (float)(-cameraY), (float)(-cameraZ));
         WRONG_BLOCK_PASS_ACTIVE = true;

         try {
            for (HologramRenderer.Section s : worldSections.values()) {
               RenderBuffer buf = s.buffers[2];
               if (buf != null) {
                  GLRenderer.render(buf);
               }
            }
         } finally {
            WRONG_BLOCK_PASS_ACTIVE = false;
            GLRenderer.popFrame();
         }
      }
   }

   public static void renderPostEntities(World world, double cameraX, double cameraY, double cameraZ, float partialTick) {
      if (world != null) {
         boolean designMode = DesignModeState.isActive();
         boolean drawSelection = designMode && BlueprintSelection.hasAny(world);
         boolean drawShapePointsFlag = designMode && ShapeToolState.hasAnyPoint(world);
         boolean drawTileEntities = !HologramAppearance.isHidden() && HologramStore.hasEntries(world);
         if (drawSelection || drawShapePointsFlag || drawTileEntities) {
            if (drawTileEntities) {
               INSTANCE.renderHologramTileEntities(world, partialTick);
            }

            if (drawSelection) {
               drawSelectionBox(world, cameraX, cameraY, cameraZ);
            }

            if (drawShapePointsFlag) {
               drawShapePoints(world, cameraX, cameraY, cameraZ);
            }
         }
      }
   }

   private void renderHologramTileEntities(World world, float partialTick) {
      Map<Long, HologramBlock> worldBlocks = HologramStore.rawView(world);
      if (!worldBlocks.isEmpty()) {
         TileEntityRenderDispatcher dispatcher = TileEntityRenderDispatcher.instance;
         ICamera camera = dispatcher.camera;
         if (camera != null) {
            Map<Long, TileEntity> cache = this.dummyTileEntities.computeIfAbsent(world, w -> new HashMap<>());
            TessellatorGeneral tess = GLRenderer.getTessellator();
            Iterator var8 = worldBlocks.entrySet().iterator();

            while (true) {
					TileEntity te;
               while (true) {
                  if (!var8.hasNext()) {
                     return;
                  }

                  Entry<Long, HologramBlock> e = (Entry<Long, HologramBlock>)var8.next();
                  long key = e.getKey();
                  HologramBlock h = e.getValue();
                  int blockId = h.blockId;
                  if (blockId > 0 && blockId < Blocks.blocksList.length) {
                     Block<?> block = Blocks.blocksList[blockId];
                     if (block != null && block.entitySupplier != null) {
                        int x = HologramStore.unpackX(key);
                        int y = HologramStore.unpackY(key);
                        int z = HologramStore.unpackZ(key);
                        if (HologramAppearance.isYVisible(y) && world.getBlockId(x, y, z) != blockId) {
                           te = cache.get(key);
                           if (te != null) {
                              break;
                           }

                           te = HologramTileEntities.instantiate(x, y, z, h);
                           if (te == null) {
                              try {
                                 if (!(block.entitySupplier.get() instanceof TileEntity)) {
                                    continue;
                                 }

                                 te.tilePos.x = x;
                                 te.tilePos.y = y;
                                 te.tilePos.z = z;
                              } catch (Throwable t) {
                                 continue;
                              }
                           }

                           te.worldObj = world;
                           cache.put(key, te);
                           break;
                        }
                     }
                  }
               }

               if (dispatcher.hasRenderer(te)) {
                  HologramPlacementContext.begin(world);
                  GLRenderer.pushFrame();

                  try {
                     GLRenderer.setLightmapCoord2i(15, 15);
                     GLRenderer.setColor3f(1.0F, 1.0F, 1.0F);
                     dispatcher.renderTileEntity(
                        tess,
                        te,
                        te.tilePos.x - TileEntityRenderDispatcher.renderPosX,
                        te.tilePos.y - TileEntityRenderDispatcher.renderPosY,
                        te.tilePos.z - TileEntityRenderDispatcher.renderPosZ,
                        partialTick
                     );
                  } catch (Throwable var24) {
                  } finally {
                     GLRenderer.popFrame();
                     HologramPlacementContext.end();
                  }
               }
            }
         }
      }
   }

   private static void drawBoundsWireframe(World world, double cx, double cy, double cz) {
      int[] bounds = HologramStore.getBounds(world);
      if (bounds != null) {
         double x0 = bounds[0] - cx;
         double y0 = bounds[1] - cy;
         double z0 = bounds[2] - cz;
         double x1 = bounds[3] + 1.0 - cx;
         double y1 = bounds[4] + 1.0 - cy;
         double z1 = bounds[5] + 1.0 - cz;
         float r = HologramAppearance.getR() / 255.0F;
         float g = HologramAppearance.getG() / 255.0F;
         float b = HologramAppearance.getB() / 255.0F;
         float a = HologramAppearance.getA() / 255.0F;
         GLRenderer.pushFrame();
         GLRenderer.setShader(Shaders.LINES);
         GLRenderer.enableState(State.BLEND);
         GLRenderer.setBlendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
         GLRenderer.setLineWidth(2.0F);
         GLRenderer.setDepthMask(false);
         TessellatorGeneral t = GLRenderer.getTessellator();
         t.startDrawing(DrawMode.LINES);
         t.setColor4f(r, g, b, a);
         emitWireCube(t, x0, y0, z0, x1, y1, z1);
         t.draw();
         GLRenderer.disableState(State.DEPTH_TEST);
         t.startDrawing(DrawMode.LINES);
         t.setColor4f(r, g, b, a * 0.25F);
         emitWireCube(t, x0, y0, z0, x1, y1, z1);
         t.draw();
         GLRenderer.popFrame();
      }
   }

   private static void drawSelectionBox(World world, double cx, double cy, double cz) {
      BlueprintSelection.Box box = BlueprintSelection.getBox(world);
      double x0;
      double y0;
      double z0;
      double x1;
      double y1;
      double z1;
      if (box != null) {
         x0 = box.minX;
         y0 = box.minY;
         z0 = box.minZ;
         x1 = box.maxX + 1.0;
         y1 = box.maxY + 1.0;
         z1 = box.maxZ + 1.0;
      } else {
         int[] a = BlueprintSelection.getCornerA(world);
         int[] b = BlueprintSelection.getCornerB(world);
         int[] only = a != null ? a : b;
         if (only == null) {
            return;
         }

         x0 = only[0];
         y0 = only[1];
         z0 = only[2];
         x1 = x0 + 1.0;
         y1 = y0 + 1.0;
         z1 = z0 + 1.0;
      }

      x0 -= cx;
      y0 -= cy;
      z0 -= cz;
      x1 -= cx;
      y1 -= cy;
      z1 -= cz;
      float r = HologramAppearance.getR() / 255.0F;
      float g = HologramAppearance.getG() / 255.0F;
      float bcol = HologramAppearance.getB() / 255.0F;
      GLRenderer.pushFrame();
      GLRenderer.enableState(State.BLEND);
      GLRenderer.setBlendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
      GLRenderer.setDepthMask(false);
      TessellatorGeneral t = GLRenderer.getTessellator();
      GLRenderer.setShader(Shaders.COLOR);
      GLRenderer.enableState(State.CULL_FACE);
      GLRenderer.enableState(State.POLYGON_OFFSET_FILL);
      GLRenderer.setPolygonOffset(-1.0F, -1.0F);
      t.startDrawingQuads();
      t.setColor4f(r, g, bcol, 0.5F);
      emitSolidCube(t, x0, y0, z0, x1, y1, z1);
      t.draw();
      GLRenderer.disableState(State.POLYGON_OFFSET_FILL);
      GLRenderer.setShader(Shaders.LINES);
      GLRenderer.disableState(State.CULL_FACE);
      GLRenderer.setLineWidth(2.0F);
      t.startDrawing(DrawMode.LINES);
      t.setColor4f(r, g, bcol, 1.0F);
      emitWireCube(t, x0, y0, z0, x1, y1, z1);
      t.draw();
      GLRenderer.disableState(State.DEPTH_TEST);
      t.startDrawing(DrawMode.LINES);
      t.setColor4f(r, g, bcol, 0.25F);
      emitWireCube(t, x0, y0, z0, x1, y1, z1);
      t.draw();
      GLRenderer.popFrame();
   }

   private static void drawShapePoints(World world, double cx, double cy, double cz) {
      int[] a = ShapeToolState.getPointA(world);
      int[] b = ShapeToolState.getPointB(world);
      if (a != null || b != null) {
         GLRenderer.pushFrame();
         GLRenderer.setShader(Shaders.LINES);
         GLRenderer.enableState(State.BLEND);
         GLRenderer.setBlendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
         GLRenderer.setLineWidth(2.0F);
         GLRenderer.setDepthMask(false);
         TessellatorGeneral t = GLRenderer.getTessellator();
         if (a != null) {
            drawShapePointCube(t, a, cx, cy, cz, 1.0F, 0.67F, 0.33F);
         }

         if (b != null) {
            drawShapePointCube(t, b, cx, cy, cz, 0.33F, 0.67F, 1.0F);
         }

         GLRenderer.popFrame();
      }
   }

   private static void drawShapePointCube(TessellatorGeneral t, int[] p, double cx, double cy, double cz, float r, float g, float b) {
      double x0 = p[0] - cx;
      double y0 = p[1] - cy;
      double z0 = p[2] - cz;
      double x1 = x0 + 1.0;
      double y1 = y0 + 1.0;
      double z1 = z0 + 1.0;
      GLRenderer.enableState(State.DEPTH_TEST);
      t.startDrawing(DrawMode.LINES);
      t.setColor4f(r, g, b, 1.0F);
      emitWireCube(t, x0, y0, z0, x1, y1, z1);
      t.draw();
      GLRenderer.disableState(State.DEPTH_TEST);
      t.startDrawing(DrawMode.LINES);
      t.setColor4f(r, g, b, 0.25F);
      emitWireCube(t, x0, y0, z0, x1, y1, z1);
      t.draw();
   }

   private static void emitWireCube(TessellatorGeneral t, double x0, double y0, double z0, double x1, double y1, double z1) {
      t.addVertex(x0, y0, z0);
      t.addVertex(x1, y0, z0);
      t.addVertex(x1, y0, z0);
      t.addVertex(x1, y0, z1);
      t.addVertex(x1, y0, z1);
      t.addVertex(x0, y0, z1);
      t.addVertex(x0, y0, z1);
      t.addVertex(x0, y0, z0);
      t.addVertex(x0, y1, z0);
      t.addVertex(x1, y1, z0);
      t.addVertex(x1, y1, z0);
      t.addVertex(x1, y1, z1);
      t.addVertex(x1, y1, z1);
      t.addVertex(x0, y1, z1);
      t.addVertex(x0, y1, z1);
      t.addVertex(x0, y1, z0);
      t.addVertex(x0, y0, z0);
      t.addVertex(x0, y1, z0);
      t.addVertex(x1, y0, z0);
      t.addVertex(x1, y1, z0);
      t.addVertex(x1, y0, z1);
      t.addVertex(x1, y1, z1);
      t.addVertex(x0, y0, z1);
      t.addVertex(x0, y1, z1);
   }

   private static void emitSolidCube(TessellatorGeneral t, double x0, double y0, double z0, double x1, double y1, double z1) {
      t.addVertex(x0, y0, z1);
      t.addVertex(x1, y0, z1);
      t.addVertex(x1, y1, z1);
      t.addVertex(x0, y1, z1);
      t.addVertex(x1, y0, z0);
      t.addVertex(x0, y0, z0);
      t.addVertex(x0, y1, z0);
      t.addVertex(x1, y1, z0);
      t.addVertex(x0, y0, z0);
      t.addVertex(x0, y0, z1);
      t.addVertex(x0, y1, z1);
      t.addVertex(x0, y1, z0);
      t.addVertex(x1, y0, z1);
      t.addVertex(x1, y0, z0);
      t.addVertex(x1, y1, z0);
      t.addVertex(x1, y1, z1);
      t.addVertex(x0, y1, z1);
      t.addVertex(x1, y1, z1);
      t.addVertex(x1, y1, z0);
      t.addVertex(x0, y1, z0);
      t.addVertex(x0, y0, z0);
      t.addVertex(x1, y0, z0);
      t.addVertex(x1, y0, z1);
      t.addVertex(x0, y0, z1);
   }

   private static void seedBlockRenderState(TessellatorShader tess) {
      tess.setLightmapCoord2i(15, 15);
      tess.setColor4i(255, 255, 255, 255);
      tess.setTextureUV(0.0, 0.0);
      tess.setShade1i(255);
      tess.setNormal(0.0F, 1.0F, 0.0F);
   }

   private static boolean blueprints$isTessellatorStateFailure(Throwable t) {
      if (!(t instanceof IllegalStateException)) {
         return false;
      }

      String message = t.getMessage();
      if (message == null) {
         return false;
      }

      String lower = message.toLowerCase();
      return lower.contains("disabled") || lower.contains("not drawing") || lower.contains("already drawing");
   }

   private void flushDirty() {
      if (!this.dirtyQueue.isEmpty()) {
         int budget = 8;
         Iterator<HologramRenderer.Section> it = this.dirtyQueue.iterator();

         while (it.hasNext() && budget > 0) {
            HologramRenderer.Section s = it.next();
            it.remove();
            s.queued = false;
            Map<Long, HologramRenderer.Section> worldSections = this.sections.get(s.world);
            if (worldSections != null && worldSections.get(s.key()) == s) {
               this.rebuild(s);
               budget--;
            } else {
               freeBuffers(s);
            }
         }

         if (!this.pendingFulfilledRemovals.isEmpty()) {
            Object[][] snap = this.pendingFulfilledRemovals.toArray(new Object[0][]);
            this.pendingFulfilledRemovals.clear();

            for (Object[] e : snap) {
               HologramStore.remove((World)e[0], (Integer)e[1], (Integer)e[2], (Integer)e[3]);
            }
         }
      }
   }

   private void rebuild(HologramRenderer.Section s) {
      s.dirty = false;
      int baseX = s.baseX();
      int baseY = s.baseY();
      int baseZ = s.baseZ();
      if (!HologramStore.hasSectionHolograms(s.world, baseX, baseY, baseZ)) {
         freeBuffers(s);
         this.sections.get(s.world).remove(s.key());
      } else {
         List<long[]> entries = new ArrayList<>();
         Set<Long> corruptEntries = null;
         int maxBaseX = baseX + 16;
         int maxBaseY = Math.min(256, baseY + 16);
         int maxBaseZ = baseZ + 16;

         for (Entry<Long, HologramBlock> e : HologramStore.rawView(s.world).entrySet()) {
            long key = e.getKey();
            int x = HologramStore.unpackX(key);
            int y = HologramStore.unpackY(key);
            int z = HologramStore.unpackZ(key);
            if (x >= baseX && x < maxBaseX && y >= baseY && y < maxBaseY && z >= baseZ && z < maxBaseZ && HologramAppearance.isYVisible(y)) {
               HologramBlock h = e.getValue();
               if (h != null && h.blockId > 0 && h.blockId < Blocks.blocksList.length && Blocks.blocksList[h.blockId] != null) {
                  int realId = s.world.getBlockId(x, y, z);
                  int realMeta = s.world.getBlockMetadata(x, y, z);
                  int mode;
                  if (realId == 0) {
                     mode = 0;
                  } else if (realId == h.blockId && realMeta == h.metadata) {
                     mode = 3;
                  } else {
                     Block<?> realBlock = realId < Blocks.blocksList.length ? Blocks.blocksList[realId] : null;
                     boolean replaceable = realBlock != null && realBlock.getMaterial().isReplaceable();
                     mode = replaceable ? 2 : 1;
                  }

                  entries.add(new long[]{x, y, z, h.blockId, h.metadata, mode, realId, realMeta});
               } else {
                  corruptEntries = blueprints$markCorruptEntry(corruptEntries, s.world, x, y, z, h, null);
               }
            }
         }

         int[] vertsPerPass = firstRebuildLogged ? null : new int[3];
         boolean[] anyRenderedPerPass = firstRebuildLogged ? null : new boolean[3];
         HologramChunkCache cache = new HologramChunkCache(
            s.world, baseX - 1, Math.max(0, baseY - 1), baseZ - 1, baseX + 16, Math.min(255, baseY + 16), baseZ + 16
         );
         TessellatorShader tess = GLRenderer.getTessellator();
         RenderBuffer[] rebuiltBuffers = new RenderBuffer[3];
         boolean fatalTessellatorFailure = false;
         Throwable fatalFailure = null;
         HOLOGRAM_PASS_ACTIVE = true;

         try {
            for (int pass = 0; pass < 2; pass++) {
               boolean anyRendered = false;
               tess.startDrawingQuads();
               seedBlockRenderState(tess);

               for (long[] entry : entries) {
                  int mode = (int)entry[5];
                  if (mode != 1 && mode != 3) {
                     int blockId = (int)entry[3];
                     if (blockId > 0 && blockId < Blocks.blocksList.length) {
                        Block<?> block = Blocks.blocksList[blockId];
                        if (block == null) {
                           corruptEntries = blueprints$markCorruptEntry(
                              corruptEntries, s.world, (int)entry[0], (int)entry[1], (int)entry[2], new HologramBlock(blockId, (int)entry[4]), null
                           );
                        } else {
                           BlockModel<?> model = (BlockModel<?>)BlockModelDispatcher.getInstance().getDispatch(block);
                           if (model.renderLayer() == pass) {
                              seedBlockRenderState(tess);
                              this.scratchPos.x = (int)entry[0];
                              this.scratchPos.y = (int)entry[1];
                              this.scratchPos.z = (int)entry[2];

                              try {
                                 anyRendered |= model.render(tess, cache, this.scratchPos);
                              } catch (Throwable t) {
                                 if (blueprints$isTessellatorStateFailure(t)) {
                                    fatalTessellatorFailure = true;
                                    fatalFailure = t;
                                    break;
                                 }

                                 LOGGER.warn(
                                    "Skipping hologram render at ({}, {}, {}) state={} after non-fatal model render failure: {}",
                                    new Object[]{
                                       this.scratchPos.x, this.scratchPos.y, this.scratchPos.z, new HologramBlock(blockId, (int)entry[4]), t.toString()
                                    }
                                 );
                              }
                           }
                        }
                     }
                  }
               }

               if (fatalTessellatorFailure) {
                  break;
               }

               if (vertsPerPass != null) {
                  vertsPerPass[pass] = tess.vertexCount;
                  anyRenderedPerPass[pass] = anyRendered;
               }

               if (anyRendered) {
                  rebuiltBuffers[pass] = tess.record(GL41.glGenVertexArrays(), GL41.glGenBuffers());
               } else {
                  tess.draw();
               }
            }
         } finally {
            HOLOGRAM_PASS_ACTIVE = false;
         }

         if (fatalTessellatorFailure) {
            if (tess.drawing) {
               try {
                  tess.draw();
               } catch (Throwable var40) {
               }
            }

            for (int pass = 0; pass < 3; pass++) {
               if (rebuiltBuffers[pass] != null) {
                  rebuiltBuffers[pass].delete();
                  rebuiltBuffers[pass] = null;
               }
            }

            LOGGER.warn(
               "Deferred hologram section rebuild at ({}, {}, {}) after tessellator state failure: {}",
               new Object[]{baseX, baseY, baseZ, fatalFailure == null ? "unknown" : fatalFailure.toString()}
            );
         } else {
            boolean anyWrong = false;

            for (long[] entry : entries) {
               int mode = (int)entry[5];
               if (mode == 1 || mode == 2) {
                  anyWrong = true;
                  break;
               }
            }

            if (anyWrong) {
               ChunkCache realCache = new ChunkCache(s.world, baseX - 1, Math.max(0, baseY - 1), baseZ - 1, baseX + 16, Math.min(255, baseY + 16), baseZ + 16);
               WRONG_BLOCK_PASS_ACTIVE = true;

               try {
                  tess.startDrawingQuads();
                  seedBlockRenderState(tess);
                  boolean wroteAny = false;

                  for (long[] entry : entries) {
                     int mode = (int)entry[5];
                     if (mode == 1 || mode == 2) {
                        int realId = (int)entry[6];
                        if (realId > 0 && realId < Blocks.blocksList.length) {
                           Block<?> block = Blocks.blocksList[realId];
                           if (block != null) {
                              BlockModel<?> model = (BlockModel<?>)BlockModelDispatcher.getInstance().getDispatch(block);
                              this.scratchPos.x = (int)entry[0];
                              this.scratchPos.y = (int)entry[1];
                              this.scratchPos.z = (int)entry[2];

                              try {
                                 seedBlockRenderState(tess);
                                 wroteAny |= model.render(tess, realCache, this.scratchPos);
                              } catch (Throwable t) {
                                 if (blueprints$isTessellatorStateFailure(t)) {
                                    fatalTessellatorFailure = true;
                                    fatalFailure = t;
                                    break;
                                 }

                                 LOGGER.warn(
                                    "Skipping wrong-block overlay render at ({}, {}, {}) realBlockId={} after non-fatal model render failure: {}",
                                    new Object[]{this.scratchPos.x, this.scratchPos.y, this.scratchPos.z, realId, t.toString()}
                                 );
                              }
                           }
                        }
                     }
                  }

                  if (fatalTessellatorFailure) {
                     if (tess.drawing) {
                        try {
                           tess.draw();
                        } catch (Throwable var41) {
                        }
                     }

                     for (int pass = 0; pass < 3; pass++) {
                        if (rebuiltBuffers[pass] != null) {
                           rebuiltBuffers[pass].delete();
                           rebuiltBuffers[pass] = null;
                        }
                     }

                     LOGGER.warn(
                        "Deferred hologram section rebuild at ({}, {}, {}) after wrong-pass tessellator state failure: {}",
                        new Object[]{baseX, baseY, baseZ, fatalFailure == null ? "unknown" : fatalFailure.toString()}
                     );
                     return;
                  }

                  if (vertsPerPass != null) {
                     vertsPerPass[2] = tess.vertexCount;
                     anyRenderedPerPass[2] = wroteAny;
                  }

                  if (wroteAny) {
                     rebuiltBuffers[2] = tess.record(GL41.glGenVertexArrays(), GL41.glGenBuffers());
                  } else {
                     tess.draw();
                  }
               } finally {
                  WRONG_BLOCK_PASS_ACTIVE = false;
               }
            }

            for (int pass = 0; pass < 3; pass++) {
               if (s.buffers[pass] != null) {
                  s.buffers[pass].delete();
               }

               s.buffers[pass] = rebuiltBuffers[pass];
            }

            if (vertsPerPass != null) {
               logFirstRebuildOnce(vertsPerPass, anyRenderedPerPass);
            }

            blueprints$pruneCorruptEntries(s.world, corruptEntries);
         }
      }
   }

   private static Set<Long> blueprints$markCorruptEntry(Set<Long> corruptEntries, World world, int x, int y, int z, HologramBlock block, Throwable t) {
      if (corruptEntries == null) {
         corruptEntries = new HashSet<>();
      }

      long packed = HologramStore.packPos(x, y, z);
      if (corruptEntries.add(packed)) {
         if (t != null) {
            LOGGER.warn("Pruning corrupt hologram at ({}, {}, {}) state={} after render failure: {}", new Object[]{x, y, z, block, t.toString()});
         } else {
            LOGGER.warn("Pruning invalid hologram at ({}, {}, {}) state={}", new Object[]{x, y, z, block});
         }
      }

      return corruptEntries;
   }

   private static void blueprints$pruneCorruptEntries(World world, Set<Long> corruptEntries) {
      if (world != null && corruptEntries != null && !corruptEntries.isEmpty()) {
         for (long packed : corruptEntries) {
            HologramStore.remove(world, HologramStore.unpackX(packed), HologramStore.unpackY(packed), HologramStore.unpackZ(packed));
         }

         HologramStore.recomputeBounds(world);
         LOGGER.warn("Removed {} corrupt hologram block(s) from the active blueprint to keep the world loadable.", corruptEntries.size());
      }
   }

   private static final class Section {
      final World world;
      final int sectionX;
      final int sectionY;
      final int sectionZ;
      final RenderBuffer[] buffers = new RenderBuffer[3];
      boolean dirty = true;
      boolean queued;

      Section(World world, long key) {
         this.world = world;
         this.sectionX = HologramStore.unpackX(key);
         this.sectionY = HologramStore.unpackY(key);
         this.sectionZ = HologramStore.unpackZ(key);
      }

      int baseX() {
         return this.sectionX << 4;
      }

      int baseY() {
         return this.sectionY << 4;
      }

      int baseZ() {
         return this.sectionZ << 4;
      }

      long key() {
         return HologramStore.packSection(this.sectionX << 4, this.sectionY << 4, this.sectionZ << 4);
      }
   }
}
