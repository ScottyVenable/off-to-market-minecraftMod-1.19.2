package com.offtomarket.mod.block;

import com.offtomarket.mod.block.entity.TradingLedgerBlockEntity;
import com.offtomarket.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class TradingLedgerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public TradingLedgerBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TradingLedgerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return createTickerHelper(type, ModBlockEntities.TRADING_LEDGER.get(),
                    TradingLedgerBlockEntity::serverTick);
        }
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TradingLedgerBlockEntity tbbe) {
                // Opening the GUI — auto-sync from adjacent containers is handled by serverTick.
                NetworkHooks.openScreen((ServerPlayer) player, tbbe, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TradingLedgerBlockEntity tbbe) {
                // Only drop items that are physically owned by this ledger block.
                // Virtual slots mirror items from adjacent containers — those items
                // stay in their source container and should NOT be duplicated on break.
                net.minecraft.world.item.ItemStack[] owned = new net.minecraft.world.item.ItemStack[TradingLedgerBlockEntity.BIN_SIZE];
                for (int i = 0; i < TradingLedgerBlockEntity.BIN_SIZE; i++) {
                    owned[i] = tbbe.isVirtualSlot(i)
                            ? net.minecraft.world.item.ItemStack.EMPTY
                            : tbbe.getItems().get(i);
                }
                for (net.minecraft.world.item.ItemStack stack : owned) {
                    net.minecraft.world.Containers.dropItemStack(level,
                            pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
