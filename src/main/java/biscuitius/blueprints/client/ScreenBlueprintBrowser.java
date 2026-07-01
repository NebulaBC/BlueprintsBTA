package biscuitius.blueprints.client;

import java.io.File;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ButtonElement;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.gui.ScrolledSelectionList;
import net.minecraft.client.gui.TextFieldElement;
import net.minecraft.client.render.tessellator.Tessellator;
import org.lwjgl.input.Keyboard;

public final class ScreenBlueprintBrowser extends Screen {
   private static final int ID_ACTION = 1;
   private static final int ID_DELETE = 2;
   private static final int ID_CANCEL = 3;
   private static final int LIST_TOP = 32;
   private static final int FOOTER_AREA = 60;
   private static final int ITEM_HEIGHT = 18;
   private final ScreenBlueprintBrowser.Mode mode;
   private final Screen returnScreen;
   private List<String> names;
   private int selectedIndex = -1;
   private ScreenBlueprintBrowser.FileList list;
   private TextFieldElement nameField;
   private ButtonElement actionButton;
   private ButtonElement deleteButton;
   private ButtonElement cancelButton;
   private boolean confirmingOverwrite;
   private boolean confirmingDelete;
   private String statusLine;
   private int statusColour = 11184810;

   public ScreenBlueprintBrowser(Screen returnScreen, ScreenBlueprintBrowser.Mode mode) {
      super(returnScreen);
      this.returnScreen = returnScreen;
      this.mode = mode;
   }

   public void init() {
      this.names = BlueprintIO.listNames();
      this.list = new ScreenBlueprintBrowser.FileList();
      int fieldWidth = 180;
      int actionWidth = 70;
      int deleteWidth = 70;
      int cancelWidth = 70;
      int gap = 4;
      int totalWidth = fieldWidth + gap + actionWidth + gap + deleteWidth + gap + cancelWidth;
      int rowY = this.height - 40;
      int fieldX = this.width / 2 - totalWidth / 2;
      int actionX = fieldX + fieldWidth + gap;
      int deleteX = actionX + actionWidth + gap;
      int cancelX = deleteX + deleteWidth + gap;
      this.nameField = new TextFieldElement(this, this.font, fieldX, rowY, fieldWidth, 20, "", "Blueprint name");
      this.nameField.setMaxStringLength(48);
      this.nameField.setFocused(true);
      this.nameField.setTextChangeListener(tf -> {
         this.confirmingOverwrite = false;
         this.confirmingDelete = false;
         this.statusLine = null;
         this.selectedIndex = -1;
         this.updateActionButton();
         this.updateDeleteButton();
      });
      this.actionButton = new ButtonElement(1, actionX, rowY, actionWidth, 20, this.actionLabel());
      this.buttons.add(this.actionButton);
      this.deleteButton = new ButtonElement(2, deleteX, rowY, deleteWidth, 20, this.deleteLabel());
      this.buttons.add(this.deleteButton);
      this.cancelButton = new ButtonElement(3, cancelX, rowY, cancelWidth, 20, "Cancel");
      this.buttons.add(this.cancelButton);
      this.updateActionButton();
      this.updateDeleteButton();
   }

   private String actionLabel() {
      if (this.mode == ScreenBlueprintBrowser.Mode.LOAD) {
         return "Load";
      } else {
         return this.confirmingOverwrite ? "Overwrite" : (this.mode == ScreenBlueprintBrowser.Mode.CREATE_FROM_SELECTION ? "Create" : "Save");
      }
   }

   private String deleteLabel() {
      return this.confirmingDelete ? "Delete Now" : "Delete";
   }

   private void updateActionButton() {
      String name = this.nameField.getText().trim();
      if (this.mode == ScreenBlueprintBrowser.Mode.LOAD) {
         this.actionButton.enabled = !name.isEmpty() && BlueprintIO.exists(name);
      } else {
         this.actionButton.enabled = !name.isEmpty();
      }

      this.actionButton.displayString = this.actionLabel();
   }

   private void updateDeleteButton() {
      String name = this.nameField.getText().trim();
      this.deleteButton.enabled = !name.isEmpty() && BlueprintIO.exists(name);
      this.deleteButton.displayString = this.deleteLabel();
   }

   protected void buttonClicked(ButtonElement button) {
      if (button == this.cancelButton) {
         this.mc.displayScreen(this.returnScreen);
      } else if (button == this.deleteButton) {
         this.performDelete();
      } else {
         if (button == this.actionButton) {
            this.performAction();
         }
      }
   }

   private void performAction() {
      String name = this.nameField.getText().trim();
      if (!name.isEmpty()) {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc != null && mc.currentWorld != null) {
            if (this.mode != ScreenBlueprintBrowser.Mode.SAVE && this.mode != ScreenBlueprintBrowser.Mode.CREATE_FROM_SELECTION) {
               this.confirmingDelete = false;
               this.updateDeleteButton();
               if (!BlueprintIO.exists(name)) {
                  this.statusLine = "No blueprint named '" + name + "'.";
                  this.statusColour = 16733525;
               } else {
                  boolean ok = BlueprintIO.load(mc.currentWorld, DesignModeState.getControlPlayer(mc), name);
                  if (ok) {
                     this.mc.displayScreen(this.returnScreen);
                  } else {
                     this.statusLine = "Load failed (corrupted file?).";
                     this.statusColour = 16733525;
                  }
               }
            } else {
               this.confirmingDelete = false;
               this.updateDeleteButton();
               if (BlueprintIO.exists(name) && !this.confirmingOverwrite) {
                  this.confirmingOverwrite = true;
                  this.statusLine = "'" + name + "' already exists — click again to overwrite.";
                  this.statusColour = 16755200;
                  this.actionButton.displayString = this.actionLabel();
               } else {
                  boolean ok;
                  if (this.mode == ScreenBlueprintBrowser.Mode.CREATE_FROM_SELECTION) {
                     BlueprintSelection.Box box = BlueprintSelection.getBox(mc.currentWorld);
                     if (box == null) {
                        this.statusLine = "Selection is incomplete — set both corners first.";
                        this.statusColour = 16733525;
                        this.confirmingOverwrite = false;
                        this.actionButton.displayString = this.actionLabel();
                        return;
                     }

                     ok = BlueprintIO.saveRegion(mc.currentWorld, name, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
                  } else {
                     ok = BlueprintIO.save(mc.currentWorld, name);
                  }

                  if (ok) {
                     this.mc.displayScreen(this.returnScreen);
                  } else {
                     this.statusLine = this.mode == ScreenBlueprintBrowser.Mode.CREATE_FROM_SELECTION
                        ? "Save failed (selection empty or out of range?)."
                        : "Save failed (no holograms to save?).";
                     this.statusColour = 16733525;
                     this.confirmingOverwrite = false;
                     this.actionButton.displayString = this.actionLabel();
                  }
               }
            }
         }
      }
   }

   private void performDelete() {
      String name = this.nameField.getText().trim();
      if (!name.isEmpty()) {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc != null && mc.currentWorld != null) {
            this.confirmingOverwrite = false;
            this.actionButton.displayString = this.actionLabel();
            if (!BlueprintIO.exists(name)) {
               this.confirmingDelete = false;
               this.updateDeleteButton();
               this.statusLine = "No blueprint named '" + name + "'.";
               this.statusColour = 16733525;
            } else if (!this.confirmingDelete) {
               this.confirmingDelete = true;
               this.statusLine = "'" + name + "' will be deleted — click again to confirm.";
               this.statusColour = 16733525;
               this.updateDeleteButton();
            } else {
               File file = BlueprintIO.resolveFile(name);
               boolean ok = file != null && file.isFile() && file.delete();
               this.confirmingDelete = false;
               this.updateDeleteButton();
               this.updateActionButton();
               if (ok) {
                  this.names = BlueprintIO.listNames();
                  this.selectedIndex = -1;
                  this.statusLine = "Deleted '" + name + "'.";
                  this.statusColour = 16733525;
               } else {
                  this.statusLine = "Delete failed for '" + name + "'.";
                  this.statusColour = 16733525;
               }
            }
         }
      }
   }

   public void mouseClicked(int mx, int my, int buttonNum) {
      super.mouseClicked(mx, my, buttonNum);
      if (this.nameField != null) {
         this.nameField.mouseClicked(mx, my, buttonNum);
      }
   }

   public void keyPressed(char eventCharacter, int eventKey, int mx, int my) {
      if (this.nameField == null || !this.nameField.isFocused || eventKey == Keyboard.KEY_ESCAPE) {
         super.keyPressed(eventCharacter, eventKey, mx, my);
      } else if (eventKey == Keyboard.KEY_RETURN) {
         if (this.actionButton.enabled) {
            this.performAction();
         }
      } else {
         this.nameField.textboxKeyTyped(eventCharacter, eventKey);
      }
   }

   public void tick() {
      if (this.nameField != null) {
         this.nameField.updateCursorCounter();
      }
   }

   public void render(int mx, int my, float partialTick) {
      if (this.list != null) {
         this.list.render(mx, my, partialTick);
      }

      String title;
      switch (this.mode) {
         case SAVE:
            title = "Save Blueprint";
            break;
         case LOAD:
            title = "Load Blueprint";
            break;
         case CREATE_FROM_SELECTION:
            title = "Create Blueprint";
            break;
         default:
            title = "Blueprint";
      }

      this.drawStringCentered(this.font, title, this.width / 2, 12, 16777215);
      if (this.statusLine != null) {
         this.drawStringCentered(this.font, this.statusLine, this.width / 2, this.height - 54, this.statusColour);
      } else {
         this.drawStringCentered(this.font, "Saved to .minecraft/blueprints/", this.width / 2, this.height - 54, 8947848);
      }

      if (this.nameField != null) {
         this.nameField.drawTextBox();
      }

      super.render(mx, my, partialTick);
   }

   public boolean isPauseScreen() {
      return false;
   }

   private final class FileList extends ScrolledSelectionList {
      FileList() {
         super(
            ScreenBlueprintBrowser.this.mc,
            ScreenBlueprintBrowser.this.width,
            ScreenBlueprintBrowser.this.height,
            32,
            ScreenBlueprintBrowser.this.height - 60,
            18
         );
      }

      protected int getItemCount() {
         return ScreenBlueprintBrowser.this.names.size();
      }

      protected void selectItem(int itemIndex, boolean doubleClicked) {
         ScreenBlueprintBrowser.this.selectedIndex = itemIndex;
         if (itemIndex >= 0 && itemIndex < ScreenBlueprintBrowser.this.names.size()) {
            ScreenBlueprintBrowser.this.nameField.setText(ScreenBlueprintBrowser.this.names.get(itemIndex));
            ScreenBlueprintBrowser.this.selectedIndex = itemIndex;
            if (doubleClicked && ScreenBlueprintBrowser.this.actionButton.enabled) {
               ScreenBlueprintBrowser.this.performAction();
            }
         }

         ScreenBlueprintBrowser.this.updateActionButton();
      }

      protected boolean isSelectedItem(int itemIndex) {
         return itemIndex == ScreenBlueprintBrowser.this.selectedIndex;
      }

      protected void renderHoleBackground() {
         ScreenBlueprintBrowser.this.renderBackground();
      }

      protected void renderItem(int index, int x, int y, int height, Tessellator tessellator) {
         if (index >= 0 && index < ScreenBlueprintBrowser.this.names.size()) {
            ScreenBlueprintBrowser.this.drawString(ScreenBlueprintBrowser.this.font, ScreenBlueprintBrowser.this.names.get(index), x + 2, y + 4, 16777215);
         }
      }
   }

   public static enum Mode {
      SAVE,
      LOAD,
      CREATE_FROM_SELECTION;
   }
}
