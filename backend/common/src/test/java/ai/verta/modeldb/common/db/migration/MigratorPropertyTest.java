package ai.verta.modeldb.common.db.migration;

import static org.assertj.core.api.Assertions.assertThat;

import ai.verta.modeldb.common.CommonDBUtil;
import ai.verta.modeldb.common.config.RdbConfig;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Pair;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(JUnitQuickcheck.class)
public class MigratorPropertyTest {
  private static final String INPUT_DIRECTORY = "migrations/testing/property";
  private Migrator migrator;
  private Connection connection;

  @Before
  public void setUp() throws Exception {
    RdbConfig rdbConfig = createH2Config();
    if (!rdbConfig.isH2()) {
      CommonDBUtil.createDBIfNotExists(rdbConfig);
    }
    connection = MigratorTest.buildStandardDbConnection(rdbConfig);
    migrator = new Migrator(connection, INPUT_DIRECTORY, rdbConfig);
  }

  private static RdbConfig createH2Config() {
    return RdbConfig.builder()
        .DBConnectionURL("jdbc:h2:mem:migrationPropertyTestDb")
        .RdbDriver("org.h2.Driver")
        .RdbDialect("org.hibernate.dialect.H2Dialect")
        .RdbDatabaseName("migrationPropertyTestDb")
        .RdbPassword("password")
        .RdbUsername("sa")
        .build();
  }

  // for running ad-hoc against mysql (todo: figure out how to get sqlserver test files)
  private static RdbConfig createMysqlConfig() {
    return RdbConfig.builder()
        .RdbUrl("jdbc:mysql://localhost:3306")
        .RdbDriver("org.mariadb.jdbc.Driver")
        .RdbDialect("org.hibernate.dialect.MySQL5Dialect")
        .RdbDatabaseName("migrationPropertyTestDb")
        .RdbUsername("root")
        .RdbPassword("MyN3wP4ssw0rd!")
        .sslEnabled(false)
        .build();
  }

  @Property
  public void application(@From(VersionGenerator.class) IntPair versions) throws Exception {
    Set<String> beforeTablesPresent = getAllTablesPresent();
    migrator.performMigration(versions.getStart());
    migrator.performMigration(versions.getEnd());

    List<Pair<Integer, Boolean>> migrationContents = getSchemaMigrationContents();
    assertThat(migrationContents).containsExactly(new Pair<>(versions.getEnd(), false));

    Set<String> tablesPresent =
        getAllTablesPresent().stream().map(String::toUpperCase).collect(Collectors.toSet());
    int finalVersion = versions.getEnd();
    String failureMessagePattern =
        "TEST_TABLE_%d : versions failed: "
            + versions
            + " in tablesPresent: "
            + tablesPresent
            + " tablesPresentBeforeRun: "
            + beforeTablesPresent;
    for (int i = 1; i <= finalVersion; i++) {
      // 100 & 101 are empty migrations
      if (i == 100 || i == 101) {
        continue;
      }
      assertThat(tablesPresent)
          .withFailMessage(String.format(failureMessagePattern, i))
          .contains("TEST_TABLE_" + i);
    }
    for (int i = 99; i > finalVersion; i--) {
      assertThat(tablesPresent)
          .withFailMessage(String.format(failureMessagePattern, i))
          .doesNotContain("TEST_TABLE_" + i);
    }
  }

  @NotNull
  private Set<String> getAllTablesPresent() throws SQLException {
    // note: this rigamarole is to make debugging issues simpler. It only returns the test tables,
    // in numeric order.
    Set<String> tablesPresent =
        new TreeSet<>(
            (o1, o2) -> {
              String o1Number = o1.replaceAll("TEST_TABLE_", "");
              String o2Number = o2.replaceAll("TEST_TABLE_", "");
              return Integer.parseInt(o1Number) - Integer.parseInt(o2Number);
            });
    try (ResultSet tables = connection.getMetaData().getTables(null, null, null, null)) {
      while (tables.next()) {
        String tableName = tables.getString("TABLE_NAME").toUpperCase();
        if (tableName.startsWith("TEST_TABLE_")) {
          tablesPresent.add(tableName);
        }
      }
    }
    return tablesPresent;
  }

  @NotNull
  private List<Pair<Integer, Boolean>> getSchemaMigrationContents() throws SQLException {
    List<Pair<Integer, Boolean>> migrationContents = new ArrayList<>();
    try (PreparedStatement ps =
        connection.prepareStatement("select version, dirty from schema_migrations")) {
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        migrationContents.add(new Pair<>(rs.getInt(1), rs.getBoolean(2)));
      }
    }
    return migrationContents;
  }

  @Value
  public static class IntPair {
    int start;
    int end;
  }

  public static class VersionGenerator extends Generator<IntPair> {

    public VersionGenerator() {
      super(IntPair.class);
    }

    @Override
    public IntPair generate(SourceOfRandomness random, GenerationStatus status) {
      int one = random.nextInt(1, 101);
      int two = random.nextInt(1, 101);
      return new IntPair(one, two);
    }
  }
}
