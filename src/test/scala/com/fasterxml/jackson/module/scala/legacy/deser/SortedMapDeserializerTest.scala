package com.fasterxml.jackson.module.scala.legacy.deser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, FlatSpecLike, FlatSpec}
import org.scalatest.matchers.ShouldMatchers
import scala.collection.SortedMap
import scala.collection.immutable.TreeMap
import com.fasterxml.jackson.module.scala.deser.SortedMapDeserializerModule

/**
 * @author Christopher Currie <ccurrie@impresys.com>
 */
@RunWith(classOf[JUnitRunner])
class SortedMapDeserializerTest extends DeserializerTest with FlatSpecLike with Matchers {

  lazy val module = new SortedMapDeserializerModule {}

  "An ObjectMapper with the SortedMapDeserializerModule" should "deserialize an object into a SortedMap" in {
    val result = deserialize[SortedMap[String,String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into an TreeMap" in {
    val result = deserialize[TreeMap[String,String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object with variable value types into a variable UnsortedMap" in {
    val result = deserialize[SortedMap[String,Any]](variantMapJson)
    result should equal (variantMapScala)
  }

  it should "deserialize an object with numeric keys into a SortedMap" in {
    // NB: This is `java.lang.Integer` because of GH-104
    val result = deserialize[SortedMap[Integer,String]](numericMapJson)
    result should equal (numericMapScala)
  }

  val mapJson =  """{ "one": "1", "two": "2" }"""
  val mapScala = SortedMap("one"->"1","two"->"2")
  val variantMapJson = """{ "one": "1", "two": 2 }"""
  val variantMapScala = SortedMap[String,Any]("one"->"1","two"->2)
  val numericMapJson = """{ "1": "one", "2": "two" }"""
  val numericMapScala = SortedMap[Integer,String](Integer.valueOf(1)->"one",Integer.valueOf(2)->"two")
}