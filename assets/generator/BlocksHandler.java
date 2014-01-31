package assets.generator;

import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public final class BlocksHandler {
    public static HashMap<Integer, BlocksHandler> handlers = new HashMap<Integer, BlocksHandler>();
    private HashMap<Integer, Block> conversionData;

    public BlocksHandler(WorldServer world){
        if(conversionData==null){
            conversionData = new HashMap<Integer, Block>();
            File savePath = new File(world.getChunkSaveLocation().getParentFile(), "GeneratorModsBlocks.dat");
            if (!savePath.canRead() || !readConversionFrom(savePath)) {
                buildConversionTo(savePath);
            }
        }
        handlers.put(world.provider.dimensionId, this);
    }

    public BlocksHandler(int dimension){
        this(DimensionManager.getWorld(dimension));
    }

    @Override
    public int hashCode(){
        return conversionData.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        return conversionData.equals(((BlocksHandler) o).conversionData);
    }

    public static Block get(int dimension, int id){
        if(!handlers.containsKey(dimension)){
            return Blocks.air;
        }
        return handlers.get(dimension).get(id);
    }

    public Block get(int id){
        return conversionData.containsKey(id)?conversionData.get(id): Blocks.air;
    }

    private final void buildConversionTo(File savePath){
        try {
            PrintWriter printer = new PrintWriter(new BufferedWriter(new FileWriter(savePath)));
            HashMap<String, Integer> mappings = new HashMap<String, Integer>();
            GameData.blockRegistry.serializeInto(mappings);
            int internalID = 0;
            for(Map.Entry<String, Integer> entry : mappings.entrySet()){
                String blockName = entry.getKey();
                Integer forgeID = entry.getValue();
                printer.println(blockName+","+forgeID+","+internalID);
                conversionData.put(internalID, GameData.blockRegistry.get(forgeID));
                internalID++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final boolean readConversionFrom(File savePath){
        try{
            BufferedReader reader = new BufferedReader(new FileReader(savePath));
            for (String read = reader.readLine(); read != null; read = reader.readLine()) {
                String[] values = read.split(",");
                if(values.length!=3){
                    return false;
                }
                int key;
                try{
                    key = Integer.parseInt(values[2]);
                }catch (NumberFormatException n){
                    return false;
                }
                Block block = null;
                try{
                    block = GameData.blockRegistry.get(Integer.parseInt(values[1]));
                }catch(Exception e){
                    block = GameData.blockRegistry.get(values[0]);
                }
                if(block==null){
                    return false;
                }
                conversionData.put(key, block);
            }
            return true;
        }catch(Exception e){
            return false;
        }
    }
}
