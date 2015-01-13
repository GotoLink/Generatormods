package assets.generator;

/*
 *  Source code for the CellullarAutomata Ruins and Walled City Generator Mods for the game Minecraft
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

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/*
 * BuildingSpiralStaircase plans and builds a 3x3 spiral staircase down from origin.
 */
public final class BuildingSpiralStaircase extends Building {
	public BuildingSpiralStaircase(WorldGeneratorThread wgt_, TemplateRule bRule_, Direction bDir_, int axXHand_, boolean centerAligned_, int height, int[] sourcePt) {
		super(0, wgt_, bRule_, bDir_, axXHand_, centerAligned_, new int[] { 3, height, 3 }, sourcePt);
	}

	public boolean bottomIsFloor() {
		int x = calcBottomX(bHeight), y = calcBottomY(bHeight);
		Direction btDir = Direction.NORTH.rotate(-bHeight - 2);
		return isFloor(x + btDir.toX(), bHeight, y + btDir.toY());
	}

	//builds a clockwise down spiral staircase with central column at (x,z,y) with end at top going in local direction topDir
	//yP is the y-value of the endpoint of a potential bottom passage link. If yP==0, then no bottom passage.
	//z is fixed at top and bottom z varies depending on yP
	//
	// Example, bheight=-7
	//
	// *|	z=0 leading stair
	// *|	z=-1 (start of loop)
	// o|
	//  |x
	//  xo
	// o|
	// o|
	//  |x
	//  |x	z=bHeight=-7, (2-in-a-row bottom stair), xfinal=2, yfinal=0
	//
	public void build(int extraTopStairs, int yP) {
		Block stairsBlockId = bRule.primaryBlock.toStair();
		Direction sDir = Direction.SOUTH;
		setBlockLocal(1, 0, 1, bRule);
		int jB0 = getSurfaceIJKPt(0, yP, j0 + bHeight + 2, true, 0)[1] + 1;
		int jB2 = getSurfaceIJKPt(2, yP, j0 + bHeight + 2, true, 0)[1] + 1;
		int pYInc = Integer.signum(yP);
		for (int n = 0; n <= extraTopStairs; n++)
			buildStairwaySegment(0, n, -n, 3, stairsBlockId, sDir);
		int x = 0, y = 1;
		setBlockLocal(x, 2, y, Blocks.air);
		for (int z = -1; z >= bHeight; z--) {
			buildStairwaySegment(x, z, y, 2, stairsBlockId, sDir);
			setBlockLocal(1, z, 1, bRule); //central column
			x -= sDir.toX();
			y -= sDir.toY();
			if (z == bHeight + 1) {
				z--; //bottommost stair is two in a row
				buildStairwaySegment(x, z, y, 3, stairsBlockId, sDir);
				setBlockLocal(1, z, 1, bRule);
				x -= sDir.toX();
				y -= sDir.toY();
			}
			buildHallwaySegment(x, z, y, 3);
			//Bottom stair can start from 3 out of 4 positions
			// pYInc
			//  ^
			//  s3 > s0
			//	^   v
			//  s2 < s1
			if (yP != 0) {
				int zP = (x == 0 ? jB0 : jB2) - j0;
				if (y == pYInc + 1 && Math.abs(y - yP) > z - zP //s3
						|| y == pYInc + 1 && Math.abs(y - yP) >= z - zP && sDir.toY() != 0 //s0
						|| y != pYInc + 1 && Math.abs(y - yP) > z - zP && sDir.rotate(1).toY() != 0) //s2
				{
					if (sDir.toY() != 0) {
						setBlockLocal(x, z - 1, y, stairsBlockId, STAIRS_DIR_TO_META[sDir.ordinal()]);
						z--;
					}
					for (int y1 = y + pYInc; y1 != yP; y1 += pYInc) {
						if (z - zP > 0) {
							z--;
							buildStairwaySegment(x, z, y1, 3, stairsBlockId, Direction.EAST.rotate(pYInc));
						} else {
							if (y1 == pYInc + 1 && !isWallBlock(x, z, y1 - pYInc)) //avoid undermining stairway above
								buildHallwaySegment(x, z, y1, 2);
							else
								buildHallwaySegment(x, z, y1, 3);
						}
					}
					break;
				}
			}
			sDir = sDir.rotate(1);
			x -= sDir.toX();
			y -= sDir.toY();
		}
        flushDelayed();
	}

	private void buildHallwaySegment(int x, int z, int y, int height) {
		setBlockLocal(x, z - 1, y, bRule);
		for (int z1 = z; z1 < z + height; z1++)
			setBlockLocal(x, z1, y, Blocks.air);
	}

	private void buildStairwaySegment(int x, int z, int y, int height, Block stairsBlockId, Direction sDir) {
		setBlockLocal(x, z - 1, y, bRule);
		setBlockLocal(x, z, y, stairsBlockId, STAIRS_DIR_TO_META[sDir.ordinal()]);
		for (int z1 = z + 1; z1 <= z + height; z1++)
			setBlockLocal(x, z1, y, Blocks.air);
	}

	//calcBottomX, calcBottomY are for use when yP==0
	public static int calcBottomX(int height) {
		if (height == 1)
			return 0;
		return 2 * ((-height - 1) / 2 % 2);
	}

	public static int calcBottomY(int height) {
		if (height == 1)
			return 1;
		return 2 * (-height / 2 % 2);
	}

    @Override
    protected BlockAndMeta getDelayedStair(Block blc, int...block){
        return new BlockAndMeta(blc, block[3]);
    }
}