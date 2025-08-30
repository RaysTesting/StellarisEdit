package com.stellarisedit.edit;

import com.stellarisedit.StellarisEdit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the undo/redo history for players.  Each player gets their own stacks
 * of operations.  When a new operation is recorded, it is pushed onto the undo
 * stack and the redo stack is cleared.  Undo pops from the undo stack and
 * pushes onto the redo stack.  Redo pops from the redo stack and pushes back
 * onto the undo stack.  The history size is capped by a configurable limit.
 */
public class HistoryManager {
    private final StellarisEdit plugin;
    private final Map<UUID, Deque<Operation>> undoMap = new HashMap<>();
    private final Map<UUID, Deque<Operation>> redoMap = new HashMap<>();
    private final int maxHistory;

    public HistoryManager(StellarisEdit plugin) {
        this.plugin = plugin;
        this.maxHistory = plugin.getConfig().getInt("undo-limit", 20);
    }

    /**
     * Records a new operation for the given player.  Clears the redo stack.
     */
    public void recordOperation(Player player, Operation op) {
        UUID uuid = player.getUniqueId();
        Deque<Operation> undoStack = undoMap.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        Deque<Operation> redoStack = redoMap.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        // Add new operation
        undoStack.push(op);
        // Trim if exceeding max
        while (undoStack.size() > maxHistory) {
            undoStack.removeLast();
        }
        // Clear redo history
        redoStack.clear();
    }

    /**
     * Undo the last operation for the player.
     */
    public void undo(Player player) {
        UUID uuid = player.getUniqueId();
        Deque<Operation> undoStack = undoMap.get(uuid);
        if (undoStack == null || undoStack.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Nothing to undo.");
            return;
        }
        Operation op = undoStack.pop();
        op.revert();
        redoMap.computeIfAbsent(uuid, k -> new ArrayDeque<>()).push(op);
        player.sendMessage(ChatColor.YELLOW + "Undo complete.");
    }

    /**
     * Redo the last undone operation for the player.
     */
    public void redo(Player player) {
        UUID uuid = player.getUniqueId();
        Deque<Operation> redoStack = redoMap.get(uuid);
        if (redoStack == null || redoStack.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Nothing to redo.");
            return;
        }
        Operation op = redoStack.pop();
        op.apply();
        undoMap.computeIfAbsent(uuid, k -> new ArrayDeque<>()).push(op);
        player.sendMessage(ChatColor.YELLOW + "Redo complete.");
    }
}
