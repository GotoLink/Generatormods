package assets.generator;

/*
 *  Source code for the The Great Wall Mod for the game Minecraft
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

import java.util.Random;

@SuppressWarnings("unused")
public final class WorldGenSingleWall extends WorldGeneratorThread {
	private int[] pt;

	public WorldGenSingleWall(PopulatorGreatWall gw, World world, Random random, int[] pt) {
		super(gw, world, random, pt[0] >> 4, pt[2] >> 4, 0, 0.0);
		this.pt = pt;
	}

	@Override
	public boolean generate(int i0, int j0, int k0) {
		TemplateWall ws = TemplateWall.pickBiomeWeightedWallStyle(((PopulatorGreatWall) master).wallStyles, world, i0, k0, world.rand, false);
		BuildingWall wall = new BuildingWall(0, this, ws, Building.Direction.NORTH, Building.R_HAND, ws.MaxL, true, i0, j0, k0);
		//BuildingWall(int ID_, WorldGeneratorThread wgt_,WallStyle ws_,int dir_,int axXHand_, int maxLength_,int i0_,int j0_, int k0_){
		//wall.setTarget(((PopulatorGreatWall) master).placedCoords);
		wall.plan(1, 0, ws.MergeWalls ? ws.WWidth : BuildingWall.DEFAULT_LOOKAHEAD, false);
		//plan(int Backtrack, int startN, int depth, int lookahead, boolean stopAtWall) throws InterruptedException {
		if (wall.bLength >= wall.y_targ) {
			wall.smooth(ws.ConcaveDownSmoothingScale, ws.ConcaveUpSmoothingScale, true);
			wall.buildFromTML();
			wall.makeBuildings(true, true, true, false, false);
			//((PopulatorGreatWall) master).placedCoords = null;
		}
		return true;
	}
}