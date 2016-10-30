import org.scalatest.FunSuite

class SampleTest extends FunSuite {

  test("An empty Set should have size 0") {
    assert(Set.empty.isEmpty)
  }

  test("Invoking head on an empty Set should produce NoSuchElementException") {
    assertThrows[NoSuchElementException] {
      Set.empty.head
    }
  }
}