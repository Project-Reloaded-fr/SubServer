package com.stackmc.subserver.instance;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

public class EventDispatcher {

    private final Set<Dispatchable> listeners = new HashSet<>();

    public void dispatchEvent(Event event) {
        listeners.stream()
                .filter(dispatchable -> dispatchable.mappings.containsKey(event.getClass()))
                .forEach(dispatchable -> dispatchable.mappings.get(event.getClass()).forEach(consumer -> consumer.accept(event)));
    }

    public void registerListener(Listener listener) {
        listeners.add(new Dispatchable(listener));
    }

    public void unregisterListener(Listener listener) {
        listeners.removeIf(dispatchable -> dispatchable.listener.equals(listener));
    }

    private static class Dispatchable {
        private final Listener listener;
        private final Map<Class<? extends Event>, List<Consumer<Event>>> mappings = new HashMap<>();

        public Dispatchable(Listener listener) {
            this.listener = listener;

            this.createMappings();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Dispatchable)) return false;
            return listener.equals(((Dispatchable) o).listener);
        }

        @Override
        public int hashCode() {
            return listener.hashCode();
        }

        private void createMappings() {
            for (Method method : listener.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(EventHandler.class)) {
                    Class<?>[] parameters = method.getParameterTypes();

                    if (parameters.length != 1) {
                        throw new IllegalArgumentException("Method " + method.getName() + " in " + listener.getClass().getName() + " has @EventHandler annotation but does not have exactly one parameter");
                    }
                    if (!Event.class.isAssignableFrom(parameters[0])) {
                        throw new IllegalArgumentException("Method " + method.getName() + " in " + listener.getClass().getName() + " has @EventHandler annotation but the parameter is not a subclass of Event");
                    }

                    Class<? extends Event> eventClass = parameters[0].asSubclass(Event.class);
                    this.mappings.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(event -> {
                        try {
                            method.setAccessible(true);
                            method.invoke(this.listener, event);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
    }
}
