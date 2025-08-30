package com.stellarisedit.edit;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a reversible world editing operation.  Operations capture the original
 * block states before modification and the new block states after modification.
 * Calling undo() restores the original blocks, while redo() re-applies the new
 * states.  Operations are recorded in a player's history to support undo/redo
 * functionality.
 */
public class Operation {
    private final Map<Location, BlockData> before;
    private final Map<Location, BlockData> after;
    private final World world;

    public Operation(World world, Map<Location, BlockData> before, Map<Location, BlockData> after) {
        this.world = world;
        this.before = new HashMap<>(before);
        this.after = new HashMap<>(after);
    }

    /**
     * Applies the new block states represented by this operation.
     */
    public void apply() {
        for (Map.Entry<Location, BlockData> entry : after.entrySet()) {
            Location loc = entry.getKey();
            BlockData data = entry.getValue();
            Block block = world.getBlockAt(loc);
            block.setBlockData(data, false);
        }
    }

    /**
     * Reverts the blocks in this operation back to their original states.
     */
    public void revert() {
        for (Map.Entry<Location, BlockData> entry : before.entrySet()) {
            Location loc = entry.getKey();
            BlockData data = entry.getValue();
            Block block = world.getBlockAt(loc);
            block.setBlockData(data, false);
        }
    }
}
