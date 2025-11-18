package org.unitedlands.skills.commands;

import org.unitedlands.classes.BaseCommandExecutor;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.skills.UnitedSkills;
import org.unitedlands.skills.commands.handlers.PointsAddHandler;
import org.unitedlands.skills.commands.handlers.PointsRemoveHandler;

public class PointsCommand extends BaseCommandExecutor<UnitedSkills> {

    public PointsCommand(UnitedSkills plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    protected void registerHandlers() {
        handlers.put("add", new PointsAddHandler(plugin, messageProvider));
        handlers.put("remove", new PointsRemoveHandler(plugin, messageProvider));
    }

}
