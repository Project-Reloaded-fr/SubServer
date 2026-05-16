package com.stackmc.subserver.events;

import com.stackmc.subserver.instance.Instance;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class InstanceJoinEvent extends InstanceEvent {
    private static final HandlerList handlers = new HandlerList();
    @Getter private final Player player;

    public InstanceJoinEvent(Instance instance, Player player) {
        super(instance);
        this.player = player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
