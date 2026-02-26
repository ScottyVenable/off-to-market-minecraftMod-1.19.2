package com.offtomarket.mod.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Represents a single mail note/letter stored in a Mailbox.
 * Notes are generated from various trading events and stored for the player to read.
 */
public class MailNote {
    private final UUID id;
    private final NoteType type;
    private final String subject;
    private final String body;
    private final String sender;
    private final long timestamp; // game time when created
    private boolean read;

    public enum NoteType {
        DIPLOMAT_FAILURE("Diplomat"),
        QUEST_COMPLETED("Quest"),
        QUEST_EXPIRED("Quest"),
        SHIPMENT_RECEIVED("Shipment"),
        PURCHASE_MADE("Purchase");

        private final String category;

        NoteType(String category) { this.category = category; }
        public String getCategory() { return category; }
    }

    public MailNote(NoteType type, String subject, String body, String sender, long timestamp) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.subject = subject;
        this.body = body;
        this.sender = sender;
        this.timestamp = timestamp;
        this.read = false;
    }

    private MailNote(UUID id, NoteType type, String subject, String body, String sender,
                     long timestamp, boolean read) {
        this.id = id;
        this.type = type;
        this.subject = subject;
        this.body = body;
        this.sender = sender;
        this.timestamp = timestamp;
        this.read = read;
    }

    // Getters
    public UUID getId() { return id; }
    public NoteType getType() { return type; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getSender() { return sender; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }

    public void markRead() { this.read = true; }

    // ==================== NBT ====================

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Type", type.name());
        tag.putString("Subject", subject);
        tag.putString("Body", body);
        tag.putString("Sender", sender);
        tag.putLong("Timestamp", timestamp);
        tag.putBoolean("Read", read);
        return tag;
    }

    public static MailNote load(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        NoteType type = NoteType.valueOf(tag.getString("Type"));
        String subject = tag.getString("Subject");
        String body = tag.getString("Body");
        String sender = tag.getString("Sender");
        long timestamp = tag.getLong("Timestamp");
        boolean read = tag.getBoolean("Read");
        return new MailNote(id, type, subject, body, sender, timestamp, read);
    }
}
