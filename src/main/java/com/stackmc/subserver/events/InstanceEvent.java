package com.stackmc.subserver.events;

import com.stackmc.subserver.instance.Instance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

@RequiredArgsConstructor
public abstract class InstanceEvent extends Event implements Cancellable {
    @Getter private final Instance instance;
}