package com.bluepowermod.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundNBT;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PacketHelper {

    public static void writeBytes(DataOutput out, byte[] bytes) throws IOException {

        out.writeInt(bytes.length);
        out.write(bytes);
    }

    public static byte[] readBytes(DataInput in) throws IOException {

        int amt = in.readInt();
        byte[] bytes = new byte[amt];
        in.readFully(bytes, 0, amt);
        return bytes;
    }

    public static void writeNBT(DataOutput out, CompoundNBT tag) throws IOException {

        ByteBuf buf = Unpooled.buffer();
        //ByteBufUtils.write(buf, tag);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        writeBytes(out, bytes);
    }

    public static CompoundNBT readNBT(DataInput in) throws IOException {

        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(readBytes(in));
        //CompoundNBT t = ByteBufUtils.read(buf);
        return new CompoundNBT();
    }
}
