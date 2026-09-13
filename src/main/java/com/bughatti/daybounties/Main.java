package com.bughatti.daybounties;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private BountyManager bountyManager;
    private BountyGUI bountyGUI;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().warning("No se encontró Vault o un plugin de economía. Los pagos con dinero estarán desactivados.");
        }

        this.bountyManager = new BountyManager(this);
        this.bountyGUI = new BountyGUI(this);

        getCommand("bounty").setExecutor(new BountyCommand(this));

        getLogger().info("DayBounties habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        getLogger().info("DayBounties deshabilitado.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        this.economy = rsp.getProvider();
        return true;
    }

    public Economy getEconomy() {
        return economy;
    }

    public BountyManager getBountyManager() {
        return bountyManager;
    }

    public BountyGUI getBountyGUI() {
        return bountyGUI;
    }

    public FileConfiguration getMessages() {
        return getConfig();
    }

    public static Main getInstance() {
        return instance;
    }
}
