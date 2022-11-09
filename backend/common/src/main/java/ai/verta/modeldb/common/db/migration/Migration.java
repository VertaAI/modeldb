package ai.verta.modeldb.common.db.migration;

import lombok.Value;

@Value
class Migration implements Comparable<Migration> {
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

  boolean isUp() {
    String[] pieces = filename.split("\\.");
    return "up".equals(pieces[1]);
  }

  boolean isDown() {
    String[] pieces = filename.split("\\.");
    return "down".equals(pieces[1]);
  }
}
