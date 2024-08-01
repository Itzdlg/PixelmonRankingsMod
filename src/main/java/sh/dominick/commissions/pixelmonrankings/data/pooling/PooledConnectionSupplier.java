package sh.dominick.commissions.pixelmonrankings.data.pooling;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class PooledConnectionSupplier implements ConnectionSupplier {
    private final HikariConfig config;
    private final HikariDataSource ds;

    public PooledConnectionSupplier(HikariConfig config) {
        this.config = config;
        this.ds = new HikariDataSource(config);
    }

    public PooledConnectionSupplier(String url) {
        this.config = new HikariConfig();

        config.setJdbcUrl(url);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.ds = new HikariDataSource(config);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
