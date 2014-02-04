package com.fasterxml.jackson.module.scala.legacy.ser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{Succeeded, fixture}
import com.fasterxml.jackson.databind.{PropertyNamingStrategy, ObjectMapper}
import com.fasterxml.jackson.core.ObjectCodec
import java.io.ByteArrayOutputStream
import com.google.common.base.Optional
import scala.reflect.BeanProperty
import javax.annotation.Nonnull
import com.fasterxml.jackson.module.scala.DefaultScalaModule

class PojoWrittenInScala {
  @Nonnull @BeanProperty var optFoo: Optional[String] = Optional.absent()
  @Nonnull @BeanProperty var bar: Int = 0
}

@RunWith(classOf[JUnitRunner])
class NamingStrategyTest extends fixture.FlatSpec with ShouldMatchers {

  type FixtureParam = ObjectMapper

  protected def withFixture(test: OneArgTest) = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
    test(mapper)
    Succeeded
  }

  "DefaultScalaModule" should "correctly handle naming strategies" in { mapper =>
    val bytes = new ByteArrayOutputStream()
    mapper.writeValue(bytes, new PojoWrittenInScala)
  }

}
