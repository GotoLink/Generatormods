package assets.generator;

/*
 *  Source code for the The Great Wall Mod, CellullarAutomata Ruins and Walled City Generator Mods for the game Minecraft
 *  Copyright (C) 2011 by Formivore - 2012 by GotoLink
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Random;

/**
 * Defines a loot whose final size is randomized within a range of values
 */
public class RandomLoot implements RandomPicker.IWeighted {
    final ItemStack loot;
    int weight, minSize, maxSize;
    public RandomLoot(Object blockOrItem, int damage, int weight, int minSize, int maxSize){
        if(blockOrItem instanceof Block)
            this.loot = new ItemStack((Block)blockOrItem, 0, damage);
        else if(blockOrItem instanceof Item)
            this.loot = new ItemStack((Item)blockOrItem, 0, damage);
        else
            throw new IllegalArgumentException(String.valueOf(blockOrItem));
        this.weight = weight;
        checkSize(minSize, maxSize);
    }

    public RandomLoot(Object...arg){
        this(arg[0], ((Integer)arg[1]).intValue(), ((Integer)arg[2]).intValue(), ((Integer)arg[3]).intValue(), ((Integer)arg[4]).intValue());
    }

    public RandomLoot(String text) throws IllegalArgumentException{
        String[] intStrs = text.trim().split(",", 5);
        if(intStrs.length<4)
            throw new IllegalArgumentException("Wrong number of separators in line");
        int index = intStrs[0].lastIndexOf("-");
        String[] idAndMeta;
        if(index!=-1) {
            idAndMeta = new String[]{intStrs[0].substring(0, index), intStrs[0].substring(index+1)};
        }else{
            idAndMeta = new String[]{intStrs[0]};
        }
        Object temp;
        try{
            int i = Integer.parseInt(idAndMeta[0]);
            temp = GameData.getItemRegistry().getObjectById(i);
            if(temp==null){
                temp = GameData.getBlockRegistry().getObjectById(i);
            }
        }catch (Throwable e){
            temp = GameData.getItemRegistry().getObject(idAndMeta[0]);
            if(temp==null){
                temp = GameData.getBlockRegistry().getObject(idAndMeta[0]);
            }
        }
        int meta = idAndMeta.length > 1 ? Integer.parseInt(idAndMeta[1]) : 0;
        if(temp instanceof Item)
            loot = new ItemStack((Item)temp, 0, meta);
        else if(temp!=null && temp != Blocks.air)
            loot = new ItemStack((Block)temp, 0, meta);
        else
            throw new IllegalArgumentException("Can't find item or block with name: "+idAndMeta[0]);
        weight = Integer.parseInt(intStrs[1]);
        checkSize(Integer.parseInt(intStrs[2]), Integer.parseInt(intStrs[3]));
        if(intStrs.length>4){
            try{
                NBTBase nbtbase = JsonToNBT.func_150315_a(intStrs[4]);
                if (nbtbase instanceof NBTTagCompound)
                    loot.setTagCompound((NBTTagCompound)nbtbase);
            }catch (Throwable exception){
                throw new IllegalArgumentException("Invalid JSon format", exception);
            }
        }
    }

    private void checkSize(int a, int b){
        if(a<b) {
            this.minSize = a;
            this.maxSize = b;
        }else{
            this.minSize = b;
            this.maxSize = a;
        }
        this.maxSize = this.maxSize > 64 ? 64 : (this.maxSize < 0 ? 0 : this.maxSize);
        this.minSize = this.minSize > 64 ? 64 : (this.minSize < 0 ? 0 : this.minSize);
        if(this.minSize==0 && this.maxSize==0)
            this.weight = 0;
    }

    public ItemStack getLoot(Random random){
        loot.stackSize = minSize + random.nextInt(maxSize-minSize+1);
        return loot.copy();
    }

    @Override
    public int getWeight(){
        return weight;
    }

    @Override
    public String toString(){
        return loot.getDisplayName()+"-"+loot.getItemDamage()+","+weight+","+minSize+","+maxSize;
    }
}
