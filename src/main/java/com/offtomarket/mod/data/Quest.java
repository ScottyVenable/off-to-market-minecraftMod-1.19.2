package com.offtomarket.mod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.offtomarket.mod.debug.DebugConfig;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * Represents a quest (trade order) from a town.
 * Towns request specific items and offer bonus coins + XP on delivery.
 * Quests refresh periodically and expire if not completed in time.
 * 
 * Quest types:
 * - STANDARD: Normal delivery quest based on town needs
 * - BULK: Large quantity of cheap items for bonus
 * - RUSH: Time-limited quest with better rewards
 * - SPECIALTY: Request for rare/valuable items
 * - CHARITY: Help a struggling town (lower profit, high reputation)
 */
public class Quest {
    public enum Status {
        /** Quest is available but not yet accepted. */
        AVAILABLE,
        /** Player has accepted this quest. */
        ACCEPTED,
        /** Items fully delivered, rewards traveling back from town. */
        DELIVERING,
        /** Quest has been fulfilled and rewards collected. */
        COMPLETED,
        /** Quest expired before completion. */
        EXPIRED
    }
    
    public enum QuestType {
        STANDARD("Delivery", 0xCCCCCC),
        BULK("Bulk Order", 0x88AAFF),
        RUSH("Rush Order", 0xFFAA44),
        SPECIALTY("Specialty", 0xDD88FF),
        CHARITY("Aid Request", 0x88FF88);
        
        private final String displayName;
        private final int color;
        
        QuestType(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public int getColor() { return color; }
    }

    private final UUID id;
    private final String townId;
    private final ResourceLocation requiredItemId;
    private final String itemDisplayName;
    private final int requiredCount;
    private final int rewardCoins;     // bonus coins (CP) on top of normal sale
    private final int rewardXp;        // bonus trader XP
    private final int rewardReputation; // reputation gained with town
    private final long createdTime;    // game tick when quest was generated
    private final long expiryTime;     // game tick when quest expires
    private final QuestType questType;
    private final String questDescription; // flavor text for the quest
    private Status status;
    private int deliveredCount;        // how many items delivered so far
    private long rewardArrivalTime;    // game tick when rewards arrive (DELIVERING→COMPLETED)

    public Quest(UUID id, String townId, ResourceLocation requiredItemId,
                 String itemDisplayName, int requiredCount, int rewardCoins,
                 int rewardXp, int rewardReputation, long createdTime, long expiryTime,
                 QuestType questType, String questDescription) {
        this.id = id;
        this.townId = townId;
        this.requiredItemId = requiredItemId;
        this.itemDisplayName = itemDisplayName;
        this.requiredCount = requiredCount;
        this.rewardCoins = rewardCoins;
        this.rewardXp = rewardXp;
        this.rewardReputation = rewardReputation;
        this.createdTime = createdTime;
        this.expiryTime = expiryTime;
        this.questType = questType;
        this.questDescription = questDescription;
        this.status = Status.AVAILABLE;
        this.deliveredCount = 0;
    }
    
    // Legacy constructor for backwards compatibility
    public Quest(UUID id, String townId, ResourceLocation requiredItemId,
                 String itemDisplayName, int requiredCount, int rewardCoins,
                 int rewardXp, long createdTime, long expiryTime) {
        this(id, townId, requiredItemId, itemDisplayName, requiredCount, rewardCoins,
                rewardXp, 5, createdTime, expiryTime, QuestType.STANDARD, "");
    }

    // ==================== Getters ====================

    public UUID getId() { return id; }
    public String getTownId() { return townId; }
    public ResourceLocation getRequiredItemId() { return requiredItemId; }
    public String getItemDisplayName() { return itemDisplayName; }
    public int getRequiredCount() { return requiredCount; }
    public int getRewardCoins() { return rewardCoins; }
    public int getRewardXp() { return rewardXp; }
    public int getRewardReputation() { return rewardReputation; }
    public long getCreatedTime() { return createdTime; }
    public long getExpiryTime() { return expiryTime; }
    public Status getStatus() { return status; }
    public int getDeliveredCount() { return deliveredCount; }
    public QuestType getQuestType() { return questType; }
    public String getQuestDescription() { return questDescription; }

    public void setStatus(Status status) { this.status = status; }
    public void setDeliveredCount(int count) { this.deliveredCount = count; }
    public long getRewardArrivalTime() { return rewardArrivalTime; }
    public void setRewardArrivalTime(long time) { this.rewardArrivalTime = time; }

    /**
     * Get ticks remaining until reward arrives (when in DELIVERING status).
     */
    public long getRewardTicksRemaining(long currentTime) {
        if (status != Status.DELIVERING) return 0;
        return Math.max(0, rewardArrivalTime - currentTime);
    }

    /**
     * Get remaining items needed to complete this quest.
     */
    public int getRemainingCount() {
        return Math.max(0, requiredCount - deliveredCount);
    }

    /**
     * Check if the quest has expired at the given time.
     */
    public boolean isExpired(long currentTime) {
        return currentTime >= expiryTime;
    }

    /**
     * Get time remaining in ticks.
     */
    public long getTicksRemaining(long currentTime) {
        return Math.max(0, expiryTime - currentTime);
    }

    /**
     * Record delivered items. Returns true if quest is now fully delivered.
     * Only valid when quest is ACCEPTED.
     */
    public boolean deliver(int count) {
        if (status != Status.ACCEPTED || count <= 0) return false;
        deliveredCount += count;
        if (deliveredCount >= requiredCount) {
            deliveredCount = requiredCount;
            status = Status.DELIVERING; // rewards travel back from town
            return true;
        }
        return false;
    }

    /**
     * Create an ItemStack for display purposes.
     */
    public ItemStack createDisplayStack() {
        Item item = ForgeRegistries.ITEMS.getValue(requiredItemId);
        if (item == null) return ItemStack.EMPTY;
        return new ItemStack(item, requiredCount);
    }

    // ==================== Quest Generation ====================

    /** Quest description templates by type */
    private static final String[] STANDARD_DESCS = {
        "The merchants need supplies.",
        "Regular shipment requested.",
        "Standard trade order.",
        "Goods needed for commerce."
    };
    
    private static final String[] BULK_DESCS = {
        "Building project requires materials!",
        "Festival preparations underway.",
        "Stockpiling for winter.",
        "Large order for the market."
    };
    
    private static final String[] RUSH_DESCS = {
        "URGENT: Supplies needed immediately!",
        "Emergency order - time is critical!",
        "Rush delivery - premium offered!",
        "Critical shortage - act fast!"
    };
    
    private static final String[] SPECIALTY_DESCS = {
        "Rare goods sought by collectors.",
        "Special request from nobility.",
        "Artisan materials required.",
        "Luxury items in demand."
    };
    
    private static final String[] CHARITY_DESCS = {
        "Town struggling - aid requested.",
        "Help the less fortunate.",
        "Charitable donation needed.",
        "Support the community."
    };

    /**
     * Generate random quests for a town based on its characteristics.
     * Now generates varied quest types with appropriate rewards.
     */
    public static List<Quest> generateQuests(TownData town, long gameTime, Random rand, int maxQuests) {
        List<Quest> quests = new ArrayList<>();
        List<ResourceLocation> needs = new ArrayList<>(town.getNeeds());
        List<ResourceLocation> surplus = new ArrayList<>(town.getSurplus());

        // Fallback: JSON towns use needLevels map rather than the legacy needs/surplus sets.
        // Build needs/surplus lists from needLevels entries so JSON-only towns get quests.
        if (needs.isEmpty()) {
            for (Map.Entry<String, NeedLevel> entry : town.getNeedLevels().entrySet()) {
                if (entry.getValue().isInDemand()) {
                    ResourceLocation rl = ResourceLocation.tryParse(entry.getKey());
                    if (rl != null) needs.add(rl);
                }
            }
        }
        if (surplus.isEmpty()) {
            for (Map.Entry<String, NeedLevel> entry : town.getNeedLevels().entrySet()) {
                if (entry.getValue().isOversupplied()) {
                    ResourceLocation rl = ResourceLocation.tryParse(entry.getKey());
                    if (rl != null) surplus.add(rl);
                }
            }
        }

        if (needs.isEmpty() && surplus.isEmpty()) return quests;

        DebugConfig.WATCH_QUEST_GEN_TOWN    = town.getId();
        DebugConfig.WATCH_QUEST_GEN_NEEDS   = needs.size();
        DebugConfig.WATCH_QUEST_GEN_SURPLUS = surplus.size();

        // Determine quest type distribution
        // Villages: more charity/bulk, Towns: balanced, Cities: more specialty/rush
        List<QuestType> possibleTypes = new ArrayList<>();
        switch (town.getType()) {
            case VILLAGE -> {
                possibleTypes.add(QuestType.STANDARD);
                possibleTypes.add(QuestType.STANDARD);
                possibleTypes.add(QuestType.BULK);
                possibleTypes.add(QuestType.CHARITY);
            }
            case TOWN -> {
                possibleTypes.add(QuestType.STANDARD);
                possibleTypes.add(QuestType.BULK);
                possibleTypes.add(QuestType.RUSH);
                possibleTypes.add(QuestType.SPECIALTY);
            }
            case CITY -> {
                possibleTypes.add(QuestType.STANDARD);
                possibleTypes.add(QuestType.RUSH);
                possibleTypes.add(QuestType.SPECIALTY);
                possibleTypes.add(QuestType.SPECIALTY);
            }
            case MARKET -> {
                possibleTypes.add(QuestType.STANDARD);
                possibleTypes.add(QuestType.BULK);
                possibleTypes.add(QuestType.BULK);
                possibleTypes.add(QuestType.SPECIALTY);
            }
            case OUTPOST -> {
                possibleTypes.add(QuestType.STANDARD);
                possibleTypes.add(QuestType.STANDARD);
                possibleTypes.add(QuestType.BULK);
                possibleTypes.add(QuestType.RUSH);
            }
        }

        Collections.shuffle(needs, rand);
        int count = Math.min(maxQuests, needs.size());

        for (int i = 0; i < count; i++) {
            ResourceLocation itemId = needs.get(i);
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) continue;

            ItemStack refStack = new ItemStack(item);
            String displayName = refStack.getHoverName().getString();
            int itemBaseValue = PriceCalculator.getBaseValue(refStack);
            
            // Choose quest type
            QuestType questType = possibleTypes.get(rand.nextInt(possibleTypes.size()));
            
            // Get description based on type
            String description = switch (questType) {
                case STANDARD -> STANDARD_DESCS[rand.nextInt(STANDARD_DESCS.length)];
                case BULK -> BULK_DESCS[rand.nextInt(BULK_DESCS.length)];
                case RUSH -> RUSH_DESCS[rand.nextInt(RUSH_DESCS.length)];
                case SPECIALTY -> SPECIALTY_DESCS[rand.nextInt(SPECIALTY_DESCS.length)];
                case CHARITY -> CHARITY_DESCS[rand.nextInt(CHARITY_DESCS.length)];
            };

            // Base quantity based on item value
            int reqCount = calculateBaseQuantity(itemBaseValue, rand);
            
            // Modify quantity based on quest type
            reqCount = switch (questType) {
                case BULK -> (int)(reqCount * 2.5); // Bulk orders need much more
                case RUSH -> Math.max(1, reqCount / 2); // Rush orders need less
                case SPECIALTY -> Math.max(1, reqCount / 3); // Specialty needs very few
                case CHARITY -> reqCount; // Normal amount
                default -> reqCount;
            };

            // Calculate base reward
            int totalItemCost = itemBaseValue * reqCount;
            double profitMargin = 1.30 + rand.nextDouble() * 0.45;
            double urgency = 1.0 + rand.nextDouble() * 0.40;
            double typeMult = switch (town.getType()) {
                case VILLAGE -> 1.0;
                case TOWN -> 1.2;
                case CITY -> 1.5;
                case MARKET -> 1.3;
                case OUTPOST -> 0.9;
            };

            // Modify reward based on quest type
            double questTypeMult = switch (questType) {
                case BULK -> 1.1; // Slight bonus for volume
                case RUSH -> 1.5; // Big bonus for speed
                case SPECIALTY -> 1.3; // Good bonus for rare items
                case CHARITY -> 0.6; // Lower profit, but high reputation
                default -> 1.0;
            };

            int baseReward = Math.max(5, (int) (totalItemCost * profitMargin * urgency * typeMult * questTypeMult));

            // XP reward
            int xpReward = Math.max(5, Math.min(50, totalItemCost / 15));
            xpReward = (int) (xpReward * typeMult);
            
            // Reputation reward - varies by quest type (higher scale: max 1000, not 200)
            int repReward = switch (questType) {
                case CHARITY -> 75 + rand.nextInt(51);    // 75-125 for charity
                case SPECIALTY -> 40 + rand.nextInt(36);  // 40-75 for specialty
                case RUSH -> 25 + rand.nextInt(26);       // 25-50 for rush
                case BULK -> 20 + rand.nextInt(21);       // 20-40 for bulk
                default -> 15 + rand.nextInt(21);         // 15-35 for standard
            };

            // Quest duration based on type
            long duration = switch (questType) {
                case RUSH -> 12000L + rand.nextInt(12001); // 0.5-1 day for rush
                case BULK -> 72000L + rand.nextInt(72001); // 3-6 days for bulk
                default -> 48000L + rand.nextInt(48001); // 2-4 days normally
            };

            quests.add(new Quest(
                    UUID.randomUUID(), town.getId(), itemId, displayName,
                    reqCount, baseReward, xpReward, repReward, gameTime, gameTime + duration,
                    questType, description
            ));
        }

        return quests;
    }
    
    private static int calculateBaseQuantity(int itemBaseValue, Random rand) {
        if (itemBaseValue <= 8) {
            return 16 + rand.nextInt(33);      // 16-48 for junk/basic
        } else if (itemBaseValue <= 20) {
            return 8 + rand.nextInt(17);       // 8-24 for common items
        } else if (itemBaseValue <= 65) {
            return 4 + rand.nextInt(9);        // 4-12 for useful items
        } else if (itemBaseValue <= 210) {
            return 2 + rand.nextInt(5);        // 2-6 for valuable items
        } else if (itemBaseValue <= 650) {
            return 1 + rand.nextInt(3);        // 1-3 for expensive items
        } else {
            return 1;                          // 1 for treasure-tier
        }
    }

    // ==================== NBT ====================

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Town", townId);
        tag.putString("Item", requiredItemId.toString());
        tag.putString("Name", itemDisplayName);
        tag.putInt("ReqCount", requiredCount);
        tag.putInt("RewardCoins", rewardCoins);
        tag.putInt("RewardXp", rewardXp);
        tag.putInt("RewardRep", rewardReputation);
        tag.putLong("Created", createdTime);
        tag.putLong("Expiry", expiryTime);
        tag.putString("Status", status.name());
        tag.putInt("Delivered", deliveredCount);
        tag.putLong("RewardArrival", rewardArrivalTime);
        tag.putString("QuestType", questType.name());
        tag.putString("Desc", questDescription);
        return tag;
    }

    public static Quest load(CompoundTag tag) {
        // Handle legacy quests without new fields
        QuestType qType = QuestType.STANDARD;
        if (tag.contains("QuestType")) {
            try {
                qType = QuestType.valueOf(tag.getString("QuestType"));
            } catch (IllegalArgumentException ignored) {}
        }
        
        int repReward = tag.contains("RewardRep") ? tag.getInt("RewardRep") : 5;
        String desc = tag.contains("Desc") ? tag.getString("Desc") : "";
        
        Quest quest = new Quest(
                tag.getUUID("Id"),
                tag.getString("Town"),
                new ResourceLocation(tag.getString("Item")),
                tag.getString("Name"),
                tag.getInt("ReqCount"),
                tag.getInt("RewardCoins"),
                tag.getInt("RewardXp"),
                repReward,
                tag.getLong("Created"),
                tag.getLong("Expiry"),
                qType,
                desc
        );
        quest.status = Status.valueOf(tag.getString("Status"));
        quest.deliveredCount = tag.getInt("Delivered");
        quest.rewardArrivalTime = tag.contains("RewardArrival") ? tag.getLong("RewardArrival") : 0;
        // Backward compat: legacy DELIVERING quests without arrival time complete instantly
        if (quest.status == Status.DELIVERING && quest.rewardArrivalTime == 0) {
            quest.status = Status.COMPLETED;
        }
        return quest;
    }
}
