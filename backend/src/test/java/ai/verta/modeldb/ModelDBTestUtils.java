package ai.verta.modeldb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModelDBTestUtils {

  private static final Logger LOGGER = LogManager.getLogger(ModelDBTestUtils.class);

  public static String readFile(String filePath) throws IOException {
    LOGGER.info("Start reading file : " + filePath);
    StringBuilder contentBuilder = new StringBuilder();
    try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
      stream.forEach(s -> contentBuilder.append(s).append("\n"));
    } catch (IOException e) {
      LOGGER.warn(e.getMessage(), e);
      throw e;
    }
    LOGGER.info("Stop reading file");
    return contentBuilder.toString();
  }
}
