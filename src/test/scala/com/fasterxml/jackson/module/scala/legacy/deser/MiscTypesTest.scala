package com.fasterxml.jackson.module.scala.legacy.deser

import org.scalatest.{Matchers, FlatSpecLike}
import java.util.UUID
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.fasterxml.jackson.module.scala.DefaultScalaModule

@RunWith(classOf[JUnitRunner])
class MiscTypesTest extends DeserializerTest with FlatSpecLike with Matchers {

  def module = DefaultScalaModule

  "Scala Module" should "deserialize UUID" in {
    val data: Seq[UUID] = Stream.continually(UUID.randomUUID).take(4).toList

    val json = mapper.writeValueAsString(data)
    val read = deserialize[List[UUID]](json)

    read should be === (data)
  }

}
