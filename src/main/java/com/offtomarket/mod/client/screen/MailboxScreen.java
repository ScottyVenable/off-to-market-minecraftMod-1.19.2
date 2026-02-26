package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.block.entity.MailboxBlockEntity;
import com.offtomarket.mod.data.MailNote;
import com.offtomarket.mod.menu.MailboxMenu;
import com.offtomarket.mod.network.DeleteAllReadNotesPacket;
import com.offtomarket.mod.network.DeleteNotePacket;
import com.offtomarket.mod.network.MarkNoteReadPacket;
import com.offtomarket.mod.network.ModNetwork;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI screen for the Mailbox.
 * Left panel: selectable list of notes with scroll.
 * Right panel: full note content for the selected note.
 * Buttons: Delete, Delete All Read, scroll arrows.
 */
public class MailboxScreen extends AbstractContainerScreen<MailboxMenu> {

    // Layout constants
    private static final int GUI_WIDTH = 384;
    private static final int GUI_HEIGHT = 230;

    // List panel
    private static final int LIST_X = 5;
    private static final int LIST_Y = 30;
    private static final int LIST_W = 150;
    private static final int LIST_ROW_H = 14;
    private static final int VISIBLE_ROWS = 13;

    // Content panel
    private static final int CONTENT_X = 160;
    private static final int CONTENT_Y = 30;
    private static final int CONTENT_W = 218;
    private static final int CONTENT_H = 182;

    // Buttons
    private Button scrollUpBtn;
    private Button scrollDownBtn;
    private Button deleteBtn;
    private Button deleteAllReadBtn;

    private int scrollOffset = 0;
    private int selectedIndex = -1;
    private UUID selectedNoteId = null;
    private int hoveredRow = -1;

    public MailboxScreen(MailboxMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = -999; // hide inventory label
        this.titleLabelY = -999;    // we draw our own title
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // Scroll buttons for the note list
        scrollUpBtn = addRenderableWidget(new Button(
                x + LIST_X + LIST_W - 12, y + LIST_Y - 1, 12, 10,
                Component.literal("\u25B2"),
                btn -> { if (scrollOffset > 0) scrollOffset--; }));

        scrollDownBtn = addRenderableWidget(new Button(
                x + LIST_X + LIST_W - 12, y + LIST_Y + VISIBLE_ROWS * LIST_ROW_H - 9, 12, 10,
                Component.literal("\u25BC"),
                btn -> {
                    int maxScroll = getMaxScroll();
                    if (scrollOffset < maxScroll) scrollOffset++;
                }));

        // Delete selected note
        deleteBtn = addRenderableWidget(new Button(
                x + CONTENT_X, y + GUI_HEIGHT - 16, 60, 14,
                Component.literal("Delete"),
                btn -> {
                    if (selectedNoteId != null) {
                        MailboxBlockEntity be = menu.getBlockEntity();
                        if (be != null) {
                            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                    new DeleteNotePacket(be.getBlockPos(), selectedNoteId));
                            selectedIndex = -1;
                            selectedNoteId = null;
                        }
                    }
                }));

        // Delete all read notes
        deleteAllReadBtn = addRenderableWidget(new Button(
                x + CONTENT_X + 65, y + GUI_HEIGHT - 16, 70, 14,
                Component.literal("Delete Read"),
                btn -> {
                    MailboxBlockEntity be = menu.getBlockEntity();
                    if (be != null) {
                        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                new DeleteAllReadNotesPacket(be.getBlockPos()));
                        selectedIndex = -1;
                        selectedNoteId = null;
                    }
                }));
    }

    private int getMaxScroll() {
        MailboxBlockEntity be = menu.getBlockEntity();
        if (be == null) return 0;
        return Math.max(0, be.getNotes().size() - VISIBLE_ROWS);
    }

    // ==================== Rendering ====================

    @Override
    protected void renderBg(PoseStack ps, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Main panel
        drawPanel(ps, x, y, GUI_WIDTH, GUI_HEIGHT);

        // Title bar
        fill(ps, x + 2, y + 2, x + GUI_WIDTH - 2, y + 18, 0xFF2C2318);

        // List panel inset
        drawInsetPanel(ps, x + LIST_X, y + LIST_Y, LIST_W, VISIBLE_ROWS * LIST_ROW_H);

        // Content panel inset
        drawInsetPanel(ps, x + CONTENT_X, y + CONTENT_Y, CONTENT_W, CONTENT_H);

        // Render note list rows
        renderNoteList(ps, x, y, mouseX, mouseY);
    }

    private void renderNoteList(PoseStack ps, int x, int y, int mouseX, int mouseY) {
        MailboxBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        List<MailNote> notes = be.getNotes();

        hoveredRow = -1;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int noteIdx = scrollOffset + i;
            if (noteIdx >= notes.size()) break;

            int rowX = x + LIST_X;
            int rowY = y + LIST_Y + i * LIST_ROW_H;
            int rowRight = rowX + LIST_W;
            int rowBottom = rowY + LIST_ROW_H;

            boolean isSelected = noteIdx == selectedIndex;
            boolean isHovered = mouseX >= rowX && mouseX < rowRight
                    && mouseY >= rowY && mouseY < rowBottom;

            if (isHovered) hoveredRow = i;

            // Row background
            if (isSelected) {
                fill(ps, rowX, rowY, rowRight, rowBottom, 0xFF5A4A30);
                fill(ps, rowX, rowY, rowX + 2, rowBottom, 0xFFFFD700); // Gold accent
            } else if (isHovered) {
                fill(ps, rowX, rowY, rowRight, rowBottom, 0xFF4A3D2B);
            } else {
                int rowColor = (i % 2 == 0) ? 0xFF3E3226 : 0xFF36291E;
                fill(ps, rowX, rowY, rowRight, rowBottom, rowColor);
            }

            // Unread indicator
            MailNote note = notes.get(noteIdx);
            if (!note.isRead()) {
                fill(ps, rowX + 2, rowY + 4, rowX + 6, rowY + 8, 0xFFFFD700); // Gold dot
            }
        }

        // Scroll indicator
        int total = notes.size();
        if (total > VISIBLE_ROWS) {
            int barH = Math.max(10, (VISIBLE_ROWS * LIST_ROW_H) * VISIBLE_ROWS / total);
            int barY = y + LIST_Y + (int) ((VISIBLE_ROWS * LIST_ROW_H - barH)
                    * ((float) scrollOffset / getMaxScroll()));
            fill(ps, x + LIST_X + LIST_W - 3, barY, x + LIST_X + LIST_W - 1, barY + barH, 0xFF8B7355);
        }
    }

    @Override
    protected void renderLabels(PoseStack ps, int mouseX, int mouseY) {
        // Title
        String title = "Mailbox";
        MailboxBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            int unread = be.getUnreadCount();
            if (unread > 0) {
                title = "Mailbox (" + unread + " unread)";
            }
        }
        int titleW = this.font.width(title);
        this.font.draw(ps, title, (GUI_WIDTH - titleW) / 2f, 6, 0xFFD700);

        // Note list labels
        if (be != null) {
            List<MailNote> notes = be.getNotes();
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int noteIdx = scrollOffset + i;
                if (noteIdx >= notes.size()) break;

                MailNote note = notes.get(noteIdx);
                int rowY = LIST_Y + i * LIST_ROW_H;

                // Category tag
                String category = "[" + note.getType().getCategory() + "]";
                int catColor = getCategoryColor(note.getType());
                this.font.draw(ps, category, LIST_X + 8, rowY + 3, catColor);

                // Subject (truncated)
                int catW = this.font.width(category) + 2;
                int maxSubjW = LIST_W - catW - 14;
                String subject = note.getSubject();
                if (this.font.width(subject) > maxSubjW) {
                    while (this.font.width(subject + "...") > maxSubjW && subject.length() > 0) {
                        subject = subject.substring(0, subject.length() - 1);
                    }
                    subject += "...";
                }
                int textColor = note.isRead() ? 0xAAAAAA : 0xFFFFFF;
                this.font.draw(ps, subject, LIST_X + 8 + catW, rowY + 3, textColor);
            }

            // Empty state
            if (notes.isEmpty()) {
                String empty = "No mail yet!";
                int emptyW = this.font.width(empty);
                this.font.draw(ps, empty, LIST_X + (LIST_W - emptyW) / 2f,
                        LIST_Y + 40, 0x888888);
            }
        }

        // Content panel — render selected note
        renderNoteContent(ps);

        // Note count
        if (be != null) {
            String count = be.getNotes().size() + " notes";
            this.font.draw(ps, count, LIST_X, LIST_Y + VISIBLE_ROWS * LIST_ROW_H + 3, 0x888888);
        }
    }

    private void renderNoteContent(PoseStack ps) {
        MailboxBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        List<MailNote> notes = be.getNotes();

        if (selectedIndex < 0 || selectedIndex >= notes.size()) {
            // No selection prompt
            String hint = "Select a note to read";
            int hintW = this.font.width(hint);
            this.font.draw(ps, hint, CONTENT_X + (CONTENT_W - hintW) / 2f,
                    CONTENT_Y + 80, 0x666666);
            return;
        }

        MailNote note = notes.get(selectedIndex);

        // Subject header
        int subjectColor = 0xFFD700;
        List<FormattedCharSequence> subjectLines = this.font.split(
                Component.literal(note.getSubject()), CONTENT_W - 10);
        int lineY = CONTENT_Y + 4;
        for (FormattedCharSequence line : subjectLines) {
            this.font.draw(ps, line, CONTENT_X + 5, lineY, subjectColor);
            lineY += 10;
        }

        // Sender + category
        lineY += 2;
        String fromLine = "From: " + note.getSender();
        this.font.draw(ps, fromLine, CONTENT_X + 5, lineY, 0xAAAAAA);
        lineY += 10;

        // Separator
        fill(ps, CONTENT_X + 5, lineY, CONTENT_X + CONTENT_W - 5, lineY + 1, 0xFF5C4A32);
        lineY += 5;

        // Body text — word-wrap
        List<FormattedCharSequence> bodyLines = this.font.split(
                Component.literal(note.getBody()), CONTENT_W - 10);
        int bodyAreaHeight = CONTENT_Y + CONTENT_H - lineY - 2;
        int maxBodyLines = bodyAreaHeight / 10;
        for (int i = 0; i < Math.min(bodyLines.size(), maxBodyLines); i++) {
            this.font.draw(ps, bodyLines.get(i), CONTENT_X + 5, lineY, 0xDDDDDD);
            lineY += 10;
        }
        if (bodyLines.size() > maxBodyLines) {
            this.font.draw(ps, "...", CONTENT_X + 5, lineY, 0x888888);
        }
    }

    private int getCategoryColor(MailNote.NoteType type) {
        return switch (type) {
            case DIPLOMAT_FAILURE -> 0xFF6644; // Red-orange
            case QUEST_COMPLETED -> 0x55FF55;  // Green
            case QUEST_EXPIRED -> 0xFFCC44;    // Yellow
            case SHIPMENT_RECEIVED -> 0x55CCFF; // Light blue
            case PURCHASE_MADE -> 0xCC88FF;    // Purple
        };
    }

    // ==================== Render & Tooltips ====================

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTick) {
        renderBackground(ps);
        super.render(ps, mouseX, mouseY, partialTick);

        // Tooltip for hovered row
        if (hoveredRow >= 0) {
            MailboxBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                int noteIdx = scrollOffset + hoveredRow;
                List<MailNote> notes = be.getNotes();
                if (noteIdx < notes.size()) {
                    MailNote note = notes.get(noteIdx);
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.literal(note.getSubject()));
                    tooltip.add(Component.literal("From: " + note.getSender())
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                    tooltip.add(Component.literal(note.isRead() ? "Read" : "Unread")
                            .withStyle(note.isRead() ? net.minecraft.ChatFormatting.GRAY
                                    : net.minecraft.ChatFormatting.GOLD));
                    renderComponentTooltip(ps, tooltip, mouseX, mouseY);
                }
            }
        }

        // Update button states
        deleteBtn.active = selectedNoteId != null;
        MailboxBlockEntity be = menu.getBlockEntity();
        deleteAllReadBtn.active = be != null && be.getNotes().stream().anyMatch(MailNote::isRead);
    }

    // ==================== Mouse Handling ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = this.leftPos;
            int y = this.topPos;

            // Check list click
            int listLeft = x + LIST_X;
            int listTop = y + LIST_Y;
            int listRight = listLeft + LIST_W - 14; // leave room for scrollbar
            int listBottom = listTop + VISIBLE_ROWS * LIST_ROW_H;

            if (mouseX >= listLeft && mouseX < listRight
                    && mouseY >= listTop && mouseY < listBottom) {
                int row = (int) ((mouseY - listTop) / LIST_ROW_H);
                int noteIdx = scrollOffset + row;
                MailboxBlockEntity be = menu.getBlockEntity();
                if (be != null && noteIdx < be.getNotes().size()) {
                    selectedIndex = noteIdx;
                    MailNote note = be.getNotes().get(noteIdx);
                    selectedNoteId = note.getId();

                    // Mark as read
                    if (!note.isRead()) {
                        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                new MarkNoteReadPacket(be.getBlockPos(), note.getId()));
                        note.markRead(); // Update client-side immediately
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int x = this.leftPos;
        int y = this.topPos;

        // Scroll the note list
        if (mouseX >= x + LIST_X && mouseX <= x + LIST_X + LIST_W
                && mouseY >= y + LIST_Y && mouseY <= y + LIST_Y + VISIBLE_ROWS * LIST_ROW_H) {
            if (delta > 0 && scrollOffset > 0) scrollOffset--;
            else if (delta < 0 && scrollOffset < getMaxScroll()) scrollOffset++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    // ==================== Helpers ====================

    private void drawPanel(PoseStack ps, int x, int y, int w, int h) {
        OtmGuiTheme.drawPanel(ps, x, y, w, h);
    }

    private void drawInsetPanel(PoseStack ps, int x, int y, int w, int h) {
        OtmGuiTheme.drawInsetPanel(ps, x, y, w, h);
    }
}
