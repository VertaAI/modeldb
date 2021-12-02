package ai.verta.modeldb.common;

import java.util.LinkedList;
import java.util.List;

public class EnumerateList<T> {
  private final LinkedList<Item<T>> lst;

  public static class Item<T> {
    private final int index;
    private final T value;

    private Item(int index, T value) {
      this.index = index;
      this.value = value;
    }

    public int getIndex() {
      return index;
    }

    public T getValue() {
      return value;
    }
  }

  public EnumerateList(List<T> other) {
    final var iterator = other.listIterator();
    lst = new LinkedList<>();
    while (iterator.hasNext()) {
      lst.add(new Item<>(iterator.nextIndex(), iterator.next()));
    }
  }

  public List<Item<T>> getList() {
    return lst;
  }
}
