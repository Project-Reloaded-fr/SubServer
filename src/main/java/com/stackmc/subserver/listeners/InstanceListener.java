package com.stackmc.subserver.listeners;

import com.stackmc.subserver.SubServer;
import com.stackmc.subserver.instance.Instance;
import com.stackmc.subserver.instance.InstanceType;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InstanceListener implements Listener {

    private final SubServer plugin;

    public InstanceListener(SubServer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getOnlinePlayers().forEach(target -> {
            event.getPlayer().hidePlayer(plugin, target);
            target.hidePlayer(plugin, event.getPlayer());
        });
        event.setJoinMessage(null);

        List<InstanceType> autospawnTypes = this.plugin.getInstanceFactory().getInstanceTypes().stream()
                .filter(InstanceType::isAutoJoin)
                .collect(Collectors.toList());

        if (autospawnTypes.isEmpty()) {
            Bukkit.getLogger().severe("No instance type is set to auto-join.");
            return;
        }

        if (autospawnTypes.size() > 1) {
            Bukkit.getLogger().severe("More than one instance type is set to auto-join.");
            return;
        }

        InstanceType type = autospawnTypes.get(0);
        Instance instance = this.plugin.getInstanceFactory().getInstances(type).stream()
                .filter(inst -> inst.getPlayers().size() < type.getMaxPlayers())
                .findAny()
                .orElse(null);

        if (instance == null) {
            Bukkit.getLogger().severe("No instance are open for auto-join.");
            event.getPlayer().kickPlayer("Server is full, sorry.");
            return;
        }

        instance.dispatchEvent(event);
        instance.sendMessage(event.joinMessage());
        instance.joinInstance(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(null);

        Instance instance = Instance.getInstance(event.getPlayer().getWorld());
        if (instance == null) return;

        instance.dispatchEvent(event);
        event.quitMessage(event.quitMessage());
        instance.quitInstance(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.deathMessage(null);

        Player player = event.getEntity().getPlayer();
        if (player == null) return;
        Instance instance = Instance.getInstance(player.getWorld());
        if (instance == null) return;

        instance.dispatchEvent(event);
        instance.sendMessage(event.deathMessage());
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Instance instance = Instance.getInstance(event.getPlayer().getWorld());
        if (instance == null) {
            Bukkit.getLogger().severe("UN JOUEUR TENTE DE PARLER DANS UN MONDE HORS INSTANCE:\n" + event.message());
            return;
        }

        Set<Audience> audience = event.viewers();
        audience.clear();
        audience.addAll(instance.getPlayers());
        audience.add(Bukkit.getConsoleSender());

        instance.dispatchEvent(event);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Instance instance = Instance.getInstance(event.getPlayer().getWorld());
        if (instance != null) {
            event.setRespawnLocation(instance.getWorlds().get(0).getWorld().getSpawnLocation());
        }
    }
}
