package assets.generator;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.IShearable;

import java.util.HashMap;
import java.util.Map;

/**
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class BlockProperties {
    private final static Map<Block,BlockProperties> props = new HashMap<Block, BlockProperties>();
    /**
     * All the studied block properties
     */
    public final boolean isWater,isStair,isDoor,isFlowing,isWallable,isOre,isArtificial,isLight,isLoaded,isDelayed;

    /**
     * Build the properties for the given block and store them into the internal map
     * @param block
     */
    public BlockProperties(Block block){
        // Lava is considered to NOT be a liquid, and is therefore not
        // wallable. This is so we can build cities on the lava surface.
        isWater = block.getMaterial() == Material.water || block == Blocks.ice;
        isStair = block instanceof BlockStairs;
        isDoor = block instanceof BlockDoor;
        isFlowing = isWater ||  block.getMaterial() == Material.lava || block instanceof BlockDynamicLiquid || block instanceof BlockSand
                || block instanceof BlockGravel;
        isWallable = isWater || block instanceof BlockAir || block instanceof BlockLog || block instanceof BlockWeb || block instanceof BlockSnow || block instanceof BlockPumpkin
                || block instanceof BlockMelon || block instanceof IShearable || block instanceof BlockHugeMushroom || block instanceof IPlantable;
        isOre = block == Blocks.clay || block instanceof BlockRedstoneOre || block instanceof BlockOre;
        // Define by what it is not. Not IS_WALLABLE and not a naturally
        // occurring solid block (obsidian/bedrock are exceptions)
        isArtificial = !(isWallable || isOre || block == Blocks.stone || block instanceof BlockDirt || block instanceof BlockGrass
                || block instanceof BlockGravel || block instanceof BlockSand || block instanceof BlockNetherrack || block instanceof BlockSoulSand || block instanceof BlockMycelium);
        isLight = block instanceof BlockTorch || block instanceof BlockGlowstone;
        isDelayed = isStair || isFlowing || isLight || block == Blocks.air || block instanceof BlockLever || block instanceof BlockSign
                || block instanceof BlockFire || block instanceof BlockButton || block instanceof BlockVine || block instanceof BlockRedstoneWire || block instanceof BlockDispenser
                || block instanceof BlockFurnace;
        // Define by what it is not.
        isLoaded = !(isWallable || isFlowing || block instanceof BlockTorch || block instanceof BlockLadder);
        props.put(block, this);
    }

    /**
     * Retrieve the properties for the given block
     * @param block
     * @return
     */
    public static BlockProperties get(Block block){
        BlockProperties p = props.get(block);
        if(p!=null){
            return p;
        }
        return new BlockProperties(block);
    }
}
