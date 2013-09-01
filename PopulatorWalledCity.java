package assets.generator;
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
 * PopulatorWalledCity is the main class that hooks into ModLoader for the Walled City Mod.
 * It reads the globalSettings file, keeps track of city locations, and runs WorldGenWalledCitys and WorldGenUndergroundCities.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = "WalledCityMod", name = "Walled City Generator", version = "0.1.3",dependencies= "after:ExtraBiomes,BiomesOPlenty")
@NetworkMod(clientSideRequired = false, serverSideRequired = false)
public class PopulatorWalledCity extends BuildingExplorationHandler{
	@Instance("WalledCityMod")
	public static PopulatorWalledCity instance;
	public final static int MIN_CITY_LENGTH=40;
	private final static int MAX_EXPLORATION_DISTANCE=10;
	final static int MAX_FOG_HEIGHT=27;
	public final static int CITY_TYPE_SURFACE=0, CITY_TYPE_NETHER=-1, CITY_TYPE_UNDERGROUND=1;
	private final static String SETTINGS_FILE_NAME="WalledCitySettings.txt",
								CITY_FILE_SAVE="WalledCities.txt",
								CITY_TEMPLATES_FOLDER_NAME="walledcity",
								STREET_TEMPLATES_FOLDER_NAME="streets";
	
	//USER MODIFIABLE PARAMETERS, values here are defaults
	public float UndergroundGlobalFrequency=0.015F;
	public int MinCitySeparation=500, UndergroundMinCitySeparation=500;
	public boolean CityBuiltMessage=false; 
	public int BacktrackLength=9;
	public boolean RejectOnPreexistingArtifacts=true;
	
	//DATA VARIABLES
	public ArrayList<TemplateWall> cityStyles=null, undergroundCityStyles=new ArrayList<TemplateWall>();
	public Map<World,ArrayList<int[]>> cityLocations;
	
	public LinkedList<int[]> citiesBuiltMessages=new LinkedList<int[]>();
	
	private File logFile,cityFile;
	private Map<World,File> cityFiles;
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		cityFiles = new HashMap();
		cityLocations = new HashMap();
	}
	
	//****************************  FUNCTION - loadDataFiles *************************************************************************************//
	public void loadDataFiles(){
		try {
			//read and check values from file
			logFile=new File(BASE_DIRECTORY,LOG_FILE_NAME);
			lw= new PrintWriter( new BufferedWriter( new FileWriter(logFile,true)));
			
			logOrPrint("Loading options and templates for the Walled City Generator.");
			getGlobalOptions();
			
			File stylesDirectory=new File(CONFIG_DIRECTORY,CITY_TEMPLATES_FOLDER_NAME);
			cityStyles=TemplateWall.loadWallStylesFromDir(stylesDirectory,this);
			TemplateWall.loadStreets(cityStyles,new File(stylesDirectory,STREET_TEMPLATES_FOLDER_NAME),this);
			for(int m=0; m<cityStyles.size(); m++){
				if(cityStyles.get(m).underground){
					TemplateWall uws = cityStyles.remove(m);
					uws.streets.add(uws); //underground cities have no outer walls, so this should be a street style
					undergroundCityStyles.add(uws);
					m--;
			}}
			
			lw.println("\nTemplate loading complete.");
			lw.println("Probability of city generation attempt per chunk explored is "+GlobalFrequency+", with "+TriesPerChunk+" tries per chunk.");
			if(GlobalFrequency <0.000001 && UndergroundGlobalFrequency<0.000001) 
				errFlag=true;
		} catch( Exception e ) {
			errFlag=true;
			lw.println( "There was a problem loading the walled city mod: "+e.getMessage() );
			logOrPrint( "There was a problem loading the walled city mod: "+e.getMessage() );
			e.printStackTrace();
		}finally{ if(lw!=null) lw.close(); }

		dataFilesLoaded=true;
	}
	
	//****************************  FUNCTION - cityIsSeparated *************************************************************************************//
	public boolean cityIsSeparated(World world, int i, int k, int cityType){
		if(cityLocations.containsKey(world)) 
		{
			for(int [] location : cityLocations.get(world)){
				if( location[2]==cityType && Math.abs(location[0]-i) + Math.abs(location[1]-k) 
						                     < (cityType==CITY_TYPE_UNDERGROUND ? UndergroundMinCitySeparation : MinCitySeparation )){
					return false;
				}
			}
		}
		return true;
	}
	public static ArrayList<int[]> getCityLocs(File city){
		ArrayList<int[]> cityLocs = new ArrayList<int[]>();
		BufferedReader br = null;
		try{
			br=new BufferedReader( new FileReader(city));
			for(String read=br.readLine(); read!=null; read=br.readLine()){
				String[] split=read.split(",");
				if(split.length==3){
					cityLocs.add(new int[]{Integer.parseInt(split[0]),Integer.parseInt(split[1]),Integer.parseInt(split[2])});
				}
			}
		}catch(IOException e) {System.err.println(e.getMessage()); }
		finally{ try{ if(br!=null) br.close();} catch(IOException e) {} }
		return cityLocs;
	}
	
	//****************************  FUNCTION - saveCityLocations *************************************************************************************//
	public void saveCityLocations(World world){
		PrintWriter pw=null;
		try{
			pw=new PrintWriter( new BufferedWriter( new FileWriter(cityFiles.get(world),true)));
			BufferedReader br = new BufferedReader( new FileReader(cityFiles.get(world)));
			if(br.readLine()==null)
			{
				pw.println("City locations in "+world.provider.getDimensionName()+" of : "+world.getWorldInfo().getWorldName());
			}
			br.close();
			int[] location = cityLocations.get(world).get(cityLocations.get(world).size()-1);
				pw.println(new StringBuilder(Integer.toString(location[0]))
								.append(",").append(Integer.toString(location[1]))
								.append(",").append(Integer.toString(location[2])));
			
		}catch(IOException e) {logOrPrint(e.getMessage()); }
		finally{ if(pw!=null) pw.close(); }
	}

	//****************************  FUNCTION - updateWorldExplored *************************************************************************************//
	@Override
	public synchronized void updateWorldExplored(World world_) {
		if (checkNewWorld(world_))
		{
			setNewWorld(world_,"Starting to survey "+world_.provider.getDimensionName()+" for city generation...");		
	
			if(this==master){
				//kill zombies
				for(WorldGeneratorThread wgt: exploreThreads) 
					killZombie(wgt);
				exploreThreads=new LinkedList<WorldGeneratorThread>();
			} else {
				master.updateWorldExplored(world_);
				exploreThreads=master.exploreThreads;
			}
		}
		cityFile=new File(getWorldSaveDir(world_),world_.provider.getDimensionName()+CITY_FILE_SAVE);
		if( cityFiles.isEmpty() || !cityFiles.containsKey(world_))
			cityFiles.put(world_, cityFile);
		try {
			if(!cityFile.createNewFile()){
				if(!cityLocations.containsKey(world_))
					cityLocations.put(world_, getCityLocs(cityFile));
			}
		} catch (IOException e) {
			logOrPrint(e.getMessage());
		}
	}
	
	//****************************  FUNCTION - isGeneratorStillValid *************************************************************************************//
	public boolean isGeneratorStillValid(WorldGeneratorThread wgt){
		return cityIsSeparated(wgt.world,wgt.chunkI,wgt.chunkK,wgt.spawn_surface ? CITY_TYPE_SURFACE : CITY_TYPE_UNDERGROUND);
	}
	
	
	//****************************  FUNCTION - chatCityBuilt *************************************************************************************//
	
	public void chatBuildingCity(String chatString, String logString){
		if(logString!=null) 
			logOrPrint(logString);
		if(!CityBuiltMessage) 
			return;
		List playerList = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
		if(playerList!=null ){
			for (int index = 0; index < playerList.size(); ++index)
	        {
	            EntityPlayerMP player = (EntityPlayerMP)playerList.get(index);
	            player.playerNetServerHandler.sendPacketToPlayer(new Packet3Chat(chatString));
	        }
		}
	}
	
	public void chatCityBuilt(int[] args){
		if(!CityBuiltMessage)
			return;
		List playerList = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
		if(playerList==null){
			citiesBuiltMessages.add(args);
		}else{
			for (int index = 0; index < playerList.size(); ++index)
	        {
	        EntityPlayerMP player = (EntityPlayerMP)playerList.get(index);            
			String dirStr="";
			int dI=args[0] - (int)player.posX;
			int dK=args[2] - (int)player.posZ;
			if(dI*dI+dK*dK < args[4]*args[4]){
				dirStr="nearby";
			}
			dirStr="to the ";
			if(Math.abs(dI)>2*Math.abs(dK)) dirStr+= dI>0 ? "east" : "west";
			else if(Math.abs(dK)>2*Math.abs(dI)) dirStr+= dK>0 ? "south" : "north";
			else dirStr+= dI > 0 
							? (dK>0 ? "southeast" : "northeast") 
							: (dK>0 ? "southwest" : "northwest");

			player.playerNetServerHandler.sendPacketToPlayer(new Packet3Chat("** Built city "+dirStr+" ("+args[0]+","+args[1]+","+args[2]+")! **"));	
	        }		
		}
	}
	//****************************  FUNCTION - generate *************************************************************************************//
	
	public final void generate( World world, Random random, int i, int k ) {
		if(CityBuiltMessage && world.playerEntities!=null)
			while(citiesBuiltMessages.size()>0) 
				chatCityBuilt(citiesBuiltMessages.remove());
		
		if(cityStyles.size() > 0 && cityIsSeparated(world,i,k,CITY_TYPE_SURFACE) && random.nextFloat() < GlobalFrequency){		
			exploreThreads.add(new WorldGenWalledCity(this, world, random, i, k,TriesPerChunk, GlobalFrequency));
		}
		if(undergroundCityStyles.size() > 0 && cityIsSeparated(world,i,k,CITY_TYPE_UNDERGROUND) && random.nextFloat() < UndergroundGlobalFrequency){
			WorldGeneratorThread wgt=new WorldGenUndergroundCity(this, world, random, i, k,1, UndergroundGlobalFrequency);
			int maxSpawnHeight=Building.findSurfaceJ(world,i,k,Building.WORLD_MAX_Y,false,Building.IGNORE_WATER)- WorldGenUndergroundCity.MAX_DIAM/2 - 5; //44 at sea level
			int minSpawnHeight=MAX_FOG_HEIGHT+WorldGenUndergroundCity.MAX_DIAM/2 - 8; //34, a pretty thin margin. Too thin for underocean cities?
			if(minSpawnHeight<=maxSpawnHeight)
				wgt.setSpawnHeight(minSpawnHeight, maxSpawnHeight, false);
			exploreThreads.add(wgt);
			
		}
	}
	
	//****************************  FUNCTION - getGlobalOptions  *************************************************************************************//
	private final void getGlobalOptions() {
		File settingsFile=new File(CONFIG_DIRECTORY,SETTINGS_FILE_NAME);
		if(settingsFile.exists()){
			BufferedReader br = null;
			try{
				br=new BufferedReader( new FileReader(settingsFile) );  
				lw.println("Getting global options for "+this.toString()+"...");    
		
				for(String read=br.readLine(); read!=null; read=br.readLine()){
		
					//outer wall parameters
					readGlobalOptions(lw,read);
					
					if(read.startsWith( "UndergroundGlobalFrequency" )) UndergroundGlobalFrequency = readFloatParam(lw,UndergroundGlobalFrequency,":",read);
					if(read.startsWith( "MinCitySeparation" )) MinCitySeparation= readIntParam(lw,MinCitySeparation,":",read);
					if(read.startsWith( "MinUndergroundCitySeparation" )) UndergroundMinCitySeparation= readIntParam(lw,UndergroundMinCitySeparation,":",read);
		
					//if(read.startsWith( "ConcaveSmoothingScale" )) ConcaveSmoothingScale = readIntParam(lw,ConcaveSmoothingScale,":",read);
					//if(read.startsWith( "ConvexSmoothingScale" )) ConvexSmoothingScale = readIntParam(lw,ConvexSmoothingScale,":",read);
					if(read.startsWith( "BacktrackLength" )) BacktrackLength = readIntParam(lw,BacktrackLength,":",read);
					if(read.startsWith( "CityBuiltMessage" )) CityBuiltMessage = readBooleanParam(lw,CityBuiltMessage,":",read);
					if(read.startsWith( "RejectOnPreexistingArtifacts" )) RejectOnPreexistingArtifacts = readBooleanParam(lw,RejectOnPreexistingArtifacts,":",read);
					readChestItemsList(lw,read,br);
		
				}
				if(TriesPerChunk > MAX_TRIES_PER_CHUNK) TriesPerChunk = MAX_TRIES_PER_CHUNK;
			}catch(IOException e) { lw.println(e.getMessage()); }
			finally{ try{ if(br!=null) br.close();} catch(IOException e) {} }
		}else{
			copyDefaultChestItems();
			PrintWriter pw=null;
			try{
				pw=new PrintWriter( new BufferedWriter( new FileWriter(settingsFile) ) );
				printGlobalOptions(pw,false);
				pw.println("<-GlobalFrequency/UndergroundGlobalFrequency controls how likely aboveground/belowground cities are to appear. Should be between 0.0 and 1.0. Lower to make less common->");
				pw.println("GlobalFrequency:"+GlobalFrequency);
				pw.println("UndergroundGlobalFrequency:"+UndergroundGlobalFrequency);
				pw.println("<-MinCitySeparation/UndergroundMinCitySeparation define a minimum allowable separation between city spawns.->");
				pw.println("MinCitySeparation:"+MinCitySeparation);
				pw.println("MinUndergroundCitySeparation:"+UndergroundMinCitySeparation);
				pw.println();
				pw.println("<-BacktrackLength - length of backtracking for wall planning if a dead end is hit->");
				pw.println("BacktrackLength:"+BacktrackLength);
				pw.println("<-CityBuiltMessage controls whether players receive message when a city is building. Set to true to receive message.->");
				pw.println("CityBuiltMessage:"+CityBuiltMessage);
				pw.println("<-RejectOnPreexistingArtifacts determines whether the planner rejects city sites that contain preexiting man-made blocks. Set to true to do this check.->");
				pw.println("RejectOnPreexistingArtifacts:"+RejectOnPreexistingArtifacts);
				pw.println();
				printDefaultChestItems(pw);
				//printDefaultBiomes(pw);
			}catch(IOException e) { lw.println(e.getMessage()); }
			finally{ if(pw!=null) pw.close(); }
		}		
	}
	@Override
	public String toString(){
		return "WalledCityMod";
	}
	//Load templates after mods have loaded so we can check whether any modded blockIDs are valid
	@PostInit
	public void modsLoaded(FMLPostInitializationEvent event)
	{
		if(!dataFilesLoaded)
			loadDataFiles();
		if(!errFlag){
			master=this;
			GameRegistry.registerWorldGenerator(this);
			TickRegistry.registerTickHandler(master, Side.SERVER);
			ForgeChunkManager.setForcedChunkLoadingCallback(this, master);
			max_exploration_distance=MAX_EXPLORATION_DISTANCE;
		}	
	}

}



