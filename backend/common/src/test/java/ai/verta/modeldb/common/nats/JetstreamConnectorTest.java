package ai.verta.modeldb.common.nats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.nats.client.*;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.log4j.Log4j2;
import np.com.madanpokharel.embed.nats.EmbeddedNatsConfig;
import np.com.madanpokharel.embed.nats.EmbeddedNatsServer;
import np.com.madanpokharel.embed.nats.NatsServerConfig;
import np.com.madanpokharel.embed.nats.NatsVersion;
import org.junit.jupiter.api.*;

@Log4j2
class JetstreamConnectorTest {
  public static EmbeddedNatsServer embeddedNatsServer;
  private JetstreamConnector connector;

  @BeforeAll
  static void beforeAll() {
    EmbeddedNatsConfig natsConfig;
    try {
      natsConfig =
          new EmbeddedNatsConfig.Builder()
              .withNatsServerConfig(
                  new NatsServerConfig.Builder()
                      .withEnableJetStream(
                          Files.createTempDirectory("jetstream").toAbsolutePath().toString())
                      .withConfigParam("--no_advertise", "")
                      .withRandomPort()
                      .withHost("localhost")
                      .withNatsVersion(NatsVersion.LATEST)
                      .build())
              .build();
      embeddedNatsServer = new EmbeddedNatsServer(natsConfig);
      embeddedNatsServer.startServer();
      await().until(() -> embeddedNatsServer.isServerRunning());
      Runtime.getRuntime().addShutdownHook(new Thread(() -> embeddedNatsServer.stopServer()));
    } catch (Exception e) {
      log.error("Could not start embedded NATS server", e);
      throw new RuntimeException(e);
    }
  }

  @BeforeEach
  void setUp() {
    JetstreamConfig config =
        new JetstreamConfig(
            true, embeddedNatsServer.getRunningHost(), embeddedNatsServer.getRunningPort(), 1);
    connector = new JetstreamConnector(config, () -> List.of("cheese", "wine"));
  }

  @AfterEach
  void tearDown() {
    if (connector != null) {
      connector.close();
    }
  }

  @AfterAll
  static void afterAll() {
    if (embeddedNatsServer != null) {
      embeddedNatsServer.stopServer();
    }
  }

  @Test
  void publishAndSubscribe() throws Exception {
    AtomicReference<String> message = new AtomicReference<>();
    var subscription =
        connector.subscribeToStream(
            "cheese",
            "cheese.>",
            msg -> {
              msg.ack();
              message.set(new String(msg.getData()));
            },
            "testDurable",
            "testGroup");
    assertThat(subscription).isNotNull();
    JetStream cheeseStream = connector.getJetStream("cheese");
    cheeseStream.publish("cheese.create", "Swiss".getBytes(StandardCharsets.UTF_8));

    await().untilAsserted(() -> assertThat(message.get()).isEqualTo("Swiss"));
  }

  @Test
  void existingConsumer() throws Exception {
    connector.subscribeToStream("cheese", "cheese.>", Message::ack, "testDurable", "testGroup");

    Optional<ConsumerInfo> consumerInfo =
        connector.existingConsumer(
            "cheese",
            ConsumerConfiguration.builder()
                .deliverGroup("testGroup")
                .durable("testDurable")
                .build());
    assertThat(consumerInfo)
        .isPresent()
        .get()
        .satisfies(
            info -> {
              assertThat(info.getConsumerConfiguration().getDurable()).isEqualTo("testDurable");
              assertThat(info.getConsumerConfiguration().getDeliverGroup()).isEqualTo("testGroup");
            });

    Optional<ConsumerInfo> missingConsumer =
        connector.existingConsumer(
            "wine",
            ConsumerConfiguration.builder()
                .deliverGroup("testGroup")
                .durable("testDurable")
                .build());
    assertThat(missingConsumer).isEmpty();
  }

  @Test
  void existingConsumer_changeFromPullToPush() throws Exception {
    JetstreamConfig config =
        new JetstreamConfig(
            true, embeddedNatsServer.getRunningHost(), embeddedNatsServer.getRunningPort(), 1);
    JetstreamConnector connector = new JetstreamConnector(config, () -> List.of("cheese", "wine"));
    connector.verifyStreams();
    ConsumerConfiguration expectedConfiguration =
        ConsumerConfiguration.builder().deliverGroup("testGroup").durable("testDurable").build();

    // no wine consumer yet at all
    assertThat(connector.existingConsumer("wine", expectedConfiguration)).isEmpty();

    try (Connection connection = Nats.connect(embeddedNatsServer.getNatsUrl())) {
      // create a pull consumer that we'll later replace
      JetStreamSubscription wine =
          connection
              .jetStream()
              .subscribe(
                  "wine.>",
                  PullSubscribeOptions.builder().durable("testDurable").stream("wine").build());
      // make a call to verify that things are working with a pull consumer.
      wine.fetch(1, Duration.ofMillis(100));
    }

    // the implementation deletes the consumer if it's the wrong type (pull vs. push)
    assertThat(connector.existingConsumer("wine", expectedConfiguration)).isEmpty();

    // now re-subscribe
    var subscription =
        connector.subscribeToStream("wine", "wine.>", Message::ack, "testDurable", "testGroup");
    assertThat(subscription).isNotNull();
    // now it should exist
    assertThat(connector.existingConsumer("wine", expectedConfiguration)).isPresent();
  }
}
