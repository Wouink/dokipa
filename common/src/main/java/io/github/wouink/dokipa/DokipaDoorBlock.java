package io.github.wouink.dokipa;

import dev.architectury.event.EventResult;
import io.github.wouink.dokipa.server.DokipaSavedData;
import io.github.wouink.dokipa.server.DoorInfo;
import io.github.wouink.dokipa.server.LocalizedBlockPos;
import io.github.wouink.dokipa.server.RoomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public class DokipaDoorBlock extends Block implements EntityBlock {
    public static DirectionProperty FACING = DoorBlock.FACING;
    public static EnumProperty<DoorHingeSide> HINGE = DoorBlock.HINGE;
    public static BooleanProperty OPEN = DoorBlock.OPEN;
    public static EnumProperty<DoubleBlockHalf> HALF = DoorBlock.HALF;

    public DokipaDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, HALF, HINGE, OPEN);
    }

    public static boolean summon(Level level, BlockPos pos, UUID doorUUID, Direction facing) {
        // check if we have room to summon the door
        BlockPos above = pos.above();
        boolean canSummon = level.getBlockState(pos).canBeReplaced() && level.getBlockState(above).canBeReplaced();

        if(canSummon) {
            Dokipa.logWithLevel(level, "Door Block (summon)", "Adding door " + doorUUID + " at " + pos + ", Facing = " + facing);

            BlockState base = Dokipa.Dokipa_Door.get().defaultBlockState().setValue(FACING, facing).setValue(OPEN, false);
            level.setBlock(pos, base.setValue(HALF, DoubleBlockHalf.LOWER), Block.UPDATE_ALL);
            level.setBlock(above, base.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);

            DokipaSavedData savedData = Dokipa.savedData(level.getServer());
            savedData.doorInfo(doorUUID).setPlacedPos(new LocalizedBlockPos(pos, level));
            savedData.setDirty();

            // set door uuid in BlockEntity
            if(level.getBlockEntity(pos) instanceof DokipaDoorBlockEntity dokipaDoor) {
                Dokipa.logWithLevel(level, "Door Block (summon)", "There is indeed a door at pos " + pos);
                dokipaDoor.setDoorUUID(doorUUID);
            }

            // particles and sound
            if(level instanceof ServerLevel serverLevel) {
                particlesAndSound(serverLevel, pos, ParticleTypes.DRAGON_BREATH, SoundEvents.ENCHANTMENT_TABLE_USE);
            }

            return true;
        } else {
            Dokipa.logWithLevel(level, "Door Block (summon)", "Could not add door " + doorUUID + " at " + pos);
        }

        return false;
    }

    public static boolean unsummon(Level level, BlockPos pos) {
        Dokipa.logWithLevel(level, "Door Block (unsummon)", "Removing door at " + pos);

        if(level.getBlockState(pos).is(Dokipa.Dokipa_Door.get())) {
            if(level.getBlockEntity(pos) instanceof DokipaDoorBlockEntity dokipaDoor) {
                UUID doorUUID = dokipaDoor.getDoorUUID();
                DokipaSavedData savedData = Dokipa.savedData(level.getServer());
                savedData.doorInfo(doorUUID).setPlacedPos(null);
                savedData.setDirty();
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        if(level.getBlockState(pos.above()).is(Dokipa.Dokipa_Door.get())) {
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        // particles and sound
        if(level instanceof ServerLevel serverLevel) {
            particlesAndSound(serverLevel, pos, ParticleTypes.FLAME, SoundEvents.FIRECHARGE_USE);
        }

        return true;
    }

    private static void particlesAndSound(ServerLevel level, BlockPos pos, ParticleOptions particles, SoundEvent sound) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1;
        double z = pos.getZ() + 0.5;
        level.sendParticles(particles, x, y, z, 10, 0, 0, 0, 1);
        level.playSound(null, pos, sound, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static BlockPos getLowerPos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    public static BlockPos getBlockEntityPos(BlockState state, BlockPos pos) {
        return getLowerPos(state, pos);
    }

    public static EventResult interact(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        Level level = player.level();
        BlockState state = level.getBlockState(pos);

        if(!state.is(Dokipa.Dokipa_Door.get())) return EventResult.pass();

        boolean interact = false;

        // todo check if interacted with from the correct side (cannot open the door from it's back)
        Dokipa.LOG.info("Door facing = " + state.getValue(FACING) + ", face = " + face);

        if(!level.isClientSide()) {
            Dokipa.logWithLevel(level, "Door Block", "Right click on Dokipa's door");
            if(level.getBlockEntity(getBlockEntityPos(state, pos)) instanceof DokipaDoorBlockEntity dokipaDoor) {
                if(!dokipaDoor.hasOwner()) {
                    Dokipa.logWithLevel(level, "Door Block", "The door does not have an owner");
                    if(dokipaDoor.trySetOwner(player)) {
                        // todo replace this message with a sound
                        player.displayClientMessage(Component.literal("You are now a Dokipa"), true);
                    }
                } else if(dokipaDoor.isOwner(player)) {
                    Dokipa.logWithLevel(level, "Door Block", "The player owns this door");
                    interact = true;
                }
            }
        }

        if(interact) {
            Dokipa.logWithLevel(level, "Door Block", "Opening/closing door");
            openOnBothSides((ServerLevel) level, getLowerPos(state, pos), !state.getValue(OPEN));
            return EventResult.interruptTrue();
        }

        return EventResult.interruptFalse();
    }

    private static void setOpen(ServerLevel level, BlockPos lowerPos, boolean open) {
        BlockState door = level.getBlockState(lowerPos);
        level.setBlockAndUpdate(lowerPos, door.setValue(OPEN, open));
        door = level.getBlockState(lowerPos.above());
        level.setBlockAndUpdate(lowerPos.above(), door.setValue(OPEN, open));
        level.playSound(null, lowerPos, open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, level.getRandom().nextFloat() * 0.1f + 0.9f);
    }

    public static void openOnBothSides(ServerLevel level, BlockPos lowerPos, boolean open) {
        if (level.getBlockEntity(lowerPos) instanceof DokipaDoorBlockEntity dokipaDoor) {
            UUID doorUUID = dokipaDoor.getDoorUUID();
            MinecraftServer server = level.getServer();
            DokipaSavedData savedData = Dokipa.savedData(server);
            DoorInfo doorInfo = savedData.doorInfo(doorUUID);

            LocalizedBlockPos placedPos = doorInfo.placedPos();
            if (placedPos != null) placedPos.executeAt(server, (l, p) -> {
                setOpen((ServerLevel) l, p, open);
            });

            LocalizedBlockPos posInRoom = doorInfo.localizedPosInRoom(server);
            if (posInRoom != null) posInRoom.executeAt(server, (l, p) -> {
                setOpen((ServerLevel) l, p, open);
            });
        }
    }

    public static void teleport(BlockState state, ServerLevel level, BlockPos pos, Entity entity) {
        if(level.getBlockEntity(getBlockEntityPos(state, pos)) instanceof DokipaDoorBlockEntity dokipaDoor) {
            MinecraftServer server = level.getServer();
            UUID doorUUID = dokipaDoor.getDoorUUID();
            DokipaSavedData savedData = Dokipa.savedData(server);
            DoorInfo doorInfo = savedData.doorInfo(doorUUID);

            LocalizedBlockPos destination = null;

            // if destination is doors level and the room does not exist, generate the room
            if(!level.dimension().equals(Dokipa.Dimension)) {
                if(!doorInfo.isRoomGenerated()) {
                    ServerLevel doorsLevel = server.getLevel(Dokipa.Dimension);
                    if(doorsLevel != null) {
                        BlockPos genPos = RoomGenerator.getPosForRoom(savedData.nextRoomNumber());
                        BlockPos doorPos = RoomGenerator.generateRoom(doorsLevel, genPos, doorUUID);
                        if(doorPos != RoomGenerator.Null_Door_Pos) {
                            doorInfo.setPosInRoom(doorPos);
                            savedData.setDirty();
                        }
                    }
                }
                destination = doorInfo.localizedPosInRoom(server);
            } else {
                destination = doorInfo.placedPos();
            }

            // tp to destination
            if(destination != null) {
                Level outLevel = destination.getDimension(server);
                BlockPos outPos = destination.getPos();

                BlockState outState = outLevel.getBlockState(outPos);
                Direction outFacing = Direction.EAST;
                if(outState.is(Dokipa.Dokipa_Door.get())) outFacing = outState.getValue(FACING);

                outPos = outPos.relative(outFacing, 1);
                entity.teleportTo((ServerLevel) outLevel, outPos.getX() + 0.5, outPos.getY(), outPos.getZ() + 0.5, Set.of(), outFacing.toYRot(), 0);
            }

            // todo else play a sound ?
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        boolean tp = !level.isClientSide() && state.getValue(OPEN).booleanValue() && entity.canChangeDimensions();
        if(tp) teleport(state, (ServerLevel) level, pos, entity);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return blockState.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? null : new DokipaDoorBlockEntity(blockPos, blockState);
    }

    // copied from net.minecraft.world.level.block.DoorBlock
    public static final VoxelShape SOUTH_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 3.0);
    public static final VoxelShape NORTH_AABB = Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0);
    public static final VoxelShape WEST_AABB = Block.box(13.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    public static final VoxelShape EAST_AABB = Block.box(0.0, 0.0, 0.0, 3.0, 16.0, 16.0);

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        Direction direction = (Direction)blockState.getValue(FACING);
        boolean bl = !(Boolean)blockState.getValue(OPEN);
        boolean bl2 = blockState.getValue(HINGE) == DoorHingeSide.RIGHT;
        switch (direction) {
            case EAST:
            default:
                return bl ? EAST_AABB : (bl2 ? NORTH_AABB : SOUTH_AABB);
            case SOUTH:
                return bl ? SOUTH_AABB : (bl2 ? EAST_AABB : WEST_AABB);
            case WEST:
                return bl ? WEST_AABB : (bl2 ? SOUTH_AABB : NORTH_AABB);
            case NORTH:
                return bl ? NORTH_AABB : (bl2 ? WEST_AABB : EAST_AABB);
        }
    }
}
