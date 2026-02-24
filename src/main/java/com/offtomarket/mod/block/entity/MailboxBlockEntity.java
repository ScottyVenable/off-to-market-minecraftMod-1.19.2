package com.offtomarket.mod.block.entity;

import com.offtomarket.mod.data.MailNote;
import com.offtomarket.mod.menu.MailboxMenu;
import com.offtomarket.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Block entity for the Mailbox. Stores a list of MailNote objects.
 * Notes are added by the TradingPostBlockEntity when events occur,
 * and read/deleted by the player through the MailboxMenu/Screen.
 */
public class MailboxBlockEntity extends BlockEntity implements MenuProvider {

    private final List<MailNote> notes = new ArrayList<>();
    public static final int MAX_NOTES = 100; // prevent unbounded growth

    public MailboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAILBOX.get(), pos, state);
    }

    // ==================== Note Management ====================

    public List<MailNote> getNotes() {
        return notes;
    }

    public int getUnreadCount() {
        int count = 0;
        for (MailNote note : notes) {
            if (!note.isRead()) count++;
        }
        return count;
    }

    /**
     * Add a new note to the mailbox. Oldest notes are removed if at capacity.
     */
    public void addNote(MailNote note) {
        while (notes.size() >= MAX_NOTES) {
            notes.remove(0); // Remove oldest
        }
        notes.add(note);
        syncToClient();
    }

    /**
     * Mark a note as read by its UUID.
     */
    public void markRead(UUID noteId) {
        for (MailNote note : notes) {
            if (note.getId().equals(noteId)) {
                note.markRead();
                syncToClient();
                return;
            }
        }
    }

    /**
     * Delete a note by its UUID.
     */
    public void deleteNote(UUID noteId) {
        notes.removeIf(n -> n.getId().equals(noteId));
        syncToClient();
    }

    /**
     * Delete all read notes.
     */
    public void deleteAllRead() {
        notes.removeIf(MailNote::isRead);
        syncToClient();
    }

    // ==================== Sync ====================

    public void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag noteList = new ListTag();
        for (MailNote note : notes) {
            noteList.add(note.save());
        }
        tag.put("Notes", noteList);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        notes.clear();
        if (tag.contains("Notes", Tag.TAG_LIST)) {
            ListTag noteList = tag.getList("Notes", Tag.TAG_COMPOUND);
            for (int i = 0; i < noteList.size(); i++) {
                notes.add(MailNote.load(noteList.getCompound(i)));
            }
        }
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.offtomarket.mailbox");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new MailboxMenu(containerId, inv, this);
    }
}
