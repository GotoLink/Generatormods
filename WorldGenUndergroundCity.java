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
 * WorldGenUndergroundCity generates a city in a large underground cavern.
 * The cavern is made from many recursively created spherical voids.
 * These are filled with street template BuildingDoubleWalls to create the city.
 */

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class WorldGenUndergroundCity extends WorldGeneratorThread{
	private PopulatorWalledCity wc;
	private final static float P_CHILDREN=0.80F;
	private final static int MAX_CHILDREN=3;
	public final static int MIN_DIAM=11, MAX_DIAM=30;
	private final static int DIAM_INCREMENT=3,Z_SHIFT=12;
	private final static float HORIZ_SHIFT_SIGMA=0.2F, THETA_SHIFT_SIGMA=5.0F;
	
	private ArrayList<int[]> hollows=new ArrayList<int[]>();
	private ArrayList<BuildingDoubleWall> streets=new ArrayList<BuildingDoubleWall>();
	private double cavernMass=0.0, cavernMass_i=0.0, cavernMass_k=0.0;
	TemplateWall pws;
	
	//****************************************  CONSTRUCTOR - WorldGenUndergroundCity   *************************************************************************************//
	public WorldGenUndergroundCity (PopulatorWalledCity wc_,World world_, Random random_, int chunkI_, int chunkK_, int TriesPerChunk_, double ChunkTryProb_) { 
		super(wc_, world_, random_,chunkI_, chunkK_, TriesPerChunk_, ChunkTryProb_);
		wc=wc_;
		BacktrackLength=wc.BacktrackLength;
		chestTries=wc.chestTries;
		chestItems=wc.chestItems;
		setName("WorldGenCavernThread");
	}
	
	//****************************  FUNCTION - generate*************************************************************************************//
	public boolean generate(int i0,int j0,int k0) throws InterruptedException{
		pws=TemplateWall.pickBiomeWeightedWallStyle(wc.undergroundCityStyles,world,i0,k0,world.rand,true);
		if(pws==null) 
			return false;
		willBuild=true;
		if(!wc.cityIsSeparated(world,i0,k0,PopulatorWalledCity.CITY_TYPE_UNDERGROUND)) 
			return false;
		
		//make hollows recursively
		hollow(i0,j0,k0,MAX_DIAM);
		if(hollows.size()==0) 
			return false;
		wc.cityLocations.get(world).add(new int[]{i0,k0,PopulatorWalledCity.CITY_TYPE_UNDERGROUND});
		wc.saveCityLocations(world);
		wc.logOrPrint("\n***** Building "+pws.name+" city with "+hollows.size()+" hollows at ("+i0+","+j0+","+k0+"). ******\n");
		
		ArrayList<BuildingUndergroundEntranceway> entranceways= buildEntranceways();
		
		//build streets, towers
		fillHollows();
		for(BuildingUndergroundEntranceway entranceway : entranceways){
			if(entranceway.street.bLength>1) {
				entranceway.street.buildFromTML();
				entranceway.street.makeBuildings(true,true,false,false,false);
		}}
		for(BuildingDoubleWall street : streets) {
			if(!master.isFlushingGenThreads) 
				suspendGen();
			street.buildTowers(true,true,false,pws.StreetDensity > TemplateWall.MAX_STREET_DENSITY/2,false);
		}

		return true;
	}
	
	//****************************  FUNCTION - hollow *************************************************************************************//
	//hollows out a nearly spherical void as part of the cavern structure
	private boolean hollow(int i,int j,int k,int diam) throws InterruptedException{
		if(diam < MIN_DIAM) return false;
		if(j-diam/2<10 || j+diam/2 > Building.findSurfaceJ(world, i+diam/2, k+diam/2, Building.WORLD_MAX_Y, false,Building.IGNORE_WATER) - 3) return false;
		if(!exploreArea(new int[]{i,0,k}, new int[]{i+diam,0,k+diam}, false)){
			wc.logOrPrint("Could not explore new chunks while generating underground city at ("+i+","+k+"). Terminating this hollow.");
			return false;
		}
		hollows.add(new int[]{i,j,k,diam,0});
		
		if(diam==MAX_DIAM) wc.chatBuildingCity("** Building underground city... **",null);
		
		for(int z1=0; z1<(diam+1)/2; z1++){
			//top half
			int top_diam=Building.SPHERE_SHAPE[diam][z1];
			int offset=(diam-top_diam)/2;
			for(int y1=0; y1<top_diam;y1++){ for(int x1=0; x1<top_diam;x1++){
				if(Building.CIRCLE_SHAPE[top_diam][x1][y1]>=0){
					Building.setBlockAndMetaNoLighting(world,i+offset+x1, j+z1, k+offset+y1,0,0);
			}}}
			for(int y1=0; y1<top_diam;y1++){ 
				for(int x1=0; x1<top_diam;x1++){
					if(Building.CIRCLE_SHAPE[top_diam][x1][y1]>=0){
						//keep gravel and water from pouring in
						for(int z2=z1+1; z2<=z1+3; z2++)
							if( Building.IS_FLOWING_BLOCK[world.getBlockId(i+offset+x1, j+z2, k+offset+y1)])
							{
								world.setBlock(i+offset+x1, j+z2, k+offset+y1,Building.STONE_ID);
							}
			}}}
			
			//bottom half, make flatter than top half
			int bottom_diam=Building.SPHERE_SHAPE[diam][2*z1/3];
			offset=(diam-bottom_diam)/2;
			if(z1>0){
				for(int y1=0; y1<bottom_diam;y1++){ for(int x1=0; x1<bottom_diam;x1++){
					if(Building.CIRCLE_SHAPE[bottom_diam][x1][y1]>=0){
						Building.setBlockAndMetaNoLighting(world,i+offset+x1, j-z1, k+offset+y1,0,0);
				}}}
				for(int y1=0; y1<bottom_diam;y1++){ for(int x1=0; x1<bottom_diam;x1++){
					if(Building.CIRCLE_SHAPE[bottom_diam][x1][y1]>=0){
						int blockId=world.getBlockId(i+offset+x1, j-z1-1, k+offset+y1);
						if(Building.IS_ORE_BLOCK[blockId] && blockId!=Building.COAL_ORE_ID)
							world.setBlock(i+offset+x1, j-z1-1, k+offset+y1,Building.STONE_ID);
				}}}
			}
		}
		
		
		//update center of mass numbers
		int hollowMass=diam*diam*diam;
		cavernMass+=hollowMass;
		cavernMass_i+=hollowMass*i;
		cavernMass_k+=hollowMass*k;
		
		//spawn nearby hollows
		int successes=0;
		for(int tries=0; tries<(diam>=MAX_DIAM-2*DIAM_INCREMENT ? 10:MAX_CHILDREN); tries++){
			if(random.nextFloat()<P_CHILDREN){
				float theta;
				if(diam>=MAX_DIAM-2*DIAM_INCREMENT) 
					theta=random.nextFloat()*6.283185F;
				//theta points away from center of mass + noise
				else theta=(float)Math.atan((cavernMass*i-cavernMass_i)/(cavernMass*k-cavernMass_k)) 
				                + THETA_SHIFT_SIGMA*random.nextFloat()*(random.nextFloat()-0.5F);
				float rshift=(float)Building.SPHERE_SHAPE[diam][diam/3] + (float)diam*(HORIZ_SHIFT_SIGMA/2 - HORIZ_SHIFT_SIGMA*random.nextFloat());
				
				if(hollow(i + (int)(MathHelper.sin(theta)*rshift),
					   j + random.nextInt(random.nextInt(Z_SHIFT) + 1) - Z_SHIFT/4,
					   k + (int)(MathHelper.cos(theta)*rshift),
					   diam-DIAM_INCREMENT))
					successes++;
				if(successes >= MAX_CHILDREN) 
					break;
			}
		}
		return true;
	}
	
	//****************************  FUNCTION - buildEntranceways *************************************************************************************//
	private ArrayList<BuildingUndergroundEntranceway> buildEntranceways() throws InterruptedException{
		if(!pws.MakeUndergroundEntranceways) 
			return new ArrayList<BuildingUndergroundEntranceway>();
		
		int[] center=new int[]{(int)(cavernMass_i/cavernMass),Building.WORLD_MAX_Y+1,(int)(cavernMass_k/cavernMass)}; 
		int[] pole=new int[]{center[0]+100,center[1],center[2]};
		ArrayList<BuildingUndergroundEntranceway> entranceways = new ArrayList<BuildingUndergroundEntranceway>();

		for(int attempts=0; attempts<Math.min(20, hollows.size()); attempts++){
			int[] hollow=getFarthestHollowFromPt(pole);
			
			int diam=Building.SPHERE_SHAPE[hollow[3]][hollow[3]/3];
			int axDir= Math.abs(center[0]-hollow[0]) > Math.abs(center[2]-hollow[2]) ?
					hollow[0]>center[0] ? Building.DIR_SOUTH:Building.DIR_NORTH :
					hollow[2]>center[2] ? Building.DIR_WEST:Building.DIR_EAST;
			int[] pt= new int[]{hollow[0] + (Math.abs(axDir)==1 ? hollow[3]/2 : (axDir==Building.DIR_SOUTH ? (hollow[3]+diam)/2:(hollow[3]-diam)/2+1))
							   ,hollow[1] - hollow[3]/3
							   ,hollow[2] + (Math.abs(axDir)==2 ? hollow[3]/2 : (axDir==Building.DIR_WEST ? (hollow[3]+diam)/2:(hollow[3]-diam)/2+1))};
			
			boolean separated=true;
			for(BuildingUndergroundEntranceway entranceway : entranceways)
				if(Building.distance(entranceway.getIJKPt(0,0,0), pt)<400) 
					separated=false;
			
			BuildingUndergroundEntranceway entranceway=new BuildingUndergroundEntranceway(attempts,this,pws,axDir,pt);
			if(separated && entranceway.build()){
				entranceways.add(entranceway);
				wc.chatBuildingCity("Built an underground entranceway at ("+ hollow[0]+","+ hollow[1]+","+ hollow[2]+").",null);
			}
			pole[0]=center[0]+(center[2]-hollow[2])/2; //new pole is midpoint of old center and hollow, rotated by 90 degrees.
			pole[2]=center[2]+(hollow[0]-center[0])/2;
			if(entranceways.size()>=4) 
				break;
		}
		return entranceways;
	}
	
	
	//****************************  FUNCTION - fillHollows *************************************************************************************//
	private void fillHollows() throws InterruptedException{
		//fills hollows with roads/buildings
		int successes=0;
		for(int tries=0; tries<pws.StreetDensity * 4; tries++){
			if(!master.isFlushingGenThreads) suspendGen();
			int[] hollow=hollows.get(random.nextInt(hollows.size()));
			
			int[] pt=new int[]{random.nextInt(hollow[3]),0,random.nextInt(hollow[3])};
			if(Building.CIRCLE_SHAPE[hollow[3]][pt[0]][pt[2]]==0){		
				pt[0]+=hollow[0];
				pt[2]+=hollow[2];
				pt[1]=Building.findSurfaceJ(world, pt[0], pt[2], hollow[1]-(hollow[3]+1)/2,false,Building.IGNORE_WATER)+1;
				TemplateWall sws=TemplateWall.pickBiomeWeightedWallStyle(pws.streets,world,pt[0],pt[2],world.rand,true);
				sws.MergeWalls=true;
				BuildingDoubleWall street=new BuildingDoubleWall(tries,this,sws,random.nextInt(4),Building.R_HAND,pt);
				if(street.plan()) {
					street.build(LAYOUT_CODE_NOCODE);
					streets.add(street);
					successes++;
				}
				if(successes > Math.min(hollows.size()*pws.StreetDensity, 4*pws.StreetDensity)) 
					break;
			}
		}
	}
	
	//****************************  FUNCTION - getFarthestHollowFromPt *************************************************************************************//
	private int[] getFarthestHollowFromPt(int[] pt){
		int[] farthestHollow=null;
		int maxDist=-1;
		for(int[] h : hollows){
			int dist=Building.distance(pt, h);
			if(dist > maxDist){
				maxDist=dist;
				farthestHollow=h;
			}
		}
		return farthestHollow;
	}
	
}
