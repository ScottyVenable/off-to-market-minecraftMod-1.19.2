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

    /**
     * Process an interactive dialogue / persuasion reply to a mail note.
     * Choice 0: Warm diplomatic response (safe +5 Reputation, +5 Trader XP).
     * Choice 1: Persuade for better terms / rebate (scales with Trader Level, Negotiator Level, and Reputation).
     */
    public void replyToNote(UUID noteId, int choiceIndex, Player player) {
        if (level == null || level.isClientSide()) return;

        MailNote target = null;
        for (MailNote n : notes) {
            if (n.getId().equals(noteId)) {
                target = n;
                break;
            }
        }
        if (target == null || target.isReplied()) return;

        // Find nearest TradingPostBlockEntity within 32 blocks to apply reputation/XP/coins
        TradingPostBlockEntity post = null;
        int radius = 32;
        for (BlockPos p : BlockPos.betweenClosed(
                worldPosition.offset(-radius, -8, -radius),
                worldPosition.offset(radius, 8, radius))) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof TradingPostBlockEntity tp) {
                post = tp;
                break;
            }
        }

        // Resolve sender town ID
        String townId = "greenhollow";
        String senderLower = target.getSender() != null ? target.getSender().toLowerCase() : "";
        for (com.offtomarket.mod.data.TownData td : com.offtomarket.mod.data.TownRegistry.getAllTowns()) {
            if (senderLower.contains(td.getDisplayName().toLowerCase())
                    || senderLower.contains(td.getId().toLowerCase())) {
                townId = td.getId();
                break;
            }
        }

        if (choiceIndex == 0) {
            // Safe friendly reply
            if (post != null) {
                post.addReputation(townId, 5);
                post.addTraderXp(5);
            }
            String summary = "Sent Warm Regards (+5 Rep, +5 Trader XP)";
            target.markReplied(summary);
            syncToClient();
            if (player != null) {
                player.displayClientMessage(Component.literal(
                        "\u00A7a[Mail] \u00A7fYou sent a warm diplomatic reply to " + target.getSender()
                                + " (+5 Rep, +5 XP)."), true);
            }
        } else {
            // Persuasion check
            int traderLvl = post != null ? post.getTraderLevel() : 1;
            int negLvl = (post != null && post.getNegotiator().isHired()) ? post.getNegotiator().getLevel() : 0;
            int rep = post != null ? post.getReputation(townId) : 0;

            // Base 45% + 4% per Trader Level + 5% per Negotiator Level + up to 20% from Reputation
            int chancePct = Math.min(92, Math.max(20, 45 + traderLvl * 4 + negLvl * 5 + (rep / 30)));
            int roll = level.getRandom().nextInt(100);
            if (roll < chancePct) {
                int bonusCoins = 25 + traderLvl * 8 + negLvl * 6;
                if (post != null) {
                    post.addReputation(townId, 12);
                    post.addTraderXp(10);
                    FinanceTableBlockEntity ft = TradingPostBlockEntity.findNearbyFinanceTable(level, post.getBlockPos());
                    if (ft != null) {
                        ft.setBalance(ft.getBalance() + bonusCoins);
                        ft.recordTransaction("Mail Rebate", bonusCoins, "Persuaded " + target.getSender());
                        ft.syncToClient();
                    } else if (player != null) {
                        player.getInventory().add(new net.minecraft.world.item.ItemStack(
                                com.offtomarket.mod.registry.ModItems.COPPER_COIN.get(),
                                Math.min(64, bonusCoins)));
                    }
                }
                String summary = "Persuasion Succeeded! (+12 Rep, +" + bonusCoins + "c Rebate)";
                target.markReplied(summary);
                syncToClient();
                if (player != null) {
                    player.displayClientMessage(Component.literal(
                            "\u00A76[Persuasion Success!] \u00A7f" + target.getSender()
                                    + " granted better terms (+12 Rep, +" + bonusCoins + "c)!"), true);
                }
            } else {
                if (post != null) {
                    post.addReputation(townId, -2);
                    post.addTraderXp(2);
                }
                String summary = "Persuasion Declined (" + chancePct + "% odds; -2 Rep, +2 XP)";
                target.markReplied(summary);
                syncToClient();
                if (player != null) {
                    player.displayClientMessage(Component.literal(
                            "\u00A7e[Persuasion Failed] \u00A77" + target.getSender()
                                    + " politely declined your counter-offer (-2 Rep)."), true);
                }
            }
        }
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
