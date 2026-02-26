package com.offtomarket.mod;

import com.mojang.logging.LogUtils;
import com.offtomarket.mod.config.ModConfig;
import com.offtomarket.mod.config.ModdedItemConfig;
import com.offtomarket.mod.content.CustomMenuLoader;
import com.offtomarket.mod.content.TownLoader;
import com.offtomarket.mod.data.ModCompatibility;
import com.offtomarket.mod.data.SupplyDemandManager;
import com.offtomarket.mod.debug.DebugCommands;
import com.offtomarket.mod.debug.DebugHooks;
import com.offtomarket.mod.network.ModNetwork;
import com.offtomarket.mod.registry.ModBlockEntities;
import com.offtomarket.mod.registry.ModBlocks;
import com.offtomarket.mod.registry.ModItems;
import com.offtomarket.mod.registry.ModMenuTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(OffToMarket.MODID)
public class OffToMarket {
    public static final String MODID = "offtomarket";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OffToMarket() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register deferred registers
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);

        // Lifecycle events
        modEventBus.addListener(this::commonSetup);

        // Register for game events
        MinecraftForge.EVENT_BUS.register(this);

        // Config
        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Off to Market - Trading Deluxe initializing!");
        event.enqueueWork(() -> {
            ModNetwork.register();
            ModdedItemConfig.loadAllConfigs();

            // Load towns from JSON definitions (overrides any hardcoded fallbacks)
            TownLoader.loadAll();
            // Load custom menu definitions for /otm menu open <id>
            CustomMenuLoader.loadAll();

            // Initialize mod compatibility - discovers items from other mods
            ModCompatibility.initialize();
            LOGGER.info("Discovered {} mods with tradeable items", ModCompatibility.getLoadedModsWithItems().size());
            LOGGER.info("Generated {} dynamic towns from mods", ModCompatibility.getDynamicTowns().size());
            for (var town : ModCompatibility.getDynamicTowns().values()) {
                LOGGER.info("  - {} ({})", town.getDisplayName(), town.getId());
            }
        });
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer() != null) {
            DebugHooks.onServerTick(event.getServer());
            SupplyDemandManager.onServerTick(event.getServer());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DebugCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onMissingMappings(MissingMappingsEvent event) {
        // Remap old "trading_bin" IDs to "trading_ledger" for world compatibility
        ResourceLocation oldId = new ResourceLocation(MODID, "trading_bin");

        for (var mapping : event.getMappings(ForgeRegistries.Keys.BLOCKS, MODID)) {
            if (mapping.getKey().equals(oldId)) {
                mapping.remap(ModBlocks.TRADING_LEDGER.get());
            }
        }
        for (var mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, MODID)) {
            if (mapping.getKey().equals(oldId)) {
                mapping.remap(ModItems.TRADING_LEDGER_ITEM.get());
            }
        }
        for (var mapping : event.getMappings(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, MODID)) {
            if (mapping.getKey().equals(oldId)) {
                mapping.remap(ModBlockEntities.TRADING_LEDGER.get());
            }
        }
    }
}
