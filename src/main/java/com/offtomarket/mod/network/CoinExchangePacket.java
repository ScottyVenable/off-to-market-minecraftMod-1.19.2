package com.offtomarket.mod.network;

import com.offtomarket.mod.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client when the player clicks a conversion button in the Coin Exchange screen.
 */
public class CoinExchangePacket {

    public enum ConversionType {
        /** 10 Copper → 1 Silver */
        CP_TO_SP(ModItems.COPPER_COIN, 10, ModItems.SILVER_COIN, 1, "10 CP \u2192 1 SP"),
        /** 10 Silver → 1 Gold */
        SP_TO_GP(ModItems.SILVER_COIN, 10, ModItems.GOLD_COIN, 1, "10 SP \u2192 1 GP"),
        /** 1 Gold → 10 Silver */
        GP_TO_SP(ModItems.GOLD_COIN, 1, ModItems.SILVER_COIN, 10, "1 GP \u2192 10 SP"),
        /** 1 Silver → 10 Copper */
        SP_TO_CP(ModItems.SILVER_COIN, 1, ModItems.COPPER_COIN, 10, "1 SP \u2192 10 CP");

        private final java.util.function.Supplier<Item> sourceItem;
        private final int sourceCost;
        private final java.util.function.Supplier<Item> resultItem;
        private final int resultAmount;
        private final String label;

        ConversionType(java.util.function.Supplier<Item> source, int cost,
                       java.util.function.Supplier<Item> result, int amount, String label) {
            this.sourceItem = source;
            this.sourceCost = cost;
            this.resultItem = result;
            this.resultAmount = amount;
            this.label = label;
        }

        public Item getSourceItem() { return sourceItem.get(); }
        public int getSourceCost() { return sourceCost; }
        public Item getResultItem() { return resultItem.get(); }
        public int getResultAmount() { return resultAmount; }
        public String getLabel() { return label; }
    }

    private final int conversionOrdinal;

    public CoinExchangePacket(ConversionType type) {
        this.conversionOrdinal = type.ordinal();
    }

    private CoinExchangePacket(int ordinal) {
        this.conversionOrdinal = ordinal;
    }

    public static void encode(CoinExchangePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.conversionOrdinal);
    }

    public static CoinExchangePacket decode(FriendlyByteBuf buf) {
        return new CoinExchangePacket(buf.readInt());
    }

    public static void handle(CoinExchangePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ConversionType[] types = ConversionType.values();
            if (msg.conversionOrdinal < 0 || msg.conversionOrdinal >= types.length) return;

            ConversionType conversion = types[msg.conversionOrdinal];
            Item source = conversion.getSourceItem();
            int cost = conversion.getSourceCost();

            // Count how many of the source coin the player has
            int available = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() == source) {
                    available += stack.getCount();
                }
            }

            if (available < cost) {
                player.displayClientMessage(
                        Component.literal("Not enough coins for that exchange!")
                                .withStyle(ChatFormatting.RED), true);
                return;
            }

            // Remove source coins
            int toRemove = cost;
            for (int i = 0; i < player.getInventory().getContainerSize() && toRemove > 0; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() == source) {
                    int take = Math.min(stack.getCount(), toRemove);
                    stack.shrink(take);
                    toRemove -= take;
                    if (stack.isEmpty()) {
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
            }

            // Give result coins
            ItemStack result = new ItemStack(conversion.getResultItem(), conversion.getResultAmount());
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }

            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
}
