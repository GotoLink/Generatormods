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

import java.util.List;
import java.util.Random;

/**
 * Allows picking at random from a collection
 */
public class RandomPicker {

    public static int pickWeightedOption(Random random, List<? extends IWeighted> weights){
        int[] w = new int[weights.size()];
        for (int i= 0; i < w.length; i++)
            w[i]= weights.get(i).getWeight();
        return pickWeightedOption(random, w);
    }

    public static int pickWeightedOption(Random random, IWeighted[] weights){
        int[] w = new int[weights.length];
        for (int i= 0; i < w.length; i++)
            w[i]= weights[i].getWeight();
        return pickWeightedOption(random, w);
    }

    public static int pickWeightedOption(Random random, int[] weights) {
        int sum = 0, n;
        for (n = 0; n < weights.length; n++)
            sum += weights[n];
        if (sum <= 0) {
            System.err.println("Error selecting options, weightsum not positive!");
            return 0; // default to returning first option
        }
        int s = random.nextInt(sum);
        sum = 0;
        n = 0;
        while (n < weights.length) {
            sum += weights[n];
            if (sum > s)
                return n;
            n++;
        }
        return weights.length - 1;
    }

    /**
     * Defines an object with relative weight for random picking in a collection
     */
    public static interface IWeighted {

        public int getWeight();
    }
}
