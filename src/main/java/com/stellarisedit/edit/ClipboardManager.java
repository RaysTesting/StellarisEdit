package com.stellarisedit.edit;

import com.stellarisedit.StellarisEdit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages clipboard operations (copy/paste) per player.  When copying a region
 * the manager records all block data relative to the minimum corner of the
 * selection.  Pasting replays that block data relative to the player's current
 * location.  Clipboard data is stored until overwritten by a subsequent copy.
 */
public class ClipboardManager {
    private final StellarisEdit plugin;
    private final Map<UUID, Clipboard> clipboardMap = new HashMap<>();

    public ClipboardManager(StellarisEdit plugin) {
        this.plugin = plugin;
    }

    /**
     * Copies the player's current selection into their clipboard.  If no valid
     * selection exists the player is notified.
     */
    public void copy(Player player) {
        Region region = plugin.getSelectionManager().getSelection(player);
        if (region == null) {
            player.sendMessage(ChatColor.RED + "You must set pos1 and pos2 first.");
            return;
        }
        Map<Vector, BlockData> relative = new HashMap<>();
        Vector min = new Vector(region.getMinX(), region.getMinY(), region.getMinZ());
        // iterate through region
        for (int x = region.getMinX(); x <= region.getMaxX(); x++) {
            for (int y = region.getMinY(); y <= region.getMaxY(); y++) {
                for (int z = region.getMinZ(); z <= region.getMaxZ(); z++) {
                    Location loc = new Location(player.getWorld(), x, y, z);
                    BlockData data = loc.getBlock().getBlockData().clone();
                    Vector rel = new Vector(x - min.getBlockX(), y - min.getBlockY(), z - min.getBlockZ());
                    relative.put(rel, data);
                }
            }
        }
        Vector size = new Vector(region.getMaxX() - region.getMinX() + 1, region.getMaxY() - region.getMinY() + 1, region.getMaxZ() - region.getMinZ() + 1);
        clipboardMap.put(player.getUniqueId(), new Clipboard(relative, size));
        player.sendMessage(ChatColor.AQUA + "Copied selection (" + relative.size() + " blocks).");
    }

    /**
     * Pastes the player's clipboard at their current location.  Blocks are placed
     * relative to the block the player is standing on.  The operation is recorded
     * in the history manager for undo support.
     */
    public void paste(Player player) {
        Clipboard clipboard = clipboardMap.get(player.getUniqueId());
        if (clipboard == null) {
            player.sendMessage(ChatColor.RED + "Your clipboard is empty. Use /se copy first.");
            return;
        }
        Location base = player.getLocation().getBlock().getLocation();
        Map<Location, BlockData> before = new HashMap<>();
        Map<Location, BlockData> after = new HashMap<>();
        for (Map.Entry<Vector, BlockData> entry : clipboard.getRelativeBlocks().entrySet()) {
            Vector offset = entry.getKey();
            BlockData data = entry.getValue();
            int tx = base.getBlockX() + offset.getBlockX();
            int ty = base.getBlockY() + offset.getBlockY();
            int tz = base.getBlockZ() + offset.getBlockZ();
            Location target = new Location(player.getWorld(), tx, ty, tz);
            before.put(target, target.getBlock().getBlockData().clone());
            after.put(target, data);
        }
        // Apply changes
        Operation op = new Operation(player.getWorld(), before, after);
        op.apply();
        plugin.getHistoryManager().recordOperation(player, op);
        player.sendMessage(ChatColor.AQUA + "Pasted clipboard at your location (" + after.size() + " blocks).");
    }
}
