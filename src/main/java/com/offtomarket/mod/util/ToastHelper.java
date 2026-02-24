package com.offtomarket.mod.util;

import com.offtomarket.mod.client.NotificationToastRenderer;
import com.offtomarket.mod.network.ModNetwork;
import com.offtomarket.mod.network.ShowToastPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

/**
 * Helper class for sending notification toasts from server to clients.
 */
public class ToastHelper {
    
    /** Range in blocks to receive toast notifications from a trading post. */
    private static final double NOTIFICATION_RANGE = 32.0;
    
    /**
     * Send a toast to a specific player.
     */
    public static void sendToPlayer(ServerPlayer player, String title, String message, 
            NotificationToastRenderer.ToastType type) {
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ShowToastPacket(title, message, type)
        );
    }
    
    /**
     * Send a toast to all players near a position.
     */
    public static void sendToNearby(Level level, BlockPos pos, String title, String message,
            NotificationToastRenderer.ToastType type) {
        if (level instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                if (player.blockPosition().closerThan(pos, NOTIFICATION_RANGE)) {
                    sendToPlayer(player, title, message, type);
                }
            }
        }
    }
    
    // Convenience methods for common notification types
    
    /**
     * Notify nearby players that a shipment has arrived.
     */
    public static void notifyShipmentArrived(Level level, BlockPos pos, String townName) {
        sendToNearby(level, pos, "📦 Shipment Arrived!", 
                "Cart returned from " + townName, 
                NotificationToastRenderer.ToastType.INFO);
    }
    
    /**
     * Notify nearby players that items were sold.
     */
    public static void notifyItemsSold(Level level, BlockPos pos, int coinAmount) {
        sendToNearby(level, pos, "💰 Items Sold!", 
                "Earned " + coinAmount + " coins", 
                NotificationToastRenderer.ToastType.COINS);
    }
    
    /**
     * Notify nearby players about earnings collected.
     */
    public static void notifyCoinsCollected(Level level, BlockPos pos, int coinAmount) {
        sendToNearby(level, pos, "✨ Coins Collected", 
                coinAmount + " coins added to inventory", 
                NotificationToastRenderer.ToastType.SUCCESS);
    }
    
    /**
     * Notify nearby players of a trader level up.
     */
    public static void notifyLevelUp(Level level, BlockPos pos, int newLevel) {
        sendToNearby(level, pos, "⭐ Level Up!", 
                "Trading Post reached Level " + newLevel, 
                NotificationToastRenderer.ToastType.LEVEL_UP);
    }
    
    /**
     * Notify nearby players that a diplomat has a proposal ready.
     */
    public static void notifyDiplomatProposal(Level level, BlockPos pos, String townName) {
        sendToNearby(level, pos, "🤝 Diplomat Proposal", 
                "Trade deal ready from " + townName, 
                NotificationToastRenderer.ToastType.DIPLOMAT);
    }
    
    /**
     * Notify nearby players that a diplomat has returned.
     */
    public static void notifyDiplomatReturned(Level level, BlockPos pos, String townName, boolean accepted) {
        String message = accepted ? 
                "Deal with " + townName + " accepted!" : 
                "Returned from " + townName;
        sendToNearby(level, pos, "🤝 Diplomat Returned", 
                message, 
                NotificationToastRenderer.ToastType.DIPLOMAT);
    }
    
    /**
     * Notify nearby players of a quest completion.
     */
    public static void notifyQuestComplete(Level level, BlockPos pos, int rewardCoins, int rewardXp) {
        sendToNearby(level, pos, "✓ Quest Complete!", 
                "+" + rewardCoins + " coins, +" + rewardXp + " XP", 
                NotificationToastRenderer.ToastType.SUCCESS);
    }
    
    /**
     * Notify nearby players of a warning (e.g., shipment about to expire).
     */
    public static void notifyWarning(Level level, BlockPos pos, String message) {
        sendToNearby(level, pos, "⚠ Warning", 
                message, 
                NotificationToastRenderer.ToastType.WARNING);
    }
    
    /**
     * Notify nearby players of a new quest available.
     */
    public static void notifyNewQuest(Level level, BlockPos pos, String questName) {
        sendToNearby(level, pos, "📜 New Quest!", 
                questName, 
                NotificationToastRenderer.ToastType.INFO);
    }
    
    /**
     * Notify nearby players of a worker being hired.
     */
    public static void notifyWorkerHired(Level level, BlockPos pos, String workerType) {
        sendToNearby(level, pos, "👷 Worker Hired!", 
                workerType + " now working", 
                NotificationToastRenderer.ToastType.SUCCESS);
    }
}
