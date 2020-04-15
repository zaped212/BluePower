/*
 * This file is part of Blue Power. Blue Power is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Blue Power is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along
 * with Blue Power. If not, see <http://www.gnu.org/licenses/>
 */

package com.bluepowermod.block;

import com.bluepowermod.api.misc.MinecraftColor;
import com.bluepowermod.block.BlockContainerBase;
import com.bluepowermod.client.render.IBPColoredBlock;
import com.bluepowermod.tile.TileBPMultipart;
import com.bluepowermod.tile.tier1.TileWire;

import com.bluepowermod.util.AABBUtils;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.*;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author MoreThanHidden
 */
public class BlockWireBase extends BlockContainerBase implements IWaterLoggable, IBPColoredBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final BooleanProperty CONNECTED_FRONT = BooleanProperty.create("connected_front");
    private static final BooleanProperty CONNECTED_BACK = BooleanProperty.create("connected_back");
    private static final BooleanProperty CONNECTED_LEFT = BooleanProperty.create("connected_left");
    private static final BooleanProperty CONNECTED_RIGHT = BooleanProperty.create("connected_right");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    protected final VoxelShape[] shapes = makeShapes();

    public BlockWireBase() {

        super(Material.IRON, TileWire.class);
        this.setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.UP)
                .with(CONNECTED_FRONT, false).with(CONNECTED_BACK, false)
                .with(CONNECTED_LEFT, false).with(CONNECTED_RIGHT, false)
                .with(WATERLOGGED, false));
    }

    @Override
    public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onReplaced(state, worldIn, pos, newState, isMoving);
        FACING.getAllowedValues().forEach(f ->{
                BlockPos neighborPos = pos.offset(f).offset(state.get(FACING).getOpposite());
                worldIn.getBlockState(neighborPos).neighborChanged(worldIn, neighborPos, state.getBlock(), pos, isMoving);
        });
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity p_180633_4_, ItemStack p_180633_5_) {
        super.onBlockPlacedBy(worldIn, pos, state, p_180633_4_, p_180633_5_);
        FACING.getAllowedValues().forEach(f -> {
            BlockPos neighborPos = pos.offset(f).offset(state.get(FACING).getOpposite());
            worldIn.getBlockState(neighborPos).neighborChanged(worldIn, neighborPos, state.getBlock(), pos, false);
        });
    }


    @Override
    public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (stateIn.get(WATERLOGGED)) {
            worldIn.getPendingFluidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(worldIn));
        }
        return super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    public IFluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
    }

    @Override
    public boolean isValidPosition(BlockState state, IWorldReader world, BlockPos pos) {
        return world.getBlockState(pos.offset(state.get(FACING).getOpposite())).isSolid();
    }

    protected VoxelShape[] makeShapes() {

        float width = 2;
        float gap = 0;
        float height = 2;

        float f = 8.0F - width;
        float f1 = 8.0F + width;
        float f2 = 8.0F - width;
        float f3 = 8.0F + width;

        VoxelShape voxelshape = Block.makeCuboidShape((double)f, 0.0D, (double)f, (double)f1, (double)height, (double)f1);
        VoxelShape voxelshape1 = Block.makeCuboidShape((double)f2, (double)gap, 0.0D, (double)f3, (double)height, (double)f3);
        VoxelShape voxelshape2 = Block.makeCuboidShape((double)f2, (double)gap, (double)f2, (double)f3, (double)height, 16.0D);
        VoxelShape voxelshape3 = Block.makeCuboidShape(0.0D, (double)gap, (double)f2, (double)f3, (double)height, (double)f3);
        VoxelShape voxelshape4 = Block.makeCuboidShape((double)f2, (double)gap, (double)f2, 16.0D, (double)height, (double)f3);
        VoxelShape voxelshape5 = VoxelShapes.or(voxelshape1, voxelshape4);
        VoxelShape voxelshape6 = VoxelShapes.or(voxelshape2, voxelshape3);

        VoxelShape[] avoxelshape = new VoxelShape[]{
                VoxelShapes.empty(), voxelshape2, voxelshape3, voxelshape6, voxelshape1,
                VoxelShapes.or(voxelshape2, voxelshape1), VoxelShapes.or(voxelshape3, voxelshape1),
                VoxelShapes.or(voxelshape6, voxelshape1), voxelshape4, VoxelShapes.or(voxelshape2, voxelshape4),
                VoxelShapes.or(voxelshape3, voxelshape4), VoxelShapes.or(voxelshape6, voxelshape4), voxelshape5,
                VoxelShapes.or(voxelshape2, voxelshape5), VoxelShapes.or(voxelshape3, voxelshape5),
                VoxelShapes.or(voxelshape6, voxelshape5)
        };

        for(int i = 0; i < 16; ++i) {
            avoxelshape[i] = VoxelShapes.or(voxelshape, avoxelshape[i]);
        }

        return avoxelshape;
    }

    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return AABBUtils.rotate(this.shapes[this.getShapeIndex(state)], state.get(FACING));
    }

    private int getShapeIndex(BlockState state) {
        int i = 0;

        if(state.get(CONNECTED_FRONT))
            i |= getMask(Direction.NORTH);
        if(state.get(CONNECTED_BACK))
            i |= getMask(Direction.SOUTH);
        if(state.get(CONNECTED_LEFT))
            i |= getMask(Direction.WEST);
        if(state.get(CONNECTED_RIGHT))
            i |= getMask(Direction.EAST);

        return i;
    }

    private static int getMask(Direction facing) {
        return 1 << facing.getHorizontalIndex();
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean bool) {
        TileEntity te = world.getTileEntity(pos);
        //Get new state based on surrounding capabilities
        BlockState newState = getStateForPos(world, pos, getDefaultState().with(FACING, state.get(FACING)), state.get(FACING));

        if (!(te instanceof TileBPMultipart)){
           //Change the block state
           world.setBlockState(pos, newState, 2);
        }else{
            //Update the state in the Multipart
            ((TileBPMultipart) te).changeState(state, newState);
        }
        state = newState;
        //If not placed on a solid block break off
        if (!world.getBlockState(pos.offset(state.get(FACING).getOpposite())).isSolid()) {
            if(te instanceof TileBPMultipart){
                ((TileBPMultipart)te).removeState(state);
                if(world instanceof ServerWorld) {
                    NonNullList<ItemStack> drops = NonNullList.create();
                    drops.add(new ItemStack(this));
                    InventoryHelper.dropItems(world, pos, drops);
                }
            }else {
                world.destroyBlock(pos, true);
            }
        }
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder){
        builder.add(FACING, CONNECTED_FRONT, CONNECTED_BACK, CONNECTED_LEFT, CONNECTED_RIGHT, WATERLOGGED);
    }

    private BlockState getStateForPos(World world, BlockPos pos, BlockState state, Direction face){
        List<Direction> directions = new ArrayList<>(FACING.getAllowedValues());

        boolean new_connected_front = state.get( CONNECTED_FRONT );
        boolean new_connected_back = state.get( CONNECTED_BACK );
        boolean new_connected_left = state.get( CONNECTED_LEFT );
        boolean new_connected_right = state.get( CONNECTED_RIGHT );


        //Populate all directions
        for (Direction d : directions) {
            TileEntity tileEntity = world.getTileEntity(pos.offset(d));

            boolean can_connect = canConnectTo(world, pos.offset(d), state, face);

            // Check to see if we can connect / join with the neighbor
            switch (state.get(FACING)) {
                case UP:
                case DOWN:
                    switch (d) {
                        case EAST:
                            new_connected_right = can_connect;
                            break;
                        case WEST:
                            new_connected_left = can_connect;
                            break;
                        case NORTH:
                            new_connected_front = can_connect;
                            break;
                        case SOUTH:
                            new_connected_back = can_connect;
                            break;
                    }
                    break;
                case NORTH:
                    switch (d) {
                        case WEST:
                            new_connected_right = can_connect;
                            break;
                        case EAST:
                            new_connected_left = can_connect;
                            break;
                        case UP:
                            new_connected_front = can_connect;
                            break;
                        case DOWN:
                            new_connected_back = can_connect;
                            break;
                    }
                    break;
                case SOUTH:
                    switch (d) {
                        case EAST:
                            new_connected_right = can_connect;
                            break;
                        case WEST:
                            new_connected_left = can_connect;
                            break;
                        case UP:
                            new_connected_front = can_connect;
                            break;
                        case DOWN:
                            new_connected_back = can_connect;
                            break;
                    }
                    break;
                case EAST:
                    switch (d) {
                        case NORTH:
                            new_connected_right = can_connect;
                            break;
                        case SOUTH:
                            new_connected_left = can_connect;
                            break;
                        case UP:
                            new_connected_front = can_connect;
                            break;
                        case DOWN:
                            new_connected_back = can_connect;
                            break;
                    }
                    break;
                case WEST:
                    switch (d) {
                        case SOUTH:
                            new_connected_right = can_connect;
                            break;
                        case NORTH:
                            new_connected_left = can_connect;
                            break;
                        case UP:
                            new_connected_front = can_connect;
                            break;
                        case DOWN:
                            new_connected_back = can_connect;
                            break;
                    }
                }
        }
        IFluidState fluidstate = world.getFluidState(pos);
        boolean new_waterlogged_state = fluidstate.getFluid() == Fluids.WATER;

        /* check if we need to update our state */
        if( ( state.get( CONNECTED_FRONT ) != new_connected_front ) ||
            ( state.get( CONNECTED_BACK ) != new_connected_back ) ||
            ( state.get( CONNECTED_LEFT ) != new_connected_left ) ||
            ( state.get( CONNECTED_RIGHT ) != new_connected_right ) ||
            ( state.get( WATERLOGGED ) != new_waterlogged_state ) )
            {
            state = state.with( CONNECTED_FRONT, new_connected_front )
                         .with( CONNECTED_BACK, new_connected_back )
                         .with( CONNECTED_LEFT, new_connected_left )
                         .with( CONNECTED_RIGHT, new_connected_right )
                         .with( WATERLOGGED, new_waterlogged_state );
            }
        return state;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return getStateForPos(context.getWorld(), context.getPos(), getDefaultState().with(FACING, context.getFace()), context.getFace());
    }

    protected boolean canConnectTo(World world, BlockPos neighbor_pos, BlockState cur_state, Direction cur_face) {
        /* Check if the neighbor block is of the same type */
        if( world.getBlockState(neighbor_pos).getBlock() instanceof BlockWireBase )
            {
            /* Only connect to instances that are facing the same direction */
            return world.getBlockState(neighbor_pos).get(FACING) == cur_face;
            }
        return false;
    }

    @Override
    public int getColor(IBlockReader world, BlockPos pos, int tintIndex) {
        return MinecraftColor.RED.getHex();
    }

    @Override
    public int getColor(int tintIndex) {
        return MinecraftColor.RED.getHex();
    }
}
