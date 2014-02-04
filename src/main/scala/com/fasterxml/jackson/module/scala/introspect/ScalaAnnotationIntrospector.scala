package com.fasterxml.jackson.module.scala
package introspect

import com.fasterxml.jackson.databind.introspect.{AnnotatedClass, Annotated, NopAnnotationIntrospector}

import scala.reflect.runtime.{universe => ru}
import com.fasterxml.jackson.databind.deser.ValueInstantiator

class ScalaAnnotationIntrospector extends NopAnnotationIntrospector {

//  override def hasCreatorAnnotation(a: Annotated): Boolean = {
//
//    val raw = a.getRawType
//    val mirror = ru.runtimeMirror(raw.getClassLoader)
//    val methodSym = mirror.classSymbol(raw).toType.declaration(ru.nme.CONSTRUCTOR).asMethod
//    val primaryCtor: ru.MethodSymbolApi = if (methodSym.isOverloaded) {
//      methodSym.alternatives.head.asMethod
//    } else methodSym
//    assert(methodSym.isPrimaryConstructor)
//
//  }

//  override def findValueInstantiator(ac: AnnotatedClass): AnyRef = {
//
//  }
}
