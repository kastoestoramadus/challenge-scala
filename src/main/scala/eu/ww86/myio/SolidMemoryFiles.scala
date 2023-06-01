package eu.ww86.myio

import java.net.URI

// TODO
class SolidMemoryFiles[F[_]] extends FilesService[F]:
  // I've seen some fs2 library for streaming
  def transformFileFromURL(uri: URI, outputFileName: String)(transformingFunction: FilesService.TransformingHooks => Unit): F[Boolean] = ???
  // tapir has support for streaming
  def getOutputFile(outputFileName: String): F[Option[String]] = ???