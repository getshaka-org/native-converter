package org.getshaka.nativeconverter

import scala.scalajs.js
import scala.scalajs.js.JSON

class ParseState private (
  val json: js.Any,
  val wholeJson: js.Any,
  jsonPath: List[String]
):

  def atIndex(index: Int, json: js.Any): ParseState =
    new ParseState(json, wholeJson, s"[$index]" :: jsonPath)

  def atKey(key: String, json: js.Any): ParseState =
    new ParseState(json, wholeJson, s".$key" :: jsonPath)

  def left[A](whatIsExpected: String): Left[String, A] =
    val pathString = "root" + jsonPath.foldLeft("")((res, path) => path + res)
    val jsonClsName = js.typeOf(json)
    Left(
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

object ParseState:
  def apply(json: js.Any): ParseState = new ParseState(json, json, Nil)
