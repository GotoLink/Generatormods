package assets.generator;

/*
 *  Source code for the CellullarAutomata Ruins for the game Minecraft
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

import java.util.Random;

import net.minecraft.world.World;

public class WorldGenCARuins extends WorldGeneratorThread {
	private byte[][] caRule = null;
	private int[][] caRulesWeightsAndIndex;
	private final int MinHeight, MaxHeight;
	private final float GlobalFrequency, SymmetricSeedDensity;
	private final int ContainerWidth, ContainerLength;
	private int[] seedTypeWeights;
	private final static int[] SEED_TYPE_CODES = new int[] { 0, 1, 2, 3 };
	private final int MinHeightBeforeOscillation;
	private final boolean SmoothWithStairs, MakeFloors;
	private TemplateRule[] blockRules;

	//****************************************  CONSTRUCTOR - WorldGenCARuins  *************************************************************************************//
	public WorldGenCARuins(PopulatorCARuins ca, World world, Random random, int chunkI, int chunkK, int triesPerChunk, double chunkTryProb) {
		super(ca, world, random, chunkI, chunkK, triesPerChunk, chunkTryProb);
		caRulesWeightsAndIndex = ca.caRulesWeightsAndIndex;
		MinHeight = ca.MinHeight;
		MaxHeight = ca.MaxHeight;
		GlobalFrequency = ca.GlobalFrequency;
		SymmetricSeedDensity = ca.SymmetricSeedDensity;
		ContainerWidth = ca.ContainerWidth;
		ContainerLength = ca.ContainerLength;
		seedTypeWeights = ca.seedTypeWeights;
		MinHeightBeforeOscillation = ca.MinHeightBeforeOscillation;
		SmoothWithStairs = ca.SmoothWithStairs;
		MakeFloors = ca.MakeFloors;
		blockRules = ca.blockRules;
	}

	//****************************************  FUNCTION - generate  *************************************************************************************//
	@Override
	public boolean generate(int i0, int j0, int k0) {
		int th = MinHeight + random.nextInt(MaxHeight - MinHeight + 1);
		if (caRule == null) //if we haven't picked in an earlier generate call 
			caRule = ((PopulatorCARuins) master).caRules.get(Building.pickWeightedOption(world.rand, caRulesWeightsAndIndex[0], caRulesWeightsAndIndex[1]));
		if (caRule == null)
			return false;
		int seedCode = Building.pickWeightedOption(world.rand, seedTypeWeights, SEED_TYPE_CODES);
		byte[][] seed = seedCode == 0 || (caRule[0][0] == 0 && caRule[0][1] == 0 && caRule[0][2] == 0 && caRule[0][3] == 0) //only use symmetric for 4-rules
		? BuildingCellularAutomaton.makeSymmetricSeed(Math.min(ContainerWidth, ContainerLength), SymmetricSeedDensity, world.rand) : seedCode == 1 ? BuildingCellularAutomaton.makeLinearSeed(
				ContainerWidth, world.rand) : seedCode == 2 ? BuildingCellularAutomaton.makeCircularSeed(Math.min(ContainerWidth, ContainerLength), world.rand) : BuildingCellularAutomaton
				.makeCruciformSeed(Math.min(ContainerWidth, ContainerLength), world.rand);
		TemplateRule blockRule = blockRules[world.getBiomeGenForCoordsBody(i0, k0).biomeID + 1];
		//can use this to test out new Building classes
		/*
		 * BuildingSpiralStaircase bss=new
		 * BuildingSpiralStaircase(this,blockRule
		 * ,random.nextInt(4),2*random.nextInt
		 * (2)-1,false,-(random.nextInt(10)+1),new int[]{i0,j0,k0});
		 * bss.build(0,0); bss.bottomIsFloor(); return true;
		 */
		BuildingCellularAutomaton bca = new BuildingCellularAutomaton(this, blockRule, random.nextInt(4), 1, false, ContainerWidth, th, ContainerLength, seed, caRule, null, new int[] { i0, j0, k0 });
		if (bca.plan(true, MinHeightBeforeOscillation) && bca.queryCanBuild(0, true)) {
			bca.build(SmoothWithStairs, MakeFloors);
			if (GlobalFrequency < 0.05 && random.nextInt(2) != 0) {
				for (int tries = 0; tries < 10; tries++) {
					int[] pt = new int[] { i0 + (2 * random.nextInt(2) - 1) * (ContainerWidth + random.nextInt(ContainerWidth)), 0,
							k0 + (2 * random.nextInt(2) - 1) * (ContainerWidth + random.nextInt(ContainerWidth)) };
					pt[1] = Building.findSurfaceJ(world, pt[0], pt[2], Building.WORLD_MAX_Y, true, 3) + 1;
					if (generate(pt[0], pt[1], pt[2])) {
						break;
					}
				}
			}
			return true;
		}
		return false;
	}
}