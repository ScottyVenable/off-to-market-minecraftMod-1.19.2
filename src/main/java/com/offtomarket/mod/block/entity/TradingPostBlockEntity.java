package com.offtomarket.mod.block.entity;

import com.offtomarket.mod.data.*;
import com.offtomarket.mod.debug.DebugConfig;
import com.offtomarket.mod.item.CoinItem;
import com.offtomarket.mod.item.CoinType;
import com.offtomarket.mod.item.ShipmentNoteItem;
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

    // Market listings refresh timer
    private int marketRefreshTimer = 0;
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

    // Diplomat requests (player requests specific items from towns)
    private final List<DiplomatRequest> activeDiplomatRequests = new ArrayList<>();

    // Dawn-based market refresh tracking
    private long lastRefreshDay = -1;

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
        setChanged();
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
            if (!stack.isEmpty()) {
                int basePrice = PriceCalculator.getBaseValue(stack);
                int price = bin.getSetPrice(i);
                if (price <= 0) price = basePrice; // default to base price

                shipmentItems.add(new Shipment.ShipmentItem(
                        net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()),
                        stack.getCount(),
                        price,
                        stack.getHoverName().getString()
                ));
                itemsToShip.add(stack.copy());
            }
        }

        if (shipmentItems.isEmpty()) return false;

        // Calculate travel time (apply trading cart bonus)
        long gameTime = level.getGameTime();
        int baseTravelTicks = town.getTravelTimeTicks(DebugConfig.getTicksPerDistance());
        int travelTicks = (int) (baseTravelTicks * getTravelTimeMultiplier());
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

        // Create note and clear bin
        int maxMarketTicks = DebugConfig.getMaxMarketTime();
        ItemStack note = ShipmentNoteItem.createNote(
                town.getDisplayName(), itemsToShip, gameTime, travelTicks + pickupDelay, maxMarketTicks);
        bin.clearAndLeaveNote(note);

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
        setChanged();
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
     * Collect returned items from a cancelled shipment.
     */
    public void collectReturnedItems(UUID shipmentId, Player player) {
        Shipment toRemove = null;
        for (Shipment shipment : activeShipments) {
            if (shipment.getId().equals(shipmentId) && shipment.getStatus() == Shipment.Status.RETURNED) {
                for (Shipment.ShipmentItem si : shipment.getItems()) {
                    if (!si.isSold()) {
                        ItemStack returnStack = si.createStack();
                        if (!player.getInventory().add(returnStack)) {
                            player.drop(returnStack, false);
                        }
                    }
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
                        // Award rewards
                        pendingCoins += quest.getRewardCoins();
                        addTraderXp(quest.getRewardXp());
                        // Award reputation with the town
                        addReputation(quest.getTownId(), quest.getRewardReputation());
                        // Award Minecraft XP for quest completion
                        int xpToAward = Math.max(5, quest.getRewardXp() * 2);
                        awardMinecraftXp(player, xpToAward);
                        // Build notification message
                        String repText = " +" + quest.getRewardReputation() + " Rep";
                        notifyNearbyPlayers(level, worldPosition,
                                Component.literal("\u2714 Quest completed! Earned " +
                                        formatCoins(quest.getRewardCoins()) + " bonus + " +
                                        quest.getRewardXp() + " XP" + repText + "!")
                                        .withStyle(ChatFormatting.GREEN));
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

        // Mark expired quests
        for (Quest quest : activeQuests) {
            if (quest.isExpired(gameTime) && quest.getStatus() != Quest.Status.EXPIRED) {
                quest.setStatus(Quest.Status.EXPIRED);
            }
        }
        activeQuests.removeIf(q -> q.getStatus() == Quest.Status.EXPIRED);

        // Generate new quests from available towns (up to 5 total)
        int maxTotal = 5;
        if (activeQuests.size() >= maxTotal) return;

        List<TownData> towns = TownRegistry.getAvailableTowns(traderLevel);
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
        Worker worker = type == Worker.WorkerType.NEGOTIATOR ? negotiator : tradingCart;
        if (worker.isHired()) return false;

        int cost = worker.getHireCost();
        if (!hasEnoughCoins(player, cost)) return false;

        deductCoins(player, cost);
        worker.setHired(true);
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
     * Deduct worker per-trip costs from shipment earnings. Returns adjusted earnings.
     */
    public int applyWorkerCosts(int earnings) {
        int costs = 0;
        if (negotiator.isHired()) {
            costs += negotiator.getPerTripCost();
            negotiator.completedTrip();
        }
        if (tradingCart.isHired()) {
            costs += tradingCart.getPerTripCost();
            tradingCart.completedTrip();
        }
        return Math.max(1, earnings - costs);
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

        net.minecraft.world.item.Item item =
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) return false;

        // Calculate base price for validation (we'll finalize price during DISCUSSING)
        int basePrice = PriceCalculator.getBaseValue(new ItemStack(item)) * count;
        
        // Calculate timing milestones
        long gameTime = level.getGameTime();
        int baseTravelTicks = (int) (town.getTravelTimeTicks(DebugConfig.getTicksPerDistance()) 
                * getTravelTimeMultiplier());
        
        // Stage durations:
        // - TRAVELING_TO: full one-way trip
        // - DISCUSSING: 30 seconds (600 ticks) to accept/decline
        // - WAITING_FOR_GOODS: 15 seconds (300 ticks)
        // - TRAVELING_BACK: full one-way trip
        long travelToEnd = gameTime + baseTravelTicks;
        long discussingEnd = travelToEnd + 600;  // 30 seconds to decide
        long waitingEnd = discussingEnd + 300;   // 15 seconds to prepare
        long returnEnd = waitingEnd + baseTravelTicks;

        String displayName = new ItemStack(item).getHoverName().getString();
        DiplomatRequest request = new DiplomatRequest(
                UUID.randomUUID(), townId, itemId, displayName, count, gameTime
        );
        request.setTimings(travelToEnd, discussingEnd, waitingEnd, returnEnd);
        
        // Calculate proposed price with random variance
        int proposedPrice = DiplomatRequest.calculateNegotiatedPrice(basePrice, town, negotiationRandom);
        int premium = proposedPrice - basePrice;
        request.setPricing(proposedPrice, premium);
        
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
                    req.setStatus(DiplomatRequest.Status.FAILED);
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
     * No coins are deducted, request is marked FAILED and eventually removed.
     */
    public boolean declineDiplomatProposal(UUID requestId) {
        for (DiplomatRequest req : activeDiplomatRequests) {
            if (req.getId().equals(requestId) && req.getStatus() == DiplomatRequest.Status.DISCUSSING) {
                req.setStatus(DiplomatRequest.Status.FAILED);
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
        boolean changed = false;

        // Process shipments
        List<Shipment> toRemove = new ArrayList<>();

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
                    be.archiveShipment(shipment);
                    // Apply negotiator bonus and deduct worker costs
                    int rawEarnings = shipment.getTotalEarnings();
                    int negotiatedEarnings = (int) (rawEarnings * be.getNegotiationBonus());
                    int finalEarnings = be.applyWorkerCosts(negotiatedEarnings);
                    // Store final earnings in shipment for collection, instead of pendingCoins
                    shipment.setTotalEarnings(finalEarnings);
                    shipment.setStatus(Shipment.Status.COMPLETED);
                    be.addTraderXp(DebugConfig.getXpPerSale());
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

        be.activeShipments.removeAll(toRemove);

        // Tick demand decay
        if (be.demandTracker.tick()) {
            changed = true;
        }

        // Dawn-based market refresh (once per Minecraft day at sunrise)
        long dayTime = level.getDayTime() % 24000;
        long dayNumber = level.getGameTime() / 24000;
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
                        req.setStatus(DiplomatRequest.Status.DISCUSSING);
                        TownData reqTown = TownRegistry.getTown(req.getTownId());
                        String townName = reqTown != null ? reqTown.getDisplayName() : req.getTownId();
                        notifyNearbyPlayers(level, pos,
                                Component.literal("\u2709 Diplomat arrived at " + townName + "! Accept or decline the proposal.")
                                        .withStyle(ChatFormatting.YELLOW));
                        SoundHelper.playDiplomatProposal(level, pos);
                        ToastHelper.notifyDiplomatProposal(level, pos, townName);
                        changed = true;
                    }
                }
                case DISCUSSING -> {
                    // Waiting for player to accept/decline - auto-fail if time runs out
                    if (gameTime >= req.getDiscussingEndTime()) {
                        req.setStatus(DiplomatRequest.Status.FAILED);
                        TownData reqTown = TownRegistry.getTown(req.getTownId());
                        String townName = reqTown != null ? reqTown.getDisplayName() : req.getTownId();
                        notifyNearbyPlayers(level, pos,
                                Component.literal("\u2718 Diplomat proposal from " + townName + " expired!")
                                        .withStyle(ChatFormatting.RED));
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
                case FAILED -> {
                    // Mark for removal after a short delay (give player time to see the status)
                    if (gameTime >= req.getDiscussingEndTime() + 200) { // 10 seconds after failure
                        failedRequests.add(req);
                    }
                }
                default -> { /* ARRIVED - waiting for collection */ }
            }
        }
        be.activeDiplomatRequests.removeAll(failedRequests);

        // Quest expiry check (every ~200 ticks)
        if (gameTime % 200 == 0) {
            for (Quest quest : be.activeQuests) {
                if (quest.isExpired(gameTime) && quest.getStatus() == Quest.Status.AVAILABLE) {
                    quest.setStatus(Quest.Status.EXPIRED);
                    changed = true;
                } else if (quest.isExpired(gameTime) && quest.getStatus() == Quest.Status.ACCEPTED) {
                    quest.setStatus(Quest.Status.EXPIRED);
                    notifyNearbyPlayers(level, pos,
                            Component.literal("\u2718 Quest expired: " + quest.getItemDisplayName())
                                    .withStyle(ChatFormatting.RED));
                    changed = true;
                }
            }
            be.activeQuests.removeIf(q -> q.getStatus() == Quest.Status.EXPIRED
                    || q.getStatus() == Quest.Status.COMPLETED);
        }

        // Dawn-based quest refresh (same as market refresh, once per day)
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
                // Price exceeds max ceiling - this item WILL NOT sell, even at auto-sell time
                allSold = false;
                continue;
            }

            double demandMult = demandTracker.getDemandMultiplier(
                    shipment.getTownId(), item.getItemId().toString());

            if (forceAutoSell) {
                // Auto-sell at 75% of set price when max market time is reached
                item.setSold(true);
                int discountedPrice = Math.max(1, (int) (item.getTotalPrice() * 0.75 * demandMult));
                earnings += discountedPrice;
                // Record the auto-sale in global supply/demand tracking
                SupplyDemandManager.recordSale(town, item.getItemId().toString(), item.getCount());
                continue;
            }

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

    // ==================== Client Sync ====================

    /**
     * Marks dirty and sends block entity data to all tracking clients.
     */
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
        tag.putInt("MarketTimer", marketRefreshTimer);
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
        marketRefreshTimer = tag.getInt("MarketTimer");
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
