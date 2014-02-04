import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.legacy.ser.SerializerTest
import org.junit.runner.RunWith
import org.scalatest.{Matchers, FlatSpecLike, FlatSpec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers

case class UnpackagedCaseClass(intValue: Int, stringValue: String)

@RunWith(classOf[JUnitRunner])
class TestUnpackagedClass extends SerializerTest with FlatSpecLike with Matchers {

  def module = DefaultScalaModule

  behavior of "DefaultScalaModule"

  it should "serialize a case class not in a package" in {
    val result = serialize(UnpackagedCaseClass(1, "foo"))
    result should be ("""{"intValue":1,"stringValue":"foo"}""")
  }

}
