package com.offtomarket.mod.block.entity;

import com.offtomarket.mod.data.MarketListing;
import com.offtomarket.mod.data.TownData;
import com.offtomarket.mod.data.TownRegistry;
import com.offtomarket.mod.debug.DebugConfig;
import com.offtomarket.mod.menu.MarketBoardMenu;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The Market Board shows available items from various towns' markets.
 * It's read-only and refreshes periodically.
 */
public class MarketBoardBlockEntity extends BlockEntity implements MenuProvider {

    /** Refresh cooldown in ticks (5 minutes = 6000 ticks at 20 tps) */
    public static final int REFRESH_COOLDOWN_TICKS = 6000;

    private final List<MarketListing> listings = new ArrayList<>();
    private int refreshTimer = 0;

    public MarketBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MARKET_BOARD.get(), pos, state);
    }

    public List<MarketListing> getListings() {
        return listings;
    }

    /** Returns remaining cooldown ticks before refresh is allowed. */
    public int getRefreshCooldown() {
        return refreshTimer;
    }

    /** Returns true if the market board can be refreshed right now. */
    public boolean canRefresh() {
        return refreshTimer <= 0 || DebugConfig.UNLIMITED_REFRESHES;
    }

    public void refreshListings() {
        if (!canRefresh()) return;

        listings.clear();
        Random rand = new Random();
        long gameTime = level != null ? level.getGameTime() : 0;

        for (TownData town : TownRegistry.getAllTowns()) {
            listings.addAll(MarketListing.generateListings(town, gameTime, rand));
        }
        refreshTimer = REFRESH_COOLDOWN_TICKS;
        syncToClient();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MarketBoardBlockEntity be) {
        if (be.refreshTimer > 0) {
            be.refreshTimer--;
            if (be.refreshTimer == 0) {
                be.refreshListings(); // auto-refresh when cooldown completes; also resets timer
            } else if (be.refreshTimer % 20 == 0) {
                // Sync timer to client once per second so the countdown displays live
                be.syncToClient();
            }
        } else if (be.listings.isEmpty()) {
            // First-time placement or world load with no listings: generate immediately
            be.refreshListings();
        }
    }

    // ==================== Client Sync ====================

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
        return Component.translatable("block.offtomarket.market_board");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new MarketBoardMenu(containerId, inv, this);
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("RefreshTimer", refreshTimer);

        ListTag listingsList = new ListTag();
        for (MarketListing ml : listings) {
            listingsList.add(ml.save());
        }
        tag.put("Listings", listingsList);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        refreshTimer = tag.getInt("RefreshTimer");

        listings.clear();
        ListTag listingsList = tag.getList("Listings", Tag.TAG_COMPOUND);
        for (int i = 0; i < listingsList.size(); i++) {
            listings.add(MarketListing.load(listingsList.getCompound(i)));
        }
    }
}
