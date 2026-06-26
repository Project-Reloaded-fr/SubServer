package com.stackmc.subserver.events;

import com.stackmc.subserver.instance.Instance;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Set;


public class InstanceRespawnEvent extends InstanceEvent {
    private static final HandlerList handlers = new HandlerList();
    @Getter private final Player player;
    @Getter private final Set<PlayerRespawnEvent.RespawnFlag> respawnFlags;
    @Getter private final Location respawnLocation;
    @Getter private final PlayerRespawnEvent.RespawnReason respawnReason;

    @Getter private final boolean isAnchorSpawn;
    @Getter private final boolean isBedSpawn;
    @Getter private final boolean isMissingRespawnBlock;

    @Getter private boolean isCancelled;

    public InstanceRespawnEvent(Instance instance, Player player,
                                Set<PlayerRespawnEvent.RespawnFlag> respawnFlags, Location respawnLocation, PlayerRespawnEvent.RespawnReason respawnReason,
                                boolean isAnchorSpawn, boolean isBedSpawn, boolean isMissingRespawnBlock) {
        super(instance);
        this.player = player;
        this.respawnFlags = respawnFlags;
        this.respawnLocation = respawnLocation;
        this.respawnReason = respawnReason;
        this.isAnchorSpawn = isAnchorSpawn;
        this.isBedSpawn = isBedSpawn;
        this.isMissingRespawnBlock = isMissingRespawnBlock;
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
