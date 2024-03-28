package me.bounser.nascraft.database.sqlite;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.Database;
import me.bounser.nascraft.database.commands.*;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.Tradable;
import me.bounser.nascraft.market.unit.stats.Instant;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class SQLite implements Database {

    private Connection connection;

    private final String PATH = Nascraft.getInstance().getDataFolder().getPath() + "/data/sqlite.db";

    private static SQLite instance;

    public static SQLite getInstance() { return instance == null ? instance = new SQLite() : instance; }

    private void createDatabaseIfNotExists() {
        File databaseFile = new File(PATH);
        if (!databaseFile.exists()) {
            try {
                File parentDir = databaseFile.getParentFile();
                if (!parentDir.exists()) {
                    boolean dirsCreated = parentDir.mkdirs();
                    if (!dirsCreated) {
                        throw new RuntimeException("Failed to create directories for the database file.");
                    }
                }

                boolean fileCreated = databaseFile.createNewFile();
                if (!fileCreated) {
                    throw new RuntimeException("Failed to create the database file.");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void createTable(Connection connection, String tableName, String columns) {
        try {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (" + columns + ");");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void connect() {

        createDatabaseIfNotExists();

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + PATH);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void disconnect() {
        saveEverything();
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void createTables() {
        createTable(connection, "items",
                "identifier TEXT PRIMARY KEY, " +
                        "lastprice DOUBLE, " +
                        "lowest DOUBLE, " +
                        "highest DOUBLE, " +
                        "stock DOUBLE DEFAULT 0, " +
                        "taxes DOUBLE");

        createTable(connection, "prices",
                "identifier TEXT PRIMARY KEY," +
                        "date TEXT," +
                        "dayprices TEXT," + // 48
                        "monthprices TEXT," + // 30
                        "yearprices TEXT");

        createTable(connection, "prices_day",
                "id INTEGER PRIMARY KEY, " +
                        "day INT, " +
                        "date TEXT," +
                        "identifier TEXT," +
                        "price DOUBLE," +
                        "volume INT");

        createTable(connection, "prices_month",
                "id INTEGER PRIMARY KEY, " +
                        "day INT NOT NULL, " +
                        "date TEXT NOT NULL," +
                        "identifier TEXT NOT NULL," +
                        "price DOUBLE NOT NULL," +
                        "volume INT NOT NULL");

        createTable(connection, "prices_history",
                "id INTEGER PRIMARY KEY, " +
                        "day INT," +
                        "date TEXT NOT NULL," +
                        "identifier INT," +
                        "price DOUBLE," +
                        "volume INT");

        createTable(connection, "inventories",
                "uuid VARCHAR(36) NOT NULL," +
                        "identifier TEXT," +
                        "amount INT");

        createTable(connection, "capacities",
                "uuid VARCHAR(36) PRIMARY KEY," +
                        "capacity INT");

        createTable(connection, "discord_links",
                "userid VARCHAR(18) NOT NULL," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "nickname TEXT NOT NULL");

        createTable(connection, "trade_log",
                "id INTEGER PRIMARY KEY, " +
                        "uuid VARCHAR(36) NOT NULL," +
                        "day INT NOT NULL," +
                        "date TEXT NOT NULL," +
                        "identifier TEXT NOT NULL," +
                        "amount INT NOT NULL," +
                        "value TEXT NOT NULL," +
                        "buy INT NOT NULL, " +
                        "discord INT NOT NULL");

        createTable(connection, "broker_shares",
                "id INTEGER PRIMARY KEY, " +
                        "uuid VARCHAR(36) NOT NULL," +
                        "date TEXT NOT NULL," +
                        "broker TEXT NOT NULL," +
                        "quantity DOUBLE NOT NULL," +
                        "cost DOUBLE NOT NULL");

        createTable(connection, "shares_value",
                "id INTEGER PRIMARY KEY, " +
                        "broker TEXT NOT NULL," +
                        "day INT NOT NULL," +
                        "lastvalue TEXT NOT NULL");

        createTable(connection, "limit_orders",
                "id INTEGER PRIMARY KEY, " +
                        "day INT NOT NULL," +
                        "date TEXT NOT NULL," +
                        "expiration TEXT NOT NULL," +
                        "cost DOUBLE NOT NULL," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "identifier TEXT NOT NULL," +
                        "price DOUBLE NOT NULL," +
                        "quantity INT NOT NULL");

        createTable(connection, "to_deliver",
                "id INTEGER PRIMARY KEY, " +
                        "day INT NOT NULL," +
                        "date TEXT NOT NULL," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "identifier TEXT NOT NULL," +
                        "quantity INT NOT NULL");

        createTable(connection, "expired_orders",
                "id INTEGER PRIMARY KEY, " +
                        "day INT NOT NULL," +
                        "date TEXT NOT NULL," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "money DOUBLE NOT NULL," +
                        "identifier TEXT NOT NULL," +
                        "quantity INT NOT NULL");
    }

    @Override
    public void saveEverything() {
        for (Item item : MarketManager.getInstance().getAllItems()) {
            saveItem(item);
        }
    }

    @Override
    public void saveLink(String userId, UUID uuid, String nickname) {
        DiscordLink.saveLink(connection, userId, uuid, nickname);
    }

    @Override
    public void removeLink(String userId) {
        DiscordLink.removeLink(connection, userId);
    }

    @Override
    public UUID getUUID(String userId) {
        return DiscordLink.getUUID(connection, userId);
    }

    @Override
    public String getNickname(String userId) {
        return DiscordLink.getNickname(connection, userId);
    }

    @Override
    public String getUserId(UUID uuid) {
        return DiscordLink.getUserId(connection, uuid);
    }

    @Override
    public void saveDayPrice(Item item, Instant instant) {
        HistorialData.saveDayPrice(connection, item, instant);
    }

    @Override
    public void saveMonthPrice(Item item, Instant instant) {
        HistorialData.saveMonthPrice(connection, item, instant);
    }

    @Override
    public void saveHistoryPrices(Item item, Instant instant) {
        HistorialData.saveHistoryPrices(connection, item, instant);
    }

    @Override
    public List<Instant> getDayPrices(Item item) {
        return HistorialData.getDayPrices(connection, item);
    }

    @Override
    public List<Instant> getMonthPrices(Item item) {
        return HistorialData.getMonthPrices(connection, item);
    }

    @Override
    public List<Instant> getYearPrices(Item item) {
        return HistorialData.getYearPrices(connection, item);
    }

    @Override
    public List<Instant> getAllPrices(Item item) {
        return HistorialData.getAllPrices(connection, item);
    }

    @Override
    public void saveItem(Item item) {
        ItemProperties.saveItem(connection, item);
    }

    @Override
    public void retrieveItem(Item item) {
        ItemProperties.retrieveItem(connection, item);
    }

    @Override
    public float retrieveLastPrice(Item item) {
        return ItemProperties.retrieveLastPrice(connection, item);
    }

    @Override
    public void saveTrade(UUID uuid, Tradable tradable, int amount, float value, boolean buy, boolean discord) {
        TradesLog.saveTrade(connection, uuid, tradable, amount, value, buy, discord);
    }

    @Override
    public List<Trade> retrieveTrades(UUID uuid, int offset) {
        return TradesLog.retrieveTrades(connection, uuid, offset);
    }

    @Override
    public void purgeHistory() {
        TradesLog.purgeHistory(connection);
    }

    @Override
    public void updateItem(UUID uuid, Item item, int quantity) {
        VirtualInventory.updateItem(connection, uuid, item, quantity);
    }

    @Override
    public void removeItem(UUID uuid, Item item) {
        VirtualInventory.removeItem(connection, uuid, item);
    }

    @Override
    public void clearInventory(UUID uuid) {
        VirtualInventory.clearInventory(connection, uuid);
    }

    @Override
    public void updateCapacity(UUID uuid, int capacity) {
        VirtualInventory.updateCapacity(connection, uuid, capacity);
    }

    @Override
    public LinkedHashMap<Item, Integer> retrieveInventory(UUID uuid) {
        return VirtualInventory.retrieveInventory(connection, uuid);
    }

    @Override
    public int retrieveCapacity(UUID uuid) {
        return VirtualInventory.retrieveCapacity(connection, uuid);
    }

}