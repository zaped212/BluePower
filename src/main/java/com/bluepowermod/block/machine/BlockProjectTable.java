/*
 * This file is part of Blue Power.
 *
 *     Blue Power is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Blue Power is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Blue Power.  If not, see <http://www.gnu.org/licenses/>
 */

package com.bluepowermod.block.machine;

import com.bluepowermod.block.BlockContainerBase;
import com.bluepowermod.container.ContainerProjectTable;
import com.bluepowermod.reference.Refs;
import com.bluepowermod.tile.TileBase;
import com.bluepowermod.tile.tier1.TileProjectTable;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;


public class BlockProjectTable extends BlockContainerBase implements INamedContainerProvider {

    public BlockProjectTable() {

        super(Material.WOOD, TileProjectTable.class);
        setRegistryName(Refs.MODID, Refs.PROJECTTABLE_NAME);
    }

    @Override
    public boolean onBlockActivated(BlockState blockState, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult) {
        if (!world.isRemote) {
            NetworkHooks.openGui((ServerPlayerEntity) player, this);
            return true;
        }
        return super.onBlockActivated(blockState, world, pos, player, hand, rayTraceResult);
    }

    public BlockProjectTable(Class<? extends TileBase> tileClass) {

        super(Material.WOOD, tileClass);
    }

    @Override
    protected boolean canRotateVertical() {

        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new StringTextComponent(Refs.PROJECTTABLE_NAME);
    }

    @Nullable
    @Override
    public Container createMenu(int id, PlayerInventory inventory, PlayerEntity playerEntity) {
        return new ContainerProjectTable(id, inventory);
    }
}
