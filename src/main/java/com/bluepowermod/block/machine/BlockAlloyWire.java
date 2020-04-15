package com.bluepowermod.block.machine;

import com.bluepowermod.api.misc.MinecraftColor;
import com.bluepowermod.api.wire.redstone.RedwireType;
import com.bluepowermod.block.BlockWireBase;
import com.bluepowermod.reference.Refs;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

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
    public int getColor(IBlockReader w, BlockPos pos, int tint) {
        return tint == 2 ? RedwireType.RED_ALLOY.getName().equals(type) ? MinecraftColor.RED.getHex() : MinecraftColor.BLUE.getHex() : -1;
    }

    @Override
    public int getColor(int tint) {
        return tint == 2 ? RedwireType.RED_ALLOY.getName().equals(type) ? MinecraftColor.RED.getHex() : MinecraftColor.BLUE.getHex() : -1;
    }
}
