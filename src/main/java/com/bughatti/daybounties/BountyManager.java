package com.bughatti.daybounties;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyManager {

    private final Main plugin;
    private final Map<UUID, List<Bounty>> bounties = new HashMap<>();
    private final int maxBounties;

    public BountyManager(Main plugin) {
        this.plugin = plugin;
        this.maxBounties = plugin.getConfig().getInt("settings.max-bounties-per-player", 5);
    }

    public List<Bounty> getBounties(UUID recompensado) {
        return bounties.getOrDefault(recompensado, new ArrayList<>());
    }

    public boolean tieneEspacio(UUID recompensado) {
        return getBounties(recompensado).size() < maxBounties;
    }

    public int getCantidadBounties(UUID recompensado) {
        return getBounties(recompensado).size();
    }

    public int getMaxBounties() {
        return maxBounties;
    }

    public void agregarBounty(Bounty bounty) {
        bounties.computeIfAbsent(bounty.getRecompensado(), k -> new ArrayList<>()).add(bounty);
    }

    public boolean cancelarBounty(UUID recompensado, UUID recompensante) {
        List<Bounty> lista = bounties.get(recompensado);
        if (lista == null) return false;

        Bounty encontrada = null;
        for (Bounty b : lista) {
            if (b.getRecompensante().equals(recompensante)) {
                encontrada = b;
                break;
            }
        }

        if (encontrada == null) return false;

        lista.remove(encontrada);
        devolverPago(encontrada, recompensante);
        return true;
    }

    public double reclamarTodas(UUID recompensado, Player reclamador) {
        List<Bounty> lista = bounties.remove(recompensado);
        if (lista == null || lista.isEmpty()) return -1;

        double totalVault = 0;

        for (Bounty b : lista) {
            if (b.getTipoPago() == Bounty.TipoPago.VAULT) {
                totalVault += b.getMontoVault();
            } else {
                for (ItemStack item : b.getItems()) {
                    reclamador.getInventory().addItem(item);
                }
            }
        }

        if (totalVault > 0 && plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(reclamador, totalVault);
        }

        return totalVault;
    }

    private void devolverPago(Bounty bounty, UUID recompensante) {
        Player jugador = plugin.getServer().getPlayer(recompensante);

        if (bounty.getTipoPago() == Bounty.TipoPago.VAULT) {
            if (plugin.getEconomy() != null) {
                plugin.getEconomy().depositPlayer(
                        plugin.getServer().getOfflinePlayer(recompensante),
                        bounty.getMontoVault()
                );
            }
        } else {
            if (jugador != null) {
                for (ItemStack item : bounty.getItems()) {
                    jugador.getInventory().addItem(item);
                }
            }
        }
    }

    public void limpiarTodo() {
        bounties.clear();
    }
  }
