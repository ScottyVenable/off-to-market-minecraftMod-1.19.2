package com.offtomarket.mod.registry;

import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.block.entity.MailboxBlockEntity;
import com.offtomarket.mod.block.entity.MarketBoardBlockEntity;
import com.offtomarket.mod.block.entity.TradingLedgerBlockEntity;
import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, OffToMarket.MODID);

    public static final RegistryObject<BlockEntityType<TradingPostBlockEntity>> TRADING_POST =
            BLOCK_ENTITIES.register("trading_post",
                    () -> BlockEntityType.Builder.of(TradingPostBlockEntity::new,
                            ModBlocks.TRADING_POST.get()).build(null));

    public static final RegistryObject<BlockEntityType<TradingLedgerBlockEntity>> TRADING_LEDGER =
            BLOCK_ENTITIES.register("trading_ledger",
                    () -> BlockEntityType.Builder.of(TradingLedgerBlockEntity::new,
                            ModBlocks.TRADING_LEDGER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MarketBoardBlockEntity>> MARKET_BOARD =
            BLOCK_ENTITIES.register("market_board",
                    () -> BlockEntityType.Builder.of(MarketBoardBlockEntity::new,
                            ModBlocks.MARKET_BOARD.get()).build(null));

    public static final RegistryObject<BlockEntityType<MailboxBlockEntity>> MAILBOX =
            BLOCK_ENTITIES.register("mailbox",
                    () -> BlockEntityType.Builder.of(MailboxBlockEntity::new,
                            ModBlocks.MAILBOX.get()).build(null));
}
