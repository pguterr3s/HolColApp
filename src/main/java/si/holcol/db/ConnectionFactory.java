package main.java.si.holcol.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Cria ligacoes ao PostgreSQL. Cada operacao da aplicacao abre a sua ligacao
 * num try-with-resources, pelo que o tempo de vida da ligacao fica limitado a operacao.
 * Parametros por omissao podem ser alterados com variaveis de ambiente:
 * HOLCOL_DB_HOST, HOLCOL_DB_PORT, HOLCOL_DB_NAME, HOLCOL_DB_USER, HOLCOL_DB_PASSWORD.
 */
public final class ConnectionFactory {
    private static final String SCHEMA = "holcol";

    private ConnectionFactory() { }

    private static String env(String name, String def) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? def : v;
    }

    public static Connection getConnection() throws SQLException {
        String url = String.format("jdbc:postgresql://%s:%s/%s?currentSchema=%s",
                env("HOLCOL_DB_HOST", "localhost"),
                env("HOLCOL_DB_PORT", "5432"),
                env("HOLCOL_DB_NAME", "holcol"),
                SCHEMA);
        Properties props = new Properties();
        props.setProperty("user", env("HOLCOL_DB_USER", "postgres"));
        props.setProperty("password", env("HOLCOL_DB_PASSWORD", "postgres"));
        return DriverManager.getConnection(url, props);
    }
}
