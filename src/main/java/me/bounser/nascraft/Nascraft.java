package me.bounser.nascraft;

import me.bounser.nascraft.advancedgui.LayoutModifier;
import me.bounser.nascraft.commands.*;
import me.bounser.nascraft.commands.discord.DiscordCommand;
import me.bounser.nascraft.commands.discord.DiscordInventoryInGame;
import me.bounser.nascraft.commands.sellall.SellAllCommand;
import me.bounser.nascraft.commands.sellall.SellAllTabCompleter;
import me.bounser.nascraft.commands.sellinv.SellInvListener;
import me.bounser.nascraft.commands.sellinv.SellInvCommand;
import me.bounser.nascraft.database.SQLite;
import me.bounser.nascraft.discord.DiscordBot;
import me.bounser.nascraft.discord.linking.LinkCommand;
import me.bounser.nascraft.market.managers.BrokersManager;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.placeholderapi.PAPIExpansion;
import me.bounser.nascraft.config.Config;
import me.leoko.advancedgui.AdvancedGUI;
import me.leoko.advancedgui.manager.GuiItemManager;
import me.leoko.advancedgui.manager.GuiWallManager;
import me.leoko.advancedgui.manager.LayoutManager;
import me.bounser.nascraft.bstats.Metrics;
import me.leoko.advancedgui.utils.VersionMediator;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

import java.io.*;
import java.util.List;


public final class Nascraft extends JavaPlugin {

    private static Nascraft main;
    private static Economy economy = null;

    private static final String AdvancedGUI_version = "2.2.7";

    public static Nascraft getInstance() { return main; }

    @Override
    public void onEnable() {

        main = this;

        new Metrics(this, 18404);

        Config config = Config.getInstance();

        if (!setupEconomy()) {
            getLogger().severe("Nascraft failed to load! Vault is required.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("AdvancedGUI") == null) {
            getLogger().warning("AdvancedGUI is not installed! You won't have graphs in-game without it!");
            getLogger().warning("Learn more about AdvancedGUI here: https://www.spigotmc.org/resources/83636/");
        } else if (!Bukkit.getPluginManager().getPlugin("AdvancedGUI").getDescription().getVersion().equals(AdvancedGUI_version)){
            getLogger().warning("This plugin was made using AdvancedGUI " + AdvancedGUI_version + "! You may encounter errors on other versions");
        }

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI detected!");
            new PAPIExpansion().register();
        }

        if(config.getDiscordEnabled()) {
            getLogger().info("Enabling discord integration!");

            getCommand("link").setExecutor(new LinkCommand());
            //getCommand("setalert").setExecutor(new SetAlertCommand());
            //("alerts").setExecutor(new AlertsCommand());
            getCommand("discord").setExecutor(new DiscordCommand());

            Bukkit.getPluginManager().registerEvents(new DiscordInventoryInGame(), this);

            new DiscordBot();
        }

        if (config.getCheckResources()) checkResources();

        MarketManager.getInstance();
        BrokersManager.getInstance();

        LayoutManager.getInstance().registerLayoutExtension(LayoutModifier.getInstance(), this);

        getCommand("nascraft").setExecutor(new NascraftCommand());
        getCommand("market").setExecutor(new MarketCommand());

        List<String> commands = config.getCommands();
        if(commands == null) return;

        if(commands.contains("sellhand")) getCommand("sellhand").setExecutor(new SellHandCommand());
        if(commands.contains("sell")) {
            getCommand("sellinv").setExecutor(new SellInvCommand());
            Bukkit.getPluginManager().registerEvents(new SellInvListener(), this);
        }
        if(commands.contains("sellall")) {
            getCommand("sellall").setExecutor(new SellAllCommand());
            getCommand("sellall").setTabCompleter(new SellAllTabCompleter());
        }

    }

    @Override
    public void onDisable() {
        SQLite.getInstance().shutdown();

        if (Config.getInstance().getDiscordEnabled()) {
            DiscordBot.getInstance().removeAllMessages();
            DiscordBot.getInstance().sendClosedMessage();
            DiscordBot.getInstance().getJDA().shutdown();
        }
    }

    public static Economy getEconomy() { return economy; }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) { return false; }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) { return false; }

        economy = rsp.getProvider();
        return economy != null;
    }

    public void checkResources() {

        getLogger().info("Checking required layouts... ");
        getLogger().info("If you want to disable this procedure, set auto_resources_injection to false in the config.yml file.");

        File fileToReplace = new File(getDataFolder().getParent() + "/AdvancedGUI/layout/Nascraft.json");

        if (!fileToReplace.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(getResource("Nascraft.json")));
                StringBuilder jsonContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonContent.append(line);
                }
                reader.close();

                FileUtils.writeStringToFile(fileToReplace, jsonContent.toString(), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            getLogger().info("Layout Nascraft.json added.");

            LayoutManager.getInstance().shutdownSync();
            GuiWallManager.getInstance().shutdown();
            GuiItemManager.getInstance().shutdown();

            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                AdvancedGUI.getInstance().readConfig();
                VersionMediator.reload();
                LayoutManager.getInstance().reload(layout -> getLogger().severe("§cFailed to load layout: " + layout + " §7(see console for details)"));
                Bukkit.getScheduler().runTask(AdvancedGUI.getInstance(), () -> {
                    GuiWallManager.getInstance().setup();
                    GuiItemManager.getInstance().setup();
                });
            });
        } else {
            getLogger().info("Layout (Nascraft.json) present!");
        }
    }
}
