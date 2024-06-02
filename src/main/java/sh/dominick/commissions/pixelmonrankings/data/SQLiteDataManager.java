package sh.dominick.commissions.pixelmonrankings.data;

import sh.dominick.commissions.pixelmonrankings.PixelmonRankingsMod;
import sh.dominick.commissions.pixelmonrankings.Statistic;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLiteDataManager implements IDataManager {
    private final String url;
    private final String changesTableName;
    private final String profilesTableName;

    public SQLiteDataManager(String url, String changesTableName, String profilesTableName) {
        this.url = url;
        this.changesTableName = changesTableName;
        this.profilesTableName = profilesTableName;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        createTablesIfNotExists();
    }

    private void createTablesIfNotExists() {
        CompletableFuture.runAsync(() -> {
            String createChangesTableSQL = "CREATE TABLE IF NOT EXISTS " + changesTableName + " (\n"
                    + "    id INTEGER PRIMARY KEY,\n"
                    + "    player BLOB,\n"
                    + "    statistic REAL,\n"
                    + "    change REAL,\n"
                    + "    timestamp INTEGER\n"
                    + ");";

            String createProfilesTableSQL = "CREATE TABLE IF NOT EXISTS " + profilesTableName + " (\n"
                    + "    player BLOB UNIQUE PRIMARY KEY,\n"
                    + "    playerName TEXT,\n"
                    + "    texture TEXT\n"
                    + ");";

            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {

                stmt.execute(createChangesTableSQL);
                stmt.execute(createProfilesTableSQL);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }, PixelmonRankingsMod.EXECUTOR);
    }

    private void setPlayer(PreparedStatement stmt, int index, UUID player) throws SQLException {
        if (player == null)
            return;

        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(player.getMostSignificantBits());
        bb.putLong(player.getLeastSignificantBits());
        byte[] ba = bb.array();

        stmt.setBytes(index, ba);
    }

    private UUID getPlayer(ResultSet rs, String column) throws SQLException {
        byte[] ba = rs.getBytes(column);

        if (ba == null)
            return null;

        ByteBuffer bb = ByteBuffer.wrap(ba);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();

        return new UUID(firstLong, secondLong);
    }

    @Override
    public CompletableFuture<Void> recordChange(Key key, double value) {
        if (key.player() == null)
            return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO " + changesTableName + "(player, statistic, change, timestamp) VALUES(?,?,?,?)";
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                setPlayer(pstmt, 1, key.player());
                pstmt.setByte(2, key.statistic().storageId());
                pstmt.setDouble(3, value);
                pstmt.setLong(4, System.currentTimeMillis());

                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }, PixelmonRankingsMod.EXECUTOR);
    }

    @Override
    public CompletableFuture<Double> aggregate(Key key, @Nullable Instant from, @Nullable Instant to) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT SUM(change) FROM " + changesTableName + " WHERE player = ? AND statistic = ?";
            if (from != null) {
                sql += " AND timestamp >= " + from.toEpochMilli();
            }
            if (to != null) {
                sql += " AND timestamp <= " + to.toEpochMilli();
            }

            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                setPlayer(pstmt, 1, key.player());
                pstmt.setByte(2, key.statistic().storageId());

                ResultSet rs = pstmt.executeQuery();
                if (rs.next())
                    return rs.getDouble(1);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }

            return 0d;
        }, PixelmonRankingsMod.EXECUTOR);
    }

    @Override
    public CompletableFuture<Entry[]> sort(Statistic statistic, @Nullable Instant from, @Nullable Instant to, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT player, SUM(change) AS total_change FROM " + changesTableName + " WHERE statistic = ?";
            if (from != null)
                sql += " AND timestamp >= " + from.toEpochMilli();
            if (to != null)
                sql += " AND timestamp <= " + to.toEpochMilli();

            sql += " GROUP BY player ORDER BY total_change DESC";

            if (limit > -1)
                sql += " LIMIT " + limit;

            sql += ";";

            List<Entry> sortedEntries = new ArrayList<>();

            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setByte(1, statistic.storageId());
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    UUID playerUUID = getPlayer(rs, "player");
                    double totalChange = rs.getDouble("total_change");

                    Key key = new Key(playerUUID, statistic);
                    Entry entry = new Entry(key, totalChange);
                    sortedEntries.add(entry);
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }

            return sortedEntries.toArray(new Entry[0]);
        }, PixelmonRankingsMod.EXECUTOR);
    }

    @Override
    public CompletableFuture<Long> findPositionSorted(Key key, @Nullable Instant from, @Nullable Instant to) {
         return CompletableFuture.supplyAsync(() -> {
             String sql = "SELECT player, SUM(change) AS total_change " +
                     "FROM " + changesTableName + " " +
                     "WHERE statistic = ? ";

             if (from != null)
                 sql += "AND timestamp >= " + from.toEpochMilli();
             if (to != null)
                 sql += " AND timestamp <= " + to.toEpochMilli();

             sql += " GROUP BY player ORDER BY total_change DESC;";

             try (Connection conn = DriverManager.getConnection(url);
                  PreparedStatement pstmt = conn.prepareStatement(sql)) {

                 pstmt.setByte(1, key.statistic().storageId());

                 ResultSet rs = pstmt.executeQuery();

                 long position = 1;
                 UUID playerUUID = key.player();
                 while (rs.next()) {
                     if (getPlayer(rs, "player").equals(playerUUID))
                         return position;
                     position++;
                 }
             } catch (SQLException e) {
                 e.printStackTrace();
             }

             return -1L; // Player not found
         }, PixelmonRankingsMod.EXECUTOR);
    }

    public CompletableFuture<Long> count(Statistic statistic, Instant from, Instant to) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(DISTINCT player) AS player_count " +
                    "FROM " + changesTableName + " " +
                    "WHERE statistic = ? ";

            if (from != null)
                sql += "AND timestamp >= " + from.toEpochMilli();
            if (to != null)
                sql += " AND timestamp <= " + to.toEpochMilli();

            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setByte(1, statistic.storageId());

                ResultSet rs = pstmt.executeQuery();

                if (rs.next())
                    return rs.getLong("player_count");

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return 0L; // No players found
        }, PixelmonRankingsMod.EXECUTOR);
    }

    @Override
    public CompletableFuture<Void> recordGameProfile(UUID player, String playerName, String texture) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO " + profilesTableName + "(player, playerName, texture) VALUES(?,?,?)";
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                setPlayer(pstmt, 1, player);
                pstmt.setString(2, playerName);
                pstmt.setString(3, texture);

                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }, PixelmonRankingsMod.EXECUTOR);
    }

    @Override
    public CompletableFuture<CachedGameProfile> getGameProfile(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT playerName, texture FROM " + profilesTableName + " WHERE player = ?";
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                setPlayer(pstmt, 1, player);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String playerName = rs.getString("playerName");
                        String texture = rs.getString("texture");
                        return new CachedGameProfile(player, playerName, texture);
                    }
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }

            return null;
        }, PixelmonRankingsMod.EXECUTOR);
    }

    @Override
    public CompletableFuture<Void> compact(@Nullable Instant from, @Nullable Instant to) {
        return CompletableFuture.runAsync(() -> {
            String sql = "SELECT player, statistic, SUM(change) AS total_change " +
                    "FROM " + changesTableName + " WHERE 1";

            if (from != null)
                sql += " AND timestamp >= " + from.toEpochMilli();
            if (to != null)
                sql += " AND timestamp <= " + to.toEpochMilli();

            sql += ";";

            long next;
            if (to != null) next = to.toEpochMilli() + 1;
            else next = System.currentTimeMillis() + 1;

            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        UUID player = getPlayer(rs, "player");
                        Statistic statistic = Statistic.ofStorageId(rs.getByte("statistic"));
                        double totalChange = rs.getDouble("total_change");

                        String insertSql = "INSERT INTO " + changesTableName + "(player, statistic, change, timestamp) VALUES(?,?,?,?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                            setPlayer(insertStmt, 1, player);
                            insertStmt.setByte(2, statistic.storageId());
                            insertStmt.setDouble(3, totalChange);
                            insertStmt.setLong(4, next);

                            insertStmt.executeUpdate();
                        }
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