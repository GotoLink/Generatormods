package mods.generator;

import net.minecraft.world.World;

public class WorldGenCARuins extends WorldGeneratorThread{
	
private PopulatorCARuins ca;
private BuildingCellularAutomaton bca;
private byte[][] caRule=null, seed;
private int[][] caRulesWeightsAndIndex;
private int MinHeight, MaxHeight;
private float GlobalFrequency, SymmetricSeedDensity;
private int ContainerWidth, ContainerLength, seedCode;
private int[] seedTypeWeights;
private final static int[] SEED_TYPE_CODES=new int[]{0,1,2,3};
private int MinHeightBeforeOscillation;
private boolean SmoothWithStairs, MakeFloors;
private TemplateRule[] blockRules, blockRule;

//****************************************  CONSTRUCTOR - WorldGenCARuins  *************************************************************************************//
	public WorldGenCARuins(PopulatorCARuins ca_, World world_, int chunkI_, int chunkK_, int TriesPerChunk_, double ChunkTryProb_) {
		super(ca_, world_, chunkI_, chunkK_, TriesPerChunk_, ChunkTryProb_);
		ca=ca_;
		chestTries=ca.chestTries;
		chestItems=ca.chestItems;
		caRulesWeightsAndIndex=ca.caRulesWeightsAndIndex;
		MinHeight=ca.MinHeight; MaxHeight=ca.MaxHeight;
		GlobalFrequency=ca.GlobalFrequency; SymmetricSeedDensity=ca.SymmetricSeedDensity;
		ContainerWidth=ca.ContainerWidth; ContainerLength=ca.ContainerLength;
		seedTypeWeights=ca.seedTypeWeights;
		MinHeightBeforeOscillation=ca.MinHeightBeforeOscillation;
		SmoothWithStairs=ca.SmoothWithStairs; MakeFloors=ca.MakeFloors;
		blockRules=ca.blockRules;
		setName("WorldGenAutomata");
	}
	//****************************************  FUNCTION - generate  *************************************************************************************//
	public boolean generate(int i0, int j0, int k0) throws InterruptedException {
		
		int th=MinHeight+world.rand.nextInt(MaxHeight-MinHeight+1);
		
		if(caRule==null) //if we haven't picked in an earlier generate call 
			caRule=ca.caRules.get(Building.pickWeightedOption(world.rand, caRulesWeightsAndIndex[0], caRulesWeightsAndIndex[1]));
		if(caRule==null) 
			return false;
		
		int seedCode=Building.pickWeightedOption(world.rand, seedTypeWeights, SEED_TYPE_CODES);
		byte[][] seed = seedCode==0 || (caRule[0][0]==0 && caRule[0][1]==0 && caRule[0][2]==0 && caRule[0][3]==0) //only use symmetric for 4-rules
						  			? BuildingCellularAutomaton.makeSymmetricSeed(Math.min(ContainerWidth,ContainerLength),SymmetricSeedDensity,world.rand)
					  : seedCode==1 ? BuildingCellularAutomaton.makeLinearSeed(ContainerWidth,world.rand)
					  : seedCode==2 ? BuildingCellularAutomaton.makeCircularSeed(Math.min(ContainerWidth,ContainerLength),world.rand)
					  : 			  BuildingCellularAutomaton.makeCruciformSeed(Math.min(ContainerWidth,ContainerLength),world.rand);
		
		TemplateRule blockRule=blockRules[world.getBiomeGenForCoords(i0,k0).biomeID+1];
		//can use this to test out new Building classes
		/*
		BuildingSpiralStaircase bss=new BuildingSpiralStaircase(this,blockRule,random.nextInt(4),2*random.nextInt(2)-1,false,-(random.nextInt(10)+1),new int[]{i0,j0,k0});
		bss.build(0,0);
		bss.bottomIsFloor();
		return true;
		*/

		BuildingCellularAutomaton bca=new BuildingCellularAutomaton(this,blockRule,world.rand.nextInt(4),1, false, 
				                           ContainerWidth, th,ContainerLength,seed,caRule,null,new int[]{i0,j0,k0});
		if(bca.plan(true,MinHeightBeforeOscillation) && bca.queryCanBuild(0,true)){
			bca.build(SmoothWithStairs,MakeFloors);													
			if(GlobalFrequency < 0.05 && world.rand.nextInt(2)!=0){
				for(int tries=0; tries < 10; tries++){
					int[] pt=new int[]{i0+(2*world.rand.nextInt(2)-1)*(ContainerWidth + world.rand.nextInt(ContainerWidth)),
								   	   0,
								       k0+(2*world.rand.nextInt(2)-1)*(ContainerWidth + world.rand.nextInt(ContainerWidth))};
					pt[1]=Building.findSurfaceJ(world,pt[0],pt[2],Building.WORLD_MAX_Y,true,3)+1;
					if(generate(pt[0], pt[1], pt[2])) 
						{
						break;}
				}
			}
			return true;
		}
		return false;
	}
}
