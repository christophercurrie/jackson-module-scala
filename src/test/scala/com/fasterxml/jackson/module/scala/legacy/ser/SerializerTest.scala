package com.fasterxml.jackson.module.scala.legacy.ser

import java.io.StringWriter
import com.fasterxml.jackson.module.scala.legacy.JacksonTest

/**
 * Undocumented class.
 */

trait SerializerTest extends JacksonTest {

  def serialize(value: Any): String = {
    val writer = new StringWriter()
    mapper.writeValue(writer, value)
    writer.toString
  }

}