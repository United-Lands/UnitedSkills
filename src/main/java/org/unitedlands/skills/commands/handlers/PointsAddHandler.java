package org.unitedlands.skills.commands.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.skills.UnitedSkills;
import org.unitedlands.skills.Utils;
import org.unitedlands.skills.points.PlayerConfiguration;
import org.unitedlands.utils.Messenger;

public class PointsAddHandler extends BaseCommandHandler<UnitedSkills> {

    public PointsAddHandler(UnitedSkills plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    private List<String> joblist = List.of("farmer", "hunter", "woodcutter", "fisherman", "digger", "miner", "brewer");

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        if (args.length == 0) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        } else if (args.length == 1) {
            return joblist;
        }

        return new ArrayList<>();
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 3) {
            Messenger.sendMessage(sender, messageProvider.get("messages.points-usage"), null,
                    messageProvider.get("messages.prefix"));
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        if (player == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.points-error-player-not-found"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        String jobName = args[1];

        Integer amount = 0;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (Exception ex) {
            Messenger.sendMessage(sender, messageProvider.get("messages.points-error-invalid-number"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        PlayerConfiguration playerConfiguration = Utils.getPlayerConfiguration(player);
        playerConfiguration.increaseJobPoints(jobName, amount);

        Messenger.sendMessage(sender, messageProvider.get("messages.points-added"),
                Map.of("player-name", player.getName(), "job-name", jobName, "points", amount.toString()),
                messageProvider.get("messages.prefix"));

    }

}
