package com.stackmc.subserver.events;

import com.stackmc.subserver.instance.Instance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@RequiredArgsConstructor
public class InstanceJoinEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    @Getter private final Player player;
    @Getter private final Instance instance;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
