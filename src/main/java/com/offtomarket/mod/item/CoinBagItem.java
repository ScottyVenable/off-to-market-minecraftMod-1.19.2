package com.offtomarket.mod.item;

import com.offtomarket.mod.menu.CoinBagMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Coin Bag - portable coin storage with 3 slots (gold, silver, copper).
 * Right-click to open. Supports high stack counts for coin storage.
 */
public class CoinBagItem extends Item {

    public CoinBagItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            int slotIndex = hand == InteractionHand.MAIN_HAND
                    ? player.getInventory().selected
                    : Inventory.SLOT_OFFHAND;

            NetworkHooks.openScreen(sp, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Coin Bag");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player p) {
                    return new CoinBagMenu(containerId, inv, stack, slotIndex);
                }
            }, buf -> {
                // Write the slot index so the client can reconstruct
                buf.writeInt(slotIndex);
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("CoinBag")) {
            CompoundTag bagTag = tag.getCompound("CoinBag");
            int gold = bagTag.getInt("Gold");
            int silver = bagTag.getInt("Silver");
            int copper = bagTag.getInt("Copper");
            int total = gold * 100 + silver * 10 + copper;

            if (total > 0) {
                if (gold > 0) tooltip.add(Component.literal("  " + gold + " Gold")
                        .withStyle(ChatFormatting.GOLD));
                if (silver > 0) tooltip.add(Component.literal("  " + silver + " Silver")
                        .withStyle(ChatFormatting.GRAY));
                if (copper > 0) tooltip.add(Component.literal("  " + copper + " Copper")
                        .withStyle(ChatFormatting.RED));
                tooltip.add(Component.literal("  Total: " + total + " CP")
                        .withStyle(ChatFormatting.YELLOW));
            } else {
                tooltip.add(Component.literal("Empty")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Component.literal("Right-click to open")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
