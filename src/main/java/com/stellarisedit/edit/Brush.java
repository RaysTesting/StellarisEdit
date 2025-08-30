package com.stellarisedit.edit;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a spherical brush.  Contains the radius, block data used for
 * painting and an optional mask specifying which materials can be replaced.
 */
public class Brush {
    private final int radius;
    private final BlockData blockData;
    private final Set<Material> mask;

    public Brush(int radius, BlockData blockData, Set<Material> mask) {
        this.radius = radius;
        this.blockData = blockData;
        this.mask = mask != null ? new HashSet<>(mask) : null;
    }

    public int getRadius() {
        return radius;
    }

    public BlockData getBlockData() {
        return blockData;
    }

    public boolean isAllowed(Material material) {
        return mask == null || mask.contains(material);
    }
}
