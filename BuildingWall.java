package generator.mods;

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
 * BuildingWall plans and builds a wall that flows along Minecraft's terrain.
 */
public class BuildingWall extends Building
{
	public final static boolean DEBUG=false;
	public final static boolean DEBUG_SIGNS=false;
	
	public final static int BUILDDOWN=12;
	public final static int SEARCHDOWN=2, MIN_SEARCHUP=2, MAX_SEARCHUP=5;
	public final static int DEFAULT_LOOKAHEAD=5, MIN_BRANCH_IMPROVEMENT=15;
	public final static int MAX_BACKTRACK_DEPTH=2;
	public final static int OVERHEAD_CLEARENCE=4, OVERHEAD_TREE_CLEARENCE=8;
	public final static int NO_GATEWAY=-1, NO_MIN_J=-1;
	private final static int MIN_GATEWAY_ROAD_LENGTH=20;
	
	//failCode values
	private final static int NO_FAIL=0, FAIL_OBSTRUCTED=1, FAIL_UNDERWATER=2, FAIL_TOO_STEEP_DOWN=3, FAIL_TOO_STEEP_UP=4,
	                         FAIL_HIT_WALL=5,FAIL_CANNOT_EXPLORE=6,FAIL_HIT_TARGET=7, FAIL_MAX_LENGTH=8;

	//**** WORKING VARIABLES **** 
	public int i1,j1,k1;
	public int n0=0;
	public int WalkHeight; //this is absolute, same as WallStyle
	public int maxLength;
	public int[] xArray, zArray;
	public int gatewayStart=NO_GATEWAY, gatewayEnd=NO_GATEWAY;
	public TemplateWall ws;

	public boolean target=false,circular=false;
	public int x_targ, z_targ, y_targ;
	public int minJ=NO_MIN_J;
	private boolean hitMaxDepth=false;
	public int failCode=NO_FAIL;
	public TemplateTML endBTemplate=null; //either a template or DEFAULT_TOWER
	public int endBLength=0; //length of end tower
	private int halfStairValue=2; //metavalue of half step based on bRule
	public int roofStyle;
	public TemplateRule towerRule,roofRule;
	public int Backtrack;

	//****************************************  CONSTRUCTORS - BuildingWall  *************************************************************************************//
	public BuildingWall(int ID_, WorldGeneratorThread wgt_,TemplateWall ws_,int dir_,int axXHand_, int maxLength_,boolean endTowers,int i1_,int j1_, int k1_){
		super(ID_,wgt_,ws_.rules[ws_.template[0][0][ws_.WWidth/2]],dir_,axXHand_, false, new int[]{ws_.WWidth,ws_.WHeight,0}, new int[]{i1_,j1_,k1_});
		constructorHelper(ws_,maxLength_,i1_,j1_,k1_);
		pickTowers(random.nextFloat() < ws.CircularProb,endTowers);
		Backtrack=wgt.BacktrackLength;
		if(maxLength>0){
			xArray[0]=0;
			zArray[0]=0;
		}
	}
	
	public BuildingWall(int ID_, WorldGeneratorThread wgt_,TemplateWall ws_,int dir_,int axXHand_, int maxLength_,boolean endTowers,int[] sourcePt){
		super(ID_,wgt_,ws_.rules[ws_.template[0][0][ws_.WWidth/2]],dir_,axXHand_, false, new int[]{ws_.WWidth,ws_.WHeight,0}, sourcePt);
		constructorHelper(ws_,maxLength_,sourcePt[0],sourcePt[1],sourcePt[2]);
		pickTowers(random.nextFloat() < ws.CircularProb,endTowers);
		Backtrack=wgt.BacktrackLength;
		if(maxLength>0){
			xArray[0]=0;
			zArray[0]=0;
		}
	}


	public BuildingWall(BuildingWall bw, int maxLength_,int i1_,int j1_, int k1_){
		super(bw.bID,bw.wgt, bw.bRule,bw.bDir,bw.bHand, false,new int[]{bw.bWidth,bw.bHeight,0}, new int[]{i1_,j1_,k1_});
		constructorHelper(bw.ws,maxLength_,i1_,j1_,k1_);
		Backtrack=bw.Backtrack;
		target=bw.target;
		x_targ=bw.x_targ;
		z_targ=bw.z_targ;
		y_targ=bw.y_targ;
	}
	
	private void constructorHelper(TemplateWall ws_,int maxLength_,int i1_,int j1_, int k1_){
		i1=i1_;
		j1=j1_;
		k1=k1_;
		ws=ws_;
		WalkHeight=ws.WalkHeight;
		maxLength=maxLength_;
		xArray=new int[maxLength];
		zArray=new int[maxLength];
		bLength=0;
		halfStairValue=blockToStepMeta(bRule.primaryBlock);
	}
	
	private void pickTowers(boolean circular_, boolean endTowers){
		circular=circular_;
		if(ws!=null){
			roofStyle=ws.pickRoofStyle(circular,random);
			towerRule=ws.TowerRule.getFixedRule(random);
			roofRule=ws.getRoofRule(circular);
			if(roofRule!=TemplateRule.RULE_NOT_PROVIDED) roofRule=roofRule.getFixedRule(random);
			if(endTowers && ws.MakeEndTowers){
				endBTemplate=ws.buildings.get(Building.pickWeightedOption(random,ws.buildingWeights[0],ws.buildingWeights[1]));
				endBLength = endBTemplate== ws.makeDefaultTower ? ws.pickTWidth(circular,random)+1 //+1 allows some extra wiggle room for roof edges etc.
							:(endBTemplate== ws.makeCARuin      ? ws.CARuinContainerWidth
							:									  endBTemplate.length);
			}
		}
	}

	
	//****************************************  FUNCTION  - setTowers *************************************************************************************//

	public BuildingWall setTowers(BuildingWall bw){
		circular=bw.circular;
		roofStyle=bw.roofStyle;
		towerRule=bw.towerRule;
		roofRule=bw.roofRule;
		endBLength=bw.endBLength;
		return this;
	}
	
	//****************************************  FUNCTION  - setMinJ *************************************************************************************//
	public BuildingWall setMinJ(int minJ_){
		minJ=minJ_;
		return this;
	}
	

	//****************************************  FUNCTION  - setCursor  *************************************************************************************//
	//Sets building class cursor to wall origin
	public void setCursor(int n){
		n0=n;
		if(n0>=0 && (n0<bLength || bLength==0)){
			setOriginLocal(i1,j1,k1,bLength==0 ? 0:xArray[n0],bLength==0 ? 0:zArray[n0],n0);
		}
	}
	
	//****************************************  FUNCTION  - getIJKPtAtN *************************************************************************************//
	public int[] getIJKPtAtN(int n, int x, int z, int y){
		if(n==n0) return getIJKPt(x,z,y);
		return getIJKPt(x+xArray[n]-xArray[n0],z+zArray[n]-zArray[n0],y+n-n0);
	}

	//****************************************  FUNCTION  - setTarget  *************************************************************************************//
	//Sets a target coordinate that the plan function can use to path towards
	//Will change EW and axY to reflect direction to target.
	//RETURNS; true if target is acceptable and reachable.
	
	public boolean setTarget(int[] targ){
		if( targ[1] > 20 && Math.abs(j1-targ[1]) < Math.max(Math.abs(i1-targ[0]),Math.abs(k1-targ[2])) ){
			target=true;
			setPrimaryAx( Math.abs(i1-targ[0]) > Math.abs(k1-targ[2])
						  	? (targ[0] > i1 ? DIR_EAST:DIR_WEST) 
						    : (targ[2] > k1 ? DIR_SOUTH:DIR_NORTH));
			setCursor(0);
			x_targ=getX(targ);
			z_targ=getZ(targ);
			y_targ=getY(targ);
			//if(DEBUG) FMLLog.getLogger().info("Set target for "+IDString()+"to "+localCoordString(x_targ,z_targ,y_targ)+"!");
		}
		//else if(DEBUG) FMLLog.getLogger().info("Could not set target for "+IDString()+", targ="+globalCoordString(targ)+" (i,j,k)="+globalCoordString(i1,j1,k1));
		return target;
	}
	
	public boolean ptIsToXHand(int[] pt, int buffer){
		setCursor(0);
		if(ws.TowerXOffset < 0) buffer-=ws.TowerXOffset;
		int ptY=getY(pt);
		if(ptY<0) return getX(pt)>=buffer;
		if(ptY>=bLength) return getX(pt)>=xArray[bLength-1]+buffer;
		return getX(pt)>=xArray[ptY]+buffer;
	}


	//****************************************  FUNCTION  - plan *************************************************************************************//
	//ASSUMPTIONS:
	//xarray and zarray contain planned values up to startN-1 inclusive.
	//RETURNS:
	//Length of new wall planned.
	//SIDE EFFECTS:
	//planL set to total length now planned.
	//xarry and zarry are filled up to planL.
	//hitMaxDepth true if planning was terminated due to depth==MAX_BACKTRACK_DEPTH.
	//failString contains termination rationale.
	
	public int plan(int startN, int depth, int lookahead, boolean stopAtWall) throws InterruptedException {
		if(startN<1 || startN >=maxLength) {System.err.println("Error, bad start length at BuildingWall.plan:"+startN+"."); return 0; }
		int fails=0;
		setOriginLocal(i1,j1,k1,xArray[startN-1],zArray[startN-1],startN);
		bLength=startN;
		
		//if(DEBUG && depth > 0) FMLLog.getLogger().info("planWall "+IDString()+", depth="+depth+" n="+startN+" maxlLen="+maxLength+" at "+globalCoordString(i0,j0,k0));
		//int searchUp=Math.min(Math.max(MIN_SEARCHUP,WalkHeight+1),MAX_SEARCHUP);
		int searchUp=MIN_SEARCHUP;
		int obstructionHeight=WalkHeight >4 ? WalkHeight+1 : bHeight+1;

		while(true){
			int gradx=0,gradz=0;
			failCode=NO_FAIL;
			//query the exploration handler to see if we have reached limit, if so then terminate
			if(!(queryExplorationHandlerForChunk(-1,0,0) && queryExplorationHandlerForChunk(bWidth,0,0))) {
				failCode=FAIL_CANNOT_EXPLORE;
				break; 
			}
			for(int x1=-1; x1<=bWidth;x1++){
				for(int z1=-SEARCHDOWN; z1<=searchUp; z1++){
					int blockId=getBlockIdLocal(x1, z1, 0);
					if(!IS_WALLABLE[blockId]){//some ArrayIndexOutofBound here ?
						gradz++;
						gradx+=signum(2*x1-bWidth+1); 
					}
					else if(IS_WATER_BLOCK[blockId])
						gradx-=signum(2*x1-bWidth+1);

					//hit another wall, want to ignore sandstone that appears naturally in deserts
					if((stopAtWall || z1 < -2) && isArtificialWallBlock(x1,z1,0))
						failCode=FAIL_HIT_WALL;
				}
				if(IS_WATER_BLOCK[getBlockIdLocal(x1,ws.waterHeight+1,0)])
					failCode=FAIL_UNDERWATER;
				if(!isWallable(x1,obstructionHeight,0) && failCode==NO_FAIL)
					failCode=FAIL_OBSTRUCTED;
				
			}

			gradz=(gradz+(bWidth+2)/2)/(bWidth+2)-SEARCHDOWN;
			if(failCode==FAIL_HIT_WALL) gradz=0;
			if(failCode==NO_FAIL && gradz < -1) failCode=FAIL_TOO_STEEP_DOWN;
			if(failCode==NO_FAIL && gradz > 4) failCode=FAIL_TOO_STEEP_UP;
			
			gradz=signum(gradz, 0);
			if(minJ!=NO_MIN_J && zArray[bLength-1]+gradz+j1 < minJ) gradz=0; //don't go below minJ
			if(gradz==0){
				int HorizForceThreshold=bWidth/2;
				int bias= target ? signum(xArray[bLength-1]-x_targ)*(2*HorizForceThreshold) : 0;
				gradx= (gradx > HorizForceThreshold + bias ? 1 : (gradx < -HorizForceThreshold + bias ? -1 : 0));
			} else gradx=0;
			
			setOriginLocal(i0,j0,k0,gradx,gradz,1);
			
			xArray[bLength]=xArray[bLength-1]+gradx;
			zArray[bLength]=zArray[bLength-1]+gradz;
			bLength++;


			//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%   TERMINATION / BACKTRACKING   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
			if(failCode==NO_FAIL) fails=0;
			else fails++;

			if(target && bLength > y_targ){
				failCode=FAIL_HIT_TARGET;
				break;
			}
			else if(bLength>=maxLength){
				failCode=FAIL_MAX_LENGTH;
				break;
			}
			else if(failCode==FAIL_HIT_WALL || failCode==FAIL_UNDERWATER){
				bLength-=fails;
				break;
			}
			else if(fails>=lookahead){
				bLength-=fails; //planL should be at first failed position at end of loop

				if( bLength-startN < Backtrack || (bLength-startN < MIN_BRANCH_IMPROVEMENT && depth!=0) ){
				
					break; //loop termination condition 2
				}
				if(depth >= MAX_BACKTRACK_DEPTH ){ //loop termination condition 2
					hitMaxDepth=true; //may still be able to proceed, note this so we can do so from root
					
					break; //loop termination condition 3
				}
				else{
					//if(DEBUG) FMLLog.getLogger().info("\nTrying branches for "+IDString()+", depth="+depth+" at n="+bLength+" x="+(xArray[bLength])+" z="+(zArray[bLength]));
					int improvement, bestImprovement=0;
					BuildingWall branch, bestBranch=null;
					//String[] branchNames={"Down","Minus","Straight","Plus","Up"};

					for(int zAx=0;zAx<=1;zAx++){
						for(int d=-1;d<=1;d++){
							if(!(zAx==0 && d==0)){


								branch=new BuildingWall(this, maxLength, i1, j1, k1);
								for(int m=0;m<Backtrack;m++){
									branch.xArray[bLength-Backtrack+m]=xArray[bLength-Backtrack]+(1-zAx)*(d*m);
									branch.zArray[bLength-Backtrack+m]=zArray[bLength-Backtrack]+zAx*(d*m);
								}
								improvement=branch.plan(bLength, depth+1,lookahead, stopAtWall);
								if(improvement > bestImprovement){
									bestBranch=branch;
									bestImprovement=improvement;
								}

							}
						}
					}
					if(bestImprovement+bLength>maxLength) bestImprovement=maxLength-bLength;
					if(bestImprovement > 0){
						//if(DEBUG==3) System.out.println("Chose branch="+bestBranch.branchName+" for wall "+IDString()+"depth="+depth+" at n="+planL+" with added length="+bestImprovement);
						for(int m=bLength-Backtrack;m<bLength+bestImprovement;m++){
							xArray[m]=bestBranch.xArray[m];
							zArray[m]=bestBranch.zArray[m];
							//failString=bestBranch.failString;
							failCode=bestBranch.failCode;

						}
						hitMaxDepth=bestBranch.hitMaxDepth;
						bLength+=bestImprovement;
					}
					//else if(DEBUG) FMLLog.getLogger().info("Could not improve wall "+IDString()+" at n="+bLength+"\n");

					if(depth==0 && hitMaxDepth && bLength<maxLength){
						hitMaxDepth=false;
						fails=1;
						//if(DEBUG) FMLLog.getLogger().info("Hit max search depth, continuing planning wall "+IDString()+"at n="+bLength+" from root");
					}
					else {
						break; //we have added branches if any and did not hit max depth, so break
					}
				}
				//if(DEBUG && planL>startN) printWall(startN);
			}
		}//end main loop
		
		if(depth==0){
			bLength-=endBLength;
			if(bLength<startN) bLength=startN;
		}

		setCursor(0);
		return bLength-startN;
	}
	
	public boolean queryLayout(int layoutCode){
		for(int n=0;n<bLength;n++){
			setCursor(n);
			if(!wgt.layoutIsClear(getIJKPt(0,0,0), getIJKPt(bWidth-1,0,0),layoutCode)){
				setCursor(0);
				return false;
			}
		}
		setLayoutCode(layoutCode);
		return true;
	}
	
	public void setLayoutCode(int layoutCode){
		for(int n=0;n<bLength;n++){
			setCursor(n);
			wgt.setLayoutCode(getIJKPt(0,0,0), getIJKPt(bWidth-1,0,0), layoutCode);
		}
		setCursor(0);
	}

	//****************************************  FUNCTION - buildFromTML*************************************************************************************//
	//Builds a planned wall from a template
	
	public void buildFromTML(){
		if(ws==null){ 
			System.err.println("Tried to build wall from template but no template was given!");
			return; 
		}
		setCursor(0);

		
		if(bLength>0) if(DEBUG)System.out.println("**** Built "+ws.name+" wall "+IDString()+", length " + (bLength) + " from "+localCoordString(xArray[0], zArray[0], 0)+
					" to "+localCoordString(xArray[bLength-1], zArray[bLength-1], bLength-1)+" ****");
		else if(DEBUG)System.out.println("**** Wall too short to build! "+IDString()+"length="+bLength+" at "+localCoordString(0,0,0)+" ****");
		if(DEBUG)System.out.println("Wall planning was terminated due to: "+failString()+"\n");
		

		int lN=0;
		int[] idAndMeta;
		int layer[][];

		//get named layers
		int base[]=ws.template[0][0]; //defaults to bottom line of first layer
		if(ws.namedLayers.containsKey("base")) base=(ws.namedLayers.get("base"))[ws.length-1];
		int[][] shifted, shiftedLeft, shiftedRight, shiftedUp, shiftedDown;
		shifted=ws.namedLayers.containsKey("shifted") ? ws.namedLayers.get("shifted") : ws.template[0];
		shiftedLeft=ws.namedLayers.containsKey("shifted_left") ? ws.namedLayers.get("shifted_left") : shifted;
		shiftedRight=ws.namedLayers.containsKey("shifted_right") ? ws.namedLayers.get("shifted_right") : shifted;
		shiftedUp=ws.namedLayers.containsKey("shifted_up") ? ws.namedLayers.get("shifted_up") : shifted;
		shiftedDown=ws.namedLayers.containsKey("shifted_down") ? ws.namedLayers.get("shifted_down") : shifted;


		for(setCursor(0); n0<bLength; setCursor(n0+1)){	
			if(n0==0) layer=shifted;
			else if(xArray[n0-1]<xArray[n0]) layer=shiftedRight;
			else if(xArray[n0-1]>xArray[n0]) layer=shiftedLeft;
			else if(zArray[n0-1]<zArray[n0]) layer=shiftedUp;
			else if(zArray[n0-1]>zArray[n0]) layer=shiftedDown;
			else if(n0==bLength-1 || xArray[n0+1]!=xArray[n0] || zArray[n0+1]!=zArray[n0]) layer=shifted;
			else layer=ws.template[lN];

			if(layer==ws.template[lN]) lN=(lN+1) % ws.height;
			else lN=0;

			//wall
			for(int x1=0; x1<bWidth;x1++){
				boolean keepWallFromAbove=true;
				for(int z1=bHeight+OVERHEAD_CLEARENCE-1; z1>=-ws.embed; z1--){
					boolean wallBlockPresent=isWallBlock(x1,z1,0);
					idAndMeta= z1<bHeight 
								? ws.rules[layer[z1+ws.embed][x1]].getBlockOrHole(random) 
								: HOLE_BLOCK_NO_LIGHTING;

					//starting from top, preserve old wall block until we run into a non-wall block
					if(keepWallFromAbove && wallBlockPresent && (idAndMeta[0]==AIR_ID || idAndMeta[0]==HOLE_ID)){
						continue;
					} else keepWallFromAbove=false;
										
					if(idAndMeta[0]==WALL_STAIR_ID){
						if(!wallBlockPresent && !IS_WATER_BLOCK[getBlockIdLocal(x1,z1,0)]){
							if(n0>0 && zArray[n0-1]>zArray[n0]){  //stairs, going down
								if((n0==1 || zArray[n0-2]==zArray[n0-1]) && (n0==bLength-1 || zArray[n0]==zArray[n0+1]))
									setBlockLocal(x1, z1, 0, STEP_ID, idAndMeta[1]);
								else setBlockLocal(x1, z1, 0, STEP_TO_STAIRS[idAndMeta[1]],2);
							}
							else if(n0<bLength-1 && zArray[n0]<zArray[n0+1]){ //stairs, going up
								if((n0==0 || zArray[n0-1]==zArray[n0]) && (n0==bLength-2 || zArray[n0+1]==zArray[n0+2]))
									setBlockLocal(x1, z1, 0, STEP_ID, idAndMeta[1]);
								else setBlockLocal(x1, z1, 0, STEP_TO_STAIRS[idAndMeta[1]],3);
							}
							else setBlockLocal(x1,z1,0,HOLE_ID);
						}
					}else{ //not a stair
						// if merging walls, don't clutter with crenelations etc.
						if(z1>=WalkHeight && ( x1==0 &&        (wallBlockPresent || isWallBlock(-1,WalkHeight-1,0) || isWallBlock(-1,WalkHeight-2,0)) 
						                 ||    x1==bWidth-1 && (wallBlockPresent || isFloor(bWidth,WalkHeight-1,0) || isWallBlock(bWidth,WalkHeight-2,0))) ){  
							continue;
						}
						
						if(idAndMeta[0]==HOLE_ID && z1<bHeight) setBlockWithLightingLocal(x1,z1,0,HOLE_ID,0,true); //force lighting update for holes
						else setBlockLocal(x1,z1,0,idAndMeta);  //straightforward build from template
					}
					
				}
			}
			//base
			for(int x1=0; x1<bWidth;x1++) 
				buildDown(x1,-1-ws.embed,0,ws.rules[base[x1]],ws.leveling,Math.min(2,ws.embed),3);


			clearTrees();
			mergeWallLayer();

			//DEBUGGING, creates signs with ID/distance info
			/*if(DEBUG_SIGNS && (n0) % 10==0){
				//String[] lines=new String[]{IDString().split(" ")[0],IDString().split(" ")[1],"Dist:"+n+ " / "+planL,globalCoordString(1,WalkHeight,0)};
				String[] lines=new String[]{IDString().split(" ")[0],xArray[n0]+"","Dist:"+n0+ " / "+bLength,localCoordString(1,WalkHeight,0)};
				setSignOrPost(1,WalkHeight,0,true,8,lines);
				setSignOrPost(-1,WalkHeight-1,0,false,3,lines);
				setSignOrPost(bWidth,WalkHeight-1,0,false,2,lines);
			}*/

		}//end main loop
		
		flushDelayed();

		setCursor(0);
	}

	//****************************************  FUNCTION - clearTrees *************************************************************************************//
	private void clearTrees(){
		for(int x1=0; x1<bWidth;x1++)
			for(int z1=bHeight+OVERHEAD_CLEARENCE; z1<bHeight+OVERHEAD_TREE_CLEARENCE; z1++)
				if(getBlockIdLocal(x1, z1, 0)==LOG_ID || getBlockIdLocal(x1, z1, 0)==LEAVES_ID || getBlockIdLocal(x1, z1, 0)==SNOW_ID )
					setBlockLocal(x1, z1, 0, AIR_ID); //kill trees aggressively
	}

	//****************************************  FUNCTION - mergeWallLayer *************************************************************************************//
	private void mergeWallLayer(){
		//if side is a floor one below, add a step down
		if(isFloor(-1,WalkHeight-1,0))   setBlockLocal(-1, WalkHeight-1, 0, STEP_ID, halfStairValue);
		if(isFloor(bWidth,WalkHeight-1,0))   setBlockLocal(bWidth, WalkHeight-1, 0, STEP_ID, halfStairValue);
		
		//      x
		// if  xxo are floors one above, add a step up
		//      x
		if(isFloor(-1,WalkHeight+1,0)  && isFloor(-2,WalkHeight+2,0) && isFloor(-2,WalkHeight+2,1)  && isFloor(-2,WalkHeight+2,-1)) 
			setBlockLocal(0, WalkHeight, 0, STEP_ID,halfStairValue);
		if(isFloor(bWidth,WalkHeight+1,0)  && isFloor(bWidth+1,WalkHeight+2,0) && isFloor(bWidth+1,WalkHeight+2,1)  && isFloor(bWidth+1,WalkHeight+2,-1)) 
			setBlockLocal(bWidth-1, WalkHeight, 0, STEP_ID, halfStairValue);

		//clean up stairs descending into this wall
		int[] pt=getIJKPt(-1,WalkHeight-1,0);
		if(IS_STAIRS_BLOCK[world.getBlockId(pt[0],pt[1],pt[2])] && STAIRS_META_TO_DIR[world.getBlockMetadata(pt[0],pt[1],pt[2])]==rotDir(bDir,-bHand))
			world.setBlock(pt[0],pt[1],pt[2], stairToSolidBlock(world.getBlockId(pt[0],pt[1],pt[2])));
		pt=getIJKPt(bWidth,WalkHeight-1,0);
		if(IS_STAIRS_BLOCK[world.getBlockId(pt[0],pt[1],pt[2])] && STAIRS_META_TO_DIR[world.getBlockMetadata(pt[0],pt[1],pt[2])]==rotDir(bDir,bHand))
			world.setBlock(pt[0],pt[1],pt[2], stairToSolidBlock(world.getBlockId(pt[0],pt[1],pt[2])));
	}


	//****************************************  FUNCTION - makeBuildings *************************************************************************************//
	public void makeBuildings(boolean buildOnL,boolean buildOnR, boolean makeGatehouseTowers, boolean overlapTowers, boolean isAvenue) throws InterruptedException{
		if(ws==null){
			System.err.println("Tried to build towers but wall style was null!");
			return;
		}
		if(!ws.MakeBuildings) return;

		makeGatehouseTowers=makeGatehouseTowers && ws.makeDefaultTower.weight>0 && !circular;
		int cursorStart=Math.max(ws.getTMaxWidth(circular)+3,2*ws.BuildingInterval/3);
		
		//main loop
		for(setCursor(cursorStart); n0<bLength; setCursor(n0+1)){
			
			//don't built if there's a gateway
			if(gatewayStart!=NO_GATEWAY && n0>=gatewayStart && n0<=gatewayEnd+ws.getTMaxWidth(circular)+2){ 
				setCursor(gatewayEnd+ws.getTMaxWidth(circular)+2);
				if(n0>=bLength) break;
			}
			
			//tw is used to see if curvature permits building a tower here. Non default tower buildings will also
			//use this tw for curvature even though it is incorrect since they will determine their own width.
			//This may cause so building-over but that's OK.
			//tw is also passed to  as the actual width for default towers inside makeBuilding().
			int tw=ws.pickTWidth(circular,random);
			
			//towers are built from n0-2 to n0-tw-1
			//n0 and nBack used to calculat curvature are 2 further from nMid
			int nMid=n0-tw/2-2, nBack=n0-tw-3;
			
			int clearSide=-bHand*signum(curvature(xArray[nBack], xArray[nMid], xArray[n0], 0),0);
			if(clearSide==0){
				if(buildOnL && buildOnR) clearSide=2*random.nextInt(2)-1;
				else clearSide= buildOnL ? L_HAND : R_HAND;
			}
			
			//try tower types
			if(makeGatehouseTowers && curvature(zArray[nBack], zArray[nMid], zArray[n0], 0)==0 
								   && curvature(xArray[nBack], xArray[nMid], xArray[n0], 2)==0){
				//FMLLog.getLogger().info("Building gatehouse for "+IDString()+" at n="+n0+" "+globalCoordString(0,0,0)+" width "+tw);
				
				BuildingTower tower = new BuildingTower(bID+n0, this, flipDir(bDir), -bHand, true, tw, ws.pickTHeight(circular,random),
													circular ? tw:ws.pickTWidth(circular,random), getIJKPtAtN(nMid,bWidth/2,0,tw/2));
				if(!tower.isObstructedRoof(-1)){
					wgt.setLayoutCode(tower.getIJKPt(0,0,0),tower.getIJKPt(tw-1,0,tw-1), WorldGeneratorThread.LAYOUT_CODE_TOWER);
					tower.build(xArray[n0-1]-xArray[nMid], xArray[nBack+1]-xArray[nMid], false);
					
					setCursor(n0+ws.BuildingInterval-1);
				}
			}
			else if((buildOnL && clearSide==L_HAND) || (buildOnR && clearSide==R_HAND)){   //side towers
				//FMLLog.getLogger().info("Building side tower for "+IDString()+" at n="+n0+" "+globalCoordString(0,0,0)+" with clearSide="+clearSide+" width "+tw);
				
       			TemplateTML template=ws.buildings.get(Building.pickWeightedOption(random,ws.buildingWeights[0],ws.buildingWeights[1]));
       			int ybuffer= - ws.TowerXOffset + (isAvenue ? 0:1);
				int[] pt=getIJKPtAtN(nMid, clearSide==bHand ? (bWidth - ybuffer):ybuffer-1, 0, 0);
				if(makeBuilding(template,tw,ybuffer,overlapTowers,rotDir(bDir,clearSide),pt))
					setCursor(n0+ws.BuildingInterval-1);
			}
		}
		setCursor(0);

		//build towers at endpoints
		if(endBLength >= BuildingTower.TOWER_UNIV_MIN_WIDTH){
			int endTN = circular ? bLength-2:bLength-1;
			if(endTN<0) endTN=0;
			int[] pt=getIJKPtAtN(endTN, bWidth/2, 0, 1);
			makeBuilding(endBTemplate,ws.pickTWidth(circular,random),1,overlapTowers,bDir,pt);
		}
	}
	
	//****************************************  FUNCTION - makeBuildings *************************************************************************************//
	private boolean makeBuilding(TemplateTML template,int tw, int ybuffer, boolean overlapTowers, int dir, int[] pt) throws InterruptedException{
		if(template==ws.makeDefaultTower){
			int maxBL = bDir==dir ? endBLength 
							      : circular ? tw: ws.pickTWidth(circular,random);
			//FMLLog.getLogger().info("Querying "+(circular? "circular " : "square ")+(bDir==dir ? "end" : "side")+" tower, ybuffer="+ybuffer+".");
			for(int tl=maxBL; tl>=ws.getTMinWidth(circular); tl--){
				BuildingTower tower=new BuildingTower(bID+n0,this,dir,1,true,circular ? tl:tw,ws.pickTHeight(circular,random),tl,pt);
				if(tower.queryCanBuild(ybuffer,overlapTowers)){
					tower.build(0,0,true);
					return true;
				}
			}
		}
		else if(template==ws.makeCARuin){
			byte[][] caRule=ws.CARuinAutomataRules.get(random.nextInt(ws.CARuinAutomataRules.size()));
			for(int tries=0; tries < 10; tries++){
				byte[][] seed = BuildingCellularAutomaton.makeSymmetricSeed(ws.CARuinContainerWidth,0.5F,random);
				BuildingCellularAutomaton bca=new BuildingCellularAutomaton(wgt,ws.CARuinRule,dir,1,true,ws.CARuinContainerWidth,
									ws.CARuinMinHeight+random.nextInt(ws.CARuinMaxHeight - ws.CARuinMinHeight+1),ws.CARuinContainerWidth, seed,caRule,null,pt);
				if(bca.plan(false,12) && bca.queryCanBuild(ybuffer,ws.CARuinContainerWidth<=15)){
					bca.build(true,true);
					return true;
				}
			}
			
			//We've failed. If an end building, try making a tower instead 
			if(bDir==dir && ws.makeDefaultTower.weight>0){
				return makeBuilding(ws.makeDefaultTower,tw,ybuffer,overlapTowers,dir,pt);
			}
		}
		else{
			BuildingTML buildingTML=new BuildingTML(bID+n0,wgt,dir,1,true,template,pt);
			if(buildingTML.queryCanBuild(ybuffer)){
				buildingTML.build();
				return true;
			}
			
			//We've failed. If an end building, try making a tower instead 
			if(bDir==dir && ws.makeDefaultTower.weight>0){
				return makeBuilding(ws.makeDefaultTower,tw,ybuffer,overlapTowers,dir,pt);
			}
		}
		return false;
	}






	//****************************  FUNCTION  - buildGateway  *************************************************************************************//
	//Builds a gateway and road on one side of gateway. Call after build() and before buildTowers().
	//
	//PARAMETERS:
	//startScan,endScan - bounds of where to look to place gateway
	//gateHeight, gateWidth - dimensions of the gateway in the wall
	//rs - wall style of avenues
	//flankTHand - the hand to build flanking towers on. 0 => n0 flanking towers.
	//XMaxLen, antiXMaxLen - maximum length of avenues for the +X and -X side avenues
	//XTarget, antiXTarget - the target point for the +X and -X side avenues
	//XHand, antiXHand - the internal handedness of the +X and -X side avenues.
	//
	//RETURNS:
	//y-position where gateway was build or -1 if no gateways was built
	//
	public BuildingWall[] buildGateway(int[] scanWindow, int scanStart, int gateHeight,int gateWidth,TemplateWall rs,int flankTHand,
			int XMaxLen,int[] XTarget,int XHand, int antiXMaxLen, int[] antiXTarget, int antiXHand) throws InterruptedException {
		BuildingWall[] avenues=null;
		if(rs!=null) gateWidth=rs.WWidth;
		if(scanStart < scanWindow[0]) scanStart=scanWindow[0];
		if(scanStart > scanWindow[1]) scanStart=scanWindow[1];
		int scanA=scanStart, scanB=scanStart+1;
		
		for(boolean aOrB=true; scanA>=scanWindow[0] || scanB<=scanWindow[1]; aOrB=!aOrB){
			setCursor(aOrB ? scanA : scanB);
			if(aOrB){ 
				if(scanA<scanWindow[0]) continue;
				scanA-=3;
			}else{
				if(scanB>scanWindow[1]) continue;
				scanB+=3;
			}
			if (n0-gateWidth-1>=0) {
			if(curvature(zArray[n0], zArray[n0-gateWidth/2], zArray[n0-gateWidth-1], 1)==0 &&
			   curvature(xArray[n0], xArray[n0-gateWidth/2], xArray[n0-gateWidth-1], 0)==0)
			{
				int tw=ws.pickTWidth(circular,random), th=ws.getTMaxHeight(circular);
				if(rs!=null){ 
					avenues=new BuildingWall[]{ new BuildingWall(bID,wgt,rs,rotDir(bDir,bHand),XHand, XMaxLen,false,getIJKPt(bWidth,0,XHand==-bHand ? 1-gateWidth :0))
					         ,new BuildingWall(bID,wgt,rs,rotDir(bDir,-bHand),antiXHand, antiXMaxLen,false,getIJKPt(-1,0,antiXHand==bHand ? 1-gateWidth :0))};

					avenues[0].setTarget(XTarget==null ? getIJKPt(bWidth+tw,0,XHand==-bHand ? 1-gateWidth :0) : XTarget);
					avenues[0].plan(1,0,DEFAULT_LOOKAHEAD,true);
					if(XTarget==null && avenues[0].bLength>=tw){
						avenues[0].target=false;
						avenues[0].plan(tw+1,0,DEFAULT_LOOKAHEAD,true);
					}
			
					if(avenues[0].bLength>=MIN_GATEWAY_ROAD_LENGTH){
						avenues[1].setTarget(antiXTarget==null ? getIJKPt(-1-tw,0,antiXHand==bHand ? 1-gateWidth :0) : antiXTarget);
						avenues[1].plan(1,0,DEFAULT_LOOKAHEAD,true);
						if(antiXTarget==null && avenues[1].bLength>=tw){
							avenues[1].target=false;
							avenues[1].plan(tw+1,0,DEFAULT_LOOKAHEAD,true);
						}
					}
				}
				
				//build it
				//gateway is built from n0-gateWidth+1 to n0	
				if(rs==null ||  avenues[1].bLength>=MIN_GATEWAY_ROAD_LENGTH){ 
					if(rs!=null){
						avenues[0].smooth(10,10,false);
						avenues[1].smooth(10,10,false);
					}
					
					int fenceBlock = bRule.chance<100 || bRule.primaryBlock[0]==NETHER_BRICK_ID ? NETHER_BRICK_FENCE_ID : FENCE_ID;
					int fenceX = flankTHand==0 ? bWidth/2 : (flankTHand==bHand ? bWidth-2+ws.TowerXOffset:1-ws.TowerXOffset);
					gateHeight = Math.min(gateHeight,bHeight-1);
					for(int y1=0;y1>-gateWidth;y1--){
						//gateway
						for(int x1=0;x1<bWidth;x1++)
							for(int z1=0;z1<gateHeight;z1++)
								if(!((y1==0 || y1==1-gateWidth) && z1==gateHeight-1))
									setBlockLocal(x1,z1,y1,AIR_ID);
						//fence gate
						for(int z1=gateHeight-2; z1<gateHeight; z1++)
							if(random.nextInt(100) < bRule.chance) 
								setBlockLocal(fenceX,z1,y1,fenceBlock);
					}
					if(flankTHand!=-bHand ) setBlockLocal(-1-ws.TowerXOffset,gateHeight-2,-gateWidth,WEST_FACE_TORCH_BLOCK);
					if(flankTHand!=-bHand ) setBlockLocal(-1-ws.TowerXOffset,gateHeight-2,1,WEST_FACE_TORCH_BLOCK);
					if(flankTHand!=bHand ) setBlockLocal(bWidth+ws.TowerXOffset,gateHeight-2,-gateWidth,EAST_FACE_TORCH_BLOCK);
					if(flankTHand!=bHand ) setBlockLocal(bWidth+ws.TowerXOffset,gateHeight-2,1,EAST_FACE_TORCH_BLOCK);
					
					
					//build flanking towers
					if(n0+gateWidth+tw > bLength) flankTHand=0;
					if(flankTHand!=0){
						int tnMid1=n0-gateWidth-tw/2;
						int tnMid2=n0+tw/2+1;
						int x1=flankTHand==bHand ? bWidth-1+ws.TowerXOffset : -ws.TowerXOffset;
						
						//preceding tower
						new BuildingTower(0,this, rotDir(bDir,flankTHand), bHand, true, tw, th, tw, 
											getIJKPtAtN(tnMid1,x1,0,0)).build(0,0,false);
						//following tower
						new BuildingTower(0,this, rotDir(bDir,flankTHand),-bHand, true, tw, th+zArray[tnMid1]-zArray[tnMid2], tw,
											getIJKPtAtN(tnMid2,x1,0,0)).build(0,0,false);
					}
					flushDelayed();
					
					//stairs up to wall
					gatewayStart=n0-gateWidth+1;
					gatewayEnd=n0;
					if(bWidth+2*ws.TowerXOffset>=5){
						int ngw1=n0-gateWidth, ngw2=n0+1;
						int x2=(flankTHand==0 || flankTHand==bHand) ? 1-ws.TowerXOffset : bWidth-2+ws.TowerXOffset;
						
						for(int n1=ngw1; n1>ngw1-5; n1--){
							if(zArray[n1-3]==zArray[n1] && xArray[n1-3]==xArray[ngw1+1]){
								new BuildingSpiralStaircase(wgt,bRule,bDir,bHand,false,-WalkHeight,
										getIJKPtAtN(n1,x2,WalkHeight-2,-3)).build(1,ngw1-n1+4);
								gatewayStart=n1-3;
								break;
						}}
						
						
						for(int n1=ngw2; n1<ngw2+5; n1++){
							if(zArray[n1+3]==zArray[n1] && xArray[n1+3]==xArray[ngw2-1]){
								new BuildingSpiralStaircase(wgt,bRule,flipDir(bDir),-bHand,false,-WalkHeight,
										getIJKPtAtN(n1,x2,WalkHeight-2,3)).build(1,n1-ngw2+5);
								gatewayEnd=n1+3;
								break;
						}}
					}
					
					
					gatewayStart-=(flankTHand!=0 ? tw+ws.BuildingInterval/2:0);
					gatewayEnd  +=(flankTHand!=0 ? tw+ws.BuildingInterval/2:0);
					return avenues;
				}
			}
		}
		}
		return null;
	}
	

	//=====================================================  HELPER FUNCTIONS ==========================================================================================================//   

	//****************************************  FUNCTION  - curvature  *************************************************************************************//
	//curvature(LinkedList ll, int a, int b, int c, int wiggle)
	//wiggle allows for some leeway before slope is detected
	//RETURNS: 0 if constant (000)
	//         1 if concave up (+0+),(00+),(+00)
	//        -1 if concave down (-0-),(-00),(00-)
	//         2 if increasing (-0+)
	//        -2 if decreasing (+0-)
	private static int curvature(int a, int b, int c, int wiggle){
		int d1=signum(a-b,wiggle);
		int d2=signum(c-b,wiggle);
		if(d1*d2<0) return 2*d2;
		return signum(d1+d2,0);
	}

	//****************************************  FUNCTION  - failString  *************************************************************************************//
	public String failString(){
		switch(failCode) {
		case FAIL_OBSTRUCTED: return  "Obstructed.";
		case FAIL_UNDERWATER: return "Underwater.";
		case FAIL_TOO_STEEP_DOWN: return "Too Steep Down.";
		case FAIL_TOO_STEEP_UP: return "Too Steep Up.";
		case FAIL_HIT_WALL: return "Hit Wall";
		case FAIL_CANNOT_EXPLORE: return "Could not explore";
		case FAIL_HIT_TARGET: return "Hit Target";
		case FAIL_MAX_LENGTH: return "Max length ("+maxLength+") reached.";
		}
		return "No Fail.";
	}


	//****************************************  FUNCTION  - printWall  *************************************************************************************//
	//For precise debugging
	/*public void printWall(){
		printWall(0);
	}


	public void printWall(int start){
		if(DEBUG) System.out.println("Printing "+IDString()+" wall from n="+start+" to n="+(bLength-1));
		for(int m=start; m<bLength; m++) {
			if(m %10==0) System.out.print("|");
			if(m %100==0) System.out.print("||");
			if(DEBUG) System.out.print(xArray[m]+",");
			if(m>0 && Math.abs(xArray[m]-xArray[m-1])>1 )
				if(DEBUG) System.out.print("(ERROR: X-slope>1 at n="+m+")");

		}
		if(DEBUG) System.out.print("\n");
		for(int m=start; m<bLength; m++) {
			if(m %10==0) System.out.print("|");
			if(m %100==0) System.out.print("||");
			if(DEBUG) System.out.print(zArray[m]+",");
			if(m>0 && Math.abs(zArray[m]-zArray[m-1])>1 )
				if(DEBUG) System.out.print("(ERROR: Z-slope>1 at n="+m+")");
		}
		if(DEBUG) System.out.println("\n");
	}*/

	//****************************************  FUNCTION - smooth  *************************************************************************************//
	public void smooth(int convexWindow, int concaveWindow,  boolean flattenEnds){
		smooth(xArray,0,bLength-1,convexWindow,concaveWindow, flattenEnds);
		smooth(zArray,0,bLength-1,convexWindow,concaveWindow, flattenEnds);
	}
	
	//smooth(int[] arry, int a, int b,int convexWindow, int concaveWindow)
	public static void smooth(int[] arry, int a, int b,int convexWindow, int concaveWindow, boolean flattenEnds){
		int n, smoothStart=-1, leadSlope, shorterWinStart=a, longerWinStart=a;
		int shorterWinInitEnd = a + Math.min(concaveWindow, convexWindow),
			longerWinInitEnd = a + Math.max(concaveWindow, convexWindow);
		
		for(int winEnd=a+2; winEnd<=b; winEnd++){
			if(winEnd>=shorterWinInitEnd) shorterWinStart++;
			if(winEnd>=longerWinInitEnd) longerWinStart++;	
			
			n = winEnd-1;
			leadSlope = arry[winEnd] - arry[n];

			//check the smaller window in both directions, and the larger only in the given direction
			if(leadSlope*(arry[n] - arry[shorterWinStart])<0 )
				smoothStart=shorterWinStart;
			else if( leadSlope*(arry[n] - arry[longerWinStart]) < 0 
				  && leadSlope*(convexWindow - concaveWindow) < 0 )
				smoothStart=longerWinStart;
			else
				smoothStart=-1;
			if(smoothStart>=0){
				if(DEBUG){
					System.out.print("smoothing: ");
					for(int m=smoothStart; m<= winEnd; m++) System.out.print(arry[m]+",");				
				}
				do {
					if(DEBUG) System.out.println("smoothing n="+n+" "+arry[n-1] +" "+ arry[n] +" "+ arry[winEnd]);
					arry[n]=arry[winEnd];
					n--;
				} while(n>smoothStart && arry[n]!=arry[winEnd]);
			}

		}
		
		//flatten the last two positions on each end if they end on slopes
		if(flattenEnds && b-a>=2){
			if(arry[a]!=arry[a+2])   arry[a]=arry[a+2];
			if(arry[b]!=arry[b-2])   arry[b]=arry[b-2];
			if(arry[a+1]!=arry[a+2]) arry[a+1]=arry[a+2];
			if(arry[b-1]!=arry[b-2]) arry[b-1]=arry[b-2];
			
		}
	}

}

