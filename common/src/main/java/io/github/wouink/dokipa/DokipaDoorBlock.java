package io.github.wouink.dokipa;

import dev.architectury.event.EventResult;
import io.github.wouink.dokipa.server.DokipaDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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
        BlockPos above = pos.above();
        // todo check if the block below can sustain a door
        // -> stateBelow.isFaceSturdy(..., Direction.UP)
        if(level.getBlockState(pos).canBeReplaced() && level.getBlockState(above).canBeReplaced()) {
            Dokipa.logWithLevel(level, "Door Block (summon)", "Adding door " + doorUUID + " at " + pos);
            Dokipa.logWithLevel(level, "Door Block (summon)", "Facing = " + facing);
            BlockState base = Dokipa.Dokipa_Door.get().defaultBlockState();
            level.setBlock(pos, base.setValue(HALF, DoubleBlockHalf.LOWER).setValue(FACING, facing).setValue(OPEN, false), Block.UPDATE_ALL);
            level.setBlock(pos.above(), base.setValue(HALF, DoubleBlockHalf.UPPER).setValue(FACING, facing).setValue(OPEN, false), Block.UPDATE_ALL);
            DokipaDataManager.getInstance(level.getServer()).setDoorPos(doorUUID, pos);

            // set door uuid in BlockEntity
            if(level.getBlockEntity(pos) instanceof DokipaDoorBlockEntity dokipaDoor) {
                Dokipa.logWithLevel(level, "Door Block (summon)", "There is indeed a door at pos " + pos);
                dokipaDoor.setDoorUUID(doorUUID);
            }

            // particles and sound
            if(level instanceof ServerLevel serverLevel) {
                double x = pos.getX() + 0.5;
                double y = pos.getY() + 1;
                double z = pos.getZ() + 0.5;
                serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, x, y, z, 10, 0, 0, 0, 1);
                serverLevel.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
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
                DokipaDataManager.getInstance(level.getServer()).setDoorPos(doorUUID, null);
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        if(level.getBlockState(pos.above()).is(Dokipa.Dokipa_Door.get())) {
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        // particles and sound
        if(level instanceof ServerLevel serverLevel) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1;
            double z = pos.getZ() + 0.5;
            serverLevel.sendParticles(ParticleTypes.ASH, x, y, z, 10, 0, 0, 0, 1);
            serverLevel.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        return true;
    }

    public static EventResult interact(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        Level level = player.level();
        BlockState state = level.getBlockState(pos);

        if(!state.is(Dokipa.Dokipa_Door.get())) return EventResult.pass();

        boolean interact = false;
        if(!level.isClientSide()) {
            Dokipa.logWithLevel(level, "Door Block", "Right click on Dokipa's door");
            BlockPos blockEntityPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            if(level.getBlockEntity(blockEntityPos) instanceof DokipaDoorBlockEntity dokipaDoor) {
                if(!dokipaDoor.hasOwner()) {
                    Dokipa.logWithLevel(level, "Door Block", "The door does not have an owner");
                    if(dokipaDoor.trySetOwner(player)) {
                        // the player is now a dokipa
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

            // 1st half
            BlockState newState1 = state.cycle(OPEN);
            level.setBlockAndUpdate(pos, newState1);
            Dokipa.logWithLevel(level, "Door Block (toggleOpen)", "newState.OPEN = " + newState1.getValue(OPEN).booleanValue());

            // 2nd half
            // todo use updateShape instead. here there is a 1 tick delay between the first half and the second one
            BlockPos secondHalfPos = newState1.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState newState2 = level.getBlockState(secondHalfPos).setValue(OPEN, newState1.getValue(OPEN));
            level.setBlockAndUpdate(secondHalfPos, newState2);

            // sound
            level.playSound(null, pos, newState1.getValue(OPEN).booleanValue() ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, level.getRandom().nextFloat() * 0.1f + 0.9f);
            return EventResult.interruptTrue();
        }
        return EventResult.interruptFalse();
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
