package com.offtomarket.mod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.data.TownData;
import com.offtomarket.mod.data.TownRegistry;
import com.offtomarket.mod.registry.ModItems;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Renders glowing color-coded 3D bounding boxes around designated containers
 * in the world when holding the Trade Route Marker tool.
 */
@Mod.EventBusSubscriber(modid = OffToMarket.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RouteMarkerRenderer {

    private static final double MAX_RENDER_DIST_SQ = 32.0 * 32.0;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        boolean holdingMarker = player.getMainHandItem().is(ModItems.ROUTE_MARKER.get())
                || player.getOffhandItem().is(ModItems.ROUTE_MARKER.get());
        if (!holdingMarker) return;

        Map<BlockPos, String> depots = ClientDepotTracker.getAssignedDepots();
        if (depots.isEmpty()) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer linesConsumer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        for (Map.Entry<BlockPos, String> entry : depots.entrySet()) {
            BlockPos pos = entry.getKey();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_RENDER_DIST_SQ) {
                continue;
            }

            String townId = entry.getValue();
            float r = 0.4f, g = 0.85f, b = 0.4f, a = 0.95f; // default bright green

            if (townId.contains("frost") || townId.contains("mine")) {
                r = 0.4f; g = 0.7f; b = 1.0f; // frosty cyan
            } else if (townId.contains("arcane") || townId.contains("spell") || townId.contains("twilight")) {
                r = 0.85f; g = 0.4f; b = 1.0f; // mystic purple
            } else if (townId.contains("sunken") || townId.contains("aqua") || townId.contains("fish")) {
                r = 0.2f; g = 0.6f; b = 0.9f; // ocean blue
            } else if (townId.contains("cataclysm") || townId.contains("dungeon") || townId.contains("blade")) {
                r = 1.0f; g = 0.35f; b = 0.25f; // combat red
            } else if (townId.contains("peenam") || townId.contains("ranch")) {
                r = 1.0f; g = 0.75f; b = 0.2f; // amber/orange
            }

            AABB box = new AABB(pos).inflate(0.005); // slight expansion so lines don't z-fight
            LevelRenderer.renderLineBox(poseStack, linesConsumer, box, r, g, b, a);
        }

        poseStack.popPose();
    }
}
