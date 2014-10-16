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

import java.util.Random;

/**
 * Created by GotoLink on 16/10/2014.
 */
public class RandomLoot {
    final ItemStack loot;
    int weight, minSize, maxSize;
    public RandomLoot(Object blockOrItem, int damage, int weight, int minSize, int maxSize){
        if(blockOrItem instanceof Block)
            this.loot = new ItemStack((Block)blockOrItem, 0, damage);
        else if(blockOrItem instanceof Item)
            this.loot = new ItemStack((Item)blockOrItem, 0, damage);
        else
            throw new IllegalArgumentException(blockOrItem.toString());
        this.weight = weight;
        checkSize(minSize, maxSize);
    }

    public RandomLoot(Object...arg){
        this(arg[0], ((Integer)arg[1]).intValue(), ((Integer)arg[2]).intValue(), ((Integer)arg[3]).intValue(), ((Integer)arg[4]).intValue());
    }

    public RandomLoot(String text){
        String[] intStrs = text.trim().split(",");
        if(intStrs.length!=4)
            throw new IllegalArgumentException("Wrong number of separators in line");
        String[] idAndMeta = intStrs[0].split("-", 2);
        Object temp;
        int[] tempArray = new int[4];
        try{
            int i = Integer.parseInt(idAndMeta[0]);
            temp = GameData.getItemRegistry().getObjectById(i);
            if(temp==null){
                temp = GameData.getBlockRegistry().getObjectById(i);
            }
        }catch (Exception e){
            temp = GameData.getItemRegistry().getObject(idAndMeta[0]);
            if(temp==null){
                temp = GameData.getBlockRegistry().getObject(idAndMeta[0]);
            }
        }
        tempArray[0] = idAndMeta.length > 1 ? Integer.parseInt(idAndMeta[1]) : 0;
        for (int m = 1; m < 4; m++)
            tempArray[m] = Integer.parseInt(intStrs[m]);
        if(temp instanceof Item)
            loot = new ItemStack((Item)temp, 0, tempArray[0]);
        else if(temp!=null && temp != Blocks.air)
            loot = new ItemStack((Block)temp, 0, tempArray[0]);
        else
            throw new IllegalArgumentException("Can't find item or block with name: "+idAndMeta[0]);
        weight = tempArray[1];
        checkSize(tempArray[2], tempArray[3]);
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
        return loot;
    }

    public int getWeight(){
        return weight;
    }

    @Override
    public String toString(){
        return GameData.getItemRegistry().getNameForObject(loot.getItem())+"-"+loot.getItemDamage()+","+weight+","+minSize+","+maxSize;
    }
}
