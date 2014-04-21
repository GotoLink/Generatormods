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
 * BuildingDoubleWall creates double-ended walls
 */
public class BuildingDoubleWall extends Building {
	public BuildingWall wall1, wall2;
	public final TemplateWall ws;

	//****************************  CONSTRUCTOR - BuildingDoubleWall *************************************************************************************//
	public BuildingDoubleWall(int ID_, WorldGeneratorThread wgt_, TemplateWall ws_, int dir_, int axXHand_, int[] sourcePt) {
		super(ID_, wgt_, ws_.TowerRule, dir_, axXHand_, false, new int[] { ws_.WWidth, ws_.WHeight, 0 }, sourcePt);
		ws = ws_;
	}

	//****************************  FUNCTION - plan  *************************************************************************************//
	public boolean plan() {
		//Plan out a pair of walls in opposite directions from given start coordinates.
		//Start planning from position 1 (pos 0 is fixed).
		wall1 = new BuildingWall(bID, wgt, ws, bDir, Building.R_HAND, ws.MaxL / 2, true, getIJKPt(0, 0, 0));
		wall2 = new BuildingWall(bID, wgt, ws, flipDir(bDir), Building.L_HAND, ws.MaxL / 2, true, getIJKPt(0, 0, -1)).setTowers(wall1);
		int a = wall1.plan(1, 0, ws.MergeWalls ? ws.WWidth : BuildingWall.DEFAULT_LOOKAHEAD, !ws.MergeWalls) + 1;
		int b = wall2.plan(1, 0, ws.MergeWalls ? ws.WWidth : BuildingWall.DEFAULT_LOOKAHEAD, !ws.MergeWalls) + 1;
		if (b + a - 1 < ws.MinL) {
			if (BuildingWall.DEBUG)
				System.out.println("Abandoning wall " + wall1.IDString() + "length=" + (b + a - 1) + ", reason 1)" + wall1.failString() + ". 2)" + wall2.failString() + ".");
			return false;
		}
		wall1.printWall();
		wall2.printWall();
		//copy to one array for smoothing
		//
		// [0, ... temp.... ,b-1,b,...     ...,a+b-1]
		// [b-1,... wall2 ...,0][0,... wall1 ...,a-1]
		//
		//note this is different th
		//
		int[] tempx = new int[a + b];
		int[] tempz = new int[a + b];
		for (int m = 0; m < b; m++) {
			tempx[m] = wall2.xArray[b - m - 1];
			tempz[m] = wall2.zArray[b - m - 1];
		}
		for (int m = 0; m < a; m++) {
			tempx[m + b] = wall1.xArray[m];
			tempz[m + b] = wall1.zArray[m];
		}
		if (BuildingWall.DEBUG)
			System.out.println("\nSMOOTHING X");
		BuildingWall.smooth(tempx, 0, a + b - 1, ws.LateralSmoothingScale, ws.LateralSmoothingScale, true);
		if (BuildingWall.DEBUG)
			System.out.println("\nSMOOTHING Z");
		BuildingWall.smooth(tempz, 0, a + b - 1, ws.ConcaveDownSmoothingScale, ws.ConcaveUpSmoothingScale, true);
		for (int m = 0; m < b; m++) {
			wall2.xArray[b - m - 1] = tempx[m];
			wall2.zArray[b - m - 1] = tempz[m];
		}
		for (int m = 0; m < a; m++) {
			wall1.xArray[m] = tempx[m + b];
			wall1.zArray[m] = tempz[m + b];
		}
		return true;
	}

	public void build(int layoutCode) {
		ws.setFixedRules(world.rand);
		if (layoutCode != WorldGeneratorThread.LAYOUT_CODE_NOCODE) {
			wall1.setLayoutCode(layoutCode);
			wall2.setLayoutCode(layoutCode);
		}
		wall1.buildFromTML();
		wall2.buildFromTML();
	}

	public void buildTowers(boolean lSideTowers, boolean rSideTowers, boolean gatehouseTowers, boolean overlapTowers, boolean isAvenue) {
		wall1.makeBuildings(lSideTowers, rSideTowers, gatehouseTowers, overlapTowers, isAvenue);
		wall2.makeBuildings(lSideTowers, rSideTowers, gatehouseTowers, overlapTowers, isAvenue);
	}
}