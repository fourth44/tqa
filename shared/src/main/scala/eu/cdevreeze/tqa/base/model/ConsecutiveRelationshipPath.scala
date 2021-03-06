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

package eu.cdevreeze.tqa.base.model

import eu.cdevreeze.yaidom.core.EName

import scala.collection.immutable

/**
 * Standard/generic inter-concept relationship path. Subsequent relationships in the path must be "consecutive". For non-dimensional
 * relationships that means that (of course) target concepts match the subsequent source concepts, and that the ELR
 * remain the same.
 *
 * A relationship path must have at least one relationship.
 *
 * @author Chris de Vreeze
 */
final case class ConsecutiveRelationshipPath[A <: InterElementDeclarationRelationship] private (
    relationships: immutable.IndexedSeq[A]) {

  require(relationships.nonEmpty, s"A relationship path must have at least one relationship")

  def sourceConcept: EName = firstRelationship.sourceElementTargetEName

  def targetConcept: EName = lastRelationship.targetElementTargetEName

  def firstRelationship: A = relationships.head

  def lastRelationship: A = relationships.last

  def concepts: immutable.IndexedSeq[EName] = {
    relationships.map(_.sourceElementTargetEName) :+ relationships.last.targetElementTargetEName
  }

  def relationshipTargetConcepts: immutable.IndexedSeq[EName] = {
    relationships.map(_.targetElementTargetEName)
  }

  def hasCycle: Boolean = {
    val concepts = relationships.map(_.sourceElementTargetEName) :+ relationships.last.targetElementTargetEName
    concepts.distinct.size < concepts.size
  }

  def isMinimalIfHavingCycle: Boolean = {
    initOption.forall(p => !p.hasCycle)
  }

  def append(relationship: A): ConsecutiveRelationshipPath[A] = {
    require(canAppend(relationship), s"Could not append relationship $relationship")
    new ConsecutiveRelationshipPath(relationships :+ relationship)
  }

  def prepend(relationship: A): ConsecutiveRelationshipPath[A] = {
    require(canPrepend(relationship), s"Could not prepend relationship $relationship")
    new ConsecutiveRelationshipPath(relationship +: relationships)
  }

  def canAppend(relationship: A): Boolean = {
    lastRelationship.isFollowedBy(relationship)
  }

  def canPrepend(relationship: A): Boolean = {
    relationship.isFollowedBy(firstRelationship)
  }

  def inits: immutable.IndexedSeq[ConsecutiveRelationshipPath[A]] = {
    relationships.inits.filter(_.nonEmpty).toIndexedSeq.map(rels => new ConsecutiveRelationshipPath[A](rels))
  }

  def tails: immutable.IndexedSeq[ConsecutiveRelationshipPath[A]] = {
    relationships.tails.filter(_.nonEmpty).toIndexedSeq.map(rels => new ConsecutiveRelationshipPath[A](rels))
  }

  def initOption: Option[ConsecutiveRelationshipPath[A]] = {
    if (relationships.size == 1) {
      None
    } else {
      assert(relationships.size >= 2)
      Some(new ConsecutiveRelationshipPath(relationships.init))
    }
  }

  def drop(n: Int): ConsecutiveRelationshipPath[A] = {
    ConsecutiveRelationshipPath(relationships.drop(n.min(relationships.size - 1)))
  }

  def dropRight(n: Int): ConsecutiveRelationshipPath[A] = {
    ConsecutiveRelationshipPath(relationships.dropRight(n.min(relationships.size - 1)))
  }
}

object ConsecutiveRelationshipPath {

  def apply[A <: InterElementDeclarationRelationship](relationship: A): ConsecutiveRelationshipPath[A] = {
    new ConsecutiveRelationshipPath(immutable.IndexedSeq(relationship))
  }

  def from[A <: InterElementDeclarationRelationship](
      relationships: immutable.IndexedSeq[A]): ConsecutiveRelationshipPath[A] = {
    require(
      relationships.sliding(2).filter(_.size == 2).forall(pair => pair(0).isFollowedBy(pair(1))),
      s"All subsequent relationships in a path must be consecutive")

    new ConsecutiveRelationshipPath(relationships)
  }
}
