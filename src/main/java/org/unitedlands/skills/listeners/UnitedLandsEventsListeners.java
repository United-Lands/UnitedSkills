package org.unitedlands.skills.listeners;

import org.bukkit.Particle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.unitedlands.classes.events.UnitedLandsValueChangeEvent;
import org.unitedlands.skills.Utils;
import org.unitedlands.skills.skill.Skill;
import org.unitedlands.skills.skill.SkillType;
import org.unitedlands.utils.Logger;

import com.destroystokyo.paper.ParticleBuilder;

public class UnitedLandsEventsListeners implements Listener {

    @EventHandler
    public void onCropPlant(UnitedLandsValueChangeEvent event) {

        if (!event.getKey().equals("CROP_PLANT"))
            return;

        var player = event.getPlayer();
        var location = event.getEventLocation();

        if (!Utils.isInJob(player, "Farmer"))
            return;

        Skill fertiliser = new Skill(player, SkillType.FERTILISER);
        if (fertiliser.getLevel() == 0) {
            return;
        }

        if (fertiliser.isSuccessful()) {
            var newAge = Math.min(fertiliser.getLevel() + 1, 4);
            event.setNewValue(newAge);
            new ParticleBuilder(Particle.HAPPY_VILLAGER)
                    .location(location.toCenterLocation())
                    .offset(0.6, 0.6, 0.6)
                    .receivers(64)
                    .count(25)
                    .spawn();
        }
    }

}
