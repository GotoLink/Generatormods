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

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.Tuple;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class BlockAndMeta extends Tuple {
    public BlockAndMeta(Block block, int meta){
        super(block,meta);
    }

    public Block get(){
        return Block.class.cast(getFirst());
    }

    public int getMeta(){
        return Integer.class.cast(getSecond());
    }

    @Override
    public boolean equals(Object obj){
        if(obj==null){
            return false;
        }
        if(obj==this){
            return true;
        }
        return obj instanceof BlockAndMeta && this.getMeta()==((BlockAndMeta) obj).getMeta() && this.get()==((BlockAndMeta) obj).get();
    }

    @Override
    public int hashCode(){
        return new HashCodeBuilder().append(getMeta()).append(get()).toHashCode();
    }

    public Block toStair(){
        if(get()== Blocks.cobblestone||get()==Blocks.mossy_cobblestone){
            return Blocks.stone_stairs;
        }else if(get()==Blocks.nether_brick){
            return Blocks.nether_brick_stairs;
        }else if(get()==Blocks.stonebrick||get()==Blocks.stone){
            return Blocks.stone_brick_stairs;
        }else if(get()==Blocks.brick_block){
            return Blocks.brick_stairs;
        }else if(get()==Blocks.sandstone){
            return Blocks.sandstone_stairs;
        }else if(get()==Blocks.quartz_block){
            return Blocks.quartz_stairs;
        }else if(get()==Blocks.planks){
            int tempdata = getMeta();
            switch (tempdata) {
                case 0:
                    return Blocks.oak_stairs;
                case 1:
                    return Blocks.spruce_stairs;
                case 2:
                    return Blocks.birch_stairs;
                case 3:
                    return Blocks.jungle_stairs;
                case 4:
                    return Blocks.acacia_stairs;
                case 5:
                    return Blocks.dark_oak_stairs;
            }
        }
        return get();
    }

    public BlockAndMeta toStep(){
        if (!BlockProperties.get(get()).isArtificial)
            return this;
        if(get()==Blocks.sandstone){
            return new BlockAndMeta(Blocks.stone_slab, 1);
        }else if(get()==Blocks.planks){
            return new BlockAndMeta(Blocks.stone_slab, 2);
        }else if(get()==Blocks.cobblestone){
            return new BlockAndMeta(Blocks.stone_slab, 3);
        }else if(get()==Blocks.brick_block){
            return new BlockAndMeta(Blocks.stone_slab, 4);
        }else if(get()==Blocks.stonebrick){
            return new BlockAndMeta(Blocks.stone_slab, 5);
        }else if(get()==Blocks.nether_brick){
            return new BlockAndMeta(Blocks.stone_slab, 6);
        }else if(get()==Blocks.quartz_block){
            return new BlockAndMeta(Blocks.stone_slab, 7);
        }else if(get()==Blocks.stone_slab||get()==Blocks.double_stone_slab){
            return new BlockAndMeta(Blocks.stone_slab, getMeta());
        }else if(get()==Blocks.double_wooden_slab||get()==Blocks.wooden_slab){
            return new BlockAndMeta(Blocks.wooden_slab, getMeta());
        }else{
            return new BlockAndMeta(get(), 0);
        }
    }

    public BlockAndMeta stairToSolid(){
        Block block = get();
        int meta = 0;
        if(block==Blocks.stone_stairs) {
            block = Blocks.cobblestone;
        }else if(block==Blocks.oak_stairs) {
            block = Blocks.planks;
        }else if(block==Blocks.spruce_stairs) {
            block = Blocks.planks;
            meta = 1;
        }else if(block==Blocks.birch_stairs) {
            block = Blocks.planks;
            meta = 2;
        }else if(block==Blocks.jungle_stairs) {
            block = Blocks.planks;
            meta = 3;
        }else if(block==Blocks.acacia_stairs) {
            block = Blocks.planks;
            meta = 4;
        }else if(block==Blocks.dark_oak_stairs) {
            block = Blocks.planks;
            meta = 5;
        }else if(block==Blocks.brick_stairs) {
            block = Blocks.brick_block;
        }else if(block==Blocks.stone_brick_stairs) {
            block = Blocks.stonebrick;
        }else if(block==Blocks.nether_brick_stairs) {
            block = Blocks.nether_brick;
        }else if(block==Blocks.sandstone_stairs) {
            block = Blocks.sandstone;
        }else if(block==Blocks.quartz_stairs) {
            block = Blocks.quartz_block;
        }
        return new BlockAndMeta(block, meta);
    }
}
