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

package com.bluepowermod.client.gui;

import com.bluepowermod.container.ContainerAlloyFurnace;
import com.bluepowermod.container.ContainerBlulectricFurnace;
import com.bluepowermod.reference.Refs;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

/**
 * @author MineMaarten
 */
public class GuiBlulectricFurnace extends GuiContainerBaseBP<ContainerBlulectricFurnace> implements IHasContainer<ContainerBlulectricFurnace> {

    private static final ResourceLocation resLoc = new ResourceLocation(Refs.MODID, "textures/gui/powered_furnace.png");
    private final ContainerBlulectricFurnace furnace;

    public GuiBlulectricFurnace(ContainerBlulectricFurnace container, PlayerInventory playerInventory, ITextComponent title){
        super(container, playerInventory, title, resLoc);
        this.furnace = container;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY){

        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);

        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;

        int bufferPercentage = (int)(furnace.getBufferPercentage() * 50);
        if (bufferPercentage > 0)
            this.blit(x + 25, y + 72 - bufferPercentage, 176, 65 - bufferPercentage, 5, bufferPercentage);

        double max = 0.55;
        double min = 0.49;
        int energyPercentage = (int)(Math.abs(Math.max(min,Math.min(furnace.getBufferPercentage(),max))-min)/Math.abs(max-min) * 50);
        if (energyPercentage > 0)
            this.blit(x + 33, y + 72 - energyPercentage, 176, 65 - energyPercentage, 5, energyPercentage);

        if(furnace.getBufferPercentage() > 0.5)
            this.blit(x + 24,y + 11,183,18,7,10);

        if(furnace.getBufferPercentage() > 0.55)
            this.blit(x + 34,y + 9,184,32,7,12);

        int processPercentage = (int)(furnace.getProcessPercentage() * 22);
        this.blit(x + 90, y + 35, 178, 0, processPercentage, 15);
    }

}
