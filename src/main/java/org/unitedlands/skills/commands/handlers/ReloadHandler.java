package org.unitedlands.skills.commands.handlers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.skills.UnitedSkills;
import org.unitedlands.skills.skill.SkillFile;
import org.unitedlands.utils.Messenger;

public class ReloadHandler extends BaseCommandHandler<UnitedSkills> {

    public ReloadHandler(UnitedSkills plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        plugin.reloadConfig();
        plugin.getMessageProvider().reload(plugin.getConfig());

        SkillFile skillFile = new SkillFile(plugin);
        skillFile.reloadConfig();

        Messenger.sendMessage(sender, messageProvider.get("messages.reload"), null,
                messageProvider.get("messages.prefix"));
    }

}
