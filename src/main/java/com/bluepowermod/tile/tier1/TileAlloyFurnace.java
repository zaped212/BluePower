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

package com.bluepowermod.tile.tier1;

import com.bluepowermod.api.recipe.IAlloyFurnaceRecipe;
import com.bluepowermod.block.machine.BlockAlloyFurnace;
import com.bluepowermod.container.ContainerAlloyFurnace;
import com.bluepowermod.recipe.AlloyFurnaceRegistry;
import com.bluepowermod.reference.Refs;
import com.bluepowermod.tile.BPTileEntityType;
import com.bluepowermod.tile.TileBase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.FurnaceTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 *
 * @author MineMaarten, Koen Beckers (K4Unl), amadornes
 */

public class TileAlloyFurnace extends TileBase implements ISidedInventory, INamedContainerProvider {

    private boolean isActive;
    private int currentBurnTime;
    private int currentProcessTime;
    private int maxBurnTime;
    public static final int SLOTS = 11;
    private NonNullList<ItemStack> inventory;
    private ItemStack fuelInventory;
    private ItemStack outputInventory;
    private IAlloyFurnaceRecipe currentRecipe;
    private boolean updatingRecipe = true;

    public TileAlloyFurnace() {
        super(BPTileEntityType.ALLOY_FURNACE);
        this.inventory = NonNullList.withSize(9, ItemStack.EMPTY);
        this.fuelInventory = ItemStack.EMPTY;
        this.outputInventory = ItemStack.EMPTY;
    }

    /*************** BASIC TE FUNCTIONS **************/

    /**
     * This function gets called whenever the world/chunk loads
     */
    @Override
    public void read(CompoundNBT tCompound) {

        super.read(tCompound);

        for (int i = 0; i < 9; i++) {
            CompoundNBT tc = tCompound.getCompound("inventory" + i);
            inventory.set(i, ItemStack.read(tc));
        }
        fuelInventory = ItemStack.read(tCompound.getCompound("fuelInventory"));
        outputInventory = ItemStack.read(tCompound.getCompound("outputInventory"));

    }

    /**
     * This function gets called whenever the world/chunk is saved
     */
    @Override
    public CompoundNBT write(CompoundNBT tCompound) {

        super.write(tCompound);

        for (int i = 0; i < 9; i++) {
            CompoundNBT tc = new CompoundNBT();
            inventory.get(i).write(tc);
            tCompound.put("inventory" + i, tc);
        }
        if (fuelInventory != null) {
            CompoundNBT fuelCompound = new CompoundNBT();
            fuelInventory.write(fuelCompound);
            tCompound.put("fuelInventory", fuelCompound);
        }

        if (outputInventory != null) {
            CompoundNBT outputCompound = new CompoundNBT();
            outputInventory.write(outputCompound);
            tCompound.put("outputInventory", outputCompound);
        }
        return tCompound;

    }

    @Override
    public void readFromPacketNBT(CompoundNBT tag) {

        super.readFromPacketNBT(tag);
        isActive = tag.getBoolean("isActive");
        currentBurnTime = tag.getInt("currentBurnTime");
        currentProcessTime = tag.getInt("currentProcessTime");
        maxBurnTime = tag.getInt("maxBurnTime");
        markForRenderUpdate();
    }

    @Override
    public void writeToPacketNBT(CompoundNBT tag) {

        super.writeToPacketNBT(tag);
        tag.putInt("currentBurnTime", currentBurnTime);
        tag.putInt("currentProcessTime", currentProcessTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putBoolean("isActive", isActive);
    }

    /**
     * Function gets called every tick. Do not forget to call the super method!
     */
    @Override
    public void tick() {

        super.tick();

        if (!world.isRemote) {
            setIsActive(currentBurnTime > 0);
            if (isActive) {
                currentBurnTime--;
            }
            if (updatingRecipe) {
                if(this.world.getRecipeManager().getRecipe(AlloyFurnaceRegistry.ALLOYFURNACE_RECIPE, this, this.world).isPresent()) {
                    currentRecipe = (IAlloyFurnaceRecipe) this.world.getRecipeManager().getRecipe(AlloyFurnaceRegistry.ALLOYFURNACE_RECIPE, this, this.world).get();
                }else{
                    currentRecipe = null;
                }
                updatingRecipe = false;
            }
            if (currentRecipe != null) {
                if (currentBurnTime <= 0) {
                    if (FurnaceTileEntity.getBurnTimes().containsKey(fuelInventory.getItem())) {
                        // Put new item in
                        currentBurnTime = maxBurnTime = FurnaceTileEntity.getBurnTimes().get(fuelInventory.getItem());
                        if (!fuelInventory.isEmpty()) {
                            fuelInventory.setCount(fuelInventory.getCount() - 1);
                            if (fuelInventory.getCount() <= 0) {
                                fuelInventory = fuelInventory.getItem().getContainerItem(fuelInventory);
                            }
                        }
                    } else {
                        currentProcessTime = 0;
                    }
                }

                //Check if progress completed, and output slot is empty and less then a stack of the same item.
                if (++currentProcessTime >= 200 && ((outputInventory.getItem() == currentRecipe.getRecipeOutput().getItem()
                        && (outputInventory.getCount() + currentRecipe.getCraftingResult(inventory).getCount()) <= 64)
                                        || outputInventory.isEmpty())) {
                    currentProcessTime = 0;
                    if (!outputInventory.isEmpty()) {
                        outputInventory.setCount(outputInventory.getCount() + currentRecipe.getCraftingResult(inventory).getCount());
                    } else {
                        outputInventory = currentRecipe.getCraftingResult(inventory).copy();
                    }
                    currentRecipe.useItems(inventory);
                    updatingRecipe = true;
                }
            } else {
                currentProcessTime = 0;
            }
        }
    }

    @Override
    protected void redstoneChanged(boolean newValue) {

        // setIsActive(newValue);
    }

    protected final IIntArray fields = new IIntArray() {
        public int get(int i) {
            switch (i) {
                case 0:
                    return TileAlloyFurnace.this.currentBurnTime;
                case 1:
                    return TileAlloyFurnace.this.currentProcessTime;
                case 2:
                    return TileAlloyFurnace.this.maxBurnTime;
                default:
                    return 0;
            }
        }

        public void set(int i, int value) {
            switch (i) {
                case 0:
                    TileAlloyFurnace.this.currentBurnTime = value;
                    break;
                case 1:
                    TileAlloyFurnace.this.currentProcessTime = value;
                    break;
                case 2:
                    TileAlloyFurnace.this.maxBurnTime = value;
            }

        }

        public int size() {
            return 3;
        }
    };

    /**
     * ************* ADDED FUNCTIONS *************
     */

    public boolean getIsActive() {

        return isActive;
    }

    public void setIsActive(boolean _isActive) {

        if (_isActive != isActive) {
            isActive = _isActive;
            BlockAlloyFurnace.setState(isActive, world, pos);
            sendUpdatePacket();
        }
    }

    /**
     * ************ IINVENTORY ****************
     */

    @Override
    public int getSizeInventory() {

        return 9 + 1 + 1; // 9 inventory, 1 fuel, 1 output
    }

    @Override
    public ItemStack getStackInSlot(int var1) {
        updatingRecipe = true;
        if (var1 == 0) {
            return fuelInventory;
        } else if (var1 == 1) {
            return outputInventory;
        } else if (var1 < 11) {
            return inventory.get(var1 - 2);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {

        ItemStack itemStack = getStackInSlot(slot);
        if (!itemStack.isEmpty()) {
            if (itemStack.getCount() <= amount) {
                setInventorySlotContents(slot, ItemStack.EMPTY);
            } else {
                itemStack = itemStack.split(amount);
                if (itemStack.getCount() == 0) {
                    setInventorySlotContents(slot, ItemStack.EMPTY);
                }
            }
        }

        return itemStack;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return getStackInSlot(index);
    }

    @Override
    public void setInventorySlotContents(int var1, ItemStack itemStack) {

        if (var1 == 0) {
            fuelInventory = itemStack;
        } else if (var1 == 1) {
            outputInventory = itemStack;
        } else {
            inventory.set(var1 - 2, itemStack);
        }
        updatingRecipe = true;
    }

    @Override
    public int getInventoryStackLimit() {

        return 64;
    }

    @Override
    public boolean isUsableByPlayer(PlayerEntity player) {
        return player.getPosition().withinDistance(pos, 64.0D);
    }

    @Override
    public void openInventory(PlayerEntity player) {

    }

    @Override
    public void closeInventory(PlayerEntity player) {

    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack itemStack) {

        if (slot == 0) {
            return FurnaceTileEntity.isFuel(itemStack);
        } else if (slot == 1) { // Output slot
            return false;
        } else {
            return true;
        }
    }

    @Override
    public NonNullList<ItemStack> getDrops() {

        NonNullList<ItemStack> drops = super.getDrops();
        if (!fuelInventory.isEmpty())
            drops.add(fuelInventory);
        if (!outputInventory.isEmpty())
            drops.add(outputInventory);
        for (ItemStack stack : inventory)
            if (!stack.isEmpty())
                drops.add(stack);
        return drops;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack item, Direction direction) {
        return isItemValidForSlot(slot, item);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, Direction direction) {
        return slot == 1;
    }

    //Todo Fields
    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public void clear() {

    }

    @Override
    public ITextComponent getDisplayName() {
        return new StringTextComponent(Refs.ALLOYFURNACE_NAME);
    }

    @Nullable
    @Override
    public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player) {
        return new ContainerAlloyFurnace(id, inventory, this, fields);
    }
}
