package assets.generator;

/*
 *  Source code for the CellullarAutomata Ruins for the game Minecraft
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

import java.io.PrintWriter;

/**
 * Represents a CellullarAutomata rule, encapsulating seed, weight, and optional comment
 */
public class CARule implements RandomPicker.IWeighted {
    public final static byte DEAD = 0, ALIVE = 1;
    private byte[] birth = new byte[9], survive = new byte[9];
    private final int weight;
    public String comment;

    public CARule(String line, PrintWriter lw) throws IllegalArgumentException{
        if (line.startsWith("B") || line.startsWith("b")) {
            String[] str = line.split(",");
            if (str[0].contains("/")) {
                String[] subs = str[0].split("/", 2);
                parse(subs[0].trim(), true, lw);
                parse(subs[1].trim(), false, lw);
            }else
                throw new IllegalArgumentException("Couldn't split birth/survive values");
            weight = BuildingExplorationHandler.readIntParam(lw, 1, "=", str.length>1?str[1].trim():"");
        }else
            throw new IllegalArgumentException("Line doesn't start with birth value");
    }

    public CARule(String birth, String survive, int weight, String comment){
        parse(birth, true, null);
        parse(survive, false, null);
        this.weight = weight;
        this.comment = comment;
    }

    /**
     * Parser for seed bytes
     * @param sub to parse
     * @param isBirth if data is for {@code birth} or {@code survive} bytes
     * @param lw optional writer to print error message into
     */
    private void parse(String sub, boolean isBirth, PrintWriter lw){
        for (int n = 1; n < sub.length(); n++) {
            try {
                int digit = Integer.parseInt(sub.substring(n, n + 1));
                if (isBirth) {
                    if (digit >= 0 && digit < birth.length)
                        birth[digit] = ALIVE;
                } else {
                    if (digit >= 0 && digit < survive.length)
                        survive[digit] = ALIVE;
                }
            }catch (Throwable t){
                if (lw != null)
                    lw.println("Error parsing automaton rule :" + t.getMessage());
            }
        }
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder(30);
        sb.append("B");
        for (int n = 0; n < birth.length; n++)
            if (birth[n] == ALIVE)
                sb.append(n);
        sb.append("/S");
        for (int n = 0; n < survive.length; n++)
            if (survive[n] == ALIVE)
                sb.append(n);
        sb.append(", weight=");
        sb.append(weight);
        if(comment!=null){
            sb.append(",  <-").append(comment).append("->");
        }
        return sb.toString();
    }

    @Override
    public int getWeight(){
        return weight;
    }

    public byte[] getBytes(boolean isBirth){
        return isBirth ? birth : survive;
    }

    public boolean isAlive(boolean isBirth, int index){
        return getBytes(isBirth)[index] == ALIVE;
    }
}
