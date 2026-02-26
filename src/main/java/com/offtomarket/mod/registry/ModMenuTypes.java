package com.offtomarket.mod.registry;

import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.block.entity.FinanceTableBlockEntity;
import com.offtomarket.mod.block.entity.MailboxBlockEntity;
import com.offtomarket.mod.block.entity.TradingLedgerBlockEntity;
import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import com.offtomarket.mod.block.entity.MarketBoardBlockEntity;
import com.offtomarket.mod.menu.CoinBagMenu;
import com.offtomarket.mod.menu.FinanceTableMenu;
import com.offtomarket.mod.menu.MailboxMenu;
import com.offtomarket.mod.menu.MarketBoardMenu;
import com.offtomarket.mod.menu.TradingLedgerMenu;
import com.offtomarket.mod.menu.TradingPostMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, OffToMarket.MODID);

    public static final RegistryObject<MenuType<TradingPostMenu>> TRADING_POST =
            MENUS.register("trading_post", () -> IForgeMenuType.create((windowId, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                BlockEntity be = inv.player.level.getBlockEntity(pos);
                if (be instanceof TradingPostBlockEntity tpbe) {
                    return new TradingPostMenu(windowId, inv, tpbe);
                }
                return new TradingPostMenu(windowId, inv);
            }));

    public static final RegistryObject<MenuType<TradingLedgerMenu>> TRADING_LEDGER =
            MENUS.register("trading_ledger", () -> IForgeMenuType.create((windowId, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                BlockEntity be = inv.player.level.getBlockEntity(pos);
                if (be instanceof TradingLedgerBlockEntity tbbe) {
                    return new TradingLedgerMenu(windowId, inv, tbbe);
                }
                return new TradingLedgerMenu(windowId, inv);
            }));

    public static final RegistryObject<MenuType<MarketBoardMenu>> MARKET_BOARD =
            MENUS.register("market_board", () -> IForgeMenuType.create((windowId, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                BlockEntity be = inv.player.level.getBlockEntity(pos);
                if (be instanceof MarketBoardBlockEntity mbbe) {
                    return new MarketBoardMenu(windowId, inv, mbbe);
                }
                return new MarketBoardMenu(windowId, inv);
            }));

    public static final RegistryObject<MenuType<CoinBagMenu>> COIN_BAG =
            MENUS.register("coin_bag", () -> IForgeMenuType.create((windowId, inv, data) -> {
                int slotIndex = data.readInt();
                return new CoinBagMenu(windowId, inv, inv.player.getInventory().getItem(slotIndex), slotIndex);
            }));

    public static final RegistryObject<MenuType<MailboxMenu>> MAILBOX =
            MENUS.register("mailbox", () -> IForgeMenuType.create((windowId, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                BlockEntity be = inv.player.level.getBlockEntity(pos);
                if (be instanceof MailboxBlockEntity mbe) {
                    return new MailboxMenu(windowId, inv, mbe);
                }
                return new MailboxMenu(windowId, inv);
            }));

    public static final RegistryObject<MenuType<FinanceTableMenu>> FINANCE_TABLE =
            MENUS.register("finance_table", () -> IForgeMenuType.create((windowId, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                BlockEntity be = inv.player.level.getBlockEntity(pos);
                if (be instanceof FinanceTableBlockEntity ftbe) {
                    return new FinanceTableMenu(windowId, inv, ftbe);
                }
                return new FinanceTableMenu(windowId, inv);
            }));
}
