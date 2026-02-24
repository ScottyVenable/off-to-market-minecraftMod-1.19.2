package com.offtomarket.mod.registry;

import com.offtomarket.mod.OffToMarket;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTab {
    public static final CreativeModeTab TAB = new CreativeModeTab(OffToMarket.MODID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.GOLD_COIN.get());
        }
    };
}
