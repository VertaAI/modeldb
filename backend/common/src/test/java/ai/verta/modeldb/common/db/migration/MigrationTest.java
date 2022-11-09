package ai.verta.modeldb.common.db.migration;

import org.junit.jupiter.api.Test;

import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationTest {
  @Test
  void ordering() {
    Migration one = new Migration("1_testOne.up.sql");
    Migration two = new Migration("2_testTwo.up.sql");
    Migration twoDown = new Migration("2_testTwo.down.sql");
    Migration ten = new Migration("10_testTen.up.sql");

    TreeSet<Migration> orderedSet = new TreeSet<>();
    orderedSet.add(twoDown);
    orderedSet.add(ten);
    orderedSet.add(one);
    orderedSet.add(two);

    assertThat(orderedSet).containsExactly(one, two, twoDown, ten);
  }

  @Test
  void upAndDown() {
    Migration oneUp = new Migration("1_testOne.up.sql");
    Migration twoDown = new Migration("2_testTwo.down.sql");
    Migration tenUp = new Migration("10_testTen.up.sql");
    assertThat(oneUp.isUp()).isTrue();
    assertThat(oneUp.isDown()).isFalse();
    assertThat(tenUp.isUp()).isTrue();
    assertThat(tenUp.isDown()).isFalse();
    assertThat(twoDown.isUp()).isFalse();
    assertThat(twoDown.isDown()).isTrue();
  }
}
