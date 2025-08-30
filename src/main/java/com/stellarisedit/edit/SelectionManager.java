package com.stellarisedit.edit;

import com.stellarisedit.StellarisEdit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
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
import java.util.Map;
import java.util.UUID;

/**
 * Handles selection logic for players.  Players can set two corners of a cuboid
 * region using the selection wand (a customised wooden axe).  The manager
 * persists each player's pos1 and pos2 vectors.  It also listens for
 * interaction events to update selections when the wand is used.
 */
public class SelectionManager implements Listener {
    private static final Material WAND_MATERIAL = Material.WOODEN_AXE;
    private static final String WAND_NAME = ChatColor.GREEN + "Stellaris Wand";
    private static final NamespacedKey WAND_KEY = new NamespacedKey("stellarisedit", "wand");

    private final StellarisEdit plugin;
    private final Map<UUID, Vector> pos1Map = new HashMap<>();
    private final Map<UUID, Vector> pos2Map = new HashMap<>();

    public SelectionManager(StellarisEdit plugin) {
        this.plugin = plugin;
    }

    /**
     * Gives the selection wand to the player.  The wand is a wooden axe with a custom
     * name and a persistent data flag so we can detect it reliably.
     */
    public void giveWand(Player player) {
        ItemStack wand = new ItemStack(WAND_MATERIAL);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(WAND_NAME);
            // set persistent data flag
            meta.getPersistentDataContainer().set(WAND_KEY, PersistentDataType.BYTE, (byte) 1);
            wand.setItemMeta(meta);
        }
        player.getInventory().addItem(wand);
        player.sendMessage(ChatColor.AQUA + "You have been given the Stellaris selection wand.");
    }

    /**
     * Gets the first position vector for the given player.
     */
    public Vector getPos1(UUID uuid) {
        return pos1Map.get(uuid);
    }

    /**
     * Gets the second position vector for the given player.
     */
    public Vector getPos2(UUID uuid) {
        return pos2Map.get(uuid);
    }

    /**
     * Sets the first position for the player.
     */
    private void setPos1(Player player, Vector pos) {
        pos1Map.put(player.getUniqueId(), pos);
        player.sendMessage(ChatColor.YELLOW + "Pos1 set to: " + format(pos));
    }

    /**
     * Sets the second position for the player.
     */
    private void setPos2(Player player, Vector pos) {
        pos2Map.put(player.getUniqueId(), pos);
        player.sendMessage(ChatColor.YELLOW + "Pos2 set to: " + format(pos));
    }

    /**
     * Calculates the region corners (min and max) for the player's current selection.
     * Returns null if either pos1 or pos2 is not set.
     */
    public Region getSelection(Player player) {
        Vector a = pos1Map.get(player.getUniqueId());
        Vector b = pos2Map.get(player.getUniqueId());
        if (a == null || b == null) {
            return null;
        }
        return new Region(a, b);
    }

    /**
     * Event handler for wand usage.  When the player left or right clicks a block
     * with the wand, set pos1 or pos2 respectively.  We cancel the event to
     * prevent the default block breaking/placing behaviour.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType() != WAND_MATERIAL) {
            return;
        }
        ItemMeta meta = event.getItem().getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(WAND_KEY, PersistentDataType.BYTE)) {
            return;
        }
        Player player = event.getPlayer();
        if (event.getClickedBlock() != null && (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            Block block = event.getClickedBlock();
            Vector pos = block.getLocation().toVector();
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                setPos1(player, pos);
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                setPos2(player, pos);
            }
            event.setCancelled(true);
        }
    }

    private String format(Vector v) {
        return ChatColor.GRAY + "(" + v.getBlockX() + ", " + v.getBlockY() + ", " + v.getBlockZ() + ")";
    }
}
