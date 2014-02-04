package com.fasterxml.jackson.module.scala.legacy.deser

import org.scalatest.{Succeeded, Outcome, fixture}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

class DeserializationFixture extends fixture.FlatSpec {

  type FixtureParam = ObjectMapper with ScalaObjectMapper

  def withFixture(test: OneArgTest): Outcome =
  {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    test(mapper)
    Succeeded
  }

}
