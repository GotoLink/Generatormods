package mods.generator;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
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
 * BuildingCellularAutomaton creates double-ended walls
 */
public class BuildingCellularAutomaton extends Building {
	private final static byte DEAD=0,ALIVE=1;
	private final float MEAN_SIDE_LENGTH_PER_POPULATE=15.0f;
	private final static int HOLE_FLOOR_BUFFER=2,UNREACHED=-1;
	private final static int SYMMETRIC_SEED_MIN_WIDTH=4,CIRCULAR_SEED_MIN_WIDTH=4;
	public final static TemplateRule 	DEFAULT_MEDIUM_LIGHT_NARROW_SPAWNER_RULE =  new TemplateRule(new int[]{BLAZE_SPAWNER_ID,BLAZE_SPAWNER_ID,BLAZE_SPAWNER_ID,SILVERFISH_SPAWNER_ID,SILVERFISH_SPAWNER_ID,LAVA_SLIME_SPAWNER_ID}),
										DEFAULT_MEDIUM_LIGHT_WIDE_SPAWNER_RULE=		new TemplateRule(new int[]{BLAZE_SPAWNER_ID,SILVERFISH_SPAWNER_ID,SILVERFISH_SPAWNER_ID,CAVE_SPIDER_SPAWNER_ID,CAVE_SPIDER_SPAWNER_ID,SPIDER_SPAWNER_ID}),
										DEFAULT_LOW_LIGHT_SPAWNER_RULE =       		new TemplateRule(new int[]{UPRIGHT_SPAWNER_ID,UPRIGHT_SPAWNER_ID,SILVERFISH_SPAWNER_ID,LAVA_SLIME_SPAWNER_ID,CAVE_SPIDER_SPAWNER_ID});
	
	private byte[][][] layers = null;
	public byte[][] seed=null;
	private byte[][] caRule=null;
	private TemplateRule lowLightSpawnerRule, mediumLightNarrowSpawnerRule,mediumLightWideSpawnerRule;
	int[][] fBB;
	int zGround=0;
	
	
	public BuildingCellularAutomaton(WorldGeneratorThread wgt_,TemplateRule bRule_,int bDir_,int axXHand_, boolean centerAligned_,int width, int height, int length, byte[][] seed_, byte[][] caRule_, TemplateRule[] spawnerRules, int[] sourcePt){
		super(0,wgt_, bRule_, bDir_,axXHand_,centerAligned_,new int[]{width,height,length},sourcePt);
		seed=seed_;
		if((bWidth - seed.length)%2 !=0 ) bWidth++; //so seed can be perfectly centered
		if((bLength - seed[0].length)%2 !=0 ) bLength++;
		caRule=caRule_;
		mediumLightNarrowSpawnerRule= spawnerRules!=null ? spawnerRules[0] : DEFAULT_MEDIUM_LIGHT_NARROW_SPAWNER_RULE;
		mediumLightWideSpawnerRule=   spawnerRules!=null ? spawnerRules[1] : DEFAULT_MEDIUM_LIGHT_WIDE_SPAWNER_RULE;
		lowLightSpawnerRule= 		  spawnerRules!=null ? spawnerRules[2] : DEFAULT_LOW_LIGHT_SPAWNER_RULE;
		
	}
	
	//unlike other Buildings, this should be called after plan()
	public boolean queryCanBuild( int ybuffer, boolean nonLayoutFrameCheck) throws InterruptedException{
		if(!( queryExplorationHandlerForChunk(0,bLength-1) && queryExplorationHandlerForChunk(bWidth-1,0) && queryExplorationHandlerForChunk(bWidth-1,bLength-1) )){
			return false;
		}
		
		int layoutCode= bWidth*bLength > 120 ? WorldGeneratorThread.LAYOUT_CODE_TOWER : WorldGeneratorThread.LAYOUT_CODE_TEMPLATE;
    	if(wgt.isLayoutGenerator()){
	    	if(wgt.layoutIsClear(getIJKPt(0,0,ybuffer),getIJKPt(bWidth-1,0,bLength-1),layoutCode))
	    		wgt.setLayoutCode(getIJKPt(0,0,ybuffer),getIJKPt(bWidth-1,0,bLength-1),layoutCode);
	    	else 
	    		return false;
    	}else if(nonLayoutFrameCheck){
    		if(isObstructedFrame(0,ybuffer)) 
    			return false;
    	}
		return true;
	}
	
	public boolean plan(boolean bury, int MinHeightBeforeOscillation){
		//layers is z-flipped from usual orientation so z=0 is the top
		layers=new byte[bHeight][bWidth][bLength];
		for(int z=0; z<bHeight; z++) for(int x=0; x<bWidth; x++) for(int y=0; y<bLength; y++)
			layers[z][x][y]=DEAD;
		
		int[][] BB=new int[4][bHeight];
		BB[0][0]=(bWidth-seed.length)/2;
		BB[1][0]=(bWidth-seed.length)/2+seed.length-1;
		BB[2][0]=(bLength-seed[0].length)/2;
		BB[3][0]=(bLength-seed[0].length)/2+seed[0].length-1;
		
		for(int x=0; x<seed.length; x++) for(int y=0; y<seed[0].length; y++)
			layers[0][BB[0][0]+x][BB[2][0]+y]=seed[x][y];
		
		int crystallizationHeight=UNREACHED;
		
		for(int z=1; z<bHeight; z++){
			boolean layerIsAlive=false;
			boolean layerIsFixed=crystallizationHeight==UNREACHED && z>=1;
			boolean layerIsPeriod2=crystallizationHeight==UNREACHED && z>=2;
			boolean layerIsPeriod3=crystallizationHeight==UNREACHED && z>=3;
			BB[0][z]=bWidth/2;
			BB[1][z]=bWidth/2;
			BB[2][z]=bLength/2;
			BB[3][z]=bLength/2;
			
			for(int x=Math.max(0,BB[0][z-1]-1); x<=Math.min(bWidth-1,BB[1][z-1]+1); x++){
				for(int y=Math.max(0,BB[2][z-1]-1); y<=Math.min(bLength-1,BB[3][z-1]+1); y++){
					//try the 8 neighboring points in previous layer
					int neighbors=0;
					for(int x1=Math.max(x-1,0); x1<=Math.min(x+1,bWidth-1); x1++)
						for(int y1=Math.max(y-1,0); y1<=Math.min(y+1,bLength-1); y1++)
							if(!(x1==x && y1==y))
								neighbors+=layers[z-1][x1][y1];
					
					//update this layer based on the rule
					layers[z][x][y]=caRule[layers[z-1][x][y]][neighbors];
				
					//culling checks and update bounding box
					if(layers[z][x][y]==ALIVE){
						if(x<BB[0][z]) BB[0][z]=x;
						if(x>BB[1][z]) BB[1][z]=x;
						if(y<BB[2][z]) BB[2][z]=y;
						if(y>BB[3][z]) BB[3][z]=y;
						layerIsAlive=true;
					}
					if(layers[z][x][y]!=layers[z-1][x][y]) layerIsFixed=false;
					if(z>=2 && layers[z][x][y]!=layers[z-2][x][y]) layerIsPeriod2=false;
					if(z>=3 && layers[z][x][y]!=layers[z-3][x][y]) layerIsPeriod3=false;
				
			}}
			
			if(!layerIsAlive){
				if(z<=MinHeightBeforeOscillation) return false;
				bHeight=z;
				break;
			}
			if(layerIsFixed){
				if(z-1<=MinHeightBeforeOscillation) return false;
				crystallizationHeight=z-1;
			}
			if(layerIsPeriod2){
				if(z-2<=MinHeightBeforeOscillation) return false;
				crystallizationHeight=z-2;
			}
			if(layerIsPeriod3){
				if(z-3<=MinHeightBeforeOscillation) return false;
				crystallizationHeight=z-3;
			}
			
			if(crystallizationHeight>UNREACHED && z>2*crystallizationHeight){
				bHeight=z;
				break;
			}
			
		}
		
		//prune top layer
		int topLayerCount=0, secondLayerCount=0;
		for(int x=0; x<bWidth; x++){ for(int y=0; y<bLength; y++){
			topLayerCount+=layers[0][x][y];
			secondLayerCount+=layers[1][x][y];
		}}
		if(2*topLayerCount >= 3*secondLayerCount){
			for(int x=0; x<bWidth; x++){ for(int y=0; y<bLength; y++){
				if(layers[0][x][y]==ALIVE && layers[1][x][y]==DEAD)
					layers[0][x][y]=DEAD;
			}}
		}
		
		//now resize building dimensions and shift down
		int minX=minOrMax(BB[0],true), maxX=minOrMax(BB[1],false),
			minY=minOrMax(BB[2],true), maxY=minOrMax(BB[3],false);
		bWidth=maxX-minX+1;
		bLength=maxY-minY+1;
		if(!shiftBuidlingJDown(15)) //do a height check to see we are not at the edge of a cliff etc.
			return false;
		boolean hitWater=false;
		if(caRule[0][2]!=ALIVE){ //if not a 2-rule
			int[] heights=new int[]{findSurfaceJ(world, getI(bWidth-1,0), getK(bWidth-1,0),j0+10,false,0),
				    findSurfaceJ(world, getI(0,bLength-1), getK(0,bLength-1),j0+10,false,0),
				    findSurfaceJ(world, getI(bWidth-1,bLength-1), getK(bWidth-1,bLength-1),j0+10,false,0),
				    findSurfaceJ(world, getI(bWidth/2,bLength/2), getK(bWidth/2,bLength/2),j0+10,false,0)};
			for(int height : heights) hitWater |= height==HIT_WATER;
		}
		
		if(j0+bHeight>WORLD_MAX_Y-1) j0=WORLD_MAX_Y-bHeight-1; //stay 1 below top to avoid lighting problems
		if(bury && !hitWater){
			zGround= caRule[0][2]==ALIVE ? Math.max(0,bHeight-bWidth/3-random.nextInt(bWidth))  
										 : random.nextInt(3*bHeight/4);
			if(j0-zGround < 5) zGround=j0-5;
			j0-=zGround; //make ruin partially buried
		}
		
		//shift level and floor arrays
		byte[][][] layers2=new byte[bHeight][bWidth][bLength]; //shrunk in all 3 dimensions
		fBB=new int[4][bHeight];
		for(int z=0; z<bHeight; z++){
			int lZ=bHeight-z-1;
			for(int x=0; x<bWidth; x++){ for(int y=0; y<bLength; y++){
				layers2[z][x][y]=layers[lZ][x+minX][y+minY];
			}}
			//floor bounding box
			fBB[0][z]=BB[0][lZ] - minX + (BB[1][lZ] - BB[0][lZ])/4;
			fBB[1][z]=BB[1][lZ] - minX - (BB[1][lZ] - BB[0][lZ])/4;
			fBB[2][z]=BB[2][lZ] - minY + (BB[3][lZ] - BB[2][lZ])/4;
			fBB[3][z]=BB[3][lZ] - minY - (BB[3][lZ] - BB[2][lZ])/4;
		}
		layers=layers2;		
		
		return true;
	}
		
	public void build(boolean SmoothWithStairs,boolean makeFloors) throws InterruptedException{
		int stairsBlock=SmoothWithStairs ? blockToStairs(bRule.primaryBlock) : 0;
		TemplateRule[] stairs=new TemplateRule[]{ new TemplateRule(new int[]{stairsBlock,STAIRS_DIR_TO_META[DIR_NORTH]},bRule.chance),
												  new TemplateRule(new int[]{stairsBlock,STAIRS_DIR_TO_META[DIR_EAST]},bRule.chance),
												  new TemplateRule(new int[]{stairsBlock,STAIRS_DIR_TO_META[DIR_SOUTH]}, bRule.chance),
												  new TemplateRule(new int[]{stairsBlock,STAIRS_DIR_TO_META[DIR_WEST]}, bRule.chance)};
		int[] floorBlockCounts = new int[bHeight];
		ArrayList<ArrayList<int[]>> floorBlocks=new ArrayList<ArrayList<int[]>>();
		for(int m=0; m<bHeight; m++){
			floorBlockCounts[m]=0;
			floorBlocks.add(new ArrayList<int[]>());
		}
		int centerX=(fBB[0][0]+fBB[1][0])/2;
		int[][]holeLimits=new int[bLength][2];
		for(int y=0; y<bLength; y++){ 
			holeLimits[y][0]=centerX; 
			holeLimits[y][1]=centerX+1; 
			}
		
		for(int z=bHeight-1; z>=0; z--){
			//for(int y=0; y<bLength; y++){ 
			//holeLimits[y][0]=centerX; holeLimits[y][1]=centerX+1; }
			for(int x=0; x<bWidth; x++){ 
				for(int y=0; y<bLength; y++){
					//if(fBB[0][z]<=x && x<=fBB[1][z] && fBB[2][z]<=y && y<=fBB[3][z])
					//	setBlockLocal(x,z,y,GLASS_ID);
					if(!queryExplorationHandlerForChunk(x,y))
						break;
					if(layers[z][x][y]==ALIVE)
						setBlockLocal(x,z,y,bRule);			
					else if(z>0 && layers[z-1][x][y]==ALIVE){ //if a floor block
						//if in central core
						if(z<bHeight-5 && fBB[0][z]<=x && x<=fBB[1][z] && fBB[2][z]<=y && y<=fBB[3][z]){ 
							if(makeFloors){
								floorBlocks.get(z).add(new int[]{x,y});
								if(x-HOLE_FLOOR_BUFFER<holeLimits[y][0]) 
									holeLimits[y][0]=Math.max(fBB[0][z], x-HOLE_FLOOR_BUFFER);
								if(x+HOLE_FLOOR_BUFFER>holeLimits[y][1]) 
									holeLimits[y][1]=Math.min(fBB[1][z], x+HOLE_FLOOR_BUFFER);
								floorBlockCounts[z]++;
							}
						}				
						//try smoothing with stairs here
						else if(stairsBlock!=0 && (z==bHeight-1 || layers[z+1][x][y]!=ALIVE)){
							if(y+1<bLength && layers[z][x][y+1]==ALIVE && (	y-1<0 || //y+1 present and (we are at the edge or...	
								(			  layers[z][x][y-1]!=ALIVE //y-1 empty and..
									&& (x+1==bWidth || !(layers[z][x+1][y]!=ALIVE && layers[z][x+1][y-1]==ALIVE)) //not obstructing gaps to the sides
									&& (x-1<0       || !(layers[z][x-1][y]!=ALIVE && layers[z][x-1][y-1]==ALIVE))				 
								) && random.nextInt(100)<bRule.chance)
							)setBlockLocal(x,z,y,stairs[DIR_NORTH]);
							else
							if(y-1>=0      && layers[z][x][y-1]==ALIVE && (	y+1==bLength ||
								(		      layers[z][x][y+1]!=ALIVE 
										&& (x+1==bWidth || !(layers[z][x+1][y]!=ALIVE && layers[z][x+1][y+1]==ALIVE)) 
										&& (x-1<0       || !(layers[z][x-1][y]!=ALIVE && layers[z][x-1][y+1]==ALIVE))				 
								) && random.nextInt(100)<bRule.chance)
							)setBlockLocal(x,z,y,stairs[DIR_SOUTH]);
							
							else
							if(x+1<bWidth && layers[z][x+1][y]==ALIVE && (	x-1<0 || 	
								(			 layers[z][x-1][y]!=ALIVE 
										&& (y+1==bLength|| !(layers[z][x][y+1]!=ALIVE && layers[z][x-1][y+1]==ALIVE))
										&& (y-1<0       || !(layers[z][x][y-1]!=ALIVE && layers[z][x-1][y-1]==ALIVE))				 
								) && random.nextInt(100)<bRule.chance)
							)setBlockLocal(x,z,y,stairs[DIR_EAST]);
							else
							if(x-1>=0     && layers[z][x-1][y]==ALIVE && (	x+1==bWidth ||
								(		     layers[z][x+1][y]!=ALIVE 
										&& (y+1==bLength|| !(layers[z][x][y+1]!=ALIVE && layers[z][x+1][y+1]==ALIVE))
										&& (y-1<0       || !(layers[z][x][y-1]!=ALIVE && layers[z][x+1][y-1]==ALIVE))				 
								) && random.nextInt(100)<bRule.chance)
							)setBlockLocal(x,z,y,stairs[DIR_WEST]);
						}
					}
				}
			}
			
			//now clear a hole surrounding the central floor volume
			for(int y=0; y<bLength; y++)
				for(int x=holeLimits[y][0]+1; x<=holeLimits[y][1]-1; x++)
					if(layers[z][x][y]!=ALIVE && queryExplorationHandlerForChunk(x,y) && !IS_ARTIFICAL_BLOCK[getBlockIdLocal(x,z,y)])
						setBlockLocal(x,z,y,0);
			
			//then gradually taper hole limits...
			if(z%2==0){
				for(int y=0; y<bLength; y++){ 
					holeLimits[y][0] = holeLimits[y][0] < centerX ? (holeLimits[y][0]+1) : centerX; 
					holeLimits[y][1] = holeLimits[y][1] > (centerX+1) ? (holeLimits[y][1]-1) : centerX+1; 
				}		
			}
		}
		
		flushDelayed();
		
		if(makeFloors)
			buildFloors(floorBlockCounts, floorBlocks);
	}
	
	public void buildFloors(int[] floorBlockCounts, ArrayList<ArrayList<int[]>> floorBlocks){
		ArrayList<int[]> floors=new ArrayList<int[]>();
		
		while(true){
			int maxFloorBlocks=floorBlockCounts[1], maxFloor=1;
			for(int floor=2; floor<bHeight-5; floor++){
				if(floorBlockCounts[floor-1]+floorBlockCounts[floor]+floorBlockCounts[floor+1] >maxFloorBlocks){ //add the two floors since we can raise one to the other
					maxFloorBlocks=floorBlockCounts[floor-1]+floorBlockCounts[floor]+floorBlockCounts[floor+1];
					maxFloor=floor;
				}
			}
			if(maxFloorBlocks > 20){
				boolean[][] layout=new boolean[bWidth][bLength];
				for(int x=0; x<bWidth; x++) 
					for(int y=0; y<bLength; y++) 
						layout[x][y]=false;
				for(int[] pt : floorBlocks.get(maxFloor-1)) 
					makeFloorCrossAt(pt[0],maxFloor,pt[1],layout);
				for(int[] pt : floorBlocks.get(maxFloor))   
					makeFloorCrossAt(pt[0],maxFloor,pt[1],layout);
				for(int[] pt : floorBlocks.get(maxFloor+1)) 
					makeFloorCrossAt(pt[0],maxFloor,pt[1],layout);
				
				floors.add(new int[]{maxFloor,maxFloorBlocks});
				
				if(maxFloor-3>=0) 
					floorBlockCounts[maxFloor-3]=0;
				if(maxFloor-2>=0) 
					floorBlockCounts[maxFloor-2]=0;
				floorBlockCounts[maxFloor-1]=0;
				floorBlockCounts[maxFloor]=0;
				floorBlockCounts[maxFloor+1]=0;
				if(maxFloor+2<bHeight) 
					floorBlockCounts[maxFloor+2]=0;
				if(maxFloor+3<bHeight) 
					floorBlockCounts[maxFloor+3]=0;
			}
			else break;
			
			flushDelayed();
		}
		
		//populate
		Collections.sort(floors,new Comparator() {
			public int compare(Object o1, Object o2) {
                int a = ((int[])o1)[0];
                int b = ((int[])o2)[0];
                return a < b ? -1 : a == b ? 0 : 1;
            }
		});
		for(int m=floors.size()-1; m>=0; m--){
			if(m>0) 
				populateLadderOrStairway(floors.get(m-1)[0],floors.get(m)[0],true);
			do{
				populateFloor(floors.get(m)[0],floors.get(m)[1]);
			}while(random.nextFloat() < 1.0f - MEAN_SIDE_LENGTH_PER_POPULATE/MathHelper.sqrt_float((float)floors.get(m)[1]));
		}
	}
	
	private void makeFloorCrossAt(int x, int z, int y, boolean[][] layout){
		makeFloorAt(x,z,y,layout);
		if(x-1 >= fBB[0][z]) 
			makeFloorAt(x-1,z,y,layout);
		if(x+1 <= fBB[1][z]) 
			makeFloorAt(x+1,z,y,layout);
		if(y-1 >= fBB[2][z]) 
			makeFloorAt(x,z,y-1,layout);
		if(y+1 <= fBB[2][z]) 
			makeFloorAt(x,z,y+1,layout);
	}
	
	private void makeFloorAt(int x, int z, int y, boolean[][] layout){
		if(layout[x][y]) return;
		if(IS_ARTIFICAL_BLOCK[getBlockIdLocal(x,z,y)] && IS_ARTIFICAL_BLOCK[getBlockIdLocal(x,z+1,y)]){ //pillar
			if(!IS_ARTIFICAL_BLOCK[getBlockIdLocal(x,z+2,y)])
				setBlockLocal(x,z+2,y,bRule);
			return;
		}
		if(!IS_ARTIFICAL_BLOCK[getBlockIdLocal(x,z-1,y)]){ //raise to floor
			int[] idAndMeta=bRule.getNonAirBlock(world.rand);
			setBlockWithLightingLocal(x,z-1,y,idAndMeta[0],idAndMeta[1],true);
		}
		setBlockWithLightingLocal(x,z,y,0,0,true);
		setBlockWithLightingLocal(x,z+1,y,0,0,true);
		layout[x][y]=true;
	}
	
	private void populateLadderOrStairway(int z1, int z2, boolean buildStairs){
		int fWidth=fBB[1][z2] - fBB[0][z2], fLength=fBB[3][z2] - fBB[2][z2];
		if(fWidth <=0 || fLength <= 0) return;
		
		BuildingSpiralStaircase bss;
		for(int tries=0; tries < 8; tries++){
			int x=random.nextInt(fWidth)+fBB[0][z2],
			    y=random.nextInt(fLength)+fBB[2][z2];
			if(isFloor(x,z2,y)){
				int dir=random.nextInt(4);
				if(buildStairs && (bss=new BuildingSpiralStaircase(wgt, bRule, dir, 1, false, z1-z2+1, getIJKPt(x,z2-1,y))).bottomIsFloor() ){
					bss.build(0,0);
				}else if(isFloor(x,z1,y)){
					//ladder
					for(int z=z1; z<z2; z++){
						setBlockLocal(x+DIR_TO_X[dir],z,y+DIR_TO_Y[dir],bRule);
						setBlockLocal(x,z,y,LADDER_ID,LADDER_DIR_TO_META[flipDir(dir)]);
					}
				}
				return;
			}
		}
	}
	
	private void populateFloor(int z,int floorBlocks){
		TemplateRule spawnerSelection=null;
		int fWidth=fBB[1][z] - fBB[0][z], fLength=fBB[3][z] - fBB[2][z];
		if(fWidth <=0 || fLength <= 0) return;
		
		//spawners
		if(random.nextInt(100)<70){
			for(int tries=0; tries < 8; tries++){
				int x=random.nextInt(fWidth)+fBB[0][z],
				    y=random.nextInt(fLength)+fBB[2][z];
				if(isFloor(x,z,y)){
					int[] pt=getIJKPt(x,z,y);
					int lightVal=world.getSavedLightValue(EnumSkyBlock.Sky, pt[0], pt[1], pt[2]);
					
					//Choose spawner types. There is some kind of bug where where lightVal coming up as zero, even though it is not.
					if(lightVal<5 && !(lightVal==0 && j0+z>Building.SEA_LEVEL)) 
						spawnerSelection=lowLightSpawnerRule;
					else if(lightVal<10)
						spawnerSelection = floorBlocks > 70 ? mediumLightWideSpawnerRule : mediumLightNarrowSpawnerRule ;
						
					if(spawnerSelection!=null){
						setBlockLocal(x,z,y,spawnerSelection);
						break;
					}
				}
			}
		}
		
		//chest
		if(random.nextInt(100) < (spawnerSelection==null ? 20:70)){
			for(int tries=0; tries < 8; tries++){
				int x=random.nextInt(fWidth)+fBB[0][z],
				    y=random.nextInt(fLength)+fBB[2][z];
				if(isFloor(x,z,y)){
					setBlockLocal(x,z-1,y,pickCAChestType(z));
					setBlockLocal(x,z-2,y,bRule);
					if(random.nextBoolean()){
						break; //chance of > 1 chest. Expected # of chests is one.
					}
				}
			}
		}
		
		//1 TNT trap
		int s=random.nextInt(1+random.nextInt(5))-2;
		for(int tries=0; tries<s; tries++){
			int x=random.nextInt(fWidth)+fBB[0][z],
		        y=random.nextInt(fLength)+fBB[2][z];
			if(isFloor(x,z,y)){
				setBlockLocal(x,z,y,STONE_PLATE_ID);
				setBlockLocal(x,z-1,y,TNT_ID);
				setBlockLocal(x,z-2,y,bRule);
			}
		}
		
		//2 TNT trap
		s=spawnerSelection==null ? random.nextInt(1+random.nextInt(7))-2 : 0;
		for(int tries=0; tries<s; tries++){
			int x=random.nextInt(fWidth)+fBB[0][z],
		        y=random.nextInt(fLength)+fBB[2][z];
			if(isFloor(x,z,y) && isFloor(x,z,y=1)){
				for(int x1=x-1;x1<=x+1;x1++) 
					for(int y1=y-1;y1<=y+2;y1++)
						for(int z1=z-3;z1<=z-2;z1++) 
							setBlockLocal(x1,z1,y1,bRule);
				setBlockLocal(x,z,y,STONE_PLATE_ID);
				setBlockLocal(x,z-2,y,TNT_ID);
				setBlockLocal(x,z-2,y+1,TNT_ID);
			}
		}
		
		
		//dispenser trap
		if(spawnerSelection==null || random.nextBoolean()){
			for(int tries=0; tries<10; tries++){
				int x=random.nextInt(fWidth)+fBB[0][z],
			        y=random.nextInt(fLength)+fBB[2][z];
				BuildingDispenserTrap bdt=new BuildingDispenserTrap(wgt, bRule, random.nextInt(4),1, getIJKPt(x,z,y));
				if(bdt.queryCanBuild(2)){
					bdt.build(random.nextBoolean() ? BuildingDispenserTrap.ARROW_MISSILE : BuildingDispenserTrap.DAMAGE_POTION_MISSILE, true);
					break;
				}
			}
		}
		
	}
	
	
	private int pickCAChestType(int z){
		if(  Math.abs(zGround-z) > random.nextInt(1 + z>zGround ? (bHeight-zGround):zGround) 
		 && (z>zGround ? (bHeight-zGround):zGround) > 20)
			return random.nextBoolean() ? MEDIUM_CHEST_ID : HARD_CHEST_ID;
		else 
			return random.nextBoolean() ? EASY_CHEST_ID : MEDIUM_CHEST_ID;
	}
	
	
	public boolean shiftBuidlingJDown(int maxShift){
		//try 4 corners and center
		int[] heights=new int[]{findSurfaceJ(world, getI(bWidth-1,0), getK(bWidth-1,0),j0+10,false,IGNORE_WATER),
							    findSurfaceJ(world, getI(0,bLength-1), getK(0,bLength-1),j0+10,false,IGNORE_WATER),
							    findSurfaceJ(world, getI(bWidth-1,bLength-1), getK(bWidth-1,bLength-1),j0+10,false,IGNORE_WATER),
							    findSurfaceJ(world, getI(bWidth/2,bLength/2), getK(bWidth/2,bLength/2),j0+10,false,IGNORE_WATER)};
		
		int minHeight=minOrMax(heights,true);
		if(minOrMax(heights,false)- minHeight > maxShift) 
			return false;
		else 
			j0=minHeight;
		return true;
	}

	
	public static byte[][]makeSymmetricSeed(int maxWidth, float seedDensity, Random random){
		maxWidth=random.nextInt(random.nextInt(Math.max(1,maxWidth-SYMMETRIC_SEED_MIN_WIDTH))+1) + 1;
		int width =random.nextInt(random.nextInt(maxWidth)+1)+SYMMETRIC_SEED_MIN_WIDTH,
			length=random.nextInt(random.nextInt(maxWidth)+1)+SYMMETRIC_SEED_MIN_WIDTH;
		byte[][] seed=new byte[width][length];
		
		int diam=Math.min(Math.max(width,length),MAX_SPHERE_DIAM);
		for(int x=0; x<(width+1)/2; x++){ 
			for(int y=0; y<(length+1)/2; y++){ 
				seed[x][y]=(Building.CIRCLE_SHAPE[diam][x][y]>=0 && random.nextFloat() < seedDensity) //use a circular mask to avoid ugly corners 
						? ALIVE:DEAD; 
				seed[width-x-1][y]=seed[x][y];
				seed[x][length-y-1]=seed[x][y];
				seed[width-x-1][length-y-1]=seed[x][y];
		}}
		return seed;
	}
	
	public static byte[][] makeLinearSeed(int maxWidth, Random random){
		if(maxWidth<=1) 
			return new byte[][]{{ALIVE}}; //degenerate case
		
		int width=random.nextInt(random.nextInt(maxWidth-1)+1)+2; //random number in (2,maxWidth) inclusive, concentrated towards low end
		byte[][] seed=new byte[width][1];
		for(int x=0; x<width; x++)
			seed[x][0]=ALIVE;
		return seed;
	}
	
	public static byte[][] makeCruciformSeed(int maxWidth, Random random){
		if(maxWidth<=1) return new byte[][]{{ALIVE}}; //degenerate case
		
		int width=2*(random.nextInt(random.nextInt(maxWidth/2)+1)+1)+1, //width and length are always odd
		   length=2*(random.nextInt(random.nextInt(maxWidth/2)+1)+1)+1;
		
		byte[][] seed=new byte[width][length];
		for(int x=0; x<width; x++) 
			for(int y=0; y<length; y++)
			seed[x][y]=(x==width/2 || y==length/2) ? ALIVE:DEAD;
		return seed;
	}
	
	public static byte[][] makeCircularSeed(int maxWidth, Random random){
		int diam=Math.min(random.nextInt(random.nextInt(random.nextInt(Math.max(1,maxWidth-CIRCULAR_SEED_MIN_WIDTH))+1)+1) + CIRCULAR_SEED_MIN_WIDTH,MAX_SPHERE_DIAM);
		byte[][] seed=new byte[diam][diam];
		for(int x=0; x<diam; x++) 
			for(int y=0; y<diam; y++)
			seed[x][y]=CIRCLE_SHAPE[diam][x][y]==1 ? ALIVE:DEAD;
		return seed;
	}
	
	
	public final static byte[][] parseCARule(String str, PrintWriter lw){
		try{
			byte[][] rule=new byte[][]{{0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0}};
			
			String birthStr=str.split("/")[0].trim();
			String surviveStr=str.split("/")[1].trim();
			for(int n=1; n<birthStr.length(); n++){
				int digit=Integer.parseInt(birthStr.substring(n,n+1));
				rule[0][digit]=ALIVE;
			}
			for(int n=1; n<surviveStr.length(); n++){
				int digit=Integer.parseInt(surviveStr.substring(n,n+1));
				rule[1][digit]=ALIVE;
			}
			return rule;
		}catch(Exception e){
			if(lw!=null) 
				lw.println("Error parsing automaton rule "+str+": "+e.getMessage());
			return null;
		}
	}
	
	public final static String ruleToString(byte[][] rule){
		StringBuilder sb=new StringBuilder(30);
		sb.append("B");
		for(int n=0; n<9; n++) 
			if(rule[0][n]==ALIVE)
				sb.append(n);
		sb.append("S");
		for(int n=0; n<9; n++) 
			if(rule[1][n]==ALIVE) 
				sb.append(n);
		return sb.toString();
	}
	
}

