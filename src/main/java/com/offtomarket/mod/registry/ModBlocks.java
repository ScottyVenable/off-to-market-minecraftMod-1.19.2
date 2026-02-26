package com.offtomarket.mod.registry;

import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.block.FinanceTableBlock;
import com.offtomarket.mod.block.MailboxBlock;
import com.offtomarket.mod.block.MarketBoardBlock;
import com.offtomarket.mod.block.TradingLedgerBlock;
import com.offtomarket.mod.block.TradingPostBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, OffToMarket.MODID);

    public static final RegistryObject<Block> TRADING_POST = BLOCKS.register("trading_post",
            () -> new TradingPostBlock(BlockBehaviour.Properties.of(Material.WOOD)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final RegistryObject<Block> TRADING_LEDGER = BLOCKS.register("trading_ledger",
            () -> new TradingLedgerBlock(BlockBehaviour.Properties.of(Material.WOOD)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> MARKET_BOARD = BLOCKS.register("market_board",
            () -> new MarketBoardBlock(BlockBehaviour.Properties.of(Material.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final RegistryObject<Block> MAILBOX = BLOCKS.register("mailbox",
            () -> new MailboxBlock(BlockBehaviour.Properties.of(Material.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final RegistryObject<Block> FINANCE_TABLE = BLOCKS.register("finance_table",
            () -> new FinanceTableBlock(BlockBehaviour.Properties.of(Material.WOOD)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()));
}
