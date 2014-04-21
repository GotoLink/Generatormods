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
 * TemplateRule reads in a rule String and defines a rule that blocks can be sampled from.
 */
import java.util.Random;

import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

public class TemplateRule {
	public final static int FIXED_FOR_BUILDING = 5;
	public final static TemplateRule RULE_NOT_PROVIDED = null;
	public final static String BLOCK_NOT_REGISTERED_ERROR_PREFIX = "Error reading rule: BlockID "; //so we can treat this error differently
	public final static TemplateRule AIR_RULE = new TemplateRule(Blocks.air, 0);
	public final static TemplateRule STONE_RULE = new TemplateRule(Blocks.stone, 0);
	private Block[] blockIDs;
    private int[] blockMDs;
	public int chance = 100, condition = 0;
	public BlockAndMeta primaryBlock = null;
	private BlockAndMeta fixedRuleChosen = null;

	public TemplateRule(String rule, boolean checkMetaValue) throws Exception {
		String[] items = rule.split(",");
		int numblocks = items.length - 2;
		if (numblocks < 1)
			throw new Exception("Error reading rule: No blockIDs specified for rule!");
		condition = Integer.parseInt(items[0].trim());
		chance = Integer.parseInt(items[1].trim());
		blockIDs = new Block[numblocks];
		blockMDs = new int[numblocks];
		String[] data;
        Block temp;
		for (int i = 0; i < numblocks; i++) {
			data = items[i + 2].trim().split("-", 2);
            try{
                temp = GameData.blockRegistry.get(Integer.parseInt(data[0]));
            }catch (Exception e){
                temp = GameData.blockRegistry.get(data[0]);
            }
            if(temp!=null){
                blockIDs[i] = temp;
                blockMDs[i] = data.length > 1 ? Integer.parseInt(data[1]) : 0;
            }else if(data[0].equalsIgnoreCase("PRESERVE")){//Preserve block rule
                blockIDs[i] = Building.PRESERVE_BLOCK.get();
                blockMDs[i] = Building.PRESERVE_BLOCK.getMeta();
            }else{
                throw new Exception(BLOCK_NOT_REGISTERED_ERROR_PREFIX + data[0] + " unknown!");
            }
			if (checkMetaValue && blockIDs[i]!=Building.PRESERVE_BLOCK.get()) {
				String checkStr = Building.metaValueCheck(blockIDs[i], blockMDs[i]);
				if (checkStr != null)
					throw new Exception("Error reading rule: " + rule + "\nBad meta value " + blockMDs[i] + ". " + checkStr);
			}
		}
		primaryBlock = getPrimaryBlock();
	}

	public TemplateRule(int block, int meta) {
		blockIDs = new Block[] { GameData.blockRegistry.get(block) };
		blockMDs = new int[] { meta };
		primaryBlock = getPrimaryBlock();
	}

    public TemplateRule(Block block, int meta) {
        blockIDs = new Block[] { block };
        blockMDs = new int[] { meta };
        primaryBlock = getPrimaryBlock();
    }

	public TemplateRule(Block block, int meta, int chance_) {
        this(block, meta);
		chance = chance_;
	}

    public TemplateRule(BlockAndMeta blockAndMeta, int chance_) {
        this(blockAndMeta.get(), blockAndMeta.getMeta(), chance_);
    }

	public TemplateRule(Block[] blockIDs_, int[] blockMDs_, int chance_) {
		blockIDs = blockIDs_;
		blockMDs = blockMDs_;
		chance = chance_;
		primaryBlock = getPrimaryBlock();
	}

	public void setFixedRule(Random random) {
		if (condition == FIXED_FOR_BUILDING) {
			int m = random.nextInt(blockIDs.length);
			fixedRuleChosen = new BlockAndMeta(blockIDs[m], blockMDs[m]);
		} else
			fixedRuleChosen = null;
	}

	public TemplateRule getFixedRule(Random random) {
		if (condition != FIXED_FOR_BUILDING)
			return this;
		int m = random.nextInt(blockIDs.length);
		return new TemplateRule(blockIDs[m], blockMDs[m], chance);
	}

	public BlockAndMeta getBlock(Random random) {
		if (chance >= 100 || random.nextInt(100) < chance) {
			if (fixedRuleChosen != null)
				return fixedRuleChosen;
			int m = random.nextInt(blockIDs.length);
			return new BlockAndMeta(blockIDs[m], blockMDs[m]);
		}
		return new BlockAndMeta(Blocks.air, 0);
	}

	public BlockAndMeta getBlockOrHole(Random random) {
		if (chance >= 100 || random.nextInt(100) < chance) {
			if (fixedRuleChosen != null)
				return fixedRuleChosen;
			int m = random.nextInt(blockIDs.length);
			return new BlockAndMeta(blockIDs[m], blockMDs[m]);
		}
		return Building.HOLE_BLOCK_LIGHTING;
	}

	public boolean isPreserveRule() {
		for (int i = 0; i<blockIDs.length; i++){
			if(blockIDs[i] != Blocks.air)
				return false;
            if(blockMDs[i] != 1)
                return  false;
        }
		return true;
	}

    public boolean hasUndeadSpawner(){
        for (int i = 0; i<blockIDs.length; i++){
            //Zombie, Skeleton, Creeper, EASY, UPRIGHT spawners
            if(blockIDs[i] == Blocks.mob_spawner && (blockMDs[i]==1||blockMDs[i]==2||blockMDs[i]==4||blockMDs[i]==28||blockMDs[i]==31))
                return true;
        }
        return false;
    }

	public BlockAndMeta getNonAirBlock(Random random) {
		int m = random.nextInt(blockIDs.length);
		return new BlockAndMeta(blockIDs[m], blockMDs[m]);
	}

	@Override
	public String toString() {
		String str = condition + "," + chance;
		for (int m = 0; m < blockIDs.length; m++) {
			str += "," + GameData.blockRegistry.getNameForObject(blockIDs[m]);
			if (blockMDs[m] != 0)
				str += "-" + blockMDs[m];
		}
		return str;
	}

	//returns the most frequent block in rule
	private BlockAndMeta getPrimaryBlock() {
		int[] hist = new int[blockIDs.length];
		for (int l = 0; l < hist.length; l++)
			for (int m = 0; m < hist.length; m++)
				if (blockIDs[l] == blockIDs[m])
					hist[l]++;
		int maxFreq = 0, pos = 0;
		for (int l = 0; l < hist.length; l++) {
			if (hist[l] > maxFreq) {
				maxFreq = hist[l];
                pos = l;
			}
		}
		return new BlockAndMeta(blockIDs[pos], blockMDs[pos]);
	}
}