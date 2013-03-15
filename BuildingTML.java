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
 * BuildingTML generates a building from a .tml template.
 */

public class BuildingTML extends Building
{
	TemplateTML tmlt;

	//****************************************  CONSTRUCTOR - BuildingTML  *************************************************************************************//
	public BuildingTML (int ID_,WorldGeneratorThread wgt,int bDir_,int axXHand_,boolean centerAligned_,TemplateTML tmlt_, int[] sourcePt) {
		super(ID_,wgt, null, bDir_,axXHand_,centerAligned_,new int[]{tmlt_.width,tmlt_.height,tmlt_.length},sourcePt);
		tmlt=tmlt_;
		j0-=tmlt.embed;
	}
	
	//****************************************  FUNCTION - queryCanBuild *************************************************************************************//
	   public boolean queryCanBuild(int ybuffer) throws InterruptedException{
		   if(j0<=0) return false;
		   
	    	if(!( queryExplorationHandlerForChunk(0,0,bLength-1) && queryExplorationHandlerForChunk(bWidth-1,0,0) && queryExplorationHandlerForChunk(bWidth-1,0,bLength-1) )){
				return false;
			}
	    	
	    	//Don't build if it would require leveling greater than tmlt.leveling
	    	for(int y=0 ;y<bLength;y++) for(int x=0 ;x<bWidth;x++)
	    		if(j0 - getSurfaceIJKPt(x,y,j0-1,true,3)[1] > tmlt.leveling + 1) return false;
	    	
	    	//check to see if we are underwater
	    	if(tmlt.waterHeight!=TemplateTML.NO_WATER_CHECK){
	    		int waterCheckHeight=tmlt.waterHeight+tmlt.embed+1; //have to unshift by embed
	    		if(IS_WATER_BLOCK[getBlockIdLocal(0,waterCheckHeight,0)] || IS_WATER_BLOCK[getBlockIdLocal(0,waterCheckHeight,bLength-1)]
	    		 ||IS_WATER_BLOCK[getBlockIdLocal(bWidth-1,waterCheckHeight,0)] || IS_WATER_BLOCK[getBlockIdLocal(bWidth-1,waterCheckHeight,bLength-1)])
	    			return false;
	    	}
	    	
	    	if(wgt.isLayoutGenerator()){
	    		
	    		//allow large templates to be built over streets by using the tower code to check
	    		//However, if we do build, always put down the template code
	    		int layoutCode= tmlt.buildOverStreets ? WorldGeneratorThread.LAYOUT_CODE_TOWER : WorldGeneratorThread.LAYOUT_CODE_TEMPLATE;
	    		
		    	if(wgt.layoutIsClear(this, tmlt.templateLayout,layoutCode)){
		    		wgt.setLayoutCode(this,tmlt.templateLayout,WorldGeneratorThread.LAYOUT_CODE_TEMPLATE);
		    	} else return false;
	    	}else{
	    		if(isObstructedFrame(0,ybuffer)) return false;
	    	}
			return true;
	    }

	//****************************************  FUNCTION - build *************************************************************************************//
	public void build() {
		tmlt.setFixedRules(random);
		
		//build base
		int[][] base=tmlt.namedLayers.get("base");
		for(int y=0;y<bLength;y++){ for(int x=0;x<bWidth;x++){
			if(base!=null) 
				buildDown(x,-1,y,tmlt.rules[base[y][x]],tmlt.leveling,0,0);
			else fillDown(getSurfaceIJKPt(x,y,j0-1,true,IGNORE_WATER),j0-1,world);
		}}

		
		//clear overhead
		for(int z=bHeight; z<tmlt.cutIn+tmlt.embed; z++)
			for(int y=0;y<bLength;y++) for(int x=0;x<bWidth;x++)
				setBlockLocal(x,z,y,0);


		//build
		for(int z=0;z<bHeight;z++)
			for(int y=0;y<bLength;y++) for(int x=0;x<bWidth;x++)
				setBlockLocal(x,z,y,tmlt.rules[tmlt.template[z][y][x]]);


		flushDelayed();
	}
	
	//****************************************  FUNCTION - isObstructedBody*************************************************************************************//
	/*
	private boolean isObstructedBody(TemplateTML tb){
    	for(int z1=Math.min(tb.height-1,2); z1<tb.height; z1++){
    		for(int x1=0; x1<tb.length; x1++)
    			if(tb.rules[tb.template[z1][tb.width-1][x1]].getBlock(random)[0]!=PRESERVE_ID && isWallBlock(x1,z1,tb.width-1)) return true;
    		for(int y1=1; y1<tb.width-1;y1++){
    			if(tb.rules[tb.template[z1][y1][0]].getBlock(random)[0]!=PRESERVE_ID && isWallBlock(0,z1,y1)) return true;
    			if(tb.rules[tb.template[z1][y1][tb.length-1]].getBlock(random)[0]!=PRESERVE_ID && isWallBlock(tb.length-1,z1,y1)) return true;
    		}
    			
    	}
    	return false;
    }
    */


}