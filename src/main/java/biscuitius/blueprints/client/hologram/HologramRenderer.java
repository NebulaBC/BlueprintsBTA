package biscuitius.blueprints.client.hologram;

import biscuitius.blueprints.client.BlueprintSelection;
import biscuitius.blueprints.client.DesignModeState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.render.LightmapHelper;
import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.TileEntityRenderDispatcher;
import net.minecraft.client.render.block.model.BlockModel;
import net.minecraft.client.render.block.model.BlockModelDispatcher;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.tessellator.TessellatorStandard;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.world.World;
import net.minecraft.core.world.chunk.ChunkCache;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.slf4j.LoggerFactory;

public final class HologramRenderer implements HologramListener {
   public static final int RENDER_PASS_SOLID = 0;
   public static final int RENDER_PASS_TRANSLUCENT = 1;
   public static final int RENDER_PASS_WRONG = 2;
   private static final int RENDER_PASSES = 3;
   private static final int MAX_REBUILDS_PER_FRAME = 8;
   public static volatile boolean HOLOGRAM_PASS_ACTIVE;
   public static volatile boolean WRONG_BLOCK_PASS_ACTIVE;
   private static final int MODE_HOLOGRAM = 0;
   private static final int MODE_WRONG_ONLY = 1;
   private static final int MODE_HOLOGRAM_WRONG = 2;
   private static final int MODE_FULFILLED = 3;
   private static final HologramRenderer INSTANCE = new HologramRenderer();
   private final Map<World, Map<Long, HologramRenderer.Section>> sections = new IdentityHashMap<>();
   private final Map<World, Map<Long, TileEntity>> dummyTileEntities = new IdentityHashMap<>();
   private final Deque<HologramRenderer.Section> dirtyQueue = new ArrayDeque<>();
   private final List<Object[]> pendingFulfilledRemovals = new ArrayList<>();
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
            int withList = 0;
            int[] nonEmpty = new int[3];

            for (HologramRenderer.Section s : worldSections.values()) {
               if (s.firstDisplayList != 0) {
                  withList++;
               }

               for (int p = 0; p < 3; p++) {
                  if (!s.emptyPass[p]) {
                     nonEmpty[p]++;
                  }
               }
            }

            LoggerFactory.getLogger("blueprints-client")
               .info(
                  "First hologram draw — storeEntries={} sections={} sectionsWithList={} nonEmpty[solid={}, translucent={}, wrong={}] designMode={} hidden={} layerCutoffY={}",
                  new Object[]{
                     storeEntries,
                     sectionCount,
                     withList,
                     nonEmpty[0],
                     nonEmpty[1],
                     nonEmpty[2],
                     DesignModeState.isActive(),
                     HologramAppearance.isHidden(),
                     HologramAppearance.getLayerCutoffY()
                  }
               );
         } catch (Throwable var9) {
         }
      }
   }

   private static void logFirstRebuildOnce(int[] vertsPerPass, boolean[] anyRenderedPerPass) {
      if (!firstRebuildLogged) {
         firstRebuildLogged = true;

         try {
            LoggerFactory.getLogger("blueprints-client")
               .info(
                  "First hologram rebuild — verts[solid={}, translucent={}, wrong={}] anyRendered[solid={}, translucent={}, wrong={}] lightmapEnabled={}",
                  new Object[]{
                     vertsPerPass[0],
                     vertsPerPass[1],
                     vertsPerPass[2],
                     anyRenderedPerPass[0],
                     anyRenderedPerPass[1],
                     anyRenderedPerPass[2],
                     LightmapHelper.isLightmapEnabled()
                  }
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
            freeDisplayLists(s);
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

   private static void drawWrongBlockPass(World world, Map<Long, HologramRenderer.Section> worldSections) {
      boolean anyWrong = false;

      for (HologramRenderer.Section s : worldSections.values()) {
         if (s.firstDisplayList != 0 && !s.emptyPass[2]) {
            anyWrong = true;
            break;
         }
      }

      if (anyWrong) {
         boolean wasBlend = GL11.glIsEnabled(3042);
         boolean wasOffset = GL11.glIsEnabled(32823);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glEnable(32823);
         GL11.glPolygonOffset(-1.0F, -1.0F);
         GL11.glDepthMask(false);
         WRONG_BLOCK_PASS_ACTIVE = true;

         try {
            for (HologramRenderer.Section sx : worldSections.values()) {
               if (sx.firstDisplayList != 0 && !sx.emptyPass[2]) {
                  GL11.glCallList(sx.firstDisplayList + 2);
               }
            }
         } finally {
            WRONG_BLOCK_PASS_ACTIVE = false;
            GL11.glDepthMask(true);
            GL11.glPolygonOffset(0.0F, 0.0F);
            if (!wasOffset) {
               GL11.glDisable(32823);
            }

            if (!wasBlend) {
               GL11.glDisable(3042);
            }
         }
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

   private static void freeDisplayLists(HologramRenderer.Section s) {
      if (s.firstDisplayList != 0) {
         GL11.glDeleteLists(s.firstDisplayList, 3);
         s.firstDisplayList = 0;
      }
   }

   private static void allocDisplayLists(HologramRenderer.Section s) {
      if (s.firstDisplayList == 0) {
         s.firstDisplayList = GL11.glGenLists(3);
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

               boolean designMode = DesignModeState.isActive();
               boolean wasBlend = false;
               if (!designMode) {
                  wasBlend = GL11.glIsEnabled(3042);
                  GL11.glEnable(3042);
                  GL11.glBlendFunc(770, 771);
                  GL11.glDepthMask(renderPass == 0);
               }

               GL11.glPushMatrix();
               GL11.glTranslated(-cameraX, -cameraY, -cameraZ);
               if (renderPass == 0) {
                  logFirstDrawOnce(world, worldSections);
               }

               for (HologramRenderer.Section s : worldSections.values()) {
                  if (s.firstDisplayList != 0 && !s.emptyPass[renderPass]) {
                     GL11.glCallList(s.firstDisplayList + renderPass);
                  }
               }

               if (renderPass == 0) {
                  drawBoundsWireframe(world);
                  drawWrongBlockPass(world, worldSections);
               }

               GL11.glPopMatrix();
               GL11.glDepthMask(true);
               if (!designMode && !wasBlend) {
                  GL11.glDisable(3042);
               }
            }
         }
      }
   }

   public static void renderPostEntities(World world, double cameraX, double cameraY, double cameraZ, float partialTick) {
      if (world != null) {
         boolean designMode = DesignModeState.isActive();
         boolean drawSelection = designMode && BlueprintSelection.hasAny(world);
         boolean drawTileEntities = !HologramAppearance.isHidden() && HologramStore.hasEntries(world);
         if (drawSelection || drawTileEntities) {
            if (drawTileEntities) {
               INSTANCE.renderHologramTileEntities(world, partialTick);
            }

            if (drawSelection) {
               GL11.glPushMatrix();
               GL11.glTranslated(-cameraX, -cameraY, -cameraZ);
               drawSelectionBox(world);
               GL11.glPopMatrix();
               GL11.glDepthMask(true);
            }
         }
      }
   }

   private void renderHologramTileEntities(World world, float partialTick) {
      Map<Long, HologramBlock> worldBlocks = HologramStore.rawView(world);
      if (!worldBlocks.isEmpty()) {
         Map<Long, TileEntity> cache = this.dummyTileEntities.computeIfAbsent(world, w -> new HashMap<>());
         Tessellator tess = Tessellator.instance;
         TileEntityRenderDispatcher dispatcher = TileEntityRenderDispatcher.instance;
         Iterator var7 = worldBlocks.entrySet().iterator();

         while (true) {
            TileEntity te;
            while (true) {
               if (!var7.hasNext()) {
                  return;
               }

               Entry<Long, HologramBlock> e = (Entry<Long, HologramBlock>)var7.next();
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

                        try {
                           Object supplied = block.entitySupplier.get();
                           if (!(supplied instanceof TileEntity)) {
                              continue;
                           }

                           te = (TileEntity)supplied;
                        } catch (Throwable var25) {
                           continue;
                        }

                        te.worldObj = world;
                        te.x = x;
                        te.y = y;
                        te.z = z;
                        cache.put(key, te);
                        break;
                     }
                  }
               }
            }

            if (dispatcher.hasRenderer(te)) {
               HologramPlacementContext.begin(world);

               try {
                  dispatcher.renderTileEntity(tess, dispatcher.camera, te, partialTick);
               } catch (Throwable var23) {
               } finally {
                  HologramPlacementContext.end();
               }
            }
         }
      }
   }

   private static void drawBoundsWireframe(World world) {
      int[] bounds = HologramStore.getBounds(world);
      if (bounds != null) {
         double x0 = bounds[0];
         double y0 = bounds[1];
         double z0 = bounds[2];
         double x1 = bounds[3] + 1.0;
         double y1 = bounds[4] + 1.0;
         double z1 = bounds[5] + 1.0;
         boolean wasTex = GL11.glIsEnabled(3553);
         boolean wasLight = GL11.glIsEnabled(2896);
         boolean wasFog = GL11.glIsEnabled(2912);
         boolean wasCull = GL11.glIsEnabled(2884);
         float prevLineW = GL11.glGetFloat(2849);
         if (wasTex) {
            GL11.glDisable(3553);
         }

         if (wasLight) {
            GL11.glDisable(2896);
         }

         if (wasFog) {
            GL11.glDisable(2912);
         }

         if (wasCull) {
            GL11.glDisable(2884);
         }

         GL11.glLineWidth(2.0F);
         float r = HologramAppearance.getR() / 255.0F;
         float g = HologramAppearance.getG() / 255.0F;
         float b = HologramAppearance.getB() / 255.0F;
         float a = HologramAppearance.getA() / 255.0F;
         GL11.glDepthMask(false);
         GL11.glEnable(2929);
         GL11.glColor4f(r, g, b, a);
         emitWireCube(x0, y0, z0, x1, y1, z1);
         GL11.glDisable(2929);
         GL11.glColor4f(r, g, b, a * 0.25F);
         emitWireCube(x0, y0, z0, x1, y1, z1);
         GL11.glEnable(2929);
         GL11.glLineWidth(prevLineW);
         if (wasCull) {
            GL11.glEnable(2884);
         }

         if (wasFog) {
            GL11.glEnable(2912);
         }

         if (wasLight) {
            GL11.glEnable(2896);
         }

         if (wasTex) {
            GL11.glEnable(3553);
         }

         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private static void emitWireCube(double x0, double y0, double z0, double x1, double y1, double z1) {
      GL11.glBegin(1);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glEnd();
   }

   private static void drawSelectionBox(World world) {
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

      float r = HologramAppearance.getR() / 255.0F;
      float g = HologramAppearance.getG() / 255.0F;
      float bcol = HologramAppearance.getB() / 255.0F;
      boolean wasTex = GL11.glIsEnabled(3553);
      boolean wasLight = GL11.glIsEnabled(2896);
      boolean wasFog = GL11.glIsEnabled(2912);
      boolean wasCull = GL11.glIsEnabled(2884);
      boolean wasBlend = GL11.glIsEnabled(3042);
      boolean wasOffset = GL11.glIsEnabled(32823);
      float prevLineW = GL11.glGetFloat(2849);
      GL13.glActiveTexture(33985);
      boolean wasLightmap = GL11.glIsEnabled(3553);
      GL11.glDisable(3553);
      GL13.glActiveTexture(33984);
      if (wasTex) {
         GL11.glDisable(3553);
      }

      if (wasLight) {
         GL11.glDisable(2896);
      }

      if (wasFog) {
         GL11.glDisable(2912);
      }

      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDepthMask(false);
      if (!wasCull) {
         GL11.glEnable(2884);
      }

      GL11.glEnable(32823);
      GL11.glPolygonOffset(-1.0F, -1.0F);
      GL11.glColor4f(r, g, bcol, 0.5F);
      emitSolidCube(x0, y0, z0, x1, y1, z1);
      GL11.glPolygonOffset(0.0F, 0.0F);
      if (!wasOffset) {
         GL11.glDisable(32823);
      }

      GL11.glDisable(2884);
      GL11.glLineWidth(2.0F);
      GL11.glEnable(2929);
      GL11.glColor4f(r, g, bcol, 1.0F);
      emitWireCube(x0, y0, z0, x1, y1, z1);
      GL11.glDisable(2929);
      GL11.glColor4f(r, g, bcol, 0.25F);
      emitWireCube(x0, y0, z0, x1, y1, z1);
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glLineWidth(prevLineW);
      if (!wasBlend) {
         GL11.glDisable(3042);
      }

      if (wasCull) {
         GL11.glEnable(2884);
      } else {
         GL11.glDisable(2884);
      }

      if (wasFog) {
         GL11.glEnable(2912);
      }

      if (wasLight) {
         GL11.glEnable(2896);
      }

      if (wasTex) {
         GL11.glEnable(3553);
      }

      GL13.glActiveTexture(33985);
      if (wasLightmap) {
         GL11.glEnable(3553);
      }

      GL13.glActiveTexture(33984);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static void emitSolidCube(double x0, double y0, double z0, double x1, double y1, double z1) {
      GL11.glBegin(7);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glEnd();
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
               freeDisplayLists(s);
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
      int baseX = s.sectionX << 4;
      int baseY = s.sectionY << 4;
      int baseZ = s.sectionZ << 4;
      if (!HologramStore.hasSectionHolograms(s.world, baseX, baseY, baseZ)) {
         freeDisplayLists(s);

         for (int p = 0; p < 3; p++) {
            s.emptyPass[p] = true;
         }

         this.sections.get(s.world).remove(s.key());
      } else {
         HologramChunkCache cache = new HologramChunkCache(
            s.world, baseX - 1, Math.max(0, baseY - 1), baseZ - 1, baseX + 16, Math.min(255, baseY + 16), baseZ + 16
         );
         RenderBlocks renderBlocks = new RenderBlocks(cache);
         RenderBlocks previousRenderBlocks = BlockModel.renderBlocks;
         BlockModel.setRenderBlocks(renderBlocks);
         allocDisplayLists(s);
         List<long[]> entries = new ArrayList<>();
         List<long[]> toRemoveAfter = null;
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
               int realId = s.world.getBlockId(x, y, z);
               int realMeta = s.world.getBlockMetadata(x, y, z);
               int mode;
               if (realId == 0) {
                  mode = 0;
               } else if (realId == h.blockId && realMeta == h.metadata) {
                  mode = 3;
                  if (toRemoveAfter == null) {
                     toRemoveAfter = new ArrayList<>();
                  }

                  toRemoveAfter.add(new long[]{x, y, z});
               } else {
                  Block<?> realBlock = realId < Blocks.blocksList.length ? Blocks.blocksList[realId] : null;
                  boolean replaceable = realBlock != null && realBlock.getMaterial().isReplaceable();
                  mode = replaceable ? 2 : 1;
               }

               entries.add(new long[]{x, y, z, h.blockId, h.metadata, mode, realId, realMeta});
            }
         }

         int[] vertsPerPass = firstRebuildLogged ? null : new int[3];
         boolean[] anyRenderedPerPass = firstRebuildLogged ? null : new boolean[3];
         Tessellator tess = Tessellator.instance;
         HOLOGRAM_PASS_ACTIVE = true;

         try {
            for (int pass = 0; pass < 2; pass++) {
               boolean anyRendered = false;
               GL11.glNewList(s.firstDisplayList + pass, 4864);
               tess.startDrawingQuads();
               if (LightmapHelper.isLightmapEnabled()) {
                  tess.setLightmapCoord(LightmapHelper.getLightmapCoord(15, 15));
               }

               for (long[] entry : entries) {
                  int mode = (int)entry[5];
                  if (mode != 1 && mode != 3) {
                     int blockId = (int)entry[3];
                     if (blockId > 0 && blockId < Blocks.blocksList.length) {
                        Block<?> block = Blocks.blocksList[blockId];
                        if (block != null) {
                           BlockModel<?> model = (BlockModel<?>)BlockModelDispatcher.getInstance().getDispatch(block);
                           if (model != null && model.renderLayer() == pass) {
                              if (LightmapHelper.isLightmapEnabled()) {
                                 tess.setLightmapCoord(LightmapHelper.getLightmapCoord(15, 0));
                              }

                              anyRendered |= model.render(tess, (int)entry[0], (int)entry[1], (int)entry[2]);
                           }
                        }
                     }
                  }
               }

               if (vertsPerPass != null) {
                  vertsPerPass[pass] = ((TessellatorStandard)tess).data.vertexCount;
                  anyRenderedPerPass[pass] = anyRendered;
               }

               tess.draw();
               GL11.glEndList();
               s.emptyPass[pass] = !anyRendered;
            }
         } finally {
            HOLOGRAM_PASS_ACTIVE = false;
            BlockModel.setRenderBlocks(previousRenderBlocks);
         }

         boolean var38 = false;

         for (long[] entryx : entries) {
            int mode = (int)entryx[5];
            if (mode == 1 || mode == 2) {
               var38 = true;
               break;
            }
         }

         GL11.glNewList(s.firstDisplayList + 2, 4864);
         if (var38) {
            ChunkCache realCache = new ChunkCache(s.world, baseX - 1, Math.max(0, baseY - 1), baseZ - 1, baseX + 16, Math.min(255, baseY + 16), baseZ + 16);
            RenderBlocks realRender = new RenderBlocks(realCache);
            RenderBlocks save = BlockModel.renderBlocks;
            BlockModel.setRenderBlocks(realRender);
            WRONG_BLOCK_PASS_ACTIVE = true;

            try {
               tess.startDrawingQuads();
               if (LightmapHelper.isLightmapEnabled()) {
                  tess.setLightmapCoord(LightmapHelper.getLightmapCoord(15, 0));
               }

               for (long[] entryxx : entries) {
                  int mode = (int)entryxx[5];
                  if (mode == 1 || mode == 2) {
                     int realId = (int)entryxx[6];
                     if (realId > 0 && realId < Blocks.blocksList.length) {
                        Block<?> block = Blocks.blocksList[realId];
                        if (block != null) {
                           BlockModel<?> model = (BlockModel<?>)BlockModelDispatcher.getInstance().getDispatch(block);
                           if (model != null) {
                              model.render(tess, (int)entryxx[0], (int)entryxx[1], (int)entryxx[2]);
                           }
                        }
                     }
                  }
               }

               if (vertsPerPass != null) {
                  vertsPerPass[2] = ((TessellatorStandard)tess).data.vertexCount;
                  anyRenderedPerPass[2] = true;
               }

               tess.draw();
            } finally {
               WRONG_BLOCK_PASS_ACTIVE = false;
               BlockModel.setRenderBlocks(save);
            }
         }

         GL11.glEndList();
         s.emptyPass[2] = !var38;
         if (vertsPerPass != null) {
            logFirstRebuildOnce(vertsPerPass, anyRenderedPerPass);
         }

         if (toRemoveAfter != null) {
            for (long[] pos : toRemoveAfter) {
               this.pendingFulfilledRemovals.add(new Object[]{s.world, (int)pos[0], (int)pos[1], (int)pos[2]});
            }
         }
      }
   }

   private static final class Section {
      final World world;
      final int sectionX;
      final int sectionY;
      final int sectionZ;
      int firstDisplayList;
      final boolean[] emptyPass = new boolean[3];
      boolean dirty = true;
      boolean queued;

      Section(World world, long key) {
         this.world = world;
         this.sectionX = HologramStore.unpackX(key);
         this.sectionY = HologramStore.unpackY(key);
         this.sectionZ = HologramStore.unpackZ(key);

         for (int i = 0; i < 3; i++) {
            this.emptyPass[i] = true;
         }
      }

      long key() {
         return HologramStore.packSection(this.sectionX << 4, this.sectionY << 4, this.sectionZ << 4);
      }
   }
}
