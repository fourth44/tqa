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

package eu.cdevreeze.tqa.base.queryapi

import scala.collection.immutable
import scala.reflect.classTag

import eu.cdevreeze.tqa.XmlFragmentKey
import eu.cdevreeze.tqa.base.relationship.ElementReferenceRelationship

/**
 * Partial implementation of `ElementReferenceRelationshipContainerApi`.
 *
 * @author Chris de Vreeze
 */
trait ElementReferenceRelationshipContainerLike extends ElementReferenceRelationshipContainerApi { self: NonStandardRelationshipContainerApi =>

  // Finding and filtering relationships without looking at source element

  final def findAllElementReferenceRelationships: immutable.IndexedSeq[ElementReferenceRelationship] = {
    findAllNonStandardRelationshipsOfType(classTag[ElementReferenceRelationship])
  }

  final def filterElementReferenceRelationships(
    p: ElementReferenceRelationship => Boolean): immutable.IndexedSeq[ElementReferenceRelationship] = {

    filterNonStandardRelationshipsOfType(classTag[ElementReferenceRelationship])(p)
  }

  // Finding and filtering outgoing relationships

  final def findAllOutgoingElementReferenceRelationships(
    sourceKey: XmlFragmentKey): immutable.IndexedSeq[ElementReferenceRelationship] = {

    findAllOutgoingNonStandardRelationshipsOfType(sourceKey, classTag[ElementReferenceRelationship])
  }

  final def filterOutgoingElementReferenceRelationships(
    sourceKey: XmlFragmentKey)(p: ElementReferenceRelationship => Boolean): immutable.IndexedSeq[ElementReferenceRelationship] = {

    filterOutgoingNonStandardRelationshipsOfType(sourceKey, classTag[ElementReferenceRelationship])(p)
  }
}
