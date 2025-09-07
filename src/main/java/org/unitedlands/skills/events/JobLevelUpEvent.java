package org.unitedlands.skills.events;


import com.gamingmesh.jobs.api.JobsLevelUpEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.unitedlands.skills.PermissionsManager;

public class JobLevelUpEvent implements Listener {

    private final PermissionsManager permissions;

    public JobLevelUpEvent(PermissionsManager permissions) {
        this.permissions = permissions;
    }

    @EventHandler(ignoreCancelled = true)
    public void onJobsLevelUp(JobsLevelUpEvent event) {
        Player player = event.getPlayer().getPlayer().getPlayer();
        if (player == null) return; // Just in case player is offline for some reason.

        permissions.applyOnLevelUp(player, event.getJob().getName(), event.getLevel());
    }
}