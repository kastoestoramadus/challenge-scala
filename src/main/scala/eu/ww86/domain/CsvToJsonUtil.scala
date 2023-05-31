package eu.ww86.domain

import io.circe.Json

class CsvToJsonUtil(headers: Vector[String]) {
  def process(csvLine: String): Option[Json] = ???
}

object CsvToJsonUtil {
  def createFromFirstLineHeaders(csvHeadersLine: String): Option[CsvToJsonUtil] = ???
}
