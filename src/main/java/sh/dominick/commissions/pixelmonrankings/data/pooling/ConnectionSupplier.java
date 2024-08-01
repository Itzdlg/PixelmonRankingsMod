package sh.dominick.commissions.pixelmonrankings.data.pooling;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionSupplier {
    Connection getConnection() throws SQLException;
}
