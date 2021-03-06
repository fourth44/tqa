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
import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.tqa.docbuilder.SimpleCatalog

/**
 * Partial URI converters, typically converting HTTP or HTTPS URIs to local file URIs. Typically a PartialUriConverter is
 * created from a SimpleCatalog. This is also desirable, because catalogs are clear and precise URI mappings. Yet not every
 * PartialUriConverter can be created from a SimpleCatalog, for example because an URI may map to the first of a list of
 * local URIs where the document can be found, which cannot be expressed with a SimpleCatalog.
 *
 * Sometimes it is desirable to create a SimpleCatalog and corresponding PartialUriConverter from some parent directory,
 * but there are several possible heuristics for creating a SimpleCatalog from such a parent directory. Hence it is up
 * to the application to generate a SimpleCatalog from the parent directory. Method PartialUriConverters.fromCatalog
 * can then be used to turn that SimpleCatalog into a PartialUriConverter.
 *
 * Note that there is only one fundamental method in this singleton object, namely fromCatalog.
 *
 * @author Chris de Vreeze
 */
object PartialUriConverters {

  type PartialUriConverter = URI => Option[URI]

  val identity: PartialUriConverter = {
    def convertUri(uri: URI): Option[URI] = Some(uri)

    convertUri
  }

  /**
   * Turns the given catalog into a partial URI converter. It can return absolute and/or relative
   * URIs. Relative URIs are typically meant to be resolved inside ZIP files.
   *
   * The partial URI converter is only defined for URIs matching URI start strings in the catalog.
   */
  def fromCatalog(catalog: SimpleCatalog): PartialUriConverter = {
    def convertUri(uri: URI): Option[URI] = {
      catalog.findMappedUri(uri)
    }

    convertUri
  }

  def fromUriConverter(uriConverter: URI => URI): PartialUriConverter = {
    uriConverter.andThen(u => Some(u))
  }

  /**
   * Turns a non-empty collection of catalogs into a partial URI converter, finding the first optional mapped URI for which
   * the given URI function returns true. This method is useful if some URIs can be mapped to multiple target URIs, and we
   * can describe this mapping as a non-empty collection of catalogs along with a filtering condition on mapped URIs.
   */
  def fromCatalogs(catalogs: immutable.IndexedSeq[SimpleCatalog], acceptTargetUri: URI => Boolean): PartialUriConverter = {
    require(catalogs.nonEmpty, s"No catalogs given")

    def convertUri(uri: URI): Option[URI] = {
      catalogs.iterator.flatMap(_.findMappedUri(uri)).find(acceptTargetUri)
    }

    convertUri
  }

  // URI filters

  // TODO URI filters for ZIP files

  /**
   * URI filter that only returns true for absolute "file" protocol URIs for existing normal files (so no directories)
   */
  def acceptOnlyExistingFile(uri: URI): Boolean = {
    uri.isAbsolute && uri.getScheme == "file" && new File(uri).isFile
  }

  /**
   * Like `acceptOnlyExistingFile`, but also returning true for absolute "http" or "https" URIs
   */
  def acceptExistingFile(uri: URI): Boolean = {
    uri.isAbsolute && (uri.getScheme == "http" || uri.getScheme == "https" || acceptOnlyExistingFile(uri))
  }
}
