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
 * BuildingExplorationHandler is a abstract superclass for PopulatorWalledCity and PopulatorGreatWall.
 * It loads settings files and runs WorldGeneratorThreads.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.logging.Logger;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.gen.ChunkProviderEnd;
import net.minecraft.world.gen.ChunkProviderHell;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.WorldEvent.Unload;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;

public abstract class BuildingExplorationHandler implements IWorldGenerator
{
	protected final static int MAX_TRIES_PER_CHUNK=100,CHUNKS_AT_WORLD_START=256;
	public final static int MAX_CHUNKS_PER_TICK=100;
	public final static int[] NO_CALL_CHUNK=null;
	private final static int MIN_CHUNK_SEPARATION_FROM_PLAYER=0;
	
	protected final static File BASE_DIRECTORY=getMinecraftBaseDir();
	protected final static File CONFIG_DIRECTORY=new File(Loader.instance().getConfigDir(),"generatormods");
	
	public BuildingExplorationHandler master=null;
	protected boolean isCreatingDefaultChunks=false, isFlushingGenThreads=false, isAboutToFlushGenThreads=false;
	protected boolean errFlag=false, dataFilesLoaded=false;
	protected boolean logActivated=false,chatMessage=false;
	//protected LinkedList<int[]> lightingList=new LinkedList<int[]>();UNUSED
	protected int max_exploration_distance;
	protected int chunksExploredThisTick=0, chunksExploredFromStart=0;
	private int lastExploredChunkI, lastExploredChunkK;	
	protected LinkedList<WorldGeneratorThread> exploreThreads=new LinkedList<WorldGeneratorThread>();
	public int[] flushCallChunk=NO_CALL_CHUNK;
	public PrintWriter lw=null;
	private World currentWorld=null;
 	int[] chestTries=new int[]{4,6,6,6};
	int[][][] chestItems=new int[][][]{null,null,null,null};
 	
	public static Logger logger=FMLLog.getLogger();
	
	abstract public void updateWorldExplored(World world);
	abstract public void loadDataFiles();
	abstract public void generate(World world, Random random, int i, int k);
	
	//****************************  FUNCTION - isGeneratorStillValid *************************************************************************************//
	//override this with e.g. the walled city generator
	public boolean isGeneratorStillValid(WorldGeneratorThread wgt){
		return true;
	}

	//****************************  FUNCTION - doOnTick *************************************************************************************//
	
	public void doOnTick(World tickWorld){
		if(this==master){
			updateWorldExplored(tickWorld);
			flushGenThreads(tickWorld, NO_CALL_CHUNK);
			runWorldGenThreads();
		}
	}
	//**************************** FORGE EVENTS ********************************************************************************************//
	@ForgeSubscribe
	public void onPopulatingChunk(PopulateChunkEvent event){
		if(chunksExploredFromStart==0 && exploreThreads.size()<3 && lastExploredChunkI!=event.chunkX && lastExploredChunkK!=event.chunkZ) 
		{
		lastExploredChunkI=event.chunkX;
		lastExploredChunkK=event.chunkZ;
		logOrPrint("Event called and last chunk changed");
		}
		
	}
	//**************************** FORGE WORLD GENERATING HOOK ****************************************************************************//
	
	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider)
    {  	if (world.getWorldInfo().isMapFeaturesEnabled())
	    { //if structures are enabled
	        if (chunkGenerator instanceof ChunkProviderHell)
	        {	//can generate in Nether
	            generateNether(world, random, chunkX*16, chunkZ*16);
	        }
	        else if ( !(chunkGenerator instanceof ChunkProviderEnd))
	        {	//can generate in any world except in The End
	            generateSurface(world, random, chunkX*16, chunkZ*16);
	        }
	    }
    }
	
	//****************************  FUNCTION - GenerateSurface  *************************************************************************************//
	public void generateSurface( World world, Random random, int i, int k ) {
		if(errFlag) return;
		updateWorldExplored(world);
		chunksExploredFromStart++;
		
		//Put flushGenThreads before the exploreThreads enqueues and include the callChunk argument.
		//This is to avoid putting mineral deposits in cities etc.
		if(this==master && !isFlushingGenThreads)
			flushGenThreads(world, new int[]{i,k});
		
		generate(world,random,i,k);
	}
	public void generateNether( World world, Random random, int chunkI, int chunkK ) {
		generateSurface(world,random,chunkI,chunkK);//TODO:Change this
	}
	//****************************  FUNCTION - killZombie *************************************************************************************//
	public void killZombie(WorldGeneratorThread wgt){
		if(wgt.hasStarted && wgt.isAlive()){
			synchronized(this){
				//wgt.killMe=true;
				synchronized(wgt){
					wgt.interrupt();
					wgt.notifyAll();
				}
				joinAtSuspension(wgt);
			}
			logOrPrint("Killed a zombie thread.");
		}
	}
	
	//****************************  FUNCTION - setNewWorld *************************************************************************************//
	public void setNewWorld(World world,String newWorldStr){
		
		chunksExploredThisTick=0;
		chunksExploredFromStart=0;
		if(world.getTotalWorldTime()<100) 
		{ 
			isCreatingDefaultChunks=true;
		}
		logOrPrint(newWorldStr);
	}
	
	//****************************  FUNCTION - queryChunk *************************************************************************************//
	//query chunk should be called from the WorldGeneratorThread wgt.
	public boolean queryExplorationHandlerForChunk(World world, int chunkI, int chunkK, WorldGeneratorThread wgt) throws InterruptedException{	
		logOrPrint("Query Chunk"+chunkI+","+chunkK);
		//SMP - world.chunkProvider.chunkExists(chunkI, chunkK) calls ChunkProviderServer.java which returns id2ChunkMap.containsKey(ChunkCoordIntPair.chunkXZ2Int(i, j));
		//If we call this after, no building is spawned !!
		if(world.getChunkProvider().chunkExists(chunkI, chunkK)){ 
			logOrPrint("Chunk already exist at"+chunkI+","+chunkK);
			lastExploredChunkI=chunkI;
			lastExploredChunkK=chunkK;
			return true;
		}
		if(chunksExploredFromStart==0) {
			lastExploredChunkI=chunkI;
			lastExploredChunkK=chunkK;
		}
		 logOrPrint("Start chunk is"+lastExploredChunkI+","+lastExploredChunkK);
		if(Math.abs(chunkI - lastExploredChunkI) > max_exploration_distance
		   || Math.abs(chunkK - lastExploredChunkK) > max_exploration_distance){
			 logOrPrint("Chunk"+chunkI+","+chunkK+"too far away from"+lastExploredChunkI+","+lastExploredChunkK);
			return false;
		}
		boolean flag=false;
		List<EntityPlayerMP> playerList = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
		if(playerList!=null){
		for (EntityPlayerMP player:playerList)
			if( Math.abs(chunkI-((int)player.posX)>>4) < MIN_CHUNK_SEPARATION_FROM_PLAYER 
			 && Math.abs(chunkK-((int)player.posZ)>>4) < MIN_CHUNK_SEPARATION_FROM_PLAYER){ //try not to bury the player alive
				if (this.chatMessage)
					player.playerNetServerHandler.sendPacketToPlayer(new Packet3Chat("Terminating "+this.toString()+" generation thread, too close to player.\n "+Thread.currentThread().getId()+". at "+(((int)player.posX>>4))+","+(((int)player.posZ>>4))+"), while querying chunk "+chunkI+","+chunkK+")."));
				flag=true;				
			}
		}
		if(flag)
			return false;
		//We've now failed world.chunkProvider.chunkExists(chunkI, chunkK),
		// so we will have to load or generate this chunk
		
		if(chunksExploredThisTick > (isFlushingGenThreads ? CHUNKS_AT_WORLD_START : MAX_CHUNKS_PER_TICK))
		{
		//suspend the thread if we've exceeded our quota of chunks to load for this tick
			wgt.suspendGen();
			logOrPrint("Too much chunks loaded this time");
			return false;
		}
		chunksExploredThisTick++;

		
		if(flushCallChunk!=NO_CALL_CHUNK){
    		if(chunkI==flushCallChunk[0] && chunkK==flushCallChunk[1])
    			return false;
    	}
		
		//SSP - world.chunkProvider.provideChunk calls ChunkProvider.java which returns (Chunk)chunkMap.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(i, j));
		//       or loadChunk(i, j); if lookup fails. Since we already failed chunkExists(chunkI, chunkK) we could go directly to loadChunk(i,j);
		//SMP - world.chunkProvider.loadChunk calls ChunkProviderServer.java which looks up id2ChunkMap.getValueByKey(l), 
		//       returns this if it exists else calls serverChunkGenerator.provideChunk(i, j);
		world.getChunkProvider().loadChunk(chunkI, chunkK);
		logOrPrint("Force loaded chunk at"+chunkI+","+chunkK);
    	lastExploredChunkI=chunkI;
		lastExploredChunkK=chunkK;
		return true;
	}
	
	//****************************  FUNCTION - flushGenThreads *************************************************************************************//
	protected void flushGenThreads(World world, int[] callChunk){		
		//announce there is about to be lag because we are about to flush generation threads
		List<EntityPlayerMP> playerList = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
		if(!isAboutToFlushGenThreads && !isCreatingDefaultChunks && playerList!=null && chunksExploredFromStart > 2*CHUNKS_AT_WORLD_START-15){
			String flushAnnouncement=chunksExploredFromStart+" chunks explored this wave, lag may occur from "+this.toString();
			for (EntityPlayerMP player:playerList)
			{
				if (this.chatMessage)
					player.playerNetServerHandler.sendPacketToPlayer(new Packet3Chat(flushAnnouncement));
			}
			logOrPrint(flushAnnouncement);
			isAboutToFlushGenThreads=true;
		}
		//Must make sure that a)There is only one call to flushGenThreads on the stack at a time
		//                    b)flushGenThreads is only called from the main Minecraft thread.
		//This check is not at the beginning of function because we want to announce we are about to flush no matter what.
		if(isFlushingGenThreads || Thread.currentThread() instanceof WorldGeneratorThread) 
				return;
		
		if(chunksExploredFromStart>= (isCreatingDefaultChunks ? CHUNKS_AT_WORLD_START-1 : 2*CHUNKS_AT_WORLD_START))
		{
			isFlushingGenThreads=true;
			flushCallChunk=callChunk;
			while(exploreThreads.size() > 0) 
			{
				doOnTick(world);
			}			
			isFlushingGenThreads=false;
			isCreatingDefaultChunks=false;
			isAboutToFlushGenThreads=false;
			flushCallChunk=NO_CALL_CHUNK;
		}
	}
	
	//****************************  FUNCTION - runWorldGenThreads *************************************************************************************//
	protected void runWorldGenThreads(){
		ListIterator<WorldGeneratorThread> itr=(ListIterator<WorldGeneratorThread>)((LinkedList<WorldGeneratorThread>)exploreThreads.clone()).listIterator();
		
		while(itr.hasNext() && (isFlushingGenThreads  || chunksExploredThisTick < MAX_CHUNKS_PER_TICK)){
			WorldGeneratorThread wgt=itr.next();
			synchronized(this){
				if(!wgt.hasStarted) 
					wgt.start();
				else{
					synchronized(wgt){
						wgt.threadSuspended=false;
						wgt.notifyAll();
					}
				}
				joinAtSuspension(wgt);
			}
			if(wgt.willBuild && !isFlushingGenThreads) 
				break;
		}
		
		itr=exploreThreads.listIterator();
		while(itr.hasNext()){
			WorldGeneratorThread wgt=itr.next();
			if(wgt.hasStarted){
				if(!wgt.isAlive()) itr.remove();
			}else if(!isGeneratorStillValid(wgt))
				itr.remove();
		}
		
		if(exploreThreads.size()==0) {
			if(chunksExploredFromStart > 10) logOrPrint("Explored "+chunksExploredFromStart+" chunks in last wave.");
			chunksExploredFromStart=0;
		}
		chunksExploredThisTick=0;
	}
	
	//TODO: Use this ?
	//****************************  FUNCTION - doQueuedLighting *************************************************************************************//
	/*
	public void queueLighting(int[] pt){
		lightingList.add(pt);
	}
	
	public void doQueuedLighting(){
		//if(lightingList.size()>100 ) logOrPrint("Doing "+lightingList.size()+" queued lighting commands.");
		lightingList=new LinkedList<int[]>();
		while(lightingList.size()>0){
			int[] pt=lightingList.remove();
			world.scheduleLightingUpdate(EnumSkyBlock.Sky,pt[0],pt[1],pt[2],pt[3],pt[4],pt[5]);
			world.scheduleLightingUpdate(EnumSkyBlock.Block,pt[0],pt[1],pt[2],pt[3],pt[4],pt[5]);
		}
	}
	*/
	
	//****************************  FUNCTION - chestContentsList *************************************************************************************//
	public void readChestItemsList(PrintWriter lw, String line, BufferedReader br) throws IOException{
		int triesIdx=-1;
		for(int l=0; l<Building.CHEST_TYPE_LABELS.length; l++){
			if(line.startsWith(Building.CHEST_TYPE_LABELS[l])){
				triesIdx=l;
				break;
			}
		}
		/*if(line.startsWith("CHEST_")){ TODO:to implement
			Building.CHEST_LABELS.add(line);
			triesIdx++;
		}*/
				
		if(triesIdx!=-1){
			chestTries[triesIdx]=readIntParam(lw,1,":",br.readLine());
			ArrayList<String> lines=new ArrayList<String>();
			for(line=br.readLine(); !(line==null || line.length()==0); line=br.readLine())
				lines.add(line);
			chestItems[triesIdx]=new int[6][lines.size()];
			for(int n=0; n<lines.size(); n++){
				String[] intStrs=lines.get(n).trim().split(",");
				try{
					chestItems[triesIdx][0][n]=n;
					String[] idAndMeta=intStrs[0].split("-");
					chestItems[triesIdx][1][n]=Integer.parseInt(idAndMeta[0]);
					chestItems[triesIdx][2][n]= idAndMeta.length>1 ? Integer.parseInt(idAndMeta[1]) : 0;
					for(int m=1; m<4; m++) 
						chestItems[triesIdx][m+2][n]=Integer.parseInt(intStrs[m]);
					
					//input checking
					if(chestItems[triesIdx][4][n]<0) chestItems[triesIdx][4][n]=0;
					if(chestItems[triesIdx][5][n]<chestItems[triesIdx][4][n]) 
						chestItems[triesIdx][5][n]=chestItems[triesIdx][4][n];
					if(chestItems[triesIdx][5][n]>64) chestItems[triesIdx][5][n]=64;
				}catch(Exception e){
					lw.println("Error parsing Settings file: "+e.toString());
					lw.println("Line:"+lines.get(n));
				}
			}
		}
	}
	
	protected void copyDefaultChestItems(){
		chestTries=new int[Building.DEFAULT_CHEST_TRIES.length];
		for(int n=0; n<Building.DEFAULT_CHEST_TRIES.length; n++)
			chestTries[n]=Building.DEFAULT_CHEST_TRIES[n];
		chestItems=new int[Building.DEFAULT_CHEST_ITEMS.length][][];
		
		//careful, we have to flip the order of the 2nd and 3rd dimension here
		for(int l=0; l<Building.DEFAULT_CHEST_ITEMS.length; l++){
			chestItems[l]=new int[6][Building.DEFAULT_CHEST_ITEMS[l].length];
			for(int m=0; m<Building.DEFAULT_CHEST_ITEMS[l].length; m++){
				for(int n=0; n<6; n++){
					chestItems[l][n][m]=Building.DEFAULT_CHEST_ITEMS[l][m][n];
		}}}
	}
	
	protected void printDefaultChestItems(PrintWriter pw){
		pw.println();
		pw.println("<-Chest contents->");
		pw.println("<-Tries is the number of selections that will be made for this chest type.->");
		pw.println("<-Format for items is <itemID>,<selection weight>,<min stack size>,<max stack size> ->");
		pw.println("<-So e.g. 262,1,1,12 means a stack of between 1 and 12 arrows, with a selection weight of 1.->");	
		for(int l=0; l<chestItems.length; l++){
			pw.println(Building.CHEST_TYPE_LABELS[l]);
			pw.println("Tries:"+chestTries[l]);
			for(int m=0; m<chestItems[l][0].length; m++){
				pw.print(chestItems[l][1][m]);
				if(chestItems[l][2][m]!=0) pw.print("-"+chestItems[l][2][m]);
				pw.print(","+chestItems[l][3][m]);
				pw.print(","+chestItems[l][4][m]);
				pw.println(","+chestItems[l][5][m]);
			}
			pw.println();
		}
	}
	
	//****************************  FUNCTION - joinAtSuspension *************************************************************************************//
	protected void joinAtSuspension(WorldGeneratorThread wgt){
		while (wgt.isAlive() && !wgt.threadSuspended){
	        try {
					wait();
	        } catch (InterruptedException e){}
		}
		try {
			if(wgt.hasTerminated) wgt.join();
		}catch (InterruptedException e){}
	}
	
	//****************************************  FUNCTIONS - error handling parameter readers  *************************************************************************************//
	public static int readIntParam(PrintWriter lw,int defaultVal,String splitString, String read){
		try{
			defaultVal=Integer.parseInt(read.split(splitString)[1].trim());
		} catch(Exception e) { 
			lw.println("Error parsing int: "+e.toString());
			lw.println("Using default "+defaultVal+". Line:"+read); 
		}
		return defaultVal;
	}
	public static boolean readBooleanParam(PrintWriter lw,boolean defaultVal,String splitString, String read){
		try{
			defaultVal=Boolean.parseBoolean(read.split(splitString)[1].trim());
		} catch(Exception e) { 
			lw.println("Error parsing boolean: "+e.toString());
			lw.println("Using default "+defaultVal+". Line:"+read); 
		}
		return defaultVal;
	}
	public static float readFloatParam(PrintWriter lw,float defaultVal,String splitString, String read){
		try{
			defaultVal=Float.parseFloat(read.split(splitString)[1].trim());
		} catch(Exception e) { 
			lw.println("Error parsing double: "+e.toString());
			lw.println("Using default "+defaultVal+". Line:"+read); 
		}
		return defaultVal;
	}
	
	//if an integer ruleId: try reading from rules and return.
	//If a rule: parse the rule, add it to rules, and return.
	public TemplateRule readRuleIdOrRule(String splitString, String read, TemplateRule[] rules) throws Exception{
		String postSplit=read.split(splitString)[1].trim();
		try{
			int ruleId=Integer.parseInt(postSplit);
			return rules[ruleId];
		} catch(NumberFormatException e) { 
			TemplateRule r=new TemplateRule(postSplit,false);
			return r;
		}catch(Exception e) { 
			throw new Exception("Error reading block rule for variable: "+e.toString()+". Line:"+read);
		}
	}
	
	public static int[] readNamedCheckList(PrintWriter lw,int[] defaultVals,String splitString, String read, String[] names, String allStr){
		if(defaultVals==null || names.length!=defaultVals.length) defaultVals=new int[names.length];
		try{
			int[] newVals=new int[names.length];
			for(int i=0;i<newVals.length;i++) newVals[i]=0;
			if((read.split(splitString)[1]).trim().equalsIgnoreCase(allStr)){
				for(int i=0;i<newVals.length;i++) newVals[i]=1;
			}else{
				for(String check : (read.split(splitString)[1]).split(",")){
					boolean found=false;
					for(int i=0;i<names.length;i++){
						if(names[i]!=null && names[i].replaceAll("\\s", "").equalsIgnoreCase(check.trim().replaceAll("\\s", ""))){
							found=true;
							newVals[i]++;
						}
					}
					if(!found) 
						lw.println("Warning, named checklist item not found:"+check+". Line:"+read);
				}
			}	
			return newVals;
		}catch(Exception e) { 
			lw.println("Error parsing checklist input: "+e.toString());
			lw.println("Using default. Line:"+read); 
		}
		return defaultVals;
	}

	
	public static int[] readIntList(PrintWriter lw,int[] defaultVals,String splitString,  String read){
		try{
			String[] check = (read.split(splitString)[1]).split(",");
			int[] newVals=new int[check.length];

			for(int i=0;i<check.length;i++){
				int val=Integer.parseInt(check[i].trim());
				newVals[i]=val;
			}
			return newVals;

		}catch(Exception e) { 
			lw.println("Error parsing intlist input: "+e.toString());
			lw.println("Using default. Line:"+read); 
		}
		return defaultVals;
	}

	
	public static ArrayList<byte[][]> readAutomataList(PrintWriter lw, String splitString,String read){
		ArrayList<byte[][]> rules=new ArrayList<byte[][]>();
		String[] ruleStrs =(read.split(splitString)[1]).split(",");
		for(String ruleStr : ruleStrs){
			byte[][] rule=BuildingCellularAutomaton.parseCARule(ruleStr.trim(),lw);
			if(rule!=null) rules.add(rule);
		}
		if(rules.size()==0) return null;
		return rules;
	}
	private static File getMinecraftBaseDir()
    {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT)
        {
            return FMLClientHandler.instance().getClient().getMinecraftDir();
        }
            
        return FMLCommonHandler.instance().getMinecraftServerInstance().getFile("");
    }
	protected boolean checkNewWorld(World world)
    {
		if (currentWorld == null)
        {
        	currentWorld = world;
            return true;
        }
		else if (currentWorld == world)
        {
            return false;
        }
        else
        {
            //check the filename in case we changed of dimension
            File olddir = getWorldSaveDir(currentWorld);
            File newdir = getWorldSaveDir(world);
            if (olddir.compareTo(newdir) != 0)
            {
                // new world has definitely been created.
                currentWorld = world;
                return true;
            }
            return false;
        }
    }
	private static File getWorldSaveDir(World world)
    {
        ISaveHandler worldSaver = world.getSaveHandler();       
        if (worldSaver.getChunkLoader(world.provider) instanceof AnvilChunkLoader)
        {
            AnvilChunkLoader loader = (AnvilChunkLoader) worldSaver.getChunkLoader(world.provider);
            
            for (Field field : loader.getClass().getDeclaredFields())
            {
                if (field.getType().equals(File.class))
                {
                    try
                    {
                        field.setAccessible(true);
                        File saveLoc = (File) field.get(loader);
                        return saveLoc;
                    }
                    catch (Exception e)
                    {
                        System.err.println("Failed trying to find World Save dir:");
                        e.printStackTrace();
                    }
                }
            }
        } 
        return null;
    }
	
	public void logOrPrint(String str) {
		if (this.logActivated)logger.info(str);	
	}
}
