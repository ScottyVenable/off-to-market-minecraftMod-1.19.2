package com.offtomarket.mod.registry;

import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.block.entity.MarketBoardBlockEntity;
import com.offtomarket.mod.block.entity.TradingBinBlockEntity;
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

    public static final RegistryObject<BlockEntityType<TradingBinBlockEntity>> TRADING_BIN =
            BLOCK_ENTITIES.register("trading_bin",
                    () -> BlockEntityType.Builder.of(TradingBinBlockEntity::new,
                            ModBlocks.TRADING_BIN.get()).build(null));

    public static final RegistryObject<BlockEntityType<MarketBoardBlockEntity>> MARKET_BOARD =
            BLOCK_ENTITIES.register("market_board",
                    () -> BlockEntityType.Builder.of(MarketBoardBlockEntity::new,
                            ModBlocks.MARKET_BOARD.get()).build(null));
}
