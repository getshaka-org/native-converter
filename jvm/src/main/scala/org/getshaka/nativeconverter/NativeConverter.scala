package org.getshaka.nativeconverter

import scala.annotation.implicitNotFound
import scala.deriving.Mirror

/**
 * Typeclass for converting between Scala.js and native JavaScript.
 * Only supported in JS projects.
 *
 * @tparam A the type to convert
 */
@implicitNotFound("Could not find an implicit NativeConverter[${A}]")
trait NativeConverter[A]:

  extension (a: A)
    def toNative: Any
    def toJson: String

  def fromNative(nativeJs: Any): A
  def fromJson(json: String): A

object NativeConverter:

  private val Msg = "NativeConverter only supported in JS projects"

  inline given derived[A](using m: Mirror.Of[A]): NativeConverter[A] with
    extension (a: A)
      def toNative: Any = throw UnsupportedOperationException(Msg)
      def toJson: String = throw UnsupportedOperationException(Msg)
    def fromNative(nativeJs: Any): A = throw UnsupportedOperationException(Msg)
    def fromJson(json: String): A = throw UnsupportedOperationException(Msg)

