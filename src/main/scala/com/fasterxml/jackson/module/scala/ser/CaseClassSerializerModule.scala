package com.fasterxml.jackson.module.scala.ser

import collection.JavaConverters._

import java.{util => ju}


import com.fasterxml.jackson.annotation.{JsonInclude, JsonIgnore}
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.databind.{BeanDescription, PropertyName, SerializationConfig}
import com.fasterxml.jackson.databind.ser.{BeanPropertyWriter, BeanSerializerModifier}
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition
import com.fasterxml.jackson.module.scala.JacksonModule
import scala.reflect.{ClassTag, NameTransformer}
import com.fasterxml.jackson.module.scala.reflect.{BeanProperty, BeanMirror}
import java.lang.annotation.Annotation

import scala.language.implicitConversions
import scala.language.reflectiveCalls

private object CaseClassBeanSerializerModifier extends BeanSerializerModifier {
  override def changeProperties(config: SerializationConfig,
                                beanDesc: BeanDescription,
                                beanProperties: ju.List[BeanPropertyWriter]): ju.List[BeanPropertyWriter] = {
    val jacksonIntrospector = config.getAnnotationIntrospector
    val defaultInclusion = beanDesc.findSerializationInclusion(config.getSerializationInclusion)

    implicit def finder(prop: BeanProperty) = {
      val method = beanDesc.findMethod(prop.name, Array())
      val param = prop.constructorParameterIndex.map { cpi =>
        beanDesc.getConstructors.get(0).getParameter(cpi)
      }
      new {
        def findAnnotation[A <: Annotation](implicit m: ClassTag[A]): Option[A] = {
          val cls = m.runtimeClass.asInstanceOf[Class[A]]
          (method :: param.toList).filter(_.hasAnnotation(cls)).map(_.getAnnotation(cls)).headOption
        }
      }
    }

    val list = try {
      val mirror = BeanMirror(beanDesc.getBeanClass)
      for {
        prop <- mirror.readableProperties.values if (prop.findAnnotation[JsonIgnore].map(!_.value).getOrElse(true))
        // Not completely happy with this test. I'd rather check the PropertyDescription
        // to see if it's a field or a method, but ScalaBeans doesn't expose that as yet.
        // I'm not sure if it truly matters as Scala generates method accessors for fields.
        // This is also realy inefficient, as we're doing a find on each iteration of the loop.
        method <- Option(beanDesc.findMethod(prop.name, Array()))
      } yield {
        // Check for the JsonInclude annotation
        val suppressNulls = prop.findAnnotation[JsonInclude].getOrElse(defaultInclusion) == NON_NULL
        prop.constructorParameterIndex match {
          case Some(paramIndex) =>
            val param = beanDesc.getConstructors.get(0).getParameter(paramIndex)
            asWriter(config, beanDesc, method, suppressNulls, Option(jacksonIntrospector.findNameForDeserialization(param)))
          case None => asWriter(config, beanDesc, method, suppressNulls)
        }
      }
    } catch {
      case _: IllegalArgumentException => Nil
    }

    if (list.isEmpty) beanProperties else new ju.ArrayList[BeanPropertyWriter](list.toList.asJava)
  }

  private def asWriter(config: SerializationConfig, beanDesc: BeanDescription, member: AnnotatedMethod, suppressNulls: Boolean, primaryName: Option[PropertyName] = None) = {
    val javaType = config.getTypeFactory.constructType(member.getGenericType)
    val defaultName = NameTransformer.decode(member.getName)
    val name = maybeTranslateName(config, member, primaryName.map(_.toString).getOrElse(defaultName))
    val propDef = new SimpleBeanPropertyDefinition(member, name)
    new BeanPropertyWriter(propDef, member, null, javaType, null, null, null, suppressNulls, null)
  }

  private def maybeTranslateName(config: SerializationConfig, member: AnnotatedMethod, name: String) = {
    Option(config.getPropertyNamingStrategy).map(_.nameForGetterMethod(config, member, name)).getOrElse(name)
  }

}

trait CaseClassSerializerModule extends JacksonModule {
  this += { _.addBeanSerializerModifier(CaseClassBeanSerializerModifier) }
}