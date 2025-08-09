package org.unitedlands.skills.abilities;

import com.destroystokyo.paper.ParticleBuilder;
import dev.lone.itemsadder.api.CustomStack;
import io.lumine.mythic.core.skills.projectiles.Projectile;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.unitedlands.skills.UnitedSkills;
import org.unitedlands.skills.Utils;
import org.unitedlands.skills.skill.ActiveSkill;
import org.unitedlands.skills.skill.Skill;
import org.unitedlands.skills.skill.SkillType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.unitedlands.skills.Utils.canActivate;

public class HunterAbilities implements Listener {
    private final UnitedSkills unitedSkills;
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final HashMap<UUID, Long> durations = new HashMap<>();
    private final Collection<LivingEntity> bleedingEntities = new ArrayList<>();
    private final HashMap<LivingEntity, Long> bleedingDurations = new HashMap<>();

    private Set<UUID> retrieverArrows = new HashSet<>();
    private Set<UUID> piercingArrows = new HashSet<>();

    public HunterAbilities(UnitedSkills unitedSkills) {
        this.unitedSkills = unitedSkills;
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        bleedingEntities.remove(entity);

        if (entity.getKiller() == null) {
            return;
        }
        var player = entity.getKiller();
        if (!isHunter(player)) {
            return;
        }
        if (isSpawnerMob(entity)) {
            return;
        }

        Skill selfReflection = new Skill(player, SkillType.SELF_REFLECTION);
        if (selfReflection.getLevel() != 0) {
            int xp = event.getDroppedExp();
            xp = (int) (xp * (1 + (selfReflection.getLevel() * 0.1)));
            event.setDroppedExp(xp);
        }
        if (entity.getType().equals(EntityType.ENDER_DRAGON)) {
            Skill leatherworking = new Skill(player, SkillType.LEATHERWORKING);
            if (leatherworking.isSuccessful()) {
                CustomStack dragonScale = CustomStack.getInstance("masterwork:dragon_scale");
                player.getInventory().addItem(dragonScale.getItemStack());
                leatherworking.notifyActivation();
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        var player = (Player) event.getEntity();
        if (!isHunter(player)) {
            return;
        }
        Skill counterAttack = new Skill(player, SkillType.COUNTER_ATTACK);
        if (counterAttack.getLevel() == 0) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }
        Entity entity = event.getDamager();
        if (counterAttack.isSuccessful()) {
            event.setCancelled(true);
            double damage = event.getDamage() / 2;
            if (entity instanceof Player) {
                return;
            }
            if (player.getHealth() == 1) {
                return;
            }
            if (entity instanceof LivingEntity) {
                spawnBleedingParticles(entity, 100);
                ((LivingEntity) entity).damage(damage);
                counterAttack.notifyActivation();
            }
        }
    }

    @EventHandler
    public void onPrecisionStrike(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            return;
        }
        var player = (Player) event.getDamager();
        if (!isHunter(player)) {
            return;
        }
        ActiveSkill precisionStrike = new ActiveSkill(player, SkillType.PRECISION_STRIKE, cooldowns, durations);
        if (precisionStrike.getLevel() == 0) {
            return;
        }
        if (!precisionStrike.isActive()) {
            return;
        }
        if (event.getEntity() instanceof LivingEntity entity) {
            if (bleedingEntities.contains(entity)) {
                return;
            }
            bleedingEntities.add(entity);
            int bleedingTimeInSeconds = 15;
            bleedingDurations.put(entity, System.currentTimeMillis() + (bleedingTimeInSeconds * 1000L));
        }
    }

    public void damageBleedingEntities() {
        unitedSkills.getServer().getScheduler().runTaskTimer(unitedSkills, task -> {
            if (bleedingEntities.isEmpty()) {
                return;
            }
            for (LivingEntity entity : bleedingEntities) {
                boolean hasStoppedBleeding = ((bleedingDurations.get(entity) - System.currentTimeMillis())
                        / 1000L) <= 0;
                if (hasStoppedBleeding) {
                    bleedingEntities.remove(entity);
                    bleedingDurations.remove(entity);
                    continue;
                }
                spawnBleedingParticles(entity, 50);
                entity.damage(2);
            }
        }, 0, 20);
    }

    @EventHandler
    public void onPrecisionStrikeActivate(PlayerInteractEvent event) {
        var player = event.getPlayer();
        if (!isHunter(player)) {
            return;
        }
        ActiveSkill precisionStrike = new ActiveSkill(player, SkillType.PRECISION_STRIKE, cooldowns, durations);
        // The skill should either activate with an axe or a sword.
        if (canActivate(event, "_AXE", precisionStrike) || canActivate(event, "SWORD", precisionStrike)) {
            precisionStrike.activate();
        }
    }

    @EventHandler
    public void onFocusActivate(PlayerInteractEvent event) {
        var player = event.getPlayer();
        if (!isHunter(player)) {
            return;
        }
        ActiveSkill focus = new ActiveSkill(player, SkillType.FOCUS, cooldowns, durations);
        if (canActivate(event, "BOW", focus)) {
            focus.activate();
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            return;
        }
        var player = shooter;
        if (!isHunter(player)) {
            return;
        }
        if (!(event.getEntity() instanceof Arrow arrow)) {
            return;
        }
        Skill retriever = new Skill(player, SkillType.RETRIEVER);
        if (retriever.getLevel() != 0) {
            ItemStack bow = player.getInventory().getItemInMainHand();
            if (!bow.containsEnchantment(Enchantment.INFINITY)) {
                if (retriever.isSuccessful()) {
                    retrieverArrows.add(arrow.getUniqueId());
                }
            }
        }
        Skill piercing = new Skill(player, SkillType.PIERCING);
        if (piercing.isSuccessful()) {
            piercingArrows.add(arrow.getUniqueId());
            spawnArrowTrail(arrow, Material.RED_WOOL);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {

        // Only process arrows
        if (!(event.getEntity() instanceof Arrow arrow))
            return;

        Bukkit.getScheduler().runTaskLater(unitedSkills, () -> {
            removeArrowFromCache(event.getEntity().getUniqueId());
        }, 1L);

        // Only process arrows that hit entities
        var hitEntity = event.getHitEntity();
        if (hitEntity == null || !(hitEntity instanceof LivingEntity))
            return;

        // Only process player projectiles
        if (!(event.getEntity().getShooter() instanceof Player shooter))
            return;

        if (!isHunter(shooter))
            return;

        var arrowId = arrow.getUniqueId();

        // Retriever skill

        if (retrieverArrows.contains(arrowId)) {

            Skill retriever = new Skill(shooter, SkillType.RETRIEVER);
            retriever.sendActivationActionBar();

            shooter.getInventory().addItem(arrow.getItemStack().asOne());
            arrow.remove();
        }
        
        // Piercing & critical hit skill

        // Shouldn't work when hitting players
        if (event.getHitEntity() instanceof Player) {
            return;
        }

        // Shouldn't work when hitting blocks
        if (event.getHitBlock() != null) {
            return;
        }

        Skill piercing = new Skill(shooter, SkillType.PIERCING);
        if (piercingArrows.contains(arrowId)) {
            piercing.sendActivationActionBar();
            int modifier = 1;
            if (piercing.getLevel() == 3) {
                modifier = 2;
            }
            arrow.setPierceLevel(arrow.getPierceLevel() + modifier);
        }
        Skill criticalHit = new Skill(shooter, SkillType.CRITICAL_HIT);
        if (criticalHit.isSuccessful()) {
            criticalHit.sendActivationSound();
            spawnBleedingParticles(event.getEntity(), 100);
            arrow.setDamage(arrow.getDamage() * 2);
        }

    }

    private void removeArrowFromCache(UUID id) {
        retrieverArrows.remove(id);
        piercingArrows.remove(id);
    }

    @EventHandler
    public void onMobDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }
        var player = damager;
        if (!isHunter(player)) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            return;
        }
        if (!(entity instanceof LivingEntity victim)) {
            return;
        }
        Skill stun = new Skill(player, SkillType.STUN);
        if (stun.isSuccessful()) {
            stun.notifyActivation();
            int level = stun.getLevel();
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, level * 20, 10));
        }
        Skill criticalHit = new Skill(player, SkillType.CRITICAL_HIT);
        if (criticalHit.isSuccessful()) {
            criticalHit.notifyActivation();
            event.setDamage(event.getDamage() * 2);
        }
        ActiveSkill focus = new ActiveSkill(player, SkillType.FOCUS, cooldowns, durations);
        if (focus.isActive()) {
            if (event.getDamager() instanceof Arrow arrow) {
                if (arrow.hasMetadata("focused")) {
                    event.setDamage(event.getDamage() * (1 + (focus.getLevel() * 0.2)));
                }
            }
        }
    }

    @EventHandler
    public void onFocusedArrowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        var player = (Player) event.getEntity();
        if (!isHunter(player)) {
            return;
        }
        ActiveSkill focus = new ActiveSkill(player, SkillType.FOCUS, cooldowns, durations);
        if (focus.getLevel() == 0) {
            return;
        }
        if (focus.isActive() && event.getForce() == 3F) {
            Arrow arrow = (Arrow) event.getProjectile();
            arrow.setMetadata("focused", new FixedMetadataValue(unitedSkills, true));
            spawnArrowTrail(arrow, Material.YELLOW_WOOL);
        }
    }

    private void spawnArrowTrail(Arrow arrow, Material material) {
        ParticleBuilder particle = new ParticleBuilder(Particle.BLOCK_CRUMBLE);
        BlockData blockData = material.createBlockData();
        Bukkit.getScheduler().runTaskTimer(unitedSkills, task -> {
            if (arrow.isDead()) {
                task.cancel();
            }
            particle.data(blockData)
                    .count(5)
                    .location(arrow.getLocation())
                    .spawn();
        }, 0, 1);
    }

    private void spawnBleedingParticles(Entity entity, int amount) {
        ParticleBuilder particle = new ParticleBuilder(Particle.BLOCK_CRUMBLE);
        BlockData redWoolData = Material.RED_WOOL.createBlockData();
        particle.data(redWoolData);
        particle.count(amount);
        particle.location(entity.getLocation());
        particle.spawn();
    }

    @EventHandler
    public void onEntitySpawn(SpawnerSpawnEvent event) {
        event.getEntity().setMetadata("spawner-mob",
                new FixedMetadataValue(unitedSkills, "spawner-mob"));
    }

    private boolean isSpawnerMob(Entity entity) {
        return entity.hasMetadata("spawner-mob");
    }

    private boolean isHunter(Player player) {
        return Utils.isInJob(player, "Hunter");
    }
}
