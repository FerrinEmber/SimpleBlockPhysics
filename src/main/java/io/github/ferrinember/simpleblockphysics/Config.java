package io.github.ferrinember.simpleblockphysics;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = SimpleBlockPhysics.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_STRINGS = BUILDER
            .comment("A list of indestructible blocks.")
            .defineListAllowEmpty("ind_blocks", List.of("minecraft:bedrock", "minecraft:command_block", "minecraft:barrier", "minecraft:structure_block", "minecraft:structure_void", "minecraft:reinforced_deepslate", "minecraft:end_portal_frame"), Config::validateBlockName);

    private static final ForgeConfigSpec.DoubleValue BLOCK_BREAK_VOLUME = BUILDER
            .comment("Block Break Volume (caused by mod).")
            .defineInRange("blockBreakVolume",1,0,Double.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue SUPPORT_LENGTH_MAX = BUILDER
            .comment("Support Strength Max.")
            .defineInRange("supportLengthMax", 10, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue SUPPORT_LENGTH_MIN = BUILDER
            .comment("Support Strength Min.")
            .defineInRange("supportLengthMin", 1, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue DMG_DIST = BUILDER
            .comment("Base block fall damage per block fallen.")
            .defineInRange("dmgDist", 1, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue DMG_MAX = BUILDER
            .comment("Max block fall damage. Max is taken from support strength, but overwritten if greater than this value.")
            .defineInRange("dmgMax", 10, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue MAX_FALLING_BLOCK = BUILDER
            .comment("Max number of falling entities.")
            .defineInRange("maxFallingEntity", 5000, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue SUPPORT_SEARCH_ITER = BUILDER
            .comment("Max number of iterative supports to scan.")
            .defineInRange("supportSearchIter", 4, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> TAG_STRINGS = BUILDER
            .comment("A list of BlockTags to overwrite native (hardness based) support strength. Listed BlockTags without matching support index (below) will use default values.")
            .defineListAllowEmpty("overwriteBlockTags", List.of("leaves"), Config::validateTagName);

    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> TAG_INTS = BUILDER
            .comment("A list of support strength values to override blocktag hardness by, matched by index to above list. ")
            .defineListAllowEmpty("overwriteTagValues", List.of(4), e -> e instanceof Integer);


    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static Set<Block> indestructibleBlocks;
    public static Double blockBreakVolume;
    public static Integer supportLengthMax;
    public static Integer supportLengthMin;
    public static Integer dmgDist;
    public static Integer dmgMax;
    public static Integer maxFallingEntity;
    public static Integer supportSearchIter;
    public static List<TagKey<Block>> overwrittenBlockTags;
    public static List<Integer> overwrittenTagValues;
    public static HashMap<TagKey<Block>,Integer> overwrittenTagMap;

    private static boolean validateBlockName(final Object obj)
    {
        return obj instanceof final String blockName && ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(blockName));
    }

    private static boolean validateTagName(final Object obj)
    {
        return obj instanceof final String tagName && ForgeRegistries.BLOCKS.tags().isKnownTagName(ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(tagName)));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        // convert the list of strings into a set of blocks
        indestructibleBlocks = BLOCK_STRINGS.get().stream()
                .map(blockName -> ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockName)))
                .collect(Collectors.toSet());
        blockBreakVolume = BLOCK_BREAK_VOLUME.get();
        supportLengthMax = SUPPORT_LENGTH_MAX.get();
        supportLengthMin = SUPPORT_LENGTH_MIN.get();
        dmgDist = DMG_DIST.get();
        dmgMax = DMG_MAX.get();
        maxFallingEntity = MAX_FALLING_BLOCK.get();
        supportSearchIter = SUPPORT_SEARCH_ITER.get();
        overwrittenBlockTags = TAG_STRINGS.get().stream()
                .map(tagName -> ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(tagName)))
                .collect(Collectors.toList());
        overwrittenTagValues = new ArrayList<>(TAG_INTS.get());
        overwrittenTagMap = new HashMap<>();
        overwrittenBlockTags.forEach(blockTagKey -> {
            if (overwrittenTagValues.isEmpty()) {
                overwrittenTagMap.put(blockTagKey,-1);
            }
            else {
                overwrittenTagMap.put(blockTagKey,overwrittenTagValues.remove(0));
            }
        });



    }
}
