package com.fasterxml.jackson.module.scala
package introspect

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.fasterxml.jackson.module.scala.deser.ScalaValueInstantiatorsModule

object TestPropertyNaming {
  class ConstructorBean(val x: Int)
}

@RunWith(classOf[JUnitRunner])
class TestPropertyNaming extends BaseSpec {
  import TestPropertyNaming._

  def mapper = {
    val m = new ObjectMapper()
    m.registerModule(new JacksonModule with ScalaValueInstantiatorsModule)
    m
  }

  "ObjectMapper" should "recognize a simple constructor" in {
    val bean = mapper.readValue("""{"x" : 42 }""", classOf[ConstructorBean])
    bean.x should be (42)
  }

}
