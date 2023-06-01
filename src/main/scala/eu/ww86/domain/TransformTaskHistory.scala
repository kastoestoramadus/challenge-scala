package eu.ww86.domain

import java.net.URI
import scala.concurrent.duration.FiniteDuration

case class TransformTaskHistory(
                                 requestedCsv: URI,
                                 state: TransformTaskStatus,
                                 scheduledAt: FiniteDuration,
                                 processingStartedAt: Option[FiniteDuration],
                                 endedAt: Option[FiniteDuration]
                               )