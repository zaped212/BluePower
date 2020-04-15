package com.bluepowermod.block.machine;

import com.bluepowermod.api.misc.MinecraftColor;
import com.bluepowermod.api.wire.redstone.RedwireType;
import com.bluepowermod.block.BlockWireBase;
import com.bluepowermod.reference.Refs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ObserverBlock;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class BlockAlloyWire extends BlockWireBase{
    private final String type;

    public BlockAlloyWire() {
        type = RedwireType.RED_ALLOY.getName();
    }

    public BlockAlloyWire(String type) {
        //super( 2, 0, 2 );
        this.type = type;
        setRegistryName(Refs.MODID + ":" + type + "_wire");
    }

    @Override
    protected boolean canConnectTo(World world, BlockPos neighbor_pos, BlockState neighbor_state, Direction neighbor_face) {
        if (Blocks.OBSERVER == neighbor_state.getBlock()){
            return neighbor_face == neighbor_state.get(ObserverBlock.FACING);
        }else if(neighbor_state.getBlock().canConnectRedstone(neighbor_state, world, neighbor_pos, neighbor_face)){
            return true;
        }else {
            return super.canConnectTo(world, neighbor_pos, neighbor_state, neighbor_face);
        }
    }

    @Override
    public int getColor(IBlockReader w, BlockPos pos, int tint) {
        return tint == 2 ? RedwireType.RED_ALLOY.getName().equals(type) ? MinecraftColor.RED.getHex() : MinecraftColor.BLUE.getHex() : -1;
    }

    @Override
    public int getColor(int tint) {
        return tint == 2 ? RedwireType.RED_ALLOY.getName().equals(type) ? MinecraftColor.RED.getHex() : MinecraftColor.BLUE.getHex() : -1;
    }
}
