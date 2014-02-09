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
 * PopulatorGreatWall is the main class that hooks into ModLoader for the Great Wall Mod.
 * It reads the globalSettings file and runs WorldGenWalledCities.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

import net.minecraft.world.World;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = "GreatWallMod", name = "Great Wall Mod", version = "0.1.4", dependencies = "after:ExtraBiomes,BiomesOPlenty")
public class PopulatorGreatWall extends BuildingExplorationHandler {
	@Instance("GreatWallMod")
	public static PopulatorGreatWall instance;
	//USER MODIFIABLE PARAMETERS, values below are defaults
	public float CurveBias = 0.5F;
	public int LengthBiasNorm = 200;
	public int BacktrackLength = 9;
	//DATA VARIABLES
	public ArrayList<TemplateWall> wallStyles = null;
	public int[] placedCoords = null;
	public World placedWorld = null;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		logger = event.getModLog();
		settingsFileName = "GreatWallSettings.txt";
		templateFolderName = "greatwall";
	}

	//****************************  FUNCTION - loadDataFiles *************************************************************************************//
	@Override
	public final void loadDataFiles() {
		try {
			initializeLogging("Loading options and templates for the Great Wall Mod.");
			//read and check values from file
			getGlobalOptions();
			File stylesDirectory = new File(CONFIG_DIRECTORY, templateFolderName);
			wallStyles = TemplateWall.loadWallStylesFromDir(stylesDirectory, this);
			finalizeLoading(true, "wall");
		} catch (Exception e) {
			errFlag = true;
			logOrPrint("There was a problem loading the great wall mod: " + e.getMessage(), "SEVERE");
			lw.println("There was a problem loading the great wall mod: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (lw != null)
				lw.close();
		}
		if (GlobalFrequency < 0.000001)
			errFlag = true;
		dataFilesLoaded = true;
	}

	//****************************  FUNCTION - generate *************************************************************************************//
	@Override
	public final void generate(World world, Random random, int i, int k) {
		if (random.nextFloat() < GlobalFrequency)
			(new WorldGenGreatWall(this, world, random, i, k, TriesPerChunk, GlobalFrequency)).run();
	}

	//****************************  FUNCTION - getGlobalOptions  *************************************************************************************//
	@Override
	public void loadGlobalOptions(BufferedReader br) {
		try {
			for (String read = br.readLine(); read != null; read = br.readLine()) {
				readGlobalOptions(lw, read);
				if (read.startsWith("CurveBias"))
					CurveBias = readFloatParam(lw, CurveBias, ":", read);
				if (read.startsWith("LengthBiasNorm"))
					LengthBiasNorm = readIntParam(lw, LengthBiasNorm, ":", read);
				if (read.startsWith("BacktrackLength"))
					BacktrackLength = readIntParam(lw, BacktrackLength, ":", read);
				readChestItemsList(lw, read, br);
			}
			if (TriesPerChunk > MAX_TRIES_PER_CHUNK)
				TriesPerChunk = MAX_TRIES_PER_CHUNK;
			if (CurveBias < 0.0)
				CurveBias = 0.0F;
			if (CurveBias > 1.0)
				CurveBias = 1.0F;
		} catch (IOException e) {
			lw.println(e.getMessage());
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void writeGlobalOptions(PrintWriter pw) {
		printGlobalOptions(pw, true);
		pw.println();
		pw.println("<-BacktrackLength - length of backtracking for wall planning if a dead end is hit->");
		pw.println("<-CurveBias - strength of the bias towards curvier walls. Value should be between 0.0 and 1.0.->");
		pw.println("<-LengthBiasNorm - wall length at which there is no penalty for generation>");
		pw.println("BacktrackLength:" + BacktrackLength);
		pw.println("CurveBias:" + CurveBias);
		pw.println("LengthBiasNorm:" + LengthBiasNorm);
		pw.println();
		printDefaultChestItems(pw);
		if (pw != null)
			pw.close();
	}

	@Override
	public String toString() {
		return "GreatWallMod";
	}

	@EventHandler
	public void modsLoaded(FMLPostInitializationEvent event) {
		if (!dataFilesLoaded)
			loadDataFiles();
		if (!errFlag) {
			GameRegistry.registerWorldGenerator(this, 1);
		}
	}
}
