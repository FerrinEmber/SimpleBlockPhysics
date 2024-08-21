package net.ferrinember.simpleblockphysics.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.*;

public class TickHandler {
    public static List<BlockPos> checkList = new ArrayList();
    public static List<LevelAccessor> levelList = new ArrayList();
    public static List<Integer> yList = new ArrayList();
    public static List<FallingBlockEntity> fallingList = new ArrayList();

    public TickHandler() {
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        boolean destroyed = false;

        while(!destroyed && checkList != null && !checkList.isEmpty() && event.phase.equals(TickEvent.Phase.START) && event.side.equals(LogicalSide.SERVER)) {
            int index = yList.indexOf(Collections.min(yList));
            destroyed = this.checkBreak((BlockPos)checkList.get(index), (LevelAccessor)levelList.get(index));
            yList.remove(index);
            checkList.remove(index);
            levelList.remove(index);
        }

    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!event.level.isClientSide() && event.level.getDayTime() % 20L == 17L && event.level.getServer() != null) {
            Iterator var2 = event.level.getServer().getPlayerList().getPlayers().iterator();

            while(var2.hasNext()) {
                ServerPlayer player = (ServerPlayer)var2.next();
                BlockPos blockPos = player.getOnPos();
                if (event.level.dimension().equals(player.level().dimension()) && !event.level.getBlockState(blockPos).isAir() && !event.level.getBlockState(blockPos).is(Blocks.BEDROCK) && !checkList.contains(blockPos)) {
                    yList.add(blockPos.getY());
                    checkList.add(blockPos.immutable());
                    levelList.add(event.level);
                }
            }
        }

        if (!event.level.isClientSide() && event.level.getServer() != null) {
            Iterator<FallingBlockEntity> fallingBlocks = fallingList.iterator();

            while(fallingBlocks.hasNext()) {
                FallingBlockEntity fallingBlockEntity = fallingBlocks.next();
                if (fallingBlockEntity.onGround()) {
                    fallingBlockEntity.getServer().sendSystemMessage(Component.literal("block has landed"));
                    //fallingBlockEntity.setPos(fallingBlockEntity.position().add(1,0,1));
                    //FallingBlockEntity slideFallingBlock = FallingBlockEntity.fall(event.level, fallingBlockEntity.blockPosition(), event.level.getBlockState(fallingBlockEntity.blockPosition()));
                    //slideFallingBlock.setPos(slideFallingBlock.position().add(1,1,1));
                    //slideFallingBlock.setDeltaMovement(1,1,1);
                    int randDir = event.level.getRandom().nextIntBetweenInclusive(1,4);
                    Vec3i randVec = switch (randDir) {
                        case 1 -> new Vec3i(0, 0, 1);
                        case 2 -> new Vec3i(1, 0, 0);
                        case 3 -> new Vec3i(0, 0, -1);
                        case 4 -> new Vec3i(-1, 0, 0);
                        default -> null;
                    };
                    if (randVec != null && !event.level.getBlockState(fallingBlockEntity.blockPosition().offset(randVec)).blocksMotion() && !event.level.getBlockState(fallingBlockEntity.blockPosition().offset(randVec.offset(0,-1,0))).blocksMotion()) {
                        event.level.setBlockAndUpdate((fallingBlockEntity.blockPosition().offset(randVec)),fallingBlockEntity.getBlockState());
                        event.level.destroyBlock((fallingBlockEntity.blockPosition()),false);
                    }
                    fallingBlocks.remove();
                }
            }
        }

    }

    public boolean checkBreak(BlockPos adjPos, LevelAccessor level) {
        List<BlockPos> supportList = new ArrayList();
        List<BlockPos> newlyAddedList = new ArrayList();
        List<BlockPos> freshList = new ArrayList();
        newlyAddedList.add(adjPos);

        for(int i = 1; i <= 4; ++i) {
            Iterator var7 = newlyAddedList.iterator();

            label44:
            while(var7.hasNext()) {
                BlockPos supPos = (BlockPos)var7.next();
                Iterator var9 = BlockPos.betweenClosed(supPos.offset(-1, -1, -1), supPos.offset(1, 1, 1)).iterator();

                while(true) {
                    BlockPos adjSupPos;
                    do {
                        do {
                            if (!var9.hasNext()) {
                                continue label44;
                            }

                            adjSupPos = (BlockPos)var9.next();
                        } while(adjSupPos.distManhattan(supPos) != 1);
                    } while(!level.getBlockState(adjSupPos).blocksMotion() && !level.getBlockState(adjSupPos).liquid());

                    if (!supportList.contains(adjSupPos)) {
                        if (adjSupPos.getY() < adjPos.getY()) {
                            return false;
                        }

                        supportList.add(adjSupPos.immutable());
                        freshList.add(adjSupPos.immutable());
                    }
                }
            }

            newlyAddedList.clear();
            newlyAddedList.addAll(freshList);
            freshList.clear();
        }

        FallingBlockEntity fallingBlockEntity = FallingBlockEntity.fall((Level) level, adjPos, level.getBlockState(adjPos));
        fallingBlockEntity.setHurtsEntities(1,10);
        fallingList.add(fallingBlockEntity);
        return true;
    }
}