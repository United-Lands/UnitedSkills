package org.unitedlands.skills;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public final class PermissionsManager {

    private final Map<String, NavigableMap<Integer, List<String>>> table;
    private final LuckPerms luckPerms;

    public PermissionsManager(org.bukkit.configuration.file.FileConfiguration cfg) {
        this.table = loadTable(cfg.getConfigurationSection("permissions"));
        this.luckPerms = LuckPermsProvider.get();
    }

    // Apply permissions (if applicable) on levelup.
    public void applyOnLevelUp(Player player, String jobName, int level) {
        String key = jobName.toLowerCase(Locale.ROOT);
        NavigableMap<Integer, List<String>> levels = table.get(key);
        if (levels == null) return;

        List<String> nodes = levels.get(level);
        if (nodes == null || nodes.isEmpty()) return;

        grant(player, nodes);
    }

    // Grant the correct permission to the player.
    private void grant(Player player, Collection<String> nodes) {
        // This helper will load, edit, and save the user's perms asynchronously.
        luckPerms.getUserManager().modifyUser(player.getUniqueId(), (User user) -> {
            for (String n : nodes) {
                user.data().add(PermissionNode.builder(n).value(true).build());
            }
            player.getServer().getLogger().info(
                    "[UnitedSkills] Granted " + nodes + " to " + player.getName()
            );
        });
    }

    private Map<String, NavigableMap<Integer, List<String>>> loadTable(ConfigurationSection root) {
        Map<String, NavigableMap<Integer, List<String>>> map = new HashMap<>();
        if (root == null) return map;

        for (String job : root.getKeys(false)) {
            NavigableMap<Integer, List<String>> perLevel = new TreeMap<>();
            ConfigurationSection jobSec = root.getConfigurationSection(job);
            if (jobSec == null) continue;

            for (String lvlKey : jobSec.getKeys(false)) {
                try {
                    int lvl = Integer.parseInt(lvlKey);
                    List<String> nodes = jobSec.getStringList(lvlKey);
                    if (!nodes.isEmpty()) perLevel.put(lvl, nodes);
                } catch (NumberFormatException ignored) {}
            }
            if (!perLevel.isEmpty()) map.put(job.toLowerCase(Locale.ROOT), perLevel);
        }
        return map;
    }
}
