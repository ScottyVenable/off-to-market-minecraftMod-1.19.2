package com.offtomarket.mod.item;

import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import com.offtomarket.mod.data.TownData;
import com.offtomarket.mod.data.TownDepotManager;
import com.offtomarket.mod.data.TownRegistry;
import com.offtomarket.mod.data.TradingData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Route Marker Tool:
 * Allows players to designate physical containers (barrels, chests) for specific trade towns.
 * Right-clicking a container flood-fills and links all touching contiguous containers into
 * a unified Town Trade Depot loading dock.
 * Right-clicking air cycles the active target town.
 */
public class RouteMarkerItem extends Item {

    public RouteMarkerItem(Properties properties) {
        super(properties);
    }

    public static String getTargetTown(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("TargetTown") && !tag.getString("TargetTown").isEmpty()) {
            return tag.getString("TargetTown");
        }
        return "saltmarsh"; // default starter trade town
    }

    public static void setTargetTown(ItemStack stack, String townId) {
        stack.getOrCreateTag().putString("TargetTown", townId);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || !be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (level instanceof ServerLevel serverLevel) {
            TradingData tradingData = TradingData.get(serverLevel);
            TownDepotManager depot = tradingData.getDepotManager();

            if (player != null && player.isShiftKeyDown()) {
                // Unbind touching cluster
                Set<BlockPos> unlinked = depot.unassignCluster(level, pos, 64);
                tradingData.setDirty();
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.9f);
                player.displayClientMessage(
                        Component.literal("\u2718 Unlinked " + unlinked.size() + " container(s) from trade routes.")
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResult.SUCCESS;
            } else {
                // Bind touching cluster to target town
                String townId = getTargetTown(stack);
                Set<BlockPos> linked = depot.linkContiguousCluster(level, pos, townId, 64);
                tradingData.setDirty();

                TownData town = TownRegistry.getTown(townId);
                String townName = town != null ? town.getDisplayName() : townId;

                level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.8f, 1.2f);
                if (player != null) {
                    player.displayClientMessage(
                            Component.literal("\u2714 Linked " + linked.size() + " container(s) to " + townName + " Trade Depot!")
                                    .withStyle(ChatFormatting.GREEN), true);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        List<TownData> towns = new ArrayList<>(TownRegistry.getAllTowns());
        if (towns.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }

        String currentTown = getTargetTown(stack);
        int currentIndex = 0;
        for (int i = 0; i < towns.size(); i++) {
            if (towns.get(i).getId().equalsIgnoreCase(currentTown)) {
                currentIndex = i;
                break;
            }
        }

        int nextIndex = (currentIndex + 1) % towns.size();
        TownData nextTown = towns.get(nextIndex);
        setTargetTown(stack, nextTown.getId());

        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.5f, 1.2f);

        if (!level.isClientSide()) {
            player.displayClientMessage(
                    Component.literal("Route Marker set to: \u00A76" + nextTown.getDisplayName() + "\u00A7r (" + nextTown.getType().getDisplayName() + ")")
                            .withStyle(ChatFormatting.YELLOW), true);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String townId = getTargetTown(stack);
        TownData town = TownRegistry.getTown(townId);
        String townName = town != null ? town.getDisplayName() : townId;

        tooltip.add(Component.literal("Target Route: " + townName)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Right-click container: Bind touching containers to route")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Shift + Right-click container: Unbind cluster")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("Right-click air: Cycle destination town")
                .withStyle(ChatFormatting.AQUA));
    }
}
