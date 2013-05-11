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
 * BuildingTower builds a procedurally generated tower.
 */


public class BuildingTower extends Building
{
	public final static int FLOOR_HAUNTED_CHANCE=50, HAUNTED_CHEST_CHANCE=60;
	public final static int TOWER_UNIV_MIN_WIDTH=5,TOWER_LEVELING=12;
	public final static String[] ROOFSTYLE_NAMES={"Crenel","Steep","Steep Trim","Shallow","Dome","Cone","Two Sided"};
	public final static int[] ROOF_STYLE_IDS=new int[ROOFSTYLE_NAMES.length];
	public final static int SURFACE_PORTAL_ODDS=20, NETHER_PORTAL_ODDS=10; 
	public final static int BOOKSHELF_ODDS=3, BED_ODDS=5, CAULDRON_ODDS=8,BREWING_STAND_ODDS=8,ENCHANTMENT_TABLE_ODDS=12; 
	public final static int ROOF_CRENEL=0, ROOF_STEEP=1, ROOF_TRIM=2, ROOF_SHALLOW=3, ROOF_DOME=4,ROOF_CONE=5,ROOF_TWO_SIDED=6;
	public final static int NORTH_FACE_DOOR_META=3, EAST_FACE_DOOR_META=0, SOUTH_FACE_DOOR_META=1, WEST_FACE_DOOR_META=2;
							   
	
	static{ 
		for(int m=0;m<ROOFSTYLE_NAMES.length;m++) ROOF_STYLE_IDS[m]=m;
	}
	
	public int baseHeight,roofStyle,minHorizDim;
	public boolean PopulateFurniture, MakeDoors,circular;
	private int[][][][] buffer;
	private int[][] circle_shape;
	private TemplateRule roofRule, SpawnerRule, ChestRule;

	//****************************************  CONSTRUCTOR - BuildingTower *************************************************************************************//
	//      ----
	//     -    -
	//    -      -
	//   -        -
	//  -          -
	// -------------- bHeight
	// -            -
	// -            -
	// -            -
	// --------------	
	// -            -
	// -            -
	// -            -
	// --------------
	// -            -
	// -            -
	// -            -
	// -            -  baseHeight==ws.WalkHeight
	// --------------  baseHeight-1 (floor)
	//
	public BuildingTower(int ID_,BuildingWall wall,int dir_,int axXHand_, boolean centerAligned_,int TWidth_, int THeight_,int TLength_, int[] sourcePt){
		super(ID_,wall.wgt, wall.towerRule,dir_,axXHand_,centerAligned_,new int[]{TWidth_,THeight_,TLength_},sourcePt);
		baseHeight=wall.WalkHeight;
		roofStyle=wall.roofStyle;
		minHorizDim=Math.min(bWidth, bLength);
		circle_shape=CIRCLE_SHAPE[minHorizDim];
		circular= wall.circular;
		ChestRule=wall.ws.ChestRule;
		roofRule= wall.roofRule;
		SpawnerRule=wall.ws.SpawnerRule;
		PopulateFurniture=wall.ws.PopulateFurniture;
		MakeDoors=wall.ws.MakeDoors;
		if(circular) bLength=bWidth=minHorizDim; //enforce equal horizontal dimensions if circular
	}
	
	public BuildingTower(int ID_,Building parent,boolean circular_,int roofStyle_,int dir_,int axXHand_, boolean centerAligned_,int TWidth_, int THeight_,int TLength_, int[] sourcePt){
		super(ID_,parent.wgt, parent.bRule,dir_,axXHand_,centerAligned_,new int[]{TWidth_,THeight_,TLength_},sourcePt);
		baseHeight=0;
		roofStyle=roofStyle_;
		minHorizDim=Math.min(bWidth, bLength);
		circle_shape=CIRCLE_SHAPE[minHorizDim];
		circular= circular_;
		ChestRule=TemplateRule.RULE_NOT_PROVIDED;
		roofRule= bRule;
		SpawnerRule=TemplateRule.RULE_NOT_PROVIDED;
		PopulateFurniture=false;
		MakeDoors=false;
		if(circular) bLength=bWidth=minHorizDim; //enforce equal horizontal dimensions if circular
	}

	//****************************************  FUNCTION - queryCanBuild *************************************************************************************//
	public boolean queryCanBuild(int ybuffer, boolean overlapTowers) throws InterruptedException{
		int rooftopJ=j0 + bHeight + (roofStyle==ROOF_CONE ? minHorizDim : minHorizDim/2)+2; 
		if(rooftopJ > WORLD_MAX_Y) bHeight-= rooftopJ - WORLD_MAX_Y;
		if(bHeight < baseHeight + 4){
			return false;
		}
		if(!( queryExplorationHandlerForChunk(0,0,bLength-1) 
		   && queryExplorationHandlerForChunk(bWidth-1,0,0) 
		   && queryExplorationHandlerForChunk(bWidth-1,0,bLength-1) )) {
			return false;
		}
		
		//check if obstructed at roof
		if(isObstructedRoof(ybuffer)){
			//if(BuildingWall.DEBUG) FMLLog.getLogger().info("Tower blocked in roof.");
			return false;
		}
		
		//check if obstructed on body
		if(wgt.isLayoutGenerator()){
			int[] pt1=getIJKPt(overlapTowers ? bWidth/4:0,            0, overlapTowers ? bLength/4:ybuffer),
				  pt2=getIJKPt(overlapTowers ? 3*bWidth/4-1:bWidth-1, 0, overlapTowers ? 3*bLength/4-1:bLength-1);
		    if(wgt.layoutIsClear(pt1,pt2,WorldGeneratorThread.LAYOUT_CODE_TOWER)){
	    	   wgt.setLayoutCode(pt1,pt2,WorldGeneratorThread.LAYOUT_CODE_TOWER);
	    	} else 
	    		return false;
		}else if(!overlapTowers){
			if(isObstructedFrame(3,ybuffer)) {
				//if(BuildingWall.DEBUG) FMLLog.getLogger().info("Tower blocked in frame.");
				return false;
		}}
		//if(BuildingWall.DEBUG) FMLLog.getLogger().info("canbuildtower");
		return true;
	}
	
	public boolean isObstructedRoof(int ybuffer){
		int rBuffer=(roofStyle==ROOF_CRENEL ? 1:( roofStyle==ROOF_DOME || roofStyle==ROOF_CONE) ? 0:-1);
		int rHeight=(roofStyle==ROOF_CRENEL ? 2 : minHorizDim/2);
		if(isObstructedSolid(new int[]{rBuffer,bHeight,Math.max(rBuffer, ybuffer)},
				             new int[]{bWidth-1-rBuffer,bHeight+rHeight,bLength-1-rBuffer}) ) {
			if(BuildingWall.DEBUG) System.err.println("Cannot build Tower "+IDString()+". Obstructed!");
			return true;
		}
		return false;
	}

	//****************************************  FUNCTION - build *************************************************************************************//
	//builds a tower:   
	//PARAMETERS:
	//doorOffset1,doorOffset2 - x-offset of the ground floor door from center
	//hanging - if true, taper away tower base if hanging over air or water
	//RETURNS:
	//true if tower was built (dependency: buildOver).
	//
	public void build(int doorOffset1, int doorOffset2,boolean hanging) {
		
		//check against spawner chance to see if haunted. 
		//If hasUndeadSpawner =>	Don't make torches anywhere in tower.
		//							Each floor has a 50% chance of building spawners except ground floor.
		//If floor hasUndeadSpawner =>	Always build spawner_count spawners (resampling for each spawner).
		//								No windows/doors unless ground floor.
		boolean undeadTower=false, ghastTower=false;
		if(SpawnerRule!=TemplateRule.RULE_NOT_PROVIDED){
			for(int blockID : SpawnerRule.getBlockIDs())
				if(blockID==ZOMBIE_SPAWNER_ID || blockID==SKELETON_SPAWNER_ID || blockID==CREEPER_SPAWNER_ID|| 
						blockID==UPRIGHT_SPAWNER_ID || blockID==EASY_SPAWNER_ID) 
					undeadTower=true;
			ghastTower=roofStyle==ROOF_CRENEL && SpawnerRule.getBlock(random)[0]==GHAST_SPAWNER_ID;
			if(ghastTower ||  random.nextInt(100)>SpawnerRule.chance) undeadTower=false;
		}

		
		if(undeadTower && bHeight - baseHeight < 9) bHeight = baseHeight  + 9;
		if(baseHeight<0) baseHeight=0;


		//buffer - dimensions have an offset of one on each side (except top) to fit in roof overhang and floor.
		//So to translate from Building coord system need to add +1 to all entries, annoying but that's life.
		buffer=new int[bWidth+2][bHeight+minHorizDim+3][bLength+2][2];
		for(int x1=0;x1<bWidth+2;x1++) for(int y1=0;y1<bLength+2;y1++) for(int z1=0;z1<bHeight+minHorizDim+3;z1++)
			buffer[x1][z1][y1]=PRESERVE_BLOCK;

		//========================================= build it ==========================================
		//*** sidewalls and base ***
		for(int x1=0;x1<bWidth;x1++){
			for(int y1=0;y1<bLength;y1++){
				if(!(circular && circle_shape[x1][y1]<0)){
					//buffer[x1+1][THeight][y1+1]=TRule.getBlockOrHole(random); 
	
					for(int z1=baseHeight-1; z1<bHeight; z1++){  
						if(	circular && (circle_shape[x1][y1]==1)  //circular sidewalls condition
						 || !circular && (x1==0 || x1==bWidth-1 || y1==0 || y1==bLength-1)) //rectangular sidewalls condition
						{
							buffer[x1+1][z1+1][y1+1]=bRule.getBlockOrHole(random);
						}
						else buffer[x1+1][z1+1][y1+1]=HOLE_BLOCK_NO_LIGHTING;
					}
					
					//column above source point
					for(int z1=-1; z1<baseHeight-1; z1++) buffer[x1+1][z1+1][y1+1]=bRule.getBlockOrHole(random);
	
					//column below source point, set zmin to taper overhangs
					
					
					//int zmin=hanging && y1>=TWidth/2 && isWallable(x1,-BUILDDOWN,y1) ?  Math.max(2*(y1-TWidth/2)+3*Math.abs(x1-TWidth/2)-5*TWidth/2,-BUILDDOWN) : -BUILDDOWN;
					buildDown(x1,-2,y1,bRule,TOWER_LEVELING,2,(bLength-y1-1)/2);
					
				}
			}
		}

		//*** floors and windows/doors ***
		int sideWindowY=bLength/2;
		for(int z1=baseHeight-1;z1<bHeight-3;z1+=4){
			//windows/doors
			int winDoorX1=Math.min(Math.max(bWidth/2 + (z1==baseHeight-1 ? doorOffset1 : 0),1),bWidth-2);
			int winDoorX2=Math.min(Math.max(bWidth/2 + (z1==baseHeight-1 ? doorOffset2 : 0),1),bWidth-2);
			int torchX1=winDoorX1 + (winDoorX1==bWidth-2 ? -1:1);
			int torchX2=winDoorX2 + (winDoorX2==bWidth-2 ? -1:1);
		
			boolean floorHasUndeadSpawner=undeadTower && z1>baseHeight-1 && random.nextInt(100)<FLOOR_HAUNTED_CHANCE;
			
			if(z1==baseHeight-1 || !floorHasUndeadSpawner){
				int winH=z1>baseHeight-1 ? 2:3;
				buildWindowOrDoor(0,z1+2,sideWindowY,-1,0,winH);
				buildWindowOrDoor(bWidth-1,z1+2,sideWindowY,1,0,winH);
				buildWindowOrDoor(winDoorX1,z1+2,0,0,-1,winH);
				buildWindowOrDoor(winDoorX2,z1+2, bLength-1,0,1,winH);
			}

			//floor
			for(int y1=1; y1<bLength-1;y1++)
				for(int x1=1; x1<bWidth-1;x1++)
					if(!circular || circle_shape[x1][y1]==0)
						buffer[x1+1][z1+1][y1+1]= bRule.primaryBlock[0]==LOG_ID ? new int[]{WOOD_ID,0} : bRule.getBlockOrHole(random);

			//door torches
			if(!undeadTower && bRule.chance==100){ 
				buffer[torchX1+1][z1+3+1][1+(circular && bLength==6 ? 1:0)+1]=NORTH_FACE_TORCH_BLOCK; 
				buffer[torchX2+1][z1+3+1][bLength-2-(circular && bLength==6 ? 1:0)+1]=SOUTH_FACE_TORCH_BLOCK;
			}
			
			//*** mob spawners***
			if(floorHasUndeadSpawner && z1==baseHeight+4 && bRule.chance==100){
				for(int x1=bWidth/2-1;x1<=bWidth/2+1;x1++){
					for(int y1=1; y1<Math.min(3, sideWindowY-1);y1++)
						if(!circular || circle_shape[x1][y1]==0)
							buffer[x1+1][z1+1][y1+1]=HOLE_BLOCK_LIGHTING;
					for(int y1=bLength-Math.min(3, sideWindowY-1); y1<bLength-1;y1++)
						if(!circular || circle_shape[x1][y1]==0)
							buffer[x1+1][z1+1][y1+1]=HOLE_BLOCK_LIGHTING;
				}
			}
			else if(SpawnerRule!=TemplateRule.RULE_NOT_PROVIDED && random.nextInt(100)<SpawnerRule.chance && !ghastTower){
				int[] spawnerBlock=SpawnerRule.getNonAirBlock(random);
				
				if(spawnerBlock[0]!=GHAST_SPAWNER_ID)
					buffer[bWidth/2+1][z1+1+1][sideWindowY+1]=spawnerBlock;
			}
			
			
			//chests
			//System.out.println("checking for chest");
			if(ChestRule!=TemplateRule.RULE_NOT_PROVIDED && random.nextInt(100)<ChestRule.chance)
				buffer[bWidth-2+1][z1+1+1][sideWindowY-1+1]=ChestRule.getNonAirBlock(random);
			else if(floorHasUndeadSpawner && random.nextInt(100) < HAUNTED_CHEST_CHANCE) //even if no chest rule, can have chests if floorIsHaunted
				buffer[bWidth-2+1][z1+1+1][sideWindowY-1+1]=z1 < 15 ? TOWER_CHEST_BLOCK : HARD_CHEST_BLOCK;

			if(z1==baseHeight-1) z1++; //ground floor is one block higher
		}
		
		//*** ladder ***
		int topFloorHeight=((bHeight-baseHeight-4)/4)*4+baseHeight+1;
		int ladderHeight=roofStyle==ROOF_CRENEL ? bHeight : (bHeight-baseHeight<8 ? 0 : topFloorHeight);  //don't continue through top floor unless crenellated
		for(int z1=baseHeight; z1<ladderHeight;z1++) buffer[1+1][z1+1][sideWindowY-1+1]=EAST_FACE_LADDER_BLOCK;

		//*** roof ***
		buildRoof();
		if(undeadTower && roofStyle==ROOF_CRENEL) 
			buffer[1+1][bHeight > baseHeight+12 ? baseHeight+9 : bHeight+1][sideWindowY-1+1] = bRule.getBlockOrHole(random);
		
		//*** run decay ***
		int zLim= bRule.chance>=100 ? buffer[0].length : propagateCollapse(bRule.chance);

		//*** build from buffer ***
		//build tower
		for(int x1=1;x1<buffer.length-1;x1++){
			for(int y1=1;y1<buffer[0][0].length-1;y1++){
				if(!circular || circle_shape[x1-1][y1-1]>=0){
					for(int z1=0; z1<Math.min(bHeight,zLim); z1++){
						setBlockLocal(x1-1,z1-1,y1-1,buffer[x1][z1][y1]);		
		}}}}
		//build roof
		for(int x1=0;x1<buffer.length;x1++){
			for(int y1=0;y1<buffer[0][0].length;y1++){
				for(int z1=bHeight; z1<zLim; z1++){
					setBlockLocal(x1-1,z1-1,y1-1,buffer[x1][z1][y1]);		
		}}}


		//*** prettify any stairs outside entrance/exit ***
		for(int x1=1; x1<bWidth-1;x1++){
			if(isStairBlock(x1,baseHeight, -1) && getBlockIdLocal(x1, baseHeight, -2)==bRule.primaryBlock[0])   setBlockLocal(x1, baseHeight, -1,bRule.primaryBlock[0]);
			if(isStairBlock(x1, baseHeight, bLength) && getBlockIdLocal(x1, baseHeight, bLength+1)==bRule.primaryBlock[0])   setBlockLocal(x1, baseHeight, bLength,bRule.primaryBlock[0]);
		}
		
		//furniture
		if(PopulateFurniture){
			for(int z1=baseHeight;z1<bHeight-2;z1+=4){
				for(int m=0; m<bLength*bWidth/25; m++){ //scale to floor area
					if(!undeadTower && random.nextInt(BED_ODDS)==0) populateBeds(z1);
					if(bHeight-baseHeight>8 && random.nextInt(BOOKSHELF_ODDS)==0) populateBookshelves(z1);
					if(random.nextInt(CAULDRON_ODDS)==0) populateFurnitureColumn(z1,new int[][]{{CAULDRON_BLOCK_ID,random.nextInt(4)}});
					if(z1>12 && random.nextInt(BREWING_STAND_ODDS)==0) populateFurnitureColumn(z1,new int[][]{bRule.primaryBlock,{BREWING_STAND_BLOCK_ID,random.nextInt(2)+1}});
					if(z1>20 && random.nextInt(ENCHANTMENT_TABLE_ODDS)==0) populateFurnitureColumn(z1,new int[][]{{ENCHANTMENT_TABLE_ID,0}});
				}
				if(z1==baseHeight) z1++;
			}
		}
		if(ghastTower) populateGhastSpawner(bHeight+1);
		else if(roofStyle==ROOF_CRENEL && bHeight>22 ) populatePortal(bHeight+1);

		//*** debug signs ***
		if(BuildingWall.DEBUG_SIGNS){
			String[] lines=new String[]{IDString().split(" ")[0],IDString().split(" ")[1],localCoordString(bWidth/2,baseHeight,bLength/2)};
			setSignOrPost(bWidth/2,baseHeight,bLength/2,true,12,lines);
		}
		//if(BuildingWall.DEBUG) FMLLog.getLogger().info("tower finished");
		flushDelayed();	
	}
	
	
	//****************************************  FUNCTION  - buildWindowOrDoor *************************************************************************************//
	//builds either a window or a door depending on whether there is a floor outside of the aperture.
	private void buildWindowOrDoor(int x,int z, int y, int xFace, int yFace, int height){
		boolean buildWoodDoor=false;
		if(isFloor(x+xFace,z-1,y+yFace) || isFloor(x+xFace,z-2,y+yFace)){
			z--;
			if(MakeDoors) buildWoodDoor=true;
		}
		if(!IS_WALLABLE[getBlockIdLocal(x,z+height-2,y+yFace)]) return;
		
		if(buildWoodDoor){
			int metadata=xFace==0 
							? (yFace > 0 ? SOUTH_FACE_DOOR_META : NORTH_FACE_DOOR_META ) 
							: (xFace > 0 ? WEST_FACE_DOOR_META : EAST_FACE_DOOR_META);
			buffer[x+1][z+1][y+1]=new int[]{WOODEN_DOOR_ID,metadata};
			buffer[x+1][z+1+1][y+1]=new int[]{WOODEN_DOOR_ID,metadata+8};
			if(isFloor(x+xFace,z-1,y+yFace) && x+xFace+1>=0 && x+xFace+1<buffer.length && y+yFace+1>=0 && y+yFace+1<buffer[0][0].length){
				buffer[x+xFace+1][z-1+1][y+yFace+1]=blockToStepMeta(bRule.primaryBlock);//build a step-up
			}
		} else for(int z1=z;z1<z+height;z1++) 
			buffer[x+1][z1+1][y+1]=HOLE_BLOCK_LIGHTING; //carve out the aperture
		
		//clear a step in front of window if there is a floor at z+1
		if(isFloor(x+xFace,z+2,y+yFace) && isWallBlock(x+xFace,z,y+yFace) &&
				x+xFace+1>=0 && x+xFace+1<buffer.length && y+yFace+1>=0 && y+yFace+1<buffer[0][0].length)
			buffer[x+xFace+1][z+1+1][y+yFace+1]=HOLE_BLOCK_LIGHTING;	
	}
	
	//****************************************  FUNCTIONS  - populators *************************************************************************************//
	private void populateBeds(int z){
		int dir=random.nextInt(4);
		int x1=random.nextInt(bWidth-2)+1;
		int y1=random.nextInt(bLength-2)+1;
		int x2 = x1+DIR_TO_X[dir], y2 = y1+DIR_TO_Y[dir];
		if(	isFloor(x1,z,y1) && !isNextToDoorway(x1,z,y1) 
		 && isFloor(x2,z,y2) && !isNextToDoorway(x2,z,y2)){
			setBlockLocal(x1,z,y1,BED_BLOCK_ID,dir+8);
			setBlockLocal(x2,z,y2,BED_BLOCK_ID,dir);
		}
	}
	
	private void populateFurnitureColumn(int z, int[][] block){
		int x1=random.nextInt(bWidth-2)+1;
		int y1=random.nextInt(bLength-2)+1;
		if(isFloor(x1,z,y1) && !isNextToDoorway(x1,z,y1)){
			for(int z1=0; z1<block.length; z1++)
				setBlockLocal(x1,z+z1,y1,block[z1]);
		}
	}
	
	private void populateBookshelves(int z){
		int x1=random.nextInt(bWidth-2)+1;
		int y1=random.nextInt(bLength-2)+1;
		int dir=random.nextInt(4);
		int xinc=DIR_TO_X[dir];
		int yinc=DIR_TO_Y[dir];
		
		//find a wall
		while(true){
			if(x1<1 || x1>=bWidth-1 || y1<1 || y1>=bLength-1 || !isFloor(x1,z,y1)) return;
			if(IS_ARTIFICAL_BLOCK[getBlockIdLocal(x1+xinc,z,y1+yinc)] && IS_ARTIFICAL_BLOCK[getBlockIdLocal(x1+xinc,z-1,y1+yinc)] 
			   && getBlockIdLocal(x1+xinc,z-1,y1+yinc)!=LADDER_ID ){
				break;
			}
			x1+=xinc;
			y1+=yinc;
		}
		
		for(int m=0;m<2;m++){
			for(int z1=z;z1<z+1+random.nextInt(3);z1++){
				if(getBlockIdLocal(x1,z1,y1)!=0 || !isWallBlock(x1+xinc,z1,y1+yinc)) break;
				setBlockLocal(x1,z1,y1,BOOKSHELF_ID);
			}
			x1+=DIR_TO_X[(dir+1)%4];
			y1+=DIR_TO_Y[(dir+1)%4];
			if(!isFloor(x1,z,y1)) break;
		}
	
	}
	
	private boolean populateGhastSpawner(int z){
		for(int tries=0; tries < 5; tries++){
			int x1=random.nextInt(bWidth-2)+1;
			int y1=random.nextInt(bLength-2)+1;
			if(isFloor(x1,z,y1)){
				setBlockLocal(x1,z,y1,GHAST_SPAWNER_ID);
				return true;
			}
		}
		return false;
	}
	
	private boolean populatePortal(int z){
		if(world.provider.isHellWorld){ if(random.nextInt(NETHER_PORTAL_ODDS)!=0) return false; }
		else if(random.nextInt(SURFACE_PORTAL_ODDS)!=0) return false;
		boolean hasSupport=false;
		for(int y1=bLength/2-2; y1<bLength/2+2;y1++){
			if(getBlockIdLocal(bWidth/2,z,y1)!=0 ) return false;
			if(getBlockIdLocal(bWidth/2,z-1,y1)!=0) hasSupport=true;
		}
		if(!hasSupport) return false;
		
		for(int y1=bLength/2-2; y1<bLength/2+2; y1++){
			setBlockLocal(bWidth/2,z,y1,OBSIDIAN_ID);
			setBlockLocal(bWidth/2,z+4,y1,OBSIDIAN_ID);
		}
		for(int z1=z+1;z1<z+4;z1++){
			setBlockLocal(bWidth/2,z1,bLength/2-2,OBSIDIAN_ID);
			setBlockLocal(bWidth/2,z1,bLength/2-1,PORTAL_ID);
			setBlockLocal(bWidth/2,z1,bLength/2,PORTAL_ID);
			setBlockLocal(bWidth/2,z1,bLength/2+1,OBSIDIAN_ID);
		}
		return true;
	}
	



	//****************************************  FUNCTION  - buildRoof  *************************************************************************************//
	private void buildRoof(){
		//If roofRule=DEFAULT_ROOF_RULE, do wooden for sloped roofstyles and TRule otherwise
		//If roofRule=cobblestone, do cobblestone for all roofstyles
		//If roofRule=sandstone/step, do wooden for steep roofstyle and sandstone/step otherwise
		//Otherwise do wooden for sloped roofstyles, and roofRule otherwise
		if(roofRule==TemplateRule.RULE_NOT_PROVIDED){
			roofRule= (roofStyle==ROOF_STEEP || roofStyle==ROOF_SHALLOW || roofStyle==ROOF_TRIM || roofStyle==ROOF_TWO_SIDED)
					? new TemplateRule(new int[]{WOOD_ID,0}) : bRule;
		}
		
		int stepMeta=blockToStepMeta(roofRule.primaryBlock)[1];

		TemplateRule stepRule=new TemplateRule(new int[]{STEP_ID,stepMeta},roofRule.chance);
		TemplateRule doubleStepRule=(stepMeta==2) ? new TemplateRule(new int[]{WOOD_ID,0},roofRule.chance) : 
				new TemplateRule(new int[]{DOUBLE_STEP_ID,stepMeta},roofRule.chance);
		
		TemplateRule trimRule = roofStyle==ROOF_TRIM ?
				new TemplateRule(new int[]{bRule.primaryBlock[0]==COBBLESTONE_ID ? LOG_ID:COBBLESTONE_ID,0},roofRule.chance)
		     :  stepRule;
		TemplateRule northStairsRule=new TemplateRule(new int[]{blockToStairs(roofRule.primaryBlock),STAIRS_DIR_TO_META[DIR_NORTH]},roofRule.chance);
		TemplateRule southStairsRule=new TemplateRule(new int[]{blockToStairs(roofRule.primaryBlock),STAIRS_DIR_TO_META[DIR_SOUTH]},roofRule.chance);
		TemplateRule eastStairsRule=new TemplateRule(new int[]{blockToStairs(roofRule.primaryBlock),STAIRS_DIR_TO_META[DIR_EAST]},roofRule.chance);
		TemplateRule westStairsRule=new TemplateRule(new int[]{blockToStairs(roofRule.primaryBlock),STAIRS_DIR_TO_META[DIR_WEST]},roofRule.chance);
	
		
		//======================================== build it! ================================================
		if(roofStyle==ROOF_CRENEL){ //crenelated
			if(circular){
				for(int y1=0; y1<bLength;y1++){ for(int x1=0; x1<bWidth;x1++){
					if(circle_shape[x1][y1]>=0) buffer[x1+1][bHeight+1][y1+1]=bRule.getBlockOrHole(random);
					if(CIRCLE_CRENEL[minHorizDim][x1][y1]==1) buffer[x1+1][bHeight+1+1][y1+1]=bRule.getBlockOrHole(random);
				}}
			}
			else{ //square
				for(int y1=0; y1<bLength;y1++) for(int x1=0; x1<bWidth;x1++)
					buffer[x1+1][bHeight+1][y1+1]=bRule.getBlockOrHole(random);
				for(int m=0; m<bWidth; m+=2){
					if(!(getBlockIdLocal(m, bHeight, -1)==bRule.primaryBlock[0]|| getBlockIdLocal(m, bHeight-1, -1)==bRule.primaryBlock[0]))
						buffer[m+1][bHeight+1+1][0+1]= (m+1)%2==0 ? HOLE_BLOCK_LIGHTING : bRule.getBlockOrHole(random);
					if(!(getBlockIdLocal(m, bHeight, bLength)==bRule.primaryBlock[0] || getBlockIdLocal(m, bHeight-1, bLength)==bRule.primaryBlock[0]))
						buffer[m+1][bHeight+1+1][bLength-1+1] = (m+1)%2==0 ? HOLE_BLOCK_LIGHTING : bRule.getBlockOrHole(random);
				}
				for(int m=0; m<bLength; m+=2){
					if(!(getBlockIdLocal(-1, bHeight, m)==bRule.primaryBlock[0] || getBlockIdLocal(-1, bHeight-1, m)==bRule.primaryBlock[0]))
						buffer[0+1][ bHeight+1+1][ m+1]= (m+1)%2==0 ? HOLE_BLOCK_LIGHTING : bRule.getBlockOrHole(random);
					if(!(getBlockIdLocal(bWidth,bHeight,m)==bRule.primaryBlock[0] || getBlockIdLocal(bWidth, bHeight-1, m)==bRule.primaryBlock[0]))
						buffer[bWidth-1+1][bHeight+1+1][ m+1]= (m+1)%2==0 ? HOLE_BLOCK_LIGHTING : bRule.getBlockOrHole(random);
				}
				for(int y1=1; y1<bLength-1;y1++)
					for(int x1=1; x1<bWidth-1;x1++)
						if(isWallBlock(x1, bHeight, y1) && 
								!(isWallBlock(x1+1, bHeight, y1) || isWallBlock(x1-1, bHeight, y1) || isWallBlock(x1, bHeight, y1+1) ||isWallBlock(x1, bHeight, y1-1) ))
							buffer[x1+1][bHeight+1-1+1][y1+1] = HOLE_BLOCK_LIGHTING;
			}
			buffer[2][bHeight+1][bLength/2]=EAST_FACE_LADDER_BLOCK;
			buffer[2][bHeight+2][bLength/2]=new int[]{TRAP_DOOR_ID,3};
		}
		else if(roofStyle==ROOF_STEEP || roofStyle==ROOF_TRIM || (roofStyle==ROOF_SHALLOW && (bWidth < 6 || bLength < 6))){ //45 degrees sloped
			for(int m=0; m<(minHorizDim+1)/2; m++){
				for(int x1=m; x1<bWidth-m; x1++){
					for(int y1=m; y1<bLength-m; y1++) {
						buffer[x1+1][ bHeight+m+1][ y1+1]= HOLE_BLOCK_LIGHTING;
						if(m==(bWidth+1)/2-1)
							buffer[x1+1][ bHeight+m+1+1][ y1+1] = trimRule.getBlockOrHole(random);
					}
					buffer[x1+1][ bHeight+m+1][ m-1+1] = northStairsRule.getBlockOrHole(random);
					buffer[x1+1][ bHeight+m+1][ bLength-m+1] = southStairsRule.getBlockOrHole(random);
					buffer[x1+1][ bHeight+m+1][ m+1] = doubleStepRule.getBlockOrHole(random);
					buffer[x1+1][ bHeight+m+1][ bLength-m-1+1] = doubleStepRule.getBlockOrHole(random);
				}
				for(int y1=m;y1<bLength-m;y1++) {
					buffer[m-1+1][ bHeight+m+1][ y1+1] = eastStairsRule.getBlockOrHole(random);
					buffer[bWidth-m+1][ bHeight+m+1][ y1+1] = westStairsRule.getBlockOrHole(random);
					buffer[m+1][ bHeight+m+1][ y1+1] = doubleStepRule.getBlockOrHole(random);
					buffer[bWidth-m-1+1][ bHeight+m+1][ y1+1] = doubleStepRule.getBlockOrHole(random);
				}
				buffer[m-1+1][bHeight+m+1][m-1+1] = trimRule.getBlockOrHole(random);
				buffer[m-1+1][bHeight+m+1][bLength-m+1] = trimRule.getBlockOrHole(random);
				buffer[bWidth-m+1][bHeight+m+1][m-1+1] = trimRule.getBlockOrHole(random);
				buffer[bWidth-m+1][bHeight+m+1][bLength-m+1] = trimRule.getBlockOrHole(random);
			}
		}
		else if(roofStyle==ROOF_SHALLOW){ //22 degrees sloped
			
			for(int z12=-1;z12<(minHorizDim+1)/2;z12++){
				int z1=(z12+1)/2;
				if((z12+1)%2==0){
					for(int y1=z12+1; y1<bLength-2*z1; y1++){
						buffer[z12+1][z1+bHeight+1][y1+1] = stepRule.getBlockOrHole(random);
						buffer[bWidth-z12-1+1][z1+bHeight+1][y1+1] = stepRule.getBlockOrHole(random);
					}
					for(int x1=z12+1; x1<bWidth-2*z1; x1++){
						buffer[x1+1][z1+bHeight+1][z12+1] = stepRule.getBlockOrHole(random);
						buffer[x1+1][z1+bHeight+1][bLength-z12-1+1] = stepRule.getBlockOrHole(random);
					}
					buffer[z12+1][z1+bHeight+1][z12+1] = doubleStepRule.getBlockOrHole(random);
					buffer[z12+1][z1+bHeight+1][bLength-z12-1+1] = doubleStepRule.getBlockOrHole(random);
					buffer[bWidth-z12-1+1][z1+bHeight+1][z12+1] = doubleStepRule.getBlockOrHole(random);
					buffer[bWidth-z12-1+1][z1+bHeight+1][bLength-z12-1+1] = doubleStepRule.getBlockOrHole(random);
				}
				else{
					for(int y1=z12; y1<bLength-z12;y1++) for(int x1=z12; x1<bWidth-z12;x1++)
						buffer[x1+1][z1+bHeight+1][y1+1] =doubleStepRule.getBlockOrHole(random);
					for(int y1=z12+2; y1<bLength-z12-2;y1++) for(int x1=z12+2; x1<bWidth-z12-2;x1++)
						buffer[x1+1][z1+bHeight+1][y1+1] =HOLE_BLOCK_LIGHTING;
					buffer[z12+1][z1+bHeight+1+1][z12+1] = stepRule.getBlockOrHole(random);
					buffer[z12+1][z1+bHeight+1+1][bLength-z12-1+1] = stepRule.getBlockOrHole(random);
					buffer[bWidth-z12-1+1][z1+bHeight+1+1][z12+1] = stepRule.getBlockOrHole(random);
					buffer[bWidth-z12-1+1][z1+bHeight+1+1][bLength-z12-1+1] = stepRule.getBlockOrHole(random);
				}	
			}
			
		}
		else if(roofStyle==ROOF_DOME){ //dome
			for(int z1=0; z1<(minHorizDim+1)/2; z1++){
				int diam=SPHERE_SHAPE[minHorizDim][z1];
				for(int y1=0; y1<diam;y1++){ for(int x1=0; x1<diam;x1++){
					if(CIRCLE_SHAPE[diam][x1][y1]>=0)
						buffer[x1+(bWidth-diam)/2+1][bHeight+z1+1+1][ y1+(bLength-diam)/2+1]
						    = (CIRCLE_SHAPE[diam][x1][y1]==1 || z1>=(minHorizDim+1)/2-2) ? roofRule.getBlockOrHole(random) : HOLE_BLOCK_LIGHTING;
					if(z1<(minHorizDim-1)/2){
						int nextDiam=SPHERE_SHAPE[minHorizDim][z1+1];
						int x2=x1-(diam-nextDiam)/2, y2=y1-(diam-nextDiam)/2;
						if(CIRCLE_SHAPE[diam][x1][y1]==0 && (x2<0 || y2 < 0 || x2>=nextDiam || y2 >=nextDiam ||CIRCLE_SHAPE[nextDiam][x2][y2]!=0))
							buffer[x1+(bWidth-diam)/2+1][bHeight+z1+1+1][ y1+(bLength-diam)/2+1]=roofRule.getBlockOrHole(random);
					}
			}}}
		}
		else if(roofStyle==ROOF_CONE){ //cone
			int prevDiam=0;
			for(int z1=0; z1<minHorizDim+1; z1++){
				int diam=minHorizDim%2==0 ? 2*((minHorizDim-z1+1)/2) : 2*((minHorizDim-z1)/2)+1;
				for(int y1=0; y1<diam;y1++){ for(int x1=0; x1<diam;x1++){
					if(CIRCLE_SHAPE[diam][x1][y1]>=0)
						buffer[x1+(bWidth-diam)/2+1][bHeight+z1+1+1][y1+(bLength-diam)/2+1] 
						     = CIRCLE_SHAPE[diam][x1][y1]==1 ? roofRule.getBlockOrHole(random) : HOLE_BLOCK_LIGHTING;
					if(z1>0 && CIRCLE_SHAPE[diam][x1][y1]!=0 && CIRCLE_SHAPE[prevDiam][x1+(prevDiam-diam)/2][y1+(prevDiam-diam)/2]==0)
						buffer[x1+(bWidth-diam)/2+1][bHeight+z1+1][ y1+(bLength-diam)/2+1]=roofRule.getBlockOrHole(random);
				}}
				prevDiam=diam;
			}
		}
		else if(roofStyle==ROOF_TWO_SIDED){ //Two Sided
			//roof peak will follow the major horizontal axis
			//if X-axis is the major axis, rot will be false, minAxLen==bLength, maxAxLen==bWidth, and p==x1, r==y1.
			boolean rot= bLength>minHorizDim;
			int minAxLen= rot ? bWidth : bLength, maxAxLen= rot ? bLength : bWidth;
			int[] forwardsStairsRule= rot ? eastStairsRule.getBlockOrHole(random) : northStairsRule.getBlockOrHole(random),
				  backwardsStirRule= rot ? westStairsRule.getBlockOrHole(random) : southStairsRule.getBlockOrHole(random);
				
			for(int m=0;m<=minHorizDim/2;m++){
				for(int p=0;p<maxAxLen;p++){
					for(int r=m+1; r<minAxLen-m-1; r++) 
						buildXYRotated(p+1,bHeight+m+1,r+1,HOLE_BLOCK_LIGHTING,rot);
					buildXYRotated(p+1,bHeight+m+1,m-1+1,forwardsStairsRule,rot);
					buildXYRotated(p+1,bHeight+m+1,minAxLen-m+1,backwardsStirRule,rot);
					if(!(minHorizDim%2==0 && m==minHorizDim/2)){
						buildXYRotated(p+1,bHeight+m+1,m+1,doubleStepRule.getBlockOrHole(random),rot);
						buildXYRotated(p+1,bHeight+m+1,minAxLen-m-1+1,doubleStepRule.getBlockOrHole(random),rot);
					}
				}
				for(int r=m; r<minAxLen-m; r++) {
					if(!(minHorizDim%2==1 && m==minHorizDim/2)){
						buildXYRotated(1,bHeight+m+1,r+1,(circular && circle_shape[0][r]<0) ? 
								doubleStepRule.getBlockOrHole(random) : bRule.getBlockOrHole(random),rot);
						buildXYRotated(maxAxLen-1+1,bHeight+m+1,r+1,(circular && circle_shape[bLength-1][r]<0) ?
								doubleStepRule.getBlockOrHole(random) : bRule.getBlockOrHole(random),rot);
				}}
			}
		}
		
		//close up corner overhangs
		if(circular && (roofStyle==ROOF_STEEP || roofStyle==ROOF_TRIM || roofStyle==ROOF_SHALLOW || roofStyle==ROOF_TWO_SIDED)){
			for(int x1=0;x1<minHorizDim;x1++){ for(int y1=0;y1<minHorizDim;y1++){
				if(circle_shape[x1][y1]<0) 
					buffer[x1+1][bHeight+1][y1+1]=doubleStepRule.getBlockOrHole(random);
				if(circle_shape[x1][y1]==1){
					for(int z1=bHeight; z1<bHeight+minHorizDim; z1++){
						if(buffer[x1+1][z1+1][y1+1]==HOLE_BLOCK_LIGHTING)
							buffer[x1+1][z1+1][y1+1]=bRule.getBlockOrHole(random);
						else break;
				}}
			}}
		}
		if(roofStyle==ROOF_DOME || roofStyle==ROOF_CONE){
			int xBuff=(bWidth-minHorizDim)/2, yBuff=(bLength-minHorizDim)/2;
			for(int x1=0;x1<minHorizDim;x1++){ for(int y1=0;y1<minHorizDim;y1++){
				if(circle_shape[x1][y1]>=0)
					buffer[x1+xBuff+1][bHeight+1][y1+yBuff+1]=circle_shape[x1][y1]==0 ? HOLE_BLOCK_LIGHTING : bRule.getBlockOrHole(random);
				else if(!circular) buffer[x1+xBuff+1][bHeight+1][y1+yBuff+1]=bRule.getBlockOrHole(random);
			}}
			if(!circular){
				for(int y1=0;y1<minHorizDim;y1++){
					for(int x1=0;x1<xBuff;x1++) buffer[x1+1][bHeight+1][y1+1]=bRule.getBlockOrHole(random);
					for(int x1=xBuff+minHorizDim;x1<bWidth;x1++) buffer[x1+1][bHeight+1][y1+1]=bRule.getBlockOrHole(random);
				}
				for(int x1=0;x1<minHorizDim;x1++){ 
					for(int y1=0;y1<yBuff;y1++) buffer[x1+1][bHeight+1][y1+1]=bRule.getBlockOrHole(random);
					for(int y1=yBuff+minHorizDim;y1<bLength;y1++) buffer[x1+1][bHeight+1][y1+1]=bRule.getBlockOrHole(random);
				}
			}
				
		}

	}
	
	
	//****************************************  FUNCTION  - buildXYRotated *************************************************************************************//
	public void buildXYRotated(int p, int q, int r, int[] block, boolean rotated){
		if(rotated) buffer[r][q][p]=block;
		else buffer[p][q][r]=block;
	}

	//****************************************  FUNCTION  - propagateCollapse  *************************************************************************************//
	public int propagateCollapse(int buildChance){
		//int buildChance= buildingRule==null ? 100 : buildingRule.chance;
		int xLim=buffer.length, zLim=buffer[0].length, yLim=buffer[0][0].length;

		//now do a support calculation
		int[][][] support=new int[xLim][zLim][yLim];
		for(int x=0;x<xLim;x++) for(int z=1; z<zLim; z++) for(int y=0;y<yLim;y++) support[x][z][y]=0;

		for(int x=0;x<xLim;x++) for(int y=0;y<yLim;y++) 
			if(buffer[x][0][y][0]>0 && buffer[x][0][y][0]!=HOLE_ID) support[x][0][y]=2;

		for(int z=1; z<zLim; z++){
			boolean levelCollapsed=true;
			for(int x=0;x<xLim;x++){ for(int y=0;y<yLim;y++){
				if(buffer[x][z][y][0]>0 && buffer[x][z][y][0]!=HOLE_ID && IS_LOAD_TRASMITER_BLOCK[buffer[x][z-1][y][0]] && support[x][z-1][y]>0) {
					support[x][z][y]=2; 
					levelCollapsed=false;
			}}}
			if(levelCollapsed) return z;

			for(int m=0;m<4;m++){
				for(int x=0;x<xLim;x++){ for(int y=0;y<yLim;y++){
					if(buffer[x][z][y][0]>0 && support[x][z][y]==0){
						int neighbors=0;
						if(x<xLim-1 && IS_LOAD_TRASMITER_BLOCK[buffer[x+1][z][y][0]]) neighbors+=support[x+1][z][y];
						if(x>0 && IS_LOAD_TRASMITER_BLOCK[buffer[x-1][z][y][0]]) neighbors+=support[x-1][z][y];
						if(y<yLim-1 && IS_LOAD_TRASMITER_BLOCK[buffer[x][z][y+1][0]]) neighbors+=support[x][z][y+1];
						if(y>0 && IS_LOAD_TRASMITER_BLOCK[buffer[x][z][y-1][0]]) neighbors+=support[x][z][y-1];
						if(neighbors>random.nextInt(4)) support[x][z][y]=1;
					}
				}}
			}


			//remove blocks if no support
			for(int x=0;x<xLim;x++){ for(int y=0;y<yLim;y++) {
				if(support[x][z][y]==0 && buffer[x][z][y][0]!=Building.PRESERVE_ID) buffer[x][z][y]=HOLE_BLOCK_LIGHTING;
				//else buffer[x][z][y]=new int[]{40+support[x][z][y],0};
			}}
		}
		return zLim;
	}
	
	
 
}


