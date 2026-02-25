package com.offtomarket.mod.registry;

import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.item.AnimalTradeSlipItem;
import com.offtomarket.mod.item.CoinBagItem;
import com.offtomarket.mod.item.CoinItem;
import com.offtomarket.mod.item.CoinType;
import com.offtomarket.mod.item.GuideBookItem;
import com.offtomarket.mod.item.LedgerItem;
import com.offtomarket.mod.item.ShipmentNoteItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, OffToMarket.MODID);

    // Block items
    public static final RegistryObject<Item> TRADING_POST_ITEM = ITEMS.register("trading_post",
            () -> new BlockItem(ModBlocks.TRADING_POST.get(),
                    new Item.Properties().tab(ModCreativeTab.TAB)));

    public static final RegistryObject<Item> TRADING_LEDGER_ITEM = ITEMS.register("trading_ledger",
            () -> new BlockItem(ModBlocks.TRADING_LEDGER.get(),
                    new Item.Properties().tab(ModCreativeTab.TAB)));

    public static final RegistryObject<Item> MARKET_BOARD_ITEM = ITEMS.register("market_board",
            () -> new BlockItem(ModBlocks.MARKET_BOARD.get(),
                    new Item.Properties().tab(ModCreativeTab.TAB)));

    // Currency
    public static final RegistryObject<Item> GOLD_COIN = ITEMS.register("gold_coin",
            () -> new CoinItem(new Item.Properties().tab(ModCreativeTab.TAB).rarity(Rarity.RARE)
                    .stacksTo(99), CoinType.GOLD));

    public static final RegistryObject<Item> SILVER_COIN = ITEMS.register("silver_coin",
            () -> new CoinItem(new Item.Properties().tab(ModCreativeTab.TAB).rarity(Rarity.UNCOMMON)
                    .stacksTo(99), CoinType.SILVER));

    public static final RegistryObject<Item> COPPER_COIN = ITEMS.register("copper_coin",
            () -> new CoinItem(new Item.Properties().tab(ModCreativeTab.TAB)
                    .stacksTo(99), CoinType.COPPER));

    // Tools
    public static final RegistryObject<Item> LEDGER = ITEMS.register("ledger",
            () -> new LedgerItem(new Item.Properties().tab(ModCreativeTab.TAB)
                    .stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SHIPMENT_NOTE = ITEMS.register("shipment_note",
            () -> new ShipmentNoteItem(new Item.Properties().tab(ModCreativeTab.TAB)
                    .stacksTo(16)));

    // Guide
    public static final RegistryObject<Item> GUIDE_BOOK = ITEMS.register("guide_book",
            () -> new GuideBookItem(new Item.Properties().tab(ModCreativeTab.TAB)
                    .stacksTo(1).rarity(Rarity.UNCOMMON)));

    // Coin Bag
    public static final RegistryObject<Item> COIN_BAG = ITEMS.register("coin_bag",
            () -> new CoinBagItem(new Item.Properties().tab(ModCreativeTab.TAB)
                    .stacksTo(1).rarity(Rarity.UNCOMMON)));

    // Animal Trade Slip
    public static final RegistryObject<Item> ANIMAL_TRADE_SLIP = ITEMS.register("animal_trade_slip",
            () -> new AnimalTradeSlipItem(new Item.Properties().tab(ModCreativeTab.TAB)
                    .stacksTo(16)));

    // Mailbox
    public static final RegistryObject<Item> MAILBOX_ITEM = ITEMS.register("mailbox",
            () -> new BlockItem(ModBlocks.MAILBOX.get(),
                    new Item.Properties().tab(ModCreativeTab.TAB)));

    // Finance Table
    public static final RegistryObject<Item> FINANCE_TABLE_ITEM = ITEMS.register("finance_table",
            () -> new BlockItem(ModBlocks.FINANCE_TABLE.get(),
                    new Item.Properties().tab(ModCreativeTab.TAB)));
}
