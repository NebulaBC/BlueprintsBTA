package biscuitius.blueprints.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicSign;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.entity.TileEntitySign;
import net.minecraft.core.enums.EnumSignPicture;
import net.minecraft.core.net.command.TextFormatting;
import net.minecraft.core.world.World;

public final class SignTextCache {
   private static final Map<SignTextCache.Pos, SignTextCache.SignData> ENTRIES = new LinkedHashMap<>();
   private static boolean suppressSignEditor;
   private static List<SignTextCache.SignData> pendingEntries;

   private SignTextCache() {
   }

   public static void put(int x, int y, int z, String[] text, int picture, int color) {
      String[] copy = new String[4];

      for (int i = 0; i < 4; i++) {
         copy[i] = i < text.length && text[i] != null ? text[i] : "";
      }

      ENTRIES.put(new SignTextCache.Pos(x, y, z), new SignTextCache.SignData(x, y, z, copy, picture, color));
   }

   public static SignTextCache.SignData get(int x, int y, int z) {
      return ENTRIES.get(new SignTextCache.Pos(x, y, z));
   }

   public static void remove(int x, int y, int z) {
      ENTRIES.remove(new SignTextCache.Pos(x, y, z));
   }

   public static void clear() {
      ENTRIES.clear();
      pendingEntries = null;
   }

   public static boolean hasEntries() {
      return !ENTRIES.isEmpty();
   }

   public static void shiftAll(int dx, int dy, int dz) {
      if (!ENTRIES.isEmpty()) {
         Map<SignTextCache.Pos, SignTextCache.SignData> shifted = new LinkedHashMap<>();

         for (SignTextCache.SignData data : ENTRIES.values()) {
            int nx = data.x + dx;
            int ny = data.y + dy;
            int nz = data.z + dz;
            shifted.put(new SignTextCache.Pos(nx, ny, nz), new SignTextCache.SignData(nx, ny, nz, data.text, data.picture, data.color));
         }

         ENTRIES.clear();
         ENTRIES.putAll(shifted);
      }
   }

   public static boolean isSuppressingSignEditor() {
      return suppressSignEditor;
   }

   public static void setSuppressSignEditor(boolean suppress) {
      suppressSignEditor = suppress;
   }

   public static void forEachEntry(SignTextCache.SignTextConsumer consumer) {
      for (SignTextCache.SignData data : ENTRIES.values()) {
         consumer.accept(data.x, data.y, data.z, data.text, data.picture, data.color);
      }
   }

   public static void setPendingEntries(List<SignTextCache.SignData> entries) {
      pendingEntries = entries;
   }

   public static void applyPendingEntries() {
      if (pendingEntries != null) {
         for (SignTextCache.SignData data : pendingEntries) {
            ENTRIES.put(new SignTextCache.Pos(data.x, data.y, data.z), data);
         }

         pendingEntries = null;
      }
   }

   public static void applySignTileEntities(World world) {
      if (world != null && !ENTRIES.isEmpty()) {
         for (SignTextCache.SignData data : ENTRIES.values()) {
            if (world.isChunkLoaded(data.x >> 4, data.z >> 4)) {
               int blockId = world.getBlockId(data.x, data.y, data.z);
               Block<?> block = Blocks.getBlock(blockId);
               if (block != null && block.getLogic() instanceof BlockLogicSign) {
                  TileEntity rawTe = world.getTileEntity(data.x, data.y, data.z);
                  TileEntitySign te;
                  if (rawTe instanceof TileEntitySign) {
                     te = (TileEntitySign)rawTe;
                  } else {
                     te = new TileEntitySign();
                     world.setTileEntity(data.x, data.y, data.z, te);
                  }

                  applySignData(te, data);
               }
            }
         }
      }
   }

   public static void applySignData(TileEntitySign te, SignTextCache.SignData data) {
      for (int i = 0; i < 4; i++) {
         te.signText[i] = data.text[i];
      }

      EnumSignPicture pic = EnumSignPicture.fromId(data.picture);
      if (pic != null) {
         te.setPicture(pic);
      }

      if (data.color >= 0 && data.color <= 15) {
         te.setColor(TextFormatting.FORMATTINGS[data.color]);
      }
   }

   private static final class Pos {
      final int x;
      final int y;
      final int z;

      Pos(int x, int y, int z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }

      @Override
      public boolean equals(Object o) {
         if (!(o instanceof SignTextCache.Pos)) {
            return false;
         } else {
            SignTextCache.Pos p = (SignTextCache.Pos)o;
            return this.x == p.x && this.y == p.y && this.z == p.z;
         }
      }

      @Override
      public int hashCode() {
         return 31 * (31 * Integer.hashCode(this.x) + Integer.hashCode(this.y)) + Integer.hashCode(this.z);
      }
   }

   public static final class SignData {
      public final int x;
      public final int y;
      public final int z;
      public final String[] text;
      public final int picture;
      public final int color;

      public SignData(int x, int y, int z, String[] text, int picture, int color) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.text = text;
         this.picture = picture;
         this.color = color;
      }
   }

   @FunctionalInterface
   public interface SignTextConsumer {
      void accept(int var1, int var2, int var3, String[] var4, int var5, int var6);
   }
}
