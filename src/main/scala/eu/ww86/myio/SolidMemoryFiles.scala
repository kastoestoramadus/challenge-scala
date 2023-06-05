package eu.ww86.myio

import cats.effect.IO

import java.net.URI

// TODO
class SolidMemoryFiles extends FilesService:
  // I've seen some fs2 library for streaming
  def transformFileFromURL(uri: URI, outputFileName: String)
                          (transformingFunction: FilesService.TransformingHooks => Either[String, Unit]): IO[Either[String, Unit]] = ???
  // tapir has support for streaming
  def getOutputFile(outputFileName: String): IO[Option[String]] = ???