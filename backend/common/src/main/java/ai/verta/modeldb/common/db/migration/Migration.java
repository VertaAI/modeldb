package ai.verta.modeldb.common.db.migration;

import lombok.Value;

@Value
public class Migration implements Comparable<Migration> {
  String filename;

  @Override
  public int compareTo(Migration o) {
    int versionDiff = this.getNumber() - o.getNumber();
    if (versionDiff == 0) {
      if (this.isUp() && o.isUp()) {
        return 0;
      }
      if (this.isDown()) {
        return 1;
      }
      return -1;
    }
    return versionDiff;
  }

  int getNumber() {
    String[] pieces = filename.split("_");
    return Integer.parseInt(pieces[0]);
  }

  /**
   * An "up" migration could also be called the "forward" migration. It is the migration that
   * applies a change to an existing database.
   */
  boolean isUp() {
    String[] pieces = filename.split("\\.");
    return "up".equals(pieces[1]);
  }

  /**
   * A "down" migration could also be called the "reverse" migration. It is the migration that
   * reverts the change from the corresponding "up" migration.
   */
  boolean isDown() {
    String[] pieces = filename.split("\\.");
    return "down".equals(pieces[1]);
  }
}
