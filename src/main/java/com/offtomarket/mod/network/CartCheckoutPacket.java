package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.MarketBoardBlockEntity;
import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import com.offtomarket.mod.block.entity.FinanceTableBlockEntity;
import com.offtomarket.mod.data.BuyOrder;
import com.offtomarket.mod.data.MarketListing;
import com.offtomarket.mod.data.TownData;
import com.offtomarket.mod.data.TownRegistry;
import com.offtomarket.mod.debug.DebugConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sent from client when the player clicks "Checkout" on their cart
 * in the Market Board screen. Contains a list of listing indices and
 * quantities to purchase. Creates buy orders at the nearest Trading Post.
 */
public class CartCheckoutPacket {

    private final BlockPos marketBoardPos;
    private final List<CartEntry> entries;

    public static class CartEntry {
        public final int listingIndex;
        public final int quantity;

        public CartEntry(int listingIndex, int quantity) {
            this.listingIndex = listingIndex;
            this.quantity = quantity;
        }
    }

    public CartCheckoutPacket(BlockPos marketBoardPos, List<CartEntry> entries) {
        this.marketBoardPos = marketBoardPos;
        this.entries = entries;
    }

    public static void encode(CartCheckoutPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.marketBoardPos);
        buf.writeInt(msg.entries.size());
        for (CartEntry entry : msg.entries) {
            buf.writeInt(entry.listingIndex);
            buf.writeInt(entry.quantity);
        }
    }

    public static CartCheckoutPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int count = buf.readInt();
        List<CartEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new CartEntry(buf.readInt(), buf.readInt()));
        }
        return new CartCheckoutPacket(pos, entries);
    }

    public static void handle(CartCheckoutPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.marketBoardPos);
            if (!(be instanceof MarketBoardBlockEntity marketBoard)) return;

            if (player.distanceToSqr(msg.marketBoardPos.getX() + 0.5,
                    msg.marketBoardPos.getY() + 0.5,
                    msg.marketBoardPos.getZ() + 0.5) > 64.0) return;

            // Find nearby Trading Post (required for buy orders)
            TradingPostBlockEntity tradingPost = findNearbyTradingPost(
                    player.level, msg.marketBoardPos);
            if (tradingPost == null) {
                player.displayClientMessage(
                        Component.literal("No Trading Post found nearby! Place one within 8 blocks.")
                                .withStyle(ChatFormatting.RED), true);
                return;
            }

            // Find nearby Finance Table (optional — supplements player coins if connected)
            FinanceTableBlockEntity financeTable = TradingPostBlockEntity.findNearbyFinanceTable(
                    player.level, tradingPost.getBlockPos());

            List<MarketListing> listings = marketBoard.getListings();

            // Validate all entries and calculate total cost
            int totalCost = 0;
            List<ValidatedEntry> validated = new ArrayList<>();

            for (CartEntry entry : msg.entries) {
                if (entry.listingIndex < 0 || entry.listingIndex >= listings.size()) continue;
                MarketListing listing = listings.get(entry.listingIndex);
                int qty = Math.min(entry.quantity, listing.getCount());
                if (qty <= 0) continue;
                totalCost += listing.getPricePerItem() * qty;
                validated.add(new ValidatedEntry(entry.listingIndex, qty, listing));
            }

            if (validated.isEmpty()) {
                player.displayClientMessage(
                        Component.literal("No valid items in cart!")
                                .withStyle(ChatFormatting.RED), true);
                return;
            }

            if (!TradingPostBlockEntity.hasEnoughCoins(player, totalCost, financeTable)) {
                player.displayClientMessage(
                        Component.literal("Not enough coins for this purchase!")
                                .withStyle(ChatFormatting.RED), true);
                return;
            }

            // Deduct coins with change-making (draws from Finance Table if needed)
            TradingPostBlockEntity.deductCoins(player, totalCost, financeTable);

            // Create buy orders for each item
            long gameTime = player.level.getGameTime();
            int orderCount = 0;

            for (ValidatedEntry entry : validated) {
                MarketListing listing = entry.listing;
                TownData town = TownRegistry.getTown(listing.getTownId());
                int travelTicks = town != null
                        ? town.getTravelTimeTicks(DebugConfig.getTicksPerDistance())
                        : 200;

                BuyOrder order = new BuyOrder(
                        UUID.randomUUID(),
                        listing.getTownId(),
                        listing.getItemId(),
                        listing.getItemDisplayName(),
                        entry.qty,
                        listing.getPricePerItem() * entry.qty,
                        gameTime,
                        gameTime + travelTicks,
                        listing.getItemNbt()
                );
                tradingPost.addBuyOrder(order);
                orderCount++;
            }

            // Update listings: remove fully purchased, reduce partial purchases
            // Process in reverse index order to avoid index shifting issues
            validated.sort((a, b) -> Integer.compare(b.index, a.index));
            for (ValidatedEntry entry : validated) {
                MarketListing listing = listings.get(entry.index);
                if (entry.qty >= listing.getCount()) {
                    listings.remove(entry.index);
                } else {
                    listings.set(entry.index, new MarketListing(
                            listing.getTownId(), listing.getItemId(),
                            listing.getItemDisplayName(),
                            listing.getCount() - entry.qty,
                            listing.getPricePerItem(), listing.getListedTime(),
                            listing.isOnSale(), listing.getSaleDiscount(),
                            listing.getItemNbt()
                    ));
                }
            }

            marketBoard.syncToClient();
            tradingPost.syncToClient();
            player.inventoryMenu.broadcastChanges();

            player.displayClientMessage(
                    Component.literal(orderCount + " order(s) placed! Check Trading Post for delivery.")
                            .withStyle(ChatFormatting.GREEN), true);
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * Find a Trading Post block entity within 8 blocks of the given position.
     */
    private static TradingPostBlockEntity findNearbyTradingPost(Level level, BlockPos center) {
        int radius = 8;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TradingPostBlockEntity tp) return tp;
        }
        return null;
    }

    private static class ValidatedEntry {
        final int index;
        final int qty;
        final MarketListing listing;

        ValidatedEntry(int index, int qty, MarketListing listing) {
            this.index = index;
            this.qty = qty;
            this.listing = listing;
        }
    }
}
