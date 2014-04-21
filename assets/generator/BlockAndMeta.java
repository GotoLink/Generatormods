package assets.generator;

import net.minecraft.block.Block;
import net.minecraft.util.Tuple;

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
        return getMeta()+31*get().hashCode();
    }
}
