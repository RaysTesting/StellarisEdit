package com.stellarisedit.command;

import com.stellarisedit.StellarisEdit;
import com.stellarisedit.edit.BrushManager;
import com.stellarisedit.edit.Region;
import com.stellarisedit.edit.Operation;
import org.bukkit.Location;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Handles the "/se" command and its subcommands.  Delegates functionality to
 * the appropriate managers defined in the main plugin.
 */
public class EditCommand implements CommandExecutor, TabCompleter {
    private final StellarisEdit plugin;

    public EditCommand(StellarisEdit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "wand":
                plugin.getSelectionManager().giveWand(player);
                return true;
            case "set":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /se set <blockdata> [mask=materials]");
                    return true;
                }
                handleSet(player, args);
                return true;
            case "replace":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /se replace <from> <to>");
                    return true;
                }
                handleReplace(player, args);
                return true;
            case "copy":
                plugin.getClipboardManager().copy(player);
                return true;
            case "paste":
                plugin.getClipboardManager().paste(player);
                return true;
            case "undo":
                plugin.getHistoryManager().undo(player);
                return true;
            case "redo":
                plugin.getHistoryManager().redo(player);
                return true;
            case "brush":
                handleBrush(player, args);
                return true;
            default:
                sendHelp(player);
                return true;
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.AQUA + "StellarisEdit commands:");
        player.sendMessage(ChatColor.YELLOW + "/se wand" + ChatColor.WHITE + " - get selection wand");
        player.sendMessage(ChatColor.YELLOW + "/se set <blockdata> [mask=materials]" + ChatColor.WHITE + " - fill selection with block");
        player.sendMessage(ChatColor.YELLOW + "/se replace <from> <to>" + ChatColor.WHITE + " - replace blocks in selection");
        player.sendMessage(ChatColor.YELLOW + "/se copy" + ChatColor.WHITE + " - copy selection to clipboard");
        player.sendMessage(ChatColor.YELLOW + "/se paste" + ChatColor.WHITE + " - paste clipboard at your location");
        player.sendMessage(ChatColor.YELLOW + "/se undo / redo" + ChatColor.WHITE + " - undo/redo last operation");
        player.sendMessage(ChatColor.YELLOW + "/se brush sphere <radius> <blockdata> [mask=materials]" + ChatColor.WHITE + " - create sphere brush");
    }

    private void handleSet(Player player, String[] args) {
        // args[1] blockdata, optional mask after
        String blockString = args[1];
        BlockData data;
        try {
            data = player.getServer().createBlockData(blockString);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ChatColor.RED + "Invalid block data: " + blockString);
            return;
        }
        String maskString = null;
        if (args.length >= 3 && args[2].toLowerCase(Locale.ROOT).startsWith("mask=")) {
            maskString = args[2].substring(5);
        }
        Set<Material> mask = BrushManager.parseMask(maskString);
        Region region = plugin.getSelectionManager().getSelection(player);
        if (region == null) {
            player.sendMessage(ChatColor.RED + "You must set pos1 and pos2 first.");
            return;
        }
        Map<Location, BlockData> before = new HashMap<>();
        Map<Location, BlockData> after = new HashMap<>();
        for (int x = region.getMinX(); x <= region.getMaxX(); x++) {
            for (int y = region.getMinY(); y <= region.getMaxY(); y++) {
                for (int z = region.getMinZ(); z <= region.getMaxZ(); z++) {
                    Location loc = new Location(player.getWorld(), x, y, z);
                    if (mask != null && !mask.contains(loc.getBlock().getType())) {
                        continue;
                    }
                    before.put(loc, loc.getBlock().getBlockData().clone());
                    after.put(loc, data);
                }
            }
        }
        Operation op = new Operation(player.getWorld(), before, after);
        op.apply();
        plugin.getHistoryManager().recordOperation(player, op);
        player.sendMessage(ChatColor.GREEN + "Set " + after.size() + " blocks.");
    }

    private void handleReplace(Player player, String[] args) {
        String fromString = args[1];
        String toString = args[2];
        BlockData from;
        BlockData to;
        try {
            from = player.getServer().createBlockData(fromString);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ChatColor.RED + "Invalid from block: " + fromString);
            return;
        }
        try {
            to = player.getServer().createBlockData(toString);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ChatColor.RED + "Invalid to block: " + toString);
            return;
        }
        Region region = plugin.getSelectionManager().getSelection(player);
        if (region == null) {
            player.sendMessage(ChatColor.RED + "You must set pos1 and pos2 first.");
            return;
        }
        Map<Location, BlockData> before = new HashMap<>();
        Map<Location, BlockData> after = new HashMap<>();
        for (int x = region.getMinX(); x <= region.getMaxX(); x++) {
            for (int y = region.getMinY(); y <= region.getMaxY(); y++) {
                for (int z = region.getMinZ(); z <= region.getMaxZ(); z++) {
                    Location loc = new Location(player.getWorld(), x, y, z);
                    BlockData current = loc.getBlock().getBlockData();
                    if (current.matches(from)) {
                        before.put(loc, current.clone());
                        after.put(loc, to);
                    }
                }
            }
        }
        if (after.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No blocks matched " + fromString + ".");
            return;
        }
        Operation op = new Operation(player.getWorld(), before, after);
        op.apply();
        plugin.getHistoryManager().recordOperation(player, op);
        player.sendMessage(ChatColor.GREEN + "Replaced " + after.size() + " blocks.");
    }

    private void handleBrush(Player player, String[] args) {
        // /se brush sphere <radius> <blockdata> [mask=materials]
        if (args.length < 4 || !args[1].equalsIgnoreCase("sphere")) {
            player.sendMessage(ChatColor.RED + "Usage: /se brush sphere <radius> <blockdata> [mask=materials]");
            return;
        }
        int radius;
        try {
            radius = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Invalid radius: " + args[2]);
            return;
        }
        String blockString = args[3];
        BlockData data;
        try {
            data = player.getServer().createBlockData(blockString);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ChatColor.RED + "Invalid block data: " + blockString);
            return;
        }
        String maskString = null;
        if (args.length >= 5 && args[4].toLowerCase(Locale.ROOT).startsWith("mask=")) {
            maskString = args[4].substring(5);
        }
        Set<Material> mask = BrushManager.parseMask(maskString);
        plugin.getBrushManager().setBrush(player, radius, data, mask);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> options = Arrays.asList("wand", "set", "replace", "copy", "paste", "undo", "redo", "brush");
            for (String opt : options) {
                if (opt.startsWith(prefix)) completions.add(opt);
            }
            return completions;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("brush")) {
            if (args.length == 2) {
                if ("sphere".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    completions.add("sphere");
                }
                return completions;
            }
        }
        return completions;
    }
}
