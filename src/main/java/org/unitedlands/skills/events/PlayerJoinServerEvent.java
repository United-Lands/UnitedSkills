package org.unitedlands.skills.events;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.unitedlands.skills.PermissionsManager;

public final class PlayerJoinServerEvent implements Listener {

    private final PermissionsManager permissions;

    public PlayerJoinServerEvent(PermissionsManager permissions) {
        this.permissions = permissions;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        JobsPlayer jp = Jobs.getPlayerManager().getJobsPlayer(e.getPlayer());
        if (jp == null) return;

        for (JobProgression prog : jp.getJobProgression()) {
            permissions.applyCumulative(e.getPlayer(), prog.getJob().getName(), prog.getLevel());
        }
    }
}