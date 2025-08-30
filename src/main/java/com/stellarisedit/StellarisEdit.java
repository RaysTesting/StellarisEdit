package com.stellarisedit;

import com.stellarisedit.command.EditCommand;
import com.stellarisedit.edit.BrushManager;
import com.stellarisedit.edit.ClipboardManager;
import com.stellarisedit.edit.HistoryManager;
import com.stellarisedit.edit.SelectionManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class.  Sets up managers, registers commands and listeners.  The heavy
 * lifting for editing operations is delegated to manager classes to keep the plugin
 * modular and maintainable.  By centralising state here we also ensure per-player
 * isolation for selection, clipboard, brushes and history.
 */
public final class StellarisEdit extends JavaPlugin {

    private SelectionManager selectionManager;
    private ClipboardManager clipboardManager;
    private BrushManager brushManager;
    private HistoryManager historyManager;

    @Override
    public void onEnable() {
        // Instantiate our managers
        this.selectionManager = new SelectionManager(this);
        this.clipboardManager = new ClipboardManager(this);
        this.brushManager = new BrushManager(this);
        this.historyManager = new HistoryManager(this);

        // Register the primary command executor
        EditCommand editCommand = new EditCommand(this);
        getCommand("se").setExecutor(editCommand);
        getCommand("se").setTabCompleter(editCommand);

        // Register listeners for selection wand and brushes
        getServer().getPluginManager().registerEvents(this.selectionManager, this);
        getServer().getPluginManager().registerEvents(this.brushManager, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic.  At present nothing to clean up explicitly.
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public ClipboardManager getClipboardManager() {
        return clipboardManager;
    }

    public BrushManager getBrushManager() {
        return brushManager;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }
}
