package com.bluepowermod.block.machine;

import com.bluepowermod.block.BlockWireBase;
import com.bluepowermod.reference.Refs;
import net.minecraft.state.BooleanProperty;

public class BlockAlloyWire extends BlockWireBase{
    private final String type;

    public BlockAlloyWire(String type) {
        //super( 2, 0, 2 );
        this.type = type;
        setRegistryName(Refs.MODID + ":" + type + "_wire");
    }
}
