package com.stellarisedit.edit;

import com.stellarisedit.StellarisEdit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;

/**
 * Manages sphere brushes.  Players can create a brush with a given radius
 * and block data (and optional mask).  They are given a blaze rod labelled
 * "Stellaris Brush" which will paint spheres when right-clicked against a block.
 */
public class BrushManager implements Listener {
    private static final Material BRUSH_MATERIAL = Material.BLAZE_ROD;
    private static final String BRUSH_NAME = ChatColor.LIGHT_PURPLE + "Stellaris Brush";
    private static final NamespacedKey BRUSH_KEY = new NamespacedKey("stellarisedit", "brush");

    private final StellarisEdit plugin;
    private final Map<UUID, Brush> brushMap = new HashMap<>();

    public BrushManager(StellarisEdit plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a new brush for the player with the given radius, block data and
     * optional mask.  The player receives a blaze rod brush item.
     */
    public void setBrush(Player player, int radius, BlockData blockData, Set<Material> mask) {
        if (radius < 1) {
            player.sendMessage(ChatColor.RED + "Radius must be at least 1.");
            return;
        }
        Brush brush = new Brush(radius, blockData, mask);
        brushMap.put(player.getUniqueId(), brush);
        // Give brush item
        ItemStack item = new ItemStack(BRUSH_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(BRUSH_NAME);
            meta.getPersistentDataContainer().set(BRUSH_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.AQUA + "Sphere brush set (r=" + radius + "). Use the brush item to paint.");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != BRUSH_MATERIAL) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(BRUSH_KEY, PersistentDataType.BYTE)) {
            return;
        }
        Player player = event.getPlayer();
        Brush brush = brushMap.get(player.getUniqueId());
        if (brush == null) {
            player.sendMessage(ChatColor.RED + "You have no brush configured. Use /se brush sphere ...");
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        // Build sphere
        int r = brush.getRadius();
        Vector center = clicked.getLocation().toVector().add(new Vector(0.5, 0.5, 0.5));
        Map<Location, BlockData> before = new HashMap<>();
        Map<Location, BlockData> after = new HashMap<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (distance <= r + 0.5) {
                        int x = clicked.getX() + dx;
                        int y = clicked.getY() + dy;
                        int z = clicked.getZ() + dz;
                        if (y < 0 || y >= player.getWorld().getMaxHeight()) {
                            continue;
                        }
                        Location loc = new Location(player.getWorld(), x, y, z);
                        Material current = loc.getBlock().getType();
                        if (!brush.isAllowed(current)) {
                            continue;
                        }
                        before.put(loc, loc.getBlock().getBlockData().clone());
                        after.put(loc, brush.getBlockData());
                    }
                }
            }
        }
        Operation op = new Operation(player.getWorld(), before, after);
        op.apply();
        plugin.getHistoryManager().recordOperation(player, op);
        player.sendMessage(ChatColor.GREEN + "Painted sphere with radius " + r + ".");
        event.setCancelled(true);
    }

    /**
     * Parses a mask argument string into a set of Materials.  The mask must be
     * provided as a comma-separated list of material names (e.g. "STONE,DIRT").
     */
    public static Set<Material> parseMask(String maskString) {
        if (maskString == null || maskString.trim().isEmpty()) {
            return null;
        }
        Set<Material> set = new HashSet<>();
        String[] parts = maskString.split(",");
        for (String part : parts) {
            try {
                Material m = Material.matchMaterial(part.trim());
                if (m != null) {
                    set.add(m);
                }
            } catch (Exception ignored) {
            }
        }
        return set.isEmpty() ? null : set;
    }
}
