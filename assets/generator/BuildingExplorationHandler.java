package assets.generator;

/*
 *  Source code for the The Great Wall Mod, CellullarAutomata Ruins and Walled City Generator Mods for the game Minecraft
 *  Copyright (C) 2011 by formivore
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

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.storage.ISaveHandler;
import org.apache.logging.log4j.Level;

import java.io.*;
import java.util.*;

/*
 * BuildingExplorationHandler is a abstract superclass for PopulatorWalledCity and PopulatorGreatWall.
 * It loads settings files and runs WorldGeneratorThreads.
 */
public abstract class BuildingExplorationHandler implements IWorldGenerator {
    protected final static String VERSION = "0.1.6";
	protected final static int MAX_TRIES_PER_CHUNK = 100;
	public final static File CONFIG_DIRECTORY = new File(Loader.instance().getConfigDir(), "generatormods");
	protected final static File LOG = new File(new File(getMinecraftBaseDir(), "logs"), "generatormods_log.txt");
	protected String settingsFileName, templateFolderName;
	public org.apache.logging.log4j.Logger logger;
	public PrintWriter lw = null;
	public float GlobalFrequency = 0.025F;
	public int TriesPerChunk = 1;
	protected HashMap<String, Integer> chestTries = new HashMap<String, Integer>();
	protected HashMap<String, RandomLoot[]> chestItems = new HashMap<String, RandomLoot[]>();
	protected boolean errFlag = false, dataFilesLoaded = false;
	protected boolean logActivated = false;
	private List<Integer> AllowedDimensions = new ArrayList<Integer>();
	private List<World> currentWorld = new ArrayList<World>();
	public static String[] BIOME_NAMES = new String[BiomeGenBase.getBiomeGenArray().length + 1];
	static {
		BIOME_NAMES[0] = "Underground";
	}

	//**************************** FORGE WORLD GENERATING HOOK ****************************************************************************//
	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
		if (world.getWorldInfo().isMapFeaturesEnabled() && !(world.provider instanceof WorldProviderEnd)) {
			//if structures are enabled can generate in any world except in The End, if id is in AllowedDimensions list
			if (AllowedDimensions.contains(world.provider.dimensionId)) {
				generateSurface(world, random, chunkX, chunkZ);
			}
		}
	}

	abstract public void generate(World world, Random random, int i, int k);

	//****************************  FUNCTION - GenerateSurface  *************************************************************************************//
	public void generateSurface(World world, Random random, int i, int k) {
		if (errFlag)
			return;
		updateWorldExplored(world);
		generate(world, random, i * 16, k * 16);
	}

	abstract public void loadGlobalOptions(BufferedReader br);

	public void logOrPrint(String str, String lvl) {
		if (this.logActivated)
			logger.log(Level.toLevel(lvl), str);
	}

	//****************************  FUNCTION - chestContentsList *************************************************************************************//
	public void readChestItemsList(PrintWriter lw, String line, BufferedReader br) throws IOException {
        if(line.startsWith("CHEST_")){
            String chestType = line.substring(6);
			chestTries.put(chestType, readIntParam(lw, 1, ":", br.readLine()));
			ArrayList<String> lines = new ArrayList<String>();
			for (line = br.readLine(); !(line == null || line.length() == 0); line = br.readLine())
				lines.add(line);
			RandomLoot[] loots = new RandomLoot[lines.size()];
			for (int n = 0; n < lines.size(); n++) {
				try {
                    loots[n] = new RandomLoot(lines.get(n));
				} catch (Exception e) {
					lw.println("Error parsing Settings file: " + e.toString());
					lw.println("Line:" + lines.get(n));
				}
			}
            chestItems.put(chestType, loots);
		}
	}

	//if an integer ruleId: try reading from rules and return.
	//If a rule: parse the rule, add it to rules, and return.
	public TemplateRule readRuleIdOrRule(String splitString, String read, TemplateRule[] rules) throws Exception {
		String postSplit = read.split(splitString, 2)[1].trim();
		try {
			int ruleId = Integer.parseInt(postSplit);
			return rules[ruleId];
		} catch (NumberFormatException e) {
			return new TemplateRule(postSplit, false);
		} catch (Exception e) {
			throw new Exception("Error reading block rule for variable: " + e.toString() + ". Line:" + read);
		}
	}

	//****************************  FUNCTION - updateWorldExplored *************************************************************************************//
	public void updateWorldExplored(World world) {
		if (isNewWorld(world)) {
			logOrPrint("Starting to survey " + world.provider.getDimensionName() + " for generation...", "INFO");
		}
	}

	abstract public void writeGlobalOptions(PrintWriter pw);

	protected void copyDefaultChestItems() {
		for (int l = 0; l < DefaultChest.ITEMS.length; l++) {
            //careful, we have to flip the order of the 2nd and 3rd dimension here
            RandomLoot[] chestItems = new RandomLoot[DefaultChest.ITEMS[l].length];
			for (int m = 0; m < DefaultChest.ITEMS[l].length; m++) {
                chestItems[m] = new RandomLoot(DefaultChest.ITEMS[l][m]);
			}
            this.chestItems.put(DefaultChest.LABELS[l], chestItems);
            this.chestTries.put(DefaultChest.LABELS[l], DefaultChest.TRIES[l]);
		}
	}

	protected void finalizeLoading(boolean hasTemplate, String structure) {
		if (hasTemplate) {
			lw.println("\nTemplate loading complete.");
		}
		lw.println("Probability of " + structure + " generation attempt per chunk explored is " + GlobalFrequency + ", with " + TriesPerChunk + " tries per chunk.");
	}

	//****************************  FUNCTION - getGlobalOptions *************************************************************************************//
	protected final void getGlobalOptions() {
		File settingsFile = new File(CONFIG_DIRECTORY, settingsFileName);
		if (settingsFile.exists()) {
			lw.println("Getting global options for " + this.toString() + " ...");
			try {
				loadGlobalOptions(new BufferedReader(new FileReader(settingsFile)));
			} catch (FileNotFoundException ignored) {
			}
		} else {
			copyDefaultChestItems();
			try {
				writeGlobalOptions(new PrintWriter(new BufferedWriter(new FileWriter(settingsFile))));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected void initializeLogging(String message) throws IOException {
		if (LOG.length() > 8350)
			LOG.delete();
		lw = new PrintWriter(new BufferedWriter(new FileWriter(LOG, LOG.canWrite())));
		logOrPrint(message, "INFO");
		if (BIOME_NAMES[1] == null || BIOME_NAMES[1].equals("")) {
			for (int i = 0; i < BIOME_NAMES.length - 1; i++) {
				if (BiomeGenBase.getBiomeGenArray()[i] != null)
					BIOME_NAMES[i + 1] = BiomeGenBase.getBiomeGenArray()[i].biomeName;
			}
		}
	}

	protected boolean isNewWorld(World world) {
		if (currentWorld.isEmpty()) {
			currentWorld.add(world);
			return true;
		} else if (currentWorld.contains(world)) {
			return false;
		} else {
			File newdir = getWorldSaveDir(world);
			for (World w : currentWorld) {
				//check the filename in case we changed of dimension
				File olddir = getWorldSaveDir(w);
				if (newdir != null && olddir != null && olddir.compareTo(newdir) != 0) {
					// new world has definitely been created.
					currentWorld.add(world);
					return true;
				}
			}
			return false;
		}
	}

	protected void printDefaultChestItems(PrintWriter pw) {
        DefaultChest.print(pw);
	}

	protected void printGlobalOptions(PrintWriter pw, boolean frequency) {
		pw.println("<-README: This file should be in the config/generatormods folder->");
		pw.println();
		if (frequency) {
			pw.println("<-GlobalFrequency controls how likely structures are to appear. Should be between 0.0 and 1.0. Lower to make less common->");
			pw.println("GlobalFrequency:" + GlobalFrequency);
		}
		pw.println("<-TriesPerChunk allows multiple attempts per chunk. Only change from 1 if you want very dense generation!->");
		pw.println("TriesPerChunk:" + TriesPerChunk);
		pw.println("<-AllowedDimensions allows structures in corresponding dimension, by dimension ID. Default is Nether(-1) and OverWorld(0)->");
		pw.println("AllowedDimensions:" + (AllowedDimensions.isEmpty() ? "-1,0" : Arrays.toString(AllowedDimensions.toArray()).replace("[", "").replace("]", "").trim()));
		pw.println("<-LogActivated controls information stored into forge logs. Set to true if you want to report an issue with complete forge logs.->");
		pw.println("LogActivated:" + logActivated);
	}

	//****************************************  FUNCTIONS - error handling parameter readers  *************************************************************************************//
	protected void readGlobalOptions(PrintWriter lw, String read) {
		if (read.startsWith("GlobalFrequency"))
			GlobalFrequency = readFloatParam(lw, GlobalFrequency, ":", read);
		if (read.startsWith("TriesPerChunk"))
			TriesPerChunk = readIntParam(lw, TriesPerChunk, ":", read);
		if (read.startsWith("AllowedDimensions"))
			AllowedDimensions = Arrays.asList(readIntList(lw, new Integer[] { -1, 0 }, ":", read));
		if (read.startsWith("LogActivated"))
			logActivated = readBooleanParam(lw, logActivated, ":", read);
	}

	public static ArrayList<CARule> readAutomataList(PrintWriter lw, String splitString, String read) {
		ArrayList<CARule> rules = new ArrayList<CARule>();
		String[] ruleStrs = (read.split(splitString, 2)[1]).split(",");
		for (String ruleStr : ruleStrs) {
            try {
                CARule rule = new CARule(ruleStr.trim(), lw);
                rules.add(rule);
            }catch (IllegalArgumentException ignored){

            }
		}
		return rules;
	}

	public static boolean readBooleanParam(PrintWriter lw, boolean defaultVal, String splitString, String read) {
		try {
			defaultVal = Boolean.parseBoolean(read.split(splitString)[1].trim());
		} catch (Exception e) {
			lw.println("Error parsing boolean: " + e.toString());
			lw.println("Using default " + defaultVal + ". Line:" + read);
		}
		return defaultVal;
	}

	public static float readFloatParam(PrintWriter lw, float defaultVal, String splitString, String read) {
		try {
			defaultVal = Float.parseFloat(read.split(splitString)[1].trim());
		} catch (Exception e) {
			lw.println("Error parsing double: " + e.toString());
			lw.println("Using default " + defaultVal + ". Line:" + read);
		}
		return defaultVal;
	}

	public static Integer[] readIntList(PrintWriter lw, Integer[] defaultVals, String splitString, String read) {
		try {
			String[] check = (read.split(splitString)[1]).split(",");
			Integer[] newVals = new Integer[check.length];
			for (int i = 0; i < check.length; i++) {
				newVals[i] = Integer.parseInt(check[i].trim());
			}
			return newVals;
		} catch (Exception e) {
			lw.println("Error parsing input as int list: " + e.toString());
			lw.println("Using default. Line:" + read);
		}
		return defaultVals;
	}

	public static int readIntParam(PrintWriter lw, int defaultVal, String splitString, String read) {
		try {
            if(read.contains(splitString))
			    defaultVal = Integer.parseInt(read.split(splitString)[1].trim());
		} catch (Exception e) {
            if(lw!=null) {
                lw.println("Error parsing int: " + e.toString());
                lw.println("Using default " + defaultVal + ". Line:" + read);
            }
		}
		return defaultVal;
	}

	public static int[] readNamedCheckList(PrintWriter lw, int[] defaultVals, String splitString, String read, String[] names, String allStr) {
		if (defaultVals == null || names.length != defaultVals.length)
			defaultVals = new int[names.length];
		try {
			int[] newVals = new int[names.length];
			for (int i = 0; i < newVals.length; i++)
				newVals[i] = 0;
			if ((read.split(splitString)[1]).trim().equalsIgnoreCase(allStr)) {
				for (int i = 0; i < newVals.length; i++)
					newVals[i] = 1;
			} else {
				for (String check : (read.split(splitString)[1]).split(",")) {
					boolean found = false;
					for (int i = 0; i < names.length; i++) {
						if (names[i] != null && names[i].replaceAll("\\s", "").trim().equalsIgnoreCase(check.replaceAll("\\s", "").trim())) {
							found = true;
							newVals[i]++;
						}
					}
					if (!found)
						lw.println("Warning, named checklist item not found:" + check + ". Line:" + read);
				}
			}
			return newVals;
		} catch (Exception e) {
			lw.println("Error parsing checklist input: " + e.toString());
			lw.println("Using default. Line:" + read);
		}
		return defaultVals;
	}

	protected static File getWorldSaveDir(World world) {
		ISaveHandler worldSaver = world.getSaveHandler();
		if (worldSaver.getChunkLoader(world.provider) instanceof AnvilChunkLoader) {
			return ((AnvilChunkLoader) worldSaver.getChunkLoader(world.provider)).chunkSaveLocation;
		}
		return null;
	}

	private static File getMinecraftBaseDir() {
		if (FMLCommonHandler.instance().getSide().isClient()) {
			return FMLClientHandler.instance().getClient().mcDataDir;
		}
		return FMLCommonHandler.instance().getMinecraftServerInstance().getFile("");
	}

    protected final void trySendMUD(FMLPreInitializationEvent event){
        if(event.getSourceFile().getName().endsWith(".jar") && event.getSide().isClient()){
            try {
                Class.forName("mods.mud.ModUpdateDetector").getDeclaredMethod("registerMod", ModContainer.class, String.class, String.class).invoke(null,
                        FMLCommonHandler.instance().findContainerFor(this),
                        "https://raw.github.com/GotoLink/Generatormods/master/update.xml",
                        "https://raw.github.com/GotoLink/Generatormods/master/changelog.md"
                );
            } catch (Throwable ignored) {
            }
        }
    }
}
