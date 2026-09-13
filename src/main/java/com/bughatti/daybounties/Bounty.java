package com.bughatti.daybounties;

import java.util.List;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class Bounty {

    public enum TipoPago {
        VAULT,
        ITEMS
    }

    private final UUID recompensante;
    private final String nombreRecompensante;
    private final UUID recompensado;
    private final String nombreRecompensado;
    private final TipoPago tipoPago;
    private final double montoVault;
    private final List<ItemStack> items;
    private final long fechaCreacion;

    public Bounty(UUID recompensante, String nombreRecompensante,
                  UUID recompensado, String nombreRecompensado,
                  TipoPago tipoPago, double montoVault,
                  List<ItemStack> items) {
        this.recompensante = recompensante;
        this.nombreRecompensante = nombreRecompensante;
        this.recompensado = recompensado;
        this.nombreRecompensado = nombreRecompensado;
        this.tipoPago = tipoPago;
        this.montoVault = montoVault;
        this.items = items;
        this.fechaCreacion = System.currentTimeMillis();
    }

    public UUID getRecompensante() {
        return recompensante;
    }

    public String getNombreRecompensante() {
        return nombreRecompensante;
    }

    public UUID getRecompensado() {
        return recompensado;
    }

    public String getNombreRecompensado() {
        return nombreRecompensado;
    }

    public TipoPago getTipoPago() {
        return tipoPago;
    }

    public double getMontoVault() {
        return montoVault;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public String getMontoDescripcion() {
        if (tipoPago == TipoPago.VAULT) {
            return String.valueOf(montoVault);
        } else {
            int total = 0;
            for (ItemStack item : items) {
                total += item.getAmount();
            }
            return total + " ítem(s)";
        }
    }
                                  }
