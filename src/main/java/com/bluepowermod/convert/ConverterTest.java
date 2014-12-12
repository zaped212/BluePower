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

package com.bluepowermod.convert;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import uk.co.qmunity.lib.part.IPart;
import uk.co.qmunity.lib.part.PartBase;

public class ConverterTest implements IPartConverter {

    @Override
    public boolean matches(String id) {

        return id.equals("mcr_face");
    }

    @Override
    public IPart convert(NBTTagCompound old) {

        System.out.println("Converting " + old);

        return new PartBase() {

            @Override
            public String getType() {

                return "TestPart";
            }

            @Override
            public ItemStack getItem() {

                return null;
            }

            @Override
            public void writeToNBT(NBTTagCompound tag) {

                super.writeToNBT(tag);

                tag.setString("TestString", "TestValue");
            }
        };
    }

}