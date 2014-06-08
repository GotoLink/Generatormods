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
 *      Building is a general class for buildings. Classes can inherit from Building to build from a local frame of reference.
 * 
 *  Local frame of reference variables:
 *     i,j,k are coordinate inputs for global frame of reference functions.
 *     x,z,y are coordinate inputs for local frame of reference functions.
 *     bHand =-1,1 determines whether X-axis points left or right respectively when facing along Y-axis.
 *
 *               (dir=0)
 *                (-k)
 *                 n
 *                 n
 *  (dir=3) (-i)www*eee(+i)  (dir=1)
 *                 s
 *                 s
 *                (+k)
 *               (dir=2)
 */
import java.util.*;

import net.minecraft.block.*;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.village.VillageDoorInfo;
import net.minecraft.world.World;

public class Building {
	public final static int HIT_WATER = -666; // , HIT_SWAMP=-667;
	public final static String EASY_CHEST = "EASY", MEDIUM_CHEST = "MEDIUM", HARD_CHEST = "HARD", TOWER_CHEST = "TOWER";
	public final static int DIR_NORTH = 0, DIR_EAST = 1, DIR_SOUTH = 2, DIR_WEST = 3;
	public final static int ROT_R = 1, R_HAND = 1, L_HAND = -1;
	public final static int SEA_LEVEL = 63, WORLD_MAX_Y = 255;
	// **** WORKING VARIABLES ****
	protected final World world;
	protected final Random random;
	protected TemplateRule bRule; // main structural blocktype
	public int bWidth, bHeight, bLength;
	public final int bID; // Building ID number
    private final LinkedList<PlacedBlock> delayedBuildQueue;
	protected final WorldGeneratorThread wgt;
	protected boolean centerAligned; // if true, alignPt x is the central axis of the building if false, alignPt is the origin
	protected int i0, j0, k0; // origin coordinates (x=0,z=0,y=0). The child class may want to move the origin as it progress to use as a "cursor" position.
	private int xI, yI, xK, yK; //
	protected int bHand; // hand of secondary axis. Takes values of 1 for right-handed, -1 for left-handed.
	protected int bDir; // Direction code of primary axis. Takes values of DIR_NORTH=0,DIR_EAST=1,DIR_SOUTH=2,DIR_WEST=3.
	// **************************************** CONSTRUCTOR - findSurfaceJ
	// *************************************************************************************//
	// Finds a surface block.
	// Depending on the value of waterIsSurface and wallIsSurface will treat
	// liquid and wall blocks as either solid or air.
	public final static int IGNORE_WATER = -1;
	// Special Blocks
	public final static int PAINTING_BLOCK_OFFSET = 3;
	public final static String[] SPAWNERS = new String[]{
            "Zombie", "Skeleton", "Spider", "Creeper", "PigZombie", "Ghast", "Enderman", "CaveSpider", "Blaze", "Slime",
            "LavaSlime", "Villager", "SnowMan", "MushroomCow", "Sheep", "Cow", "Chicken", "Squid", "Wolf", "Giant",
            "Silverfish", "EnderDragon", "Ozelot", "VillagerGolem", "WitherBoss", "Bat", "Witch"
    };
	// maps block metadata to a dir
	public final static int[] BED_META_TO_DIR = new int[] { DIR_SOUTH, DIR_WEST, DIR_NORTH, DIR_EAST }, STAIRS_META_TO_DIR = new int[] { DIR_EAST, DIR_WEST, DIR_SOUTH, DIR_NORTH },
			LADDER_META_TO_DIR = new int[] { DIR_NORTH, DIR_SOUTH, DIR_WEST, DIR_EAST }, TRAPDOOR_META_TO_DIR = new int[] { DIR_SOUTH, DIR_NORTH, DIR_EAST, DIR_WEST }, VINES_META_TO_DIR = new int[] {
					0, DIR_SOUTH, DIR_WEST, 0, DIR_NORTH, 0, 0, 0, DIR_EAST }, DOOR_META_TO_DIR = new int[] { DIR_WEST, DIR_NORTH, DIR_EAST, DIR_SOUTH };
	// inverse map should be {North_inv,East_inv,dummy,West_inv,South_inv}
	// inverse map should be {North_inv,East_inv,South_inv, West_inv}
	public final static int[] BED_DIR_TO_META = new int[] { 2, 3, 0, 1 }, BUTTON_DIR_TO_META = new int[] { 4, 1, 3, 2 }, STAIRS_DIR_TO_META = new int[] { 3, 0, 2, 1 }, LADDER_DIR_TO_META = new int[] {
			2, 5, 3, 4 }, TRAPDOOR_DIR_TO_META = new int[] { 1, 2, 0, 3 }, VINES_DIR_TO_META = new int[] { 4, 8, 1, 2 }, DOOR_DIR_TO_META = new int[] { 3, 0, 1, 2 },
			PAINTING_DIR_TO_FACEDIR = new int[] { 0, 3, 2, 1 };
	public final static int[] DIR_TO_I = new int[] { 0, 1, 0, -1 }, DIR_TO_K = new int[] { -1, 0, 1, 0 };
	// for use in local orientation
	public final static int[] DIR_TO_X = new int[] { 0, 1, 0, -1 }, DIR_TO_Y = new int[] { 1, 0, -1, 0 };
	// some prebuilt directional blocks
	public final static BlockAndMeta WEST_FACE_TORCH_BLOCK = new BlockAndMeta(Blocks.torch, BUTTON_DIR_TO_META[DIR_WEST]), EAST_FACE_TORCH_BLOCK = new BlockAndMeta(Blocks.torch, BUTTON_DIR_TO_META[DIR_EAST]),
			NORTH_FACE_TORCH_BLOCK = new BlockAndMeta(Blocks.torch, BUTTON_DIR_TO_META[DIR_NORTH]), SOUTH_FACE_TORCH_BLOCK = new BlockAndMeta(Blocks.torch, BUTTON_DIR_TO_META[DIR_SOUTH]),
			EAST_FACE_LADDER_BLOCK = new BlockAndMeta(Blocks.ladder, LADDER_DIR_TO_META[DIR_EAST]), HOLE_BLOCK_LIGHTING = new BlockAndMeta(Blocks.air, 0), HOLE_BLOCK_NO_LIGHTING = new BlockAndMeta(Blocks.air, 1),
			PRESERVE_BLOCK = new BlockExtended(Blocks.air, 0, "PRESERVE"),
			TOWER_CHEST_BLOCK = new BlockExtended(Blocks.chest, 0, TOWER_CHEST), HARD_CHEST_BLOCK = new BlockExtended(Blocks.chest, 0, HARD_CHEST),
			GHAST_SPAWNER = new BlockExtended(Blocks.mob_spawner, 0, "Ghast");
	public final static int MAX_SPHERE_DIAM = 40;
	public final static int[][] SPHERE_SHAPE = new int[MAX_SPHERE_DIAM + 1][];
	public final static int[][][] CIRCLE_SHAPE = new int[MAX_SPHERE_DIAM + 1][][], CIRCLE_CRENEL = new int[MAX_SPHERE_DIAM + 1][][];
	static {
		for (int diam = 1; diam <= MAX_SPHERE_DIAM; diam++) {
			circleShape(diam);
		}
		// change diam 6 shape to look better
		CIRCLE_SHAPE[6] = new int[][] { { -1, -1, 1, 1, -1, -1 }, { -1, 1, 0, 0, 1, -1 }, { 1, 0, 0, 0, 0, 1 }, { 1, 0, 0, 0, 0, 1 }, { -1, 1, 0, 0, 1, -1 }, { -1, -1, 1, 1, -1, -1 } };
		CIRCLE_CRENEL[6] = new int[][] { { -1, -1, 1, 0, -1, -1 }, { -1, 0, 0, 0, 1, -1 }, { 1, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 1 }, { -1, 1, 0, 0, 0, -1 }, { -1, -1, 0, 1, -1, -1 } };
	}
	private final static int LIGHTING_INVERSE_DENSITY = 10;
	private final static boolean[] randLightingHash = new boolean[512];
	static {
		Random rand = new Random();
		for (int m = 0; m < randLightingHash.length; m++)
			randLightingHash[m] = rand.nextInt(LIGHTING_INVERSE_DENSITY) == 0;
	}
	public final static String[] CHEST_TYPE_LABELS = new String[] { "EASY", "MEDIUM", "HARD", "TOWER" };
	public final static int[] DEFAULT_CHEST_TRIES = new int[] { 4, 6, 6, 6 };
	// chest items[n] format is array of 6 arrays
	// 0array - idx
	// 1array - blockId
	// 2array - block damage/meta
	// 3array - block weight
	// 4array - block min stacksize
	// 5array - block max stacksize
	public final static Object[][][] DEFAULT_CHEST_ITEMS = new Object[][][] { { // Easy
			{ 0, Items.arrow, 0, 2, 1, 12 }, { 1, Items.iron_sword, 0, 2, 1, 1 }, { 2, Items.leather_leggings, 0, 1, 1, 1 }, { 3, Items.iron_shovel, 0, 1, 1, 1 },
					{ 4, Items.string, 0, 1, 1, 1 }, { 5, Items.iron_pickaxe, 0, 2, 1, 1 }, { 6, Items.leather_boots, 0, 1, 1, 1 }, { 7, Items.bucket, 0, 1, 1, 1 },
					{ 8, Items.leather_helmet, 0, 1, 1, 1 }, { 9, Items.wheat_seeds, 0, 1, 10, 15 }, { 10, Items.gold_nugget, 0, 2, 3, 8 }, { 11, Items.potionitem, 5, 2, 1, 1 }, // healing I
					{ 12, Items.potionitem, 4, 1, 1, 1 } }, // poison, hehe
			{ // Medium
			{ 0, Items.golden_sword, 0, 2, 1, 1 }, { 1, Items.milk_bucket, 0, 2, 1, 1 }, { 2, Blocks.web, 0, 1, 8, 16 }, { 3, Items.golden_shovel, 0, 1, 1, 1 },
					{ 4, Items.golden_hoe, 0, 1, 0, 1 }, { 5, Items.clock, 0, 1, 1, 1 }, { 6, Items.iron_axe, 0, 3, 1, 1 }, { 7, Items.map, 0, 1, 1, 1 },
					{ 8, Items.apple, 0, 2, 2, 3 }, { 9, Items.compass, 0, 1, 1, 1 }, { 10, Items.iron_ingot, 0, 1, 5, 8 }, { 11, Items.slime_ball, 0, 1, 1, 3 },
					{ 12, Blocks.obsidian, 0, 1, 1, 4 }, { 13, Items.bread, 0, 2, 8, 15 }, { 14, Items.potionitem, 2, 1, 1, 1 }, { 15, Items.potionitem, 37, 3, 1, 1 }, // healing II
					{ 16, Items.potionitem, 34, 1, 1, 1 }, // swiftness II
					{ 17, Items.potionitem, 9, 1, 1, 1 } }, // strength
			{ // Hard
			{ 0, Blocks.sticky_piston, 0, 2, 6, 12 }, { 1, Blocks.web, 0, 1, 8, 24 }, { 2, Items.cookie, 0, 2, 8, 18 }, { 3, Items.diamond_axe, 0, 1, 1, 1 },
					{ 4, Items.minecart, 0, 1, 12, 24 }, { 5, Items.redstone, 0, 2, 12, 24 }, { 6, Items.lava_bucket, 0, 2, 1, 1 }, { 7, Items.ender_pearl, 0, 1, 1, 1 },
					{ 8, Blocks.mob_spawner, 0, 1, 2, 4 }, { 9, Items.record_13, 0, 1, 1, 1 }, { 10, Items.golden_apple, 0, 1, 4, 8 }, { 11, Blocks.tnt, 0, 2, 8, 20 },
					{ 12, Items.diamond, 0, 2, 1, 4 }, { 13, Items.gold_ingot, 0, 2, 30, 64 }, { 14, Items.potionitem, 37, 3, 1, 1 }, // healing II
					{ 15, Items.potionitem, 49, 2, 1, 1 }, // regeneration II
					{ 16, Items.potionitem, 3, 2, 1, 1 } }, // fire resistance
			{ // Tower
			{ 0, Items.arrow, 0, 1, 1, 12 }, { 1, Items.fish, 0, 2, 1, 1 }, { 2, Items.golden_helmet, 0, 1, 1, 1 }, { 3, Blocks.web, 0, 1, 1, 12 },
					{ 4, Items.iron_ingot, 0, 1, 2, 3 }, { 5, Items.stone_sword, 0, 1, 1, 1 }, { 6, Items.iron_axe, 0, 1, 1, 1 }, { 7, Items.egg, 0, 2, 8, 16 },
					{ 8, Items.saddle, 0, 1, 1, 1 }, { 9, Items.wheat, 0, 2, 3, 6 }, { 10, Items.gunpowder, 0, 1, 2, 4 }, { 11, Items.leather_chestplate, 0, 1, 1, 1 },
					{ 12, Blocks.pumpkin, 0, 1, 1, 5 }, { 13, Items.gold_nugget, 0, 2, 1, 3 } } };

    // **************************** CONSTRUCTORS - Building
    // *************************************************************************************//
    public Building(int ID_, WorldGeneratorThread wgt_, TemplateRule buildingRule_, int dir_, int axXHand_, boolean centerAligned_, int[] dim, int[] alignPt) {
        bID = ID_;
        wgt = wgt_;
        world = wgt.world;
        bRule = buildingRule_;
        if (bRule == null)
            bRule = TemplateRule.STONE_RULE;
        bWidth = dim[0];
        bHeight = dim[1];
        bLength = dim[2];
        random = wgt.random;
        bHand = axXHand_;
        centerAligned = centerAligned_;
        setPrimaryAx(dir_);
        if (alignPt != null && alignPt.length == 3) {
            if (centerAligned)
                setOrigin(alignPt[0] - xI * bWidth / 2, alignPt[1], alignPt[2] - xK * bWidth / 2);
            else
                setOrigin(alignPt[0], alignPt[1], alignPt[2]);
        }
        delayedBuildQueue = new LinkedList<PlacedBlock>();
    }
	// ******************** LOCAL COORDINATE FUNCTIONS - ACCESSORS
	// *************************************************************************************************************//
	// Use these instead of World.java functions when to build from a local
	// reference frame
	// when i0,j0,k0 are set to working values.
	public final int getI(int x, int y) {
		return i0 + yI * y + xI * x;
	}

	public final int[] getIJKPt(int x, int z, int y) {
		int[] pt = new int[3];
		pt[0] = i0 + yI * y + xI * x;
		pt[1] = j0 + z;
		pt[2] = k0 + yK * y + xK * x;
		return pt;
	}

	public final int getJ(int z) {
		return j0 + z;
	}

	public final int getK(int x, int y) {
		return k0 + yK * y + xK * x;
	}

	public final int[] getSurfaceIJKPt(int x, int y, int j, boolean wallIsSurface, int waterSurfaceBuffer) {
		int[] pt = getIJKPt(x, 0, y);
		pt[1] = findSurfaceJ(world, pt[0], pt[2], j, wallIsSurface, waterSurfaceBuffer);
		return pt;
	}

	public final int getX(int[] pt) {
		return xI * (pt[0] - i0) + xK * (pt[2] - k0);
	}

	public final int getY(int[] pt) {
		return yI * (pt[0] - i0) + yK * (pt[2] - k0);
	}

	public final int getZ(int[] pt) {
		return pt[1] - j0;
	}

	public final String localCoordString(int x, int z, int y) {
		int[] pt = getIJKPt(x, z, y);
		return "(" + pt[0] + "," + pt[1] + "," + pt[2] + ")";
	}

	// outputs dir rotated to this Building's orientation and handedness
	// dir input should be the direction desired if bDir==DIR_NORTH and
	// bHand=R_HAND
	public int orientDirToBDir(int dir) {
		return bHand < 0 && dir % 2 == 1 ? (bDir + dir + 2) & 0x3 : (bDir + dir) & 0x3;
	}

	// &&&&&&&&&&&&&&&&& SPECIAL BLOCK FUNCTION - setPainting
	// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&//
	public void setPainting(int[] pt, int metadata) {
		// painting uses same orientation meta as ladders.
		// Have to adjust ijk since unlike ladders the entity exists at the
		// block it is hung on.
		int dir = orientDirToBDir(LADDER_META_TO_DIR[metadata]);
		pt[0] -= DIR_TO_I[dir];
		pt[2] -= DIR_TO_K[dir];
		if (dir == DIR_NORTH)
			pt[2]++;
		else if (dir == DIR_SOUTH)
			pt[2]--;
		else if (dir == DIR_WEST)
			pt[0]++;
		else
			pt[0]--;
		EntityPainting entitypainting = new EntityPainting(world, pt[0], pt[1], pt[2], PAINTING_DIR_TO_FACEDIR[dir]);
		if (!world.isRemote && entitypainting.onValidSurface())
			world.spawnEntityInWorld(entitypainting);
	}

	// ******************** ORIENTATION FUNCTIONS
	// *************************************************************************************************************//
	public void setPrimaryAx(int dir_) {
		bDir = dir_;
		// changes of basis
		switch (bDir) {
		case DIR_NORTH:
			xI = bHand;
			yI = 0;
			xK = 0;
			yK = -1;
			break;
		case DIR_EAST:
			xI = 0;
			yI = 1;
			xK = bHand;
			yK = 0;
			break;
		case DIR_SOUTH:
			xI = -bHand;
			yI = 0;
			xK = 0;
			yK = 1;
			break;
		case DIR_WEST:
			xI = 0;
			yI = -1;
			xK = -bHand;
			yK = 0;
			break;
		}
	}

	// &&&&&&&&&&&&&&&&& SPECIAL BLOCK FUNCTION - setSignOrPost
	// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&//
	public void setSignOrPost(int x2, int z2, int y2, boolean post, int sDir, String[] lines) {
		int[] pt = getIJKPt(x2, z2, y2);
		world.setBlock(pt[0], pt[1], pt[2], post ? Blocks.standing_sign : Blocks.wall_sign, sDir, 2);
		TileEntitySign tileentitysign = (TileEntitySign) world.getTileEntity(pt[0], pt[1], pt[2]);
		if (tileentitysign == null)
			return;
        System.arraycopy(lines, 0, tileentitysign.signText, 0, Math.min(lines.length, 4));
	}

	// call with z=start of builDown, will buildDown a maximum of maxDepth
	// blocks + foundationDepth.
	// if buildDown column is completely air, instead buildDown reserveDepth
	// blocks.
	public void buildDown(int x, int z, int y, TemplateRule buildRule, int maxDepth, int foundationDepth, int reserveDepth) {
		int stopZ;
		for (stopZ = z; stopZ > z - maxDepth; stopZ--) {
			if (!isWallable(x, stopZ, y))
				break; // find ground height
		}
		if (stopZ == z - maxDepth && isWallable(x, z - maxDepth, y)) // if we never hit ground
			stopZ = z - reserveDepth;
		else
			stopZ -= foundationDepth;
		for (int z1 = z; z1 > stopZ; z1--) {
			setBlockWithLightingLocal(x, z1, y, buildRule, false);
		}
	}

	protected final Block getBlockIdLocal(int x, int z, int y) {
		return world.getBlock(i0 + yI * y + xI * x, j0 + z, k0 + yK * y + xK * x);
	}

	protected final int getBlockMetadataLocal(int x, int z, int y) {
		return world.getBlockMetadata(i0 + yI * y + xI * x, j0 + z, k0 + yK * y + xK * x);
	}

	// replaces orientationString
	protected final String IDString() {
		String str = "ID=" + bID + " axes(Y,X)=";
		switch (bDir) {
		case DIR_SOUTH:
			return str + "(S," + (bHand > 0 ? "W)" : "E)");
		case DIR_NORTH:
			return str + "(N," + (bHand > 0 ? "E)" : "W)");
		case DIR_WEST:
			return str + "(W," + (bHand > 0 ? "N)" : "S)");
		case DIR_EAST:
			return str + "(E," + (bHand > 0 ? "S)" : "N)");
		}
		return "Error - bad dir value for ID=" + bID;
	}

	protected final boolean isArtificialWallBlock(int x, int z, int y) {
		Block blockId = getBlockIdLocal(x, z, y);
		return BlockProperties.get(blockId).isArtificial && !(blockId == Blocks.sandstone && (getBlockIdLocal(x, z + 1, y) == Blocks.sand || getBlockIdLocal(x, z + 2, y) == Blocks.sand));
	}

	protected final boolean isDoorway(int x, int z, int y) {
		return isFloor(x, z, y) && (isWallBlock(x + 1, z, y) && isWallBlock(x - 1, z, y) || isWallBlock(x, z, y + 1) && isWallBlock(x - 1, z, y - 1));
	}

	protected final boolean hasNoDoorway(int x, int z, int y) {
		return !(isDoorway(x - 1, z, y) || isDoorway(x + 1, z, y) || isDoorway(x, z, y - 1) || isDoorway(x - 1, z, y + 1));
	}

	protected boolean isObstructedFrame(int zstart, int ybuffer) {
		for (int z1 = zstart; z1 < bHeight; z1++) {
			// for(int x1=0; x1<length; x1++) for(int y1=ybuffer;
			// y1<width-1;y1++)
			// if(isWallBlock(x1,z1,y1))
			// return true;
			for (int x1 = 0; x1 < bWidth; x1++) {
				if (isArtificialWallBlock(x1, z1, bLength - 1)) {
					return true;
				}
			}
			for (int y1 = ybuffer; y1 < bLength - 1; y1++) {
				if (isArtificialWallBlock(0, z1, y1)) {
					return true;
				}
				if (isArtificialWallBlock(bWidth - 1, z1, y1)) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean isObstructedSolid(int pt1[], int pt2[]) {
		for (int x1 = pt1[0]; x1 <= pt2[0]; x1++) {
			for (int z1 = pt1[1]; z1 <= pt2[1]; z1++) {
				for (int y1 = pt1[2]; y1 <= pt2[2]; y1++) {
					if (!isWallable(x1, z1, y1)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// ******************** LOCAL COORDINATE FUNCTIONS - BLOCK TEST FUNCTIONS
	// *************************************************************************************************************//
    // true if block is air, block below is wall block
    protected final boolean isFloor(int x, int z, int y) {
        Block blkId1 = getBlockIdLocal(x, z, y), blkId2 = getBlockIdLocal(x, z - 1, y);
        // return ((blkId1==0 || blkId1==STEP_ID) && IS_WALL_BLOCK[blkId2] &&
        // blkId2!=LADDER_ID);
        return blkId1 == Blocks.air && BlockProperties.get(blkId2).isArtificial && blkId2 != Blocks.ladder;
    }

    protected final boolean isStairBlock(int x, int z, int y) {
        Block blkId = getBlockIdLocal(x, z, y);
        return blkId == Blocks.stone_slab || BlockProperties.get(blkId).isStair;
    }

	protected final boolean isWallable(int x, int z, int y) {
		return BlockProperties.get(world.getBlock(i0 + yI * y + xI * x, j0 + z, k0 + yK * y + xK * x)).isWallable;
	}

	protected final boolean isWallableIJK(int pt[]) {
		return pt!=null && BlockProperties.get(world.getBlock(pt[0], pt[1], pt[2])).isWallable;
	}

	protected final boolean isWallBlock(int x, int z, int y) {
		return BlockProperties.get(world.getBlock(i0 + yI * y + xI * x, j0 + z, k0 + yK * y + xK * x)).isArtificial;
	}

    public final void flushDelayed(){
        while(delayedBuildQueue.size()>0){
            PlacedBlock block = delayedBuildQueue.poll();
            setDelayed(block.get(), block.x, block.y, block.z, block.getMeta());
        }
    }

	protected void setDelayed(Block blc, int...block) {
		if (BlockProperties.get(blc).isStair) {
            BlockAndMeta temp = getDelayedStair(blc, block);
            blc = temp.get();
            block[3] = temp.getMeta();
		} else if (blc instanceof BlockVine) {
			if (block[3] == 0 && !isSolidBlock(world.getBlock(block[0], block[1] + 1, block[2])))
				block[3] = 1;
			if (block[3] != 0) {
				int dir = VINES_META_TO_DIR[block[3]];
				while (true) {
					if (isSolidBlock(world.getBlock(block[0] + DIR_TO_I[dir], block[1], block[2] + DIR_TO_K[dir])))
						break;
					dir = (dir + 1) % 4;
					if (dir == VINES_META_TO_DIR[block[3]]) { // we've looped through everything
						if (isSolidBlock(world.getBlock(block[0], block[1] + 1, block[2]))) {
							dir = -1;
							break;
						}
						return; // did not find a surface we can attach to
					}
				}
				block[4] = dir == -1 ? 0 : VINES_DIR_TO_META[dir];
			}
		}
		// It seems Minecraft orients torches automatically, so I shouldn't have to do anything...
		// else if(block[3]==TORCH_ID || block[3]==REDSTONE_TORCH_ON_ID ||
		// block[3]==REDSTONE_TORCH_OFF_ID){
		// block[4]=1;
		// }
		if (blc == Blocks.air && block[3]>=PAINTING_BLOCK_OFFSET)//Remember:Paintings are not blocks
			setPainting(block, block[3]-PAINTING_BLOCK_OFFSET);
		else if (blc == Blocks.torch) {
			if (Blocks.torch.canPlaceBlockAt(world, block[0], block[1], block[2]))
				world.setBlock(block[0], block[1], block[2], blc, block[3], 3);// force lighting update
		} else if (blc == Blocks.glowstone)
			world.setBlock(block[0], block[1], block[2], blc, block[3], 3);// force lighting update
		else if(blc!=null) {
            if((randLightingHash[(block[0] & 0x7) | (block[1] & 0x38) | (block[2] & 0x1c0)]))
                world.setBlock(block[0], block[1], block[2], blc, block[3], 3);
            else
                setBlockAndMetaNoLighting(world, block[0], block[1], block[2], blc, block[3]);
        }
	}

    protected BlockAndMeta getDelayedStair(Block blc, int...block){
        // if stairs are running into ground. replace them with a solid block
        int dirX = block[0] - DIR_TO_I[STAIRS_META_TO_DIR[block[3] % 4]];
        int dirZ = block[2] - DIR_TO_K[STAIRS_META_TO_DIR[block[3] % 4]];
        if(world.getHeightValue(dirX, dirZ)>block[1]) {
            Block adjId = world.getBlock(dirX, block[1], dirZ);
            Block aboveID = world.getBlock(block[0], block[1] + 1, block[2]);
            if (BlockProperties.get(aboveID).isGround && BlockProperties.get(adjId).isGround) {
                return new BlockAndMeta(blc, block[3]).stairToSolid();
            } else if (!BlockProperties.get(adjId).isWallable || !BlockProperties.get(aboveID).isWallable) {
                return new BlockAndMeta(null, 0); // solid or liquid non-wall block. In this case, just don't build the stair (aka preserve block).
            }
        }
        return new BlockAndMeta(blc, block[3]);
    }

	// The origin of this building was placed to match a centerline.
	// The building previously had bWidth=oldWidth, now it has the current
	// value of bWidth and needs to have origin updated.
	protected final void recenterFromOldWidth(int oldWidth) {
		i0 += xI * (oldWidth - bWidth) / 2;
		k0 += xK * (oldWidth - bWidth) / 2;
	}

	// ******************** LOCAL COORDINATE FUNCTIONS - SET BLOCK FUNCTIONS
	// *************************************************************************************************************//
	protected final void setBlockLocal(int x, int z, int y, Block blockID) {
		setBlockLocal(x, z, y, blockID, 0);
	}

	protected final void setBlockLocal(int x, int z, int y, Block blockID, int metadata) {
        int[] pt = getIJKPt(x, z, y);
        if (blockID == Blocks.air && world.isAirBlock(pt[0], pt[1], pt[2]))
            return;
        if (!(blockID instanceof BlockChest))
            emptyIfChest(pt);
        if (BlockProperties.get(blockID).isDelayed){
            delayedBuildQueue.offer(new PlacedBlock(blockID, new int[]{pt[0], pt[1], pt[2], rotateMetadata(blockID, metadata)}));
        }else if (randLightingHash[(x & 0x7) | (y & 0x38) | (z & 0x1c0)]) {
            world.setBlock(pt[0], pt[1], pt[2], blockID, rotateMetadata(blockID, metadata), 2);
        } else {
            setBlockAndMetaNoLighting(world, pt[0], pt[1], pt[2], blockID, rotateMetadata(blockID, metadata));
        }
        if (BlockProperties.get(blockID).isDoor) {
            addDoorToNewListIfAppropriate(pt[0], pt[1], pt[2]);
        }
	}

	protected final void setBlockLocal(int x, int z, int y, BlockAndMeta block) {
        if(block instanceof BlockExtended){
            setSpecialBlockLocal(x, z, y, block.get(), block.getMeta(), ((BlockExtended) block).info);
        }else{
            setBlockLocal(x, z, y, block.get(), block.getMeta());
        }
	}

	protected final void setBlockLocal(int x, int z, int y, TemplateRule rule) {
		setBlockLocal(x, z, y, rule.getBlockOrHole(random));
	}

    protected final void setBlockWithLightingLocal(int x, int z, int y, TemplateRule rule, boolean lighting) {
        setBlockWithLightingLocal(x, z, y, rule.getBlockOrHole(random), lighting);
    }

    protected final void removeBlockWithLighting(int x, int z, int y){
        setBlockWithLightingLocal(x, z, y, TemplateRule.AIR_RULE, true);
    }

    protected final void setBlockWithLightingLocal(int x, int z, int y, BlockAndMeta block, boolean lighting) {
        if(block instanceof BlockExtended){
            setSpecialBlockLocal(x, z, y, block.get(), block.getMeta(), ((BlockExtended) block).info);
        }else{
            setBlockWithLightingLocal(x, z, y, block.get(), block.getMeta(), lighting);
        }
    }

	// allows control of lighting. Also will build even if replacing air with air.
	protected final void setBlockWithLightingLocal(int x, int z, int y, Block blockID, int metadata, boolean lighting) {
		int[] pt = getIJKPt(x, z, y);
        if (blockID == Blocks.air && world.isAirBlock(pt[0], pt[1], pt[2]))
            return;
		if (!(blockID instanceof BlockChest))
			emptyIfChest(pt);
		if (BlockProperties.get(blockID).isDelayed)
            delayedBuildQueue.offer(new PlacedBlock(blockID, new int[]{pt[0], pt[1], pt[2], rotateMetadata(blockID, metadata)}));
		else if (lighting)
			world.setBlock(pt[0], pt[1], pt[2], blockID, rotateMetadata(blockID, metadata), 3);
		else
			setBlockAndMetaNoLighting(world, pt[0], pt[1], pt[2], blockID, rotateMetadata(blockID, metadata));
		if (BlockProperties.get(blockID).isDoor) {
			addDoorToNewListIfAppropriate(pt[0], pt[1], pt[2]);
		}
	}

	protected final void setOrigin(int i0_, int j0_, int k0_) {
		i0 = i0_;
		j0 = j0_;
		k0 = k0_;
	}

	protected final void setOriginLocal(int i1, int j1, int k1, int x, int z, int y) {
		i0 = i1 + yI * y + xI * x;
		j0 = j1 + z;
		k0 = k1 + yK * y + xK * x;
	}

	// ******************** LOCAL COORDINATE FUNCTIONS - SPECIAL BLOCK FUNCTIONS
	// *************************************************************************************************************//
	// &&&&&&&&&&&&&&&&& SPECIAL BLOCK FUNCTION - setSpecialBlockLocal &&&&&&&&&&&&&&&&&&&&&&&&&&&&&//
	protected final void setSpecialBlockLocal(int x, int z, int y, Block blockID, int metadata, String extra) {
		if (extra.equals(TemplateRule.SPECIAL_AIR))
			return; // preserve existing world block
		int[] pt = getIJKPt(x, z, y);
		if (blockID instanceof BlockAir) {
            if(extra.equals(TemplateRule.SPECIAL_STAIR) && metadata<=0){
                world.setBlock(pt[0], pt[1], pt[2], Blocks.stone_slab, rotateMetadata(Blocks.stone_slab, -metadata), 2);
                return;
            }
            if (extra.equals(TemplateRule.SPECIAL_PAINT) && metadata>=PAINTING_BLOCK_OFFSET) {//Remember:Paintings are not blocks
                delayedBuildQueue.offer(new PlacedBlock(blockID, new int[]{pt[0], pt[1], pt[2], metadata}));
                return;
            }
			Block presentBlock = world.getBlock(pt[0], pt[1], pt[2]);
			if (!presentBlock.isAir(world, pt[0], pt[1], pt[2]) && !BlockProperties.get(presentBlock).isWater) {
				if (!(BlockProperties.get(world.getBlock(pt[0] - 1, pt[1], pt[2])).isWater || BlockProperties.get(world.getBlock(pt[0], pt[1], pt[2] - 1)).isWater
						|| BlockProperties.get(world.getBlock(pt[0] + 1, pt[1], pt[2])).isWater || BlockProperties.get(world.getBlock(pt[0], pt[1], pt[2] + 1)).isWater || BlockProperties.get(world.getBlock(pt[0], pt[1] + 1,
						pt[2])).isWater)) {// don't adjacent to a water block
					world.setBlockToAir(pt[0], pt[1], pt[2]);
				}
			}
		}else if(blockID instanceof BlockMobSpawner){
            setMobSpawner(pt, blockID, metadata, extra);
        }else if(blockID instanceof BlockChest){
			setLootChest(pt, blockID, metadata, extra);
        }else{
			world.setBlock(pt[0], pt[1], pt[2], blockID, metadata, 2);
		}
	}

	private void addDoorToNewListIfAppropriate(int par1, int par2, int par3) {
		if (!(this.wgt instanceof WorldGenWalledCity)) {
			return;
		}
		int id = getKnownBuilding();
		if (id == -1) {
			((PopulatorWalledCity) this.wgt.master).cityDoors.put(bID, new ArrayList<VillageDoorInfo>());
			id = bID;
		}
		int l = ((BlockDoor) Blocks.wooden_door).func_150013_e(this.world, par1, par2, par3);
		int i1;
		if (l != 0 && l != 2) {
			i1 = 0;
			for (int j1 = -5; j1 < 0; ++j1) {
				if (this.world.canBlockSeeTheSky(par1, par2, par3 + j1)) {
					--i1;
				}
			}
			for (int j1 = 1; j1 <= 5; ++j1) {
				if (this.world.canBlockSeeTheSky(par1, par2, par3 + j1)) {
					++i1;
				}
			}
			if (i1 != 0) {
				((PopulatorWalledCity) this.wgt.master).cityDoors.get(id).add(new VillageDoorInfo(par1, par2, par3, 0, i1 > 0 ? -2 : 2, 0));
			}
		} else {
			i1 = 0;
			for (int j1 = -5; j1 < 0; ++j1) {
				if (this.world.canBlockSeeTheSky(par1 + j1, par2, par3)) {
					--i1;
				}
			}
			for (int j1 = 1; j1 <= 5; ++j1) {
				if (this.world.canBlockSeeTheSky(par1 + j1, par2, par3)) {
					++i1;
				}
			}
			if (i1 != 0) {
				((PopulatorWalledCity) this.wgt.master).cityDoors.get(id).add(new VillageDoorInfo(par1, par2, par3, i1 > 0 ? -2 : 2, 0, 0));
			}
		}
	}

	// ******************** LOCAL COORDINATE FUNCTIONS - HELPER FUNCTIONS
	// *************************************************************************************************************//
	private void emptyIfChest(int[] pt) {
		// if block is a chest empty it
		if (pt != null && world.getBlock(pt[0], pt[1], pt[2]) instanceof BlockChest) {
			TileEntityChest tileentitychest = (TileEntityChest) world.getTileEntity(pt[0], pt[1], pt[2]);
			for (int m = 0; m < tileentitychest.getSizeInventory(); m++)
				tileentitychest.setInventorySlotContents(m, null);
		}
	}

	private ItemStack getChestItemstack(String chestType) {
		if (chestType.equals(TOWER_CHEST) && random.nextInt(4) == 0) { // for tower chests, chance of returning the tower block
			return new ItemStack(bRule.primaryBlock.get(), random.nextInt(10), bRule.primaryBlock.getMeta());
		}
		Object[][] itempool = wgt.chestItems.get(chestType);
		int idx = pickWeightedOption(world.rand, Arrays.asList(itempool[3]), Arrays.asList(itempool[0]));
        Object obj = itempool[1][idx];
        if(obj == null){
            return null;
        }
        if(obj instanceof Block){
            if(obj == Blocks.air)
                return null;
            obj = Item.getItemFromBlock((Block)obj);
        }
		return new ItemStack((Item)obj, Integer.class.cast(itempool[4][idx]) + random.nextInt(Integer.class.cast(itempool[5][idx]) - Integer.class.cast(itempool[4][idx]) + 1), Integer.class.cast(itempool[2][idx]));
	}

	private int getKnownBuilding() {
		Set<?> keys = ((PopulatorWalledCity) this.wgt.master).cityDoors.keySet();
		for (int id = bID - 3; id < bID + 4; id++) {
			if (keys.contains(id))
				return id;
		}
		return -1;
	}

	private int rotateMetadata(Block blockID, int metadata) {
		int tempdata = 0;
		if (BlockProperties.get(blockID).isStair) {
			if (metadata >= 4) {
				tempdata += 4;
				metadata -= 4;
			}
			return STAIRS_DIR_TO_META[orientDirToBDir(STAIRS_META_TO_DIR[metadata])] + tempdata;
		}
		if (BlockProperties.get(blockID).isDoor) {
			// think of door metas applying to doors with hinges on the left
			// that open in (when seen facing in)
			// in this case, door metas match the dir in which the door opens
			// e.g. a door on the south face of a wall, opening in to the north
			// has a meta value of 0 (or 4 if the door is opened).
			if (metadata >= 8)// >=8:the top half of the door
				return metadata;
			if (metadata >= 4) {
				// >=4:the door is open
				tempdata += 4;
			}
			return DOOR_DIR_TO_META[orientDirToBDir(DOOR_META_TO_DIR[metadata % 4])] + tempdata;
		}
		if(blockID==Blocks.lever||blockID==Blocks.stone_button||blockID==Blocks.wooden_button){
			// check to see if this is flagged as thrown
			if (metadata - 8 > 0) {
				tempdata += 8;
				metadata -= 8;
			}
			if (metadata == 0 || (blockID==Blocks.lever && metadata >= 5))
				return metadata + tempdata;
			return BUTTON_DIR_TO_META[orientDirToBDir(STAIRS_META_TO_DIR[metadata - 1])] + tempdata;
        }else if(blockID==Blocks.torch||blockID==Blocks.redstone_torch||blockID==Blocks.unlit_redstone_torch){
			if (metadata == 0 || metadata >= 5) {
				return metadata;
			}
			return BUTTON_DIR_TO_META[orientDirToBDir(STAIRS_META_TO_DIR[metadata - 1])];
        }else if(blockID==Blocks.ladder||blockID==Blocks.dispenser||blockID==Blocks.furnace||blockID==Blocks.lit_furnace||blockID==Blocks.wall_sign||blockID==Blocks.piston||blockID==Blocks.piston_extension||blockID==Blocks.chest||blockID==Blocks.hopper||blockID==Blocks.dropper){
			if (blockID==Blocks.piston|| blockID==Blocks.piston_extension) {
				if (metadata - 8 >= 0) {
					// pushed or not, sticky or not
					tempdata += 8;
					metadata -= 8;
				}
			}
			if (metadata <= 1)
				return metadata + tempdata;
			return LADDER_DIR_TO_META[orientDirToBDir(LADDER_META_TO_DIR[metadata - 2])] + tempdata;
        }else if(blockID==Blocks.rail||blockID==Blocks.golden_rail||blockID==Blocks.detector_rail||blockID==Blocks.activator_rail){
			switch (bDir) {
			case DIR_NORTH:
				// flat tracks
				if (metadata == 0) {
					return 0;
				}
				if (metadata == 1) {
					return 1;
				}
				// ascending tracks
				if (metadata == 2) {
					return 2;
				}
				if (metadata == 3) {
					return 3;
				}
				if (metadata == 4) {
					return bHand == 1 ? 4 : 5;
				}
				if (metadata == 5) {
					return bHand == 1 ? 5 : 4;
				}
				// curves
				if (metadata == 6) {
					return bHand == 1 ? 6 : 9;
				}
				if (metadata == 7) {
					return bHand == 1 ? 7 : 8;
				}
				if (metadata == 8) {
					return bHand == 1 ? 8 : 7;
				}
				if (metadata == 9) {
					return bHand == 1 ? 9 : 6;
				}
			case DIR_EAST:
				// flat tracks
				if (metadata == 0) {
					return 1;
				}
				if (metadata == 1) {
					return 0;
				}
				// ascending tracks
				if (metadata == 2) {
					return 5;
				}
				if (metadata == 3) {
					return 4;
				}
				if (metadata == 4) {
					return bHand == 1 ? 2 : 3;
				}
				if (metadata == 5) {
					return bHand == 1 ? 3 : 2;
				}
				// curves
				if (metadata == 6) {
					return bHand == 1 ? 7 : 6;
				}
				if (metadata == 7) {
					return bHand == 1 ? 8 : 9;
				}
				if (metadata == 8) {
					return bHand == 1 ? 9 : 8;
				}
				if (metadata == 9) {
					return bHand == 1 ? 6 : 7;
				}
			case DIR_SOUTH:
				// flat tracks
				if (metadata == 0) {
					return 0;
				}
				if (metadata == 1) {
					return 1;
				}
				// ascending tracks
				if (metadata == 2) {
					return 3;
				}
				if (metadata == 3) {
					return 2;
				}
				if (metadata == 4) {
					return bHand == 1 ? 5 : 4;
				}
				if (metadata == 5) {
					return bHand == 1 ? 4 : 5;
				}
				// curves
				if (metadata == 6) {
					return bHand == 1 ? 8 : 7;
				}
				if (metadata == 7) {
					return bHand == 1 ? 9 : 6;
				}
				if (metadata == 8) {
					return bHand == 1 ? 6 : 9;
				}
				if (metadata == 9) {
					return bHand == 1 ? 7 : 8;
				}
			case DIR_WEST:
				// flat tracks
				if (metadata == 0) {
					return 1;
				}
				if (metadata == 1) {
					return 0;
				}
				// ascending tracks
				if (metadata == 2) {
					return 4;
				}
				if (metadata == 3) {
					return 5;
				}
				if (metadata == 4) {
					return bHand == 1 ? 3 : 2;
				}
				if (metadata == 5) {
					return bHand == 1 ? 2 : 3;
				}
				// curves
				if (metadata == 6) {
					return bHand == 1 ? 9 : 8;
				}
				if (metadata == 7) {
					return bHand == 1 ? 6 : 7;
				}
				if (metadata == 8) {
					return bHand == 1 ? 7 : 6;
				}
				if (metadata == 9) {
					return bHand == 1 ? 8 : 9;
				}
			}
        }else if(blockID==Blocks.bed||blockID==Blocks.fence_gate||blockID==Blocks.tripwire_hook||blockID==Blocks.pumpkin||blockID==Blocks.lit_pumpkin||blockID==Blocks.powered_repeater||blockID==Blocks.unpowered_repeater){
			while (metadata >= 4) {
				tempdata += 4;
                metadata -= 4;
			}
			if (blockID==Blocks.trapdoor)
				return TRAPDOOR_DIR_TO_META[orientDirToBDir(TRAPDOOR_META_TO_DIR[metadata])] + tempdata;
			else
				return BED_DIR_TO_META[orientDirToBDir(BED_META_TO_DIR[metadata])] + tempdata;
        }else if(blockID==Blocks.vine){
			if (metadata == 0)
				return 0;
			else if (metadata == 1 || metadata == 2 || metadata == 4 || metadata == 8)
				return VINES_DIR_TO_META[(bDir + VINES_META_TO_DIR[metadata]) % 4];
			else
				return 1; // default case since vine do not have to have correct
			// metadata
        }else if(blockID==Blocks.standing_sign){
			// sign posts
			switch (bDir) {
			case DIR_NORTH:
				if (metadata == 0) {
					return bHand == 1 ? 0 : 8;
				}
				if (metadata == 1) {
					return bHand == 1 ? 1 : 7;
				}
				if (metadata == 2) {
					return bHand == 1 ? 2 : 6;
				}
				if (metadata == 3) {
					return bHand == 1 ? 3 : 5;
				}
				if (metadata == 4) {
					return 4;
				}
				if (metadata == 5) {
					return bHand == 1 ? 5 : 3;
				}
				if (metadata == 6) {
					return bHand == 1 ? 6 : 2;
				}
				if (metadata == 7) {
					return bHand == 1 ? 7 : 1;
				}
				if (metadata == 8) {
					return bHand == 1 ? 8 : 0;
				}
				if (metadata == 9) {
					return bHand == 1 ? 9 : 15;
				}
				if (metadata == 10) {
					return bHand == 1 ? 10 : 14;
				}
				if (metadata == 11) {
					return bHand == 1 ? 11 : 13;
				}
				if (metadata == 12) {
					return 12;
				}
				if (metadata == 13) {
					return bHand == 1 ? 13 : 11;
				}
				if (metadata == 14) {
					return bHand == 1 ? 14 : 10;
				}
				if (metadata == 15) {
					return bHand == 1 ? 15 : 9;
				}
			case DIR_EAST:
				if (metadata == 0) {
					return bHand == 1 ? 4 : 12;
				}
				if (metadata == 1) {
					return bHand == 1 ? 5 : 11;
				}
				if (metadata == 2) {
					return bHand == 1 ? 6 : 10;
				}
				if (metadata == 3) {
					return bHand == 1 ? 7 : 9;
				}
				if (metadata == 4) {
					return 8;
				}
				if (metadata == 5) {
					return bHand == 1 ? 9 : 7;
				}
				if (metadata == 6) {
					return bHand == 1 ? 10 : 6;
				}
				if (metadata == 7) {
					return bHand == 1 ? 11 : 5;
				}
				if (metadata == 8) {
					return bHand == 1 ? 12 : 4;
				}
				if (metadata == 9) {
					return bHand == 1 ? 13 : 3;
				}
				if (metadata == 10) {
					return bHand == 1 ? 14 : 2;
				}
				if (metadata == 11) {
					return bHand == 1 ? 15 : 1;
				}
				if (metadata == 12) {
					return 0;
				}
				if (metadata == 13) {
					return bHand == 1 ? 1 : 15;
				}
				if (metadata == 14) {
					return bHand == 1 ? 2 : 14;
				}
				if (metadata == 15) {
					return bHand == 1 ? 3 : 13;
				}
			case DIR_SOUTH:
				if (metadata == 0) {
					return bHand == 1 ? 8 : 0;
				}
				if (metadata == 1) {
					return bHand == 1 ? 9 : 15;
				}
				if (metadata == 2) {
					return bHand == 1 ? 10 : 14;
				}
				if (metadata == 3) {
					return bHand == 1 ? 11 : 13;
				}
				if (metadata == 4) {
					return 12;
				}
				if (metadata == 5) {
					return bHand == 1 ? 13 : 11;
				}
				if (metadata == 6) {
					return bHand == 1 ? 14 : 10;
				}
				if (metadata == 7) {
					return bHand == 1 ? 15 : 9;
				}
				if (metadata == 8) {
					return bHand == 1 ? 0 : 8;
				}
				if (metadata == 9) {
					return bHand == 1 ? 1 : 7;
				}
				if (metadata == 10) {
					return bHand == 1 ? 2 : 6;
				}
				if (metadata == 11) {
					return bHand == 1 ? 3 : 5;
				}
				if (metadata == 12) {
					return 4;
				}
				if (metadata == 13) {
					return bHand == 1 ? 5 : 3;
				}
				if (metadata == 14) {
					return bHand == 1 ? 6 : 2;
				}
				if (metadata == 15) {
					return bHand == 1 ? 7 : 1;
				}
			case DIR_WEST:
				if (metadata == 0) {
					return bHand == 1 ? 12 : 4;
				}
				if (metadata == 1) {
					return bHand == 1 ? 13 : 3;
				}
				if (metadata == 2) {
					return bHand == 1 ? 14 : 2;
				}
				if (metadata == 3) {
					return bHand == 1 ? 15 : 1;
				}
				if (metadata == 4) {
					return 0;
				}
				if (metadata == 5) {
					return bHand == 1 ? 1 : 15;
				}
				if (metadata == 6) {
					return bHand == 1 ? 2 : 14;
				}
				if (metadata == 7) {
					return bHand == 1 ? 3 : 13;
				}
				if (metadata == 8) {
					return bHand == 1 ? 4 : 12;
				}
				if (metadata == 9) {
					return bHand == 1 ? 5 : 11;
				}
				if (metadata == 10) {
					return bHand == 1 ? 6 : 10;
				}
				if (metadata == 11) {
					return bHand == 1 ? 7 : 9;
				}
				if (metadata == 12) {
					return 8;
				}
				if (metadata == 13) {
					return bHand == 1 ? 9 : 7;
				}
				if (metadata == 14) {
					return bHand == 1 ? 10 : 6;
				}
				if (metadata == 15) {
					return bHand == 1 ? 11 : 5;
				}
			}
		}
		return metadata + tempdata;
	}

	// &&&&&&&&&&&&&&&&& SPECIAL BLOCK FUNCTION - setLootChest
	// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&//
	private void setLootChest(int[] pt, Block chestBlock, int meta, String chestType) {
		if (world.setBlock(pt[0], pt[1], pt[2], chestBlock, meta, 2)) {
			TileEntityChest chest = (TileEntityChest) world.getTileEntity(pt[0], pt[1], pt[2]);
			if (wgt.chestTries != null && wgt.chestTries.containsKey(chestType)) {
				for (int m = 0; m < wgt.chestTries.get(chestType); m++) {
					if (random.nextBoolean()) {
						ItemStack itemstack = getChestItemstack(chestType);
						if (itemstack != null && chest != null)
							chest.setInventorySlotContents(random.nextInt(chest.getSizeInventory()), itemstack);
					}
				}
			}
		}
	}

	// &&&&&&&&&&&&&&&&& SPECIAL BLOCK FUNCTION - setMobSpawner
	// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&//
	private void setMobSpawner(int[] pt, Block spawner, int metadata, String info) {
		if(world.setBlock(pt[0], pt[1], pt[2], spawner, metadata, 2)){
            TileEntityMobSpawner tileentitymobspawner = (TileEntityMobSpawner) world.getTileEntity(pt[0], pt[1], pt[2]);
            if (tileentitymobspawner != null){
                if(info.equals("UPRIGHT")) {
                    if (random.nextInt(3) == 0)
                        setMobSpawner(tileentitymobspawner, 1, 3);
                    else
                        setMobSpawner(tileentitymobspawner, 2, 0);
                }else if(info.equals("EASY")){
                    setMobSpawner(tileentitymobspawner, 2, 0);
                }else if(info.equals("MEDIUM")){
                    setMobSpawner(tileentitymobspawner, 3, 0);
                }else if(info.equals("HARD")){
                    setMobSpawner(tileentitymobspawner, 4, 0);
                }else
                    tileentitymobspawner.func_145881_a().setEntityName(info);
            }
        }
	}

    private void setMobSpawner(TileEntityMobSpawner spawner, int nTypes, int offset) {
        String mob = "Pig";
        int n = random.nextInt(nTypes) + offset;
        if(n<SPAWNERS.length){
            mob = SPAWNERS[n];
        }
        spawner.func_145881_a().setEntityName(mob);
    }

	public static int distance(int[] pt1, int[] pt2) {
		return (int) Math.sqrt((pt1[0] - pt2[0]) * (pt1[0] - pt2[0]) + (pt1[1] - pt2[1]) * (pt1[1] - pt2[1]) + (pt1[2] - pt2[2]) * (pt1[2] - pt2[2]));
	}

	public static int findSurfaceJ(World world, int i, int k, int jinit, boolean wallIsSurface, int waterSurfaceBuffer) {
		Block blockId;
		//if(world.getChunkProvider().chunkExists(i>>4, k>>4))
		{
			if (world.provider.isHellWorld) {// the Nether
				if ((i % 2 == 1) ^ (k % 2 == 1)) {
					for (int j = (int) (WORLD_MAX_Y * 0.5); j > -1; j--) {
						if (world.isAirBlock(i, j, k))
							for (; j > -1; j--)
								if (!BlockProperties.get(world.getBlock(i, j, k)).isWallable)
									return j;
					}
				} else {
					for (int j = 0; j <= (int) (WORLD_MAX_Y * 0.5); j++)
						if (world.isAirBlock(i, j, k))
							return j;
				}
				return -1;
			} else { // other dimensions
				int minecraftHeight = world.getChunkFromBlockCoords(i, k).getHeightValue(i & 0xf, k & 0xf);
				if (minecraftHeight < jinit)
					jinit = minecraftHeight;
				for (int j = jinit; j >= 0; j--) {
					blockId = world.getBlock(i, j, k);
					if (!BlockProperties.get(blockId).isWallable && (wallIsSurface || !BlockProperties.get(blockId).isArtificial))
						return j;
					if (waterSurfaceBuffer != IGNORE_WATER && BlockProperties.get(blockId).isWater)
						return BlockProperties.get(world.getBlock(i, j - waterSurfaceBuffer, k)).isWater ? HIT_WATER : j;
					// so we can still build in swamps...
				}
			}
		}
		return -1;
	}

	public static int flipDir(int dir) {
		return (dir + 2) % 4;
	}

	public static String metaValueCheck(Block blockID, int metadata) {
		if (metadata < 0 || metadata >= 16)
			return "All Minecraft meta values should be between 0 and 15";
		String fail = blockID.getUnlocalizedName() + " meta value should be between";
		if (BlockProperties.get(blockID).isStair)
			return metadata < 8 ? null : fail + " 0 and 7";
		// orientation metas
		if(blockID==Blocks.rail){
			return metadata < 10 ? null : fail + " 0 and 9";
        }else if(blockID==Blocks.stone_button || blockID== Blocks.wooden_button){
			return metadata % 8 > 0 && metadata % 8 < 5 ? null : fail + " 1 and 4 or 9 and 12";
        }else if(blockID==Blocks.ladder||blockID==Blocks.dispenser||blockID==Blocks.furnace||blockID==Blocks.lit_furnace||blockID==Blocks.wall_sign
		||blockID==Blocks.piston||blockID==Blocks.piston_extension||blockID==Blocks.chest||blockID==Blocks.hopper||blockID==Blocks.dropper||blockID==Blocks.golden_rail||blockID==Blocks.detector_rail||blockID==Blocks.activator_rail){
			return metadata % 8 < 6 ? null : fail + " 0 and 5 or 8 and 13";
        }else if(blockID==Blocks.pumpkin||blockID==Blocks.lit_pumpkin){
			return metadata < 5 ? null : fail + " 0 and 4";
        }else if(blockID==Blocks.fence_gate){
			return metadata < 8 ? null : fail + " 0 and 7";
        }else if(blockID==Blocks.wooden_slab ||blockID==Blocks.bed){
			return metadata % 8 < 4 ? null : fail + " 0 and 3 or 8 and 11";
        }else if(blockID==Blocks.torch||blockID==Blocks.redstone_torch||blockID==Blocks.unlit_redstone_torch){
			return metadata > 0 && metadata < 7 ? null : fail + " 1 and 6";
		}
		return null;
	}

    public static int pickWeightedOption(Random random, List<Object> weights, List<Object> options){
        int[] w = new int[weights.size()];
        int[] o = new int[options.size()];
        for (int i= 0; i < w.length; i++)
            w[i]= ((Integer)weights.get(i));
        for (int i= 0; i < o.length; i++)
            o[i]= ((Integer)options.get(i));
        return pickWeightedOption(random, w, o);
    }

	public static int pickWeightedOption(Random random, int[] weights, int[] options) {
		int sum = 0, n;
		for (n = 0; n < weights.length; n++)
			sum += weights[n];
		if (sum <= 0) {
			System.err.println("Error selecting options, weightsum not positive!");
			return options[0]; // default to returning first option
		}
		int s = random.nextInt(sum);
		sum = 0;
		n = 0;
		while (n < weights.length) {
			sum += weights[n];
			if (sum > s)
				return options[n];
			n++;
		}
		return options[options.length - 1];
	}

	public static int rotDir(int dir, int rotation) {
		return (dir + rotation + 4) % 4;
	}

	public static void setBlockAndMetaNoLighting(World world, int i, int j, int k, Block blockId, int meta) {
		if (i < 0xfe363c80 || k < 0xfe363c80 || i >= 0x1c9c380 || k >= 0x1c9c380 || j < 0 || j > Building.WORLD_MAX_Y)
			return;
		world.getChunkFromChunkCoords(i >> 4, k >> 4).func_150807_a(i & 0xf, j, k & 0xf, blockId, meta);
	}

	// ******************** STATIC FUNCTIONS
	// ******************************************************************************************************************************************//
	protected static void fillDown(int[] lowPt, int jtop, World world) {
		while (BlockProperties.get(world.getBlock(lowPt[0], lowPt[1], lowPt[2])).isArtificial)
			lowPt[1]--;
		Block oldSurfaceBlockId = world.getBlock(lowPt[0], lowPt[1], lowPt[2]);
		if (BlockProperties.get(oldSurfaceBlockId).isOre)
			oldSurfaceBlockId = Blocks.stone;
		if (oldSurfaceBlockId == Blocks.dirt || (lowPt[1] <= SEA_LEVEL && oldSurfaceBlockId == Blocks.sand))
			oldSurfaceBlockId = Blocks.grass;
		if (oldSurfaceBlockId == Blocks.air)
			oldSurfaceBlockId = world.provider.isHellWorld ? Blocks.netherrack : Blocks.grass;
		Block fillBlockId = oldSurfaceBlockId == Blocks.grass ? Blocks.dirt : oldSurfaceBlockId;
		for (; lowPt[1] <= jtop; lowPt[1]++)
			setBlockAndMetaNoLighting(world, lowPt[0], lowPt[1], lowPt[2], lowPt[1] == jtop ? oldSurfaceBlockId : fillBlockId, 0);
	}

	protected static int minOrMax(int[] a, boolean isMin) {
		if (isMin) {
			int min = Integer.MAX_VALUE;
			for (int i : a)
				min = Math.min(min, i);
			return min;
		} else {
			int max = Integer.MIN_VALUE;
			for (int i : a)
				max = Math.max(max, i);
			return max;
		}
	}

	// wiggle allows for some leeway before nonzero is detected
	protected static int signum(int n, int wiggle) {
		if (n <= wiggle && -n <= wiggle)
			return 0;
		return n < 0 ? -1 : 1;
	}

	private static void circleShape(int diam) {
		float rad = diam / 2.0F;
		float[][] shape_density = new float[diam][diam];
		for (int x = 0; x < diam; x++)
			for (int y = 0; y < diam; y++)
				shape_density[y][x] = ((x + 0.5F - rad) * (x + 0.5F - rad) + (y + 0.5F - rad) * (y + 0.5F - rad)) / (rad * rad);
		int[] xheight = new int[diam];
		for (int y = 0; y < diam; y++) {
			int x = 0;
			while (shape_density[y][x] > 1.0F) {
                x++;
			}
			xheight[y] = x;
		}
		CIRCLE_SHAPE[diam] = new int[diam][diam];
		CIRCLE_CRENEL[diam] = new int[diam][diam];
		SPHERE_SHAPE[diam] = new int[(diam + 1) / 2];
		int nextHeight, crenel_adj = 0;
		for (int x = 0; x < diam; x++)
			for (int y = 0; y < diam; y++) {
				CIRCLE_SHAPE[diam][y][x] = 0;
				CIRCLE_CRENEL[diam][y][x] = 0;
			}
		for (int y = 0; y < diam; y++) {
			if (y == 0 || y == diam - 1)
				nextHeight = diam / 2 + 1;
			else
				nextHeight = xheight[y < diam / 2 ? y - 1 : y + 1] + (xheight[y] == xheight[y < diam / 2 ? y - 1 : y + 1] ? 1 : 0);
			if (y > 0 && xheight[y] == xheight[y - 1])
				crenel_adj++;
			int x = 0;
			for (; x < xheight[y]; x++) {
				CIRCLE_SHAPE[diam][y][x] = -1;
				CIRCLE_SHAPE[diam][y][diam - x - 1] = -1;
				CIRCLE_CRENEL[diam][y][x] = -1;
				CIRCLE_CRENEL[diam][y][diam - x - 1] = -1;
			}
			for (; x < nextHeight; x++) {
				CIRCLE_SHAPE[diam][y][x] = 1;
				CIRCLE_SHAPE[diam][y][diam - x - 1] = 1;
				CIRCLE_CRENEL[diam][y][x] = (x + crenel_adj) % 2;
				CIRCLE_CRENEL[diam][y][diam - x - 1] = (x + crenel_adj + diam + 1) % 2;
			}
		}
		for (int y = diam / 2; y < diam; y++)
			SPHERE_SHAPE[diam][y - diam / 2] = (2 * (diam / 2 - xheight[y]) + (diam % 2 == 0 ? 0 : 1));
	}

	private static boolean isSolidBlock(Block blockID) {
		return blockID.getMaterial().isSolid();
	}
}