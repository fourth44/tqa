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

package eu.cdevreeze.tqa.docbuilder.indexed

import java.net.URI

import eu.cdevreeze.tqa.docbuilder.DocumentBuilder
import eu.cdevreeze.yaidom.indexed.Document
import eu.cdevreeze.yaidom.parse.DocumentParser
import org.xml.sax.InputSource

/**
 * Indexed document builder using a yaidom DocumentParser and URI resolver.
 *
 * The URI resolver is used for parsing the documents themselves (unlike SAX EntityResolver).
 * Typically the URI resolver takes HTTP(S) URIs and resolves them to resources in a local mirror.
 *
 * @author Chris de Vreeze
 */
final class IndexedDocumentBuilder(val docParser: DocumentParser, val uriResolver: URI => InputSource)
    extends DocumentBuilder {

  type BackingDoc = Document

  def build(uri: URI): Document = {
    val is = uriResolver(uri)

    val doc = docParser.parse(is).withUriOption(Some(uri))
    Document(doc)
  }
}

object IndexedDocumentBuilder {

  /**
   * Creates an IndexedDocumentBuilder from a yaidom DocumentParser, and an URI resolver.
   * The URI resolver is typically obtained through the UriResolvers singleton object.
   */
  def apply(docParser: DocumentParser, uriResolver: URI => InputSource): IndexedDocumentBuilder = {
    new IndexedDocumentBuilder(docParser, uriResolver)
  }
}
