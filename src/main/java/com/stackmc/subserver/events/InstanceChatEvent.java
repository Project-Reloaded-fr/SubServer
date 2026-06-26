package com.stackmc.subserver.events;

import com.stackmc.subserver.instance.Instance;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.Set;


public class InstanceChatEvent extends InstanceEvent {
    private static final HandlerList handlers = new HandlerList();
    @Getter private final Player player;
    @Getter private final Component message;
    @Getter private final Set<Audience> viewers;

    @Getter private boolean isCancelled;

    public InstanceChatEvent(Instance instance, Player player, Component message, Set<Audience> viewers) {
        super(instance);
        this.player = player;
        this.message = message;
        this.viewers = viewers;
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
