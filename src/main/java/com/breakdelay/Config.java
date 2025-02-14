package com.breakdelay;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = BreakDelay.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();


    private static final ForgeConfigSpec.IntValue BREAKING_DELAY = BUILDER
            .comment("The delay in ticks before you can being breaking another block")
            .defineInRange("Break Delay", 500, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec SPEC = BUILDER.build();


    public static int breakingDelay;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        breakingDelay = BREAKING_DELAY.get();
    }
}
