package mods.generator;

import java.util.Random;

import net.minecraft.world.World;

public class WorldGenSingleWall extends WorldGeneratorThread{
        private int[] pt;
        private PopulatorGreatWall gw;
       
        public WorldGenSingleWall (PopulatorGreatWall gw_, World world_, Random random_, int[] pt_) {
                super(gw_, world_, random_, pt_[0]>>4, pt_[2]>>4, 0, 0.0);
                setName("WorldGenSingleWallThread");
                pt=pt_;
                gw=gw_;
        }
       
        @Override
        public void run(){
                hasStarted=true;
                try{
                        generate(pt[0],pt[1],pt[2]);
                } catch(InterruptedException e){
                	Thread.currentThread().interrupt();
                }

                synchronized(master){
                        hasTerminated=true;
                        threadSuspended=true;
                        master.notifyAll();
                }
        }
       
        public boolean generate(int i0, int j0, int k0) throws InterruptedException{
                TemplateWall ws=TemplateWall.pickBiomeWeightedWallStyle(gw.wallStyles,world,i0,k0,random,false);
                BuildingWall wall=new BuildingWall(0,this,ws,Building.DIR_NORTH,Building.R_HAND, ws.MaxL,true,i0,j0,k0);
                //BuildingWall(int ID_, WorldGeneratorThread wgt_,WallStyle ws_,int dir_,int axXHand_, int maxLength_,int i0_,int j0_, int k0_){
               
                wall.setTarget(gw.placedCoords);
                wall.plan(1,0,ws.MergeWalls ? ws.WWidth : BuildingWall.DEFAULT_LOOKAHEAD,false);
                //plan(int Backtrack, int startN, int depth, int lookahead, boolean stopAtWall) throws InterruptedException {
               
                if(wall.bLength>=wall.y_targ){
                        wall.smooth(ws.ConcaveDownSmoothingScale,ws.ConcaveUpSmoothingScale,true);
                        wall.buildFromTML();
                        wall.makeBuildings(true,true,true,false,false);        
                        gw.placedCoords=null;
                }
               
                return true;
        }

}

