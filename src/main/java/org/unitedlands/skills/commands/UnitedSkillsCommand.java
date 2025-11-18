package org.unitedlands.skills.commands;

import org.unitedlands.classes.BaseCommandExecutor;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.skills.UnitedSkills;
import org.unitedlands.skills.commands.handlers.ReloadHandler;

public class UnitedSkillsCommand extends BaseCommandExecutor<UnitedSkills> {

    public UnitedSkillsCommand(UnitedSkills plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    protected void registerHandlers() {
        handlers.put("reload", new ReloadHandler(plugin, messageProvider));
    }

}
