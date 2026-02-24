package com.offtomarket.mod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

/**
 * Helper class for playing sounds in the trading mod.
 * Uses vanilla sounds as placeholders until custom sounds are added.
 */
public class SoundHelper {

    // ==================== Client-side UI sounds ====================

    /**
     * Play a click sound on the client (for UI interactions).
     */
    public static void playUIClick() {
        playClientSound(SoundEvents.UI_BUTTON_CLICK);
    }

    /**
     * Play a tab switch sound on the client.
     */
    public static void playTabSwitch() {
        playClientSound(SoundEvents.BOOK_PAGE_TURN, 1.0f, 1.2f);
    }

    /**
     * Play a notification sound on the client.
     */
    public static void playNotification() {
        playClientSound(SoundEvents.NOTE_BLOCK_BELL, 0.5f, 1.5f);
    }

    // ==================== Trading sounds (world) ====================

    /**
     * Play coin collection sound at the player's location.
     */
    public static void playCoinCollect(Level level, Player player) {
        if (level.isClientSide) {
            // Chain multiple coin sounds for a jingling effect
            playClientSound(SoundEvents.CHAIN_STEP, 0.3f, 1.8f);
            playClientSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.4f, 1.5f);
        }
    }

    /**
     * Play coin jingle sound (smaller amount).
     */
    public static void playCoinJingle(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.5f, 1.2f);
    }

    /**
     * Play shipment departure sound.
     */
    public static void playShipmentSend(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.8f, 0.9f);
        level.playSound(null, pos, SoundEvents.HORSE_ARMOR, SoundSource.BLOCKS, 0.5f, 0.8f);
    }

    /**
     * Play shipment arrival sound.
     */
    public static void playShipmentArrive(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.8f, 1.0f);
        level.playSound(null, pos, SoundEvents.VILLAGER_CELEBRATE, SoundSource.BLOCKS, 0.3f, 1.0f);
    }

    /**
     * Play item sold sound.
     */
    public static void playItemSold(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.3f, 1.2f);
    }

    // ==================== Quest sounds ====================

    /**
     * Play quest accepted sound.
     */
    public static void playQuestAccept(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.0f);
        level.playSound(null, pos, SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 0.4f, 1.0f);
    }

    /**
     * Play quest completed sound.
     */
    public static void playQuestComplete(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5f, 1.5f);
        level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.6f, 1.2f);
    }

    // ==================== Worker sounds ====================

    /**
     * Play worker hired sound.
     */
    public static void playWorkerHire(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.VILLAGER_TRADE, SoundSource.BLOCKS, 0.7f, 1.0f);
    }

    /**
     * Play worker level up sound.
     */
    public static void playWorkerLevelUp(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.4f, 1.3f);
    }

    // ==================== Diplomat sounds ====================

    /**
     * Play diplomat send sound.
     */
    public static void playDiplomatSend(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.HORSE_SADDLE, SoundSource.BLOCKS, 0.8f, 1.0f);
    }

    /**
     * Play diplomat returned sound.
     */
    public static void playDiplomatReturn(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.DONKEY_CHEST, SoundSource.BLOCKS, 0.8f, 1.0f);
    }

    /**
     * Play diplomat proposal ready sound.
     */
    public static void playDiplomatProposal(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f, 1.0f);
    }

    // ==================== Level up sounds ====================

    /**
     * Play trader level up sound.
     */
    public static void playTraderLevelUp(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 0.5f, 1.2f);
    }

    // ==================== Internal helpers ====================

    /**
     * Play a sound on the client only.
     */
    private static void playClientSound(SoundEvent sound) {
        playClientSound(sound, 1.0f, 1.0f);
    }

    /**
     * Play a sound on the client with volume and pitch.
     */
    private static void playClientSound(SoundEvent sound, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }
}
