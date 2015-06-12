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

import net.minecraft.block.*;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.village.VillageDoorInfo;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.BlockSnapshot;

import java.util.*;

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
public class Building {
	public enum Direction{
		NORTH,EAST,SOUTH,WEST;

		public Direction flip(){
			return from(this.ordinal() + 2);
		}

		public Direction next(){
			return from(this.ordinal() + 1);
		}

		public Direction rotate(int rotation){
			return from(this.ordinal() + rotation + 4);
		}

		public int toX(){
			if(this==EAST){
				return 1;
			}else if(this==WEST){
				return -1;
			}
			return 0;
		}

		public int toY(){
			if(this==NORTH){
				return 1;
			}else if(this==SOUTH){
				return -1;
			}
			return 0;
		}

		private static Direction from(int i){
			return Direction.values()[i%4];
		}

		public static Direction from(Random random){
			return from(random.nextInt(4));
		}
	}
	public final static int HIT_WATER = -666; // , HIT_SWAMP=-667;
	public final static String HARD_CHEST = "HARD", TOWER_CHEST = "TOWER";
	public final static int DIR_NORTH = Direction.NORTH.ordinal(), DIR_EAST = Direction.EAST.ordinal(), DIR_SOUTH = Direction.SOUTH.ordinal(), DIR_WEST = Direction.WEST.ordinal();
	public final static int R_HAND = 1, L_HAND = -1;
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
	protected Direction bDir; // Direction code of primary axis. Takes values of DIR_NORTH=0,DIR_EAST=1,DIR_SOUTH=2,DIR_WEST=3.

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
	// some prebuilt directional blocks
	public final static BlockAndMeta WEST_FACE_TORCH_BLOCK = new BlockAndMeta(Blocks.torch, BUTTON_DIR_TO_META[DIR_WEST]), EAST_FACE_TORCH_BLOCK = new BlockAndMeta(Blocks.torch, BUTTON_DIR_TO_META[DIR_EAST]),
			HOLE_BLOCK_LIGHTING = new BlockAndMeta(Blocks.air, 0), HOLE_BLOCK_NO_LIGHTING = new BlockAndMeta(Blocks.air, 1),
			PRESERVE_BLOCK = new BlockExtended(Blocks.air, 0, "PRESERVE");
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

    // **************************** CONSTRUCTORS - Building
    // *************************************************************************************//
    public Building(int ID_, WorldGeneratorThread wgt_, TemplateRule buildingRule_, Direction dir_, int axXHand_, boolean centerAligned_, int[] dim, int[] alignPt) {
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

	public final Direction getDir(){
		return Direction.from(random);
	}

	// outputs dir rotated to this Building's orientation and handedness
	// dir input should be the direction desired if bDir==DIR_NORTH and
	// bHand=R_HAND
	public int orientDirToBDir(int dir) {
		return bHand < 0 && dir % 2 == 1 ? (bDir.ordinal() + dir + 2) & 0x3 : (bDir.ordinal() + dir) & 0x3;
	}

	// &&&&&&&&&&&&&&&&& SPECIAL BLOCK FUNCTION - setPainting
	// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&//
	public void setPainting(int[] pt, int metadata) {
		// painting uses same orientation meta as ladders.
		// Have to adjust ijk since unlike ladders the entity exists at the
		// block it is hung on.
		int dir = orientDirToBDir(LADDER_META_TO_DIR[metadata % LADDER_META_TO_DIR.length]);
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
		if (entitypainting.onValidSurface())
			world.spawnEntityInWorld(entitypainting);
	}

	// ******************** ORIENTATION FUNCTIONS
	// *************************************************************************************************************//
	public void setPrimaryAx(Direction dir_) {
		bDir = dir_;
		// changes of basis
		switch (bDir) {
		case NORTH:
			xI = bHand;
			yI = 0;
			xK = 0;
			yK = -1;
			break;
		case EAST:
			xI = 0;
			yI = 1;
			xK = bHand;
			yK = 0;
			break;
		case SOUTH:
			xI = -bHand;
			yI = 0;
			xK = 0;
			yK = 1;
			break;
		case WEST:
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
		case SOUTH:
			return str + "(S," + (bHand > 0 ? "W)" : "E)");
		case NORTH:
			return str + "(N," + (bHand > 0 ? "E)" : "W)");
		case WEST:
			return str + "(W," + (bHand > 0 ? "N)" : "S)");
		case EAST:
			return str + "(E," + (bHand > 0 ? "S)" : "N)");
		}
		return "Error - bad dir value for ID=" + bID;
	}

	protected final boolean isArtificialWallBlock(int x, int z, int y) {
		Block blockId = getBlockIdLocal(x, z, y);
		return BlockProperties.get(blockId).isArtificial() && !(blockId == Blocks.sandstone && (getBlockIdLocal(x, z + 1, y) instanceof BlockSand || getBlockIdLocal(x, z + 2, y) instanceof BlockSand));
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
        return blkId1 == Blocks.air && BlockProperties.get(blkId2).isArtificial() && blkId2 != Blocks.ladder;
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
		return BlockProperties.get(world.getBlock(i0 + yI * y + xI * x, j0 + z, k0 + yK * y + xK * x)).isArtificial();
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
				int dir = VINES_META_TO_DIR[block[3] % VINES_META_TO_DIR.length];
				while (true) {
					if (isSolidBlock(world.getBlock(block[0] + DIR_TO_I[dir], block[1], block[2] + DIR_TO_K[dir])))
						break;
					dir = (dir + 1) % 4;
					if (dir == VINES_META_TO_DIR[block[3] % VINES_META_TO_DIR.length]) { // we've looped through everything
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
		else if (blc instanceof BlockTorch) {
			if (blc.canPlaceBlockAt(world, block[0], block[1], block[2]))
				world.setBlock(block[0], block[1], block[2], blc, block[3], 3);// force lighting update
		} else if (blc instanceof BlockGlowstone)
			world.setBlock(block[0], block[1], block[2], blc, block[3], 3);// force lighting update
		else if(blc!=null) {
            if((randLightingHash[(block[0] & 0x7) | (block[1] & 0x38) | (block[2] & 0x1c0)]))
                world.setBlock(block[0], block[1], block[2], blc, block[3], 3);
            else
                setBlockAndMetaNoLighting(world, block[0], block[1], block[2], blc, block[3], 3);
        }
		if (blc instanceof BlockDoor) {
			addDoorToNewListIfAppropriate(block[0], block[1], block[2]);
		}
	}

    protected BlockAndMeta getDelayedStair(Block blc, int...block){
        // if stairs are running into ground. replace them with a solid block
        int dirX = block[0] - DIR_TO_I[STAIRS_META_TO_DIR[block[3] % STAIRS_META_TO_DIR.length]];
        int dirZ = block[2] - DIR_TO_K[STAIRS_META_TO_DIR[block[3] % STAIRS_META_TO_DIR.length]];
        if(world.getHeightValue(dirX, dirZ)>block[1]) {
            Block adjId = world.getBlock(dirX, block[1], dirZ);
            Block aboveID = world.getBlock(block[0], block[1] + 1, block[2]);
            if (BlockProperties.get(aboveID).isGround() && BlockProperties.get(adjId).isGround()) {
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
        if(emptyIfChest(blockID, pt))
			return;
        if (BlockProperties.get(blockID).isDelayed){
            delayedBuildQueue.offer(new PlacedBlock(blockID, new int[]{pt[0], pt[1], pt[2], rotateMetadata(blockID, metadata)}));
        }else if (randLightingHash[(x & 0x7) | (y & 0x38) | (z & 0x1c0)]) {
            world.setBlock(pt[0], pt[1], pt[2], blockID, rotateMetadata(blockID, metadata), 2);
        } else {
            setBlockAndMetaNoLighting(world, pt[0], pt[1], pt[2], blockID, rotateMetadata(blockID, metadata), 2);
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
		if(emptyIfChest(blockID, pt))
			return;
		if (BlockProperties.get(blockID).isDelayed)
            delayedBuildQueue.offer(new PlacedBlock(blockID, new int[]{pt[0], pt[1], pt[2], rotateMetadata(blockID, metadata)}));
		else if (lighting)
			world.setBlock(pt[0], pt[1], pt[2], blockID, rotateMetadata(blockID, metadata), 3);
		else
			setBlockAndMetaNoLighting(world, pt[0], pt[1], pt[2], blockID, rotateMetadata(blockID, metadata), 3);
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
			setBlockLocal(x, z, y, blockID, metadata);
		}
	}

	private void addDoorToNewListIfAppropriate(int par1, int par2, int par3) {
		if (!(this.wgt.master instanceof PopulatorWalledCity)) {
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
	private boolean emptyIfChest(Block newID, int[] pt) {
		Block old = world.getBlock(pt[0], pt[1], pt[2]);
		if (newID == Blocks.air && old.isAir(world, pt[0], pt[1], pt[2]))
			return true;
		// if block is a chest empty it
		if (old instanceof ITileEntityProvider && !(newID instanceof ITileEntityProvider)) {
			TileEntity tileentity = world.getTileEntity(pt[0], pt[1], pt[2]);
			if(tileentity instanceof IInventory) {
				for (int m = 0; m < ((IInventory)tileentity).getSizeInventory(); m++)
					((IInventory)tileentity).setInventorySlotContents(m, null);
			}
		}
		return false;
	}

	private ItemStack getChestItemstack(String chestType) {
		if (chestType.equals(TOWER_CHEST) && random.nextInt(4) == 0) { // for tower chests, chance of returning the tower block
			return new ItemStack(bRule.primaryBlock.get(), random.nextInt(10), bRule.primaryBlock.getMeta());
		}
		RandomLoot[] itempool = wgt.chestItems.get(chestType);
		int idx = RandomPicker.pickWeightedOption(random, itempool);
        return itempool[idx].getLoot(random);
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
		else if (blockID instanceof BlockDoor) {
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
			return DOOR_DIR_TO_META[orientDirToBDir(DOOR_META_TO_DIR[metadata % DOOR_META_TO_DIR.length])] + tempdata;
		}
		else if(blockID instanceof BlockLever||blockID instanceof BlockButton){
			// check to see if this is flagged as thrown
			if (metadata - 8 > 0) {
				tempdata += 8;
				metadata -= 8;
			}
			if (metadata == 0 || (blockID instanceof BlockLever && metadata >= 5))
				return metadata + tempdata;
			return BUTTON_DIR_TO_META[orientDirToBDir(STAIRS_META_TO_DIR[metadata - 1])] + tempdata;
        }else if(blockID instanceof BlockTorch){
			if (metadata == 0 || metadata >= 5) {
				return metadata;
			}
			return BUTTON_DIR_TO_META[orientDirToBDir(STAIRS_META_TO_DIR[metadata - 1])];
        }else if(blockID instanceof BlockLadder||blockID instanceof BlockDispenser||blockID instanceof BlockFurnace||blockID==Blocks.wall_sign||blockID==Blocks.piston||blockID==Blocks.piston_extension||blockID instanceof BlockChest||blockID==Blocks.hopper||blockID==Blocks.dropper){
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
        }else if(blockID instanceof BlockRailBase){
			switch (bDir) {
			case NORTH:
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
			case EAST:
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
			case SOUTH:
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
			case WEST:
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
        }else if(blockID==Blocks.bed||blockID==Blocks.fence_gate||blockID==Blocks.tripwire_hook||blockID instanceof BlockPumpkin||blockID instanceof BlockRedstoneRepeater){
			while (metadata >= 4) {
				tempdata += 4;
                metadata -= 4;
			}
			if (blockID instanceof BlockTrapDoor)
				return TRAPDOOR_DIR_TO_META[orientDirToBDir(TRAPDOOR_META_TO_DIR[metadata])] + tempdata;
			else
				return BED_DIR_TO_META[orientDirToBDir(BED_META_TO_DIR[metadata])] + tempdata;
        }else if(blockID instanceof BlockVine){
			if (metadata == 0)
				return 0;
			else if (metadata == 1 || metadata == 2 || metadata == 4 || metadata == 8)
				return VINES_DIR_TO_META[(bDir.ordinal() + VINES_META_TO_DIR[metadata]) % VINES_DIR_TO_META.length];
			else
				return 1; // default case since vine do not have to have correct metadata
        }else if(blockID==Blocks.standing_sign){
			// sign posts
			switch (bDir) {
			case NORTH:
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
			case EAST:
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
			case SOUTH:
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
			case WEST:
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
	protected void setLootChest(int[] pt, Block chestBlock, int meta, String chestType) {
		if (world.setBlock(pt[0], pt[1], pt[2], chestBlock, rotateMetadata(chestBlock, meta), 2)) {
			TileEntityChest chest = (TileEntityChest) world.getTileEntity(pt[0], pt[1], pt[2]);
			if (chest != null && wgt.chestTries != null && wgt.chestTries.containsKey(chestType)) {
				for (int m = 0; m < wgt.chestTries.get(chestType); m++) {
					if (random.nextBoolean()) {
						ItemStack itemstack = getChestItemstack(chestType);
						if (itemstack != null)
							chest.setInventorySlotContents(random.nextInt(chest.getSizeInventory()), itemstack);
					}
				}
			}
		}
	}

	// &&&&&&&&&&&&&&&&& SPECIAL BLOCK FUNCTION - setMobSpawner
	protected void setMobSpawner(int[] pt, Block spawner, int metadata, String info) {
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
                }else if(EntityList.stringToClassMapping.containsKey(info))
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

	// Finds a surface block.
	// Depending on the value of waterIsSurface and wallIsSurface will treat
	// liquid and wall blocks as either solid or air.
	public static int findSurfaceJ(World world, int i, int k, int jinit, boolean wallIsSurface, int waterSurfaceBuffer) {
		//if(world.getChunkProvider().chunkExists(i>>4, k>>4))
		{
			if (world.provider.isHellWorld) {// the Nether
				int max = world.provider.getActualHeight()-1;
				if(jinit<max)
					max = jinit;
				if ((i % 2 == 1) ^ (k % 2 == 1)) {
					for (int j = max; j > -1; j--) {
						if (world.isAirBlock(i, j, k))
							for (; j > -1; j--)
								if (!BlockProperties.get(world.getBlock(i, j, k)).isWallable)
									return j;
					}
				} else {
					for (int j = 0; j <= max; j++)
						if (world.isAirBlock(i, j, k))
							return j;
				}
				return -1;
			} else { // other dimensions
				int minecraftHeight = world.getChunkFromBlockCoords(i, k).getHeightValue(i & 0xf, k & 0xf);
				if (minecraftHeight < jinit)
					jinit = minecraftHeight;
				for (int j = jinit; j >= 0; j--) {
					Block blockId = world.getBlock(i, j, k);
					if (!BlockProperties.get(blockId).isWallable && (wallIsSurface || !BlockProperties.get(blockId).isArtificial()))
						return j;
					if (waterSurfaceBuffer != IGNORE_WATER && BlockProperties.get(blockId).isWater)
						return BlockProperties.get(world.getBlock(i, j - waterSurfaceBuffer, k)).isWater ? HIT_WATER : j;
					// so we can still build in swamps...
				}
			}
		}
		return -1;
	}

	public static void setBlockAndMetaNoLighting(World world, int i, int j, int k, Block blockId, int meta, int flag) {
		if (i < 0xfe363c80 || k < 0xfe363c80 || i >= 0x1c9c380 || k >= 0x1c9c380 || j < 0 || j > Building.WORLD_MAX_Y)
			return;
		Chunk chunk = world.getChunkFromChunkCoords(i >> 4, k >> 4);
        Block oldBlock = null;
        BlockSnapshot blockSnapshot = null;
        if ((flag & 1) != 0) {
            oldBlock = chunk.getBlock(i & 15, j, k & 15);
        }
        if(world.captureBlockSnapshots){
            blockSnapshot = BlockSnapshot.getBlockSnapshot(world, i, j, k, flag);
            world.capturedBlockSnapshots.add(blockSnapshot);
        }
        boolean success = chunk.func_150807_a(i & 0xf, j, k & 0xf, blockId, meta);
        if(!success && blockSnapshot != null){
            world.capturedBlockSnapshots.remove(blockSnapshot);
        }
        if(success && blockSnapshot == null){
            world.markAndNotifyBlock(i, j, k, chunk, oldBlock, blockId, flag);
        }
	}

	// ******************** STATIC FUNCTIONS
	// ******************************************************************************************************************************************//
	protected static void fillDown(int[] lowPt, int jtop, World world) {
		while (BlockProperties.get(world.getBlock(lowPt[0], lowPt[1], lowPt[2])).isArtificial())
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
			setBlockAndMetaNoLighting(world, lowPt[0], lowPt[1], lowPt[2], lowPt[1] == jtop ? oldSurfaceBlockId : fillBlockId, 0, 2);
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

	public static boolean isSolidBlock(Block blockID) {
		return blockID.getMaterial().isSolid();
	}
}