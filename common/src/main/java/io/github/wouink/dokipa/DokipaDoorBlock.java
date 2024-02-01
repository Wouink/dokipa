package io.github.wouink.dokipa;

import io.github.wouink.dokipa.server.DokipaDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
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
            level.setBlock(pos, base.setValue(HALF, DoubleBlockHalf.LOWER).setValue(FACING, facing), Block.UPDATE_ALL);
            level.setBlock(pos.above(), base.setValue(HALF, DoubleBlockHalf.UPPER).setValue(FACING, facing), Block.UPDATE_ALL);
            DokipaDataManager.getInstance(level.getServer()).setDoorPos(doorUUID, pos);

            // set door uuid in BlockEntity
            if(level.getBlockEntity(pos) instanceof DokipaDoorBlockEntity dokipaDoor) {
                Dokipa.logWithLevel(level, "Door Block (summon)", "There is indeed a door at pos " + pos);
                dokipaDoor.setDoorUUID(doorUUID);
            }
            // todo particles and sound
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
        // todo particles and sound
        return true;
    }

    // this gets called twice... the door therefore never opens
    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        boolean interact = false;
        if(!level.isClientSide()) {
            Dokipa.logWithLevel(level, "Door Block", "Right click on Dokipa's door");
            BlockPos blockEntityPos = blockState.getValue(HALF) == DoubleBlockHalf.LOWER ? blockPos : blockPos.above();
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
            toggleOpen(player, level, blockState, blockPos);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        else return InteractionResult.FAIL;
    }

    public void toggleOpen(@Nullable Entity entity, Level level, BlockState state, BlockPos blockPos) {
        BlockState newState = state.cycle(OPEN);
        level.setBlock(blockPos, newState, Block.UPDATE_ALL);
        Dokipa.logWithLevel(level, "Door Block (toggleOpen)", "newState.OPEN = " + newState.getValue(OPEN).booleanValue());
        level.playSound(null, blockPos, newState.getValue(OPEN).booleanValue() ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, level.getRandom().nextFloat() * 0.1f + 0.9f);
    }

    // updates the other door half when the first half is changed
    @Override
    public BlockState updateShape(BlockState blockState, Direction direction, BlockState source, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos sourcePos) {
        if((sourcePos == blockPos.above() || sourcePos == blockPos.below()) && source.is(this)) {
            return blockState.setValue(OPEN, source.getValue(OPEN));
        }
        return blockState;
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
