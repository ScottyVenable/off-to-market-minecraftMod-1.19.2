package com.offtomarket.mod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class CoinItem extends Item {
    private final CoinType coinType;

    public CoinItem(Properties props, CoinType coinType) {
        super(props);
        this.coinType = coinType;
    }

    public CoinType getCoinType() {
        return coinType;
    }

    public int getValue() {
        return coinType.getValue();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        switch (coinType) {
            case GOLD -> tooltip.add(Component.translatable("tooltip.offtomarket.gold_coin")
                    .withStyle(ChatFormatting.GOLD));
            case SILVER -> tooltip.add(Component.translatable("tooltip.offtomarket.silver_coin")
                    .withStyle(ChatFormatting.GRAY));
            case COPPER -> tooltip.add(Component.translatable("tooltip.offtomarket.copper_coin")
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return coinType == CoinType.GOLD;
    }
}
