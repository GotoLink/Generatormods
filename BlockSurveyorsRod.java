package mods.generator;


import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;


public class BlockSurveyorsRod extends Block
{
        PopulatorGreatWall gw;
       
  public BlockSurveyorsRod (int i, int j, PopulatorGreatWall gw_){
      super(i, Material.rock);
      gw=gw_;
  }

        public void onBlockRemoval(World world, int i, int j, int k){
                if(gw.placedCoords!=null && gw.placedCoords[0]==i && gw.placedCoords[1]==j && gw.placedCoords[2]==k)
                        gw.placedCoords=null;
  }

  public int idDropped(int i, Random random){
      return blockID;
  }
   
  public void onBlockAdded(World world, int i, int j, int k){
        if(gw.placedCoords==null || gw.placedWorld!=world){
                gw.placedCoords=new int[]{i,j,k};
                gw.placedWorld=world;
        }
        else{
                System.out.println("\nPrevious magicWall placed at "+gw.placedCoords[0]+" "+gw.placedCoords[1]+" "+gw.placedCoords[2]);
                System.out.println("this magicWall placed at "+i+" "+j+" "+k);
                gw.exploreThreads.add(new WorldGenSingleWall(gw, world, new Random(), new int[]{i,j,k}));
 
        }
  }
       
}
