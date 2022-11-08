package ai.verta.modeldb.common.db.migration;

import lombok.Value;

@Value
public class Migration implements Comparable<Migration> {
  String filename;

  @Override
  public int compareTo(Migration o) {
    return this.getNumber() - o.getNumber();
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
