package com.offtomarket.mod.debug;

import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.block.entity.TradingBinBlockEntity;
import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import com.offtomarket.mod.data.Shipment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Debug hooks that run every server tick.
 * <p>
 * These integrate with the DebugConfig globals so that edits made in the
 * debugger's Variables pane take effect each tick.
 * <p>
 * SET A BREAKPOINT on the first line of {@link #onServerTick} to inspect
 * all DebugConfig statics at a glance.
 */
public class DebugHooks {

    private static long lastTickNano = System.nanoTime();
    private static long tickCount = 0;
    private static final double[] tpsSamples = new double[20];
    private static int tpsIndex = 0;

    /**
     * Called every server tick from the event handler.
     * Place a breakpoint here to inspect/edit all debug globals.
     */
    public static void onServerTick(MinecraftServer server) {
        // ---- TPS measurement ----
        long now = System.nanoTime();
        double elapsed = (now - lastTickNano) / 1_000_000_000.0;
        lastTickNano = now;
        if (elapsed > 0) {
            tpsSamples[tpsIndex] = 1.0 / elapsed;
            tpsIndex = (tpsIndex + 1) % tpsSamples.length;
            double sum = 0;
            for (double s : tpsSamples) sum += s;
            DebugConfig.WATCH_SERVER_TPS = Math.round(sum / tpsSamples.length * 10.0) / 10.0;
        }
        tickCount++;

        if (!DebugConfig.DEBUG_ENABLED) return;

        // ---- Update game time watch ----
        ServerLevel overworld = server.overworld();
        if (overworld != null) {
            DebugConfig.WATCH_GAME_TIME = overworld.getGameTime();
        }

        // ---- Process cheat triggers for each player's nearest Trading Post ----
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TradingPostBlockEntity tpbe = findNearestTradingPost(player);
            if (tpbe == null) continue;

            // Update watch variables from the first found trading post
            DebugConfig.WATCH_ACTIVE_SHIPMENTS = tpbe.getActiveShipments().size();
            DebugConfig.WATCH_PENDING_COINS = tpbe.getPendingCoins();
            DebugConfig.WATCH_TRADER_LEVEL = tpbe.getTraderLevel();
            DebugConfig.WATCH_TRADER_XP = tpbe.getTraderXp();

            // ---- GRANT_COINS ----
            if (DebugConfig.GRANT_COINS > 0) {
                int amount = DebugConfig.GRANT_COINS;
                DebugConfig.GRANT_COINS = 0;
                // Add directly to pending to collect
                try {
                    java.lang.reflect.Field f = TradingPostBlockEntity.class.getDeclaredField("pendingCoins");
                    f.setAccessible(true);
                    f.setInt(tpbe, tpbe.getPendingCoins() + amount);
                    tpbe.syncToClient();
                    DebugConfig.WATCH_LAST_EVENT = "Granted " + amount + " CP to pending coins";
                    log("Granted " + amount + " CP to pending coins");
                } catch (Exception e) {
                    DebugConfig.WATCH_LAST_EVENT = "GRANT_COINS failed: " + e.getMessage();
                }
            }

            // ---- GRANT_XP ----
            if (DebugConfig.GRANT_XP > 0) {
                int amount = DebugConfig.GRANT_XP;
                DebugConfig.GRANT_XP = 0;
                tpbe.addTraderXp(amount);
                DebugConfig.WATCH_LAST_EVENT = "Granted " + amount + " XP";
                log("Granted " + amount + " XP");
            }

            // ---- SET_TRADER_LEVEL ----
            if (DebugConfig.SET_TRADER_LEVEL > 0) {
                int lvl = DebugConfig.SET_TRADER_LEVEL;
                DebugConfig.SET_TRADER_LEVEL = 0;
                try {
                    java.lang.reflect.Field fLvl = TradingPostBlockEntity.class.getDeclaredField("traderLevel");
                    fLvl.setAccessible(true);
                    fLvl.setInt(tpbe, lvl);
                    java.lang.reflect.Field fXp = TradingPostBlockEntity.class.getDeclaredField("traderXp");
                    fXp.setAccessible(true);
                    fXp.setInt(tpbe, 0);
                    tpbe.syncToClient();
                    DebugConfig.WATCH_LAST_EVENT = "Set trader level to " + lvl;
                    log("Set trader level to " + lvl);
                } catch (Exception e) {
                    DebugConfig.WATCH_LAST_EVENT = "SET_LEVEL failed: " + e.getMessage();
                }
            }

            // ---- INSTANT_DELIVERY ----
            if (DebugConfig.INSTANT_DELIVERY) {
                DebugConfig.INSTANT_DELIVERY = false;
                long gameTime = overworld != null ? overworld.getGameTime() : 0;
                int count = 0;
                for (Shipment s : tpbe.getActiveShipments()) {
                    if (s.getStatus() == Shipment.Status.IN_TRANSIT) {
                        s.setStatus(Shipment.Status.AT_MARKET);
                        s.setMarketListedTime(gameTime);
                        count++;
                    }
                }
                if (count > 0) {
                    tpbe.syncToClient();
                    DebugConfig.WATCH_LAST_EVENT = "Instant delivery: " + count + " shipments arrived";
                    log("Instant delivery: " + count + " shipments arrived");
                }
            }

            // ---- INSTANT_SELL ----
            if (DebugConfig.INSTANT_SELL) {
                DebugConfig.INSTANT_SELL = false;
                long gameTime = overworld != null ? overworld.getGameTime() : 0;
                int count = 0;
                for (Shipment s : tpbe.getActiveShipments()) {
                    if (s.getStatus() == Shipment.Status.AT_MARKET) {
                        int earnings = 0;
                        for (Shipment.ShipmentItem item : s.getItems()) {
                            if (!item.isSold()) {
                                item.setSold(true);
                                earnings += item.getTotalPrice();
                            }
                        }
                        s.setTotalEarnings(s.getTotalEarnings() + earnings);
                        s.setSoldTime(gameTime);
                        s.setStatus(Shipment.Status.SOLD);
                        count++;
                    }
                }
                if (count > 0) {
                    tpbe.syncToClient();
                    DebugConfig.WATCH_LAST_EVENT = "Instant sell: " + count + " shipments sold";
                    log("Instant sell: " + count + " shipments sold");
                }
            }

            // Verbose tick log
            if (DebugConfig.VERBOSE_LOGGING && tickCount % 100 == 0) {
                log("[Tick " + tickCount + "] Lvl=" + tpbe.getTraderLevel()
                        + " XP=" + tpbe.getTraderXp()
                        + " Coins=" + tpbe.getPendingCoins()
                        + " Shipments=" + tpbe.getActiveShipments().size()
                        + " TPS=" + DebugConfig.WATCH_SERVER_TPS);
            }

            break; // Only process first player's nearest post for debug
        }
    }

    /**
     * Find the nearest Trading Post within 32 blocks of the player.
     */
    private static TradingPostBlockEntity findNearestTradingPost(ServerPlayer player) {
        ServerLevel level = player.getLevel();
        BlockPos center = player.blockPosition();
        int radius = 32;

        TradingPostBlockEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TradingPostBlockEntity tpbe) {
                double dist = center.distSqr(pos);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = tpbe;
                }
            }
        }

        return nearest;
    }

    private static void log(String msg) {
        OffToMarket.LOGGER.info("[OTM-Debug] {}", msg);
    }
}
