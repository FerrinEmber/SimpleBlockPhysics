package io.github.ferrinember.simpleblockphysics.utils;

import io.github.ferrinember.simpleblockphysics.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


public class BlockBreakHandler {
    public static BlockState lastBroken;
    public BlockBreakHandler() {
    }

    @SubscribeEvent
    public void checkSupportsFromBreak(BlockEvent.NeighborNotifyEvent event) {
        Level world = (Level) event.getLevel();
        if (!world.isClientSide() && world.getBlockState(event.getPos()).isAir() && world.dimension() != Level.END && world.getServer() != null) {
            BlockPos blockPos = event.getPos();
            for (BlockPos adjPos : BlockPos.betweenClosed(blockPos.offset(-1, -1, -1), blockPos.offset(1, 1, 1))) {
                if (adjPos.distManhattan(blockPos) >= 1 && world.getBlockState(adjPos).blocksMotion() && !Config.indestructibleBlocks.contains(world.getBlockState(adjPos).getBlock()) && !TickHandler.checkMap.containsKey(adjPos) && world.dimension() != Level.END) {
                    TickHandler.checkMap.put(adjPos.immutable(),new Tuple<>(world,1));
                }
            }
        }
    }

    @SubscribeEvent
    public void checkSupportsFromPlace(BlockEvent.EntityPlaceEvent event) {
        Level world = (Level) event.getLevel();
        if (!world.isClientSide() && !TickHandler.checkMap.containsKey(event.getPos()) && world.getBlockState(event.getPos()).blocksMotion() && !Config.indestructibleBlocks.contains(world.getBlockState(event.getPos()).getBlock()) && world.dimension() != Level.END) {
            BlockPos blockPos = event.getPos();
            TickHandler.checkMap.put(blockPos.immutable(),new Tuple<>(world,1));
        }

    }


}

