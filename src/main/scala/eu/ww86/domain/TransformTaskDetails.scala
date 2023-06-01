package eu.ww86.domain

import java.net.URI

case class TransformTaskDetails(
                                 requestedCsv: URI,
                                 processingDetails: Option[ProcessingDetails],
                                 state: TransformTaskStatus,
                                 resultsFileName: Option[String] // could be URL instead of fileName
                               )

case class ProcessingDetails(startedAt: Long, linesProcessed: Int, linesPerMinute: Double)