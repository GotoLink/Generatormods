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
 * WorldGenGreatWall creates a great wall in Minecraft.
 * This class is chiefly a WorldGeneratorThread wrapper for a BuildingDoubleWall.
 * It also checks curviness and length.
 */

import java.util.Random;

import net.minecraft.world.World;

public class WorldGenGreatWall extends WorldGeneratorThread
{
	//private final static boolean DEBUG=false;

	//**** WORKING VARIABLES **** 
	private PopulatorGreatWall gw;
	
	//****************************  CONSTRUCTOR - WorldGenGreatWall *************************************************************************************//
	public WorldGenGreatWall (PopulatorGreatWall gw_, World world_, Random random_, int chunkI_, int chunkK_, int TriesPerChunk_, double ChunkTryProb_) { 
		super(gw_, world_, random_, chunkI_, chunkK_, TriesPerChunk_, ChunkTryProb_);//got the "master" thingy out
		gw=gw_;
		BacktrackLength=gw.BacktrackLength;
		chestTries=gw.chestTries;
		chestItems=gw.chestItems;
		setName("WorldGenGreatWallThread");
	}

	//****************************  FUNCTION - generate  *************************************************************************************//
	public boolean generate(int i0, int j0, int k0) throws InterruptedException{
		TemplateWall ws=TemplateWall.pickBiomeWeightedWallStyle(gw.wallStyles,world,i0,k0,random,false);
		if(ws==null) return false;
				
		BuildingDoubleWall dw=new BuildingDoubleWall(10*(random.nextInt(9000)+1000),this,ws,random.nextInt(4),1,new int[] {i0,j0,k0});
		if(!dw.plan()) return false;

		//calculate the integrated curvature
		if(gw.CurveBias>0.01){
			//Perform a probabilistic test
			//Test formula considers both length and curvature, bias is towards longer and curvier walls.
			double curviness=0;
			for(int m=1;m<dw.wall1.bLength;m++) 
				curviness+= (dw.wall1.xArray[m]==dw.wall1.xArray[m-1] ? 0:1)+(dw.wall1.zArray[m]==dw.wall1.zArray[m-1] ? 0:1);
			for(int m=1;m<dw.wall2.bLength;m++) 
				curviness+= (dw.wall2.xArray[m]==dw.wall2.xArray[m-1] ? 0:1)+(dw.wall2.zArray[m]==dw.wall2.zArray[m-1] ? 0:1);
			curviness/=(double)(2*(dw.wall1.bLength+dw.wall2.bLength - 1));
			
			//R plotting - sigmoid function
			/*
				pwall<-function(curviness,curvebias) 1/(1+exp(-30*(curviness-(curvebias/5))))
				plotpwall<-function(curvebias){
					plot(function(curviness) pwall(curviness,curvebias),ylim=c(0,1),xlim=c(0,0.5),xlab="curviness",ylab="p",main=paste("curvebias=",curvebias))
				}
				plotpwall(0.5)
			 */

			double p=1.0/(1.0+Math.exp(-30.0*(curviness-(gw.CurveBias/5.0))));
						
			if(random.nextFloat() > p && curviness!=0){
				gw.logOrPrint("Rejected great wall, curviness="+curviness+", length="+(dw.wall1.bLength+dw.wall1.bLength - 1)+", P="+p);
				return false;
			}
		}

		dw.build(LAYOUT_CODE_NOCODE);
		dw.buildTowers(true,true,ws.MakeGatehouseTowers,false,false);
		return true;
	}

}










