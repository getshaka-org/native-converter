package org.getshaka.nativeconverter

class Lazy[A](obj: => A):
  lazy val value: A = obj

object Lazy:

  given [A](using obj: => A): Lazy[A] = new Lazy(obj)
