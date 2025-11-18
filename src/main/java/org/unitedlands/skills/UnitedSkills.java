package org.unitedlands.skills;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.skills.abilities.*;
import org.unitedlands.skills.commands.PointsCommand;
import org.unitedlands.skills.commands.UnitedSkillsCommand;
import org.unitedlands.skills.events.JobLevelUpEvent;
import org.unitedlands.skills.events.PlayerJoinServerEvent;
import org.unitedlands.skills.hooks.UnitedSkillsPlaceholders;
import org.unitedlands.skills.points.JobsListener;
import org.unitedlands.skills.skill.SkillFile;

public final class UnitedSkills extends JavaPlugin {

    private static UnitedSkills instance;

    private MessageProvider messageProvider;
    private PermissionsManager permissions;

    @Override
    public void onEnable() {

        instance = this;

        // Ensure config exists before building the managers that read it.
        saveDefaultConfig();

        messageProvider = new MessageProvider(getConfig());

        // Build services.
        this.permissions = new PermissionsManager(getConfig());
        registerCommands();
        registerListeners();
        registerPlaceholderExpansion();

        SkillFile skillFile = new SkillFile(this);
        skillFile.createSkillsFile();
    }

    private void registerCommands() {

        var unitedSkillsCmd = new UnitedSkillsCommand(instance, messageProvider);
        getCommand("unitedskills").setTabCompleter(unitedSkillsCmd);
        getCommand("unitedskills").setExecutor(unitedSkillsCmd);

        var pointsCmd = new PointsCommand(instance, messageProvider);
        getCommand("points").setTabCompleter(pointsCmd);
        getCommand("points").setExecutor(pointsCmd);
    }

    private void registerListeners() {
        final Listener[] listeners = {
                new JobsListener(this),
                new BrewerAbilities(this, messageProvider),
                new FarmerAbilities(this),
                new HunterAbilities(this),
                new DiggerAbilities(this),
                new WoodcutterAbilities(this),
                new FishermanAbilities(this),
                new MinerAbilities(this),
                new MasterworkListener(this),
                new MobNetAbilities(this, messageProvider),
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

    public static UnitedSkills getInstance() {
        return instance;
    }

    public IMessageProvider getMessageProvider() {
        return messageProvider;
    }
}
