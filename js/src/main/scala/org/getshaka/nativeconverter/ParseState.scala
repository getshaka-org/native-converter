package org.getshaka.nativeconverter

import org.getshaka.nativeconverter.PathPosition.*

import scala.scalajs.js
import scala.scalajs.js.JSON

class ParseState private (
    val json: js.Any,
    val wholeJson: js.Any,
    jsonPath: List[PathPosition]
):

  def atIndex(index: Int, json: js.Any): ParseState =
    new ParseState(json, wholeJson, Index(index) :: jsonPath)

  def atKey(key: String, json: js.Any): ParseState =
    new ParseState(json, wholeJson, Key(key) :: jsonPath)

  def fail[A](whatIsExpected: String): A =
    val pathString = "root" + jsonPath.foldLeft("")((res, pathPos) =>

      val pathPosString = pathPos match
        case Index(i) => "[" + i + "]"
        case Key(k)   => "." + k

      pathPosString + res
    )

    val jsonClsName = js.typeOf(json)

    throw RuntimeException(
      s"""
         |Json parse error. Expected to be $whatIsExpected, instead was $jsonClsName:
         |${JSON.stringify(json)}
         |
         |At position:
         |$pathString
         |
         |In:
         |${JSON.stringify(wholeJson)}
         |""".stripMargin
    )
  end fail

object ParseState:
  def apply(json: js.Any): ParseState = new ParseState(json, json, Nil)
