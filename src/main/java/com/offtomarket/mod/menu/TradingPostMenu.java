package com.offtomarket.mod.menu;

import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import com.offtomarket.mod.item.CoinItem;
import com.offtomarket.mod.registry.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container/Menu for the Trading Post.
 * The Trading Post UI is mostly button-driven (selecting towns, sending shipments, etc.)
 * It has a ledger slot and displays trader level / shipment info.
 */
public class TradingPostMenu extends AbstractContainerMenu {
    private final TradingPostBlockEntity blockEntity;
    private final ContainerData data;
    private final SimpleContainer coinExchange = new SimpleContainer(3);
    private boolean coinSlotsActive = false;

    /** Index of the first coin exchange slot in this.slots */
    public static final int COIN_SLOT_START = 36; // after 27 inv + 9 hotbar

    // Data indices
    public static final int DATA_TRADER_LEVEL = 0;
    public static final int DATA_TRADER_XP = 1;
    public static final int DATA_PENDING_COINS = 2;
    public static final int DATA_MIN_DISTANCE = 3;
    public static final int DATA_MAX_DISTANCE = 4;
    public static final int DATA_SIZE = 5;

    // Client-side constructor
    public TradingPostMenu(int containerId, Inventory inv) {
        this(containerId, inv, null);
    }

    public TradingPostMenu(int containerId, Inventory inv, TradingPostBlockEntity be) {
        super(ModMenuTypes.TRADING_POST.get(), containerId);
        this.blockEntity = be;

        if (be != null) {
            this.data = new ContainerData() {
                @Override
                public int get(int index) {
                    return switch (index) {
                        case DATA_TRADER_LEVEL -> be.getTraderLevel();
                        case DATA_TRADER_XP -> be.getTraderXp();
                        case DATA_PENDING_COINS -> be.getPendingCoins();
                        case DATA_MIN_DISTANCE -> be.getMinDistance();
                        case DATA_MAX_DISTANCE -> be.getMaxDistance();
                        default -> 0;
                    };
                }

                @Override
                public void set(int index, int value) {
                    switch (index) {
                        case DATA_MIN_DISTANCE -> be.setMinDistance(value);
                        case DATA_MAX_DISTANCE -> be.setMaxDistance(value);
                    }
                }

                @Override
                public int getCount() { return DATA_SIZE; }
            };
        } else {
            this.data = new SimpleContainerData(DATA_SIZE);
        }

        addDataSlots(this.data);

        // Player inventory slots (3 rows of 9 + hotbar)
        addPlayerInventory(inv, 48, 148);
        addPlayerHotbar(inv, 48, 206);

        // Coin exchange slots (3 slots for drag-and-drop coin conversion)
        // isActive() returns false when not on Coins tab, hiding them automatically
        for (int i = 0; i < 3; i++) {
            this.addSlot(new CoinSlot(coinExchange, i, 72 + i * 36, 72));
        }
    }

    public SimpleContainer getCoinExchange() { return coinExchange; }

    public void setCoinSlotsActive(boolean active) { this.coinSlotsActive = active; }

    public TradingPostBlockEntity getBlockEntity() { return blockEntity; }

    public int getTraderLevel() { return data.get(DATA_TRADER_LEVEL); }
    public int getTraderXp() { return data.get(DATA_TRADER_XP); }
    public int getPendingCoins() { return data.get(DATA_PENDING_COINS); }
    public int getMinDistance() { return data.get(DATA_MIN_DISTANCE); }
    public int getMaxDistance() { return data.get(DATA_MAX_DISTANCE); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Return any coins left in exchange slots to the player
        if (!player.level.isClientSide()) {
            for (int i = 0; i < coinExchange.getContainerSize(); i++) {
                ItemStack stack = coinExchange.getItem(i);
                if (!stack.isEmpty()) {
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                    coinExchange.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return true;
        return player.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    private void addPlayerInventory(Inventory inv, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inv, int x, int y) {
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, x + col * 18, y));
        }
    }

    /**
     * Convert all coins in the 3 exchange slots.
     * @param convertUp true = copper→silver→gold; false = gold→silver→copper
     * @param player the player (for overflow items)
     */
    public void convertCoinSlots(boolean convertUp, Player player) {
        for (int i = 0; i < coinExchange.getContainerSize(); i++) {
            ItemStack stack = coinExchange.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof CoinItem coin)) continue;

            com.offtomarket.mod.item.CoinType type = coin.getCoinType();
            int count = stack.getCount();

            if (convertUp) {
                // Copper \u2192 Silver (10:1), Silver \u2192 Gold (10:1)
                switch (type) {
                    case COPPER -> {
                        int silver = count / 10;
                        int remainder = count % 10;
                        if (silver > 0) {
                            coinExchange.setItem(i, remainder > 0
                                    ? new ItemStack(com.offtomarket.mod.registry.ModItems.COPPER_COIN.get(), remainder)
                                    : ItemStack.EMPTY);
                            giveCoins(player, new ItemStack(com.offtomarket.mod.registry.ModItems.SILVER_COIN.get(), silver), i);
                        }
                    }
                    case SILVER -> {
                        int gold = count / 10;
                        int remainder = count % 10;
                        if (gold > 0) {
                            coinExchange.setItem(i, remainder > 0
                                    ? new ItemStack(com.offtomarket.mod.registry.ModItems.SILVER_COIN.get(), remainder)
                                    : ItemStack.EMPTY);
                            giveCoins(player, new ItemStack(com.offtomarket.mod.registry.ModItems.GOLD_COIN.get(), gold), i);
                        }
                    }
                    case GOLD -> {} // Already highest tier
                }
            } else {
                // Gold \u2192 Silver (1:10), Silver \u2192 Copper (1:10)
                switch (type) {
                    case GOLD -> {
                        int silver = count * 10;
                        coinExchange.setItem(i, ItemStack.EMPTY);
                        giveCoins(player, new ItemStack(com.offtomarket.mod.registry.ModItems.SILVER_COIN.get(), silver), i);
                    }
                    case SILVER -> {
                        int copper = count * 10;
                        coinExchange.setItem(i, ItemStack.EMPTY);
                        giveCoins(player, new ItemStack(com.offtomarket.mod.registry.ModItems.COPPER_COIN.get(), copper), i);
                    }
                    case COPPER -> {} // Already lowest tier
                }
            }
        }
    }

    /**
     * Place coins back into the exchange slot if possible, overflow to player inventory.
     */
    private void giveCoins(Player player, ItemStack coins, int preferredSlot) {
        ItemStack existing = coinExchange.getItem(preferredSlot);
        if (existing.isEmpty()) {
            // Put as much as fits in the slot (max stack size)
            int max = coins.getMaxStackSize();
            if (coins.getCount() <= max) {
                coinExchange.setItem(preferredSlot, coins);
                return;
            } else {
                coinExchange.setItem(preferredSlot, coins.split(max));
            }
        }
        // Overflow to player inventory / drop
        if (!coins.isEmpty()) {
            if (!player.getInventory().add(coins)) {
                player.drop(coins, false);
            }
        }
    }

    /** A slot that only accepts CoinItem instances and can be hidden. */
    private class CoinSlot extends Slot {
        public CoinSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof CoinItem;
        }

        @Override
        public boolean isActive() {
            return coinSlotsActive;
        }
    }
}
