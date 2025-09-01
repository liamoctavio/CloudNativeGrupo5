package com.function.model;

import com.function.db.Db;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.ResultSet;

public class TestDbFunction {

  @FunctionName("testdb")
  public HttpResponseMessage run(
      @HttpTrigger(
          name = "req",
          methods = { HttpMethod.GET },
          authLevel = AuthorizationLevel.ANONYMOUS, // sin clave para probar rápido
          route = "testdb"
      ) HttpRequestMessage<String> request,
      final ExecutionContext ctx) {

    try (var con = Db.connect();
         var st  = con.createStatement();
         ResultSet rs = st.executeQuery("SELECT 1 FROM DUAL")) {

      if (rs.next()) {
        int v = rs.getInt(1);
        ctx.getLogger().info("Conexión OK. SELECT 1 = " + v);
        return request.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "text/plain")
            .body("Conexión OK. SELECT 1 = " + v)
            .build();
      }
      return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Conexión establecida pero sin resultados.")
          .build();

    } catch (Exception ex) {
      ctx.getLogger().severe("Error conectando a Oracle: " + ex.getMessage());
      return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error: " + ex.getMessage())
          .build();
    }
  }
}
