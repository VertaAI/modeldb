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
      if (this.isUp()) {
        return 1;
      }
      return -1;
    }
    return versionDiff;
  }

  public int getNumber() {
    String[] pieces = filename.split("_");
    return Integer.parseInt(pieces[0]);
  }

  public boolean isUp() {
    String[] pieces = filename.split("\\.");
    return "up".equals(pieces[1]);
  }

  public boolean isDown() {
    String[] pieces = filename.split("\\.");
    return "down".equals(pieces[1]);
  }
}
