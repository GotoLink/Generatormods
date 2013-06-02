package mods.generator;
/*
 *  Source code for the The Great Wall Mod and Walled City Generator Mods for the game Minecraft
 *  Copyright (C) 2011 by formivore

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * PopulatorGreatWall is the main class that hooks into ModLoader for the Great Wall Mod.
 * It reads the globalSettings file and runs WorldGenWalledCities.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = "GreatWallMod", name = "Great Wall Mod", version = "0.1.1",dependencies= "after:ExtraBiomes,BiomesOPlenty")
@NetworkMod(clientSideRequired = false, serverSideRequired = false)
public class PopulatorGreatWall extends BuildingExplorationHandler{
	@Instance("GreatWallMod")
	public static PopulatorGreatWall instance;
	private final static int MAX_EXPLORATION_DISTANCE=10, HIGH_DENSITY_MAX_EXPLORATION_DISTANCE=12;
	private final static String SETTINGS_FILE_NAME="GreatWallSettings.txt",
								LOG_FILE_NAME="great_wall_log.txt",
								CITY_TEMPLATES_FOLDER_NAME="greatwall";

	//USER MODIFIABLE PARAMETERS, values below are defaults
	public float GlobalFrequency=0.005F;
	public float CurveBias=0.5F;
	public int LengthBiasNorm=200;
	public int BacktrackLength=9;

	//DATA VARIABLES
	public ArrayList<TemplateWall> wallStyles=null;
	public int[] placedCoords=null;
	public World placedWorld=null;
	@PreInit
	public void preInit(FMLPreInitializationEvent event) {	instance=this;}
	
	
	//****************************  FUNCTION - loadDataFiles *************************************************************************************//
	public final void loadDataFiles(){
		try {
			//read and check values from file
			lw= new PrintWriter( new BufferedWriter( new FileWriter(new File(BASE_DIRECTORY,LOG_FILE_NAME)) ));
			logOrPrint("Loading options and templates for the Great Wall Mod.");
			getGlobalOptions();
			
			File stylesDirectory=new File(CONFIG_DIRECTORY,CITY_TEMPLATES_FOLDER_NAME);
			wallStyles=TemplateWall.loadWallStylesFromDir(stylesDirectory,this);

			lw.println("\nTemplate loading complete.");
			lw.println("Probability of generation attempt per chunk explored is "+GlobalFrequency+", with "+TriesPerChunk+" tries per chunk.");
			if(GlobalFrequency <0.000001) errFlag=true;
		} catch( Exception e ) {
			errFlag=true;
			logOrPrint( "There was a problem loading the great wall mod: "+ e.getMessage() );
			lw.println( "There was a problem loading the great wall mod: "+ e.getMessage() );
			e.printStackTrace();
		}finally{ if(lw!=null) lw.close(); }
		dataFilesLoaded=true;
	}

	//****************************  FUNCTION - updateWorldExplored *************************************************************************************//
	public synchronized void updateWorldExplored(World world_) {
		if (checkNewWorld(world_))
		{
			setNewWorld(world_,"Starting to survey a world for wall generation...");
			
			if(this==master){
				//kill zombies
				for(WorldGeneratorThread wgt: exploreThreads) killZombie(wgt);
				exploreThreads=new LinkedList<WorldGeneratorThread>();
			} else {
				master.updateWorldExplored(world_);
				exploreThreads=master.exploreThreads;
			}
		}
	}
	
	//****************************  FUNCTION - generate *************************************************************************************//
	
	public final void generate( World world, Random random, int i, int k ) {	
		if(random.nextFloat() < GlobalFrequency)
			exploreThreads.add(new WorldGenGreatWall(this,world, random, i, k,TriesPerChunk, GlobalFrequency));		
	}

	//****************************  FUNCTION - getGlobalOptions  *************************************************************************************//
	private final void getGlobalOptions(){
		File settingsFile=new File(CONFIG_DIRECTORY,SETTINGS_FILE_NAME);
		if(settingsFile.exists()){
			BufferedReader br = null;
			try{
				br=new BufferedReader( new FileReader(settingsFile) );
				lw.println("Getting global options...");    
	
				for(String read=br.readLine(); read!=null; read=br.readLine()){
					if(read.startsWith( "GlobalFrequency" )) GlobalFrequency = readFloatParam(lw,GlobalFrequency,":",read);
					if(read.startsWith( "TriesPerChunk" )) TriesPerChunk = readIntParam(lw,TriesPerChunk,":",read);
					if(read.startsWith( "AllowedDimensions" )) AllowedDimensions = readIntList(lw,AllowedDimensions,":",read);
					if(read.startsWith( "CurveBias" )) CurveBias = readFloatParam(lw,CurveBias,":",read);
					if(read.startsWith( "LengthBiasNorm" )) LengthBiasNorm = readIntParam(lw,LengthBiasNorm,":",read);
					if(read.startsWith( "BacktrackLength" )) BacktrackLength = readIntParam(lw,BacktrackLength,":",read);
					if(read.startsWith( "LogActivated" )) logActivated = readBooleanParam(lw,logActivated,":",read);
					if(read.startsWith( "ChatMessage" )) chatMessage = readBooleanParam(lw,chatMessage,":",read);
					
					readChestItemsList(lw,read,br);					
				}
				
				if(TriesPerChunk > MAX_TRIES_PER_CHUNK) TriesPerChunk = MAX_TRIES_PER_CHUNK;
				if(CurveBias<0.0) CurveBias=0.0F;
				if(CurveBias>1.0) CurveBias=1.0F;
			}catch(IOException e) { lw.println(e.getMessage()); }
			finally{ try{ if(br!=null) br.close();} catch(IOException e) {} }
		}
		else{
			copyDefaultChestItems();
			PrintWriter pw=null;
			try{
				pw=new PrintWriter( new BufferedWriter( new FileWriter(settingsFile) ) );
				pw.println("<-README: This file should be in the config/generatormods folder->");
				pw.println();
				pw.println("<-GlobalFrequency controls how likely walls are to appear. Should be between 0.0 and 1.0. Lower to make less common->");
				pw.println("<-TriesPerChunk allows multiple attempts per chunk. Only change from 1 if you want very dense walls!->");
				pw.println("GlobalFrequency:"+GlobalFrequency);
				pw.println("TriesPerChunk:"+TriesPerChunk);
				pw.println("AllowedDimensions:"+Arrays.toString(AllowedDimensions).replace("[", "").replace("]", "").trim());
				pw.println();
				pw.println("<-BacktrackLength - length of backtracking for wall planning if a dead end is hit->");
				pw.println("<-CurveBias - strength of the bias towards curvier walls. Value should be between 0.0 and 1.0.->");
				pw.println("<-LengthBiasNorm - wall length at which there is no penalty for generation>");
				pw.println("BacktrackLength:"+BacktrackLength);
				pw.println("CurveBias:"+CurveBias);
				pw.println("LengthBiasNorm:"+LengthBiasNorm);
				pw.println("<-LogActivated controls information stored into forge logs. Set to true if you want to report an issue with complete forge logs.->");
				pw.println("LogActivated:"+logActivated);							
				pw.println("<-ChatMessage controls lag warnings.->");
				pw.println("ChatMessage:"+chatMessage);
				pw.println();
				printDefaultChestItems(pw);
				//printDefaultBiomes(pw);		
			}
			catch(Exception e) { lw.println(e.getMessage()); }
			finally{ if(pw!=null) pw.close(); }
			
			//reduce max exploration distance for infinite cities to improve performance
			if(GlobalFrequency > 0.05) max_exploration_distance=HIGH_DENSITY_MAX_EXPLORATION_DISTANCE;
		}	
	}
	@Override
	public String toString(){
		return "GreatWallMod";
	}
	

	@PostInit
	public void modsLoaded(FMLPostInitializationEvent event)
	{		
		if(!dataFilesLoaded)
			loadDataFiles();
		if(!errFlag){
				//see if the walled city mod is loaded. If it is, make it load its templates (if not already loaded) and then combine explorers.
			if (Loader.isModLoaded("WalledCityMod")){
				PopulatorWalledCity wcm= PopulatorWalledCity.instance;
				if(!wcm.dataFilesLoaded)  
					wcm.loadDataFiles();
				if(!wcm.errFlag){
					master=wcm.master;
					if(master!=null)
						logOrPrint("Combining chunk explorers for "+this.toString()+" and "+master.toString()+".");
				}
			}
			if(master==null) 
			{
				master=this;
				GameRegistry.registerWorldGenerator(this);
				TickRegistry.registerTickHandler(master, Side.SERVER);
				ForgeChunkManager.setForcedChunkLoadingCallback(this, master);
			}
			max_exploration_distance=MAX_EXPLORATION_DISTANCE;
		}		
	}
}
