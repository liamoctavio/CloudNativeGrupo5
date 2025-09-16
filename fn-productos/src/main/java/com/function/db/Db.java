package com.function.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
  private Db() {}

  public static Connection connect() throws SQLException {
    final String url  = getenvRequired("DB_URL"); 
    final String user = getenvRequired("DB_USER");
    final String pass = getenvRequired("DB_PASS");
    return DriverManager.getConnection(url, user, pass);
  }

  private static String getenvRequired(String key) {
    String v = System.getenv(key);
    if (v == null || v.isBlank()) {
      throw new IllegalStateException("Falta variable de entorno: " + key);
    }
    return v;
  }
}


