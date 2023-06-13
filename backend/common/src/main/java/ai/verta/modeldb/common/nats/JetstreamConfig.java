package ai.verta.modeldb.common.nats;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter(AccessLevel.PRIVATE)
@With
public class JetstreamConfig {
  private boolean enabled = false;
  private String host = "";
  private int port = 0;
  private int maxMessageReplicas;
}
