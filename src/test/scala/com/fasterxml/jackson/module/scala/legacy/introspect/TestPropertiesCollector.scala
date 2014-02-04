package com.fasterxml.jackson.module.scala
package legacy
package introspect

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.fixture.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.{Module, ObjectMapper}
import com.fasterxml.jackson.databind.Module.SetupContext
import com.fasterxml.jackson.annotation.JsonProperty
import scala.volatile
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import scala.reflect.{BeanProperty, BeanInfo}
import org.scalatest.Succeeded
import com.fasterxml.jackson.module.scala.deser.ScalaValueInstantiatorsModule
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.introspect.ScalaClassIntrospectorModule

class Fields {
  @JsonProperty // make it "visible" for Jackson
  private [this] // make it a "field" property for ScalaBeans
  val field: Int = 0
}

class Methods {
  val getter = 0

  var varGetterSetter = 0

  def defGetterSetter = 0
  def defGetterSetter_=(i: Int) { }
}

case class Constructors(plainField: Int = 0)

@SerialVersionUID(uid = 8675309)
case class SerialID(firstField: String, secondField: Int) {
  @transient var excluded = 10
  @volatile var alsoExcluded = "no"
}

@BeanInfo
class SearchCriteria {
  @BeanProperty var mapZoom:Int = 4
}

@RunWith(classOf[JUnitRunner])
class TestPropertiesCollector extends FlatSpec with ShouldMatchers {

  type FixtureParam = ObjectMapper

  def withFixture(test: OneArgTest) = {

    val mapper = new ObjectMapper()
    mapper.registerModule(new JacksonModule with ScalaClassIntrospectorModule with ScalaValueInstantiatorsModule)

    test(mapper)
    Succeeded
  }

  behavior of "ScalaPropertiesCollector"

  // Make sure we don't break Java classes. This is done in ScalaClassIntrospector
  // by deferring to the base class if we're not actually operating on Scala generated classes.
  it should "detect java field properties" in { mapper: FixtureParam =>
    mapper.writeValueAsString(new JavaFields) should be ("""{"string":"string"}""")
  }

  it should "detect java method properties" in { mapper: FixtureParam =>
    mapper.writeValueAsString(new JavaMethods) should be ("""{"stuff":"stuff"}""")
  }


  // Now do basic checks on Scala classes
  it should "detect field properties" in { mapper: FixtureParam =>
    mapper.writeValueAsString(new Fields) should be ("""{"field":0}""")
  }

  it should "detect method properties" in { mapper: FixtureParam =>
    mapper.writeValueAsString(new Methods) should be ("""{"getter":0,"varGetterSetter":0,"defGetterSetter":0}""")
  }

  it should "detect constructor properties" in { mapper: FixtureParam =>
    mapper.readValue("""{"plainField":-1}""", classOf[Constructors]) should be (Constructors(-1))
  }

  it should "not serialize static, volatile or transient fields" in { mapper: FixtureParam =>
    mapper.writeValueAsString(SerialID("Hi", 0)) should be ("""{"firstField":"Hi","secondField":0}""")
  }

  it should "not deserialize static, volatile or transient fields" in { mapper: FixtureParam =>
    evaluating {
      mapper.readValue("""{"firstField":"Hi","secondField":0,"excluded":15}""", classOf[SerialID])
    } should produce[UnrecognizedPropertyException]
  }

  it should "deduplicate @BeanInfo properties" in { mapper: FixtureParam =>
    val result = mapper.readValue("""{"mapZoom":0}""", classOf[SearchCriteria])
    result.mapZoom should be === 0
  }
}
