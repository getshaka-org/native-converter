package org.getshaka.nativeconverter

/**
 * Sorry purists, I made a mutable Product. It's faster.
 */
class ArrayProduct(size: Int) extends Product:
  private val a = Array.ofDim[Any](size)

  def update(i: Int, any: Any): Unit =
    a(i) = any

  override def productArity: Int = size

  override def productElement(n: Int): Any = a(n)

  override def productIterator: Iterator[Any] = a.iterator

  override def productPrefix: String = "ArrayProduct"

  override def canEqual(that: Any): Boolean = that match
    case ap: ArrayProduct if ap.productArity == productArity => true
    case _ => false
