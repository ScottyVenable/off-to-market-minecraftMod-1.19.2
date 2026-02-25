package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.block.entity.TradingLedgerBlockEntity;
import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import com.offtomarket.mod.network.ModNetwork;
import com.offtomarket.mod.network.SendShipmentPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Master Ledger GUI — shows all nearby Trading Ledger bins and allows
 * shipping all of them to market from a single "Send to Market" button.
 */
public class MasterLedgerScreen extends Screen {

    private static final int WIN_W = 320;
    private static final int WIN_H = 220;
    private static final int SEARCH_RADIUS = 64;

    private int leftPos, topPos;
    private Button sendBtn;

    private final List<TradingLedgerBlockEntity> nearbyLedgers = new ArrayList<>();
    private BlockPos nearestPostPos = null;

    public MasterLedgerScreen() {
        super(Component.literal("Master Ledger"));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - WIN_W) / 2;
        this.topPos = (this.height - WIN_H) / 2;

        scanNearby();

        int x = leftPos;
        int y = topPos;

        sendBtn = addRenderableWidget(new Button(x + 8, y + WIN_H - 24, 150, 14,
                Component.literal("Send to Market"), btn -> doSend()));

        addRenderableWidget(new Button(x + WIN_W - 88, y + WIN_H - 24, 80, 14,
                Component.literal("Close"), btn -> onClose()));

        sendBtn.active = nearestPostPos != null && !nearbyLedgers.isEmpty();
    }

    private void scanNearby() {
        nearbyLedgers.clear();
        nearestPostPos = null;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) return;

        BlockPos playerPos = player.blockPosition();
        double nearestPostDist = Double.MAX_VALUE;

        int cxMin = (playerPos.getX() - SEARCH_RADIUS) >> 4;
        int cxMax = (playerPos.getX() + SEARCH_RADIUS) >> 4;
        int czMin = (playerPos.getZ() - SEARCH_RADIUS) >> 4;
        int czMax = (playerPos.getZ() + SEARCH_RADIUS) >> 4;

        for (int cx = cxMin; cx <= cxMax; cx++) {
            for (int cz = czMin; cz <= czMax; cz++) {
                for (Map.Entry<BlockPos, BlockEntity> entry :
                        level.getChunk(cx, cz).getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    BlockEntity be = entry.getValue();
                    double distSq = pos.distSqr(playerPos);
                    if (distSq > (double)(SEARCH_RADIUS * SEARCH_RADIUS)) continue;
                    if (be instanceof TradingLedgerBlockEntity lbe) {
                        nearbyLedgers.add(lbe);
                    } else if (be instanceof TradingPostBlockEntity) {
                        if (distSq < nearestPostDist) {
                            nearestPostDist = distSq;
                            nearestPostPos = pos;
                        }
                    }
                }
            }
        }
    }

    private void doSend() {
        if (nearestPostPos == null) return;
        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                new SendShipmentPacket(nearestPostPos));
        onClose();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);

        int x = leftPos;
        int y = topPos;

        OtmGuiTheme.drawPanel(poseStack, x, y, WIN_W, WIN_H);
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 3, WIN_W - 8, 14);                 // title bar
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 19, WIN_W - 8, WIN_H - 46);        // content
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + WIN_H - 26, WIN_W - 8, 20);        // button bar

        // Title
        drawCenteredString(poseStack, this.font, "\u00A7e\u00A7lMaster Ledger",
                x + WIN_W / 2, y + 6, 0xFFFFFF);

        int relContentY = 22;

        // Trading Post status line
        if (nearestPostPos != null) {
            Player player = Minecraft.getInstance().player;
            String distStr = player != null
                    ? " (" + (int) Math.sqrt(nearestPostPos.distSqr(player.blockPosition())) + " blk)"
                    : "";
            String postText = "Post: " + nearestPostPos.getX() + ", " + nearestPostPos.getY()
                    + ", " + nearestPostPos.getZ() + distStr;
            this.font.draw(poseStack, postText, x + 8, y + relContentY, 0x88CC88);
        } else {
            this.font.draw(poseStack, "\u00A7cNo Trading Post found within "
                    + SEARCH_RADIUS + " blocks.", x + 8, y + relContentY, 0xFF6666);
        }
        relContentY += 12;
        OtmGuiTheme.drawDividerH(poseStack, x + 4, x + WIN_W - 4, y + relContentY + 1);
        relContentY += 5;

        // Ledger bins list
        if (nearbyLedgers.isEmpty()) {
            drawCenteredString(poseStack, this.font,
                    "No Trading Ledger bins found nearby.", x + WIN_W / 2, y + 90, 0x666666);
        } else {
            this.font.draw(poseStack,
                    "Ledger Bins (" + nearbyLedgers.size() + " found):",
                    x + 8, y + relContentY, 0xCCCCCC);
            relContentY += 12;

            int totalItems = 0;
            long totalPayout = 0;
            int maxRows = Math.min(nearbyLedgers.size(), 8);

            for (int i = 0; i < maxRows; i++) {
                TradingLedgerBlockEntity lbe = nearbyLedgers.get(i);
                BlockPos pos = lbe.getBlockPos();

                int itemCount = 0;
                for (int s = 0; s < TradingLedgerBlockEntity.BIN_SIZE; s++) {
                    if (!lbe.getItem(s).isEmpty()) itemCount++;
                }
                long payout = lbe.getTotalProposedPayout();
                totalItems += itemCount;
                totalPayout += payout;

                int ry = y + relContentY + i * 13;
                // Position label
                String posStr = "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
                // Truncate if too wide
                while (this.font.width(posStr) > 110 && posStr.length() > 6)
                    posStr = posStr.substring(0, posStr.length() - 1);
                this.font.draw(poseStack, posStr, x + 8, ry, 0xAAAAAA);
                // Item count
                this.font.draw(poseStack, itemCount + " items", x + 130, ry, 0x88CC88);
                // Payout (right-aligned)
                String payStr = formatCoins((int) Math.min(payout, Integer.MAX_VALUE));
                int payW = this.font.width(payStr);
                this.font.draw(poseStack, payStr, x + WIN_W - payW - 8, ry, 0xCCAA66);
            }

            // Totals row
            int totRelY = relContentY + maxRows * 13 + 4;
            OtmGuiTheme.drawDividerH(poseStack, x + 4, x + WIN_W - 4, y + totRelY - 1);
            String totalStr = "Total: " + totalItems + " items   "
                    + formatCoins((int) Math.min(totalPayout, Integer.MAX_VALUE));
            this.font.draw(poseStack, totalStr, x + 8, y + totRelY + 2, 0xCCAA66);
        }

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private String formatCoins(int cp) {
        if (cp <= 0) return "0 cp";
        int gp = cp / 100;
        int sp = (cp % 100) / 10;
        int c = cp % 10;
        StringBuilder sb = new StringBuilder();
        if (gp > 0) sb.append(gp).append("g ");
        if (sp > 0) sb.append(sp).append("s ");
        if (c > 0 || sb.length() == 0) sb.append(c).append("c");
        return sb.toString().trim();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
