package com.offtomarket.mod.network;

import com.offtomarket.mod.OffToMarket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(OffToMarket.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, SendShipmentPacket.class,
                SendShipmentPacket::encode, SendShipmentPacket::decode,
                SendShipmentPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, SelectTownPacket.class,
                SelectTownPacket::encode, SelectTownPacket::decode,
                SelectTownPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, CollectCoinsPacket.class,
                CollectCoinsPacket::encode, CollectCoinsPacket::decode,
                CollectCoinsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, RequestReturnPacket.class,
                RequestReturnPacket::encode, RequestReturnPacket::decode,
                RequestReturnPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, SetDistancePacket.class,
                SetDistancePacket::encode, SetDistancePacket::decode,
                SetDistancePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, SetPricePacket.class,
                SetPricePacket::encode, SetPricePacket::decode,
                SetPricePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, RefreshMarketPacket.class,
                RefreshMarketPacket::encode, RefreshMarketPacket::decode,
                RefreshMarketPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, CoinExchangePacket.class,
                CoinExchangePacket::encode, CoinExchangePacket::decode,
                CoinExchangePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, BuyMarketItemPacket.class,
                BuyMarketItemPacket::encode, BuyMarketItemPacket::decode,
                BuyMarketItemPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, CartCheckoutPacket.class,
                CartCheckoutPacket::encode, CartCheckoutPacket::decode,
                CartCheckoutPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, CollectOrderPacket.class,
                CollectOrderPacket::encode, CollectOrderPacket::decode,
                CollectOrderPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, CoinSlotConvertPacket.class,
                CoinSlotConvertPacket::encode, CoinSlotConvertPacket::decode,
                CoinSlotConvertPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, AcceptQuestPacket.class,
                AcceptQuestPacket::encode, AcceptQuestPacket::decode,
                AcceptQuestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, DeliverQuestPacket.class,
                DeliverQuestPacket::encode, DeliverQuestPacket::decode,
                DeliverQuestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, HireWorkerPacket.class,
                HireWorkerPacket::encode, HireWorkerPacket::decode,
                HireWorkerPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, FireWorkerPacket.class,
                FireWorkerPacket::encode, FireWorkerPacket::decode,
                FireWorkerPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, SendDiplomatPacket.class,
                SendDiplomatPacket::encode, SendDiplomatPacket::decode,
                SendDiplomatPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, CollectDiplomatPacket.class,
                CollectDiplomatPacket::encode, CollectDiplomatPacket::decode,
                CollectDiplomatPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, AcceptDiplomatPacket.class,
                AcceptDiplomatPacket::encode, AcceptDiplomatPacket::decode,
                AcceptDiplomatPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, DeclineDiplomatPacket.class,
                DeclineDiplomatPacket::encode, DeclineDiplomatPacket::decode,
                DeclineDiplomatPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, UpdateBinSettingsPacket.class,
                UpdateBinSettingsPacket::encode, UpdateBinSettingsPacket::decode,
                UpdateBinSettingsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, CollectShipmentCoinsPacket.class,
                CollectShipmentCoinsPacket::encode, CollectShipmentCoinsPacket::decode,
                CollectShipmentCoinsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, CancelShipmentPacket.class,
                CancelShipmentPacket::encode, CancelShipmentPacket::decode,
                CancelShipmentPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, CollectReturnedItemsPacket.class,
                CollectReturnedItemsPacket::encode, CollectReturnedItemsPacket::decode,
                CollectReturnedItemsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, AdjustShipmentPricePacket.class,
                AdjustShipmentPricePacket::encode, AdjustShipmentPricePacket::decode,
                AdjustShipmentPricePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, CreateRequestPacket.class,
                CreateRequestPacket::encode, CreateRequestPacket::decode,
                CreateRequestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, ShowToastPacket.class,
                ShowToastPacket::encode, ShowToastPacket::new,
                ShowToastPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, MarkNoteReadPacket.class,
                MarkNoteReadPacket::encode, MarkNoteReadPacket::decode,
                MarkNoteReadPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, DeleteNotePacket.class,
                DeleteNotePacket::encode, DeleteNotePacket::decode,
                DeleteNotePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, DeleteAllReadNotesPacket.class,
                DeleteAllReadNotesPacket::encode, DeleteAllReadNotesPacket::decode,
                DeleteAllReadNotesPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, WithdrawBinItemPacket.class,
                WithdrawBinItemPacket::encode, WithdrawBinItemPacket::decode,
                WithdrawBinItemPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, UpgradeCaravanWeightPacket.class,
                UpgradeCaravanWeightPacket::encode, UpgradeCaravanWeightPacket::decode,
                UpgradeCaravanWeightPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, OpenCustomMenuPacket.class,
                OpenCustomMenuPacket::encode, OpenCustomMenuPacket::decode,
                OpenCustomMenuPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, DepositCoinsPacket.class,
                DepositCoinsPacket::encode, DepositCoinsPacket::decode,
                DepositCoinsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, WithdrawCoinsPacket.class,
                WithdrawCoinsPacket::encode, WithdrawCoinsPacket::decode,
                WithdrawCoinsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, WithdrawAmountPacket.class,
                WithdrawAmountPacket::encode, WithdrawAmountPacket::decode,
                WithdrawAmountPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, DepositAmountPacket.class,
                DepositAmountPacket::encode, DepositAmountPacket::decode,
                DepositAmountPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, DispatchScoutPacket.class,
                DispatchScoutPacket::encode, DispatchScoutPacket::decode,
                DispatchScoutPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
