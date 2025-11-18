package org.unitedlands.skills.abilities;

import com.gamingmesh.jobs.container.blockOwnerShip.BlockOwnerShip;
import com.gamingmesh.jobs.container.blockOwnerShip.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.skills.MessageProvider;
import org.unitedlands.skills.UnitedSkills;
import org.unitedlands.skills.Utils;
import org.unitedlands.skills.skill.Skill;
import org.unitedlands.skills.skill.SkillType;
import org.unitedlands.utils.Messenger;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.unitedlands.skills.Utils.*;

public class BrewerAbilities implements Listener {
    private final UnitedSkills plugin;
    private final MessageProvider messageProvider;

    public BrewerAbilities(UnitedSkills unitedSkills, MessageProvider messageProvider) {
        this.plugin = unitedSkills;
        this.messageProvider = messageProvider;
    }

    @EventHandler
    public void onPotionBrew(BrewEvent event) {
        Block block = event.getBlock();
        BrewingStand brewingStand = (BrewingStand) block.getState();
        var player = getBrewingStandOwner(block);
        if (player == null) {
            return;
        }
        Skill qualityIngredients = new Skill(player, SkillType.QUALITY_INGREDIENTS);
        if (qualityIngredients.getLevel() > 0) {
            return;
        }
        for (ItemStack item : event.getContents()) {
            PotionMeta potionMeta = (PotionMeta) item.getItemMeta();

            var basePotionType = potionMeta.getBasePotionType();
            if (basePotionType.toString().startsWith("LONG_") || basePotionType.toString().startsWith("STRONG_")) {
                return;
            }

            var enhancedPotionType = PotionType.valueOf("LONG_" + basePotionType.toString());
            potionMeta.setBasePotionType(enhancedPotionType);

            item.setItemMeta(potionMeta);
            brewingStand.update();
        }
    }

    @EventHandler
    public void onBrewingStart(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!inventory.getType().equals(InventoryType.BREWING)) {
            return;
        }
        var player = (Player) event.getWhoClicked();
        if (!isBrewer(player)) {
            return;
        }
        Skill modifiedHardware = new Skill(player, SkillType.MODIFIED_HARDWARE);
        if (modifiedHardware.getLevel() == 0) {
            return;
        }
        Block brewingStandBlock = Objects.requireNonNull(inventory.getLocation()).getBlock();
        BrewingStand brewingStand = (BrewingStand) brewingStandBlock.getState(false);
        if (!ownsBrewingStand(player, brewingStandBlock)) {
            return;
        }
        if (canStartBrewing(brewingStand)) {
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, task -> {
            if (canStartBrewing(brewingStand)) {
                return;
            }
            if (brewingStand.getBrewingTime() == 0) {
                return;
            }
            int defaultTime = 20;
            int brewingTime = (int) (defaultTime * (1 - (modifiedHardware.getLevel() * 0.10)));
            brewingStand.setBrewingTime(brewingTime * 20);
            brewingStand.update();
        }, 2);
    }

    private boolean canStartBrewing(BrewingStand brewingStand) {
        if (hasBottleOrPotion(brewingStand.getInventory()))
            return false;
        if (hasBrewingItem(brewingStand.getInventory()))
            return false;
        return !hasBlazePowder(brewingStand);
    }

    private boolean hasBottleOrPotion(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item == null) {
                continue;
            }
            if (item.getType().equals(Material.POTION) || item.getType().equals(Material.SPLASH_POTION)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBrewingItem(@NotNull Inventory inventory) {
        FileConfiguration config = plugin.getConfig();
        @NotNull
        List<String> brewingItems = config.getStringList("brewing-items");
        ItemStack item = inventory.getItem(3);
        if (item == null) {
            return false;
        }
        return brewingItems.contains(item.getType().toString());
    }

    private boolean hasBlazePowder(BrewingStand brewingStand) {
        ItemStack blazePowderSlot = brewingStand.getInventory().getItem(3);
        if (brewingStand.getFuelLevel() > 0) {
            return true;
        }
        if (blazePowderSlot == null) {
            return false;
        }
        return blazePowderSlot.getType().equals(Material.BLAZE_POWDER);
    }

    @SuppressWarnings("SameReturnValue")
    private Player getBrewingStandOwner(Block block) {
        if (block.hasMetadata("jobsBrewingOwner")) {
            BlockOwnerShip blockOwner = getJobs().getBlockOwnerShip(BlockTypes.BREWING_STAND).orElse(null);
            assert blockOwner != null;
            List<MetadataValue> data = blockOwner.getBlockMetadatas(block);
            if (data.isEmpty()) {
                return null;
            }
            MetadataValue value = data.getFirst();
            String uuid = value.asString();
            Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        }
        return null;
    }

    private boolean ownsBrewingStand(Player player, Block block) {
        if (block.hasMetadata("jobsBrewingOwner")) {
            BlockOwnerShip blockOwner = getJobs().getBlockOwnerShip(BlockTypes.BREWING_STAND).orElse(null);
            assert blockOwner != null;
            List<MetadataValue> data = blockOwner.getBlockMetadatas(block);
            if (data.isEmpty()) {
                return false;
            }
            MetadataValue value = data.getFirst();
            String uuid = value.asString();
            return uuid.equals(player.getUniqueId().toString());
        }
        return false;
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {

        var player = event.getPlayer();
        if (!isBrewer(player)) {
            return;
        }

        ItemStack item = event.getItem();
        if (!item.getType().equals(Material.POTION)) {
            return;
        }

        PotionMeta potionMeta = (PotionMeta) item.getItemMeta();

        if (isExposureTherapyPotion(potionMeta)) {

            Skill exposureTherapy = new Skill(player, SkillType.EXPOSURE_THERAPY);
            if (exposureTherapy.isSuccessful()) {
                Messenger.sendMessage(player, messageProvider.get("messages.exposure-therapy-success"), null,
                        messageProvider.get("messages.prefix"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 2f, 1f);
                if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                    player.getInventory().remove(item);
                    player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
                }
                event.setCancelled(true);
            }
            return;
        }

        if (isAssistedHealingPotion(potionMeta)) {

            Skill assistedHealing = new Skill(player, SkillType.ASSISTED_HEALING);
            var assistedHealingLevel = assistedHealing.getLevel();
            if (assistedHealingLevel == 0)
                return;

            event.setCancelled(true);
            increaseHealingEffect(player, assistedHealingLevel, potionMeta);
            if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                player.getInventory().remove(item);
                player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
            }
            return;
        }

    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        PotionMeta potionMeta = event.getPotion().getPotionMeta();
        if (isExposureTherapyPotion(potionMeta)) {
            for (Entity entity : event.getAffectedEntities()) {
                if (!(entity instanceof Player))
                    continue;

                var target = (Player) entity;
                if (!isBrewer(target))
                    continue;

                Skill exposureTherapy = new Skill(target, SkillType.EXPOSURE_THERAPY);
                if (exposureTherapy.isSuccessful()) {
                    Messenger.sendMessage(target, messageProvider.get("messages.exposure-therapy-success"), null,
                            messageProvider.get("messages.prefix"));
                    target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 2f, 1f);
                    event.setIntensity(target, 0);
                }
            }
            return;
        }

        if (isAssistedHealingPotion(potionMeta)) {
            for (Entity entity : event.getAffectedEntities()) {
                if (!(entity instanceof Player))
                    continue;

                var target = (Player) entity;
                if (!isBrewer(target))
                    continue;

                Skill assistedHealing = new Skill(target, SkillType.ASSISTED_HEALING);
                var assistedHealingLevel = assistedHealing.getLevel();
                if (assistedHealingLevel == 0)
                    continue;

                event.setIntensity(target, 0);
                increaseHealingEffect(target, assistedHealingLevel, potionMeta);
                return;
            }
        }
    }

    private void increaseHealingEffect(Player target, int skillLevel, PotionMeta potionMeta) {
        double health = target.getHealth();
        int healthModifier = skillLevel + 1;
        var basePotionType = potionMeta.getBasePotionType().toString();
        if (basePotionType.startsWith("LONG_") || basePotionType.startsWith("STRONG_")) {
            target.setHealth(Math.min(20, health + ((healthModifier + 2) * skillLevel * 2)));
        } else {
            target.setHealth(Math.min(20, health + healthModifier * skillLevel * 2));
        }
        Messenger.sendMessage(target, messageProvider.get("messages.assisted-healing-success"), null,
                messageProvider.get("messages.prefix"));
    }

    private boolean isExposureTherapyPotion(PotionMeta potionMeta) {
        @NotNull
        List<String> exposureTherapyPotions = plugin.getConfig().getStringList("exposure-therapy");
        String potionBaseType = potionMeta.getBasePotionType().toString();
        for (String potion : exposureTherapyPotions) {
            if (potionBaseType.contains(potion))
                return true;
        }
        return false;
    }

    private boolean isAssistedHealingPotion(PotionMeta potionMeta) {
        @NotNull
        List<String> assistedHealingPotions = plugin.getConfig().getStringList("assisted-healing");
        String potionBaseType = potionMeta.getBasePotionType().toString();
        for (String potion : assistedHealingPotions) {
            if (potionBaseType.contains(potion))
                return true;
        }
        return false;
    }

    private boolean isBrewer(Player player) {
        var isBrewer = Utils.isInJob(player, "Brewer");
        return isBrewer;
    }

}
