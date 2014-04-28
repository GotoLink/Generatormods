package assets.generator;

import net.minecraft.block.Block;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class BlockExtended extends BlockAndMeta{
    public final String info;
    public BlockExtended(Block block, int meta, String extra) {
        super(block, meta);
        info = extra;
    }

    @Override
    public boolean equals(Object obj){
        if(obj==null){
            return false;
        }
        if(obj==this){
            return true;
        }
        if(!super.equals(obj)){
            return false;
        }else if(obj instanceof BlockExtended){
            return this.info.equals(((BlockExtended) obj).info);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return new HashCodeBuilder().append(getMeta()).append(get()).append(info).toHashCode();
    }
}
