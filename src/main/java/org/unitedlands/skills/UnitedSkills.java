package org.unitedlands.skills;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.skills.abilities.*;
import org.unitedlands.skills.commands.PointsCommand;
import org.unitedlands.skills.commands.UnitedSkillsCommand;
import org.unitedlands.skills.events.JobLevelUpEvent;
import org.unitedlands.skills.events.PlayerJoinServerEvent;
import org.unitedlands.skills.hooks.UnitedSkillsPlaceholders;
import org.unitedlands.skills.points.JobsListener;
import org.unitedlands.skills.skill.SkillFile;

import java.util.Objects;

public final class UnitedSkills extends JavaPlugin {

    private PermissionsManager permissions;

    @Override
    public void onEnable() {
        // Ensure config exists before building the managers that read it.
        saveDefaultConfig();
        // Build services.
        this.permissions = new PermissionsManager(getConfig());
        registerCommands();
        registerListeners();
        registerPlaceholderExpansion();
        SkillFile skillFile = new SkillFile(this);
        skillFile.createSkillsFile();
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("unitedskills")).setExecutor(new UnitedSkillsCommand(this));
        Objects.requireNonNull(getCommand("points")).setExecutor(new PointsCommand(this));
    }

    private void registerListeners() {
        final Listener[] listeners = {
                new JobsListener(this),
                new BrewerAbilities(this),
                new FarmerAbilities(this),
                new HunterAbilities(this),
                new DiggerAbilities(this),
                new WoodcutterAbilities(this),
                new FishermanAbilities(this),
                new MinerAbilities(this),
                new MasterworkListener(this),
                new MobNetAbilities(this),
                new JobLevelUpEvent(this.permissions),
                new PlayerJoinServerEvent(this.permissions)
        };

        registerEvents(listeners);

        final HunterAbilities hunterAbilities = new HunterAbilities(this);
        hunterAbilities.damageBleedingEntities();
    }

    private void registerPlaceholderExpansion() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new UnitedSkillsPlaceholders(this).register();
        }
    }

    private void registerEvents(Listener[] listeners) {
        final PluginManager pluginManager = getServer().getPluginManager();
        for (Listener listener : listeners) {
            pluginManager.registerEvents(listener, this);
        }
    }
}
