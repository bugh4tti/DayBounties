package com.bughatti.daybounties;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BountyCommand implements CommandExecutor {

    private final Main plugin;

    public BountyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            enviarMensaje(sender, "general.comando-invalido", null, null, null, null);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "set":
                manejarSet(sender, args);
                break;
            case "claim":
                manejarClaim(sender, args);
                break;
            case "list":
                manejarList(sender, args);
                break;
            case "cancel":
                manejarCancel(sender, args);
                break;
            case "gui":
            case "menu":
                manejarGui(sender, args);
                break;
            case "help":
                manejarHelp(sender);
                break;
            default:
                enviarMensaje(sender, "general.comando-invalido", null, null, null, null);
                break;
        }

        return true;
    }

    // ==========================
    // /bounty set <jugador> <monto|items>
    // ==========================
    private void manejarSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            enviarMensaje(sender, "general.solo-jugadores", null, null, null, null);
            return;
        }

        Player recompensante = (Player) sender;

        if (!recompensante.hasPermission("daybounties.set")) {
            enviarMensaje(sender, "general.no-permiso", null, null, null, null);
            return;
        }

        if (args.length < 3) {
            enviarMensaje(sender, "general.comando-invalido", null, null, null, null);
            return;
        }

        String nombreObjetivo = args[1];
        OfflinePlayer objetivo = Bukkit.getOfflinePlayer(nombreObjetivo);

        if (objetivo == null || (!objetivo.hasPlayedBefore() && !objetivo.isOnline())) {
            enviarMensaje(sender, "general.jugador-no-encontrado", null, nombreObjetivo, null, null);
            return;
        }

        if (objetivo.getUniqueId().equals(recompensante.getUniqueId())) {
            enviarMensaje(sender, "set.no-a-uno-mismo", null, null, null, null);
            return;
        }

        if (!plugin.getBountyManager().tieneEspacio(objetivo.getUniqueId())) {
            enviarMensaje(sender, "set.limite-alcanzado", recompensante.getName(), objetivo.getName(), null,
                    String.valueOf(plugin.getBountyManager().getMaxBounties()));
            return;
        }

        String tipoArg = args[2].toLowerCase();

        if (tipoArg.equals("items") || tipoArg.equals("item")) {
            crearBountyConItems(recompensante, objetivo);
        } else {
            crearBountyConVault(recompensante, objetivo, tipoArg);
        }
    }

    private void crearBountyConVault(Player recompensante, OfflinePlayer objetivo, String montoStr) {
        double monto;
        try {
            monto = Double.parseDouble(montoStr);
        } catch (NumberFormatException e) {
            enviarMensaje(recompensante, "set.monto-invalido", null, null, null, null);
            return;
        }

        if (monto <= 0) {
            enviarMensaje(recompensante, "set.monto-invalido", null, null, null, null);
            return;
        }

        if (plugin.getEconomy() == null || !plugin.getEconomy().has(recompensante, monto)) {
            enviarMensaje(recompensante, "set.fondos-insuficientes", null, null, null, null);
            return;
        }

        plugin.getEconomy().withdrawPlayer(recompensante, monto);

        Bounty bounty = new Bounty(
                recompensante.getUniqueId(), recompensante.getName(),
                objetivo.getUniqueId(), objetivo.getName(),
                Bounty.TipoPago.VAULT, monto, new ArrayList<>()
        );

        finalizarCreacion(recompensante, objetivo, bounty, String.valueOf(monto));
    }

    private void crearBountyConItems(Player recompensante, OfflinePlayer objetivo) {
        ItemStack itemMano = recompensante.getInventory().getItemInMainHand();

        if (itemMano == null || itemMano.getType().isAir()) {
            enviarMensaje(recompensante, "set.items-en-mano-invalidos", null, null, null, null);
            return;
        }

        List<ItemStack> items = new ArrayList<>();
        items.add(itemMano.clone());

        recompensante.getInventory().setItemInMainHand(null);

        Bounty bounty = new Bounty(
                recompensante.getUniqueId(), recompensante.getName(),
                objetivo.getUniqueId(), objetivo.getName(),
                Bounty.TipoPago.ITEMS, 0, items
        );

        finalizarCreacion(recompensante, objetivo, bounty, bounty.getMontoDescripcion());
    }

    private void finalizarCreacion(Player recompensante, OfflinePlayer objetivo, Bounty bounty, String montoDesc) {
        plugin.getBountyManager().agregarBounty(bounty);

        enviarMensaje(recompensante, "set.exito", recompensante.getName(), objetivo.getName(), montoDesc, null);

        if (objetivo.isOnline()) {
            enviarMensaje((Player) objetivo, "set.exito-recompensado", recompensante.getName(), objetivo.getName(), montoDesc, null);
        }

        String broadcast = obtenerMensaje("set.broadcast", recompensante.getName(), objetivo.getName(), montoDesc, null);
        Bukkit.broadcastMessage(broadcast);
    }

    // ==========================
    // /bounty claim <jugador>
    // ==========================
    private void manejarClaim(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            enviarMensaje(sender, "general.solo-jugadores", null, null, null, null);
            return;
        }

        Player reclamador = (Player) sender;

        if (!reclamador.hasPermission("daybounties.claim")) {
            enviarMensaje(sender, "general.no-permiso", null, null, null, null);
            return;
        }

        if (args.length < 2) {
            enviarMensaje(sender, "general.comando-invalido", null, null, null, null);
            return;
        }

        String nombreObjetivo = args[1];
        OfflinePlayer objetivo = Bukkit.getOfflinePlayer(nombreObjetivo);

        if (objetivo == null) {
            enviarMensaje(sender, "general.jugador-no-encontrado", null, nombreObjetivo, null, null);
            return;
        }

        List<Bounty> activas = plugin.getBountyManager().getBounties(objetivo.getUniqueId());

        if (activas.isEmpty()) {
            enviarMensaje(reclamador, "claim.sin-bounty", null, objetivo.getName(), null, null);
            return;
        }

        boolean todasPropias = activas.stream().allMatch(b -> b.getRecompensante().equals(reclamador.getUniqueId()));
        if (todasPropias) {
            enviarMensaje(reclamador, "claim.no-puede-reclamar-propia", null, null, null, null);
            return;
        }

        double totalGanado = plugin.getBountyManager().reclamarTodas(objetivo.getUniqueId(), reclamador);

        if (totalGanado < 0) {
            enviarMensaje(reclamador, "claim.sin-bounty", null, objetivo.getName(), null, null);
            return;
        }

        String montoDesc = String.valueOf(totalGanado);

        enviarMensaje(reclamador, "claim.exito", reclamador.getName(), objetivo.getName(), montoDesc, null);

        String broadcast = obtenerMensaje("claim.broadcast", reclamador.getName(), objetivo.getName(), montoDesc, null);
        Bukkit.broadcastMessage(broadcast);
    }

    // ==========================
    // /bounty list <jugador>
    // ==========================
    private void manejarList(CommandSender sender, String[] args) {
        String nombreObjetivo = args.length >= 2 ? args[1] : sender.getName();
        OfflinePlayer objetivo = Bukkit.getOfflinePlayer(nombreObjetivo);

        List<Bounty> activas = plugin.getBountyManager().getBounties(objetivo.getUniqueId());

        if (activas.isEmpty()) {
            enviarMensaje(sender, "list.sin-resultados", null, objetivo.getName(), null, null);
            return;
        }

        enviarMensaje(sender, "list.titulo", null, objetivo.getName(), null, null);

        for (Bounty b : activas) {
            String linea = plugin.getConfig().getString("messages.list.formato-item", "");
            linea = linea.replace("<recompensante>", b.getNombreRecompensante())
                         .replace("<monto>", b.getMontoDescripcion());
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', linea));
        }
    }

    // ==========================
    // /bounty cancel <jugador>
    // ==========================
    private void manejarCancel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            enviarMensaje(sender, "general.solo-jugadores", null, null, null, null);
            return;
        }

        Player recompensante = (Player) sender;

        if (args.length < 2) {
            enviarMensaje(sender, "general.comando-invalido", null, null, null, null);
            return;
        }

        String nombreObjetivo = args[1];
        OfflinePlayer objetivo = Bukkit.getOfflinePlayer(nombreObjetivo);

        boolean exito = plugin.getBountyManager().cancelarBounty(objetivo.getUniqueId(), recompensante.getUniqueId());

        if (exito) {
            enviarMensaje(recompensante, "cancel.exito", null, objetivo.getName(), null, null);
        } else {
            enviarMensaje(recompensante, "cancel.sin-permiso-cancelar", null, null, null, null);
        }
    }

    // ==========================
    // /bounty gui <jugador>
    // ==========================
    private void manejarGui(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            enviarMensaje(sender, "general.solo-jugadores", null, null, null, null);
            return;
        }

        Player jugador = (Player) sender;

        if (args.length >= 2) {
            String nombreObjetivo = args[1];
            OfflinePlayer objetivo = Bukkit.getOfflinePlayer(nombreObjetivo);
            plugin.getBountyGUI().abrirMenuPrincipal(jugador, objetivo);
        } else {
            enviarMensaje(sender, "general.comando-invalido", null, null, null, null);
        }
    }

    // ==========================
    // /bounty help
    // ==========================
    private void manejarHelp(CommandSender sender) {
        sender.sendMessage(colorearHelp("&8&m--------------------------------"));
        sender.sendMessage(colorearHelp("&6&lDayBounties &7- Comandos"));
        sender.sendMessage(colorearHelp("&e/bounty set <jugador> <monto> &7- Coloca recompensa en dinero"));
        sender.sendMessage(colorearHelp("&e/bounty set <jugador> items &7- Coloca recompensa con el ítem en mano"));
        sender.sendMessage(colorearHelp("&e/bounty claim <jugador> &7- Reclama las recompensas activas"));
        sender.sendMessage(colorearHelp("&e/bounty list [jugador] &7- Lista las recompensas activas"));
        sender.sendMessage(colorearHelp("&e/bounty cancel <jugador> &7- Cancela tu recompensa"));
        sender.sendMessage(colorearHelp("&e/bounty gui <jugador> &7- Abre el menú visual"));
        sender.sendMessage(colorearHelp("&7Alias: /db, /bounties, /dbounties, /daybounties"));
        sender.sendMessage(colorearHelp("&8&m--------------------------------"));
    }

    private String colorearHelp(String texto) {
        return ChatColor.translateAlternateColorCodes('&', texto);
    }

    // ==========================
    // Utilidades de mensajes
    // ==========================
    private void enviarMensaje(CommandSender sender, String ruta, String recompensante, String recompensado, String monto, String max) {
        String mensaje = obtenerMensaje(ruta, recompensante, recompensado, monto, max);
        sender.sendMessage(mensaje);
    }

    private String obtenerMensaje(String ruta, String recompensante, String recompensado, String monto, String max) {
        String prefix = plugin.getConfig().getString("settings.prefix", "");
        String texto = plugin.getConfig().getString("messages." + ruta, "");

        texto = texto.replace("<prefix>", prefix);

        if (recompensante != null) texto = texto.replace("<recompensante>", recompensante).replace("<recompensador>", recompensante);
        if (recompensado != null) texto = texto.replace("<recompensado>", recompensado).replace("<jugador>", recompensado);
        if (monto != null) texto = texto.replace("<monto>", monto);
        if (max != null) texto = texto.replace("<max>", max);

        return ChatColor.translateAlternateColorCodes('&', texto);
    }
    }
