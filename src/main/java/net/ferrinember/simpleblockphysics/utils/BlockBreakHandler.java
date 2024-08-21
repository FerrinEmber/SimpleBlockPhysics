package net.ferrinember.simpleblockphysics.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;

public class BlockBreakHandler {
    public BlockBreakHandler() {
    }

    @SubscribeEvent
    public void checkSupportsFromBreak(BlockEvent.NeighborNotifyEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel().getBlockState(event.getPos()).isAir()) {
            BlockPos blockPos = event.getPos();
            Iterator var3 = BlockPos.betweenClosed(blockPos.offset(-1, -1, -1), blockPos.offset(1, 1, 1)).iterator();

            while(var3.hasNext()) {
                BlockPos adjPos = (BlockPos)var3.next();
                if (adjPos.distManhattan(blockPos) >= 1 && event.getLevel().getBlockState(adjPos).blocksMotion() && !event.getLevel().getBlockState(adjPos).is(Blocks.BEDROCK) && !TickHandler.checkList.contains(adjPos)) {
                    TickHandler.yList.add(adjPos.getY());
                    TickHandler.checkList.add(adjPos.immutable());
                    TickHandler.levelList.add(event.getLevel());
                }
            }
        }
    }

    @SubscribeEvent
    public void checkSupportsFromPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide() && !TickHandler.checkList.contains(event.getPos()) && !event.getLevel().getBlockState(event.getPos()).is(Blocks.BEDROCK)) {
            BlockPos blockPos = event.getPos();
            TickHandler.yList.add(blockPos.getY());
            TickHandler.checkList.add(blockPos.immutable());
            TickHandler.levelList.add(event.getLevel());
        }

    }
}

