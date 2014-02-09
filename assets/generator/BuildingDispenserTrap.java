package assets.generator;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;

/*
 *  Source code for the Walled City Generator and CARuins Mods for the game Minecraft
 *  Copyright (C) 2011 by formivore

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * BuildingDispenserTrap generates a redstone activated dispenser trap
 */
public class BuildingDispenserTrap extends Building {
	public final static int ARROW_MISSILE = 0, DAMAGE_POTION_MISSILE = 1;
	private static BlockAndMeta[] CODE_TO_BLOCK = new BlockAndMeta[] { PRESERVE_BLOCK, null, new BlockAndMeta(Blocks.air, 0), new BlockAndMeta(Blocks.redstone_wire, 0), new BlockAndMeta(Blocks.redstone_torch, BUTTON_DIR_TO_META[DIR_NORTH]),
            new BlockAndMeta(Blocks.unlit_redstone_torch, BUTTON_DIR_TO_META[DIR_SOUTH]), new BlockAndMeta(Blocks.redstone_torch, 5) };
	private static int[][][] MECHANISM = new int[][][] { { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 1, 0 }, { 0, 0, 0 } }, { { 0, 0, 0 }, { 1, 1, 1 }, { 1, 4, 1 }, { 1, 1, 1 } },
			{ { 0, 0, 0 }, { 1, 1, 1 }, { 1, 1, 1 }, { 1, 5, 1 } }, { { 0, 1, 0 }, { 1, 1, 1 }, { 1, 3, 1 }, { 1, 1, 1 } }, { { 0, 1, 0 }, { 1, 6, 1 }, { 1, 0, 1 }, { 1, 2, 1 } },
			{ { 0, 1, 0 }, { 1, 1, 1 }, { 1, 1, 1 }, { 1, 1, 1 } }, };

	public BuildingDispenserTrap(WorldGeneratorThread wgt_, TemplateRule bRule_, int bDir_, int plateSeparation, int[] sourcePt) {
		super(0, wgt_, bRule_, bDir_, 1, true, new int[] { 3, 6, plateSeparation }, sourcePt);
	}

	//      ---   bLength+3 - end of mechanism
	//      | |
	//      | |
	//      ---   y=bLength - mechanism start,
	//       *    y==bLength-1 - end of redstone wire
	//       *
	//       0    y=0 - trigger plate
	public void build(int missileType, boolean multipleTriggers) {
		if (bLength < 0)
			bLength = 0;
		//if (BuildingWall.DEBUG) FMLLog.getLogger().info("Building dispenser trap at "+i0+","+j0+","+k0+", plateSeparation="+bLength);
		for (int x = 0; x < MECHANISM[0][0].length; x++) {
			for (int y = 0; y < MECHANISM[0].length; y++) {
				for (int z = 0; z < MECHANISM.length; z++) {
					if (MECHANISM[z][3 - y][x] == 1)
						setBlockLocal(x, z - 3, y + bLength, bRule);
					else
						setBlockLocal(x, z - 3, y + bLength, CODE_TO_BLOCK[MECHANISM[z][3 - y][x]]);
				}
			}
		}
		for (int y = 0; y < bLength; y++) {
			setBlockLocal(1, -3, y, bRule);
			setBlockLocal(1, -2, y, Blocks.redstone_wire);
			setBlockLocal(0, -2, y, bRule);
			setBlockLocal(2, -2, y, bRule);
			setBlockLocal(1, -1, y, bRule);
			setBlockLocal(1, 0, y, multipleTriggers && random.nextBoolean() ? Blocks.stone_pressure_plate : Blocks.air);
			setBlockLocal(1, 1, y, Blocks.air);
		}
		setBlockLocal(1, 0, 0, Blocks.stone_pressure_plate);
		ItemStack itemstack = missileType == ARROW_MISSILE ? new ItemStack(Items.arrow, 30 + random.nextInt(10), 0) : new ItemStack(Items.potionitem, 30 + random.nextInt(10), 12 | 0x4000);
		setItemDispenser(1, 1, bLength + 1, DIR_SOUTH, itemstack);
	}

	public boolean queryCanBuild(int minLength) {
		if (!isFloor(1, 0, 0))
			return false;
		//search along floor for a height 2 wall. If we find it, reset bLength and return true.
		for (int y = 1; y < 9; y++) {
			if (isFloor(1, 0, y))
				continue;
			if (!isWallBlock(1, -1, y))
				return false;
			//now must have block at (1,0,y)...
			if (y < minLength)
				return false;
			if (!isWallable(1, 1, y)) {
				bLength = y;
				return true;
			}
			return false;
		}
		return false;
	}

	private void setItemDispenser(int x, int z, int y, int metaDir, ItemStack itemstack) {
		int[] pt = getIJKPt(x, z, y);
		world.func_147465_d(pt[0], pt[1], pt[2], Blocks.dispenser, 0, 2);
		world.setBlockMetadataWithNotify(pt[0], pt[1], pt[2], LADDER_DIR_TO_META[orientDirToBDir(metaDir)], 3);
		try {
			TileEntityDispenser tileentitychest = (TileEntityDispenser) world.func_147438_o(pt[0], pt[1], pt[2]);
			if (itemstack != null && tileentitychest != null)
				tileentitychest.setInventorySlotContents(random.nextInt(tileentitychest.getSizeInventory()), itemstack);
		} catch (Exception e) {
			System.err.println("Error filling dispenser: " + e.toString());
			e.printStackTrace();
		}
	}
}