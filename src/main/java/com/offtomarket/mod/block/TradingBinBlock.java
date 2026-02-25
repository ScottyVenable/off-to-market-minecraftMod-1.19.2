package com.offtomarket.mod.block;

import com.offtomarket.mod.block.entity.TradingBinBlockEntity;
import com.offtomarket.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class TradingBinBlock extends BaseEntityBlock {

    public TradingBinBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TradingBinBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return createTickerHelper(type, ModBlockEntities.TRADING_BIN.get(),
                    TradingBinBlockEntity::serverTick);
        }
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TradingBinBlockEntity tbbe) {
                // Deposit held item into the bin before opening the GUI
                ItemStack held = player.getItemInHand(hand);
                if (!held.isEmpty()) {
                    ItemStack toDeposit = held.copy();
                    if (tbbe.addItem(toDeposit)) {
                        player.setItemInHand(hand, ItemStack.EMPTY);
                    }
                }
                NetworkHooks.openScreen((ServerPlayer) player, tbbe, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TradingBinBlockEntity tbbe) {
                Containers.dropContents(level, pos, tbbe.getItems());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
