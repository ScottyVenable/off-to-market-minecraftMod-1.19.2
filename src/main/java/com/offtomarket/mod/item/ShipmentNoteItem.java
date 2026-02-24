package com.offtomarket.mod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A note left in the Trading Bin after items are picked up for shipment.
 * Contains information about what was shipped and where.
 */
public class ShipmentNoteItem extends Item {
    public static final String TAG_TOWN = "Town";
    public static final String TAG_ITEMS = "ShippedItems";
    public static final String TAG_TIMESTAMP = "Timestamp";
    public static final String TAG_TRAVEL_TICKS = "TravelTicks";
    public static final String TAG_MARKET_TICKS = "MarketTicks";

    public ShipmentNoteItem(Properties props) {
        super(props);
    }

    /**
     * Create a shipment note with the given details.
     */
    public static ItemStack createNote(String townName, List<ItemStack> items, long timestamp,
                                        int travelTicks, int maxMarketTicks) {
        ItemStack note = new ItemStack(com.offtomarket.mod.registry.ModItems.SHIPMENT_NOTE.get());
        CompoundTag tag = note.getOrCreateTag();
        tag.putString(TAG_TOWN, townName);
        tag.putLong(TAG_TIMESTAMP, timestamp);
        tag.putInt(TAG_TRAVEL_TICKS, travelTicks);
        tag.putInt(TAG_MARKET_TICKS, maxMarketTicks);

        ListTag itemList = new ListTag();
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putString("Name", item.getHoverName().getString());
                itemTag.putInt("Count", item.getCount());
                itemList.add(itemTag);
            }
        }
        tag.put(TAG_ITEMS, itemList);

        return note;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                player.displayClientMessage(
                        Component.literal("=== Shipment Note ===").withStyle(ChatFormatting.GOLD), false);
                player.displayClientMessage(
                        Component.literal("Your items have been brought to be sold.")
                                .withStyle(ChatFormatting.YELLOW), false);
                player.displayClientMessage(
                        Component.literal("Destination: " + tag.getString(TAG_TOWN))
                                .withStyle(ChatFormatting.AQUA), false);

                // Estimated times
                if (tag.contains(TAG_TRAVEL_TICKS)) {
                    int travel = tag.getInt(TAG_TRAVEL_TICKS);
                    int market = tag.getInt(TAG_MARKET_TICKS);
                    player.displayClientMessage(
                            Component.literal("Est. travel: " + ticksToTime(travel) +
                                    "  |  Max market time: " + ticksToTime(market))
                                    .withStyle(ChatFormatting.DARK_AQUA), false);
                }

                player.displayClientMessage(
                        Component.literal("Items shipped:").withStyle(ChatFormatting.WHITE), false);

                ListTag items = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
                for (int i = 0; i < items.size(); i++) {
                    CompoundTag itemTag = items.getCompound(i);
                    String name = itemTag.getString("Name");
                    int count = itemTag.getInt("Count");
                    player.displayClientMessage(
                            Component.literal("  - " + name + " x" + count)
                                    .withStyle(ChatFormatting.GRAY), false);
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tooltip.add(Component.literal("Destination: " + tag.getString(TAG_TOWN))
                    .withStyle(ChatFormatting.AQUA));

            if (tag.contains(TAG_TRAVEL_TICKS)) {
                int travel = tag.getInt(TAG_TRAVEL_TICKS);
                int market = tag.getInt(TAG_MARKET_TICKS);
                tooltip.add(Component.literal("Travel: " + ticksToTime(travel) +
                        "  |  Market: " + ticksToTime(market))
                        .withStyle(ChatFormatting.DARK_AQUA));
            }

            tooltip.add(Component.literal("\"Your items have been brought to be sold.\"")
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.YELLOW));

            ListTag items = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
            if (items.size() > 0) {
                tooltip.add(Component.literal("Items: " + items.size() + " type(s)")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_TOWN)) {
            return Component.literal("Shipment Note - " + tag.getString(TAG_TOWN));
        }
        return super.getName(stack);
    }

    private static String ticksToTime(long ticks) {
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        seconds %= 60;
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
