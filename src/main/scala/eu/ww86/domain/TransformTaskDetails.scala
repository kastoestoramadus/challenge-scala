package eu.ww86.domain

import java.net.URI

case class TransformTaskDetails(
                                 requestedCsv: URI,
                                 processingDetails: Option[ProcessingDetails],
                                 state: TransformTaskStatus,
                                 outputJson: Option[URI]
                               )

case class ProcessingDetails(startedAt: Long, linesProcessed: Int, processingSpeed: Int)