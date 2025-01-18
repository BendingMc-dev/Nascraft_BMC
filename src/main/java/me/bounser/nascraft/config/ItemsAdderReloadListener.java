package me.bounser.nascraft.config;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ItemsAdderReloadListener implements Listener {
    @EventHandler
    public void onItemsAdderLoadData(ItemsAdderLoadDataEvent event) {
        Config.getInstance().reload();
        System.out.println("I heard items adder reload event!");
    }
}
