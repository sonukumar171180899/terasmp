package com.tokensmp.storage;

import com.tokensmp.TokenSMP;
import com.tokensmp.config.ConfigManager;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;

public class StorageManager {

    private final TokenSMP plugin;
    private Connection connection;
    private final File databaseFile;

    public StorageManager(TokenSMP plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "tokensmp.db");
    }

    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            
            boolean isNew = !databaseFile.exists();
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            connection.setAutoCommit(true);
            
            if (isNew) {
                createTables();
            } else {
                migrateTables();
            }
            
            plugin.getLogger().info("Database connected: " + databaseFile.getAbsolutePath());
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite JDBC driver not found!", e);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to database!", e);
        }
    }

    private void createTables() throws SQLException {
        String createPlayersTable = """
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                lives INTEGER NOT NULL DEFAULT 10,
                last_login INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
        """;
        
        String createTokenDataTable = """
            CREATE TABLE IF NOT EXISTS token_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                token_id TEXT NOT NULL,
                data TEXT,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                UNIQUE(player_uuid, token_id)
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPlayersTable);
            stmt.execute(createTokenDataTable);
            
            // Create indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_token_data_player ON token_data(player_uuid)");
        }
    }

    private void migrateTables() throws SQLException {
        // Check if lives column exists, if not add it
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, "players", "lives")) {
            if (!rs.next()) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE players ADD COLUMN lives INTEGER NOT NULL DEFAULT 10");
                    plugin.getLogger().info("Added lives column to players table");
                }
            }
        }
    }

    public void createPlayer(UUID uuid, int defaultLives) {
        String sql = """
            INSERT OR IGNORE INTO players (uuid, lives, last_login, created_at)
            VALUES (?, ?, ?, ?)
        """;
        
        long now = System.currentTimeMillis();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, defaultLives);
            stmt.setLong(3, now);
            stmt.setLong(4, now);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create player: " + uuid, e);
        }
    }

    public int getLives(UUID uuid) {
        String sql = "SELECT lives FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("lives");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get lives for: " + uuid, e);
        }
        return -1; // Player not found
    }

    public void setLives(UUID uuid, int lives) {
        String sql = "UPDATE players SET lives = ? WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, Math.max(0, lives));
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to set lives for: " + uuid, e);
        }
    }

    public void addLives(UUID uuid, int amount) {
        String sql = "UPDATE players SET lives = lives + ? WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, amount);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to add lives for: " + uuid, e);
        }
    }

    public void removeLives(UUID uuid, int amount) {
        String sql = "UPDATE players SET lives = MAX(0, lives - ?) WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, amount);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to remove lives for: " + uuid, e);
        }
    }

    public void updateLastLogin(UUID uuid) {
        String sql = "UPDATE players SET last_login = ? WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update last login for: " + uuid, e);
        }
    }

    public boolean playerExists(UUID uuid) {
        String sql = "SELECT 1 FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check player existence: " + uuid, e);
        }
        return false;
    }

    public void saveTokenData(UUID uuid, String tokenId, String data) {
        String sql = """
            INSERT INTO token_data (player_uuid, token_id, data, updated_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(player_uuid, token_id) DO UPDATE SET
                data = excluded.data,
                updated_at = excluded.updated_at
        """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, tokenId);
            stmt.setString(3, data);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save token data for: " + uuid, e);
        }
    }

    public String getTokenData(UUID uuid, String tokenId) {
        String sql = "SELECT data FROM token_data WHERE player_uuid = ? AND token_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, tokenId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("data");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get token data for: " + uuid, e);
        }
        return null;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to close database connection", e);
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }
}