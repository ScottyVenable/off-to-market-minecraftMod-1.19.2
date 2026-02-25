package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.network.CoinExchangePacket;
import com.offtomarket.mod.network.CoinExchangePacket.ConversionType;
import com.offtomarket.mod.network.ModNetwork;
import com.offtomarket.mod.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

/**
 * Client-side screen for converting coins between denominations using the Ledger.
 */
public class CoinExchangeScreen extends Screen {

    private static final int GUI_WIDTH = 180;
    private static final int GUI_HEIGHT = 140;

    public CoinExchangeScreen() {
        super(Component.literal("Coin Exchange"));
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        int btnW = 160;
        int btnH = 18;
        int bx = x + (GUI_WIDTH - btnW) / 2;
        int startY = y + 52;
        int spacing = 22;

        for (int i = 0; i < ConversionType.values().length; i++) {
            final ConversionType type = ConversionType.values()[i];
            addRenderableWidget(new Button(bx, startY + i * spacing, btnW, btnH,
                    Component.literal(type.getLabel()), btn -> {
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                        new CoinExchangePacket(type));
            }));
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);

        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        // Draw outer frame
        OtmGuiTheme.drawPanel(poseStack, x, y, GUI_WIDTH, GUI_HEIGHT);

        // Title bar inset
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 3, GUI_WIDTH - 8, 14);

        drawCenteredString(poseStack, this.font, "Coin Exchange", x + GUI_WIDTH / 2, y + 6, OtmGuiTheme.TEXT_TITLE);

        // Coin counts from player inventory
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            Inventory inv = mc.player.getInventory();
            int gold = countItem(inv, ModItems.GOLD_COIN.get());
            int silver = countItem(inv, ModItems.SILVER_COIN.get());
            int copper = countItem(inv, ModItems.COPPER_COIN.get());

            // Coin count area inset
            OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 19, GUI_WIDTH - 8, 28);

            int row1Y = y + 23;
            int row2Y = y + 34;

            // Gold
            this.font.draw(poseStack, "\u2B50 Gold:", x + 10, row1Y, 0xFFD700);
            String goldStr = String.valueOf(gold);
            this.font.draw(poseStack, goldStr, x + 60, row1Y, 0xFFFFFF);

            // Silver
            this.font.draw(poseStack, "\u25CB Silver:", x + 10, row2Y, 0xC0C0C0);
            String silverStr = String.valueOf(silver);
            this.font.draw(poseStack, silverStr, x + 60, row2Y, 0xFFFFFF);

            // Copper (right column)
            this.font.draw(poseStack, "\u25CF Copper:", x + 95, row1Y, 0xCD7F32);
            String copperStr = String.valueOf(copper);
            this.font.draw(poseStack, copperStr, x + 148, row1Y, 0xFFFFFF);

            // Total value
            int totalCP = gold * 100 + silver * 10 + copper;
            String totalStr = "Total: " + com.offtomarket.mod.item.CoinType.formatValue(totalCP);
            int totalW = this.font.width(totalStr);
            this.font.draw(poseStack, totalStr, x + 95 + (GUI_WIDTH - 95 - 10 - totalW) / 2.0f + 10, row2Y, 0xAAAAAA);
        }

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private int countItem(Inventory inv, Item item) {
        int count = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
