/*
 * Copyright 2011-2017 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.tqa.docbuilder.jvm

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import scala.collection.JavaConverters._

import eu.cdevreeze.tqa.docbuilder.SimpleCatalog
import org.xml.sax.InputSource

/**
 * Partial URI resolvers, converting an URI to a SAX InputSource.
 *
 * Note that the only fundamental methods in this singleton object are fromPartialUriConverter and its counterpart for ZIP files,
 * namely forZipFile.
 *
 * @author Chris de Vreeze
 */
object PartialUriResolvers {

  type PartialUriResolver = URI => Option[InputSource]

  /**
   * Creates a PartialUriResolver from a partial URI converter. Typically the URI converter converts HTTP(S) URIs
   * to file protocol URIs, but it must in any case return only absolute URIs.
   *
   * The created PartialUriResolver is defined for the same URIs as the input PartialUriConverter.
   */
  def fromPartialUriConverter(partialUriConverter: URI => Option[URI]): PartialUriResolver = {
    def resolveUri(uri: URI): Option[InputSource] = {
      val mappedUriOption = partialUriConverter(uri)

      mappedUriOption.map { mappedUri =>
        require(mappedUri.isAbsolute, s"Cannot resolve relative URI '$mappedUri'")

        val is: InputStream =
          if (mappedUri.getScheme == "file") {
            new FileInputStream(new File(mappedUri))
          } else {
            mappedUri.toURL.openStream()
          }

        new InputSource(is)
      }
    }

    resolveUri
  }

  /**
   * Creates a PartialUriResolver for a ZIP file, using the given partial URI converter. This partial URI
   * converter must return only relative URIs.
   *
   * The created PartialUriResolver is defined for the same URIs as the input PartialUriConverter.
   *
   * The ZIP file should be closed after use (typically when the taxonomy has been loaded), thus closing
   * all its entry input streams.
   */
  def forZipFile(zipFile: ZipFile, partialUriConverter: URI => Option[URI]): PartialUriResolver = {
    val zipEntriesByRelativeUri: Map[URI, ZipEntry] = computeZipEntryMap(zipFile)

    def resolveUri(uri: URI): Option[InputSource] = {
      val mappedUriOption = partialUriConverter(uri)

      mappedUriOption.map { mappedUri =>
        require(!mappedUri.isAbsolute, s"Cannot resolve absolute URI '$mappedUri'")

        val optionalZipEntry: Option[ZipEntry] = zipEntriesByRelativeUri.get(mappedUri)

        require(optionalZipEntry.isDefined, s"Missing ZIP entry in ZIP file $zipFile with URI $mappedUri")

        val is = zipFile.getInputStream(optionalZipEntry.get)

        new InputSource(is)
      }
    }

    resolveUri
  }

  /**
   * Returns `fromPartialUriConverter(PartialUriConverters.fromCatalog(catalog))`.
   */
  def fromCatalog(catalog: SimpleCatalog): PartialUriResolver = {
    fromPartialUriConverter(PartialUriConverters.fromCatalog(catalog))
  }

  /**
   * Returns `forZipFile(zipFile, PartialUriConverters.fromCatalog(catalog))`.
   */
  def forZipFileUsingCatalog(zipFile: ZipFile, catalog: SimpleCatalog): PartialUriResolver = {
    forZipFile(zipFile, PartialUriConverters.fromCatalog(catalog))
  }

  /**
   * Creates a PartialUriResolver from the given UriResolver, returning None for all URIs for which the URI filter returns false.
   */
  def fromUriResolver(uriResolver: URI => InputSource, filterUri: URI => Boolean): PartialUriResolver = {
    { uri: URI => if (filterUri(uri)) Some(uriResolver(uri)) else None }
  }

  /**
   * Returns `fromUriResolver(uriResolver, _ => true)`.
   */
  def fromUriResolver(uriResolver: URI => InputSource): PartialUriResolver = {
    fromUriResolver(uriResolver, _ => true)
  }

  val default: PartialUriResolver = {
    fromPartialUriConverter(PartialUriConverters.identity)
  }

  private def computeZipEntryMap(zipFile: ZipFile): Map[URI, ZipEntry] = {
    val zipEntries = zipFile.entries().asScala.toIndexedSeq

    val zipFileParent = dummyDirectory

    zipEntries.map(e => toRelativeUri(e, zipFileParent) -> e).toMap
  }

  private def toRelativeUri(zipEntry: ZipEntry, zipFileParent: File): URI = {
    val adaptedZipEntryUri = new File(zipFileParent, zipEntry.getName).toURI
    val zipFileParentUri = URI.create(returnWithTrailingSlash(zipFileParent.toURI))
    val relativeZipEntryUri = zipFileParentUri.relativize(adaptedZipEntryUri)
    require(!relativeZipEntryUri.isAbsolute, s"Not a relative URI: $relativeZipEntryUri")

    relativeZipEntryUri
  }

  private def returnWithTrailingSlash(uri: URI): String = {
    val s = uri.toString
    if (s.endsWith("/")) s else s + "/"
  }

  private val dummyDirectory = new File("/dummyRoot")
}
