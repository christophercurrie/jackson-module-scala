package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator
import com.fasterxml.jackson.databind.deser.{SettableBeanProperty, CreatorProperty, ValueInstantiator, ValueInstantiators}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.JacksonModule
import scala.collection.JavaConverters._
import scala.reflect.runtime.{universe => ru}
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams

private class ScalaValueInstantiator(config: DeserializationConfig, beanDesc: BeanDescription, creator: ru.MethodSymbol)
  extends StdValueInstantiator(config, beanDesc.getType)
{
  val raw = beanDesc.getBeanClass
  val cl = raw.getClassLoader
  val mirror = ru.runtimeMirror(cl)
  val overloads: Iterable[AnnotatedWithParams] = if (creator.isConstructor) {
    beanDesc.getConstructors.asScala.view
  } else {
    beanDesc.getClassInfo.memberMethods().asScala.view.filter(_.getName == creator.fullName)
  }

  val method: Option[AnnotatedWithParams] =
    // This is rudimentary and will fail on type conversions
     overloads.find(_.getParameterCount == creator.paramss.head.size)
  val annotated = method.get

  val props: Array[SettableBeanProperty] = {
    val propList = for {
      (sym, idx) <- creator.paramss.head.zipWithIndex
      name = sym.name.encoded
      tpe = sym.asTerm.typeSignature
      cls = mirror.runtimeClass(tpe)
      param = annotated.getParameter(idx)
    } yield {
      new CreatorProperty(
        new PropertyName(name),
        config.getTypeFactory.constructType(cls, raw),
        null, null, null,
        param, idx, null,
        PropertyMetadata.construct(true, null)
      )
    }
    propList.toArray
  }

  override def canCreateFromObjectWith = true

  override def getFromObjectArguments(config: DeserializationConfig): Array[SettableBeanProperty] = props

  override def createFromObjectWith(ctxt: DeserializationContext, args: Array[AnyRef]) = {
    val classSymbol = creator.owner.asClass
    val classMirror = mirror.reflectClass(classSymbol)
    val methodMirror = classMirror.reflectConstructor(creator)
    try {
      val o = methodMirror(args: _*)
      o.asInstanceOf[AnyRef]
    }
    catch {
      case e: Exception => throw ctxt.instantiationException(raw, e)
      case e: Error => throw ctxt.instantiationException(raw, e)
    }
  }

}

private object ScalaValueInstantiators extends ValueInstantiators.Base {

  private def toSymbol(beanDesc: BeanDescription) = {
    val raw = beanDesc.getBeanClass
    ru.runtimeMirror(raw.getClassLoader).classSymbol(raw)
  }

  override def findValueInstantiator(config: DeserializationConfig,
                                     beanDesc: BeanDescription,
                                     defaultInstantiator: ValueInstantiator) = {
    val sym = toSymbol(beanDesc)
    if (sym.isJava || sym.isPrimitive)
      defaultInstantiator
    else {
      if (sym.companionSymbol != ru.NoSymbol) {
        val companionClass = beanDesc.getBeanClass.getClassLoader.loadClass(sym.companionSymbol.asModule.moduleClass.asClass.name.encodedName.toString)

      }

      new ScalaValueInstantiator(config, beanDesc, sym.toType.members.find(m => m.isMethod && m.asMethod.isPrimaryConstructor).head.asMethod)
    }
  }

}

trait ScalaValueInstantiatorsModule extends JacksonModule {
  this += { _.addValueInstantiators(ScalaValueInstantiators) }
}