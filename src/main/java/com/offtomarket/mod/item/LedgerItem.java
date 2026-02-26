package com.offtomarket.mod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import com.offtomarket.mod.registry.ModBlocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Trader's Ledger - can be synced to a Trading Post by placing it in
 * the dedicated slot. When synced, right-clicking it opens a portable
 * trading interface. Must be placed in the Trading Post's ledger slot to sync.
 */
public class LedgerItem extends Item {
    public static final String TAG_SYNCED = "SyncedPost";
    public static final String TAG_POST_X = "PostX";
    public static final String TAG_POST_Y = "PostY";
    public static final String TAG_POST_Z = "PostZ";

    public LedgerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        // Right-click on a Trading Post to sync the ledger
        if (level.getBlockState(pos).is(ModBlocks.TRADING_POST.get())) {
            if (!level.isClientSide()) {
                ItemStack stack = context.getItemInHand();
                CompoundTag tag = stack.getOrCreateTag();
                tag.putBoolean(TAG_SYNCED, true);
                tag.putInt(TAG_POST_X, pos.getX());
                tag.putInt(TAG_POST_Y, pos.getY());
                tag.putInt(TAG_POST_Z, pos.getZ());

                context.getPlayer().displayClientMessage(
                        Component.literal("Ledger synced to Trading Post at " +
                                pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                                .withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Sneak + use opens the Coin Exchange
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft.getInstance().setScreen(
                            new com.offtomarket.mod.client.screen.CoinExchangeScreen());
                });
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // Normal use: open Master Ledger screen showing all nearby Trading Ledger bins
        if (level.isClientSide()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.offtomarket.mod.client.screen.MasterLedgerScreen());
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static boolean isSynced(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_SYNCED);
    }

    @Nullable
    public static BlockPos getSyncedPos(ItemStack stack) {
        if (!isSynced(stack)) return null;
        CompoundTag tag = stack.getTag();
        return new BlockPos(
                tag.getInt(TAG_POST_X),
                tag.getInt(TAG_POST_Y),
                tag.getInt(TAG_POST_Z));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isSynced(stack)) {
            BlockPos pos = getSyncedPos(stack);
            tooltip.add(Component.translatable("tooltip.offtomarket.ledger.synced",
                            pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.offtomarket.ledger.not_synced")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
