package com.offtomarket.mod.block.entity;

import com.offtomarket.mod.block.MailboxBlock;
import com.offtomarket.mod.data.*;
import com.offtomarket.mod.debug.DebugConfig;
import com.offtomarket.mod.item.CoinItem;
import com.offtomarket.mod.item.CoinType;
import com.offtomarket.mod.menu.TradingPostMenu;
import com.offtomarket.mod.registry.ModBlockEntities;
import com.offtomarket.mod.registry.ModItems;
import com.offtomarket.mod.util.SoundHelper;
import com.offtomarket.mod.util.ToastHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The Trading Post is the main hub. It manages:
 * - Shipments to/from towns
 * - Trader level / XP
 * - Selected town for trading
 * - Ledger sync slot
 * - Distance min/max settings
 * - Incoming coin storage
 */
public class TradingPostBlockEntity extends BlockEntity implements MenuProvider {

    // Trader progression
    private int traderLevel = 1;
    private int traderXp = 0;

    // Town selection
    private String selectedTownId = "greenhollow";
    private int minDistance = 1;
    private int maxDistance = 10;

    // Shipments
    private final List<Shipment> activeShipments = new ArrayList<>();

    // Ledger slot (stores a single ledger item)
    private ItemStack ledgerSlot = ItemStack.EMPTY;

    // Earned coins waiting to be collected
    private int pendingCoins = 0; // in copper pieces

    // Sale check timer
    private int saleCheckTimer = 0;

    // Market listings
    private final List<MarketListing> marketListings = new ArrayList<>();

    // Completed shipment history (newest first, capped)
    private final List<CompoundTag> shipmentHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 50;

    // Economy tracking
    private long lifetimeEarnings = 0;       // total coins ever earned (copper pieces)
    private int totalShipmentsSent = 0;      // total shipments completed
    private final Map<String, Long> earningsByTown = new HashMap<>();    // townId → total earnings
    private final Map<String, Long> earningsByItem = new HashMap<>();    // item display name → total earnings

    // Dynamic demand tracking
    private final DemandTracker demandTracker = new DemandTracker();
    
    // Town reputation tracking (reputation gained from completing quests and trading)
    private final Map<String, Integer> townReputation = new HashMap<>();

    // Buy orders (items purchased from market, in transit to player)
    private final List<BuyOrder> activeBuyOrders = new ArrayList<>();

    // Quests (trade orders from towns for bonus rewards)
    private final List<Quest> activeQuests = new ArrayList<>();
    private long lastQuestRefreshDay = -1;

    // Workers (hired helpers that provide passive bonuses)
    private Worker negotiator = new Worker(Worker.WorkerType.NEGOTIATOR);
    private Worker tradingCart = new Worker(Worker.WorkerType.TRADING_CART);
    private Worker bookkeeper = new Worker(Worker.WorkerType.BOOKKEEPER);

    // Diplomat requests (player requests specific items from towns)
    private final List<DiplomatRequest> activeDiplomatRequests = new ArrayList<>();

    // Dawn-based market refresh tracking
    private long lastRefreshDay = -1;

    // Prevents recursive propagation when syncing linked posts
    private boolean isSyncing = false;

    public TradingPostBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRADING_POST.get(), pos, state);
    }

    // ==================== Getters/Setters ====================

    public int getTraderLevel() { return traderLevel; }
    public int getTraderXp() { return traderXp; }
    public String getSelectedTownId() { return selectedTownId; }
    public int getMinDistance() { return minDistance; }
    public int getMaxDistance() { return maxDistance; }
    public List<Shipment> getActiveShipments() { return activeShipments; }
    public ItemStack getLedgerSlot() { return ledgerSlot; }
    public int getPendingCoins() { return pendingCoins; }
    public List<MarketListing> getMarketListings() { return marketListings; }
    public List<CompoundTag> getShipmentHistory() { return shipmentHistory; }
    public DemandTracker getDemandTracker() { return demandTracker; }
    public long getLifetimeEarnings() { return lifetimeEarnings; }
    public int getTotalShipmentsSent() { return totalShipmentsSent; }
    public Map<String, Long> getEarningsByTown() { return Collections.unmodifiableMap(earningsByTown); }
    public Map<String, Long> getEarningsByItem() { return Collections.unmodifiableMap(earningsByItem); }
    public List<BuyOrder> getActiveBuyOrders() { return activeBuyOrders; }
    public List<Quest> getActiveQuests() { return activeQuests; }
    public Worker getNegotiator() { return negotiator; }
    public Worker getTradingCart() { return tradingCart; }
    public Worker getBookkeeper() { return bookkeeper; }

    /**
     * Get a worker by type.
     */
    public Worker getWorker(Worker.WorkerType type) {
        return switch (type) {
            case NEGOTIATOR -> negotiator;
            case TRADING_CART -> tradingCart;
            case BOOKKEEPER -> bookkeeper;
        };
    }

    public List<DiplomatRequest> getActiveDiplomatRequests() { return activeDiplomatRequests; }
    public long getLastRefreshDay() { return lastRefreshDay; }
    public Map<String, Integer> getTownReputation() { return Collections.unmodifiableMap(townReputation); }
    
    /**
     * Get reputation with a specific town. Returns 0 if no reputation exists.
     */
    public int getReputation(String townId) {
        return townReputation.getOrDefault(townId, 0);
    }
    
    /**
     * Add reputation with a town.
     */
    public void addReputation(String townId, int amount) {
        int current = townReputation.getOrDefault(townId, 0);
        townReputation.put(townId, Math.max(0, current + amount));
        syncToClient();
    }
    
    /**
     * Get reputation level name based on value.
     */
    public static String getReputationLevel(int rep) {
        if (rep >= 200) return "Exalted";
        if (rep >= 150) return "Revered";
        if (rep >= 100) return "Honored";
        if (rep >= 50) return "Friendly";
        if (rep >= 20) return "Neutral";
        return "Stranger";
    }
    
    /**
     * Get reputation color based on value.
     */
    public static int getReputationColor(int rep) {
        if (rep >= 200) return 0xFFD700; // Gold
        if (rep >= 150) return 0xDD88FF; // Purple
        if (rep >= 100) return 0x55FF55; // Green
        if (rep >= 50) return 0x88BBFF; // Blue
        if (rep >= 20) return 0xCCCCCC; // Gray
        return 0x888888; // Dark gray
    }

    public void setSelectedTownId(String id) {
        this.selectedTownId = id;
        syncToClient();
    }

    public void setMinDistance(int min) {
        this.minDistance = Math.max(1, Math.min(min, 10));
        syncToClient();
    }

    public void setMaxDistance(int max) {
        this.maxDistance = Math.max(1, Math.min(max, 10));
        syncToClient();
    }

    public void setLedgerSlot(ItemStack stack) {
        this.ledgerSlot = stack;
        setChanged();
    }

    public TownData getSelectedTown() {
        return TownRegistry.getTown(selectedTownId);
    }

    public int getXpForNextLevel() {
        return DebugConfig.getBaseXpToLevel() * traderLevel;
    }

    // ==================== Linked Post Sync ====================

    /**
     * Save all shared (global) trader state to a CompoundTag.
     * This is pushed to TradingData so all posts stay in sync.
     */
    public CompoundTag saveSharedState() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("TraderLevel", traderLevel);
        tag.putInt("TraderXp", traderXp);
        tag.putInt("PendingCoins", pendingCoins);
        tag.putInt("SaleTimer", saleCheckTimer);
        tag.putLong("LastRefreshDay", lastRefreshDay);

        ListTag shipmentList = new ListTag();
        for (Shipment s : activeShipments) shipmentList.add(s.save());
        tag.put("Shipments", shipmentList);

        ListTag listingsList = new ListTag();
        for (MarketListing ml : marketListings) listingsList.add(ml.save());
        tag.put("Listings", listingsList);

        ListTag historyList = new ListTag();
        for (CompoundTag h : shipmentHistory) historyList.add(h.copy());
        tag.put("History", historyList);

        tag.put("Demand", demandTracker.save());

        ListTag buyOrderList = new ListTag();
        for (BuyOrder bo : activeBuyOrders) buyOrderList.add(bo.save());
        tag.put("BuyOrders", buyOrderList);

        ListTag questList = new ListTag();
        for (Quest q : activeQuests) questList.add(q.save());
        tag.put("Quests", questList);
        tag.putLong("LastQuestRefresh", lastQuestRefreshDay);

        tag.put("Negotiator", negotiator.save());
        tag.put("TradingCart", tradingCart.save());
        tag.put("Bookkeeper", bookkeeper.save());

        ListTag diplomatList = new ListTag();
        for (DiplomatRequest dr : activeDiplomatRequests) diplomatList.add(dr.save());
        tag.put("Diplomats", diplomatList);

        tag.putLong("LifetimeEarnings", lifetimeEarnings);
        tag.putInt("TotalShipments", totalShipmentsSent);
        CompoundTag townEarningsTag = new CompoundTag();
        for (Map.Entry<String, Long> e : earningsByTown.entrySet()) {
            townEarningsTag.putLong(e.getKey(), e.getValue());
        }
        tag.put("EarningsByTown", townEarningsTag);
        CompoundTag itemEarningsTag = new CompoundTag();
        for (Map.Entry<String, Long> e : earningsByItem.entrySet()) {
            itemEarningsTag.putLong(e.getKey(), e.getValue());
        }
        tag.put("EarningsByItem", itemEarningsTag);

        CompoundTag repTag = new CompoundTag();
        for (Map.Entry<String, Integer> e : townReputation.entrySet()) {
            repTag.putInt(e.getKey(), e.getValue());
        }
        tag.put("TownReputation", repTag);

        return tag;
    }

    /**
     * Load shared (global) trader state from a CompoundTag.
     * Called when pulling state from TradingData into this block entity.
     */
    public void loadSharedState(CompoundTag tag) {
        traderLevel = tag.getInt("TraderLevel");
        if (traderLevel < 1) traderLevel = 1;
        traderXp = tag.getInt("TraderXp");
        pendingCoins = tag.getInt("PendingCoins");
        saleCheckTimer = tag.getInt("SaleTimer");
        lastRefreshDay = tag.getLong("LastRefreshDay");

        activeShipments.clear();
        ListTag shipmentList = tag.getList("Shipments", Tag.TAG_COMPOUND);
        for (int i = 0; i < shipmentList.size(); i++) {
            activeShipments.add(Shipment.load(shipmentList.getCompound(i)));
        }

        marketListings.clear();
        ListTag listingsList = tag.getList("Listings", Tag.TAG_COMPOUND);
        for (int i = 0; i < listingsList.size(); i++) {
            marketListings.add(MarketListing.load(listingsList.getCompound(i)));
        }

        shipmentHistory.clear();
        if (tag.contains("History")) {
            ListTag historyList = tag.getList("History", Tag.TAG_COMPOUND);
            for (int i = 0; i < historyList.size(); i++) {
                shipmentHistory.add(historyList.getCompound(i));
            }
        }

        if (tag.contains("Demand")) {
            demandTracker.load(tag.getCompound("Demand"));
        }

        activeBuyOrders.clear();
        if (tag.contains("BuyOrders")) {
            ListTag buyOrderList = tag.getList("BuyOrders", Tag.TAG_COMPOUND);
            for (int i = 0; i < buyOrderList.size(); i++) {
                activeBuyOrders.add(BuyOrder.load(buyOrderList.getCompound(i)));
            }
        }

        activeQuests.clear();
        if (tag.contains("Quests")) {
            ListTag questList = tag.getList("Quests", Tag.TAG_COMPOUND);
            for (int i = 0; i < questList.size(); i++) {
                activeQuests.add(Quest.load(questList.getCompound(i)));
            }
        }
        lastQuestRefreshDay = tag.getLong("LastQuestRefresh");

        if (tag.contains("Negotiator")) {
            negotiator = Worker.load(tag.getCompound("Negotiator"));
        }
        if (tag.contains("TradingCart")) {
            tradingCart = Worker.load(tag.getCompound("TradingCart"));
        }
        if (tag.contains("Bookkeeper")) {
            bookkeeper = Worker.load(tag.getCompound("Bookkeeper"));
        }

        activeDiplomatRequests.clear();
        if (tag.contains("Diplomats")) {
            ListTag diplomatList = tag.getList("Diplomats", Tag.TAG_COMPOUND);
            for (int i = 0; i < diplomatList.size(); i++) {
                activeDiplomatRequests.add(DiplomatRequest.load(diplomatList.getCompound(i)));
            }
        }

        lifetimeEarnings = tag.getLong("LifetimeEarnings");
        totalShipmentsSent = tag.getInt("TotalShipments");
        earningsByTown.clear();
        if (tag.contains("EarningsByTown")) {
            CompoundTag townEarnings = tag.getCompound("EarningsByTown");
            for (String key : townEarnings.getAllKeys()) {
                earningsByTown.put(key, townEarnings.getLong(key));
            }
        }
        earningsByItem.clear();
        if (tag.contains("EarningsByItem")) {
            CompoundTag itemEarnings = tag.getCompound("EarningsByItem");
            for (String key : itemEarnings.getAllKeys()) {
                earningsByItem.put(key, itemEarnings.getLong(key));
            }
        }

        townReputation.clear();
        if (tag.contains("TownReputation")) {
            CompoundTag repTag = tag.getCompound("TownReputation");
            for (String key : repTag.getAllKeys()) {
                townReputation.put(key, repTag.getInt(key));
            }
        }
    }

    /**
     * Called when this block entity's chunk is loaded.
     * Registers with TradingData and pulls shared state to stay in sync
     * with other Trading Posts. If TradingData is empty (existing world
     * upgrading to linked posts), pushes our local data as the initial state.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && level instanceof ServerLevel serverLevel) {
            TradingData data = TradingData.get(serverLevel);
            data.register(worldPosition);
            if (data.isEmpty()) {
                // First post to load — push our state as the canonical shared state (migration)
                data.setSharedState(saveSharedState());
            } else {
                // Pull shared state from TradingData to sync with other posts
                loadSharedState(data.getSharedState());
            }
        }
    }

    /**
     * Called when this block entity is removed (block broken or chunk unloaded).
     * Pushes final state and unregisters from TradingData.
     */
    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide() && level instanceof ServerLevel serverLevel) {
            TradingData data = TradingData.get(serverLevel);
            // Push our current state so it persists even after this post is gone
            data.setSharedState(saveSharedState());
            data.unregister(worldPosition);
        }
        super.setRemoved();
    }

    /**
     * Push shared state to TradingData and sync all other loaded Trading Posts.
     * Called after any shared state mutation to keep all posts consistent.
     */
    private void propagateToOtherPosts() {
        if (level == null || level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        TradingData data = TradingData.get(serverLevel);
        CompoundTag sharedTag = saveSharedState();
        data.setSharedState(sharedTag);

        isSyncing = true;
        try {
            for (BlockPos pos : data.getRegisteredPositions()) {
                if (pos.equals(worldPosition)) continue;
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof TradingPostBlockEntity other) {
                    other.loadSharedState(sharedTag);
                    other.setChanged();
                    level.sendBlockUpdated(pos, other.getBlockState(), other.getBlockState(), 3);
                }
            }
        } finally {
            isSyncing = false;
        }
    }

    // ==================== Trading Logic ====================

    /**
     * Send items from a nearby Trading Bin to the selected town.
     * Returns true if items were successfully dispatched.
     */
    public boolean sendItemsToMarket(Level level, BlockPos postPos) {
        TownData town = getSelectedTown();
        if (town == null || town.getMinTraderLevel() > traderLevel) return false;

        // Find a nearby Trading Bin
        TradingBinBlockEntity bin = findNearbyBin(level, postPos);
        if (bin == null || bin.isEmpty()) return false;

        // Collect items from the bin
        List<ItemStack> itemsToShip = new ArrayList<>();
        List<Shipment.ShipmentItem> shipmentItems = new ArrayList<>();

        for (int i = 0; i < TradingBinBlockEntity.BIN_SIZE; i++) {
            ItemStack stack = bin.getItem(i);
            if (!stack.isEmpty() && stack.getItem() != net.minecraft.world.item.Items.AIR) {
                int price = bin.getEffectivePriceForSlot(i);
                if (price <= 0) price = PriceCalculator.getBaseValue(stack);

                shipmentItems.add(new Shipment.ShipmentItem(
                        net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()),
                        stack.getCount(),
                        price,
                        stack.getHoverName().getString(),
                        stack.getTag() // preserve NBT (e.g. potion type) so returned items are identical
                ));
                itemsToShip.add(stack.copy());
            }
        }

        if (shipmentItems.isEmpty()) return false;

        // Calculate travel time (apply trading cart bonus)
        long gameTime = level.getGameTime();
        int baseTravelTicks = town.getTravelTimeTicks(DebugConfig.getTicksPerDistance());
        int travelTicks = (int) (baseTravelTicks * getTravelTimeMultiplier());
        // Track trading cart lifetime time saved (in ticks)
        if (tradingCart.isHired()) {
            int ticksSaved = baseTravelTicks - travelTicks;
            if (ticksSaved > 0) tradingCart.addLifetimeBonusValue(ticksSaved);
        }
        int pickupDelay = DebugConfig.SKIP_PICKUP_DELAY ? 0 : DebugConfig.getPickupDelay();
        long arrivalTime = gameTime + pickupDelay + travelTicks;

        // Create shipment
        Shipment shipment = new Shipment(
                UUID.randomUUID(), town.getId(), shipmentItems,
                gameTime, arrivalTime
        );
        activeShipments.add(shipment);

        // Record supply for demand tracking
        for (Shipment.ShipmentItem si : shipmentItems) {
            demandTracker.recordSupply(town.getId(), si.getItemId().toString(), si.getCount());
        }

        // Clear bin after dispatch (shipment notices are now handled by mailbox notes)
        bin.clearAndLeaveNote(ItemStack.EMPTY);

        syncToClient();
        return true;
    }

    /**
     * Request to return items from a shipment at the market.
     */
    public boolean requestReturn(UUID shipmentId) {
        if (level == null) return false;
        
        for (Shipment shipment : activeShipments) {
            if (shipment.getId().equals(shipmentId) && shipment.getStatus() == Shipment.Status.AT_MARKET) {
                TownData town = TownRegistry.getTown(shipment.getTownId());
                if (town != null) {
                    long gameTime = level.getGameTime();
                    int returnTicks = town.getTravelTimeTicks(DebugConfig.getTicksPerDistance());
                    shipment.setReturnArrivalTime(gameTime + returnTicks);
                    shipment.setStatus(Shipment.Status.RETURNING);
                    syncToClient();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Collect pending coins and receive them as items.
     */
    public List<ItemStack> collectCoins() {
        List<ItemStack> coins = new ArrayList<>();
        if (pendingCoins <= 0) return coins;

        int remaining = pendingCoins;

        // Convert to gold coins
        int gp = remaining / CoinType.GOLD.getValue();
        remaining %= CoinType.GOLD.getValue();
        while (gp > 0) {
            int stack = Math.min(gp, 64);
            coins.add(new ItemStack(ModItems.GOLD_COIN.get(), stack));
            gp -= stack;
        }

        // Convert to silver coins
        int sp = remaining / CoinType.SILVER.getValue();
        remaining %= CoinType.SILVER.getValue();
        while (sp > 0) {
            int stack = Math.min(sp, 64);
            coins.add(new ItemStack(ModItems.SILVER_COIN.get(), stack));
            sp -= stack;
        }

        // Remaining as copper coins
        while (remaining > 0) {
            int stack = Math.min(remaining, 64);
            coins.add(new ItemStack(ModItems.COPPER_COIN.get(), stack));
            remaining -= stack;
        }

        pendingCoins = 0;
        syncToClient();
        return coins;
    }

    /**
     * Add trader XP and handle level ups.
     */
    public void addTraderXp(int amount) {
        int oldLevel = traderLevel;
        traderXp += amount;
        while (traderXp >= getXpForNextLevel() && traderLevel < DebugConfig.getMaxTraderLevel()) {
            traderXp -= getXpForNextLevel();
            traderLevel++;
        }
        // Play level up sound and notify if leveled up
        if (traderLevel > oldLevel && level != null) {
            SoundHelper.playTraderLevelUp(level, worldPosition);
            ToastHelper.notifyLevelUp(level, worldPosition, traderLevel);
            notifyNearbyPlayers(level, worldPosition,
                    Component.literal("\u2605 Trader Level Up! Now level " + traderLevel + "!")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        syncToClient();
    }

    /**
     * Award Minecraft experience points to a player.
     */
    private void awardMinecraftXp(Player player, int amount) {
        if (player != null && amount > 0) {
            player.giveExperiencePoints(amount);
        }
    }

    /**
     * Buy an item from the market board using coins from the player.
     */
    public boolean buyFromMarket(Player player, MarketListing listing) {
        int totalCost = listing.getTotalPrice();
        if (!hasEnoughCoins(player, totalCost)) return false;

        // Deduct coins
        deductCoins(player, totalCost);

        // Give item to player
        net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(listing.getItemId());
        if (item != null) {
            ItemStack stack = new ItemStack(item, listing.getCount());
            if (!player.getInventory().add(stack)) {
                // Drop on ground if inventory is full
                player.drop(stack, false);
            }
        }

        marketListings.remove(listing);
        syncToClient();
        // Always issue purchase receipts to mailbox
        if (level != null && !level.isClientSide()) {
            TownData purchaseTown = TownRegistry.getTown(listing.getTownId());
            String purchaseTownName = purchaseTown != null ? purchaseTown.getDisplayName() : listing.getTownId();
            deliverNoteToNearbyMailboxes(level, worldPosition,
                    NoteTemplates.createNote(MailNote.NoteType.PURCHASE_MADE,
                            purchaseTownName, listing.getItemDisplayName(), listing.getCount(),
                            formatCoins(totalCost), "", player.getName().getString(),
                            level.getGameTime()));
        }
        return true;
    }

    // ==================== Coin Utilities ====================

    public static boolean hasEnoughCoins(Player player, int copperAmount) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof CoinItem coin) {
                total += coin.getValue() * stack.getCount();
            }
        }
        // Include coin bag contents
        total += getCoinBagValue(player);
        return total >= copperAmount;
    }

    /**
     * Get the total CP value stored in a player's coin bag (if they have one).
     */
    public static int getCoinBagValue(Player player) {
        ItemStack bagStack = findCoinBag(player);
        if (bagStack.isEmpty()) return 0;
        CompoundTag tag = bagStack.getTag();
        if (tag == null || !tag.contains("CoinBag")) return 0;
        CompoundTag bagTag = tag.getCompound("CoinBag");
        return bagTag.getInt("Gold") * 100 + bagTag.getInt("Silver") * 10 + bagTag.getInt("Copper");
    }

    /**
     * Find the first coin bag in the player's inventory.
     */
    public static ItemStack findCoinBag(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof com.offtomarket.mod.item.CoinBagItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void deductCoins(Player player, int copperAmount) {
        int remaining = copperAmount;

        // Deduct from inventory coins first (copper, then silver, then gold)
        for (CoinType type : new CoinType[]{CoinType.COPPER, CoinType.SILVER, CoinType.GOLD}) {
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof CoinItem coin && coin.getCoinType() == type) {
                    int coinValue = coin.getValue();
                    int coinsNeeded = remaining / coinValue;
                    int toRemove = Math.min(coinsNeeded, stack.getCount());

                    if (toRemove > 0) {
                        stack.shrink(toRemove);
                        remaining -= toRemove * coinValue;
                    }

                    // If we still need coins and this stack can cover remaining with change
                    if (remaining > 0 && !stack.isEmpty() && coinValue > remaining) {
                        stack.shrink(1);
                        int change = coinValue - remaining;
                        remaining = 0;
                        giveChange(player, change);
                    }
                }
            }
        }

        // If still remaining, deduct from coin bag
        if (remaining > 0) {
            deductFromCoinBag(player, remaining);
        }
    }

    /**
     * Deduct a copper amount from the player's coin bag.
     * Converts denominations as needed.
     */
    private static void deductFromCoinBag(Player player, int copperAmount) {
        ItemStack bagStack = findCoinBag(player);
        if (bagStack.isEmpty()) return;
        CompoundTag tag = bagStack.getOrCreateTag();
        CompoundTag bagTag = tag.contains("CoinBag") ? tag.getCompound("CoinBag") : new CompoundTag();

        int gold = bagTag.getInt("Gold");
        int silver = bagTag.getInt("Silver");
        int copper = bagTag.getInt("Copper");

        // Convert everything to copper for simplicity, deduct, repack
        int totalCopper = gold * 100 + silver * 10 + copper;
        totalCopper -= copperAmount;
        if (totalCopper < 0) totalCopper = 0;

        int newGold = totalCopper / 100;
        int newSilver = (totalCopper % 100) / 10;
        int newCopper = totalCopper % 10;

        CompoundTag newBagTag = new CompoundTag();
        if (newGold > 0) newBagTag.putInt("Gold", newGold);
        if (newSilver > 0) newBagTag.putInt("Silver", newSilver);
        if (newCopper > 0) newBagTag.putInt("Copper", newCopper);
        tag.put("CoinBag", newBagTag);
    }

    private static void giveChange(Player player, int copperAmount) {
        if (copperAmount <= 0) return;

        int gp = copperAmount / CoinType.GOLD.getValue();
        copperAmount %= CoinType.GOLD.getValue();
        int sp = copperAmount / CoinType.SILVER.getValue();
        copperAmount %= CoinType.SILVER.getValue();

        if (gp > 0) player.getInventory().add(new ItemStack(ModItems.GOLD_COIN.get(), gp));
        if (sp > 0) player.getInventory().add(new ItemStack(ModItems.SILVER_COIN.get(), sp));
        if (copperAmount > 0) player.getInventory().add(new ItemStack(ModItems.COPPER_COIN.get(), copperAmount));
    }

    // ==================== Buy Order Methods ====================

    /**
     * Add a buy order (created when player purchases from Market Board).
     */
    public void addBuyOrder(BuyOrder order) {
        activeBuyOrders.add(order);
        syncToClient();
    }

    /**
     * Collect an arrived buy order, giving the items to the player.
     */
    public void collectBuyOrder(UUID orderId, Player player) {
        BuyOrder toRemove = null;
        for (BuyOrder order : activeBuyOrders) {
            if (order.getId().equals(orderId) && order.getStatus() == BuyOrder.Status.ARRIVED) {
                ItemStack stack = order.createStack();
                if (!stack.isEmpty()) {
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                }
                toRemove = order;
                break;
            }
        }
        if (toRemove != null) {
            activeBuyOrders.remove(toRemove);
            syncToClient();
        }
    }

    /**
     * Collect coins from a completed (sold) shipment, giving coins to the player.
     */
    public void collectShipmentCoins(UUID shipmentId, Player player) {
        Shipment toRemove = null;
        for (Shipment shipment : activeShipments) {
            if (shipment.getId().equals(shipmentId) && shipment.getStatus() == Shipment.Status.COMPLETED) {
                int earnings = shipment.getTotalEarnings();
                if (earnings > 0) {
                    List<ItemStack> coins = convertToCoins(earnings);
                    for (ItemStack stack : coins) {
                        if (!player.getInventory().add(stack)) {
                            player.drop(stack, false);
                        }
                    }
                    // Award Minecraft XP based on earnings (1 XP per 10 copper)
                    int xpToAward = Math.max(1, earnings / 10);
                    awardMinecraftXp(player, xpToAward);
                }
                toRemove = shipment;
                break;
            }
        }
        if (toRemove != null) {
            activeShipments.remove(toRemove);
            syncToClient();
        }
    }

    /**
     * Adjust the price of a specific item in an AT_MARKET shipment.
     * Allows the player to change listing prices to improve sale chances.
     */
    public void adjustShipmentPrice(UUID shipmentId, int itemIndex, int newPrice) {
        for (Shipment shipment : activeShipments) {
            if (shipment.getId().equals(shipmentId) && shipment.getStatus() == Shipment.Status.AT_MARKET) {
                List<Shipment.ShipmentItem> items = shipment.getItems();
                if (itemIndex >= 0 && itemIndex < items.size()) {
                    Shipment.ShipmentItem item = items.get(itemIndex);
                    if (!item.isSold()) { // Can only adjust unsold items
                        item.setPricePerItem(newPrice);
                        syncToClient();
                    }
                }
                break;
            }
        }
    }

    /**
     * Cancel a shipment, setting it to return. Only works for IN_TRANSIT or AT_MARKET.
     */
    public void cancelShipment(UUID shipmentId) {
        for (Shipment shipment : activeShipments) {
            if (shipment.getId().equals(shipmentId)) {
                Shipment.Status status = shipment.getStatus();
                if (status == Shipment.Status.IN_TRANSIT || status == Shipment.Status.AT_MARKET) {
                    // Calculate return time = same as original travel time
                    long travelTime = shipment.getArrivalTime() - shipment.getDepartureTime();
                    long currentTime = level != null ? level.getGameTime() : 0;
                    shipment.setReturnArrivalTime(currentTime + travelTime);
                    shipment.setStatus(Shipment.Status.RETURNING);
                    syncToClient();
                }
                break;
            }
        }
    }

    /**
     * Collect returned items from a cancelled/auto-returned shipment.
     * Also gives coins for any items that sold before the return.
     */
    public void collectReturnedItems(UUID shipmentId, Player player) {
        Shipment toRemove = null;
        for (Shipment shipment : activeShipments) {
            if (shipment.getId().equals(shipmentId) && shipment.getStatus() == Shipment.Status.RETURNED) {
                // Give back unsold items
                for (Shipment.ShipmentItem si : shipment.getItems()) {
                    if (!si.isSold()) {
                        ItemStack returnStack = si.createStack();
                        if (!player.getInventory().add(returnStack)) {
                            player.drop(returnStack, false);
                        }
                    }
                }
                // Give coins for any items that sold before the return (partial sales)
                int earnings = shipment.getTotalEarnings();
                if (earnings > 0) {
                    List<ItemStack> coins = convertToCoins(earnings);
                    for (ItemStack stack : coins) {
                        if (!player.getInventory().add(stack)) {
                            player.drop(stack, false);
                        }
                    }
                    int xpToAward = Math.max(1, earnings / 10);
                    awardMinecraftXp(player, xpToAward);
                }
                toRemove = shipment;
                break;
            }
        }
        if (toRemove != null) {
            activeShipments.remove(toRemove);
            syncToClient();
        }
    }

    /**
     * Convert a copper amount to coin stacks.
     */
    private List<ItemStack> convertToCoins(int copperAmount) {
        List<ItemStack> coins = new ArrayList<>();
        int remaining = copperAmount;

        // Convert to gold coins
        int gp = remaining / CoinType.GOLD.getValue();
        remaining %= CoinType.GOLD.getValue();
        while (gp > 0) {
            int stack = Math.min(gp, 64);
            coins.add(new ItemStack(ModItems.GOLD_COIN.get(), stack));
            gp -= stack;
        }

        // Convert to silver coins
        int sp = remaining / CoinType.SILVER.getValue();
        remaining %= CoinType.SILVER.getValue();
        while (sp > 0) {
            int stack = Math.min(sp, 64);
            coins.add(new ItemStack(ModItems.SILVER_COIN.get(), stack));
            sp -= stack;
        }

        // Remaining as copper coins
        while (remaining > 0) {
            int stack = Math.min(remaining, 64);
            coins.add(new ItemStack(ModItems.COPPER_COIN.get(), stack));
            remaining -= stack;
        }

        return coins;
    }

    // ==================== Quest Methods ====================

    /**
     * Accept a quest (change status from AVAILABLE to ACCEPTED).
     */
    public boolean acceptQuest(UUID questId) {
        for (Quest quest : activeQuests) {
            if (quest.getId().equals(questId) && quest.getStatus() == Quest.Status.AVAILABLE) {
                quest.setStatus(Quest.Status.ACCEPTED);
                syncToClient();
                return true;
            }
        }
        return false;
    }

    /**
     * Complete a quest by delivering items from the player's inventory.
     * Returns the number of items actually consumed.
     */
    public int deliverQuestItems(UUID questId, Player player) {
        for (Quest quest : activeQuests) {
            if (quest.getId().equals(questId) && quest.getStatus() == Quest.Status.ACCEPTED) {
                int needed = quest.getRemainingCount();
                if (needed <= 0) continue;

                // Find matching items in player inventory
                int delivered = 0;
                for (int i = 0; i < player.getInventory().getContainerSize() && delivered < needed; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty()) {
                        net.minecraft.resources.ResourceLocation itemId =
                                net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                        if (itemId != null && itemId.equals(quest.getRequiredItemId())) {
                            int take = Math.min(stack.getCount(), needed - delivered);
                            stack.shrink(take);
                            delivered += take;
                        }
                    }
                }

                if (delivered > 0) {
                    boolean completed = quest.deliver(delivered);
                    if (completed) {
                        // Set travel time for rewards — items were delivered, rewards travel back
                        TownData town = TownRegistry.getTown(quest.getTownId());
                        int baseTravelTicks = town != null
                                ? town.getTravelTimeTicks(DebugConfig.getTicksPerDistance())
                                : 1200; // fallback ~1 minute
                        int travelTicks = (int) (baseTravelTicks * getTravelTimeMultiplier());
                        long currentTime = level != null ? level.getGameTime() : 0;
                        quest.setRewardArrivalTime(currentTime + travelTicks);
                        
                        notifyNearbyPlayers(level, worldPosition,
                                Component.literal("\u2714 All items delivered! Rewards arriving in " +
                                        formatTravelTime(travelTicks) + "...")
                                        .withStyle(ChatFormatting.YELLOW));
                    }
                    syncToClient();
                }
                return delivered;
            }
        }
        return 0;
    }

    /**
     * Refresh quests — generate new quests from available towns.
     */
    private void refreshQuests(long gameTime) {
        // Remove completed and expired quests
        activeQuests.removeIf(q -> q.getStatus() == Quest.Status.COMPLETED
                || q.getStatus() == Quest.Status.EXPIRED);

        // Mark expired quests (but never expire DELIVERING quests — their rewards are in transit)
        for (Quest quest : activeQuests) {
            if (quest.isExpired(gameTime)
                    && quest.getStatus() != Quest.Status.EXPIRED
                    && quest.getStatus() != Quest.Status.DELIVERING
                    && quest.getStatus() != Quest.Status.COMPLETED) {
                quest.setStatus(Quest.Status.EXPIRED);
            }
        }
        activeQuests.removeIf(q -> q.getStatus() == Quest.Status.EXPIRED);

        // Generate new quests from available towns (up to 5 total)
        int maxTotal = 5;
        if (activeQuests.size() >= maxTotal) return;

        List<TownData> towns = new java.util.ArrayList<>(TownRegistry.getAvailableTowns(traderLevel));
        java.util.Random rand = new java.util.Random();
        java.util.Collections.shuffle(towns, rand);

        for (TownData town : towns) {
            if (activeQuests.size() >= maxTotal) break;
            if (town.getDistance() < minDistance || town.getDistance() > maxDistance) continue;

            // Check if we already have a quest from this town
            boolean hasTownQuest = activeQuests.stream()
                    .anyMatch(q -> q.getTownId().equals(town.getId()));
            if (hasTownQuest) continue;

            List<Quest> newQuests = Quest.generateQuests(town, gameTime, rand, 1);
            activeQuests.addAll(newQuests);
        }
    }

    // ==================== Worker Methods ====================

    /**
     * Hire a worker by paying the hire cost.
     */
    public boolean hireWorker(Worker.WorkerType type, Player player) {
        Worker worker = getWorker(type);
        if (worker.isHired()) return false;

        int cost = worker.getHireCost();
        if (!hasEnoughCoins(player, cost)) return false;

        deductCoins(player, cost);
        worker.setHired(true);
        syncToClient();
        return true;
    }

    /**
     * Fire (dismiss) a worker, giving back a partial refund (50% of hire cost).
     */
    public boolean fireWorker(Worker.WorkerType type, Player player) {
        Worker worker = getWorker(type);
        if (!worker.isHired()) return false;

        int refund = worker.getFireRefund();
        if (refund > 0) {
            giveChange(player, refund);
        }
        worker.setHired(false);
        syncToClient();
        return true;
    }

    /**
     * Get the negotiation bonus (multiplier) if negotiator is hired.
     */
    public double getNegotiationBonus() {
        if (negotiator.isHired()) {
            return 1.0 + negotiator.getNegotiationBonus();
        }
        return 1.0;
    }

    /**
     * Get the travel time reduction multiplier if trading cart is hired.
     */
    public double getTravelTimeMultiplier() {
        if (tradingCart.isHired()) {
            return 1.0 - tradingCart.getSpeedBonus();
        }
        return 1.0;
    }

    /**
     * Get the bookkeeper cost reduction factor (0.0 to ~0.95).
     * Returns 0.0 if bookkeeper is not hired.
     */
    public double getBookkeeperCostReduction() {
        if (bookkeeper.isHired()) {
            return bookkeeper.getCostReductionBonus();
        }
        return 0.0;
    }

    /**
     * Deduct worker per-trip costs from shipment earnings. Returns adjusted earnings.
     * Bookkeeper reduces the per-trip cost of all workers (including itself if perk unlocked).
     */
    public int applyWorkerCosts(int earnings) {
        double costReduction = getBookkeeperCostReduction();
        int totalCosts = 0;
        int costsBeforeReduction = 0;

        if (negotiator.isHired()) {
            int rawCost = negotiator.getPerTripCost();
            int adjustedCost = (int) Math.max(1, rawCost * (1.0 - costReduction));
            totalCosts += adjustedCost;
            costsBeforeReduction += rawCost;
            negotiator.completedTrip();
        }
        if (tradingCart.isHired()) {
            int rawCost = tradingCart.getPerTripCost();
            int adjustedCost = (int) Math.max(1, rawCost * (1.0 - costReduction));
            totalCosts += adjustedCost;
            costsBeforeReduction += rawCost;
            tradingCart.completedTrip();
        }
        if (bookkeeper.isHired()) {
            int rawCost = bookkeeper.getPerTripCost();
            // Bookkeeper's own cost is only reduced if "Penny Pincher" perk (level 3+)
            double selfReduction = bookkeeper.hasPerk(3) ? costReduction : 0.0;
            int adjustedCost = (int) Math.max(1, rawCost * (1.0 - selfReduction));
            totalCosts += adjustedCost;
            costsBeforeReduction += rawCost;
            bookkeeper.completedTrip();
            // Track lifetime savings from bookkeeper cost reduction
            int saved = costsBeforeReduction - totalCosts;
            if (saved > 0) bookkeeper.addLifetimeBonusValue(saved);
        }

        // Track negotiator lifetime bonus (extra earnings from negotiation)
        // This is tracked at the call site where negotiation bonus is applied

        return Math.max(1, earnings - totalCosts);
    }

    // ==================== Diplomat Methods ====================

    /**
     * Random for negotiation variance.
     */
    private final java.util.Random negotiationRandom = new java.util.Random();

    /**
     * Send a diplomat to request items from a town.
     * This starts the TRAVELING_TO phase. No coins are deducted yet.
     * The player will need to accept the proposed price after DISCUSSING phase.
     */
    public boolean sendDiplomat(Player player, String townId,
                                 net.minecraft.resources.ResourceLocation itemId, int count) {
        TownData town = TownRegistry.getTown(townId);
        if (town == null || town.getMinTraderLevel() > traderLevel) return false;
        if (!DiplomatRequest.canTownSupply(town, itemId)) return false;

        return sendDiplomatWithScore(player, townId, itemId, count);
    }

    /**
     * Create an item request from the Requests tab — auto-selects the best town.
     * Any item can be requested; the town's supply score affects price and fulfillment chance.
     */
    public boolean createRequest(Player player, net.minecraft.resources.ResourceLocation itemId, int count) {
        net.minecraft.world.item.Item item =
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) return false;
        if (count < 1 || count > 64) return false;

        // Find the best unlocked town to fulfill this request
        TownData bestTown = findBestTownForItem(itemId);
        if (bestTown == null) {
            // No available town — notify player
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "\u00A7cNo town available to fulfill this request."));
            }
            return false;
        }

        // Delegate to sendDiplomat with the auto-selected town
        return sendDiplomatWithScore(player, bestTown.getId(), itemId, count);
    }

    /**
     * Find the best unlocked town to supply the given item.
     * Returns null if no town is available.
     */
    private TownData findBestTownForItem(net.minecraft.resources.ResourceLocation itemId) {
        TownData bestTown = null;
        int bestScore = -1;

        for (TownData town : TownRegistry.getAllTowns()) {
            if (town.getMinTraderLevel() > traderLevel) continue; // locked
            int score = DiplomatRequest.getSupplyScore(town, itemId);
            // Tiebreak: prefer closer towns (within same score tier)
            if (score > bestScore || (score == bestScore && bestTown != null
                    && town.getDistance() < bestTown.getDistance())) {
                bestScore = score;
                bestTown = town;
            }
        }
        return bestTown;
    }

    /**
     * Internal: sends a diplomat request with supply score tracking.
     * Used by both sendDiplomat (legacy) and createRequest (new).
     */
    private boolean sendDiplomatWithScore(Player player, String townId,
                                           net.minecraft.resources.ResourceLocation itemId, int count) {
        TownData town = TownRegistry.getTown(townId);
        if (town == null || town.getMinTraderLevel() > traderLevel) return false;

        net.minecraft.world.item.Item item =
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) return false;

        int supplyScore = DiplomatRequest.getSupplyScore(town, itemId);

        // Calculate base price
        int basePrice = PriceCalculator.getBaseValue(new ItemStack(item)) * count;

        // Calculate timing milestones
        long gameTime = level.getGameTime();
        int baseTravelTicks = (int) (town.getTravelTimeTicks(DebugConfig.getTicksPerDistance())
                * getTravelTimeMultiplier());

        long travelToEnd = gameTime + baseTravelTicks;
        long discussingEnd = travelToEnd + 12000;
        long waitingEnd = discussingEnd + 300;
        long returnEnd = waitingEnd + baseTravelTicks;

        String displayName = new ItemStack(item).getHoverName().getString();
        DiplomatRequest request = new DiplomatRequest(
                UUID.randomUUID(), townId, itemId, displayName, count, gameTime
        );
        request.setTimings(travelToEnd, discussingEnd, waitingEnd, returnEnd);
        request.setSupplyScore(supplyScore);

        // Calculate proposed price using score-based premium
        double premium = DiplomatRequest.getScoreBasedPremium(supplyScore, town);
        double variance = 0.8 + negotiationRandom.nextDouble() * 0.4;
        double finalMult = 1.0 + (premium - 1.0) * variance;
        int proposedPrice = (int) (basePrice * finalMult);
        int premiumAmount = proposedPrice - basePrice;
        request.setPricing(proposedPrice, premiumAmount);

        activeDiplomatRequests.add(request);
        syncToClient();
        return true;
    }

    /**
     * Accept a diplomat's proposed price.
     * Deducts coins and advances to WAITING_FOR_GOODS.
     */
    public boolean acceptDiplomatProposal(UUID requestId, Player player) {
        for (DiplomatRequest req : activeDiplomatRequests) {
            if (req.getId().equals(requestId) && req.getStatus() == DiplomatRequest.Status.DISCUSSING) {
                int cost = req.getProposedPrice();
                if (!hasEnoughCoins(player, cost)) {
                    // Not enough coins - decline automatically
                    req.setStatus(DiplomatRequest.Status.DECLINED);
                    TownData costTown = TownRegistry.getTown(req.getTownId());
                    String costTownName = costTown != null ? costTown.getDisplayName() : req.getTownId();
                    if (level != null && !level.isClientSide()) {
                        deliverNoteToNearbyMailboxes(level, worldPosition,
                                NoteTemplates.createNote(MailNote.NoteType.DIPLOMAT_FAILURE,
                                        costTownName, req.getItemDisplayName(), req.getRequestedCount(),
                                        formatCoins(cost), "", player.getName().getString(),
                                        level.getGameTime()));
                    }
                    syncToClient();
                    return false;
                }
                
                deductCoins(player, cost);
                req.setStatus(DiplomatRequest.Status.WAITING_FOR_GOODS);
                syncToClient();
                return true;
            }
        }
        return false;
    }

    /**
     * Decline a diplomat's proposed price.
     * No coins are deducted, request is marked DECLINED and eventually removed.
     */
    public boolean declineDiplomatProposal(UUID requestId) {
        for (DiplomatRequest req : activeDiplomatRequests) {
            if (req.getId().equals(requestId) && req.getStatus() == DiplomatRequest.Status.DISCUSSING) {
                req.setStatus(DiplomatRequest.Status.DECLINED);
                TownData decTown = TownRegistry.getTown(req.getTownId());
                String decTownName = decTown != null ? decTown.getDisplayName() : req.getTownId();
                if (level != null && !level.isClientSide()) {
                    deliverNoteToNearbyMailboxes(level, worldPosition,
                            NoteTemplates.createNote(MailNote.NoteType.DIPLOMAT_FAILURE,
                                    decTownName, req.getItemDisplayName(), req.getRequestedCount(),
                                    formatCoins(req.getProposedPrice()), "", "",
                                    level.getGameTime()));
                }
                syncToClient();
                return true;
            }
        }
        return false;
    }

    /**
     * Collect an arrived diplomat request.
     */
    public void collectDiplomatRequest(UUID requestId, Player player) {
        DiplomatRequest toRemove = null;
        for (DiplomatRequest req : activeDiplomatRequests) {
            if (req.getId().equals(requestId) && req.getStatus() == DiplomatRequest.Status.ARRIVED) {
                ItemStack stack = req.createStack();
                if (!stack.isEmpty()) {
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                }
                toRemove = req;
                break;
            }
        }
        if (toRemove != null) {
            activeDiplomatRequests.remove(toRemove);
            syncToClient();
        }
    }

    // ==================== Helper Methods ====================

    @Nullable
    private TradingBinBlockEntity findNearbyBin(Level level, BlockPos center) {
        int radius = DebugConfig.getBinSearchRadius();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TradingBinBlockEntity bin) {
                return bin;
            }
        }
        return null;
    }

    public void dropContents(Level level, BlockPos pos) {
        if (!ledgerSlot.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), ledgerSlot);
            ledgerSlot = ItemStack.EMPTY;
        }
    }

    // ==================== Server Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   TradingPostBlockEntity be) {
        long gameTime = level.getGameTime();

        // Only one trading post processes the global tick logic per game tick.
        // All posts share state via TradingData; the first to tick each game tick wins.
        if (level instanceof ServerLevel serverLevel) {
            TradingData tradingData = TradingData.get(serverLevel);
            if (!tradingData.tryClaimTick(gameTime)) {
                return; // Another post already processed this tick
            }
        }

        boolean changed = false;

        // Process shipments

        // Check for sales periodically (outside the shipment loop)
        boolean checkSales = false;
        be.saleCheckTimer++;
        DebugConfig.WATCH_SALE_TIMER = be.saleCheckTimer;
        if (be.saleCheckTimer >= DebugConfig.getSaleCheckInterval()) {
            be.saleCheckTimer = 0;
            checkSales = true;
        }

        for (Shipment shipment : be.activeShipments) {
            switch (shipment.getStatus()) {
                case IN_TRANSIT:
                    if (gameTime >= shipment.getArrivalTime()) {
                        shipment.setStatus(Shipment.Status.AT_MARKET);
                        shipment.setMarketListedTime(gameTime);
                        TownData arrTown = TownRegistry.getTown(shipment.getTownId());
                        String arrName = arrTown != null ? arrTown.getDisplayName() : shipment.getTownId();
                        notifyNearbyPlayers(level, pos,
                                Component.literal("\u2709 Shipment arrived at " + arrName + "!")
                                        .withStyle(ChatFormatting.AQUA));
                        SoundHelper.playShipmentArrive(level, pos);
                        ToastHelper.notifyShipmentArrived(level, pos, arrName);
                        // Shipment notices removed in v0.3.0 (mailbox focuses on actionable receipts/updates).
                        changed = true;
                    }
                    break;

                case AT_MARKET:
                    if (checkSales) {
                        TownData town = TownRegistry.getTown(shipment.getTownId());
                        if (town != null) {
                            be.processMarketSales(shipment, town, gameTime);
                        }
                    }
                    break;

                case SOLD:
                    // Apply negotiator bonus and deduct worker costs
                    int rawEarnings = shipment.getTotalEarnings();
                    int negotiatedEarnings = (int) (rawEarnings * be.getNegotiationBonus());
                    // Track negotiator lifetime bonus
                    int negotiationBonus = negotiatedEarnings - rawEarnings;
                    if (negotiationBonus > 0 && be.negotiator.isHired()) {
                        be.negotiator.addLifetimeBonusValue(negotiationBonus);
                    }
                    int finalEarnings = be.applyWorkerCosts(negotiatedEarnings);
                    // Store final earnings in shipment for collection, instead of pendingCoins
                    shipment.setTotalEarnings(finalEarnings);
                    shipment.setStatus(Shipment.Status.COMPLETED);
                    // Archive AFTER adjusting earnings so history tracks final amounts
                    be.archiveShipment(shipment);
                    be.addTraderXp(DebugConfig.getXpPerSale());
                    // Award reputation for regular sales (count of sold item types)
                    int soldCount = (int) shipment.getItems().stream()
                            .filter(Shipment.ShipmentItem::isSold).count();
                    if (soldCount > 0) {
                        be.addReputation(shipment.getTownId(), soldCount);
                    }
                    TownData soldTown = TownRegistry.getTown(shipment.getTownId());
                    String soldName = soldTown != null ? soldTown.getDisplayName() : shipment.getTownId();
                    notifyNearbyPlayers(level, pos,
                            Component.literal("\u2714 Items sold at " + soldName + "! Click to collect " + formatCoins(finalEarnings) + ".")
                                    .withStyle(ChatFormatting.GREEN));
                    SoundHelper.playItemSold(level, pos);
                    SoundHelper.playCoinJingle(level, pos);
                    ToastHelper.notifyItemsSold(level, pos, finalEarnings);
                    // Don't remove - stays in COMPLETED status until player collects
                    changed = true;
                    break;

                case RETURNING:
                    if (gameTime >= shipment.getReturnArrivalTime()) {
                        shipment.setStatus(Shipment.Status.RETURNED);
                        TownData retTown = TownRegistry.getTown(shipment.getTownId());
                        String retName = retTown != null ? retTown.getDisplayName() : shipment.getTownId();
                        notifyNearbyPlayers(level, pos,
                                Component.literal("\u21A9 Items returned from " + retName + ". Click to collect.")
                                        .withStyle(ChatFormatting.YELLOW));
                        changed = true;
                    }
                    break;

                case COMPLETED:
                    // Stay in COMPLETED status until player clicks to collect coins
                    break;

                case RETURNED:
                    // Stay in RETURNED status until player clicks to collect items
                    break;
            }
        }

        // Tick demand decay
        if (be.demandTracker.tick()) {
            changed = true;
        }

        // Dawn-based market refresh (once per Minecraft day at sunrise).
        // Use getDayTime() (not getGameTime()) so that sleeping — which advances
        // the day/night cycle without a matching increase in raw game ticks — still
        // triggers the daily refresh correctly.
        long dayTime = level.getDayTime() % 24000;
        long dayNumber = level.getDayTime() / 24000;
        if (be.lastRefreshDay < 0 || be.marketListings.isEmpty()) {
            // First time or empty: do initial refresh
            be.lastRefreshDay = dayNumber;
            be.refreshMarketListings(gameTime);
            changed = true;
        } else if (dayTime >= 0 && dayTime < 200 && dayNumber > be.lastRefreshDay) {
            // Dawn of a new day: refresh market
            be.lastRefreshDay = dayNumber;
            be.refreshMarketListings(gameTime);
            notifyNearbyPlayers(level, pos,
                    Component.literal("\u2600 The market has refreshed with new goods at dawn!")
                            .withStyle(ChatFormatting.GOLD));
            changed = true;
        }

        // Process buy orders (IN_TRANSIT → ARRIVED)
        for (BuyOrder order : be.activeBuyOrders) {
            if (order.getStatus() == BuyOrder.Status.IN_TRANSIT
                    && gameTime >= order.getArrivalTime()) {
                order.setStatus(BuyOrder.Status.ARRIVED);
                TownData orderTown = TownRegistry.getTown(order.getTownId());
                String orderTownName = orderTown != null ? orderTown.getDisplayName() : order.getTownId();
                notifyNearbyPlayers(level, pos,
                        Component.literal("\u2709 Purchase from " + orderTownName + " has arrived! Collect at Trading Post.")
                                .withStyle(ChatFormatting.AQUA));
                changed = true;
            }
        }

        // Process diplomat requests through stages
        List<DiplomatRequest> failedRequests = new ArrayList<>();
        for (DiplomatRequest req : be.activeDiplomatRequests) {
            switch (req.getStatus()) {
                case TRAVELING_TO -> {
                    // Diplomat traveling to town
                    if (gameTime >= req.getTravelToEndTime()) {
                        TownData reqTown = TownRegistry.getTown(req.getTownId());
                        String townName = reqTown != null ? reqTown.getDisplayName() : req.getTownId();

                        // Requests now always reach proposal phase; difficult items cost more via premium.
                        req.setStatus(DiplomatRequest.Status.DISCUSSING);
                        notifyNearbyPlayers(level, pos,
                            Component.literal("\u2709 Diplomat arrived at " + townName + "! Accept or decline the proposal.")
                                .withStyle(ChatFormatting.YELLOW));
                        SoundHelper.playDiplomatProposal(level, pos);
                        ToastHelper.notifyDiplomatProposal(level, pos, townName);
                        changed = true;
                    }
                }
                case DISCUSSING -> {
                        // Waiting for player to accept/decline - auto-decline if time runs out
                    if (gameTime >= req.getDiscussingEndTime()) {
                        req.setStatus(DiplomatRequest.Status.DECLINED);
                        TownData reqTown = TownRegistry.getTown(req.getTownId());
                        String townName = reqTown != null ? reqTown.getDisplayName() : req.getTownId();
                        notifyNearbyPlayers(level, pos,
                            Component.literal("\u2718 Diplomat proposal from " + townName + " auto-declined.")
                                        .withStyle(ChatFormatting.RED));
                        deliverNoteToNearbyMailboxes(level, pos,
                                NoteTemplates.createNote(MailNote.NoteType.DIPLOMAT_FAILURE,
                                        townName, req.getItemDisplayName(), req.getRequestedCount(),
                                        formatCoins(req.getProposedPrice()), "", "", gameTime));
                        changed = true;
                    }
                }
                case WAITING_FOR_GOODS -> {
                    // Town preparing goods
                    if (gameTime >= req.getWaitingEndTime()) {
                        req.setStatus(DiplomatRequest.Status.TRAVELING_BACK);
                        changed = true;
                    }
                }
                case TRAVELING_BACK -> {
                    // Diplomat returning with goods
                    if (gameTime >= req.getReturnEndTime()) {
                        req.setStatus(DiplomatRequest.Status.ARRIVED);
                        TownData reqTown = TownRegistry.getTown(req.getTownId());
                        String reqTownName = reqTown != null ? reqTown.getDisplayName() : req.getTownId();
                        notifyNearbyPlayers(level, pos,
                                Component.literal("\u2709 Diplomat returned from " + reqTownName + " with " + req.getItemDisplayName() + "!")
                                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                        SoundHelper.playDiplomatReturn(level, pos);
                        ToastHelper.notifyDiplomatReturned(level, pos, reqTownName, true);
                        changed = true;
                    }
                }
                case FAILED, DECLINED -> {
                    // Mark for removal after a short delay (give player time to see the status)
                    if (gameTime >= req.getDiscussingEndTime() + 200) { // 10 seconds after failure
                        failedRequests.add(req);
                    }
                }
                default -> { /* ARRIVED - waiting for collection */ }
            }
        }
        be.activeDiplomatRequests.removeAll(failedRequests);

        // Quest reward arrival check — DELIVERING quests whose rewards have arrived
        for (Quest quest : be.activeQuests) {
            if (quest.getStatus() == Quest.Status.DELIVERING
                    && quest.getRewardArrivalTime() > 0
                    && gameTime >= quest.getRewardArrivalTime()) {
                quest.setStatus(Quest.Status.COMPLETED);
                // Pay out quest rewards
                be.pendingCoins += quest.getRewardCoins();
                be.addTraderXp(quest.getRewardXp());
                be.addReputation(quest.getTownId(), quest.getRewardReputation());
                TownData questTown = TownRegistry.getTown(quest.getTownId());
                String questTownName = questTown != null ? questTown.getDisplayName() : quest.getTownId();
                notifyNearbyPlayers(level, pos,
                        Component.literal("\u2714 Quest complete! +" + formatCoins(quest.getRewardCoins())
                                + " bonus, +" + quest.getRewardXp() + " XP, +"
                                + quest.getRewardReputation() + " rep")
                                .withStyle(ChatFormatting.GREEN));
                deliverNoteToNearbyMailboxes(level, pos,
                        NoteTemplates.createNote(MailNote.NoteType.QUEST_COMPLETED,
                                questTownName, quest.getItemDisplayName(), quest.getRequiredCount(),
                                formatCoins(quest.getRewardCoins()),
                                quest.getRewardXp() + " XP, " + quest.getRewardReputation() + " rep",
                                "", gameTime));
                changed = true;
            }
        }

        // Quest expiry check (every ~200 ticks)
        if (gameTime % 200 == 0) {
            for (Quest quest : be.activeQuests) {
                if (quest.isExpired(gameTime) && quest.getStatus() == Quest.Status.AVAILABLE) {
                    quest.setStatus(Quest.Status.EXPIRED);
                    changed = true;
                } else if (quest.isExpired(gameTime) && quest.getStatus() == Quest.Status.ACCEPTED) {
                    quest.setStatus(Quest.Status.EXPIRED);
                    TownData expTown = TownRegistry.getTown(quest.getTownId());
                    String expTownName = expTown != null ? expTown.getDisplayName() : quest.getTownId();
                    notifyNearbyPlayers(level, pos,
                            Component.literal("\u2718 Quest expired: " + quest.getItemDisplayName())
                                    .withStyle(ChatFormatting.RED));
                    deliverNoteToNearbyMailboxes(level, pos,
                            NoteTemplates.createNote(MailNote.NoteType.QUEST_EXPIRED,
                                    expTownName, quest.getItemDisplayName(), quest.getRequiredCount(),
                                    formatCoins(quest.getRewardCoins()), "", "", gameTime));
                    changed = true;
                }
            }
            be.activeQuests.removeIf(q -> q.getStatus() == Quest.Status.EXPIRED
                    || q.getStatus() == Quest.Status.COMPLETED);
        }

        // Dawn-based quest refresh (same as market refresh, once per day).
        // Also uses getDayTime() / 24000 so sleeping properly triggers a refresh.
        if (be.lastQuestRefreshDay < 0 || be.activeQuests.isEmpty()) {
            be.lastQuestRefreshDay = dayNumber;
            be.refreshQuests(gameTime);
            changed = true;
        } else if (dayTime >= 0 && dayTime < 200 && dayNumber > be.lastQuestRefreshDay) {
            be.lastQuestRefreshDay = dayNumber;
            be.refreshQuests(gameTime);
            changed = true;
        }

        if (changed) {
            be.syncToClient();
        }
    }

    private void processMarketSales(Shipment shipment, TownData town, long gameTime) {
        Random rand = new Random();
        boolean allSold = true;
        int earnings = 0;

        long timeAtMarket = shipment.getTimeAtMarket(gameTime);
        int maxMarketTime = DebugConfig.getMaxMarketTime();
        double timeRatio = Math.min(1.0, (double) timeAtMarket / maxMarketTime);
        boolean forceAutoSell = timeAtMarket >= maxMarketTime;

        // Escalation: sale chance increases as items approach max market time
        double escalation = 1.0 + (DebugConfig.getSaleChanceEscalation() - 1.0) * timeRatio;

        // Auto-return: when max market time expires, return unsold items instead of force-selling
        if (forceAutoSell) {
            boolean anyUnsold = false;
            for (Shipment.ShipmentItem item : shipment.getItems()) {
                if (!item.isSold()) {
                    anyUnsold = true;
                }
            }
            if (anyUnsold) {
                // Initiate return — unsold items travel back, sold items' earnings are preserved
                long travelTime = shipment.getArrivalTime() - shipment.getDepartureTime();
                long currentTime = level != null ? level.getGameTime() : 0;
                shipment.setReturnArrivalTime(currentTime + travelTime);
                shipment.setStatus(Shipment.Status.RETURNING);
                notifyNearbyPlayers(level, worldPosition, Component.literal(
                        "\u00A7e[Market] \u00A77Shipment to " + shipment.getTownId()
                        + " auto-returning after " + (maxMarketTime / 20 / 60) + " minutes"));
                syncToClient();
            } else {
                // All items were already sold naturally
                shipment.setSoldTime(gameTime);
                shipment.setStatus(Shipment.Status.SOLD);
                syncToClient();
            }
            return;
        }

        for (Shipment.ShipmentItem item : shipment.getItems()) {
            if (item.isSold()) continue;

            ItemStack stack = item.createStack();
            int fairValue = PriceCalculator.calculateFinalValue(
                    stack, PriceCalculator.getBaseValue(stack), town);
            int maxPrice = PriceCalculator.calculateFinalMaxPrice(
                    stack, PriceCalculator.getMaxPrice(stack), town);

            double saleSpeed = PriceCalculator.getSaleSpeedMultiplier(
                    item.getPricePerItem(), fairValue, maxPrice,
                    DebugConfig.getOverpriceThreshold());

            if (saleSpeed <= 0.0) {
                // Price exceeds max ceiling - this item will not sell
                allSold = false;
                continue;
            }

            double demandMult = demandTracker.getDemandMultiplier(
                    shipment.getTownId(), item.getItemId().toString());

            double saleChance = DebugConfig.getBaseSaleChance() * saleSpeed * escalation * demandMult;
            saleChance = Math.min(saleChance, 0.95); // cap at 95%

            if (rand.nextDouble() < saleChance) {
                item.setSold(true);
                earnings += item.getTotalPrice();
                // Record the sale in global supply/demand tracking
                SupplyDemandManager.recordSale(town, item.getItemId().toString(), item.getCount());
            } else {
                allSold = false;
            }
        }

        if (earnings > 0) {
            shipment.setTotalEarnings(shipment.getTotalEarnings() + earnings);
        }

        if (allSold) {
            shipment.setSoldTime(gameTime);
            shipment.setStatus(Shipment.Status.SOLD);
            syncToClient();
        }
    }

    private void refreshMarketListings(long gameTime) {
        marketListings.clear();
        Random rand = new Random();
        List<TownData> availableTowns = TownRegistry.getAvailableTowns(traderLevel);

        for (TownData town : availableTowns) {
            if (town.getDistance() >= minDistance && town.getDistance() <= maxDistance) {
                marketListings.addAll(MarketListing.generateListings(town, gameTime, rand));
            }
        }
    }

    // ==================== Notifications & Helpers ====================

    private static void notifyNearbyPlayers(Level level, BlockPos pos, Component message) {
        AABB area = new AABB(pos).inflate(64.0);
        for (Player player : level.getEntitiesOfClass(Player.class, area)) {
            player.displayClientMessage(message, false);
        }
    }

    /**
     * Deliver a mail note to all Mailbox block entities within a 32-block radius.
     */
    private static void deliverNoteToNearbyMailboxes(Level level, BlockPos pos, MailNote note) {
        int radius = 32;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos check = pos.offset(x, y, z);
                    if (level.getBlockState(check).getBlock() instanceof MailboxBlock) {
                        BlockEntity be = level.getBlockEntity(check);
                        if (be instanceof MailboxBlockEntity mailbox) {
                            mailbox.addNote(note);
                        }
                    }
                }
            }
        }
    }

    private static String formatCoins(int copperPieces) {
        int gp = copperPieces / 100;
        int sp = (copperPieces % 100) / 10;
        int cp = copperPieces % 10;
        StringBuilder sb = new StringBuilder();
        if (gp > 0) sb.append(gp).append("g");
        if (sp > 0) { if (sb.length() > 0) sb.append(" "); sb.append(sp).append("s"); }
        if (cp > 0 || sb.length() == 0) { if (sb.length() > 0) sb.append(" "); sb.append(cp).append("c"); }
        return sb.toString();
    }

    /**
     * Format ticks as a human-readable travel time string (e.g., "2m 30s").
     */
    private static String formatTravelTime(int ticks) {
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes > 0 && seconds > 0) return minutes + "m " + seconds + "s";
        if (minutes > 0) return minutes + "m";
        return seconds + "s";
    }

    // ==================== Client Sync ====================

    /**
     * Marks dirty and sends block entity data to all tracking clients.
     * Also propagates shared state to all other linked Trading Posts.
     */
    public void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            if (!isSyncing) {
                propagateToOtherPosts();
            }
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

    // ==================== Menu Provider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.offtomarket.trading_post");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new TradingPostMenu(containerId, inv, this);
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("TraderLevel", traderLevel);
        tag.putInt("TraderXp", traderXp);
        tag.putString("SelectedTown", selectedTownId);
        tag.putInt("MinDist", minDistance);
        tag.putInt("MaxDist", maxDistance);
        tag.putInt("PendingCoins", pendingCoins);
        tag.putInt("SaleTimer", saleCheckTimer);
        tag.putLong("LastRefreshDay", lastRefreshDay);

        if (!ledgerSlot.isEmpty()) {
            tag.put("Ledger", ledgerSlot.save(new CompoundTag()));
        }

        ListTag shipmentList = new ListTag();
        for (Shipment s : activeShipments) {
            shipmentList.add(s.save());
        }
        tag.put("Shipments", shipmentList);

        ListTag listingsList = new ListTag();
        for (MarketListing ml : marketListings) {
            listingsList.add(ml.save());
        }
        tag.put("Listings", listingsList);

        ListTag historyList = new ListTag();
        for (CompoundTag h : shipmentHistory) {
            historyList.add(h.copy());
        }
        tag.put("History", historyList);

        tag.put("Demand", demandTracker.save());

        ListTag buyOrderList = new ListTag();
        for (BuyOrder bo : activeBuyOrders) {
            buyOrderList.add(bo.save());
        }
        tag.put("BuyOrders", buyOrderList);

        // Quests
        ListTag questList = new ListTag();
        for (Quest q : activeQuests) {
            questList.add(q.save());
        }
        tag.put("Quests", questList);
        tag.putLong("LastQuestRefresh", lastQuestRefreshDay);

        // Workers
        tag.put("Negotiator", negotiator.save());
        tag.put("TradingCart", tradingCart.save());
        tag.put("Bookkeeper", bookkeeper.save());

        // Diplomat requests
        ListTag diplomatList = new ListTag();
        for (DiplomatRequest dr : activeDiplomatRequests) {
            diplomatList.add(dr.save());
        }
        tag.put("Diplomats", diplomatList);

        // Economy dashboard stats
        tag.putLong("LifetimeEarnings", lifetimeEarnings);
        tag.putInt("TotalShipments", totalShipmentsSent);
        CompoundTag townEarnings = new CompoundTag();
        for (Map.Entry<String, Long> entry : earningsByTown.entrySet()) {
            townEarnings.putLong(entry.getKey(), entry.getValue());
        }
        tag.put("EarningsByTown", townEarnings);
        CompoundTag itemEarnings = new CompoundTag();
        for (Map.Entry<String, Long> entry : earningsByItem.entrySet()) {
            itemEarnings.putLong(entry.getKey(), entry.getValue());
        }
        tag.put("EarningsByItem", itemEarnings);
        
        // Town reputation
        CompoundTag repTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : townReputation.entrySet()) {
            repTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("TownReputation", repTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        traderLevel = tag.getInt("TraderLevel");
        if (traderLevel < 1) traderLevel = 1;
        traderXp = tag.getInt("TraderXp");
        selectedTownId = tag.getString("SelectedTown");
        if (selectedTownId.isEmpty()) selectedTownId = "greenhollow";
        minDistance = tag.getInt("MinDist");
        maxDistance = tag.getInt("MaxDist");
        if (maxDistance < 1) maxDistance = 10;
        pendingCoins = tag.getInt("PendingCoins");
        saleCheckTimer = tag.getInt("SaleTimer");
        lastRefreshDay = tag.getLong("LastRefreshDay");

        if (tag.contains("Ledger")) {
            ledgerSlot = ItemStack.of(tag.getCompound("Ledger"));
        }

        activeShipments.clear();
        ListTag shipmentList = tag.getList("Shipments", Tag.TAG_COMPOUND);
        for (int i = 0; i < shipmentList.size(); i++) {
            activeShipments.add(Shipment.load(shipmentList.getCompound(i)));
        }

        marketListings.clear();
        ListTag listingsList = tag.getList("Listings", Tag.TAG_COMPOUND);
        for (int i = 0; i < listingsList.size(); i++) {
            marketListings.add(MarketListing.load(listingsList.getCompound(i)));
        }

        shipmentHistory.clear();
        if (tag.contains("History")) {
            ListTag historyList = tag.getList("History", Tag.TAG_COMPOUND);
            for (int i = 0; i < historyList.size(); i++) {
                shipmentHistory.add(historyList.getCompound(i));
            }
        }

        if (tag.contains("Demand")) {
            demandTracker.load(tag.getCompound("Demand"));
        }

        activeBuyOrders.clear();
        if (tag.contains("BuyOrders")) {
            ListTag buyOrderList = tag.getList("BuyOrders", Tag.TAG_COMPOUND);
            for (int i = 0; i < buyOrderList.size(); i++) {
                activeBuyOrders.add(BuyOrder.load(buyOrderList.getCompound(i)));
            }
        }

        // Quests
        activeQuests.clear();
        if (tag.contains("Quests")) {
            ListTag questList = tag.getList("Quests", Tag.TAG_COMPOUND);
            for (int i = 0; i < questList.size(); i++) {
                activeQuests.add(Quest.load(questList.getCompound(i)));
            }
        }
        lastQuestRefreshDay = tag.getLong("LastQuestRefresh");

        // Workers
        if (tag.contains("Negotiator")) {
            negotiator = Worker.load(tag.getCompound("Negotiator"));
        }
        if (tag.contains("TradingCart")) {
            tradingCart = Worker.load(tag.getCompound("TradingCart"));
        }
        if (tag.contains("Bookkeeper")) {
            bookkeeper = Worker.load(tag.getCompound("Bookkeeper"));
        }

        // Diplomat requests
        activeDiplomatRequests.clear();
        if (tag.contains("Diplomats")) {
            ListTag diplomatList = tag.getList("Diplomats", Tag.TAG_COMPOUND);
            for (int i = 0; i < diplomatList.size(); i++) {
                activeDiplomatRequests.add(DiplomatRequest.load(diplomatList.getCompound(i)));
            }
        }

        // Economy dashboard stats
        lifetimeEarnings = tag.getLong("LifetimeEarnings");
        totalShipmentsSent = tag.getInt("TotalShipments");
        earningsByTown.clear();
        if (tag.contains("EarningsByTown")) {
            CompoundTag townEarnings = tag.getCompound("EarningsByTown");
            for (String key : townEarnings.getAllKeys()) {
                earningsByTown.put(key, townEarnings.getLong(key));
            }
        }
        earningsByItem.clear();
        if (tag.contains("EarningsByItem")) {
            CompoundTag itemEarnings = tag.getCompound("EarningsByItem");
            for (String key : itemEarnings.getAllKeys()) {
                earningsByItem.put(key, itemEarnings.getLong(key));
            }
        }
        
        // Town reputation
        townReputation.clear();
        if (tag.contains("TownReputation")) {
            CompoundTag repTag = tag.getCompound("TownReputation");
            for (String key : repTag.getAllKeys()) {
                townReputation.put(key, repTag.getInt(key));
            }
        }
    }

    // ==================== Shipment History ====================

    /**
     * Archive a completed shipment into the history ledger.
     * Stores the most recent 50 shipments (newest first).
     */
    private void archiveShipment(Shipment shipment) {
        CompoundTag record = new CompoundTag();
        TownData town = TownRegistry.getTown(shipment.getTownId());
        String townName = town != null ? town.getDisplayName() : shipment.getTownId();
        record.putString("Town", townName);
        record.putInt("Earnings", shipment.getTotalEarnings());
        record.putLong("DepartTime", shipment.getDepartureTime());

        // Economy dashboard tracking
        int earnings = shipment.getTotalEarnings();
        lifetimeEarnings += earnings;
        totalShipmentsSent++;
        earningsByTown.merge(shipment.getTownId(), (long) earnings, Long::sum);

        boolean anySold = false, anyUnsold = false;
        ListTag items = new ListTag();
        for (Shipment.ShipmentItem si : shipment.getItems()) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.putString("Name", si.getDisplayName());
            itemTag.putInt("Count", si.getCount());
            itemTag.putInt("Price", si.getPricePerItem());
            itemTag.putBoolean("Sold", si.isSold());
            items.add(itemTag);
            if (si.isSold()) {
                anySold = true;
                // Track per-item earnings
                int itemEarnings = si.getPricePerItem() * si.getCount();
                earningsByItem.merge(si.getDisplayName(), (long) itemEarnings, Long::sum);
            } else {
                anyUnsold = true;
            }
        }
        record.put("Items", items);

        String outcome;
        if (anySold && !anyUnsold) outcome = "SOLD";
        else if (!anySold) outcome = "RETURNED";
        else outcome = "PARTIAL";
        record.putString("Outcome", outcome);

        shipmentHistory.add(0, record); // newest first
        while (shipmentHistory.size() > MAX_HISTORY) {
            shipmentHistory.remove(shipmentHistory.size() - 1);
        }
    }
}
