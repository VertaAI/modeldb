package ai.verta.client.entities.utils

import ai.verta.client.entities.DummyArtifact

import org.scalatest.FunSuite
import org.scalatest.Assertions._


class TestValueType extends FunSuite {
  test("ListValueType should correctly infer type, and not accept wrong type") {
    val list: ValueType = List[ValueType](1, "abc", 4.5)
    val underlyingList = list.asList.get

    assert(underlyingList(0).asBigInt.get == BigInt(1))
    assert(underlyingList(1).asString.get == "abc")
    assert(underlyingList(2).asDouble.get == 4.5)

    val artifact = new DummyArtifact("hello")
    assertTypeError("val list: ValueType = List[ValueType](artifact)")
  }

  test("Converting ListValueType to GenericObject and back should not corrupt the values") {
    val list: ValueType = List[ValueType](1, "abc", 4.5)

    val convertedList = KVHandler.convertFromValueType(list, "conversion failed")
      .flatMap(obj => KVHandler.convertToValueType(obj, "conversion failed"))
      .get

    assert(list == convertedList)
  }
}
