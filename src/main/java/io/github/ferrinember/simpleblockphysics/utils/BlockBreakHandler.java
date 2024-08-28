package io.github.ferrinember.simpleblockphysics.utils;

import io.github.ferrinember.simpleblockphysics.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


public class BlockBreakHandler {
    public BlockBreakHandler() {
    }

    @SubscribeEvent
    public void checkSupportsFromBreak(BlockEvent.NeighborNotifyEvent event) {
        Level world = (Level) event.getLevel();
        if (!world.isClientSide() && event.getState().isAir() && !event.getState().blocksMotion() && world.dimension() != Level.END && world.getServer() != null) {
            BlockPos blockPos = event.getPos();
            for (BlockPos adjPos : BlockPos.betweenClosed(blockPos.offset(-1, -1, -1), blockPos.offset(1, 1, 1))) {
                if (adjPos.distManhattan(blockPos) >= 1 && world.getBlockState(adjPos).blocksMotion() && !world.getBlockState(adjPos).is(Blocks.PISTON_HEAD) && !world.getBlockState(adjPos).is(Blocks.MOVING_PISTON) && !Config.indestructibleBlocks.contains(world.getBlockState(adjPos).getBlock()) && !TickHandler.checkMap.containsKey(adjPos) && Config.allowedDimensions.contains(world.dimension())) {
                    TickHandler.checkMap.put(adjPos.immutable(),new Tuple<>(world,1));
                }
            }
        }
    }

    @SubscribeEvent
    public void checkSupportsFromPlace(BlockEvent.EntityPlaceEvent event) {
        Level world = (Level) event.getLevel();
        if (!world.isClientSide() && !TickHandler.checkMap.containsKey(event.getPos()) && world.getBlockState(event.getPos()).blocksMotion() && !Config.indestructibleBlocks.contains(world.getBlockState(event.getPos()).getBlock()) && Config.allowedDimensions.contains(world.dimension())) {
            BlockPos blockPos = event.getPos();
            TickHandler.checkMap.put(blockPos.immutable(),new Tuple<>(world,1));
        }

    }


}

