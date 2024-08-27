package io.github.ferrinember.simpleblockphysics.utils;

import io.github.ferrinember.simpleblockphysics.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TickHandler {
    public static HashSet<FallingBlockEntity> fallingSet = new HashSet<>();
    public static LinkedHashMap<BlockPos, Tuple<LevelAccessor,Integer>> checkMap = new LinkedHashMap<>();

    public TickHandler() {
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        boolean delay = false;

        while(!delay && checkMap != null && !checkMap.isEmpty() && event.phase.equals(TickEvent.Phase.START) && event.side.equals(LogicalSide.SERVER)) {
            Map.Entry<BlockPos, Tuple<LevelAccessor, Integer>> checkVal = checkMap.entrySet().iterator().next();
            checkMap.remove(checkVal.getKey());
            BlockPos checkBlock = checkVal.getKey();
            LevelAccessor checkLevel = checkVal.getValue().getA();
            Integer checkGen = checkVal.getValue().getB();
            if (this.checkBreak(checkBlock, checkLevel, checkGen)) {
                if (fallingSet.size() > Config.maxFallingEntity) {
                    delay = true;
                }
            }
        }

    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {

        if (!event.level.isClientSide() && event.level.getServer() != null && event.level.getDayTime() % 20L == 1L) {

            for (ServerPlayer player : event.level.getServer().getPlayerList().getPlayers()) {
                BlockPos blockPos = player.getOnPos();
                if (event.level.dimension().equals(player.level().dimension()) && !player.gameMode.getGameModeForPlayer().equals(GameType.SPECTATOR) && event.level.getBlockState(blockPos).blocksMotion() && !Config.indestructibleBlocks.contains(event.level.getBlockState(blockPos).getBlock()) && !checkMap.containsKey(blockPos) && event.level.dimension() != Level.END) {
                    checkMap.put(blockPos,new Tuple<>(event.level,1));
                }
            }
        }


        if (event.side.isServer() && event.level.getServer() != null && event.phase.equals(TickEvent.Phase.START)) {
            Iterator<FallingBlockEntity> fallingBlocks = fallingSet.iterator();
            HashSet<FallingBlockEntity> tempFallingBlocks = new HashSet<>();
            HashSet<BlockPos> aboutToGroundPos = new HashSet<>();

            while(fallingBlocks.hasNext()) {
                FallingBlockEntity fallingBlockEntity = fallingBlocks.next();
                Level world = fallingBlockEntity.level();
                if (world.dimension().equals(fallingBlockEntity.level().dimension())) {
                    if (fallingBlockEntity.isAlive() && (world.getBlockState(fallingBlockEntity.blockPosition().atY((int) Math.floor(fallingBlockEntity.getY() + fallingBlockEntity.getDeltaMovement().y - 0.04))).blocksMotion()) || aboutToGroundPos.contains(fallingBlockEntity.blockPosition().atY((int) Math.floor(fallingBlockEntity.getY() + fallingBlockEntity.getDeltaMovement().y - 0.04)))) {
                        world.playSound(null, fallingBlockEntity.blockPosition(), fallingBlockEntity.getBlockState().getSoundType().getBreakSound(), SoundSource.BLOCKS, Config.blockBreakVolume.floatValue(), 1F);
                        if (fallingBlockEntity.getDeltaMovement().y*-5 > world.getRandom().nextIntBetweenInclusive(0,10)) {
                            world.getServer().getLevel(world.dimension()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,fallingBlockEntity.getBlockState()),fallingBlockEntity.getX(),fallingBlockEntity.getY(),fallingBlockEntity.getZ(),10,0,0,0,1);
                            fallingBlockEntity.causeFallDamage(fallingBlockEntity.fallDistance,Config.dmgMax,fallingBlockEntity.damageSources().fallingBlock(fallingBlockEntity));
                            fallingBlockEntity.discard();
                        } else {
                            int nextY = (int) Math.floor(fallingBlockEntity.getY() + fallingBlockEntity.getDeltaMovement().y - 0.04);
                            List<Vec3i> validShiftDirList = new ArrayList<>();
                            for (int i = 1; i <= 4; ++i) {
                                Vec3i dirVec = switch (i) {
                                    case 1 -> new Vec3i(0, 0, 1);
                                    case 2 -> new Vec3i(1, 0, 0);
                                    case 3 -> new Vec3i(0, 0, -1);
                                    case 4 -> new Vec3i(-1, 0, 0);
                                    default -> throw new IllegalStateException("Unexpected value: " + i);
                                };
                                if (!world.getBlockState(fallingBlockEntity.blockPosition().atY(nextY + 1).offset(dirVec)).blocksMotion() && !world.getBlockState(fallingBlockEntity.blockPosition().atY(nextY).offset(dirVec)).blocksMotion()) {
                                    validShiftDirList.add(dirVec);
                                }
                            }
                            if (!validShiftDirList.isEmpty()) {
                                Vec3i fallVec = validShiftDirList.get(world.getRandom().nextIntBetweenInclusive(0, validShiftDirList.size() - 1));
                                FallingBlockEntity newFallingBlockEntity = FallingBlockEntity.fall(world, fallingBlockEntity.blockPosition().offset(fallVec), fallingBlockEntity.getBlockState());
                                newFallingBlockEntity.setHurtsEntities(Config.dmgDist, Config.dmgMax);
                                tempFallingBlocks.add(newFallingBlockEntity);
                                fallingBlockEntity.causeFallDamage(fallingBlockEntity.fallDistance,Config.dmgMax,fallingBlockEntity.damageSources().fallingBlock(fallingBlockEntity));
                                fallingBlockEntity.discard();
                            } else {
                                aboutToGroundPos.add(fallingBlockEntity.blockPosition().atY(nextY + 1));
                            }
                        }
                        fallingBlocks.remove();
                    } else if (!fallingBlockEntity.isAlive()) {
                        fallingBlocks.remove();
                    }
                }
            }

            fallingSet.addAll(tempFallingBlocks);
            aboutToGroundPos.clear();

        }

    }

    public boolean checkBreak(BlockPos blockPos, LevelAccessor level, Integer generation) {
        HashSet<BlockPos> supportSet = new HashSet<>();
        HashSet<BlockPos> newlyAddedSet = new HashSet<>();
        HashSet<BlockPos> freshSet = new HashSet<>();
        newlyAddedSet.add(blockPos);

        for(int i = 1; i <= getSupportStrength(level.getBlockState(blockPos)); ++i) {

            for (BlockPos currentPos : newlyAddedSet) {
                for (BlockPos adjCurrentPos : BlockPos.betweenClosed(currentPos.offset(-1, -1, -1), currentPos.offset(1, 1, 1))) {
                    if (level.getBlockState(adjCurrentPos).blocksMotion() && !supportSet.contains(adjCurrentPos) && adjCurrentPos.distManhattan(currentPos) == 1 && !adjCurrentPos.equals(blockPos)) {
                        if (adjCurrentPos.getY() < blockPos.getY()) {
                            if (!Config.indestructibleBlocks.contains(level.getBlockState(adjCurrentPos).getBlock()) && generation <= Config.supportSearchIter) {
                                checkMap.put(adjCurrentPos.immutable(),new Tuple<>(level,generation+1));
                            }
                            return false;
                        }
                        supportSet.add(adjCurrentPos.immutable());
                        freshSet.add(adjCurrentPos.immutable());
                    }
                }
            }
            newlyAddedSet.clear();
            newlyAddedSet.addAll(freshSet);
            freshSet.clear();
        }

        FallingBlockEntity fallingBlockEntity = FallingBlockEntity.fall((Level) level, blockPos, level.getBlockState(blockPos));
        fallingBlockEntity.setHurtsEntities(Config.dmgDist,Math.min(getSupportStrength(fallingBlockEntity.getBlockState()),Config.dmgMax));
        fallingSet.add(fallingBlockEntity);
        return true;
    }

    public static int getSupportStrength(BlockState blockState) {
        AtomicInteger strength = new AtomicInteger(-1);
        blockState.getTags().forEach(blockTagKey -> {
            if (Config.overwrittenTagMap.containsKey(blockTagKey)) {
                strength.set(Config.overwrittenTagMap.get(blockTagKey));
            }
        });
        if (strength.get() == -1) {
            float hardness = blockState.getBlock().defaultDestroyTime();
            if (hardness > 7F) {
                hardness = (7F);
            }
            return Math.round((float)(Math.log10(hardness +1) * ((Config.supportLengthMax-Config.supportLengthMin)/0.903089987) + Config.supportLengthMin));
        }
        else{
            return strength.get();
        }
    }
}