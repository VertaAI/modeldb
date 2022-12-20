package ai.verta.modeldb.common.db.migration;

import static org.assertj.core.api.Assertions.assertThat;

import ai.verta.modeldb.common.generators.SetGenerator;
import ai.verta.modeldb.common.generators.StringGenerator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Pair;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import lombok.Value;
import org.junit.runner.RunWith;

@RunWith(JUnitQuickcheck.class)
public class MigrationPropertyTest {
  @Property
  public void ordering(@From(MigrationGenerator.class) Migrations migrations) {
    var ordered = migrations.getMigrations();
    Migration previous = null;
    for (Migration migration : ordered) {
      if (previous != null) {
        if (migration.isUp()) {
          assertThat(migration.getNumber()).isGreaterThan(previous.getNumber());
        } else {
          assertThat(migration.getNumber()).isEqualTo(previous.getNumber());
          assertThat(migration.isDown()).isTrue();
          Migration finalPrevious = previous;
          assertThat(previous.isUp())
              .withFailMessage(() -> "previous: " + finalPrevious + "\ncurrent: " + migration)
              .isTrue();
        }
      }
      previous = migration;
    }
  }

  @Value
  public static class Migrations {
    Set<Migration> migrations;
  }

  public static class MigrationGenerator extends Generator<Migrations> {
    private final Set<Integer> numbersUsed = new HashSet<>();

    public MigrationGenerator() {
      super(Migrations.class);
    }

    @Override
    public Migrations generate(SourceOfRandomness random, GenerationStatus status) {
      // we assume:
      // a) all migrations come in up/down pairs
      // b) there are no duplicate migration numbers
      Set<Migration> migrations =
          SetGenerator.generateByPairs(
              10,
              200,
              () -> {
                int number = pickUnusedNumber(random);
                String migrationName = StringGenerator.generateFilenamePart(4, 100, random, status);
                return new Pair<>(
                    new Migration(number + "_" + migrationName + ".down.sql"),
                    new Migration(number + "_" + migrationName + ".up.sql"));
              },
              random,
              status);
      return new Migrations(new TreeSet<>(migrations));
    }

    private int pickUnusedNumber(SourceOfRandomness random) {
      int tries = 0;
      while (true) {
        int number = random.nextInt(1, 20000);
        if (numbersUsed.add(number)) {
          return number;
        }
        if (tries++ > 100) {
          throw new IllegalStateException("failed to find an unused integer");
        }
      }
    }
  }
}
