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
 * TemplateRule reads in a rule String and defines a rule that blocks can be sampled from.
 */

import java.util.Random;

public class TemplateRule {
    public final static int FIXED_FOR_BUILDING=5;
    public final static TemplateRule RULE_NOT_PROVIDED=null;
    
    public final static String BLOCK_NOT_REGISTERED_ERROR_PREFIX="Error reading rule: BlockID ";  //so we can treat this error differently
    
    public final static TemplateRule AIR_RULE=new TemplateRule(Building.AIR_BLOCK);
    public final static TemplateRule STONE_RULE=new TemplateRule(Building.STONE_BLOCK);
    public final static TemplateRule NETHER_BRICK_RULE=new TemplateRule(new int[]{Building.NETHER_BRICK_ID,0});
    
    private int[] blockIDs, blockMDs;
    public int chance = 100, condition = 0;
    public int[] primaryBlock=null;
    private int[] fixedRuleChosen=null;

    public TemplateRule (String rule, BuildingExplorationHandler explorationHandler, boolean checkMetaValue ) throws Exception {
        String[] items = rule.split( "," );
        int numblocks = items.length - 2;
        if( numblocks < 1 ) { throw new Exception( "Error reading rule: No blockIDs specified for rule!" ); }
        condition = Integer.parseInt( items[0].trim() );
        chance = Integer.parseInt( items[1].trim() );
        blockIDs = new int[numblocks];
        blockMDs = new int[numblocks];
        
		String[] data;
        for( int i = 0; i < numblocks; i++ ) {
        	data = items[i + 2].trim().split( "-" );
        	blockIDs[i]=Integer.parseInt( data[0] );
        	if(!Building.isValidRuleBlock(blockIDs[i],explorationHandler)){
        		throw new Exception(BLOCK_NOT_REGISTERED_ERROR_PREFIX+blockIDs[i]+" not registered!");
        	}
        	blockMDs[i]= data.length>1 ? Integer.parseInt( data[1]) : 0;
        	if(checkMetaValue){
        		String checkStr=Building.metaValueCheck(blockIDs[i], blockMDs[i]);
        		if(checkStr!=Building.META_VALUE_OK)
        			throw new Exception("Error reading rule: "+rule+"\nBad meta value "+blockMDs[i]+". "+checkStr);
        	}
        }
        
        setPrimaryBlock();
    } 

    public TemplateRule(int[] block){
    	blockIDs=new int[]{block[0]};
    	blockMDs=new int[]{block[1]};
    	setPrimaryBlock();
    }
    
    public TemplateRule(int[] block, int chance_){
    	blockIDs=new int[]{block[0]};
    	blockMDs=new int[]{block[1]};
    	chance=chance_;
    	setPrimaryBlock();
    }
    
    public TemplateRule(int[] blockIDs_, int[] blockMDs_, int chance_){
    	blockIDs=blockIDs_;
    	blockMDs=blockMDs_;
    	chance=chance_;
    	setPrimaryBlock();
    }
    
    public void setFixedRule(Random random){
    	if(condition==FIXED_FOR_BUILDING){
	    	int m=random.nextInt(blockIDs.length);
	    	fixedRuleChosen= new int[]{blockIDs[m],blockMDs[m]};
    	}else fixedRuleChosen=null;
    }
    
    public TemplateRule getFixedRule(Random random){
    	if(condition!=FIXED_FOR_BUILDING) return this;
    	
    	int m=random.nextInt(blockIDs.length);
    	return new TemplateRule(new int[]{blockIDs[m],blockMDs[m]},chance);
    }
    
    public int[] getBlock(Random random){
    	if(chance >=100 || random.nextInt(100) < chance){
    		if(fixedRuleChosen!=null) return fixedRuleChosen;
    		
    		int m=random.nextInt(blockIDs.length);
    		return new int[]{blockIDs[m],blockMDs[m]};
    	}
    	return Building.AIR_BLOCK;
    }
    
    public int[] getBlockOrHole(Random random){
    	if(chance >=100 || random.nextInt(100) < chance){
    		if(fixedRuleChosen!=null) return fixedRuleChosen;
    		
    		int m=random.nextInt(blockIDs.length);
    		return new int[]{blockIDs[m],blockMDs[m]};
    	}
    	return Building.HOLE_BLOCK_LIGHTING;
    }
    
    public boolean isPreserveRule(){
    	for(int blockID : blockIDs)
    		if(blockID!=Building.PRESERVE_ID ) return false;
    	return true;
    }
    
    public int[] getNonAirBlock(Random random){
    	int m=random.nextInt(blockIDs.length);
    	return new int[]{blockIDs[m],blockMDs[m]};
    }
    
    public int[] getBlockIDs(){
    	return blockIDs;
    }
    
    @Override
    public String toString(){
    	String str=condition +","+chance;
    	for(int m=0; m<blockIDs.length; m++){
    		str+=","+blockIDs[m];
    		if(blockMDs[m]!=0) str+="-"+blockMDs[m];
    	}
    	return str;
    }

    
    //returns the most frequent block in rule
    private void setPrimaryBlock(){
    	int[] hist=new int[blockIDs.length];
    	for(int l=0;l<hist.length;l++)
    		for(int m=0;m<hist.length;m++)
    			if(blockIDs[l]==blockIDs[m]) hist[l]++;
    	
    	int maxFreq=0;
    	for(int l=0;l<hist.length;l++){
    		if(hist[l]>maxFreq){
    			maxFreq=hist[l];
    			primaryBlock=new int[]{blockIDs[l],blockMDs[l]};
    		}
    	}
    }
    

}