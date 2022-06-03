package ai.verta.modeldb.common.configuration;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.jmx.BuildInfoCollector;
import io.prometheus.jmx.JmxCollector;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MalformedObjectNameException;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RegisteredMetricsBeans {

  // Export all JMX metrics to Prometheus
  private static final String JMX_RULES = "---\n" + "rules:\n" + "  - pattern: \".*\"";
  private static final AtomicBoolean metricsInitialized = new AtomicBoolean(false);

  @Bean
  public ServletRegistrationBean<MetricsServlet> servletRegistrationBean()
      throws MalformedObjectNameException {
    if (!metricsInitialized.getAndSet(true)) {
      DefaultExports.initialize();
      new BuildInfoCollector().register();
      new JmxCollector(JMX_RULES).register();
    }
    return new ServletRegistrationBean<>(new MetricsServlet(), "/metrics");
  }
}
