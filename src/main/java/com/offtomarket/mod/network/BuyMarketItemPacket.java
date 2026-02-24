package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.MarketBoardBlockEntity;
import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import com.offtomarket.mod.data.MarketListing;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Sent from client when the player clicks "Buy" on a market listing.
 * Contains the Market Board block position and the index of the listing to purchase.
 */
public class BuyMarketItemPacket {

    private final BlockPos pos;
    private final int listingIndex;

    public BuyMarketItemPacket(BlockPos pos, int listingIndex) {
        this.pos = pos;
        this.listingIndex = listingIndex;
    }

    public static void encode(BuyMarketItemPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.listingIndex);
    }

    public static BuyMarketItemPacket decode(FriendlyByteBuf buf) {
        return new BuyMarketItemPacket(buf.readBlockPos(), buf.readInt());
    }

    public static void handle(BuyMarketItemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Validate block entity
            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof MarketBoardBlockEntity marketBoard)) return;

            // Validate distance (same as menu stillValid check)
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;

            List<MarketListing> listings = marketBoard.getListings();

            // Validate listing index
            if (msg.listingIndex < 0 || msg.listingIndex >= listings.size()) {
                player.displayClientMessage(
                        Component.literal("That listing is no longer available.")
                                .withStyle(ChatFormatting.RED), true);
                return;
            }

            MarketListing listing = listings.get(msg.listingIndex);
            int totalCost = listing.getTotalPrice();

            // Check if player has enough coins
            if (!TradingPostBlockEntity.hasEnoughCoins(player, totalCost)) {
                player.displayClientMessage(
                        Component.literal("Not enough coins! Need " + formatCoinValue(totalCost))
                                .withStyle(ChatFormatting.RED), true);
                return;
            }

            // Deduct coins from player inventory
            TradingPostBlockEntity.deductCoins(player, totalCost);

            // Give the purchased item to the player
            ItemStack stack = listing.createItemStack(listing.getCount());
            if (!stack.isEmpty()) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }

            // Remove the listing
            listings.remove(msg.listingIndex);
            marketBoard.syncToClient();

            // Update player's container view
            player.inventoryMenu.broadcastChanges();

            // Success message
            player.displayClientMessage(
                    Component.literal("Purchased " + listing.getCount() + "x " +
                            listing.getItemDisplayName() + " for " + formatCoinValue(totalCost) + "!")
                            .withStyle(ChatFormatting.GREEN), true);
        });
        ctx.get().setPacketHandled(true);
    }

    private static String formatCoinValue(int copper) {
        int gp = copper / 100;
        int sp = (copper % 100) / 10;
        int cp = copper % 10;
        StringBuilder sb = new StringBuilder();
        if (gp > 0) sb.append(gp).append("GP ");
        if (sp > 0) sb.append(sp).append("SP ");
        if (cp > 0 || sb.length() == 0) sb.append(cp).append("CP");
        return sb.toString().trim();
    }
}
