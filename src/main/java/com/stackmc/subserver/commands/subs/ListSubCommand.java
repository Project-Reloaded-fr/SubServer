package com.stackmc.subserver.commands.subs;

import com.stackmc.subserver.SubServer;
import com.stackmc.subserver.instance.Instance;
import lombok.RequiredArgsConstructor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ListSubCommand implements TabExecutor {

    private final SubServer plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command rootCommand, String label, String[] args) {
        String message = Instance.getInstances().stream().map(instance ->  {
            return instance.getName() + " (" + instance.getWorlds().stream().map(Instance.InstanciableWorld::getWorld).map(World::toString).collect(Collectors.joining(", ")) + ")";
        }).collect(Collectors.joining("\n"));
        sender.sendMessage("Voici la liste des instances :\n" + message);
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command rootCommand, String label, String[] args) {
        return Collections.emptyList();
    }

}
