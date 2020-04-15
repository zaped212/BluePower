package com.bluepowermod.block.machine;

import com.bluepowermod.api.misc.MinecraftColor;
import com.bluepowermod.api.wire.redstone.RedwireType;
import com.bluepowermod.block.BlockWireBase;
import com.bluepowermod.reference.Refs;
import net.minecraft.block.BlockState;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class BlockInsulatedAlloyWire extends BlockAlloyWire{
    private final MinecraftColor color;
    private final String type;

    public BlockInsulatedAlloyWire(String type, MinecraftColor color) {
        this.color = color;
        this.type = type;
        setRegistryName(Refs.MODID + ":" + "wire." + type + "." + color.name().toLowerCase());
    }

    @Override
    protected boolean canConnectTo(World world, BlockPos neighbor_pos, BlockState neighbor_state, Direction neighbor_face) {
        if( ( neighbor_state.getBlock() instanceof BlockInsulatedAlloyWire ) &&
            (((BlockInsulatedAlloyWire) neighbor_state.getBlock()).color != this.color ) ) {
            /* Colors of insulated wire dont match. Dont allow a connection */
            return false;
        }
        return super.canConnectTo( world, neighbor_pos, neighbor_state, neighbor_face );
    }

    @Override
    public int getColor(IBlockReader world, BlockPos pos, int tintIndex) {
        //Color for Block
        return tintIndex == 1 ? color.getHex() : tintIndex == 2 ? RedwireType.RED_ALLOY.getName().equals(type) ? MinecraftColor.RED.getHex() : MinecraftColor.BLUE.getHex() : -1;
    }

    @Override
    public int getColor(int tintIndex) {
        //Color for Block
        return tintIndex == 1 ? color.getHex() : tintIndex == 2 ? RedwireType.RED_ALLOY.getName().equals(type) ? MinecraftColor.RED.getHex() : MinecraftColor.BLUE.getHex() : -1;
    }
}
