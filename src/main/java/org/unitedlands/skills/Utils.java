package org.unitedlands.skills;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.skills.points.PlayerConfiguration;
import org.unitedlands.skills.skill.ActiveSkill;


public class Utils {

    public static void multiplyItem(Player player, ItemStack item, int multiplier) {
        for (int i = 0; i < multiplier; i++) {
            player.getInventory().addItem(item);
        }
    }

    public static boolean takeItemFromMaterial(@NotNull Player player, @NotNull Material material) {
        int slot = player.getInventory().first(material);
        if (slot < 0)
            return false;

        ItemStack item = player.getInventory().getItem(slot);
        if (item == null || item.getType().isAir())
            return false;

        item.setAmount(item.getAmount() - 1);
        return true;
    }

    public static boolean takeItem(@NotNull Player player, @NotNull ItemStack item) {
        Inventory inventory = player.getInventory();

        int slot = inventory.first(item.getType());
        if (slot < 0)
            return false;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemInInventory = inventory.getItem(i);
            if (itemInInventory == null || itemInInventory.getType().isAir())
                continue;
            if (itemInInventory.getType().equals(item.getType())) {
                if (itemInInventory.getItemMeta().equals(item.getItemMeta())) {
                    itemInInventory.setAmount(itemInInventory.getAmount() - 1);
                    return true;
                }
            }
        }
        return false;
    }

    // public static boolean isPlaced(Block block) {
    // return PlayerBlockTracker.isTracked(block);
    // }

    public static Boolean wasRecentlyPlaced(Block block) {
        var placeTime = Jobs.getExploitManager().getTime(block);
        if (placeTime != null) {
            // Naturally spawned blocks always return global break time, make
            // sure to stay above that.
            if (placeTime - System.currentTimeMillis() > 60000)
                return true;
        }
        return false;
    }

    public static boolean canActivate(PlayerInteractEvent event, String materialKeyword, ActiveSkill skill) {
        if (event.getItem() == null)
            return false;
        if (!event.getAction().isRightClick())
            return false;

        Player player = event.getPlayer();
        if (!player.isSneaking())
            return false;
        if (!event.getItem().getType().toString().contains(materialKeyword))
            return false;
        return skill.getLevel() != 0;
    }

    public static boolean isInJob(Player player, String jobName) {
        JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
        if (jobsPlayer == null)
            return false;
        for (JobProgression job : jobsPlayer.getJobProgression()) {
            if (job.getJob().getName().equals(jobName)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static PlayerConfiguration getPlayerConfiguration(OfflinePlayer player) {
        return new PlayerConfiguration(UnitedSkills.getInstance(), player);
    }

    public static PlayerConfiguration getPlayerConfiguration(CommandSender sender) {
        if (sender instanceof Player player) {
            return new PlayerConfiguration(UnitedSkills.getInstance(), player);
        }
        return null;
    }

    public static Jobs getJobs() {
        return (Jobs) Bukkit.getPluginManager().getPlugin("Jobs");
    }

}
