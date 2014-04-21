package assets.generator;

/*
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import cpw.mods.fml.common.registry.GameData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;

import java.io.*;
import java.util.*;

/**
 * Scan command for players to get templates from structures built in game in the specified location
 */
public class CommandScan extends CommandBase{
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender commandSender){
        return commandSender instanceof EntityPlayer;
    }

    @Override
    public String getCommandName() {
        return "scantml";
    }

    @Override
    public String getCommandUsage(ICommandSender var1) {
        return "/" + getCommandName() + " <templatetype:wall/building> <templatename> <x0,x1> <y0,y1> <z0,z1>";
    }

    @Override
    public void processCommand(ICommandSender var1, String[] command) {
        if(command.length == 5){
            int minX = parseInt(var1, command[2].split(",")[0]);
            int maxX = parseInt(var1, command[2].split(",")[1]);
            if(minX>maxX){
                int temp = maxX;
                maxX = minX;
                minX = temp;
            }
            int minY = parseInt(var1, command[3].split(",")[0]);
            int maxY = parseInt(var1, command[3].split(",")[1]);
            if(minY>maxY){
                int temp = maxY;
                maxY = minY;
                minY = temp;
            }
            int minZ = parseInt(var1, command[4].split(",")[0]);
            int maxZ = parseInt(var1, command[4].split(",")[1]);
            if(minZ>maxZ){
                int temp = maxZ;
                maxZ = minZ;
                minZ = temp;
            }
            File template = new File(BuildingExplorationHandler.CONFIG_DIRECTORY, command[1]+".tml");
            try{
            if(template.createNewFile()){
                List<BlockAndMeta> blocks = new ArrayList<BlockAndMeta>();
                blocks.add(new BlockAndMeta(Blocks.air, 0));
                HashMap<Integer, List<Integer>> layers = new HashMap<Integer, List<Integer>>();
                List<String> rules = new ArrayList<String>();
                HashSet<String> biomes = new HashSet<String>();
                if(command[0].equals("building")) {
                    for (int y = minY; y <= maxY; y++) {
                        layers.put(y - minY, new ArrayList<Integer>());
                        for (int z = minZ; z <= maxZ; z++) {
                            for (int x = minX; x <= maxX; x++) {
                                BlockAndMeta blc = new BlockAndMeta(var1.getEntityWorld().getBlock(x, y, z), var1.getEntityWorld().getBlockMetadata(x, y, z));
                                if (!blocks.contains(blc)) {
                                    rules.add("rule" + blocks.size() + "=0,100," + GameData.blockRegistry.getNameForObject(blc.get()) + "-" + blc.getMeta());
                                    blocks.add(blc);
                                }
                                layers.get(y - minY).add(blocks.indexOf(blc));
                                biomes.add(var1.getEntityWorld().getBiomeGenForCoords(x, z).biomeName);
                            }
                        }
                    }
                }else if(command[0].equals("wall")){
                    for (int x = minX; x <= maxX; x++) {
                        layers.put(x - minX, new ArrayList<Integer>());
                        for (int z = minZ; z <= maxZ; z++) {
                            for (int y = minY; y <= maxY; y++) {
                                BlockAndMeta blc = new BlockAndMeta(var1.getEntityWorld().getBlock(x, y, z), var1.getEntityWorld().getBlockMetadata(x, y, z));
                                if (!blocks.contains(blc)) {
                                    rules.add("rule" + blocks.size() + "=0,100," + GameData.blockRegistry.getNameForObject(blc.get()) + "-" + blc.getMeta());
                                    blocks.add(blc);
                                }
                                layers.get(x - minX).add(blocks.indexOf(blc));
                                biomes.add(var1.getEntityWorld().getBiomeGenForCoords(x, z).biomeName);
                            }
                        }
                    }
                }else {
                    throw new WrongUsageException(getCommandUsage(var1));
                }
                PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(template)));
                writer.println("weight=1");
                writer.println("embed_into_distance=4");
                writer.println("max_leveling=2");
                int SIZE_0 = command[0].equals("wall")?maxX-minX+1:command[0].equals("building")?maxY-minY+1:0;
                int SIZE_1 = command[0].equals("wall")?maxY-minY+1:command[0].equals("building")?maxX-minX+1:0;
                int SIZE_2 = maxZ-minZ+1;
                writer.println("dimensions="+SIZE_0+","+SIZE_1+","+SIZE_2);
                StringBuffer buf = new StringBuffer("biomes=");
                for(String biome:biomes){
                    buf.append(biome).append(",");
                }
                buf.deleteCharAt(buf.length()-1);
                writer.println(buf.toString());
                if(command[0].equals("wall")){
                    writer.println("building_templates=NONE");
                    writer.println("min_length=100");
                    writer.println("building_interval=45");
                    writer.println("max_length=300");
                    writer.println("make_buildings=1");
                    writer.println("make_end_towers=1");
                    writer.println("tower_rule=1");
                    writer.println("default_tower_weight=1");
                    writer.println("spawner_rule=0,10,52-0#pig");
                    writer.println("spawner_count=3");
                    writer.println("circular_probability=0.25");
                    writer.println("square_min_height=10");
                    writer.println("square_max_height=10");
                    writer.println("square_min_width=7");
                    writer.println("square_max_width=7");
                    writer.println("square_roof_styles=Crenel,Steep");
                    writer.println("circular_tower_min_height=8");
                    writer.println("circular_tower_max_height=15");
                    writer.println("circular_tower_min_width=10");
                    writer.println("circular_tower_max_width=10");
                    writer.println("circular_tower_roof_styles=Crenel,Dome");
                }
                for(String rule:rules){
                    writer.println(rule);
                }
                for(int index:layers.keySet()){
                    writer.println("layer");
                    for(int j = 0; j<SIZE_1; j++){
                        buf = new StringBuffer();
                        for(int i = 0; i<SIZE_2; i++){
                            int id = layers.get(index).get(i+j*SIZE_2);
                            buf.append(id);
                            if(i<SIZE_2-1){
                                buf.append(",");
                            }
                        }
                        writer.println(buf.toString());
                    }
                    writer.println("endlayer");
                }
                writer.close();
                notifyAdmins(var1, "Template scanner used by " + var1.getCommandSenderName(), var1.getCommandSenderName(), command);
            }
            }catch (IOException io){}
        } else {
            throw new WrongUsageException(getCommandUsage(var1));
        }
    }

    @Override
    public List addTabCompletionOptions(ICommandSender var1, String[] var2) {
        return var2.length == 0 ? getListOfStringsMatchingLastWord(var2, "wall", "building") : var2.length == 1 ? getListOfStringsMatchingLastWord(var2, "TemplateDefault") : null;
    }

    @Override
    public int compareTo(Object par1Obj){
        return this.compareTo((ICommand)par1Obj);
    }
}
