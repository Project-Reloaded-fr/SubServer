package com.stackmc.subserver.instance;

import com.stackmc.subserver.SubServer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class InstanceFactory {

    private final SubServer plugin;

    @Getter private final Set<InstanceType> instanceTypes = new HashSet<>();
    private final Map<InstanceType, Set<Instance>> instances = new HashMap<>();
    @Getter public Instance autoJoinInstance = null;

    private BukkitTask task;

    public Set<Instance> getInstances(InstanceType type) {
        return instances.getOrDefault(type, new HashSet<>());
    }

    public void registerType(InstanceType type) {
        instanceTypes.add(type);
    }

    public void unregisterType(InstanceType type) {
        instanceTypes.remove(type);
    }

    public void startLoop() {
        if (task != null) {
            return;
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::generateInstances, 20, 20);
    }

    public void stopLoop() {
        if (task == null) {
            return;
        }

        task.cancel();
        task = null;
    }

    public void generateInstances() {
        for (InstanceType type : instanceTypes) {
            Set<Instance> instances = this.instances.getOrDefault(type, new HashSet<>());
            for (int i = 0; i < type.getMaxInstancesCount() - instances.size(); i++) {
                Instance instance = new Instance(type.getName() + "_" + (i * new Random().nextInt(10000) * 5), plugin, type);
                generateWorlds(type, instance);
                instance.register();
                instances.add(instance);
                if (type.isAutoJoin()) autoJoinInstance = instance;
            }
            this.instances.put(type, instances);
        }
    }

    private void generateWorlds(InstanceType type, Instance instance) {
        AtomicInteger i = new AtomicInteger();
        int max = type.getWorlds().size();
        for (InstanceType.InstanciableWorld world : type.getWorlds()) {
            instance.loadWorld(world.getWorldName(), world.isSavable(), (str) -> {
                i.getAndIncrement();
                if (i.get() == max) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        instance.setState(InstanceState.CLOSED);
                        type.getPostInitRunnable().accept(instance);
                    });
                }
            });
        }
    }
}
