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

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.HashMap;
import java.util.Random;

/*
 * WorldGeneratorThread is a thread that generates structures in the Minecraft world.
 * It is intended to serially hand back and forth control with a BuildingExplorationHandler (not to run parallel).
 */
public abstract class WorldGeneratorThread {
	public final static int LAYOUT_CODE_NOCODE = -1;
	public final static int LAYOUT_CODE_EMPTY = 0, LAYOUT_CODE_WALL = 1, LAYOUT_CODE_AVENUE = 2, LAYOUT_CODE_STREET = 3, LAYOUT_CODE_TOWER = 4, LAYOUT_CODE_TEMPLATE = 5;
	protected final static int[][] LAYOUT_CODE_OVERRIDE_MATRIX = new int[][] { //present code=rows, attempted overriding code=columns
        { 0, 1, 1, 1, 1, 1 }, //present empty
        { 0, 0, 0, 0, 0, 0 }, //present wall
        { 0, 0, 1, 1, 0, 0 }, //present avenue
        { 0, 0, 1, 1, 1, 0 }, //present street
        { 0, 0, 0, 0, 0, 0 }, //present tower
        { 0, 0, 0, 0, 0, 0 } }; //present template
	public final static char[] LAYOUT_CODE_TO_CHAR = new char[] { ' ', '#', '=', '-', '@', '&' };
	public final BuildingExplorationHandler master;
	public final World world;
	public final Random random;
	public final int chunkI, chunkK, triesPerChunk;
	public final double chunkTryProb;
	private int min_spawn_height = 0, max_spawn_height = 127;
	public boolean spawn_surface = true;
	HashMap<String, Integer> chestTries = null;
	HashMap<String, RandomLoot[]> chestItems = null;
	//public int ConcaveSmoothingScale=10, ConvexSmoothingScale=20, 
	//All WorldGeneratorThreads will have these, even if not used.
	public int backtrackLength = 9;

	//****************************  CONSTRUCTOR - WorldGeneratorThread *************************************************************************************//
	public WorldGeneratorThread(BuildingExplorationHandler master, World world, Random random, int chunkI, int chunkK, int TriesPerChunk, double ChunkTryProb) {
		this.master = master;
		this.chestTries = master.chestTries;
		this.chestItems = master.chestItems;
		this.world = world;
		this.random = random;
		this.chunkI = chunkI;
		this.chunkK = chunkK;
		this.triesPerChunk = TriesPerChunk;
		this.chunkTryProb = ChunkTryProb;
		max_spawn_height = Building.WORLD_MAX_Y;
	}

	//****************************  FUNCTION - abstract and stub functions  *************************************************************************************//
	public abstract boolean generate(int i0, int j0, int k0);

	public boolean isLayoutGenerator() {
		return false;
	}

	public boolean layoutIsClear(int[] pt1, int[] pt2, int layoutCode) {
		return true;
	}

	public boolean layoutIsClear(Building building, boolean[][] templateLayout, int layoutCode) {
		return true;
	}

	public void setLayoutCode(int[] pt1, int[] pt2, int layoutCode) {
	}

	public void setLayoutCode(Building building, boolean[][] templateLayout, int layoutCode) {
	}

	//****************************  FUNCTION - run *************************************************************************************//
	public void run() {
		boolean success = false;
		int tries = 0, j0 = 0, i0, k0;
		do {
			if (tries == 0 || this.random.nextDouble() < chunkTryProb) {
				i0 = chunkI + this.random.nextInt(16);
				k0 = chunkK + this.random.nextInt(16);
				if (spawn_surface) {
					j0 = Building.findSurfaceJ(this.world, i0, k0, Building.WORLD_MAX_Y, true, 3) + 1;
				} else {
					j0 = min_spawn_height + this.random.nextInt(max_spawn_height - min_spawn_height + 1);
				}
				if (j0 > 0 && world.getBiomeGenForCoordsBody(i0, k0) != BiomeGenBase.ocean)
					success = generate(i0, j0, k0);
			}
			tries++;
		} while (!success && tries < triesPerChunk && j0 != Building.HIT_WATER);
	}

	//****************************  FUNCTION - setSpawnHeight *************************************************************************************//
	public void setSpawnHeight(int min_spawn_height_, int max_spawn_height_, boolean spawn_surface_) {
		min_spawn_height = min_spawn_height_;
		max_spawn_height = max_spawn_height_;
		spawn_surface = spawn_surface_;
	}
}
