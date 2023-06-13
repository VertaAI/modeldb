package ai.verta.modeldb.common.nats;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.google.common.annotations.VisibleForTesting;
import io.nats.client.*;
import io.nats.client.api.*;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class JetstreamConnector {
  private static final Duration RECONNECT_WAIT_INTERVAL = Duration.ofSeconds(2L);
  public static final int MEGABYTE = 1024 * 1024;
  public static final int MAX_STREAM_BYTES = 100 * MEGABYTE;
  public static final int MAX_STREAM_MESSAGES = 10000;
  public final int maxMessageReplicas;
  private final Map<String, JetStream> jetStreamMap = new HashMap<>();
  private final JetstreamConfig config;
  private final Supplier<Collection<String>> streamNamesToManage;
  private Connection natsConnection;

  public JetstreamConnector(
      JetstreamConfig config, Supplier<Collection<String>> streamNamesToManage) {
    this.config = config;
    this.streamNamesToManage = streamNamesToManage;
    this.maxMessageReplicas = config.getMaxMessageReplicas();
  }

  public JetStream getJetStream(String streamName) {
    log.info("Getting JetStream for stream {}", streamName);
    checkNatsConnection();
    return jetStreamMap.get(streamName);
  }

  private void checkNatsConnection() {
    log.info("Checking NATS connection");
    if (natsConnection == null || natsConnection.getStatus() == Connection.Status.CLOSED) {
      log.info("Getting new connection");
      natsConnection = getConnection();
    }
    log.info("Ensuring stream exists");
  }

  private Connection getConnection() {
    if (!config.isEnabled()) {
      throw new IllegalStateException("JetStream is not enabled");
    }
    String port = config.getPort() > 0 ? ":" + config.getPort() : "";
    final var natsUri = "nats://" + config.getHost() + port;

    Options options =
        new Options.Builder()
            .server(natsUri)
            // Ref: https://docs.nats.io/using-nats/developer/connecting/reconnect/wait
            .reconnectWait(RECONNECT_WAIT_INTERVAL)
            .errorListener(new NatsErrorListener())
            .connectionListener(
                (connection, events) -> {
                  log.info(
                      "Nats server status:{}, message:{}", connection.getStatus(), events.name());
                  if (connection.getStatus().equals(Connection.Status.CONNECTED)
                      && events.equals(ConnectionListener.Events.RESUBSCRIBED)) {
                    updateJetStreamFromNewConnection(connection, events);
                  }
                })
            .build();

    log.info("Connecting to NATS JetStream uri: " + natsUri);
    return connect(options);
  }

  private void updateJetStreamFromNewConnection(
      Connection connection, ConnectionListener.Events events) {
    if (connection.getStatus().equals(Connection.Status.CONNECTED)
        && events.name().equals("RESUBSCRIBED")) {
      verifyStreams();
    }
  }

  public void verifyStreams() {
    streamNamesToManage.get().forEach(name -> jetStreamMap.put(name, ensureStreamExists(name)));
  }

  @VisibleForTesting
  Connection connect(Options options) {
    try {
      return Nats.connect(options);
    } catch (IOException e) {
      throw new RuntimeException("Failed to connect to NATS", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  private JetStream ensureStreamExists(String streamName) {
    try {
      log.info("Ensuring stream with name {} exists", streamName);
      JetStreamManagement jsm = natsConnection.jetStreamManagement();
      StreamConfiguration sc =
          StreamConfiguration.builder()
              .name(streamName)
              .subjects(streamName + ".>")
              .retentionPolicy(RetentionPolicy.WorkQueue)
              .maxMessages(MAX_STREAM_MESSAGES)
              .maxBytes(MAX_STREAM_BYTES)
              .discardPolicy(DiscardPolicy.Old)
              .replicas(maxMessageReplicas)
              .storageType(StorageType.File)
              .build();

      if (!jsm.getStreamNames().contains(streamName)) {
        // Stream doesn't exist, create it
        log.info("Creating JetStream stream {}", streamName);
        jsm.addStream(sc);
      } else {
        // The stream exists, but may be outdated.  Validate the config.
        log.info("JetStream stream {} already exists", streamName);
        Optional<StreamInfo> streamInfo =
            jsm.getStreams().stream()
                .filter(stream -> stream.getConfiguration().getName().equals(streamName))
                .findFirst();
        final var existing = streamInfo.map(StreamInfo::getConfiguration).orElse(null);
        if (existing == null
            || existing.getRetentionPolicy() != sc.getRetentionPolicy()
            || existing.getMaxBytes() != sc.getMaxBytes()
            || existing.getMaxMsgs() != sc.getMaxMsgs()
            || existing.getDiscardPolicy() != sc.getDiscardPolicy()
            || existing.getReplicas() != sc.getReplicas()
            || existing.getStorageType() != sc.getStorageType()) {
          jsm.deleteStream(streamName);
          jsm.addStream(sc);
        }
        jetStreamMap.put(streamName, natsConnection.jetStream());
      }

      return natsConnection.jetStream();
    } catch (IOException | JetStreamApiException e) {
      log.error(e.getMessage(), e);
      throw new ModelDBException(e);
    }
  }

  public void subscribeToStream(
      String streamName,
      String subject,
      MessageHandler handler,
      String durableName,
      String deliverGroupName)
      throws StreamNotFoundException {
    natsConnection = getConnection();
    Dispatcher dispatcher = natsConnection.createDispatcher();
    try {
      log.info("Subscribing to JetStream stream {}", streamName);
      JetStream jetStream = natsConnection.jetStream();
      PushSubscribeOptions options =
          PushSubscribeOptions.builder().stream(streamName)
              .durable(durableName)
              .deliverGroup(deliverGroupName)
              .build();
      jetStream.subscribe(subject, dispatcher, handler, false, options);
    } catch (JetStreamApiException e) {
      // todo: is there a better way to detect this?
      // TODO: don't close connection if there is a failure, since we still might need the
      //   connection elsewhere?
      if (e.getMessage().toLowerCase().contains("stream not found")) {
        closeConnection(natsConnection);
        throw new StreamNotFoundException();
      }
      // close the connection to shut down any background reconnect threads that NATS spawned
      closeConnection(natsConnection);
      throw new RuntimeException("Error subscribing to stream.", e);
    } catch (Exception e) {
      // close the connection to shut down any background reconnect threads that NATS spawned
      closeConnection(natsConnection);
      throw new RuntimeException("Error subscribing to stream.", e);
    }
  }

  public static void closeConnection(Connection connection) {
    try {
      connection.close();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  public static class StreamNotFoundException extends Exception {}

  private static class NatsErrorListener implements ErrorListener {
    @Override
    public void errorOccurred(Connection conn, String error) {
      log.debug("The nats server notified with error: {} ", error);
    }

    @Override
    public void exceptionOccurred(Connection conn, Exception exp) {
      log.debug("The nats connection handled an exception: {} ", exp.getLocalizedMessage());
    }

    @Override
    public void slowConsumerDetected(Connection conn, Consumer consumer) {
      log.info("A slow consumer was detected for nats");
      log.info("A slow consumer dropped messages: {} ", consumer.getDroppedCount());
    }
  }
}
