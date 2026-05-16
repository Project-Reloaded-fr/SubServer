package com.stackmc.subserver.instance;

import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.exceptions.WorldLoadedException;
import com.stackmc.subserver.SubServer;
import com.stackmc.subserver.events.InstanceJoinEvent;
import com.stackmc.subserver.events.InstanceQuitEvent;
import com.stackmc.subserver.worldgen.SWMUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
public class Instance {

    @Getter private static final Set<Instance> instances = new HashSet<>();

    public static Instance getInstance(World world) {
        return instances.stream()
                .filter(instance -> instance.getWorlds()
                        .stream()
                        .anyMatch(instanciableWorld -> instanciableWorld.getWorld().equals(world)))
                .findAny()
                .orElse(null);
    }

    public static Instance getInstance(String name) {
        return instances.stream().filter(instance -> instance.getName().contains(name)).findAny().orElse(null);
    }

    private final String name;
    private final SubServer plugin;
    private final InstanceType type;
    private final List<InstanciableWorld> worlds = new ArrayList<>();
    private final Set<OfflinePlayer> offlinePlayers = new HashSet<>();
    private final UUID uniqueId = UUID.randomUUID();
    @Setter private InstanceState state = InstanceState.INIT;

    @Getter
    @RequiredArgsConstructor
    public static class InstanciableWorld {
        private final World world;
        private final boolean savable;
    }

    public InstanciableWorld getInstanciableWorld(String worldName) {
        return worlds.stream()
                .filter(instanciableWorld -> instanciableWorld.getWorld().getName().equals(worldName))
                .findAny()
                .orElse(null);
    }

    private final EventDispatcher eventDispatcher = new EventDispatcher();

    public void register() {
        instances.add(this);
    }

    public void registerListener(Listener listener) {
        eventDispatcher.registerListener(listener);
    }

    public void unregisterListener(Listener listener) {
        eventDispatcher.unregisterListener(listener);
    }

    public void dispatchEvent(Event event) {
        eventDispatcher.dispatchEvent(event);
    }

    public void close() {
        offlinePlayers.forEach(offlinePlayer -> {
            Player player = offlinePlayer.getPlayer();
            if (player != null) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
        });

        worlds.forEach(world -> {
            Bukkit.unloadWorld(world.getWorld(), false);
            if (!world.isSavable()) {
                SWMUtils.deleteWorld(world.getWorld().getName());
            }
        });

        worlds.clear();
        offlinePlayers.clear();
        instances.remove(this);
    }

    public void loadWorld(String worldName, boolean isSavable, @Nullable Consumer<String> callback) {
        final Consumer<String> finalCallback = (callback == null ? (s -> {}) : callback);

        String destWorldName;
        if (isSavable) {
            destWorldName = worldName;
        } else {
            destWorldName = getUniqueId().toString() + "_" + worldName;
        }

        File src = new File(SWMUtils.getWorldSlimeFolder() + File.separator + worldName + ".slime");
        File dest = new File( SWMUtils.getWorldSlimeFolder() + File.separator + destWorldName + ".slime");

        long startTime = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (!isSavable) {
                    Files.copy(src.toPath(), dest.toPath());
                }
            } catch (IOException e) {
                finalCallback.accept("§cLe monde spécifié (" + worldName + ") n'existe pas.");
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    SWMUtils.loadWorld(destWorldName);
                } catch (UnknownWorldException | IOException | CorruptedWorldException | NewerFormatException |
                         WorldLoadedException e) {
                    finalCallback.accept("§cUne erreur est survenue lors du chargement du monde.");
                }
            });

            World world;
            do {
                world = Bukkit.getWorld(destWorldName);
            } while (world == null); // I can do this because i'm in an async thread

            this.addWorld(world, isSavable);

            long totalTime = System.currentTimeMillis() - startTime;
            finalCallback.accept("Monde " + destWorldName +  " chargé en " + totalTime + "ms ou " + ((float) totalTime / 50f) + " ticks .");
        });
    }

    public void addWorld(World world, boolean isSavable) {
        worlds.add(new InstanciableWorld(world,isSavable));
    }

    public void joinInstance(Player player) {
        Instance oldInstance = Instance.getInstance(player.getWorld());
        if(oldInstance != null) oldInstance.quitInstance(player);
        getPlayers().forEach(target -> {
            player.showPlayer(plugin, target);
            target.showPlayer(plugin, player);
        });
        offlinePlayers.add(player);
        player.teleport(worlds.get(0).getWorld().getSpawnLocation());

        //PlayerJoinEvent event = new PlayerJoinEvent(player," ");
        //this.dispatchEvent(event);
        Bukkit.getPluginManager().callEvent(new InstanceJoinEvent(this, player));
    }

    public void joinInstance(Player player, World world) {
        Instance oldInstance = Instance.getInstance(player.getWorld());
        if(oldInstance != null) oldInstance.quitInstance(player);
        getPlayers().forEach(target -> {
            player.showPlayer(plugin, target);
            target.showPlayer(plugin, player);
        });
        offlinePlayers.add(player);
        player.teleport(getInstanciableWorld(world.getName()).getWorld().getSpawnLocation());

        Bukkit.getPluginManager().callEvent(new InstanceJoinEvent(this, player));
    }

    public void quitInstance(Player player) {
        getPlayers().forEach(target -> {
            player.hidePlayer(plugin, target);
            target.hidePlayer(plugin, player);
        });
        offlinePlayers.remove(player);

        //PlayerQuitEvent event = new PlayerQuitEvent(player," ");
        //this.dispatchEvent(event);
        Bukkit.getPluginManager().callEvent(new InstanceQuitEvent(this, player));
    }

    public void sendMessage(String message) {
        if (message == null) {
            return;
        }

        getPlayers().forEach(receiver -> receiver.sendMessage(message));
    }

    public List<Player> getPlayers() {
        return offlinePlayers.stream().filter(OfflinePlayer::isOnline).map(OfflinePlayer::getPlayer).collect(Collectors.toList());
    }
}