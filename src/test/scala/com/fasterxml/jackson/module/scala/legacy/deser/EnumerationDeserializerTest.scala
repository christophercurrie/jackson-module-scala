package com.fasterxml.jackson.module.scala
package legacy
package deser

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{Matchers, FlatSpecLike, FlatSpec}
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import com.fasterxml.jackson.module.scala.legacy.Weekday
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

class EnumContainer {

	var day = Weekday.Fri
}


class WeekdayType extends TypeReference[Weekday.type]
case class AnnotatedEnumHolder(@JsonScalaEnumeration(classOf[WeekdayType]) weekday: Weekday.Weekday)

class EnumMapHolder {

  @JsonScalaEnumeration(classOf[WeekdayType])
  var weekdayMap: Map[Weekday.Value, String] = Map.empty

}


@RunWith(classOf[JUnitRunner])
class EnumerationDeserializerTest extends DeserializerTest with FlatSpecLike with Matchers {

  lazy val module = DefaultScalaModule

	"An ObjectMapper with EnumDeserializerModule" should "deserialize a value into a scala Enumeration as a bean property" in {
		val expectedDay = Weekday.Fri
    val result = deserialize[EnumContainer](fridayEnumJson)
    result.day should be (expectedDay)
	}

  it should "deserialize an annotated Enumeration value" in {
    val result = deserialize[AnnotatedEnumHolder](annotatedFridayJson)
    result.weekday should be (Weekday.Fri)
  }

  it should "deserialize an annotated Enumeration as a key" in {
    val result = deserialize[EnumMapHolder](weekdayMapJson)
    result.weekdayMap should contain key (Weekday.Mon)
  }

	val fridayEnumJson = """{"day": {"enumClass":"com.fasterxml.jackson.module.scala.Weekday","value":"Fri"}}"""

  val annotatedFridayJson = """{"weekday":"Fri"}"""

  val weekdayMapJson = """{"weekdayMap":{"Mon":"Boo","Fri":"Hooray!"}}"""


}