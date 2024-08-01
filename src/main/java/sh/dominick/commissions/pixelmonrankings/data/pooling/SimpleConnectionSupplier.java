package sh.dominick.commissions.pixelmonrankings.data.pooling;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SimpleConnectionSupplier implements ConnectionSupplier {
    private final String url;

    public SimpleConnectionSupplier(String url) {
        this.url = url;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }
}
