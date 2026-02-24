package com.offtomarket.mod.event;

import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.client.screen.CoinBagScreen;
import com.offtomarket.mod.client.screen.MailboxScreen;
import com.offtomarket.mod.client.screen.MarketBoardScreen;
import com.offtomarket.mod.client.screen.TradingBinScreen;
import com.offtomarket.mod.client.screen.TradingPostScreen;
import com.offtomarket.mod.debug.DebugOverlay;
import com.offtomarket.mod.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = OffToMarket.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.TRADING_POST.get(), TradingPostScreen::new);
            MenuScreens.register(ModMenuTypes.TRADING_BIN.get(), TradingBinScreen::new);
            MenuScreens.register(ModMenuTypes.MARKET_BOARD.get(), MarketBoardScreen::new);
            MenuScreens.register(ModMenuTypes.COIN_BAG.get(), CoinBagScreen::new);
            MenuScreens.register(ModMenuTypes.MAILBOX.get(), MailboxScreen::new);

            // Register debug overlay on the FORGE bus (game events, not mod lifecycle)
            MinecraftForge.EVENT_BUS.register(new DebugOverlay());
        });
    }
}
