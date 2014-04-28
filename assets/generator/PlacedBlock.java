package assets.generator;

import net.minecraft.block.Block;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class PlacedBlock extends BlockAndMeta{
    public final int x, y, z;
    public PlacedBlock(Block block, int[] data) {
        super(block, data[3]);
        this.x = data[0];
        this.y = data[1];
        this.z = data[2];
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
        }else if(obj instanceof PlacedBlock){
            return this.x == ((PlacedBlock) obj).x && this.y == ((PlacedBlock) obj).y && this.z == ((PlacedBlock) obj).z;
        }
        return false;
    }

    @Override
    public int hashCode(){
        return new HashCodeBuilder().append(getMeta()).append(get()).append(x).append(y).append(z).toHashCode();
    }
}
