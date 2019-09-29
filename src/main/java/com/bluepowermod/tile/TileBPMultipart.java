/*
 * This file is part of Blue Power. Blue Power is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Blue Power is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along
 * with Blue Power. If not, see <http://www.gnu.org/licenses/>
 */

package com.bluepowermod.tile;

import com.bluepowermod.block.BlockBPMultipart;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.extensions.IForgeBlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author MoreThanHidden
 */
public class TileBPMultipart extends TileEntity implements ITickableTileEntity {

    public static final ModelProperty<Map<BlockState, IModelData>> PROPERTY_INFO = new ModelProperty<>();
    private Map<BlockState, TileEntity> stateMap = new HashMap<>();

    public TileBPMultipart() {
        super(BPTileEntityType.MULTIPART);
    }

    @Nonnull
    @Override
    public IModelData getModelData() {
        //Get Model Data for States with Tile Entities in the Multipart
        Map<BlockState, IModelData> modelDataMap = stateMap.keySet().stream().filter(IForgeBlockState::hasTileEntity)
                .collect(Collectors.toMap(s -> s, this::getModelData));

        //Add States without Tile Entities
        stateMap.keySet().stream().filter(s -> !s.hasTileEntity()).forEach(s -> modelDataMap.put(s, null));

        return new ModelDataMap.Builder().withInitial(PROPERTY_INFO, modelDataMap).build();
    }

    private IModelData getModelData(BlockState state) {
        //Get Model Data for specific state
        return stateMap.get(state).getModelData();
    }

    public void addState(BlockState state) {
        TileEntity tile = null;
        if(state.hasTileEntity()){
            tile = state.getBlock().createTileEntity(state, world);
            if (tile != null) {
                tile.setPos(pos);
                if (world != null) {
                    tile.setWorld(world);
                }
            }
        }
        this.stateMap.put(state, tile);
        markDirtyClient();
    }

    public void removeState(BlockState state) {
        stateMap.get(state).remove();
        this.stateMap.remove(state);
        markDirtyClient();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        List<LazyOptional<T>> capability =  stateMap.values().stream().filter(Objects::nonNull).map(t -> t.getCapability(cap, side)).filter(LazyOptional::isPresent).collect(Collectors.toList());
        return capability.size() > 0 ? capability.get(0) : LazyOptional.empty();
    }

    public List<BlockState> getStates(){
        return new ArrayList<>(stateMap.keySet());
    }

    private void markDirtyClient() {
        markDirty();
        if (getWorld() != null) {
            BlockState state = getWorld().getBlockState(getPos());
            getWorld().notifyBlockUpdate(getPos(), state, state, 3);
        }
        this.requestModelDataUpdate();
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);
        compound.putInt("size", getStates().size());
        for (int i = 0; i < getStates().size(); i++) {
            //write state data
            compound.put("state" + i, BlockState.serialize(NBTDynamicOps.INSTANCE, getStates().get(i)).getValue());
            //write tile NBT data
            if(stateMap.get(getStates().get(i)) != null)
                compound.put("tile" + i, stateMap.get(getStates().get(i)).write(new CompoundNBT()));
        }
        return compound;
    }

    @Override
    public void read(CompoundNBT compound) {
        super.read(compound);
        Map<BlockState, TileEntity> states = new HashMap<>();
        int size = compound.getInt("size");
        for (int i = 0; i < size; i++) {
            BlockState state = BlockState.deserialize(new Dynamic<>(NBTDynamicOps.INSTANCE, compound.get("state" + i)));
            TileEntity tile = state.getBlock().createTileEntity(state, world);
            if (tile != null) {
                tile.read(compound.getCompound("tile" + i));
                tile.setPos(pos);
            }
            states.put(state, tile);
        }
        this.stateMap = states;
        markDirtyClient();
    }

    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT updateTag = super.getUpdateTag();
        write(updateTag);
        return updateTag;
    }

    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        CompoundNBT nbtTag = new CompoundNBT();
        write(nbtTag);
        return new SUpdateTileEntityPacket(getPos(), 1, nbtTag);
    }

    @Override
    public void onDataPacket(NetworkManager networkManager, SUpdateTileEntityPacket packet) {
        List<BlockState> states = getStates();
        CompoundNBT tagCompound = packet.getNbtCompound();
        super.onDataPacket(networkManager, packet);
        read(tagCompound);
        if (world.isRemote) {
            // Update if needed
            if (!getStates().equals(states)) {
                world.markChunkDirty(getPos(), this.getTileEntity());
            }
        }
    }

    public void changeState(BlockState state, BlockState newState) {
        TileEntity te = stateMap.get(state);
        stateMap.remove(state);
        stateMap.put(newState, te);
        markDirtyClient();
    }

    @Override
    public void tick() {
      //Tick the Tickable Multiparts
      stateMap.values().stream().filter(t -> t instanceof ITickableTileEntity)
              .forEach(t-> ((ITickableTileEntity)t).tick());
    }
}
