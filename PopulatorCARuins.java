package mods.generator;

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
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.Mod.ServerStarting;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;
/*
 *  Source code for the CA Ruins Mod for the game Minecraft
 *  Copyright (C) 2011 by formivore

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@Mod(modid = "CARuins", name = "Cellular Automata Generator", version = "0.0.8",dependencies= "after:ExtraBiomes,BiomesOPlenty")
@NetworkMod(clientSideRequired = false, serverSideRequired = false)
public class PopulatorCARuins extends BuildingExplorationHandler{
	@Instance("CARuins")
	public static PopulatorCARuins instance;
	private final static int MAX_EXPLORATION_DISTANCE=10;
	private final static String AUTOMATA_RULES_STRING="AUTOMATA RULES",LINEAR_STR="linear",SYMMETRIC_STR="symmetric", BOTH_STR="both";
	private final static TemplateRule DEFAULT_TEMPLATE=new TemplateRule(new int[]{4,48,48},new int[]{0,0,0},100);
	private final static TemplateRule[] DEFAULT_BLOCK_RULES = new TemplateRule[257];
	static{
		DEFAULT_BLOCK_RULES[0]=DEFAULT_TEMPLATE;			//Underground, unused
		DEFAULT_BLOCK_RULES[1]=DEFAULT_TEMPLATE;			//Ocean
		DEFAULT_BLOCK_RULES[2]=new TemplateRule(new int[]{1,98,98},new int[]{0,1,2},100);   //Plains                
		DEFAULT_BLOCK_RULES[3]=new TemplateRule(new int[]{24},new int[]{0},100);          	//Desert            
		DEFAULT_BLOCK_RULES[4]=new TemplateRule(new int[]{1,98,98},new int[]{0,0,2},100);   //Hills             
		DEFAULT_BLOCK_RULES[5]=DEFAULT_TEMPLATE;          //Forest            
		DEFAULT_BLOCK_RULES[6]=DEFAULT_TEMPLATE;          //Taiga             
		DEFAULT_BLOCK_RULES[7]=DEFAULT_TEMPLATE;          //Swampland         
		DEFAULT_BLOCK_RULES[8]=DEFAULT_TEMPLATE;          //River             
		DEFAULT_BLOCK_RULES[9]=new TemplateRule(new int[]{112},new int[]{0},100);//Nether            
		DEFAULT_BLOCK_RULES[10]=new TemplateRule(new int[]{121},new int[]{0},100);          //Sky                     
		DEFAULT_BLOCK_RULES[11]=new TemplateRule(new int[]{79,80,98},new int[]{0,0,2},100);	//FrozenOcean
		DEFAULT_BLOCK_RULES[12]=new TemplateRule(new int[]{79,80,98},new int[]{0,0,2},100);	//FrozenRiver
		DEFAULT_BLOCK_RULES[13]=new TemplateRule(new int[]{80,98,98},new int[]{0,2,2},100); //IcePlains         
		DEFAULT_BLOCK_RULES[14]=new TemplateRule(new int[]{80,98,98},new int[]{0,2,2},100); //IceMountains      
		DEFAULT_BLOCK_RULES[15]=DEFAULT_TEMPLATE;        	//MushroomIsland    
		DEFAULT_BLOCK_RULES[16]=DEFAULT_TEMPLATE;			//Shore
		DEFAULT_BLOCK_RULES[17]=DEFAULT_TEMPLATE;     	    //Beach
		DEFAULT_BLOCK_RULES[18]=new TemplateRule(new int[]{24},new int[]{0},100);		//DesertHills
		DEFAULT_BLOCK_RULES[19]=DEFAULT_TEMPLATE;			//ForestHills
		DEFAULT_BLOCK_RULES[20]=DEFAULT_TEMPLATE;			//TaigaHills
		DEFAULT_BLOCK_RULES[21]=new TemplateRule(new int[]{1,98,98},new int[]{0,0,2},100);//ExtremeHillsEdge
		for (int i = 22; i < Building.BIOME_NAMES.length; i++)
        {
    	  if (Building.BIOME_NAMES[i]!=null)  	  
    		  DEFAULT_BLOCK_RULES[i]=DEFAULT_TEMPLATE;   	  
        }
	}
	
	//WARNING! Make sure the first DEFAULT_BLOCK_RULES.length biome Strings in Building are the ones we want here.
	private final static String[] BLOCK_RULE_NAMES; 
	static
	{
		BLOCK_RULE_NAMES=new String[DEFAULT_BLOCK_RULES.length];
		for(int m=0; m<BLOCK_RULE_NAMES.length; m++)
		{if (Building.BIOME_NAMES[m]!=null)
			BLOCK_RULE_NAMES[m]=Building.BIOME_NAMES[m].replaceAll("\\s", "") + "BlockRule";
		}
	}
	private final static String[] SPAWNER_RULE_NAMES=new String[]{"MediumLightNarrowFloorSpawnerRule","MediumLightWideFloorSpawnerRule","LowLightSpawnerRule"};
	
	private final static String SETTINGS_FILE_NAME="CARuinsSettings.txt",LOG_FILE_NAME="caruins_log.txt";

	public final static String[][] DEFAULT_CA_RULES=new String[][]{
		//3-rule
		{"B3/S23",        "5", "Life - good for weird temples"},
		{"B36/S013468",   "3", "pillars and hands"},
		{"B367/S02347",   "2", "towers with interiors and chasms"},
		{"B34/S2356",     "3", "towers with hetrogenous shapes"},
		{"B368/S245",     "8", "Morley - good hanging bits"},
		{"B36/S125",      "4", "2x2 - pillar & arch temple/tower/statue"},
		{"B36/S23",       "4", "High Life - space invaders, hanging arms."},
		{"B3568/S148",    "4", "fuzzy stilts"},
		{"B3/S1245",      "8", "complex"},
		{"B3567/S13468",  "5", "fat fuzzy"},
		{"B356/S16",      "5", "fuzzy with spurs"},
		{"B3468/S123",    "3", "towers with arches"},
		{"B35678/S015678","2", "checkerboard"},
		{"B35678/S0156",  "15", "spermatazoa"},
		//2-rule
		{"B26/S12368",    "1", "mayan pyramid"},
		{"B248/S45",      "1", "gaudi pyramid"},
		{"B2457/S013458", "1", "complex interior pyramid"},
		//4-rule
		{"B45/S2345",     "6", "45-rule - square towers"},
	};
	public final static String[] SEED_TYPE_STRINGS=new String[]{"SymmetricSeedWeight","LinearSeedWeight","CircularSeedWeight","CruciformSeedWeight"};
	public int[] seedTypeWeights=new int[]{8,2,2,1};
	
	public float GlobalFrequency=0.1F,SymmetricSeedDensity=0.5F;
	public int MinHeight=20,MaxHeight=70;
	public int ContainerWidth=40, ContainerLength=40;
	public int MinHeightBeforeOscillation=12;
	public boolean SmoothWithStairs=true, MakeFloors=true;
	
	public TemplateRule[] blockRules=new TemplateRule[DEFAULT_BLOCK_RULES.length];
	public TemplateRule[] spawnerRules=new TemplateRule[]{	BuildingCellularAutomaton.DEFAULT_MEDIUM_LIGHT_NARROW_SPAWNER_RULE, 
															BuildingCellularAutomaton.DEFAULT_MEDIUM_LIGHT_WIDE_SPAWNER_RULE,
															BuildingCellularAutomaton.DEFAULT_LOW_LIGHT_SPAWNER_RULE };
	
	ArrayList<byte[][]> caRules=null;
	int[][] caRulesWeightsAndIndex=null;
	
	@PreInit
	public void preInit(FMLPreInitializationEvent event) {	instance=this;	}
	
	@Init
	public void load(FMLInitializationEvent event) {		
		GameRegistry.registerWorldGenerator(this);

		max_exploration_distance=MAX_EXPLORATION_DISTANCE;
		
		/*for(int m=0; m<DEFAULT_BLOCK_RULES .length; m++)
		{
			blockRules[m]=DEFAULT_BLOCK_RULES[m];
		}*/
		master=this;	
	}
	@ServerStarting
	public void serverStarting(FMLServerStartingEvent event){
		event.registerServerCommand(new CommandBuild());
	}
	//****************************   FUNCTION - updateWorldExplored *************************************************************************************//
	public synchronized void updateWorldExplored(World world_) {
		if (checkNewWorld(world_))
		{setNewWorld(world_,"Starting to survey a world for automata generation...");
			
			if(this==master)
			{//kill zombies
				for(WorldGeneratorThread wgt: exploreThreads) killZombie(wgt);
				exploreThreads=new LinkedList<WorldGeneratorThread>();
			} else
			{
				master.updateWorldExplored(world_);
				exploreThreads=master.exploreThreads;
			}
		}
	}
	
	//****************************  FUNCTION - loadDataFiles *************************************************************************************//
	public final void loadDataFiles(){
		try {
			//read and check values from file
			lw= new PrintWriter( new BufferedWriter( new FileWriter(new File(BASE_DIRECTORY,LOG_FILE_NAME)) ));
			
			logOrPrint("Loading options for the Cellular Automata Generator");
			getGlobalOptions();

			lw.println("Probability of generation attempt per chunk explored is "+GlobalFrequency+", with "+TriesPerChunk+" tries per chunk.");
			if(GlobalFrequency <0.000001 || caRules==null || caRules.size()==0) errFlag=true;
		} catch( Exception e ) {
			errFlag=true;
			logOrPrint( "There was a problem loading the Cellular Automata Generator: "+ e.getMessage() );
			lw.println( "There was a problem loading the Cellular Automata Generator: "+ e.getMessage() );
			e.printStackTrace();
		}finally{ if(lw!=null) lw.close(); }
		dataFilesLoaded=true;
	}
	
	//****************************  FUNCTION - getGlobalOptions *************************************************************************************//
	private final void getGlobalOptions(){
		File settingsFile=new File(CONFIG_DIRECTORY,SETTINGS_FILE_NAME);
		caRules=new ArrayList<byte[][]>();
		ArrayList<Integer> caRuleWeights=new ArrayList<Integer>();
		if(settingsFile.exists()){
			BufferedReader br = null;
			try{
				br=new BufferedReader( new FileReader(settingsFile) );
				lw.println("Getting global options...");    
	
				for(String read=br.readLine(); read!=null; read=br.readLine()){
					if(read.startsWith( "GlobalFrequency" )) GlobalFrequency = readFloatParam(lw,GlobalFrequency,":",read);
					if(read.startsWith( "TriesPerChunk" )) TriesPerChunk = readIntParam(lw,TriesPerChunk,":",read);
					if(read.startsWith( "AllowedDimensions" )) AllowedDimensions = readIntList(lw,AllowedDimensions,":",read);
					if(read.startsWith( "MinHeight" )) MinHeight = readIntParam(lw,MinHeight,":",read);
					if(read.startsWith( "MaxHeight" )) MaxHeight = readIntParam(lw,MaxHeight,":",read);
					if(read.startsWith( "MinHeightBeforeOscillation" )) MinHeightBeforeOscillation = readIntParam(lw,MinHeightBeforeOscillation,":",read);
					if(read.startsWith( "SmoothWithStairs" )) SmoothWithStairs = readBooleanParam(lw,SmoothWithStairs,":",read);
					if(read.startsWith( "MakeFloors" )) MakeFloors = readBooleanParam(lw,MakeFloors,":",read);
					if(read.startsWith( "LogActivated" )) logActivated = readBooleanParam(lw,logActivated,":",read);					
					if(read.startsWith( "ChatMessage" )) chatMessage = readBooleanParam(lw,chatMessage,":",read);
					if(read.startsWith( "ContainerWidth" )) ContainerWidth = readIntParam(lw,ContainerWidth,":",read);
					if(read.startsWith( "ContainerLength" )) ContainerLength = readIntParam(lw,ContainerLength,":",read);
					readChestItemsList(lw,read,br);
					if(read.startsWith( "SymmetricSeedDensity" )) SymmetricSeedDensity = readFloatParam(lw,SymmetricSeedDensity,":",read);
					for(int m=0; m<SEED_TYPE_STRINGS.length; m++){
						if(read.startsWith(SEED_TYPE_STRINGS[m] )) seedTypeWeights[m] = readIntParam(lw,seedTypeWeights[m],":",read);
					}
					
					for(int m=0; m<spawnerRules.length; m++){
						if(read.startsWith(SPAWNER_RULE_NAMES[m])){
							try{
								spawnerRules[m]=readRuleIdOrRule(":",read,null);
							}catch(Exception e ){  
								spawnerRules[m]=BuildingCellularAutomaton.DEFAULT_MEDIUM_LIGHT_NARROW_SPAWNER_RULE; 
								lw.println(e.getMessage());
					}}}

					
					for(int m=0; m<DEFAULT_BLOCK_RULES.length; m++){
						if(Building.BIOME_NAMES[m]!=null && read.startsWith(BLOCK_RULE_NAMES[m])) {
							try{ 
								blockRules[m]=readRuleIdOrRule(":",read,null); 
							}catch(Exception e ){  
								blockRules[m]=DEFAULT_BLOCK_RULES[m]; 
								lw.println(e.getMessage());
					}}}
					
					
					if(read.startsWith(AUTOMATA_RULES_STRING)){
						for(read=br.readLine(); read!= null; read=br.readLine()){
							if(read.startsWith("B") || read.startsWith("b")){
								String[] splitStr=read.split(",");
								caRules.add(BuildingCellularAutomaton.parseCARule(splitStr[0],lw));
								caRuleWeights.add(readIntParam(lw,1,"=",splitStr[1].trim()));
							}
						}
						break;
					}
				}
				
				if(TriesPerChunk > MAX_TRIES_PER_CHUNK) TriesPerChunk = MAX_TRIES_PER_CHUNK;
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
				pw.println("<-GlobalFrequency controls how likely structures are to appear. Should be between 0.0 and 1.0. Lower to make less common->");
				pw.println("<-TriesPerChunk allows multiple attempts per chunk. Only change from 1 if you want very dense generation!->");
				pw.println("GlobalFrequency:"+GlobalFrequency);
				pw.println("TriesPerChunk:"+TriesPerChunk);
				pw.println("AllowedDimensions:"+Arrays.toString(AllowedDimensions).replace("[", "").replace("]", "").trim());
				pw.println();
				pw.println("<-MinHeight and MaxHeight are the minimum and maximum allowed height of the structures->");
				pw.println("<-MinHeightBeforeOscillation - Any structures that form oscillators before MaxOscillatorCullStep will be culled.->");
				pw.println("<-Smooth with stairs - If set to true, will smooth out ruins by placing extra stair blocks.->");
				pw.println("<-ContainerWidth and ContainerLength are the dimensions of the bounding rectangle.->");
				pw.println("MinHeight:"+MinHeight);
				pw.println("MaxHeight:"+MaxHeight);
				pw.println("MinHeightBeforeOscillation:"+MinHeightBeforeOscillation);
				pw.println("SmoothWithStairs:"+SmoothWithStairs);
				pw.println("MakeFloors:"+MakeFloors);								
				pw.println("ContainerWidth:"+ContainerWidth);
				pw.println("ContainerLength:"+ContainerLength);
				pw.println("<-LogActivated controls information stored into forge logs. Set to true if you want to report an issue with complete forge logs.->");
				pw.println("LogActivated:"+logActivated);
				pw.println("<-ChatMessage controls lag warnings.->");
				pw.println("ChatMessage:"+chatMessage);
				pw.println();
				printDefaultChestItems(pw);
				//printDefaultBiomes(lw);
				pw.println();
				pw.println("<-Seed type weights are the relative likelihood weights that different seeds will be used. Weights are nonnegative integers.->");
				pw.println("<-SymmetricSeedDensity is the density (out of 1.0) of live blocks in the symmetric seed.->");
				pw.println("SymmetricSeedDensity:"+SymmetricSeedDensity);
				for(int m=0; m<SEED_TYPE_STRINGS.length; m++){
					pw.println(SEED_TYPE_STRINGS[m]+":"+seedTypeWeights[m]);
				}
				
				pw.println();
				pw.println("<-These spawner rule variables control what spawners will be used depending on the light level and floor width.->");
				for(int m=0; m<spawnerRules.length; m++){
					pw.println(SPAWNER_RULE_NAMES[m]+":"+spawnerRules[m].toString());
				}
				
				pw.println();
				pw.println("<-BlockRule is the template rule that controls what blocks the structure will be made out of.->");
				pw.println("<-Default is BiomeNameBlockRule:"+DEFAULT_TEMPLATE.toString()+"->");
				pw.println("<-Which translates into: (special condition) then,(100%=complete)ruin in either normal(1 out of 3 chance) or mossy cobblestone(2 out of 3) in said biome->");
				pw.println("<-Metadatas are supported, use blockid-blockmetadata syntax->");
				for(int m=0; m<DEFAULT_BLOCK_RULES.length; m++){
					if (BLOCK_RULE_NAMES[m]!=null)
					pw.println(BLOCK_RULE_NAMES[m]+":"+DEFAULT_BLOCK_RULES[m].toString());
				}
				pw.println();
				pw.println("<-An automata rule should be in the form B<neighbor digits>/S<neighbor digits>, where B stands for \"birth\" and S stands->");
				pw.println("<-   for \"survive\". <neighbor digits> are the subset the digits from 0 to 8 on which the rule will birth or survive.->");
				pw.println("<-   For example, the Game of Life has the rule code B3/S23.->");
				pw.println("<-Rule weights are the relative likelihood weights that different rules will be used. Weights are nonnegative integers.->");
				pw.println(AUTOMATA_RULES_STRING);
				for(String[] defaultRule : DEFAULT_CA_RULES){
					pw.println(defaultRule[0] + ", weight="+defaultRule[1]+(defaultRule[2].length()>0 ? (",  <-"+defaultRule[2])+"->" : ""));
					caRules.add(BuildingCellularAutomaton.parseCARule(defaultRule[0],lw));
					caRuleWeights.add(Integer.parseInt(defaultRule[1]));
				}
			}
			catch(Exception e) { lw.println(e.getMessage()); }
			finally{ if(pw!=null) pw.close(); }
		}
		
		caRulesWeightsAndIndex=new int[2][caRuleWeights.size()];
		for(int m=0; m<caRuleWeights.size(); m++){
			caRulesWeightsAndIndex[0][m]=caRuleWeights.get(m);
			caRulesWeightsAndIndex[1][m]=m;
		}
	}
	
	//****************************  FUNCTION - generate *************************************************************************************//
	
	public final void generate( World world, Random random, int i, int k ) {	
		if(random.nextFloat() < GlobalFrequency)
			exploreThreads.add(new WorldGenCARuins(this, world, random, i, k,TriesPerChunk, GlobalFrequency));		
	}
	@Override
	public String toString(){
		return "Cellular Automata Ruins";
	}
	
	@PostInit
	public void modsLoaded(FMLPostInitializationEvent event)
	{		
		//see if the walled city mod is loaded. If it is, make it load its templates (if not already loaded) and then combine explorers.
		if (Loader.isModLoaded("WalledCityMod")){//FIXME ?
			PopulatorWalledCity wcm= PopulatorWalledCity.instance;
			if(!wcm.dataFilesLoaded)  wcm.loadDataFiles();
			if(!wcm.errFlag){
				master=wcm;
				logOrPrint("Combining chunk explorers for "+toString()+" and "+master.toString()+".");
			}
		}
		if(master==null) master=this;
		if(!dataFilesLoaded)loadDataFiles();				
	}
	
	//TODO: Use this ?
		 /**
		  * print all biomes available with default template
		  * @param pw the printwriter needed for operation
		  */
		public void printDefaultBiomes(PrintWriter pw){
			pw.println("<-Biomes Available and Templates->");	
	        for (int i = 0;   i <Building.BIOME_NAMES.length ; i++)
	        {	if (Building.BIOME_NAMES[i] != null)
	            pw.println("Default template for:" + Building.BIOME_NAMES[i] + "is"+DEFAULT_BLOCK_RULES[i]);
	        }
		}
}
