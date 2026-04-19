package com.stackmc.subserver.listeners;

import com.google.common.collect.Sets;
import com.stackmc.subserver.SubServer;
import com.stackmc.subserver.instance.Instance;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.vehicle.VehicleEvent;
import org.bukkit.event.weather.WeatherEvent;
import org.bukkit.event.world.WorldEvent;
import org.bukkit.plugin.Plugin;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Function;

public class EventListener implements Listener {

    private final SubServer plugin;

    public EventListener(SubServer plugin) {
        this.plugin = plugin;
        catchAllEvents();

        // Écoute les plugins qui se chargent après toi
        Bukkit.getPluginManager().registerEvent(
                PluginEnableEvent.class, this, EventPriority.MONITOR,
                (listener, event) -> scanPlugin(((PluginEnableEvent) event).getPlugin()),
                this.plugin
        );
    }

    private static final Set<Class<? extends Event>> skippedEvents = Sets.newHashSet(
            AsyncPlayerChatEvent.class,
            PlayerJoinEvent.class,
            PlayerQuitEvent.class,
            PlayerDeathEvent.class
    );

    private final Set<Class<? extends Event>> registeredEvents = new HashSet<>();

    private void catchAllEvents() {
        // Bukkit events
        Reflections reflections = new Reflections("org.bukkit.event.");
        registerEventClasses(reflections.getSubTypesOf(Event.class));

        // Events des plugins déjà chargés
        for (Plugin loadedPlugin : Bukkit.getPluginManager().getPlugins()) {
            scanPlugin(loadedPlugin);
        }
    }

    private void scanPlugin(Plugin targetPlugin) {
        try {
            ClassLoader classLoader = targetPlugin.getClass().getClassLoader();

            // Les plugin classloaders de Bukkit sont des URLClassLoader
            // ClasspathHelper ne les résout pas toujours correctement
            URL[] urls;
            if (classLoader instanceof URLClassLoader) {
                urls = ((URLClassLoader) classLoader).getURLs();
            } else {
                System.out.println("ClassLoader non supporté pour: " + targetPlugin.getName());
                return;
            }

            ConfigurationBuilder config = new ConfigurationBuilder()
                    .addClassLoaders(classLoader)
                    .addUrls(urls); // On passe directement les URLs du JAR

            Reflections pluginReflections = new Reflections(config);
            registerEventClasses(pluginReflections.getSubTypesOf(Event.class));
            Arrays.stream(urls).forEach(url -> System.out.println("URL trouvée: " + url));
        } catch (Exception e) {
            System.out.println("Impossible de scanner le plugin: " + targetPlugin.getName() + " - " + e.getMessage());
        }
    }

    private void registerEventClasses(Set<Class<? extends Event>> eventClasses) {
        for (Class<? extends Event> eventClass : eventClasses) {
            if (skippedEvents.contains(eventClass)) continue;
            if (registeredEvents.contains(eventClass)) continue; // évite les doubles registrations

            try {
                eventClass.getDeclaredField("handlers");
            } catch (NoSuchFieldException e) {
                System.out.println("Skipped (no handlers): " + eventClass.getName());
                continue;
            } catch (Throwable e) {
                System.out.println("Skipped (unresolved dependency): " + eventClass.getName() + " - " + e.getMessage());
                continue;
            }

            try {
                System.out.println("Registered: " + eventClass.getName());
                registeredEvents.add(eventClass);
                Bukkit.getPluginManager().registerEvent(
                        eventClass, this, EventPriority.MONITOR,
                        (listener, event) -> onEvent(event),
                        this.plugin
                );
            } catch (Throwable e) {
                System.out.println("Failed to register event: " + eventClass.getName() + " - " + e.getMessage());
            }
        }
    }

    public void onEvent(Event event) {
        Instance instance = fetchInstance(event);
        if (instance == null) {
            return;
        }

        instance.dispatchEvent(event);
    }

    private Instance fetchInstance(Event event) {
        World world = fetchWorld(event);
        if (world == null) {
            return null;
        }

        return Instance.getInstance(world);
    }

    private final Map<Class<? extends Event>, Function<Event, World>> worldFetchers = new HashMap<>();

    {
        worldFetchers.put(HangingEvent.class, event -> ((HangingEvent) event).getEntity().getWorld());
        worldFetchers.put(InventoryEvent.class, event -> ((InventoryEvent) event).getView().getPlayer().getWorld());
        worldFetchers.put(EntityEvent.class, event -> ((EntityEvent) event).getEntity().getWorld());
        worldFetchers.put(PlayerEvent.class, event -> ((PlayerEvent) event).getPlayer().getWorld());
        worldFetchers.put(BlockEvent.class, event -> ((BlockEvent) event).getBlock().getWorld());
        worldFetchers.put(VehicleEvent.class, event -> ((VehicleEvent) event).getVehicle().getWorld());
        worldFetchers.put(WeatherEvent.class, event -> ((WeatherEvent) event).getWorld());
        worldFetchers.put(WorldEvent.class, event -> ((WorldEvent) event).getWorld());
    }

    private World fetchWorld(Event event) {
        return worldFetchers.entrySet().stream()
                .filter(entry -> entry.getKey().isInstance(event))
                .findAny()
                .map(entry -> entry.getValue().apply(event))
                .orElse(null);
    }


}
