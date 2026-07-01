package biscuitius.blueprints.client.hologram;

public final class HologramBlock {
   public final int blockId;
   public final int metadata;
   public final Object nbt;

   public HologramBlock(int blockId, int metadata) {
      this(blockId, metadata, null);
   }

   public HologramBlock(int blockId, int metadata, Object nbt) {
      this.blockId = blockId;
      this.metadata = metadata;
      this.nbt = nbt;
   }

   public HologramBlock withMetadata(int newMeta) {
      return newMeta == this.metadata ? this : new HologramBlock(this.blockId, newMeta, this.nbt);
   }

   public HologramBlock withNbt(Object newNbt) {
      return newNbt == this.nbt ? this : new HologramBlock(this.blockId, this.metadata, newNbt);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof HologramBlock)) {
         return false;
      } else {
         HologramBlock other = (HologramBlock)o;
         return this.blockId == other.blockId && this.metadata == other.metadata && (this.nbt == null ? other.nbt == null : this.nbt.equals(other.nbt));
      }
   }

   @Override
   public int hashCode() {
      int h = this.blockId * 31 + this.metadata;
      if (this.nbt != null) {
         h = h * 31 + this.nbt.hashCode();
      }

      return h;
   }

   @Override
   public String toString() {
      return "HologramBlock{" + this.blockId + ":" + this.metadata + (this.nbt != null ? " +nbt" : "") + "}";
   }
}
