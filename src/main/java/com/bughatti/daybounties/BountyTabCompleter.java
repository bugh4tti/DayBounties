package com.bughatti.daybounties;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BountyTabCompleter implements TabCompleter {

    private final List<String> subcomandos = List.of("set", "claim", "list", "cancel", "gui", "help");
    private final List<String> tiposPago = List.of("items");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> resultado = new ArrayList<>();

        if (args.length == 1) {
            String actual = args[0].toLowerCase();
            resultado = subcomandos.stream()
                    .filter(s -> s.startsWith(actual))
                    .collect(Collectors.toList());

        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set") || sub.equals("claim") || sub.equals("list") ||
                sub.equals("cancel") || sub.equals("gui") || sub.equals("menu")) {

                String actual = args[1].toLowerCase();
                resultado = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(nombre -> nombre.toLowerCase().startsWith(actual))
                        .collect(Collectors.toList());
            }

        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set")) {
                String actual = args[2].toLowerCase();
                resultado = tiposPago.stream()
                        .filter(s -> s.startsWith(actual))
                        .collect(Collectors.toList());
            }
        }

        return resultado;
    }
                                      }
