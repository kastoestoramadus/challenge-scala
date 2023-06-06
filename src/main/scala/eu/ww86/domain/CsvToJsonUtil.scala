package eu.ww86.domain

import io.circe.*
import io.circe.generic.auto.*
import io.circe.Json
import io.circe.syntax.*

import scala.collection.mutable

// TODO cover special characters, find external lib?
class CsvToJsonUtil(headers: Vector[String]) {
  def process(csvLine: String): Option[Json] = {
    val cells = csvLine.split(CsvToJsonUtil.splitPattern)
    if(cells.length != headers.length) None
    else {
      val fields = mutable.Map[String, String]()
      for i <- headers.indices
        do {
          fields += headers(i) -> cells(i)
        }
      Some(fields.asJson)
    }
  }
}

// TODO cover special characters
object CsvToJsonUtil {
  val splitPattern = """(?:^\s*"\s*|\s*"\s*$|\s*"?\s*,\s*"?\s*)"""
  def createFromFirstLineHeaders(csvHeadersLine: String): Option[CsvToJsonUtil] =
    Some(CsvToJsonUtil(csvHeadersLine.split(splitPattern).toVector))
}
