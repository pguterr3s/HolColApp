package si.holcol.db;

import java.sql.Connection;
import java.sql.SQLException;

/** Execucao de um bloco de trabalho numa unica transacao (commit ou rollback). */
public final class Tx {
    private Tx() { }

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    public static <T> T run(Connection c, Work<T> work) throws SQLException {
        boolean previous = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            T result = work.run(c);
            c.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(previous);
        }
    }
}
