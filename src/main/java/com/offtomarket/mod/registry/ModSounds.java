package com.offtomarket.mod.registry;

import com.offtomarket.mod.OffToMarket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Custom sound effects for the trading mod.
 * Uses vanilla sounds as placeholders - can be replaced with custom sounds later.
 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, OffToMarket.MODID);

    // Coin-related sounds
    public static final RegistryObject<SoundEvent> COINS_COLLECT = registerSound("coins_collect");
    public static final RegistryObject<SoundEvent> COINS_JINGLE = registerSound("coins_jingle");
    
    // Trading sounds
    public static final RegistryObject<SoundEvent> SHIPMENT_SEND = registerSound("shipment_send");
    public static final RegistryObject<SoundEvent> SHIPMENT_ARRIVE = registerSound("shipment_arrive");
    public static final RegistryObject<SoundEvent> ITEM_SOLD = registerSound("item_sold");
    
    // Quest sounds
    public static final RegistryObject<SoundEvent> QUEST_ACCEPT = registerSound("quest_accept");
    public static final RegistryObject<SoundEvent> QUEST_COMPLETE = registerSound("quest_complete");
    
    // Worker sounds
    public static final RegistryObject<SoundEvent> WORKER_HIRE = registerSound("worker_hire");
    public static final RegistryObject<SoundEvent> WORKER_LEVEL_UP = registerSound("worker_level_up");
    
    // Diplomat sounds
    public static final RegistryObject<SoundEvent> DIPLOMAT_SEND = registerSound("diplomat_send");
    public static final RegistryObject<SoundEvent> DIPLOMAT_RETURN = registerSound("diplomat_return");
    public static final RegistryObject<SoundEvent> DIPLOMAT_PROPOSAL = registerSound("diplomat_proposal");
    
    // UI sounds
    public static final RegistryObject<SoundEvent> UI_CLICK = registerSound("ui_click");
    public static final RegistryObject<SoundEvent> UI_TAB_SWITCH = registerSound("ui_tab_switch");
    public static final RegistryObject<SoundEvent> NOTIFICATION = registerSound("notification");
    
    // Level up
    public static final RegistryObject<SoundEvent> TRADER_LEVEL_UP = registerSound("trader_level_up");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        ResourceLocation loc = new ResourceLocation(OffToMarket.MODID, name);
        return SOUNDS.register(name, () -> new SoundEvent(loc));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
