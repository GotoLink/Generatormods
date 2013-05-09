package mods.generator;

import java.util.List;
import java.util.Random;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

public class CommandBuild extends CommandBase{
	private BuildingExplorationHandler ruin,wall,city;
	private World world;
	@Override
	public int getRequiredPermissionLevel()
    {
        return 2;
    }
	
	public String getCommandName() {
		return "build";
	}

	public String getCommandUsage(ICommandSender commandSender) {
		return "/" + getCommandName() + " <ruin:wall:city> <x> <z>";
	}

	public List getCommandAliases() {
		return null;
	}
	//TODO
	public void processCommand(ICommandSender var1, String[] coordinate) {
		if (coordinate.length == 3)
        {	//int bound = 10000000;
            int posX = Integer.parseInt(coordinate[1]);//parseIntBounded(var1, coordinate[1],-bound,bound);
            int posZ = Integer.parseInt(coordinate[2]);//parseIntBounded(var1, coordinate[2],-bound,bound);
            world=MinecraftServer.getServer().worldServers[0];
            
            if ("ruin".equalsIgnoreCase(coordinate[0]))
            {  
            	ruin=PopulatorCARuins.instance;
            	ruin.exploreThreads.add(new WorldGenCARuins((PopulatorCARuins) ruin, world,new Random(), posX, posZ, 100, 1));	
            }
            else if ("wall".equalsIgnoreCase(coordinate[0]))
            {
            	wall=PopulatorGreatWall.instance;
            	wall.exploreThreads.add(new WorldGenGreatWall((PopulatorGreatWall) wall, world, new Random(), posX, posZ, 100, 1));         	            	
            }
            else if ("city".equalsIgnoreCase(coordinate[0]))
            {
            	city=PopulatorWalledCity.instance;
            	city.exploreThreads.add(new WorldGenWalledCity((PopulatorWalledCity) city, world, new Random(), posX, posZ, 100, 1));
        		
            }
        }
        else
        {
            throw new WrongUsageException(getCommandUsage(var1));
        }
		
	}

	@Override
	public List addTabCompletionOptions(ICommandSender var1, String[] var2) {
		return var2.length == 1 ? getListOfStringsMatchingLastWord(var2, new String[] {"ruin", "wall", "city"}): null;
	    
	}

}
