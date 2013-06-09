package mods.generator;

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
 *                           (dir=0)
 *                                (-k)
 *                 n
 *                 n
 *  (dir=3) (-i)www*eee(+i)  (dir=1)
 *                 s
 *                 s
 *                (+k)
 *               (dir=2)
*/
               
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockFlowing;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockGlowStone;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockGravel;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockMelon;
import net.minecraft.block.BlockMushroomCap;
import net.minecraft.block.BlockMycelium;
import net.minecraft.block.BlockNetherrack;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockSand;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockSoulSand;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockVine;
import net.minecraft.block.BlockWeb;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.IShearable;

public class Building
{
    public final static int HIT_WATER=-666; //, HIT_SWAMP=-667;
    public final static int EASY_CHEST=0, MEDIUM_CHEST=1, HARD_CHEST=2, TOWER_CHEST=3;
    public final static int DIR_NORTH=0, DIR_EAST=1, DIR_SOUTH=2, DIR_WEST=3;
    public final static int ROT_R=1,R_HAND=1,L_HAND=-1;
    public final static int SEA_LEVEL=63,WORLD_MAX_Y=255;
   
    //**** WORKING VARIABLES ****
    protected World world;
    protected TemplateRule bRule; //main structural blocktype
    public int bWidth, bHeight, bLength;
    public int bID; //Building ID number
    private LinkedList<int[]> delayedBuildQueue;
    protected WorldGeneratorThread wgt;
    protected boolean centerAligned; //if true, alignPt x is the central axis of the building
                                                                     //if false, alignPt is the origin
   
    protected int i0, j0, k0; //origin coordinates (x=0,z=0,y=0). The child class may want to move the origin as it progress to use as a "cursor" position.
    private int xI,yI,xK,yK; //
   
    protected int bHand; //hand of secondary axis. Takes values of 1 for right-handed, -1 for left-handed.
    protected int bDir; //Direction code of primary axis. Takes values of DIR_NORTH=0,DIR_EAST=1,DIR_SOUTH=2,DIR_WEST=3.


    //****************************  CONSTRUCTORS - Building  *************************************************************************************//
    public Building(int ID_, WorldGeneratorThread wgt_,TemplateRule buildingRule_,int dir_,int axXHand_, boolean centerAligned_,int[] dim, int[] alignPt) {
        bID=ID_;
        wgt=wgt_;
        world=wgt.world;
        bRule=buildingRule_;
        if(bRule==null) 
        	bRule=TemplateRule.STONE_RULE;
        bWidth=dim[0];
        bHeight=dim[1];
        bLength=dim[2];
        bHand=axXHand_;
        centerAligned=centerAligned_;
        setPrimaryAx(dir_);
        if(alignPt!=null && alignPt.length==3){
            if(centerAligned) 
            	setOrigin(alignPt[0]-xI*bWidth/2,alignPt[1],alignPt[2]-xK*bWidth/2);
            else 
            	setOrigin(alignPt[0],alignPt[1],alignPt[2]);
        }
        delayedBuildQueue=new LinkedList<int[]>();
    }
   
    //******************** ORIENTATION FUNCTIONS *************************************************************************************************************//
       
    public void setPrimaryAx(int dir_){
        bDir=dir_;         
        //changes of basis
        switch(bDir){
            case DIR_NORTH: xI=bHand;       yI=0;
                                            xK=0;           yK=-1;
            break;
            case DIR_EAST:  xI=0;           yI=1;  
                                            xK=bHand;       yK=0;
            break;
            case DIR_SOUTH: xI=-bHand;      yI=0;
                                            xK=0;           yK=1;  
            break;
            case DIR_WEST:  xI=0;           yI=-1;
                                            xK=-bHand;      yK=0;
            break;
        }
    }
      
    public final static int rotDir(int dir,int rotation){
        return (dir+rotation+4) % 4;
    }
   
    public final static int flipDir(int dir){
        return (dir+2) % 4;
    }
   
    //outputs dir rotated to this Building's orientation and handedness
    //dir input should be the direction desired if bDir==DIR_NORTH and bHand=R_HAND
    public int orientDirToBDir(int dir){
        return bHand < 0 && dir % 2==1
                        ? (bDir + dir + 2) & 0x3
                        : (bDir + dir) & 0x3;
    }
   
    protected final void setOrigin(int i0_,int j0_, int k0_){
        i0=i0_;
        j0=j0_;
        k0=k0_;
    }
   
    protected final void setOriginLocal(int i1,int j1, int k1, int x, int z, int y){
        i0=i1+yI*y+xI*x;
        j0=j1+z;
        k0=k1+yK*y+xK*x;
    }
   
    //The origin of this building was placed to match a centerline.
    //The building previously had bWidth=oldWidth, now it has the current
    //value of bWidth and needs to have origin updated.
    protected final void recenterFromOldWidth(int oldWidth){
        i0+=xI*(oldWidth-bWidth)/2;
        k0+=xK*(oldWidth-bWidth)/2;
    }
       
  //******************** LOCAL COORDINATE FUNCTIONS - ACCESSORS *************************************************************************************************************//
    //Use these instead of World.java functions when to build from a local reference frame
    //when i0,j0,k0 are set to working values.
   
    public final int getI(int x, int y){
        return i0+yI*y+xI*x;
    }
   
    public final int getJ(int z){
        return j0+z;
    }
   
    public final int getK(int x, int y){
        return k0+yK*y+xK*x;
    }
   
    public final int[] getIJKPt(int x, int z, int y){
        int[] pt=new int[3];
        pt[0]=i0+yI*y+xI*x;
        pt[1]=j0+z;
        pt[2]=k0+yK*y+xK*x;
        return pt;
    }
   
    public final int[] getSurfaceIJKPt(int x, int y, int j, boolean wallIsSurface, int waterSurfaceBuffer){
        int[] pt=getIJKPt(x,0,y);
        pt[1]=findSurfaceJ(world, pt[0], pt[2],j, wallIsSurface, waterSurfaceBuffer);
        return pt;
    }
   
    public final int getX(int[] pt){
        return xI*(pt[0]-i0) + xK*(pt[2]-k0);
    }
   
    public final int getZ(int[] pt){
        return pt[1]-j0;
    }
   
    public final int getY(int[] pt){
        return yI*(pt[0]-i0) + yK*(pt[2]-k0);
    }
   
    protected final boolean queryExplorationHandlerForChunk(int x, int y) throws InterruptedException{
    	return wgt.master.queryExplorationHandlerForChunk(world, getI(x,y)>>4, getK(x,y)>>4, wgt);
    }
   
    protected final int getBlockIdLocal(int x, int z, int y){
        return world.getBlockId(i0+yI*y+xI*x,j0+z,k0+yK*y+xK*x);
    }
   
    protected final int getBlockMetadataLocal(int x, int z, int y){
        return world.getBlockMetadata(i0+yI*y+xI*x,j0+z,k0+yK*y+xK*x);
    }
   
  //******************** LOCAL COORDINATE FUNCTIONS - SET BLOCK FUNCTIONS *************************************************************************************************************//
    protected final void setBlockLocal(int x, int z, int y, int blockID){
    	setBlockLocal(x,z,y,new int[]{blockID, -1});
    }
   
    protected final void setBlockLocal(int x, int z, int y, int blockID, int metadata){
    	setBlockLocal(x,z,y,new int[]{blockID, metadata});
    }
   
    protected final void setBlockLocal(int x, int z, int y, int[] block){
        if(block[0]>=SPECIAL_BLOCKID_START) 
        { 
        	setSpecialBlockLocal(x,z,y,block[0],block[1]); 
        	return; 
        }
        int[] pt=getIJKPt(x,z,y);
        if(block[0]==0 && world.getBlockId(pt[0], pt[1], pt[2])==0) 
        	return;
        if(block[0]!=CHEST_ID) 
        	emptyIfChest(pt);
        /*if(IS_DELAY_BLOCK[block[0]] && block[1]!=-1) 
        	delayedBuildQueue.offer(new int[]{pt[0],pt[1],pt[2],block[0],rotateMetadata(block[0],block[1])});
        else*/ if(randLightingHash[(x & 0x7) | (y & 0x38) | (z & 0x1c0)])
        	if(block[1]==-1)
        		world.setBlock(pt[0],pt[1],pt[2],block[0]);
        	else
        		world.setBlock(pt[0],pt[1],pt[2],block[0],rotateMetadata(block[0],block[1]),3);
        else 
        	if(block[1]==-1)
        		setBlockNoLighting(world,pt[0],pt[1],pt[2],block[0]);
        	else
        		setBlockAndMetaNoLighting(world,pt[0],pt[1],pt[2],block[0],rotateMetadata(block[0],block[1]));
    }
   
    protected final void setBlockLocal(int x, int z, int y, TemplateRule rule){
        setBlockLocal(x,z,y,rule.getBlock(world.rand));
    }
     
    //allows control of lighting. Also will build even if replacing air with air.
    protected final void setBlockWithLightingLocal(int x, int z, int y, int blockID, int metadata, boolean lighting){
        if(blockID>=SPECIAL_BLOCKID_START) { 
        	setSpecialBlockLocal(x,z,y,blockID,metadata); return; }
       
        int[] pt=getIJKPt(x,z,y);
        if(blockID!=CHEST_ID) emptyIfChest(pt);
        /*if(IS_DELAY_BLOCK[blockID]) 
        	delayedBuildQueue.offer(new int[]{pt[0],pt[1],pt[2],blockID,rotateMetadata(blockID,metadata)});
        else*/ if(lighting)
            world.setBlock(pt[0],pt[1],pt[2],blockID,rotateMetadata(blockID,metadata),3);
        else 
        	setBlockAndMetaNoLighting(world,pt[0],pt[1],pt[2],blockID,rotateMetadata(blockID,metadata));
    }
   
    protected final void flushDelayed(){
        while(delayedBuildQueue.size()>0){
            int[] block=delayedBuildQueue.poll();
           
            //if stairs are running into ground. replace them with a solid block
            if(IS_STAIRS_BLOCK[block[3]]){
                int adjId=world.getBlockId(block[0]-DIR_TO_I[STAIRS_META_TO_DIR[block[4]/*<4?block[4]:(block[4]-4)*/]],block[1],block[2]-DIR_TO_K[STAIRS_META_TO_DIR[block[4]/*<4?block[4]:(block[4]-4)*/]]);
                int aboveID=world.getBlockId(block[0],block[1]+1,block[2]);
                if(IS_ARTIFICAL_BLOCK[adjId] || IS_ARTIFICAL_BLOCK[aboveID]){
                    block[3]=stairToSolidBlock(block[3]);
                    block[4]=0;
                }
                else if(!IS_WALLABLE[adjId] || !IS_WALLABLE[aboveID] || IS_WATER_BLOCK[adjId] || IS_WATER_BLOCK[aboveID] ){
                    continue;  //solid or liquid non-wall block. In this case, just don't build the stair (aka preserve block).
                }
            }
            else if(block[3]==VINES_ID){
	                if(block[4]==0 && !isSolidBlock(world.getBlockId(block[0],block[1]+1,block[2])))
	                    block[4]=1;
               
                if(block[4]!=0){
                    int dir=VINES_META_TO_DIR[block[4]];
                    while(true){
                        if(isSolidBlock(world.getBlockId(block[0]+DIR_TO_I[dir],block[1],block[2]+DIR_TO_K[dir])))
                            break;
                       
                        dir=(dir+1)%4;
                        if(dir==VINES_META_TO_DIR[block[4]]){ //we've looped through everything
                            if(isSolidBlock(world.getBlockId(block[0],block[1]+1,block[2]))){
                                    dir=-1;
                                    break;
                            }
                            return; //did not find a surface we can attach to
                        }
                    }
                    block[4]=dir==-1 ? 0 : VINES_DIR_TO_META[dir];
                }
            }
           
            //It seems Minecraft orients torches automatically, so I shouldn't have to do anything...
            //else if(block[3]==TORCH_ID || block[3]==REDSTONE_TORCH_ON_ID || block[3]==REDSTONE_TORCH_OFF_ID){
            //      block[4]=1;
            //}
           
            else if(block[3]==PAINTING_SPECIAL_ID) 
            	setPainting(block, block[4]);           
            else if(block[3]==TORCH_ID)
                if(Block.torchWood.canPlaceBlockAt(world,block[0],block[1],block[2]))
                	world.setBlock(block[0],block[1],block[2],block[3],block[4],3);//force lighting update
            else if(block[3]==GLOWSTONE_ID)
            	world.setBlock(block[0],block[1],block[2],block[3],block[4],3);//force lighting update
            else if(randLightingHash[(block[0] & 0x7) | (block[1] & 0x38) | (block[2] & 0x1c0)])
            	world.setBlock(block[0],block[1],block[2],block[3],block[4],3);
            else 
            	setBlockAndMetaNoLighting(world,block[0],block[1],block[2],block[3],block[4]);
        }
    }
 
  //******************** LOCAL COORDINATE FUNCTIONS - SPECIAL BLOCK FUNCTIONS *************************************************************************************************************//
 
  //&&&&&&&&&&&&&&&&& SPECIAL BLOCK FUNCTION - setSpecialBlockLocal &&&&&&&&&&&&&&&&&&&&&&&&&&&&&//
    protected final void setSpecialBlockLocal(int x, int z, int y, int blockID, int metadata){
        if(blockID==PRESERVE_ID) return; // preserve existing world block
       
        int[] pt=getIJKPt(x,z,y);
       
        if(blockID==0){
            int presentBlock=world.getBlockId(pt[0], pt[1], pt[2]);
            if(presentBlock!=0 && !IS_WATER_BLOCK[presentBlock]){
                if( !(IS_WATER_BLOCK[world.getBlockId(pt[0]-1,pt[1],pt[2])] || IS_WATER_BLOCK[world.getBlockId(pt[0],pt[1],pt[2]-1)]
                   || IS_WATER_BLOCK[world.getBlockId(pt[0]+1,pt[1],pt[2])] || IS_WATER_BLOCK[world.getBlockId(pt[0],pt[1],pt[2]+1)]
                   || IS_WATER_BLOCK[world.getBlockId(pt[0],pt[1]+1,pt[2])])) {//don't adjacent to a water block
                	world.setBlockToAir(pt[0], pt[1], pt[2]);
                }
            }
        }
       
        switch(blockID) {
            case ZOMBIE_SPAWNER_ID: setMobSpawner(pt,1,0); return;
            case SKELETON_SPAWNER_ID: setMobSpawner(pt,1,1); return;
            case SPIDER_SPAWNER_ID: setMobSpawner(pt,1,2); return;
            case CREEPER_SPAWNER_ID: setMobSpawner(pt,1,3); return;
            case UPRIGHT_SPAWNER_ID: if(world.rand.nextInt(3)==0) setMobSpawner(pt,1,3); else setMobSpawner(pt,2,0); return;
            case EASY_SPAWNER_ID: setMobSpawner(pt,2,0); return;
            case MEDIUM_SPAWNER_ID: setMobSpawner(pt,3,0); return;
            case HARD_SPAWNER_ID: setMobSpawner(pt,4,0); return;
            case EASY_CHEST_ID: setLootChest(pt,EASY_CHEST); return;
            case MEDIUM_CHEST_ID: setLootChest(pt,MEDIUM_CHEST); return;
            case HARD_CHEST_ID: setLootChest(pt,HARD_CHEST); return;
            case TOWER_CHEST_ID: setLootChest(pt,TOWER_CHEST); return;
            case PIG_ZOMBIE_SPAWNER_ID: setMobSpawner(pt,1,4); return;
            case ENDERMAN_SPAWNER_ID: setMobSpawner(pt,1,6); return;
            case CAVE_SPIDER_SPAWNER_ID: setMobSpawner(pt,1,7); return;
            case GHAST_SPAWNER_ID: setMobSpawner(pt,1,5); return;
            case WALL_STAIR_ID: world.setBlock(pt[0],pt[1],pt[2],STEP_ID,rotateMetadata(STEP_ID,metadata),3); return; //this case should not be reached
            //case PAINTING_SPECIAL_ID: delayedBuildQueue.offer(new int[]{pt[0],pt[1],pt[2],blockID,metadata}); return;
            case BLAZE_SPAWNER_ID: setMobSpawner(pt,1,8); return;
            case SLIME_SPAWNER_ID: setMobSpawner(pt,1,9); return;
            case LAVA_SLIME_SPAWNER_ID: setMobSpawner(pt,1,10); return;
            case VILLAGER_SPAWNER_ID: setMobSpawner(pt,1,11); return;
            case SNOW_GOLEM_SPAWNER_ID: setMobSpawner(pt,1,12); return;
            case MUSHROOM_COW_SPAWNER_ID: setMobSpawner(pt,1,13); return;
            case SHEEP_SPAWNER_ID: setMobSpawner(pt,1,14); return;
            case COW_SPAWNER_ID: setMobSpawner(pt,1,15); return;
            case CHICKEN_SPAWNER_ID: setMobSpawner(pt,1,16); return;
            case SQUID_SPAWNER_ID: setMobSpawner(pt,1,17); return;
            case WOLF_SPAWNER_ID: setMobSpawner(pt,1,18); return;
            case GIANT_ZOMBIE_SPAWNER_ID: setMobSpawner(pt,1,19); return;
            case SILVERFISH_SPAWNER_ID: setMobSpawner(pt,1,20); return;
            case DRAGON_SPAWNER_ID: setMobSpawner(pt,1,21); return;
            case OCELOT_SPAWNER_ID: setMobSpawner(pt,1,22); return;
            case IRON_GOLEM_SPAWNER_ID: setMobSpawner(pt,1,23); return;
            case WITHERBOSS_SPAWNER_ID: setMobSpawner(pt,1,24); return;
            case BAT_SPAWNER_ID: setMobSpawner(pt,1,25); return;
            case WITCH_SPAWNER_ID: setMobSpawner(pt,1,26); return;
            default:world.setBlock(pt[0],pt[1],pt[2],blockID,metadata,3);return;
        }
    }
   
    //&&&&&&&&&&&&&&&&& SPECIAL BLOCK FUNCTION - setMobSpawner &&&&&&&&&&&&&&&&&&&&&&&&&&&&&//
    private final void setMobSpawner(int[] pt, int nTypes, int offset){
        String mob="";
        int n = world.rand.nextInt(nTypes)+offset;
        switch(n) {
            case 0: mob="Zombie"; break;
            case 1: mob="Skeleton"; break;
            case 2: mob="Spider"; break;
            case 3: mob="Creeper"; break;
            case 4: mob="PigZombie"; break;
            case 5: mob="Ghast"; break;
            case 6: mob="Enderman"; break;
            case 7: mob="CaveSpider"; break;
            case 8: mob="Blaze"; break;
            case 9: mob="Slime"; break;
            case 10: mob="LavaSlime"; break;
            case 11: mob="Villager"; break;
            case 12: mob="SnowMan"; break;
            case 13: mob="MushroomCow"; break;
            case 14: mob="Sheep"; break;
            case 15: mob="Cow"; break;
            case 16: mob="Chicken"; break;
            case 17: mob="Squid"; break;
            case 18: mob="Wolf"; break;
            case 19: mob="Giant"; break;
            case 20: mob="Silverfish"; break;
            case 21: mob="EnderDragon"; break;
            case 22: mob="Ozelot"; break;
            case 23: mob="VillagerGolem"; break;
            case 24: mob="WitherBoss"; break;
            case 25: mob="Bat"; break;
            case 26: mob="Witch";break;
            default: mob="Skeleton"; break;
        }
        world.setBlock(pt[0],pt[1],pt[2],MOB_SPAWNER_ID);
        TileEntityMobSpawner tileentitymobspawner=( TileEntityMobSpawner)world.getBlockTileEntity(pt[0],pt[1],pt[2]);
        if(tileentitymobspawner!=null) 
        	tileentitymobspawner.func_98049_a().setMobID(mob);//set mobID in MobSpawnerBaseLogic
    }
   
  //&&&&&&&&&&&&&&&&& SPECIAL BLOCK FUNCTION - setLootChest &&&&&&&&&&&&&&&&&&&&&&&&&&&&&//
    private final void setLootChest(int[] pt,int chestType){
        if(world.setBlock(pt[0],pt[1],pt[2],CHEST_ID)){
	        TileEntityChest chest=(TileEntityChest)world.getBlockTileEntity(pt[0],pt[1],pt[2]);
	        if (wgt.chestTries!=null){
	         for(int m=0; m<wgt.chestTries[chestType]; m++){
	                if(world.rand.nextBoolean()){
                        ItemStack itemstack=getChestItemstack(chestType);
                        if(itemstack != null && chest!=null)
                                chest.setInventorySlotContents(world.rand.nextInt(chest.getSizeInventory()), itemstack);
	                }
	            }
	        }
        }
    }
       
    private ItemStack getChestItemstack(int chestType){
        if(chestType==TOWER_CHEST && world.rand.nextInt(4)==0){ //for tower chests, chance of returning the tower block
            return new ItemStack(bRule.primaryBlock[0],world.rand.nextInt(10),bRule.primaryBlock[1]);
        }
        int[][] itempool=wgt.chestItems[chestType];
        int idx=pickWeightedOption(world.rand,itempool[3],itempool[0]);
        return new ItemStack(itempool[1][idx],itempool[4][idx] + world.rand.nextInt(itempool[5][idx]-itempool[4][idx]+1),
                                                 itempool[2][idx]);
    }
       
    //&&&&&&&&&&&&&&&&& SPECIAL BLOCK FUNCTION - setSignOrPost &&&&&&&&&&&&&&&&&&&&&&&&&&&&&//
    public void setSignOrPost(int x2,int z2,int y2,boolean post,int sDir,String[] lines){
    	wgt.master.logOrPrint("trying to set a sign, but method is not yet working");
    	/*  int[] pt=getIJKPt(x2,z2,y2);
            world.setBlockAndMetadata(pt[0],pt[1],pt[2],post ? SIGN_POST_ID : WALL_SIGN_ID,sDir);
           
            TileEntitySign tileentitysign=(TileEntitySign)world.getBlockTileEntity(pt[0],pt[1],pt[2]);
            if(tileentitysign==null) return;
           
            for(int m=0;m<Math.min(lines.length, 4);m++){
                            
                            tileentitysign.signText[m]=lines[m];
        }*/
    }
       
    //&&&&&&&&&&&&&&&&& SPECIAL BLOCK FUNCTION - setPainting &&&&&&&&&&&&&&&&&&&&&&&&&&&&&//
   //FIXME
    public void setPainting(int[] pt, int metadata){
   		wgt.master.logOrPrint("trying to set a painting, but method is not yet working");
        //painting uses same orientation meta as ladders.
        //Have to adjust ijk since unlike ladders the entity exists at the block it is hung on.
        /*int dir=orientDirToBDir(LADDER_META_TO_DIR[metadata]);
        pt[0]-=DIR_TO_I[dir];
        pt[2]-=DIR_TO_K[dir];
        if(dir==DIR_NORTH) pt[2]++; 
        else if(dir==DIR_SOUTH) pt[2]--; 
        else if(dir==DIR_WEST) pt[0]++; 
        else pt[0]--;
                   
            EntityPainting entitypainting = new EntityPainting(world,pt[0],pt[1],pt[2],PAINTING_DIR_TO_FACEDIR[dir]);
    if(!world.isRemote && entitypainting.onValidSurface() )
        world.spawnEntityInWorld(entitypainting);*/
   
    }
   
  //******************** LOCAL COORDINATE FUNCTIONS - BLOCK TEST FUNCTIONS *************************************************************************************************************//
    protected final boolean isWallable(int x, int z, int y){
        return IS_WALLABLE[world.getBlockId(i0+yI*y+xI*x,j0+z,k0+yK*y+xK*x)];
    }
         
    protected final boolean isWallableIJK(int pt[]){
        if(pt==null) return false;
        return IS_WALLABLE[world.getBlockId(pt[0], pt[1], pt[2])];
    }
         
    protected final boolean isWallBlock(int x, int z, int y){
         return IS_ARTIFICAL_BLOCK[world.getBlockId(i0+yI*y+xI*x,j0+z,k0+yK*y+xK*x)];
    }
         
    protected final boolean isArtificialWallBlock(int x, int z, int y){
        int blockId=getBlockIdLocal(x,z,y);
        return IS_ARTIFICAL_BLOCK[blockId] && !(blockId==SANDSTONE_ID && (getBlockIdLocal(x,z+1,y)==SAND_ID || getBlockIdLocal(x,z+2,y)==SAND_ID));
    }
   
    protected final boolean isStairBlock(int x, int z, int y){
        int blkId=getBlockIdLocal(x,z,y);
        return (blkId==STEP_ID || IS_STAIRS_BLOCK[blkId]);
    }
   
    protected final boolean isNextToDoorway(int x, int z, int y){
        return isDoorway(x-1,z,y) || isDoorway(x+1,z,y) || isDoorway(x,z,y-1) || isDoorway(x-1,z,y+1);
    }
   
    protected final boolean isDoorway(int x, int z, int y){
        return isFloor(x,z,y) && (isWallBlock(x+1,z,y) && isWallBlock(x-1,z,y) || isWallBlock(x,z,y+1) && isWallBlock(x-1,z,y-1));
    }
   
  //true if block is air, block below is wall block
    protected final boolean isFloor(int x, int z, int y){
        int blkId1=getBlockIdLocal(x,z,y), blkId2=getBlockIdLocal(x,z-1,y);
        //return ((blkId1==0 || blkId1==STEP_ID) && IS_WALL_BLOCK[blkId2] && blkId2!=LADDER_ID);
        return ((blkId1==0) && IS_ARTIFICAL_BLOCK[blkId2] && blkId2!=LADDER_ID);
    }
   
   
  //******************** LOCAL COORDINATE FUNCTIONS - HELPER FUNCTIONS *************************************************************************************************************//
    private final void emptyIfChest(int[] pt){
        //if block is a chest empty it
        if(pt!=null && world.getBlockId(pt[0],pt[1],pt[2])==CHEST_ID){
               
                TileEntityChest tileentitychest=(TileEntityChest)world.getBlockTileEntity(pt[0],pt[1], pt[2]);
                for(int m=0;m<tileentitychest.getSizeInventory();m++)  tileentitychest.setInventorySlotContents(m,null);
            }
    }
   
    public final String localCoordString(int x, int z, int y){
        int[] pt=getIJKPt(x,z,y);
        return "("+pt[0]+","+pt[1]+","+pt[2]+")";
    }
   
    public final static String globalCoordString(int[] pt){
        return "("+pt[0]+","+pt[1]+","+pt[2]+")";
    }
   
    public final static String globalCoordString(int i, int j, int k){
        return "("+i+","+j+","+k+")";
    }
   
    //replaces orientationString
    protected final String IDString(){
        String str="ID="+bID+" axes(Y,X)=";
        switch(bDir){
                case DIR_SOUTH: return str + "(S," + (bHand>0 ? "W)" : "E)");
                case DIR_NORTH: return str+ "(N," + (bHand>0 ? "E)" : "W)");
                case DIR_WEST: return str+ "(W," + (bHand>0 ? "N)" : "S)");
                case DIR_EAST: return str + "(E," + (bHand>0 ? "S)" : "N)");
        }
        return "Error - bad dir value for ID="+bID;
    }
   
    protected final static void fillDown(int[] lowPt, int jtop, World world){
       while(IS_ARTIFICAL_BLOCK[world.getBlockId(lowPt[0],lowPt[1],lowPt[2])]) 
    	   lowPt[1]--;
       int oldSurfaceBlockId=world.getBlockId(lowPt[0], lowPt[1], lowPt[2]);
       if(IS_ORE_BLOCK[oldSurfaceBlockId]) 
    	   oldSurfaceBlockId=STONE_ID;
       if(oldSurfaceBlockId==DIRT_ID || (lowPt[1] <= SEA_LEVEL && oldSurfaceBlockId==SAND_ID))
               oldSurfaceBlockId=GRASS_ID;
       if(oldSurfaceBlockId==0) 
    	   oldSurfaceBlockId= world.provider.isHellWorld ? NETHERRACK_ID : GRASS_ID;
       int fillBlockId=oldSurfaceBlockId==GRASS_ID ? DIRT_ID : oldSurfaceBlockId;
           
       for(; lowPt[1]<=jtop; lowPt[1]++)
               setBlockAndMetaNoLighting(world,lowPt[0],lowPt[1],lowPt[2], lowPt[1]==jtop ? oldSurfaceBlockId: fillBlockId,0);
    }
   
   //call with z=start of builDown, will buildDown a maximum of maxDepth blocks + foundationDepth.
   //if buildDown column is completely air, instead buildDown reserveDepth blocks.
    protected final void buildDown(int x, int z, int y, TemplateRule buildRule, int maxDepth, int foundationDepth, int reserveDepth){
       int stopZ;
       for(stopZ=z; stopZ>z-maxDepth; stopZ--){
               if(!isWallable(x,stopZ,y)) 
            	   break; //find ground height
       }
       
       if(stopZ==z-maxDepth && isWallable(x,z-maxDepth,y)) //if we never hit ground
               stopZ=z-reserveDepth;
       else stopZ-=foundationDepth;
               
       for(int z1=z; z1>stopZ; z1--){
            int[] idAndMeta=buildRule.getBlock(world.rand);
            setBlockWithLightingLocal(x,z1,y,idAndMeta[0],idAndMeta[1],false);
        }
    }
   
    protected boolean isObstructedFrame(int zstart,int ybuffer){
        for(int z1=zstart; z1<bHeight; z1++){
            //for(int x1=0; x1<length; x1++) for(int y1=ybuffer; y1<width-1;y1++)
            //      if(isWallBlock(x1,z1,y1))
            //              return true;
           
            for(int x1=0; x1<bWidth; x1++){
                    if(isArtificialWallBlock(x1,z1,bLength-1)){
                            return true;
                    }}
            for(int y1=ybuffer; y1<bLength-1;y1++){
                    if(isArtificialWallBlock(0,z1,y1)){
                            return true;
                    }
                    if(isArtificialWallBlock(bWidth-1,z1,y1)) {
                            return true;
                    }
            }
        }
        return false;
    }
   
    protected boolean isObstructedSolid(int pt1[],int pt2[]){
        for(int x1=pt1[0]; x1<=pt2[0]; x1++){
            for(int z1=pt1[1]; z1<=pt2[1]; z1++){
                for(int y1=pt1[2]; y1<=pt2[2]; y1++){
                    if(!isWallable(x1,z1,y1)){
                            return true;
        }}}}
        return false;
    }  
         
   //******************** STATIC FUNCTIONS ******************************************************************************************************************************************//
   
    public static void setBlockNoLighting(World world, int i, int j, int k, int blockId){
	   setBlockAndMetaNoLighting(world, i, j, k, blockId, 0);
    }
   
    public static void setBlockAndMetaNoLighting(World world, int i, int j, int k, int blockId, int meta){
       if(i < 0xfe363c80 || k < 0xfe363c80 || i >= 0x1c9c380 || k >= 0x1c9c380 || j < 0 || j > Building.WORLD_MAX_Y)
    	   return;

       world.getChunkFromChunkCoords(i >> 4, k >> 4).setBlockIDWithMetadata(i & 0xf, j, k & 0xf, blockId, meta);
    }
   
    //wiggle allows for some leeway before nonzero is detected
    protected final static int signum(int n,int wiggle){
        if(n<=wiggle && -n<=wiggle) 
        	return 0;
        return n < 0 ? -1 : 1;
    }
   
    protected final static int minOrMax(int[] a,boolean isMin){
        if (isMin){
        	int min= Integer.MAX_VALUE;
        for (int i : a)
        	min = Math.min(min, i);
        return min;
        }   	       	
    	else {
    		int max= Integer.MIN_VALUE;
    	for (int i : a)
    		max = Math.max(max, i);
    	return max;	
    	}
    }
   
    public static int distance(int[] pt1, int[] pt2){
        return (int)Math.sqrt((double)((pt1[0]-pt2[0])*(pt1[0]-pt2[0]) + (pt1[1]-pt2[1])*(pt1[1]-pt2[1]) + (pt1[2]-pt2[2])*(pt1[2]-pt2[2])));
    }
   
        //****************************************  CONSTRUCTOR - findSurfaceJ *************************************************************************************//
        //Finds a surface block.
        //Depending on the value of waterIsSurface and wallIsSurface will treat liquid and wall blocks as either solid or air.
        public final static int IGNORE_WATER=-1;
      
    public static int findSurfaceJ(World world, int i, int k, int jinit, boolean wallIsSurface, int waterSurfaceBuffer){
        int blockId;
        //if(world.getChunkProvider().chunkExists(i>>4, k>>4))
        {
            if(world.provider.isHellWorld) {//the Nether
                    if( (i%2==1) ^ (k%2==1) ) {
                            for(int j=(int) (WORLD_MAX_Y*0.5); j>-1; j--) {
                                    if(world.getBlockId(i,j,k)==0)
                                            for(; j>-1; j--)
                                                    if(!IS_WALLABLE[world.getBlockId(i,j,k)])
                                                            return j;
                            }
                    }else {
                            for(int j=0; j<=(int)(WORLD_MAX_Y*0.5); j++)
                                    if(world.getBlockId(i,j,k)==0 )
                                                            return j;
                    }
                    return -1;
            }
            else{ //other dimensions
                int minecraftHeight=world.getChunkFromBlockCoords(i,k).getHeightValue(i & 0xf,k & 0xf);
                if(minecraftHeight < jinit) jinit=minecraftHeight;
                for(int j=jinit; j>=0; j--){
                    blockId=world.getBlockId(i,j,k);
                    if(!IS_WALLABLE[blockId] && (wallIsSurface || !IS_ARTIFICAL_BLOCK[blockId]))
                            return j;
                    if(waterSurfaceBuffer!=IGNORE_WATER && IS_WATER_BLOCK[blockId])
                            return IS_WATER_BLOCK[world.getBlockId(i, j-waterSurfaceBuffer, k)] ? HIT_WATER : j;  //so we can still build in swamps...
                }
            }
        }
        return -1;
    }
         
      public static int pickWeightedOption( Random random, int[] weights, int[] options){
            int sum=0, n;
            for(n=0;n<weights.length;n++) sum+=weights[n];
            if(sum<=0) {
                System.err.println("Error selecting options, weightsum not positive!");
                return options[0]; //default to returning first option
            }
            int s=random.nextInt(sum);
            sum=0;
            n=0;
            while(n<weights.length){
                sum+=weights[n];
                if(sum>s) return options[n];
                n++;
            }
            return options[options.length-1];
      }
       
    private int rotateMetadata( int blockID, int metadata) {
        int tempdata = 0;
        if(IS_STAIRS_BLOCK[blockID]){
        	if(metadata>=4)
			{
    			tempdata+=4;
    			metadata-=4;
			}
            return STAIRS_DIR_TO_META[orientDirToBDir(STAIRS_META_TO_DIR[metadata])]+tempdata;
        }                
        if(IS_DOOR_BLOCK[blockID]){
        	//think of door metas applying to doors with hinges on the left that open in (when seen facing in)
            //in this case, door metas match the dir in which the door opens
            //e.g. a door on the south face of a wall, opening in to the north has a meta value of 0 (or 4 if the door is opened).                                                                          
        	if(metadata>=8)// >=8:the top half of the door
        		return metadata;
        	if(metadata >= 4 ) {
                    // >=4:the door has swung counterclockwise around its hinge
                    tempdata+=4;
                    metadata -= 4;
            }
        	return DOOR_DIR_TO_META[orientDirToBDir(DOOR_META_TO_DIR[metadata])] + tempdata;
        }
        switch( blockID ) {
      
        case LEVER_ID: case STONE_BUTTON_ID: case WOOD_BUTTON_ID:
        	// check to see if this is flagged as thrown
                if( metadata - 8 > 0 ) {
                        tempdata += 8;
                        metadata -= 8;
                }
                // now see if it's a floor switch
                if( blockID == LEVER_ID && ( metadata == 5 || metadata == 6 || metadata == 7) ) {
                    // we'll leave this as-is
                    return metadata + tempdata;
                }          
                return BUTTON_DIR_TO_META[orientDirToBDir(BUTTON_META_TO_DIR[metadata])] + tempdata;
        case TORCH_ID: case REDSTONE_TORCH_OFF_ID: case REDSTONE_TORCH_ON_ID:
        	if(metadata >= 5){
        		return metadata;
        	}
        	return BUTTON_DIR_TO_META[orientDirToBDir(BUTTON_META_TO_DIR[metadata])] + tempdata;
        case LADDER_ID: case DISPENSER_ID: case FURNACE_ID: case BURNING_FURNACE_ID: case WALL_SIGN_ID: case PISTON_ID: case PISTON_EXTENSION_ID:
                if(blockID==PISTON_ID || blockID==PISTON_EXTENSION_ID){
                        if( metadata - 8 >= 0 ) {
                                //pushed or not, sticky or not
                                tempdata += 8;
                                metadata -= 8;
                        }
                        if(metadata==0 || metadata==1) 
                        	return metadata + tempdata;
                }
                return LADDER_DIR_TO_META[orientDirToBDir(LADDER_META_TO_DIR[metadata])] + tempdata;                    
        case RAILS_ID: case POWERED_RAIL_ID: case DETECTOR_RAIL_ID: case ACTIVATORRAIL_ID:
                switch( bDir ) {
                case DIR_NORTH:
                        // flat tracks
                        if( metadata == 0 ) { return 0; }
                        if( metadata == 1 ) { return 1; }
                        // ascending tracks
                        if( metadata == 2 ) { return 2; }
                        if( metadata == 3 ) { return 3; }
                        if( metadata == 4 ) { return bHand==1 ? 4:5; }
                        if( metadata == 5 ) { return bHand==1 ? 5:4; }
                        // curves
                        if( metadata == 6 ) { return bHand==1 ? 6:9; }
                        if( metadata == 7 ) { return bHand==1 ? 7:8; }
                        if( metadata == 8 ) { return bHand==1 ? 8:7; }
                        if( metadata == 9 ) { return bHand==1 ? 9:6; }
                case DIR_EAST:
                        // flat tracks
                        if( metadata == 0 ) { return 1; }
                        if( metadata == 1 ) { return 0; }
                        // ascending tracks
                        if( metadata == 2 ) { return 5; }
                        if( metadata == 3 ) { return 4; }
                        if( metadata == 4 ) { return bHand==1 ? 2:3; }
                        if( metadata == 5 ) { return bHand==1 ? 3:2; }
                        // curves
                        if( metadata == 6 ) { return bHand==1 ? 7:6; }
                        if( metadata == 7 ) { return bHand==1 ? 8:9; }
                        if( metadata == 8 ) { return bHand==1 ? 9:8; }
                        if( metadata == 9 ) { return bHand==1 ? 6:7; }
                case DIR_SOUTH:
                        // flat tracks
                        if( metadata == 0 ) { return 0; }
                        if( metadata == 1 ) { return 1; }
                        // ascending tracks
                        if( metadata == 2 ) { return 3; }
                        if( metadata == 3 ) { return 2; }
                        if( metadata == 4 ) { return bHand==1 ? 5:4; }
                        if( metadata == 5 ) { return bHand==1 ? 4:5; }
                        // curves
                        if( metadata == 6 ) { return bHand==1 ? 8:7; }
                        if( metadata == 7 ) { return bHand==1 ? 9:6; }
                        if( metadata == 8 ) { return bHand==1 ? 6:9; }
                        if( metadata == 9 ) { return bHand==1 ? 7:8; }
                case DIR_WEST:
                        // flat tracks
                        if( metadata == 0 ) { return 1; }
                        if( metadata == 1 ) { return 0; }
                        // ascending tracks
                        if( metadata == 2 ) { return 4; }
                        if( metadata == 3 ) { return 5; }
                        if( metadata == 4 ) { return bHand==1 ? 3:2; }
                        if( metadata == 5 ) { return bHand==1 ? 2:3; }
                        // curves
                        if( metadata == 6 ) { return bHand==1 ? 9:8; }
                        if( metadata == 7 ) { return bHand==1 ? 6:7; }
                        if( metadata == 8 ) { return bHand==1 ? 7:6; }
                        if( metadata == 9 ) { return bHand==1 ? 8:9; }
                }
                break;
        case TRAP_DOOR_ID:
            while( metadata >= 4){
                tempdata += 4;
                metadata -= 4;
            }
            return TRAPDOOR_DIR_TO_META[orientDirToBDir(TRAPDOOR_META_TO_DIR[metadata])] + tempdata;
        
        case BED_BLOCK_ID: case FENCE_GATE_ID: case TRIPWIRE_SOURCE_ID:       
        case PUMPKIN_ID: case JACK_O_LANTERN_ID: case DIODE_BLOCK_OFF_ID: case DIODE_BLOCK_ON_ID:
                while( metadata >= 4 ) 
                {
                    tempdata += 4;
                    metadata -= 4;
                }
                return BED_DIR_TO_META[orientDirToBDir(BED_META_TO_DIR[metadata])] + tempdata;
       
        case VINES_ID:
                if(metadata==0) return 0;
                else if(metadata==1 || metadata==2 || metadata==4 || metadata==8)
                        return VINES_DIR_TO_META[(bDir+VINES_META_TO_DIR[metadata]) % 4];
                else return 1; //default case since vine do not have to have correct metadata
        
        case SIGN_POST_ID:
                // sign posts
                switch( bDir ) {
                case DIR_NORTH:
                        if( metadata == 0 ) { return bHand==1 ? 0:8; }
                        if( metadata == 1 ) { return bHand==1 ? 1:7; }
                        if( metadata == 2 ) { return bHand==1 ? 2:6; }
                        if( metadata == 3 ) { return bHand==1 ? 3:5; }
                        if( metadata == 4 ) { return 4; }
                        if( metadata == 5 ) { return bHand==1 ? 5:3; }
                        if( metadata == 6 ) { return bHand==1 ? 6:2; }
                        if( metadata == 7 ) { return bHand==1 ? 7:1; }
                        if( metadata == 8 ) { return bHand==1 ? 8:0; }
                       
                        if( metadata == 9 ) { return bHand==1 ? 9:15; }
                        if( metadata == 10 ) { return bHand==1 ? 10:14; }
                        if( metadata == 11 ) { return bHand==1 ? 11:13; }
                        if( metadata == 12 ) { return 12; }
                        if( metadata == 13 ) { return bHand==1 ? 13:11; }
                        if( metadata == 14 ) { return bHand==1 ? 14:10; }
                        if( metadata == 15 ) { return bHand==1 ? 15:9; }
                case DIR_EAST:
                        if( metadata == 0 ) { return bHand==1 ? 4:12; }
                        if( metadata == 1 ) { return bHand==1 ? 5:11; }
                        if( metadata == 2 ) { return bHand==1 ? 6:10; }
                        if( metadata == 3 ) { return bHand==1 ? 7:9; }
                        if( metadata == 4 ) { return 8; }
                        if( metadata == 5 ) { return bHand==1 ? 9:7; }
                        if( metadata == 6 ) { return bHand==1 ? 10:6; }
                        if( metadata == 7 ) { return bHand==1 ? 11:5; }
                        if( metadata == 8 ) { return bHand==1 ? 12:4; }
                       
                        if( metadata == 9 ) { return bHand==1 ? 13:3; }
                        if( metadata == 10 ) { return bHand==1 ? 14:2; }
                        if( metadata == 11 ) { return bHand==1 ? 15:1; }
                        if( metadata == 12 ) { return 0; }
                        if( metadata == 13 ) { return bHand==1 ? 1:15; }
                        if( metadata == 14 ) { return bHand==1 ? 2:14; }
                        if( metadata == 15 ) { return bHand==1 ? 3:13; }
                case DIR_SOUTH:
                        if( metadata == 0 ) { return bHand==1 ? 8:0; }
                        if( metadata == 1 ) { return bHand==1 ? 9:15; }
                        if( metadata == 2 ) { return bHand==1 ? 10:14; }
                        if( metadata == 3 ) { return bHand==1 ? 11:13; }
                        if( metadata == 4 ) { return 12; }
                        if( metadata == 5 ) { return bHand==1 ? 13:11; }
                        if( metadata == 6 ) { return bHand==1 ? 14:10; }
                        if( metadata == 7 ) { return bHand==1 ? 15:9; }
                        if( metadata == 8 ) { return bHand==1 ? 0:8; }
                       
                        if( metadata == 9 ) { return bHand==1 ? 1:7; }
                        if( metadata == 10 ) { return bHand==1 ? 2:6; }
                        if( metadata == 11 ) { return bHand==1 ? 3:5; }
                        if( metadata == 12 ) { return 4; }
                        if( metadata == 13 ) { return bHand==1 ? 5:3; }
                        if( metadata == 14 ) { return bHand==1 ? 6:2; }
                        if( metadata == 15 ) { return bHand==1 ? 7:1; }
                case DIR_WEST:
                        if( metadata == 0 ) { return bHand==1 ? 12:4; }
                        if( metadata == 1 ) { return bHand==1 ? 13:3; }
                        if( metadata == 2 ) { return bHand==1 ? 14:2; }
                        if( metadata == 3 ) { return bHand==1 ? 15:1; }
                        if( metadata == 4 ) { return 0; }
                        if( metadata == 5 ) { return bHand==1 ? 1:15; }
                        if( metadata == 6 ) { return bHand==1 ? 2:14; }
                        if( metadata == 7 ) { return bHand==1 ? 3:13; }
                        if( metadata == 8 ) { return bHand==1 ? 4:12; }
                       
                        if( metadata == 9 ) { return bHand==1 ? 5:11; }
                        if( metadata == 10 ) { return bHand==1 ? 6:10; }
                        if( metadata == 11 ) { return bHand==1 ? 7:9; }
                        if( metadata == 12 ) { return 8; }
                        if( metadata == 13 ) { return bHand==1 ? 9:7; }
                        if( metadata == 14 ) { return bHand==1 ? 10:6; }
                        if( metadata == 15 ) { return bHand==1 ? 11:5; }
                }
        }
        return metadata + tempdata;
    }
   
    public static String metaValueCheck(int blockID, int metadata){
    	if(metadata<0 || metadata >=16) 
    		return "All Minecraft meta values should be between 0 and 15";
    	String fail = Block.blocksList[blockID].getUnlocalizedName()+" meta value should be between";
        
        if(IS_STAIRS_BLOCK[blockID])
        	return metadata < 8 ? null : fail+" 0 and 7";
        switch( blockID ) {                                      
                //orientation metas    					
            case RAILS_ID:
            	return metadata < 10 ? null : fail+" 0 and 9";                      
            case STONE_BUTTON_ID: case WOOD_BUTTON_ID:
                if(metadata > 8)
                	metadata-=8;
                return metadata>0 && metadata < 5 ? null : fail+" 1 and 4 or 9 and 12";                        
            case LADDER_ID: case DISPENSER_ID: case FURNACE_ID: case BURNING_FURNACE_ID:
            case WALL_SIGN_ID: case PAINTING_SPECIAL_ID: case PISTON_ID: case PISTON_EXTENSION_ID:
            case CHEST_ID: case HOPPER_ID: case DROPPER_ID:
            case POWERED_RAIL_ID: case DETECTOR_RAIL_ID: case ACTIVATORRAIL_ID:
                if(metadata >= 8) 
                	metadata-=8;
                return metadata < 6 ? null : fail+" 0 and 5 or 8 and 13";                             
            case PUMPKIN_ID: case JACK_O_LANTERN_ID:
                return metadata < 5 ? null : fail+" 0 and 4";
            case FENCE_GATE_ID: case STEP_ID:
                return metadata < 8 ? null : fail+" 0 and 7";
            case BED_BLOCK_ID:
            	if(metadata >= 8) 
            		metadata-=8;
    			return metadata < 4 ?  null : fail+" 0 and 3 or 8 and 11";
            case TORCH_ID: case REDSTONE_TORCH_OFF_ID: case REDSTONE_TORCH_ON_ID:
            	return metadata > 0 && metadata <7 ? null : fail+" 1 and 6";
        }
        return null;
    }
    //TODO should use real blockID instead
    public final static int STONE_ID=1, GRASS_ID=2,DIRT_ID=3,COBBLESTONE_ID=4,WOOD_ID=5,
    		WATER_ID=8,STATIONARY_WATER_ID=9,LAVA_ID=10,
    		STATIONARY_LAVA_ID=11,SAND_ID=12,GRAVEL_ID=13,
    		COAL_ORE_ID=16,LOG_ID=17,GLASS_ID=20,
    		DISPENSER_ID=23,SANDSTONE_ID=24,
    		BED_BLOCK_ID=26,POWERED_RAIL_ID=27,DETECTOR_RAIL_ID=28,STICKY_PISTON_ID=29,
    		WEB_ID=30,LONG_GRASS_ID=31,DEAD_BUSH_ID=32,PISTON_ID=33,PISTON_EXTENSION_ID=34,
    		DOUBLE_STEP_ID=43,
    		STEP_ID=44,BRICK_ID=45,TNT_ID=46,BOOKSHELF_ID=47,MOSSY_COBBLESTONE_ID=48,
    		OBSIDIAN_ID=49,TORCH_ID=50,FIRE_ID=51,MOB_SPAWNER_ID=52,WOOD_STAIRS_ID=53,
    		CHEST_ID=54,REDSTONE_WIRE_ID=55,FURNACE_ID=61,BURNING_FURNACE_ID=62,
    		SIGN_POST_ID=63,WOODEN_DOOR_ID=64,LADDER_ID=65,RAILS_ID=66,COBBLESTONE_STAIRS_ID=67,
    		WALL_SIGN_ID=68,LEVER_ID=69,STONE_PLATE_ID=70,WOOD_PLATE_ID=72,
    		REDSTONE_ORE_ID=73,GLOWING_REDSTONE_ORE_ID=74,REDSTONE_TORCH_OFF_ID=75,
    		REDSTONE_TORCH_ON_ID=76,STONE_BUTTON_ID=77,SNOW_ID=78,ICE_ID=79,
    		CACTUS_ID=81,CLAY_ID=82,SUGAR_CANE_BLOCK_ID=83,FENCE_ID=85,
    		PUMPKIN_ID=86,NETHERRACK_ID=87,SOUL_SAND_ID=88,GLOWSTONE_ID=89,PORTAL_ID=90,
    		JACK_O_LANTERN_ID=91,DIODE_BLOCK_OFF_ID=93,DIODE_BLOCK_ON_ID=94;
    public final static int LOCKED_CHEST_ID=95,TRAP_DOOR_ID=96,SILVERFISH_BLOCK_ID=97,
    		STONE_BRICK_ID=98,
    		IRON_BARS_ID=101,GLASS_PANE_ID=102,PUMPKIN_STEM_ID=104,
    		MELON_STEM_ID=105,VINES_ID=106,FENCE_GATE_ID=107,BRICK_STAIRS_ID=108,
    		STONE_BRICK_STAIRS_ID=109,MYCELIUM_ID=110,NETHER_BRICK_ID=112,
    		NETHER_BRICK_FENCE_ID=113,NETHER_BRICK_STAIRS_ID=114,NETHER_WART_ID=115,
    		ENCHANTMENT_TABLE_ID=116,BREWING_STAND_BLOCK_ID=117,CAULDRON_BLOCK_ID=118,
    		END_PORTAL_ID=119,END_PORTAL_FRAME_ID=120,END_STONE_ID=121,DRAGON_EGG_ID=122,
    		REDSTONE_LAMP_OFF_ID=123,REDSTONE_LAMP_ON_ID=124,WOOD_DOUBLE_SLAB_ID=125,
    		WOOD_SLAB_ID=126,SAND_STAIRS_ID=128,TRIPWIRE_SOURCE_ID=131,TRIPWIRE_ID=132;
    public final static int SPRUCE_STAIRS_ID=134,BIRCH_STAIRS_ID=135,JUNGLE_STAIRS_ID=136,
    		WOOD_BUTTON_ID=143,CHEST_TRAP_ID=146,COMPARATOR_ID=149,
    		LIGHT_DETECTOR_ID=151,HOPPER_ID=154,QUARTZ_ID=155,QUARTZ_STAIRS_ID=156,
    		ACTIVATORRAIL_ID=157,DROPPER_ID=158;
   
    //Special Blocks
    public final static int SPECIAL_BLOCKID_START=300,
    		PRESERVE_ID=300,ZOMBIE_SPAWNER_ID=301, SKELETON_SPAWNER_ID=302,
    		SPIDER_SPAWNER_ID=303,CREEPER_SPAWNER_ID=304,UPRIGHT_SPAWNER_ID=305,
    		EASY_SPAWNER_ID=306,MEDIUM_SPAWNER_ID=307,HARD_SPAWNER_ID=308,EASY_CHEST_ID=309,
    		MEDIUM_CHEST_ID=310,HARD_CHEST_ID=311,TOWER_CHEST_ID=312,PIG_ZOMBIE_SPAWNER_ID=313,
    		ENDERMAN_SPAWNER_ID=314,CAVE_SPIDER_SPAWNER_ID=315,GHAST_SPAWNER_ID=316,
    		WALL_STAIR_ID=319,PAINTING_SPECIAL_ID=320,BLAZE_SPAWNER_ID=327,
    		SLIME_SPAWNER_ID=328,LAVA_SLIME_SPAWNER_ID=329,VILLAGER_SPAWNER_ID=330,
    		SNOW_GOLEM_SPAWNER_ID=331,MUSHROOM_COW_SPAWNER_ID=332,SHEEP_SPAWNER_ID=333,
    		COW_SPAWNER_ID=334,CHICKEN_SPAWNER_ID=335,SQUID_SPAWNER_ID=336,WOLF_SPAWNER_ID=337,
    		GIANT_ZOMBIE_SPAWNER_ID=338,SILVERFISH_SPAWNER_ID=339,DRAGON_SPAWNER_ID=340,
    		OCELOT_SPAWNER_ID=341,IRON_GOLEM_SPAWNER_ID=342,WITHERBOSS_SPAWNER_ID=343,
    		BAT_SPAWNER_ID=344,WITCH_SPAWNER_ID=345;
    
    //maps block metadata to a dir
    public final static int[]       BED_META_TO_DIR=new int[]       {       DIR_SOUTH,DIR_WEST,DIR_NORTH,DIR_EAST},
    								BUTTON_META_TO_DIR=new int[] 	{0, DIR_EAST,DIR_WEST,DIR_SOUTH,DIR_NORTH},
            						STAIRS_META_TO_DIR=new int[]    {       DIR_EAST,DIR_WEST,DIR_SOUTH,DIR_NORTH},
                                    LADDER_META_TO_DIR=new int[]    {0,0,   DIR_NORTH,DIR_SOUTH,DIR_WEST,DIR_EAST},
                                    TRAPDOOR_META_TO_DIR=new int[]  {       DIR_SOUTH,DIR_NORTH,DIR_EAST,DIR_WEST},
                                    VINES_META_TO_DIR=new int[]     {0,     DIR_SOUTH,DIR_WEST,0,DIR_NORTH,0,0,0,DIR_EAST},
                                    DOOR_META_TO_DIR=new int[]      {       DIR_WEST,DIR_NORTH,DIR_EAST,DIR_SOUTH};
   
    //inverse map should be {North_inv,East_inv,dummy,West_inv,South_inv}
    //inverse map should be {North_inv,East_inv,South_inv, West_inv}
    public final static int[]       BED_DIR_TO_META         		=new int[]{2,3,0,1},
                                    BUTTON_DIR_TO_META              =new int[]{4,1,3,2},
                                    STAIRS_DIR_TO_META              =new int[]{3,0,2,1},
                                    LADDER_DIR_TO_META              =new int[]{2,5,3,4},
                                    TRAPDOOR_DIR_TO_META    		=new int[]{1,2,0,3},
                                    VINES_DIR_TO_META               =new int[]{4,8,1,2},
                                    DOOR_DIR_TO_META                =new int[]{3,0,1,2},
                                    PAINTING_DIR_TO_FACEDIR 		=new int[]{0,3,2,1};
   
    public final static int[] DIR_TO_I=new int[]{ 0,1,0,-1},DIR_TO_K=new int[]{-1,0,1, 0};
   
    //for use in local orientation
    public final static int[] DIR_TO_X=new int[]{0,1,0,-1},DIR_TO_Y=new int[]{1,0,-1,0};
   
     //some prebuilt directional blocks
    public final static int[] WEST_FACE_TORCH_BLOCK=new int[]{TORCH_ID,BUTTON_DIR_TO_META[DIR_WEST]},
                              EAST_FACE_TORCH_BLOCK=new int[]{TORCH_ID,BUTTON_DIR_TO_META[DIR_EAST]},
                              NORTH_FACE_TORCH_BLOCK=new int[]{TORCH_ID,BUTTON_DIR_TO_META[DIR_NORTH]},
                              SOUTH_FACE_TORCH_BLOCK=new int[]{TORCH_ID,BUTTON_DIR_TO_META[DIR_SOUTH]},
                              EAST_FACE_LADDER_BLOCK=new int[]{LADDER_ID,LADDER_DIR_TO_META[DIR_EAST]},
                              HOLE_BLOCK_LIGHTING=new int[]{0,0},
                              HOLE_BLOCK_NO_LIGHTING=new int[]{0,1},
                              PRESERVE_BLOCK=new int[]{PRESERVE_ID,0},
                              HARD_SPAWNER_BLOCK=new int[]{HARD_SPAWNER_ID,0},
                              PIG_ZOMBIE_SPAWNER_BLOCK=new int[]{PIG_ZOMBIE_SPAWNER_ID,0},
                              ENDERMAN_SPAWNER_BLOCK=new int[]{ENDERMAN_SPAWNER_ID,0},
                              TOWER_CHEST_BLOCK=new int[]{TOWER_CHEST_ID,0},
                              HARD_CHEST_BLOCK=new int[]{HARD_CHEST_ID,0},
                              PORTAL_BLOCK=new int[]{PORTAL_ID,0};
           
   
    protected static boolean[]  IS_ARTIFICAL_BLOCK=new boolean[4096],
    							IS_WALLABLE=new boolean[4096],
    							IS_DELAY_BLOCK=new boolean[4096],
    							IS_LOAD_TRASMITER_BLOCK=new boolean[4096],
    							IS_WATER_BLOCK=new boolean[4096],
    							IS_FLOWING_BLOCK=new boolean[4096],
    							IS_ORE_BLOCK=new boolean[4096],
    							IS_STAIRS_BLOCK=new boolean[4096],
    							IS_DOOR_BLOCK=new boolean[4096];

    static{
        for(int blockID=0;blockID<IS_ARTIFICAL_BLOCK.length;blockID++){
        	Block block = Block.blocksList[blockID];
        	if(block!=null)
        	{
        		IS_STAIRS_BLOCK[blockID]= block instanceof BlockStairs;
        		IS_DOOR_BLOCK[blockID]= block instanceof BlockDoor;   
                //note lava is considered to NOT be a liquid, and is therefore not wallable. This is so we can build cities on the lava surface.
                IS_WATER_BLOCK[blockID]= blockID==WATER_ID || blockID==STATIONARY_WATER_ID || blockID==ICE_ID;
               
                IS_FLOWING_BLOCK[blockID]=block instanceof BlockFlowing || IS_WATER_BLOCK[blockID] || blockID==STATIONARY_LAVA_ID || blockID==LAVA_ID || block instanceof BlockSand || block instanceof BlockGravel;
               
                IS_WALLABLE[blockID]= IS_WATER_BLOCK[blockID]
                   || block instanceof BlockLog || block instanceof BlockWeb                
                   || block instanceof BlockSnow|| block instanceof BlockPumpkin 
                   || block instanceof BlockMelon|| block instanceof IShearable
                   || block instanceof BlockMushroomCap || block instanceof IPlantable;
             
                IS_ORE_BLOCK[blockID]= blockID==REDSTONE_ORE_ID|| blockID==CLAY_ID || block instanceof BlockOre;
               
                //Define by what it is not. Not IS_WALLABLE and not a naturally occurring solid block (obsidian/bedrock are exceptions)
                IS_ARTIFICAL_BLOCK[blockID]= !( IS_WALLABLE[blockID] || IS_ORE_BLOCK[blockID]
                   || blockID==STONE_ID || block instanceof BlockDirt || block instanceof BlockGrass 
                   || block instanceof BlockGravel || block instanceof BlockSand || block instanceof BlockNetherrack
                   || block instanceof BlockSoulSand || block instanceof BlockMycelium);
               
                IS_DELAY_BLOCK[blockID]=IS_STAIRS_BLOCK[blockID] || IS_FLOWING_BLOCK[blockID]
                   || block instanceof BlockTorch || block instanceof BlockLever || block instanceof BlockSign 
                   || block instanceof BlockFire || block instanceof BlockButton || block instanceof BlockGlowStone 
                   || block instanceof BlockVine || block instanceof BlockRedstoneWire
                   || block instanceof BlockDispenser || block instanceof BlockFurnace;
               
                //Define by what it is not.
                IS_LOAD_TRASMITER_BLOCK[blockID]= !(IS_WALLABLE[blockID]  || IS_FLOWING_BLOCK[blockID]
                     || block instanceof BlockTorch || blockID==LADDER_ID);
        	} 
        	else{
        		IS_WALLABLE[blockID]= blockID==0;
        		IS_LOAD_TRASMITER_BLOCK[blockID]= !(IS_WALLABLE[blockID]|| blockID==PRESERVE_ID );
        	}
        }
    }
   
    private final static boolean isSolidBlock(int blockID){
        return blockID!=0 && Block.blocksList[blockID].blockMaterial.isSolid();
    }
   
    protected final static int[] STEP_TO_STAIRS={STONE_BRICK_STAIRS_ID,SAND_STAIRS_ID,WOOD_STAIRS_ID,COBBLESTONE_STAIRS_ID,BRICK_STAIRS_ID,STONE_BRICK_STAIRS_ID,NETHER_BRICK_STAIRS_ID,QUARTZ_STAIRS_ID };
   
    protected final static int[] blockToStepMeta(int[] idAndMeta){
        if(!IS_ARTIFICAL_BLOCK[idAndMeta[0]]) return new int[]{idAndMeta[0],idAndMeta[1]};
        switch(idAndMeta[0]){
            case SANDSTONE_ID:                      return new int[]{STEP_ID,1};
            case WOOD_ID:							return new int[]{STEP_ID,2};
            case COBBLESTONE_ID:                    return new int[]{STEP_ID,3};
            case BRICK_ID:                          return new int[]{STEP_ID,4};
            case STONE_BRICK_ID:                    return new int[]{STEP_ID,5};
            case NETHER_BRICK_ID:					return new int[]{STEP_ID,6};
            case QUARTZ_ID:							return new int[]{STEP_ID,7};
            case STEP_ID: case DOUBLE_STEP_ID:case WOOD_DOUBLE_SLAB_ID:
            	case WOOD_SLAB_ID:
            										return new int[]{STEP_ID,idAndMeta[1]};
            	default:							return new int[]{idAndMeta[0],0};
        }
    }
   
    protected final static int stairToSolidBlock(int blockID){
        switch(blockID){
                case COBBLESTONE_STAIRS_ID: return COBBLESTONE_ID;
                case WOOD_STAIRS_ID:        return WOOD_ID;
                case BRICK_STAIRS_ID:       return BRICK_ID;
                case STONE_BRICK_STAIRS_ID: return STONE_BRICK_ID;
                case NETHER_BRICK_STAIRS_ID:return NETHER_BRICK_ID;
                case SAND_STAIRS_ID:        return SANDSTONE_ID;
                case QUARTZ_STAIRS_ID:		return QUARTZ_ID;
                default:                    return blockID;
        }
    }
   
    protected final static int blockToStairs(int[] idAndMeta){        
    	switch(idAndMeta[0]){
            case COBBLESTONE_ID: case MOSSY_COBBLESTONE_ID: return COBBLESTONE_STAIRS_ID;
            case NETHER_BRICK_ID:                           return NETHER_BRICK_STAIRS_ID;
            case STONE_BRICK_ID: case STONE_ID:             return STONE_BRICK_STAIRS_ID;                  
            case BRICK_ID:                                  return BRICK_STAIRS_ID;
            case SANDSTONE_ID:                              return SAND_STAIRS_ID;
            case QUARTZ_ID:									return QUARTZ_STAIRS_ID;
            case LOG_ID:
            	int tempdata=idAndMeta[1];
            	while (tempdata>=4)
            	{
            		tempdata-=4;
                }
            	switch(tempdata)
            	{
	            	case 0: return WOOD_STAIRS_ID;
	            	case 1: return SPRUCE_STAIRS_ID;
	            	case 2: return BIRCH_STAIRS_ID;
	            	case 3: return JUNGLE_STAIRS_ID;
            	}
            default:                         				return idAndMeta[0];               
        }
    }
   
    private final static void circleShape(int diam){
        float rad=(float)diam/2.0F;
        float[][] shape_density=new float[diam][diam];
        for(int x=0;x<diam;x++)
            for(int y=0;y<diam;y++)
                    shape_density[y][x]=(((float)x+0.5F-rad)*((float)x+0.5F-rad) + ((float)y+0.5F-rad)*((float)y+0.5F-rad))/(rad*rad);
   
        int[] xheight=new int[diam];
        for(int y=0; y<diam; y++){
            int x=0;
            for(; shape_density[y][x]>1.0F; x++){}
            xheight[y]=x;
        }
           
        CIRCLE_SHAPE[diam]=new int[diam][diam];
        CIRCLE_CRENEL[diam]=new int[diam][diam];
        SPHERE_SHAPE[diam]=new int[(diam+1)/2];
        int nextHeight=0,crenel_adj=0;
        for(int x=0;x<diam;x++) 
        	for(int y=0;y<diam;y++) { 
        		CIRCLE_SHAPE[diam][y][x]=0; 
        		CIRCLE_CRENEL[diam][y][x]=0; }
        for(int y=0; y<diam; y++){
                if(y==0 || y==diam-1) 
                	nextHeight = diam/2+1;
                else 
                	nextHeight = xheight[y<diam/2 ? y-1:y+1] + (xheight[y]==xheight[y<diam/2 ? y-1:y+1] ? 1:0);
                if(y>0 && xheight[y]==xheight[y-1]) 
                	crenel_adj++;
               
                int x=0;
                for(;x<xheight[y];x++) {
                    CIRCLE_SHAPE[diam][y][x]=-1;
                    CIRCLE_SHAPE[diam][y][diam-x-1]=-1;
                    CIRCLE_CRENEL[diam][y][x]=-1;
                    CIRCLE_CRENEL[diam][y][diam-x-1]=-1;
                }
                for(; x < nextHeight ; x++) {
                    CIRCLE_SHAPE[diam][y][x]=1;
                    CIRCLE_SHAPE[diam][y][diam-x-1]=1;
                    CIRCLE_CRENEL[diam][y][x]=(x+crenel_adj)%2;
                    CIRCLE_CRENEL[diam][y][diam-x-1]=(x+crenel_adj+diam+1)%2;
                }
        }
        for(int y=diam/2;y<diam;y++)
            SPHERE_SHAPE[diam][y-diam/2]=(2*(diam/2-xheight[y])+(diam%2==0 ? 0:1) );
    }
       
    public final static int MAX_SPHERE_DIAM=40;
    public final static int[][] SPHERE_SHAPE=new int[MAX_SPHERE_DIAM+1][];
    public final static int[][][] CIRCLE_SHAPE=new int[MAX_SPHERE_DIAM+1][][],
    		CIRCLE_CRENEL=new int[MAX_SPHERE_DIAM+1][][];
    static{
            for(int diam=1; diam<=MAX_SPHERE_DIAM; diam++){
                    circleShape(diam);
            }
            //change diam 6 shape to look better
            CIRCLE_SHAPE[6]=new int[][]{{-1,-1, 1, 1,-1,-1},
                                                                    {-1, 1, 0, 0, 1,-1},
                                                                    { 1, 0, 0, 0, 0, 1},
                                                                    { 1, 0, 0, 0, 0, 1},
                                                                    {-1, 1, 0, 0, 1,-1},
                                                                    {-1,-1, 1, 1,-1,-1}};
            CIRCLE_CRENEL[6]=new int[][]{{-1,-1, 1, 0,-1,-1},
                                                                    {-1, 0, 0, 0, 1,-1},
                                                                    { 1, 0, 0, 0, 0, 0},
                                                                    { 0, 0, 0, 0, 0, 1},
                                                                    {-1, 1, 0, 0, 0,-1},
                                                                    {-1,-1, 0, 1,-1,-1}};
    }
 
    private final static int LIGHTING_INVERSE_DENSITY=10;
    private final static boolean[] randLightingHash=new boolean[512];
    static{
            Random rand=new Random();
            for(int m=0; m<randLightingHash.length; m++)
                    randLightingHash[m]=rand.nextInt(LIGHTING_INVERSE_DENSITY)==0;
    }
      
    public static String[] BIOME_NAMES=new String[BiomeGenBase.biomeList.length+1];
    static{
    	BIOME_NAMES[0]="Underground";      	
      for (int i = 0; i < BIOME_NAMES.length-1; i++)
        {
    	  if (BiomeGenBase.biomeList[i]!=null)  	  
          BIOME_NAMES[i+1]=BiomeGenBase.biomeList[i].biomeName;   	  
        }
    }
        
    //TODO:Extend chest_type_labels as config option
    public static String[] CHEST_TYPE_LABELS=new String[]{"CHEST_EASY","CHEST_MEDIUM","CHEST_HARD","CHEST_TOWER"};
    public static List<String>CHEST_LABELS=new ArrayList <String> ();
    public static int[] DEFAULT_CHEST_TRIES=new int[]{4,6,6,6};
    //chest items[n] format is array of 6 arrays
    //0array - idx
    //1array - blockId
    //2array - block damage/meta
    //3array - block weight
    //4array - block min stacksize
    //5array block max stacksize
    public static int[][][] DEFAULT_CHEST_ITEMS=new int[][][]{
                {        //Easy
                        {0,Item.arrow.itemID,0,2,1,12},  
                        {1,Item.swordIron.itemID,0,2,1,1},
                        {2,Item.legsLeather.itemID,0,1,1,1},
                        {3,Item.shovelIron.itemID,0,1,1,1},
                        {4,Item.silk.itemID,0,1,1,1},
                        {5,Item.pickaxeIron.itemID,0,2,1,1},
                        {6,Item.bootsLeather.itemID,0,1,1,1},
                        {7,Item.bucketEmpty.itemID,0,1,1,1},
                        {8,Item.helmetLeather.itemID,0,1,1,1},
                        {9,Item.seeds.itemID,0,1,10,15},
                        {10,Item.goldNugget.itemID,0,2,3,8},
                        {11,Item.potion.itemID,5,2,1,1}, //healing I
                        {12,Item.potion.itemID,4,1,1,1}}, //poison, hehe
                       
                {       //Medium
                        {0,Item.swordGold.itemID,0,2,1,1},
                        {1,Item.bucketMilk.itemID,0,2,1,1},
                        {2,WEB_ID,0,1,8,16},
                        {3,Item.shovelGold.itemID,0,1,1,1},
                        {4,Item.hoeGold.itemID,0,1,0,1},
                        {5,Item.pocketSundial.itemID,0,1,1,1},
                        {6,Item.axeIron.itemID,0,3,1,1},
                        {7,Item.map.itemID,0,1,1,1},
                        {8,Item.appleRed.itemID,0,2,2,3},
                        {9,Item.compass.itemID,0,1,1,1},
                        {10,Item.ingotIron.itemID,0,1,5,8},
                        {11,Item.slimeBall.itemID,0,1,1,3},
                        {12,OBSIDIAN_ID,0,1,1,4},
                        {13,Item.bread.itemID,0,2,8,15},
                        {14,Item.potion.itemID,2,1,1,1},
                        {15,Item.potion.itemID,37,3,1,1}, //healing II
                        {16,Item.potion.itemID,34,1,1,1}, //swiftness II
                        {17,Item.potion.itemID,9,1,1,1}}, //strength
                       
                {       //Hard
                        {0,STICKY_PISTON_ID,0,2,6,12},  
                        {1,WEB_ID,0,1,8,24},
                        {2,Item.cookie.itemID,0,2,8,18},
                        {3,Item.axeDiamond.itemID,0,1,1,1},
                        {4,Item.minecartEmpty.itemID,0,1,12,24},
                        {5,Item.redstone.itemID,0,2,12,24},
                        {6,Item.bucketLava.itemID,0,2,1,1},
                        {7,Item.enderPearl.itemID,0,1,1,1},
                        {8,MOB_SPAWNER_ID,0,1,2,4},
                        {9,Item.record13.itemID,0,1,1,1},
                        {10,Item.appleGold.itemID,0,1,4,8},
                        {11,TNT_ID,0,2,8,20},
                        {12,Item.diamond.itemID,0,2,1,4},
                        {13,Item.ingotGold.itemID,0,2,30,64},
                        {14,Item.potion.itemID,37,3,1,1}, //healing II
                        {15,Item.potion.itemID,49,2,1,1}, //regeneration II
                        {16,Item.potion.itemID,3,2,1,1}}, //fire resistance
                             
                {       //Tower
                        {0,Item.arrow.itemID,0,1,1,12},  
                        {1,Item.fishRaw.itemID,0,2,1,1},
                        {2,Item.helmetGold.itemID,0,1,1,1},
                        {3,WEB_ID,0,1,1,12},
                        {4,Item.ingotIron.itemID,0,1,2,3},
                        {5,Item.swordStone.itemID,0,1,1,1},
                        {6,Item.axeIron.itemID,0,1,1,1},
                        {7,Item.egg.itemID,0,2,8,16},
                        {8,Item.saddle.itemID,0,1,1,1},
                        {9,Item.wheat.itemID,0,2,3,6},
                        {10,Item.gunpowder.itemID,0,1,2,4},
                        {11,Item.plateLeather.itemID,0,1,1,1},
                        {12,PUMPKIN_ID,0,1,1,5},
                        {13,Item.goldNugget.itemID,0,2,1,3}}

        };
}

