package dev.donutspawners.listener;

import dev.donutspawners.DonutSpawners;
import dev.donutspawners.gui.SpawnerItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpawnerCommand implements CommandExecutor, TabCompleter {

    private final DonutSpawners plugin;

    // Supported mob types
    private static final List<String> VALID_TYPES = Arrays.asList(
            "SKELETON", "ZOMBIE", "SPIDER", "CAVE_SPIDER", "CREEPER", "BLAZE",
            "ENDERMAN", "WITCH", "SLIME", "MAGMA_CUBE", "PIGLIN", "ZOMBIFIED_PIGLIN",
            "IRON_GOLEM", "COW", "PIG", "CHICKEN", "SHEEP", "SILVERFISH",
            "GUARDIAN", "ELDER_GUARDIAN", "DROWNED", "HUSK", "STRAY",
            "WITHER_SKELETON", "PHANTOM", "PILLAGER", "RAVAGER"
    );

    public SpawnerCommand(DonutSpawners plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /donutspawner give <player> <type> [amount]
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /donutspawner give <player> <type> [stack_size]")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (!args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(Component.text("Unknown subcommand. Usage: /donutspawner give <player> <type> [stack_size]")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /donutspawner give <player> <type> [stack_size]")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (!sender.hasPermission("donutspawners.give")) {
            sender.sendMessage(Component.text("You don't have permission to give spawners!")
                    .color(NamedTextColor.RED));
            return true;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + playerName + "' not found or is offline!")
                    .color(NamedTextColor.RED));
            return true;
        }

        String typeStr = args[2].toUpperCase();
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid mob type: " + typeStr)
                    .color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Valid types: " + String.join(", ", VALID_TYPES))
                    .color(NamedTextColor.GRAY));
            return true;
        }

        int stack = 1;
        if (args.length >= 4) {
            try {
                stack = Integer.parseInt(args[3]);
                if (stack < 1) stack = 1;
                if (stack > 1000) stack = 1000;
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid stack size, using 1.")
                        .color(NamedTextColor.YELLOW));
            }
        }

        ItemStack spawnerItem = SpawnerItem.create(entityType, stack);
        target.getInventory().addItem(spawnerItem);

        target.sendMessage(Component.text("You received a " + stack + "x " + typeStr + " Spawner!")
                .color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Given " + stack + "x " + typeStr + " Spawner to " + target.getName() + "!")
                .color(NamedTextColor.GREEN));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if ("give".startsWith(args[0].toLowerCase())) completions.add("give");
        } else if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) completions.add(p.getName());
            }
        } else if (args.length == 3) {
            String partial = args[2].toUpperCase();
            completions = VALID_TYPES.stream()
                    .filter(t -> t.startsWith(partial))
                    .collect(Collectors.toList());
        } else if (args.length == 4) {
            completions = Arrays.asList("1", "5", "10", "50", "100");
        }

        return completions;
    }
}
