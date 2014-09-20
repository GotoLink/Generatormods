package assets.generator;

/*
 *  Source code for the The Great Wall Mod, CellullarAutomata Ruins and Walled City Generator Mods for the game Minecraft
 *  Copyright (C) 2011 by Formivore - 2012 by GotoLink
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.IShearable;

import java.util.HashMap;
import java.util.Map;

public class BlockProperties {
    private final static Map<Block,BlockProperties> props = new HashMap<Block, BlockProperties>();
    /**
     * All the studied block properties
     */
    public final boolean isWater,isStair,isDoor,isTree,isFlowing,isWallable,isOre,isGround,isArtificial,isLight,isLoaded,isDelayed;

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
        isTree = block instanceof BlockLog || block instanceof IShearable || block instanceof BlockSnow;
        isFlowing = isWater ||  block.getMaterial() == Material.lava || block instanceof BlockDynamicLiquid || block instanceof BlockFalling;
        isWallable = isWater || block instanceof BlockAir || isTree || block instanceof BlockWeb || block instanceof BlockPumpkin
                || block instanceof BlockMelon || block instanceof BlockHugeMushroom || block instanceof IPlantable;
        isOre = block == Blocks.clay || block instanceof BlockRedstoneOre || block instanceof BlockOre;
        isGround = block == Blocks.stone || block instanceof BlockDirt || block instanceof BlockGrass
                || block instanceof BlockGravel || block instanceof BlockSand || block instanceof BlockNetherrack || block instanceof BlockSoulSand || block instanceof BlockMycelium;
        // Define by what it is not. Not IS_WALLABLE and not a naturally
        // occurring solid block (obsidian/bedrock are exceptions)
        isArtificial = !(isWallable || isOre || isGround);
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
