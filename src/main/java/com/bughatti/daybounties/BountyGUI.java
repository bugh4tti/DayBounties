package com.bughatti.daybounties;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyGUI implements Listener {

    private final Main plugin;

    // Jugadores que están esperando escribir un monto por chat
    private final Map<UUID, UUID> esperandoMontoVault = new HashMap<>();

    public BountyGUI(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ==========================
    // Holders para identificar menús
    // ==========================
    private class MenuPrincipalHolder implements InventoryHolder {
        final OfflinePlayer objetivo;
        MenuPrincipalHolder(OfflinePlayer objetivo) { this.objetivo = objetivo; }
        public Inventory getInventory() { return null; }
    }

    private class MenuColocarHolder implements InventoryHolder {
        final OfflinePlayer objetivo;
        MenuColocarHolder(OfflinePlayer objetivo) { this.objetivo = objetivo; }
        public Inventory getInventory() { return null; }
    }

    private class MenuItemsHolder implements InventoryHolder {
        final OfflinePlayer objetivo;
        MenuItemsHolder(OfflinePlayer objetivo) { this.objetivo = objetivo; }
        public Inventory getInventory() { return null; }
    }

    // ==========================
    // Menú principal (info + botón colocar)
    // ==========================
    public void abrirMenuPrincipal(Player jugador, OfflinePlayer objetivo) {
        String titulo = obtenerTexto("gui.titulo-principal", objetivo.getName());
        Inventory inv = Bukkit.createInventory(new MenuPrincipalHolder(objetivo), 27, titulo);

        List<Bounty> activas = plugin.getBountyManager().getBounties(objetivo.getUniqueId());
        int max = plugin.getBountyManager().getMaxBounties();

        ItemStack info = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(colorear(obtenerTexto("gui.item-info-nombre", objetivo.getName())));

        double totalVault = 0;
        for (Bounty b : activas) {
            if (b.getTipoPago() == Bounty.TipoPago.VAULT) totalVault += b.getMontoVault();
        }

        List<String> lore = new ArrayList<>();
        for (String linea : plugin.getConfig().getStringList("gui.item-info-lore")) {
            linea = linea.replace("<monto>", String.valueOf(totalVault))
                         .replace("<cantidad>", String.valueOf(activas.size()))
                         .replace("<max>", String.valueOf(max));
            lore.add(colorear(linea));
        }
        infoMeta.setLore(lore);
        info.setItemMeta(infoMeta);
        inv.setItem(13, info);

        ItemStack colocar = new ItemStack(Material.EMERALD);
        ItemMeta colocarMeta = colocar.getItemMeta();
        colocarMeta.setDisplayName(colorear("&aColocar recompensa"));
        colocar.setItemMeta(colocarMeta);
        inv.setItem(22, colocar);

        jugador.openInventory(inv);
    }

    // ==========================
    // Menú colocar (elegir Vault o Items)
    // ==========================
    private void abrirMenuColocar(Player jugador, OfflinePlayer objetivo) {
        String titulo = obtenerTexto("gui.titulo-colocar", objetivo.getName());
        Inventory inv = Bukkit.createInventory(new MenuColocarHolder(objetivo), 27, titulo);

        ItemStack vault = new ItemStack(Material.GOLD_INGOT);
        ItemMeta vaultMeta = vault.getItemMeta();
        vaultMeta.setDisplayName(colorear(plugin.getConfig().getString("gui.boton-vault-nombre", "&aPagar con Vault")));
        vault.setItemMeta(vaultMeta);
        inv.setItem(11, vault);

        ItemStack items = new ItemStack(Material.CHEST);
        ItemMeta itemsMeta = items.getItemMeta();
        itemsMeta.setDisplayName(colorear(plugin.getConfig().getString("gui.boton-items-nombre", "&6Pagar con ítems")));
        items.setItemMeta(itemsMeta);
        inv.setItem(15, items);

        jugador.openInventory(inv);
    }

    // ==========================
    // Menú para depositar ítems + confirmar
    // ==========================
    private void abrirMenuItems(Player jugador, OfflinePlayer objetivo) {
        String titulo = obtenerTexto("gui.titulo-colocar", objetivo.getName());
        Inventory inv = Bukkit.createInventory(new MenuItemsHolder(objetivo), 27, titulo);

        ItemStack confirmar = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confMeta = confirmar.getItemMeta();
        confMeta.setDisplayName(colorear(plugin.getConfig().getString("gui.boton-confirmar-nombre", "&a✔ Confirmar")));
        confirmar.setItemMeta(confMeta);
        inv.setItem(25, confirmar);

        ItemStack cancelar = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancMeta = cancelar.getItemMeta();
        cancMeta.setDisplayName(colorear(plugin.getConfig().getString("gui.boton-cancelar-nombre", "&c✘ Cancelar")));
        cancelar.setItemMeta(cancMeta);
        inv.setItem(19, cancelar);

        jugador.openInventory(inv);
    }

    // ==========================
    // Eventos de click
    // ==========================
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        Player jugador = (Player) event.getWhoClicked();

        if (holder instanceof MenuPrincipalHolder) {
            event.setCancelled(true);
            if (event.getSlot() == 22) {
                OfflinePlayer objetivo = ((MenuPrincipalHolder) holder).objetivo;
                jugador.closeInventory();
                abrirMenuColocar(jugador, objetivo);
            }
        } else if (holder instanceof MenuColocarHolder) {
            event.setCancelled(true);
            OfflinePlayer objetivo = ((MenuColocarHolder) holder).objetivo;

            if (event.getSlot() == 11) {
                jugador.closeInventory();
                esperandoMontoVault.put(jugador.getUniqueId(), objetivo.getUniqueId());
                jugador.sendMessage(colorear("&6Escribí en el chat el monto que querés colocar como recompensa."));
            } else if (event.getSlot() == 15) {
                jugador.closeInventory();
                abrirMenuItems(jugador, objetivo);
            }
        } else if (holder instanceof MenuItemsHolder) {
            OfflinePlayer objetivo = ((MenuItemsHolder) holder).objetivo;

            if (event.getSlot() == 25) {
                event.setCancelled(true);
                confirmarPagoItems(jugador, objetivo, event.getInventory());
            } else if (event.getSlot() == 19) {
                event.setCancelled(true);
                devolverItemsDelMenu(jugador, event.getInventory());
                jugador.closeInventory();
            } else if (event.getSlot() >= 0 && event.getSlot() < 19 && event.getSlot() != 11 && event.getSlot() != 15) {
                // Permitir depositar ítems libremente en slots vacíos (0-18, excluyendo decorativos)
                if (event.getSlot() == 19 || event.getSlot() == 25) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // ==========================
    // Confirmar bounty pagada con ítems
    // ==========================
    private void confirmarPagoItems(Player jugador, OfflinePlayer objetivo, Inventory inv) {
        List<ItemStack> itemsDepositados = new ArrayList<>();

        for (int slot = 0; slot < 18; slot++) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                itemsDepositados.add(item.clone());
            }
        }

        if (itemsDepositados.isEmpty()) {
            jugador.sendMessage(colorear("&cNo depositaste ningún ítem."));
            return;
        }

        if (!plugin.getBountyManager().tieneEspacio(objetivo.getUniqueId())) {
            devolverItemsDelMenu(jugador, inv);
            jugador.sendMessage(colorear("&c" + objetivo.getName() + " ya tiene el máximo de recompensas activas."));
            jugador.closeInventory();
            return;
        }

        Bounty bounty = new Bounty(
                jugador.getUniqueId(), jugador.getName(),
                objetivo.getUniqueId(), objetivo.getName(),
                Bounty.TipoPago.ITEMS, 0, itemsDepositados
        );

        plugin.getBountyManager().agregarBounty(bounty);

        jugador.sendMessage(colorear("&aColocaste una recompensa con ítems sobre &e" + objetivo.getName() + "&a."));
        Bukkit.broadcastMessage(colorear("&6" + jugador.getName() + " &fcolocó una recompensa con ítems sobre &6" + objetivo.getName() + "&f."));

        jugador.closeInventory();
    }

    private void devolverItemsDelMenu(Player jugador, Inventory inv) {
        for (int slot = 0; slot < 18; slot++) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                jugador.getInventory().addItem(item);
                inv.setItem(slot, null);
            }
        }
    }

    // ==========================
    // Chat: monto de la bounty en Vault
    // ==========================
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player jugador = event.getPlayer();
        UUID objetivoId = esperandoMontoVault.get(jugador.getUniqueId());

        if (objetivoId == null) return;

        event.setCancelled(true);
        esperandoMontoVault.remove(jugador.getUniqueId());

        String mensaje = event.getMessage();
        double monto;

        try {
            monto = Double.parseDouble(mensaje);
        } catch (NumberFormatException e) {
            jugador.sendMessage(colorear("&cMonto inválido. Se canceló la operación."));
            return;
        }

        if (monto <= 0) {
            jugador.sendMessage(colorear("&cEl monto debe ser mayor a 0."));
            return;
        }

        if (plugin.getEconomy() == null || !plugin.getEconomy().has(jugador, monto)) {
            jugador.sendMessage(colorear("&cNo tenés suficiente dinero."));
            return;
        }

        OfflinePlayer objetivo = Bukkit.getOfflinePlayer(objetivoId);

        if (!plugin.getBountyManager().tieneEspacio(objetivoId)) {
            jugador.sendMessage(colorear("&c" + objetivo.getName() + " ya tiene el máximo de recompensas activas."));
            return;
        }

        plugin.getEconomy().withdrawPlayer(jugador, monto);

        Bounty bounty = new Bounty(
                jugador.getUniqueId(), jugador.getName(),
                objetivoId, objetivo.getName(),
                Bounty.TipoPago.VAULT, monto, new ArrayList<>()
        );

        plugin.getBountyManager().agregarBounty(bounty);

        Bukkit.getScheduler().runTask(plugin, () -> {
            jugador.sendMessage(colorear("&aColocaste una recompensa de &e" + monto + "&a sobre &e" + objetivo.getName() + "&a."));
            Bukkit.broadcastMessage(colorear("&6" + jugador.getName() + " &fcolocó una recompensa de &e" + monto + "&f sobre &6" + objetivo.getName() + "&f."));
        });
    }

    // ==========================
    // Utilidades
    // ==========================
    private String obtenerTexto(String ruta, String recompensado) {
        String texto = plugin.getConfig().getString(ruta, "");
        texto = texto.replace("<recompensado>", recompensado);
        return colorear(texto);
    }

    private String colorear(String texto) {
        return ChatColor.translateAlternateColorCodes('&', texto);
    }
  }
