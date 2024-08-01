package sh.dominick.commissions.pixelmonrankings.data;

import sh.dominick.commissions.pixelmonrankings.PixelmonRankingsMod;
import sh.dominick.commissions.pixelmonrankings.Statistic;
import sh.dominick.commissions.pixelmonrankings.data.pooling.ConnectionSupplier;
import sh.dominick.commissions.pixelmonrankings.data.pooling.PooledConnectionSupplier;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MySQLDataManager implements IDataManager {
    private final ConnectionSupplier connectionSupplier;

    private final String url;
    private final String changesTableName;
    private final String profilesTableName;

    public MySQLDataManager(String url, String changesTableName, String profilesTableName) {
        this.url = url;
        this.changesTableName = changesTableName;
        this.profilesTableName = profilesTableName;
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        this.connectionSupplier = new PooledConnectionSupplier(url);
        
        createTablesIfNotExists();
    }

    private void createTablesIfNotExists() {
        CompletableFuture.runAsync(() -> {
            String createChangesTableSQL = "CREATE TABLE IF NOT EXISTS " + changesTableName + " (\n"
                    + "    id INT AUTO_INCREMENT PRIMARY KEY,\n"
                    + "    player VARCHAR(36) NOT NULL,\n"
                    + "    statistic TINYINT NOT NULL,\n"
                    + "    `change` DOUBLE NOT NULL,\n"
                    + "    timestamp BIGINT NOT NULL\n"
                    + ");";

            String createProfilesTableSQL = "CREATE TABLE IF NOT EXISTS " + profilesTableName + " (\n"
                    + "    player VARCHAR(36) PRIMARY KEY,\n"
                    + "    playerName VARCHAR(32) NOT NULL,\n"
                    + "    texture TEXT NOT NULL\n"
                    + ");";

            try (Connection conn = connectionSupplier.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute(createChangesTableSQL);
                stmt.execute(createProfilesTableSQL);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, PixelmonRankingsMod.EXECUTOR);
    }

    private void setPlayer(PreparedStatement stmt, int index, UUID player) throws SQLException {
        if (player == null)
            return;

        stmt.setString(index, player.toString());
    }

    private UUID getPlayer(ResultSet rs, String column) throws SQLException {
        return UUID.fromString(rs.getString(column));
    }

    @Override
    public CompletableFuture<Void> recordChange(Key key, double value) {
        return CompletableFuture.runAsync(() -> {
            String query = "INSERT INTO " + changesTableName + " (player, statistic, `change`, timestamp) VALUES (?, ?, ?, ?)";
            try (Connection conn = connectionSupplier.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
                setPlayer(stmt, 1, key.player());
                stmt.setInt(2, key.statistic().storageId());
                stmt.setDouble(3, value);
                stmt.setLong(4, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, PixelmonRankingsMod.EXECUTOR);
    }

    @Override
    public CompletableFuture<Double> aggregate(Key key, @Nullable Instant from, @Nullable Instant to) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT SUM(`change`) FROM " + changesTableName + " WHERE player = ? AND statistic = ?"
                    + (from != null ? " AND timestamp >= ?" : "")
                    + (to != null ? " AND timestamp <= ?" : "")
                    + ";";

            try (Connection conn = connectionSupplier.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
                setPlayer(stmt, 1, key.player());
                stmt.setInt(2, key.statistic().storageId());

                int index = 3;
                if (from != null)
                    stmt.setLong(index++, from.toEpochMilli());

                if (to != null)
                    stmt.setLong(index, to.toEpochMilli());

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return 0.0;
        }, PixelmonRankingsMod.EXECUTOR);
    }

    @Override
    public CompletableFuture<Entry[]> sort(Statistic statistic, @Nullable Instant from, @Nullable Instant to, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT player, SUM(`change`) as `total_change` FROM " + changesTableName + " WHERE statistic = ?"
                    + (from != null ? " AND timestamp >= ?" : "")
                    + (to != null ? " AND timestamp <= ?" : "")
                    + " GROUP BY player ORDER BY `total_change` DESC"
                    + (limit > 0 ? " LIMIT ?" : "")
                    + ";";

            List<Entry> sortedEntries = new ArrayList<>();

            try (Connection conn = connectionSupplier.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, statistic.storageId());
                int index = 2;
                if (from != null)
                    stmt.setLong(index++, from.toEpochMilli());

                if (to != null)
                    stmt.setLong(index++, to.toEpochMilli());

                if (limit > 0)
                    stmt.setInt(index, limit);

                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    UUID player = getPlayer(rs, "player");
                    double totalChange = rs.getDouble("total_change");

                    Key key = new Key(player, statistic);
                    Entry entry = new Entry(key, totalChange);
                    sortedEntries.add(entry);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return sortedEntries.toArray(new Entry[0]);
        }, PixelmonRankingsMod.EXECUTOR);
    }

    @Override
    public CompletableFuture<Long> findPositionSorted(Key key, @Nullable Instant from, @Nullable Instant to) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT player, SUM(`change`) as `total_change`, RANK() OVER (ORDER BY SUM(`change`) DESC) as `rank` FROM " + changesTableName + " WHERE statistic = ?"
                    + (from != null ? " AND timestamp >= ?" : "")
                    + (to != null ? " AND timestamp <= ?" : "")
                    + " GROUP BY player"
                    + ";";

            try (Connection conn = connectionSupplier.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, key.statistic().storageId());

                int index = 2;
                if (from != null)
                    stmt.setLong(index++, from.toEpochMilli());

                if (to != null)
                    stmt.setLong(index, to.toEpochMilli());

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    if (getPlayer(rs, "player").equals(key.player()))
                        return rs.getLong("rank");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return -1L;
        }, PixelmonRankingsMod.EXECUTOR);
    }

    @Override
    public CompletableFuture<Long> count(Statistic statistic, @Nullable Instant from, @Nullable Instant to) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT COUNT(DISTINCT player) AS player_count FROM " + changesTableName + " WHERE statistic = ?"
                    + (from != null ? " AND timestamp >= ?" : "")
                    + (to != null ? " AND timestamp <= ?" : "")
                    + ";";
            try (Connection conn = connectionSupplier.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, statistic.storageId());

                int index = 2;
                if (from != null)
                    stmt.setLong(index++, from.toEpochMilli());

                if (to != null)
                    stmt.setLong(index, to.toEpochMilli());

                ResultSet rs = stmt.executeQuery();
                if (rs.next())
                    return rs.getLong("player_count");

            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0L;
        }, PixelmonRankingsMod.EXECUTOR);
    }

    @Override
    public CompletableFuture<Void> recordGameProfile(UUID player, String playerName, String texture) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO " + profilesTableName + " (player, playerName, texture) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE playerName = VALUES(playerName), texture = VALUES(texture)";
            try (Connection conn = connectionSupplier.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                setPlayer(pstmt, 1, player);
                pstmt.setString(2, playerName);
                pstmt.setString(3, texture);

                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, PixelmonRankingsMod.EXECUTOR);
    }

    @Override
    public CompletableFuture<CachedGameProfile> getGameProfile(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT playerName, texture FROM " + profilesTableName + " WHERE player = ?";
            try (Connection conn = connectionSupplier.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
                setPlayer(stmt, 1, player);

                ResultSet rs = stmt.executeQuery();
                if (rs.next())
                    return new CachedGameProfile(player, rs.getString("playerName"), rs.getString("texture"));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }, PixelmonRankingsMod.EXECUTOR);
    }

    @Override
    public CompletableFuture<Void> compact(@Nullable Instant from, @Nullable Instant to) {
        return CompletableFuture.runAsync(() -> {
            String query = "SELECT player, statistic, SUM(`change`) as `total_change` FROM " + changesTableName + " WHERE 1"
                    + (from != null ? " AND timestamp >= ?" : "")
                    + (to != null ? " AND timestamp <= ?" : "")
                    + " GROUP BY player, statistic;";

            long next;
            if (to != null) next = to.toEpochMilli() + 1;
            else next = System.currentTimeMillis() + 1;

            try (Connection conn = connectionSupplier.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
                int index = 1;
                if (from != null)
                    stmt.setLong(index++, from.toEpochMilli());

                if (to != null)
                    stmt.setLong(index, to.toEpochMilli());

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    UUID player = getPlayer(rs, "player");
                    Statistic statistic = Statistic.ofStorageId(rs.getByte("statistic"));
                    double totalChange = rs.getDouble("total_change");

                    String insertSql = "INSERT INTO " + changesTableName + "(player, statistic, `change`, timestamp) VALUES(?,?,?,?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                        setPlayer(insertStmt, 1, player);
                        insertStmt.setByte(2, statistic.storageId());
                        insertStmt.setDouble(3, totalChange);
                        insertStmt.setLong(4, next);

                        insertStmt.executeUpdate();
                    }
                }

                String deleteSql = "DELETE FROM " + changesTableName + " WHERE timestamp != " + next;
                if (from != null)
                    deleteSql += " AND timestamp >= " + from.toEpochMilli();
                if (to != null)
                    deleteSql += " AND timestamp <= " + to.toEpochMilli();

                deleteSql += ";";

                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, PixelmonRankingsMod.EXECUTOR);
    }
}
