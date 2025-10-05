package com.function;

import com.google.gson.*;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.util.logging.Logger;

public class OnInventarioEventFunction {

  @FunctionName("onInventarioEvent")
  public void run(
      @EventGridTrigger(name = "eventGridEvent") String content,
      final ExecutionContext context
  ) {
    Logger logger = context.getLogger();
    logger.info("Función con Event Grid trigger ejecutada.");

    try {
      JsonElement root = JsonParser.parseString(content);

      if (root.isJsonArray()) {
        JsonArray arr = root.getAsJsonArray();
        for (JsonElement el : arr) {
          if (el.isJsonObject()) procesarUno(el.getAsJsonObject(), logger);
        }
      } else if (root.isJsonObject()) {
        procesarUno(root.getAsJsonObject(), logger);
      } else {
        logger.warning("Contenido no reconocido: " + content);
      }

    } catch (Exception e) {
      logger.severe("Error procesando evento: " + e.getMessage());
    }
  }

  private void procesarUno(JsonObject ev, Logger logger) {
    String eventType = getString(ev, "eventType");
    if (eventType == null) eventType = getString(ev, "type");

    String subject = getString(ev, "subject");
    if (subject == null) subject = getString(ev, "source");

    JsonElement data = ev.get("data");

    logger.info("Evento recibido -> type=" + eventType + " subject=" + subject);
    if (data != null) logger.info("Data del evento: " + data.toString());
  }

  private static String getString(JsonObject obj, String prop) {
    return (obj.has(prop) && !obj.get(prop).isJsonNull()) ? obj.get(prop).getAsString() : null;
  }
}
