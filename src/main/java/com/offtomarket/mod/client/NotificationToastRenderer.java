package com.offtomarket.mod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.OffToMarket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Handles rendering of notification toasts for trading events.
 * These appear at the top of the screen and slide in/out smoothly.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = OffToMarket.MODID, value = Dist.CLIENT)
public class NotificationToastRenderer {
    private static final ResourceLocation TOAST_TEXTURE = 
            new ResourceLocation("minecraft", "textures/gui/toasts.png");
    
    private static final int TOAST_WIDTH = 160;
    private static final int TOAST_HEIGHT = 32;
    private static final int MAX_VISIBLE = 3;
    private static final long DISPLAY_TIME_MS = 5000; // 5 seconds
    private static final long SLIDE_TIME_MS = 300;
    
    private static final Deque<Toast> activeToasts = new ArrayDeque<>();
    private static final Deque<Toast> queuedToasts = new ArrayDeque<>();
    
    /**
     * Toast data class.
     */
    public static class Toast {
        public final String title;
        public final String message;
        public final ToastType type;
        public final long startTime;
        
        public Toast(String title, String message, ToastType type) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.startTime = System.currentTimeMillis();
        }
        
        public float getProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.min(1.0f, elapsed / (float) DISPLAY_TIME_MS);
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > DISPLAY_TIME_MS;
        }
        
        public float getSlideProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed < SLIDE_TIME_MS) {
                // Slide in
                return elapsed / (float) SLIDE_TIME_MS;
            } else if (elapsed > DISPLAY_TIME_MS - SLIDE_TIME_MS) {
                // Slide out
                return (DISPLAY_TIME_MS - elapsed) / (float) SLIDE_TIME_MS;
            }
            return 1.0f;
        }
    }
    
    /**
     * Toast types with different colors.
     */
    public enum ToastType {
        INFO(0x3399FF),        // Blue - general info
        SUCCESS(0x55FF55),     // Green - sales, completions
        WARNING(0xFFAA00),     // Orange - expiry warnings
        COINS(0xFFD700),       // Gold - earnings
        LEVEL_UP(0xFF55FF),    // Magenta - level ups
        DIPLOMAT(0xAA88FF);    // Purple - diplomat events
        
        public final int color;
        
        ToastType(int color) {
            this.color = color;
        }
    }
    
    /**
     * Add a new notification toast.
     */
    public static void addToast(String title, String message, ToastType type) {
        Toast toast = new Toast(title, message, type);
        if (activeToasts.size() < MAX_VISIBLE) {
            activeToasts.addLast(toast);
        } else {
            queuedToasts.addLast(toast);
        }
    }
    
    // Convenience methods for common toast types
    public static void showInfo(String title, String message) {
        addToast(title, message, ToastType.INFO);
    }
    
    public static void showSuccess(String title, String message) {
        addToast(title, message, ToastType.SUCCESS);
    }
    
    public static void showCoins(String title, String message) {
        addToast(title, message, ToastType.COINS);
    }
    
    public static void showLevelUp(String title, String message) {
        addToast(title, message, ToastType.LEVEL_UP);
    }
    
    public static void showDiplomat(String title, String message) {
        addToast(title, message, ToastType.DIPLOMAT);
    }
    
    public static void showWarning(String title, String message) {
        addToast(title, message, ToastType.WARNING);
    }
    
    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        // Only render on main HUD
        if (!event.getOverlay().id().equals(new ResourceLocation("minecraft", "hotbar"))) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return; // Don't render when GUI is open
        
        // Remove expired toasts and add queued ones
        while (!activeToasts.isEmpty() && activeToasts.peekFirst().isExpired()) {
            activeToasts.pollFirst();
            if (!queuedToasts.isEmpty()) {
                activeToasts.addLast(queuedToasts.pollFirst());
            }
        }
        
        if (activeToasts.isEmpty()) return;
        
        PoseStack poseStack = event.getPoseStack();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        
        int yOffset = 5;
        for (Toast toast : activeToasts) {
            renderToast(poseStack, mc, toast, screenWidth, yOffset);
            yOffset += TOAST_HEIGHT + 2;
        }
    }
    
    private static void renderToast(PoseStack poseStack, Minecraft mc, Toast toast, int screenWidth, int yOffset) {
        float slide = toast.getSlideProgress();
        int xOffset = (int) ((1.0f - slide) * (TOAST_WIDTH + 10));
        int x = screenWidth - TOAST_WIDTH - 5 + xOffset;
        int y = yOffset;
        
        poseStack.pushPose();
        
        // Background
        RenderSystem.setShaderTexture(0, TOAST_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Draw toast background (using vanilla toast texture slice)
        GuiComponent.blit(poseStack, x, y, 0, 0, TOAST_WIDTH, TOAST_HEIGHT, 256, 256);
        
        // Title with type color
        int titleColor = toast.type.color;
        mc.font.draw(poseStack, toast.title, x + 8, y + 7, titleColor);
        
        // Message in white/gray
        mc.font.draw(poseStack, toast.message, x + 8, y + 18, 0xCCCCCC);
        
        // Progress bar at bottom
        float progress = toast.getProgress();
        int barWidth = (int) ((TOAST_WIDTH - 10) * (1.0f - progress));
        GuiComponent.fill(poseStack, x + 5, y + TOAST_HEIGHT - 3, x + 5 + barWidth, y + TOAST_HEIGHT - 1, 
                (titleColor & 0xFFFFFF) | 0x88000000);
        
        poseStack.popPose();
    }
    
    /**
     * Clear all toasts (for testing/reset).
     */
    public static void clearAll() {
        activeToasts.clear();
        queuedToasts.clear();
    }
}
