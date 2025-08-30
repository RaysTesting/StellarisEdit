package com.stellarisedit.edit;

import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A clipboard stores a collection of block data relative to an origin.  The origin
 * represents the lowest X/Y/Z coordinate of the copied region so that pastes are
 * properly aligned relative to the player's position.  The internal map keys
 * represent offsets from the origin.
 */
public class Clipboard {
    private final Map<Vector, BlockData> relativeBlocks;
    private final Vector size;

    public Clipboard(Map<Vector, BlockData> relativeBlocks, Vector size) {
        this.relativeBlocks = new HashMap<>(relativeBlocks);
        this.size = size.clone();
    }

    public Map<Vector, BlockData> getRelativeBlocks() {
        return Collections.unmodifiableMap(relativeBlocks);
    }

    public Vector getSize() {
        return size.clone();
    }
}
