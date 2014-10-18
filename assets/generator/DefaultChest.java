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
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;

import java.io.PrintWriter;

/**
 * Concentrate all default values regarding chests
 */
public class DefaultChest {
    public final static String[] LABELS = new String[] { "EASY", "MEDIUM", "HARD", "TOWER" };
    public final static int[] TRIES = new int[] { 4, 6, 6, 6 };
    // chest items[n] format is array of 5 arrays
    // 0array - blockId
    // 1array - block damage/meta
    // 2array - block weight
    // 3array - block min stacksize
    // 4array - block max stacksize
    public final static Object[][][] ITEMS = new Object[][][] {
            { // Easy
                    { Items.arrow, 0, 2, 1, 12 }, { Items.iron_sword, 0, 2, 1, 1 }, { Items.leather_leggings, 0, 1, 1, 1 }, { Items.iron_shovel, 0, 1, 1, 1 },
                    { Items.string, 0, 1, 1, 1 }, { Items.iron_pickaxe, 0, 2, 1, 1 }, { Items.leather_boots, 0, 1, 1, 1 }, { Items.bucket, 0, 1, 1, 1 },
                    { Items.leather_helmet, 0, 1, 1, 1 }, { Items.wheat_seeds, 0, 1, 10, 15 }, { Items.gold_nugget, 0, 2, 3, 8 }, { Items.potionitem, 5, 2, 1, 1 }, // healing I
                    { Items.potionitem, 4, 1, 1, 1 } }, // poison, hehe
            { // Medium
                    { Items.golden_sword, 0, 2, 1, 1 }, { Items.milk_bucket, 0, 2, 1, 1 }, { Blocks.web, 0, 1, 8, 16 }, { Items.golden_shovel, 0, 1, 1, 1 },
                    { Items.golden_hoe, 0, 1, 0, 1 }, { Items.clock, 0, 1, 1, 1 }, { Items.iron_axe, 0, 3, 1, 1 }, { Items.map, 0, 1, 1, 1 },
                    { Items.apple, 0, 2, 2, 3 }, { Items.compass, 0, 1, 1, 1 }, { Items.iron_ingot, 0, 1, 5, 8 }, { Items.slime_ball, 0, 1, 1, 3 },
                    { Blocks.obsidian, 0, 1, 1, 4 }, { Items.bread, 0, 2, 8, 15 }, { Items.potionitem, 2, 1, 1, 1 }, { Items.potionitem, 37, 3, 1, 1 }, // healing II
                    { Items.potionitem, 34, 1, 1, 1 }, // swiftness II
                    { Items.potionitem, 9, 1, 1, 1 } }, // strength
            { // Hard
                    { Blocks.sticky_piston, 0, 2, 6, 12 }, { Blocks.web, 0, 1, 8, 24 }, { Items.cookie, 0, 2, 8, 18 }, { Items.diamond_axe, 0, 1, 1, 1 },
                    { Items.minecart, 0, 1, 12, 24 }, { Items.redstone, 0, 2, 12, 24 }, { Items.lava_bucket, 0, 2, 1, 1 }, { Items.ender_pearl, 0, 1, 1, 1 },
                    { Blocks.mob_spawner, 0, 1, 2, 4 }, { Items.record_13, 0, 1, 1, 1 }, { Items.golden_apple, 0, 1, 4, 8 }, { Blocks.tnt, 0, 2, 8, 20 },
                    { Items.diamond, 0, 2, 1, 4 }, { Items.gold_ingot, 0, 2, 30, 64 }, { Items.potionitem, 37, 3, 1, 1 }, // healing II
                    { Items.potionitem, 49, 2, 1, 1 }, // regeneration II
                    { Items.potionitem, 3, 2, 1, 1 } }, // fire resistance
            { // Tower
                    { Items.arrow, 0, 1, 1, 12 }, { Items.fish, 0, 2, 1, 1 }, { Items.golden_helmet, 0, 1, 1, 1 }, { Blocks.web, 0, 1, 1, 12 },
                    { Items.iron_ingot, 0, 1, 2, 3 }, { Items.stone_sword, 0, 1, 1, 1 }, { Items.iron_axe, 0, 1, 1, 1 }, { Items.egg, 0, 2, 8, 16 },
                    { Items.saddle, 0, 1, 1, 1 }, { Items.wheat, 0, 2, 3, 6 }, { Items.gunpowder, 0, 1, 2, 4 }, { Items.leather_chestplate, 0, 1, 1, 1 },
                    { Blocks.pumpkin, 0, 1, 1, 5 }, { Items.gold_nugget, 0, 2, 1, 3 } } };

    public static void print(PrintWriter pw){
        pw.println();
        pw.println("<-Chest contents->");
        pw.println("<-Tries is the number of selections that will be made for this chest type.->");
        pw.println("<-Format for items is <item name-damage value>,<selection weight>,<min stack size>,<max stack size>,<json> ->");
        pw.println("<-Where <damage value> and <json> are optional. Json format follows same rules as /give command->");
        pw.println("<-So e.g. minecraft:arrow,2,1,12 means a stack of between 1 and 12 arrows, with a selection weight of 2.->");
        for (int l = 0; l < LABELS.length; l++) {
            pw.println("CHEST_" + LABELS[l]);
            pw.println("Tries:" + TRIES[l]);
            for (int m = 0; m < ITEMS[l].length; m++) {
                try{
                    String txt = GameData.getItemRegistry().getNameForObject(ITEMS[l][m][0]);
                    if(txt==null){
                        txt = GameData.getBlockRegistry().getNameForObject(ITEMS[l][m][0]);
                    }
                    if(txt!=null){
                        pw.print(txt);
                        pw.print("-" + ITEMS[l][m][1]);
                        pw.print("," + ITEMS[l][m][2]);
                        pw.print("," + ITEMS[l][m][3]);
                        pw.println("," + ITEMS[l][m][4]);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            pw.println();
        }
    }
}
