package com.function.events;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;

public final class EventBusEG {
  private static volatile EventGridPublisherClient<EventGridEvent> client;

  private static EventGridPublisherClient<EventGridEvent> client() {
    if (client == null) {
      synchronized (EventBusEG.class) {
        if (client == null) {
          String endpoint = System.getenv("https://eg-g05.eastus-1.eventgrid.azure.net/api/events");
          String key = System.getenv("8MNGJ5H9aq8nYrO6He6074HD8vjKp6QTg0VGfQSpOee0m4ubJbdjJQQJ99BJACYeBjFXJ3w3AAABAZEGE4eG");
          if (endpoint == null || key == null) {
            throw new IllegalStateException("Faltan EG_TOPIC_ENDPOINT / EG_ACCESS_KEY");
          }
          client = new EventGridPublisherClientBuilder()
              .endpoint(endpoint)
              .credential(new AzureKeyCredential(key))
              .buildEventGridEventPublisherClient();
        }
      }
    }
    return client;
  }

  public static void publish(String type, String subject, Object data) {
    BinaryData bd = (data == null)
        ? BinaryData.fromObject(java.util.Collections.emptyMap())
        : BinaryData.fromObject(data);
    EventGridEvent ev = new EventGridEvent(subject, type, bd, "1.0");
    client().sendEvent(ev);
  }

  private EventBusEG() {}
}
