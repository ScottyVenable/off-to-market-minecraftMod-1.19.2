package com.offtomarket.mod.item;

import com.offtomarket.mod.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Animal Trade Slip - Used to capture and transport animals.
 * When empty, right-clicking an animal captures it onto the slip.
 * When filled, right-clicking on ground spawns the animal.
 */
public class AnimalTradeSlipItem extends Item {
    public static final String TAG_ANIMAL_TYPE = "AnimalType";
    public static final String TAG_ANIMAL_NAME = "AnimalName";
    public static final String TAG_ANIMAL_DATA = "AnimalData";
    public static final String TAG_BASE_VALUE = "BaseValue";
    public static final String TAG_IS_FILLED = "IsFilled";

    // Base values for different animals (in copper pieces)
    private static final Map<String, Integer> ANIMAL_BASE_VALUES = new HashMap<>();
    
    static {
        // Common farm animals
        ANIMAL_BASE_VALUES.put("minecraft:chicken", 50);
        ANIMAL_BASE_VALUES.put("minecraft:pig", 150);
        ANIMAL_BASE_VALUES.put("minecraft:sheep", 120);
        ANIMAL_BASE_VALUES.put("minecraft:cow", 200);
        ANIMAL_BASE_VALUES.put("minecraft:mooshroom", 500);
        ANIMAL_BASE_VALUES.put("minecraft:rabbit", 80);
        ANIMAL_BASE_VALUES.put("minecraft:goat", 180);
        
        // Horses and mounts
        ANIMAL_BASE_VALUES.put("minecraft:horse", 800);
        ANIMAL_BASE_VALUES.put("minecraft:donkey", 400);
        ANIMAL_BASE_VALUES.put("minecraft:mule", 600);
        ANIMAL_BASE_VALUES.put("minecraft:llama", 350);
        ANIMAL_BASE_VALUES.put("minecraft:trader_llama", 450);
        ANIMAL_BASE_VALUES.put("minecraft:camel", 1000);
        
        // Pets and companions
        ANIMAL_BASE_VALUES.put("minecraft:cat", 300);
        ANIMAL_BASE_VALUES.put("minecraft:wolf", 400);
        ANIMAL_BASE_VALUES.put("minecraft:parrot", 600);
        ANIMAL_BASE_VALUES.put("minecraft:fox", 350);
        ANIMAL_BASE_VALUES.put("minecraft:ocelot", 250);
        
        // Aquatic animals
        ANIMAL_BASE_VALUES.put("minecraft:axolotl", 700);
        ANIMAL_BASE_VALUES.put("minecraft:turtle", 400);
        ANIMAL_BASE_VALUES.put("minecraft:frog", 200);
        ANIMAL_BASE_VALUES.put("minecraft:tadpole", 50);
        
        // Bees
        ANIMAL_BASE_VALUES.put("minecraft:bee", 300);
        
        // Exotic/rare
        ANIMAL_BASE_VALUES.put("minecraft:panda", 1200);
        ANIMAL_BASE_VALUES.put("minecraft:polar_bear", 800);
        ANIMAL_BASE_VALUES.put("minecraft:sniffer", 1500);
        ANIMAL_BASE_VALUES.put("minecraft:allay", 2000);
    }

    // Set of valid animal types that can be captured
    private static final Set<String> VALID_ANIMALS = ANIMAL_BASE_VALUES.keySet();

    public AnimalTradeSlipItem(Properties props) {
        super(props);
    }

    /**
     * Check if this slip is filled with an animal.
     */
    public static boolean isFilled(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_IS_FILLED);
    }

    /**
     * Create a filled animal trade slip for a specific animal type.
     * Used by the market to sell animals.
     */
    public static ItemStack createFilledSlip(String animalType, @Nullable String customName) {
        ItemStack slip = new ItemStack(ModItems.ANIMAL_TRADE_SLIP.get());
        CompoundTag tag = slip.getOrCreateTag();
        
        tag.putBoolean(TAG_IS_FILLED, true);
        tag.putString(TAG_ANIMAL_TYPE, animalType);
        tag.putInt(TAG_BASE_VALUE, getBaseValue(animalType));
        
        if (customName != null && !customName.isEmpty()) {
            tag.putString(TAG_ANIMAL_NAME, customName);
        }
        
        return slip;
    }

    /**
     * Get the base value for an animal type.
     */
    public static int getBaseValue(String animalType) {
        return ANIMAL_BASE_VALUES.getOrDefault(animalType, 100);
    }

    /**
     * Check if an entity can be captured.
     */
    public static boolean canCapture(Entity entity) {
        if (entity == null) return false;
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && VALID_ANIMALS.contains(key.toString());
    }

    /**
     * Get display name for an animal type.
     */
    public static String getAnimalDisplayName(String animalType) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(animalType));
        if (type != null) {
            return type.getDescription().getString();
        }
        // Fallback: extract name from resource location
        String name = animalType.contains(":") ? animalType.split(":")[1] : animalType;
        return name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ");
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level.isClientSide()) {
            return InteractionResult.PASS;
        }

        // Only capture if slip is empty
        if (isFilled(stack)) {
            player.displayClientMessage(
                    Component.literal("This slip already contains an animal!").withStyle(ChatFormatting.RED),
                    true);
            return InteractionResult.FAIL;
        }

        // Check if entity can be captured
        if (!canCapture(target)) {
            player.displayClientMessage(
                    Component.literal("This animal cannot be captured!").withStyle(ChatFormatting.RED),
                    true);
            return InteractionResult.FAIL;
        }

        // Capture the animal
        ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (entityKey == null) return InteractionResult.FAIL;

        String animalType = entityKey.toString();

        ItemStack filledSlip = new ItemStack(ModItems.ANIMAL_TRADE_SLIP.get());
        CompoundTag tag = filledSlip.getOrCreateTag();
        
        tag.putBoolean(TAG_IS_FILLED, true);
        tag.putString(TAG_ANIMAL_TYPE, animalType);
        tag.putInt(TAG_BASE_VALUE, getBaseValue(animalType));
        
        // Save custom name if present
        if (target.hasCustomName()) {
            tag.putString(TAG_ANIMAL_NAME, target.getCustomName().getString());
        }
        
        // Save full entity data for restoration
        CompoundTag entityData = new CompoundTag();
        target.save(entityData);
        // Remove position data - we'll set new position on spawn
        entityData.remove("Pos");
        entityData.remove("Motion");
        entityData.remove("Rotation");
        tag.put(TAG_ANIMAL_DATA, entityData);
        
        // Remove the entity from the world
        target.discard();

        // Consume exactly one empty slip and replace/give one filled slip.
        // This avoids mutating the NBT of the whole stack in-hand.
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            if (!player.getInventory().add(filledSlip)) {
                player.drop(filledSlip, false);
            }
        }
        
        player.displayClientMessage(
                Component.literal("Captured " + getAnimalDisplayName(animalType) + "!")
                        .withStyle(ChatFormatting.GREEN),
                true);
        
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        
        if (player == null) return InteractionResult.FAIL;
        
        // Only spawn if slip is filled
        if (!isFilled(stack)) {
            return InteractionResult.PASS;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null) return InteractionResult.FAIL;

        String animalType = tag.getString(TAG_ANIMAL_TYPE);
        if (animalType.isEmpty()) return InteractionResult.FAIL;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Vec3 spawnPos = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        // Spawn the animal
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(animalType));
        if (entityType == null) {
            player.displayClientMessage(
                    Component.literal("Invalid animal type!").withStyle(ChatFormatting.RED),
                    true);
            return InteractionResult.FAIL;
        }

        Entity entity = entityType.create(level);
        if (entity == null) {
            player.displayClientMessage(
                    Component.literal("Failed to create animal!").withStyle(ChatFormatting.RED),
                    true);
            return InteractionResult.FAIL;
        }

        // Load saved entity data if present
        if (tag.contains(TAG_ANIMAL_DATA)) {
            CompoundTag entityData = tag.getCompound(TAG_ANIMAL_DATA);
            entity.load(entityData);
        }

        // Set position
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        
        // Set custom name if present
        if (tag.contains(TAG_ANIMAL_NAME)) {
            entity.setCustomName(Component.literal(tag.getString(TAG_ANIMAL_NAME)));
        }

        // Add to world
        level.addFreshEntity(entity);
        
        player.displayClientMessage(
                Component.literal("Released " + getAnimalDisplayName(animalType) + "!")
                        .withStyle(ChatFormatting.GREEN),
                true);

        // Consume exactly one filled slip
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isFilled(stack)) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                String animalType = tag.getString(TAG_ANIMAL_TYPE);
                String displayName = getAnimalDisplayName(animalType);
                int baseValue = tag.getInt(TAG_BASE_VALUE);
                
                // Animal type
                tooltip.add(Component.literal("Contains: " + displayName)
                        .withStyle(ChatFormatting.AQUA));
                
                // Custom name if present
                if (tag.contains(TAG_ANIMAL_NAME)) {
                    tooltip.add(Component.literal("Name: \"" + tag.getString(TAG_ANIMAL_NAME) + "\"")
                            .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
                }
                
                // Base value
                tooltip.add(Component.literal("Base Value: " + formatValue(baseValue))
                        .withStyle(ChatFormatting.GOLD));
                
                tooltip.add(Component.literal(""));
                tooltip.add(Component.literal("Right-click ground to release")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        } else {
            tooltip.add(Component.literal("Empty Animal Slip")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("Right-click an animal to capture")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isFilled(stack)) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(TAG_ANIMAL_TYPE)) {
                String displayName = getAnimalDisplayName(tag.getString(TAG_ANIMAL_TYPE));
                return Component.literal("Animal Slip - " + displayName);
            }
        }
        return Component.literal("Animal Trade Slip");
    }

    /**
     * Format value as coin text (e.g., "1g 50s" or "50c")
     */
    private static String formatValue(int copper) {
        int gold = copper / 100;
        int silver = (copper % 100) / 10;
        int copperRem = copper % 10;
        
        StringBuilder sb = new StringBuilder();
        if (gold > 0) {
            sb.append(gold).append("g ");
        }
        if (silver > 0 || gold > 0) {
            sb.append(silver).append("s ");
        }
        sb.append(copperRem).append("c");
        
        return sb.toString().trim();
    }

    /**
     * Get the list of all tradeable animal types.
     */
    public static Set<String> getTradeableAnimals() {
        return Collections.unmodifiableSet(VALID_ANIMALS);
    }

    /**
     * Get the animal base values map.
     */
    public static Map<String, Integer> getAnimalBaseValues() {
        return Collections.unmodifiableMap(ANIMAL_BASE_VALUES);
    }
}
