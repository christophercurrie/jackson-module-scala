package com.fasterxml.jackson.module.scala.legacy.deser

import org.scalatest.matchers.ShouldMatchers

class IterableDeserializerTest extends DeserializationFixture with ShouldMatchers {

  // Testing values
  val listJson =  "[1,2,3,4,5,6]"
  val listScala = 1 to 6

  behavior of "ObjectMapper"

  it should "deserialize a list to an Iterable" in { f =>

    val result = f.readValue[Iterable[Int]](listJson)
    result should equal (listScala)

  }

}
