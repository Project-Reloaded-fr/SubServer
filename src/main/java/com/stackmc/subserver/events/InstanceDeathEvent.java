package com.stackmc.subserver.events;

import com.stackmc.subserver.instance.Instance;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class InstanceDeathEvent extends InstanceEvent {
    private static final HandlerList handlers = new HandlerList();
    @Getter private final Player player;
    @Getter private final DamageSource damageSource;
    @Getter private final Component deathMessage;

    @Getter private boolean isCancelled;

    public InstanceDeathEvent(Instance instance, Player player, DamageSource damageSource, Component deathMessage) {
        super(instance);
        this.player = player;
        this.damageSource = damageSource;
        this.deathMessage = deathMessage;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }
}
