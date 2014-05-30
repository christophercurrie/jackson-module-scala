package com.fasterxml.jackson.module.scala.introspect

import com.fasterxml.jackson.databind.introspect._
import com.fasterxml.jackson.module.scala.JacksonModule
import scala.reflect.runtime.{universe => ru}
import scala.Some
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyName

object ScalaAnnotationIntrospector extends NopAnnotationIntrospector
{
  // TODO: Replace with a precise matching of the primary constructor's parameter count and types
  private def isPrimaryConsructor(ac: AnnotatedConstructor): Boolean = {
    val cls = ac.getDeclaringClass
    val ctors = cls.getConstructors
    if (ctors.isEmpty) {
      throw new RuntimeException(cls.getCanonicalName + " has no ctors?!")
    }
    ac.getAnnotated == cls.getConstructors()(0)
  }

  // Certain name encodings aren't automatically handled.
  private val Specialized = """^(.*)\$.*\$sp$""".r
  private def decodedName(member: AnnotatedMember): String = {
    if (!member.getName.contains("$")) {
      return member.getName
    }
    val termName = ru.newTermName(member.getName)
    termName.decoded
    val cls = member.getDeclaringClass
    val canonicalName = cls.getCanonicalName
    if (canonicalName == null) {
      // this isn't a perfect solution, but for now it will suffice, since
      // most cases for this that we're not handling are methods we don't
      // actually care about anyway.
      return termName.decoded
    }
    val PrivateField = (canonicalName.replace(".", "\\$") + "\\$\\$(.*)").r
    termName.decoded match {
      case Specialized(prefix) => prefix
      case PrivateField(suffix) => suffix
      case name => name
    }
  }

  private def termFor(member: AnnotatedMember): ru.SymbolApi = {
    val cls = member.getDeclaringClass

    val mirror = ru.runtimeMirror(cls.getClassLoader)
    val clsSym = mirror.classSymbol(cls).asClass
    val clsTpe = clsSym.toType
    val name = decodedName(member)
    val termName = ru.newTermName(name)

    member match {
      case ac: AnnotatedConstructor =>
        // The only reliable way of matching constructors is by types
        val paramTypes = for(i <- 0 until ac.getParameterCount) yield {
          val cls = ac.getRawParameterType(i)
          val m = ru.runtimeMirror(cls.getClassLoader)
          m.classSymbol(cls).asClass.toType
        }

        val ctors = for {
          meth <- clsTpe.declarations if meth.isMethod
          ctor = meth.asMethod if ctor.isConstructor
        } yield ctor

        ctors.find { ctor =>
          val paramSymbols = ctor.paramss.flatten
          if (paramSymbols.length == paramTypes.length)
            paramSymbols.map(_.typeSignature).zip(paramTypes).forall { case (t1, t2) => t1 =:= t2 }
          else
            false
        }.get
      case ap: AnnotatedParameter =>
        val meth = termFor(ap.getOwner).asMethod
        val params = meth.paramss.view.flatten.toIndexedSeq
        params(ap.getIndex).asTerm
      case _: AnnotatedMethod | _: AnnotatedField =>
        val decl = clsTpe.declaration(termName)
        if (decl.isTerm) decl.asTerm else ru.NoSymbol
    }
  }

  private def paramFor(member: AnnotatedMember): ru.SymbolApi = {
    val sym = termFor(member)
    if (!sym.isTerm) {
      return ru.NoSymbol
    }
    val term = sym.asTerm
    if (term.isParameter) {
      return term
    }
    if (!(term.isParamAccessor || term.isParameter)) {
      return ru.NoSymbol
    }

    val ownerTpe = term.owner.asType.toType
    val getter = if (term.isParamAccessor) {
      term.accessed.asTerm.getter
    } else if (term.isVal || term.isVar) {
      term.getter
    } else {
      ru.NoSymbol
    }

    val m = ru.runtimeMirror(member.getDeclaringClass.getClassLoader)
    val tpe = m.classSymbol(member.getDeclaringClass).asClass.toType
    val ctor = {
      val sym = tpe.declaration(ru.nme.CONSTRUCTOR)
      if (sym == ru.NoSymbol) {
        return ru.NoSymbol
      }
      if (sym.isMethod) sym.asMethod
      else {
        val ctors = sym.asTerm.alternatives
        ctors.map(_.asMethod).find(_.isPrimaryConstructor).get
      }
    }
    ctor.paramss.view.flatten.find(p => ownerTpe.declaration(p.name) == getter).getOrElse(ru.NoSymbol)
  }

  private def parameterPropertyName(member: AnnotatedParameter): Option[String] = {
    member.getOwner match {
      case ac: AnnotatedConstructor if isPrimaryConsructor(ac) =>
        val mirror = ru.runtimeMirror(member.getDeclaringClass.getClassLoader)
        val clsSym = mirror.classSymbol(member.getDeclaringClass).asClass
        val ctorSym = clsSym.toType.declarations.view.filter(_.isMethod).map(_.asMethod).find(_.isPrimaryConstructor).head
        val params = ctorSym.paramss.flatten.toIndexedSeq
        if (params.length > member.getIndex)
          Some(params(member.getIndex).name.decoded)
        else
          None
      case _ => None
    }
  }

  private def methodPropertyName(member: AnnotatedMethod): Option[String] = {
    val mirror = ru.runtimeMirror(member.getDeclaringClass.getClassLoader)
    val clsSym = mirror.classSymbol(member.getDeclaringClass).asClass
    val memberSym = clsSym.toType.declaration(ru.newTermName(member.getName))
    if (memberSym.isMethod) {
      val methodSym = memberSym.asMethod
      if (methodSym.isGetter) {
        return Some(methodSym.name.decoded)
      }
      if (methodSym.isSetter) {
        return Some(methodSym.accessed.asTerm.getter.name.decoded)
      }
    }
    None
  }

  private def isJava(member: AnnotatedMember): Boolean = {
    val cls = member.getDeclaringClass
    val m = ru.runtimeMirror(cls.getClassLoader)
    m.classSymbol(cls).isJava
  }

  override def findImplicitPropertyName(member: AnnotatedMember): String = {
    if (isJava(member)) {
      return null
    }
    val name = member match {
      case am: AnnotatedMethod => methodPropertyName(am)
      case ap: AnnotatedParameter => parameterPropertyName(ap)
      case _ => None
    }
    name.orNull
  }

  private val JSON_PROPERTY = classOf[JsonProperty]
  private def findJsonPropertyValue(sym: ru.SymbolApi): Option[String] = {
    if (sym == ru.NoSymbol) {
      return None
    }
    val m = ru.runtimeMirror(JSON_PROPERTY.getClassLoader)
    val tpe = m.classSymbol(JSON_PROPERTY).asClass.toType
    sym.annotations.find(_.tpe =:= tpe)
      .flatMap(_.javaArgs.get(ru.newTermName("value")))
      .map(_.toString.stripPrefix("\"").stripSuffix("\""))
  }

  override def findNameForSerialization(a: Annotated): PropertyName = {
    a match {
      case ap: AnnotatedParameter => null
      case member: AnnotatedMember =>
        findJsonPropertyValue(paramFor(member)).map(new PropertyName(_)).orNull
      case _ => null
    }
  }

  override def findNameForDeserialization(a: Annotated): PropertyName = {
    a match {
      case ap: AnnotatedParameter => null
      case member: AnnotatedMember =>
        findJsonPropertyValue(paramFor(member)).map(new PropertyName(_)).orNull
      case _ => null
    }
  }
}

trait ScalaAnnotationIntrospectorModule extends JacksonModule
{
  this += (_ appendAnnotationIntrospector ScalaAnnotationIntrospector)
}