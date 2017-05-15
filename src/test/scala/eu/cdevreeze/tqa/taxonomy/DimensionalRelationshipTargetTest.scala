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

package eu.cdevreeze.tqa.taxonomy

import java.io.File

import scala.collection.immutable
import scala.reflect.classTag

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.tqa.ENames
import eu.cdevreeze.tqa.SubstitutionGroupMap
import eu.cdevreeze.tqa.backingelem.nodeinfo.SaxonDocumentBuilder
import eu.cdevreeze.tqa.dom.LocalElementDeclaration
import eu.cdevreeze.tqa.dom.RoleRef
import eu.cdevreeze.tqa.dom.RoleType
import eu.cdevreeze.tqa.dom.TaxonomyBase
import eu.cdevreeze.tqa.dom.TaxonomyElem
import eu.cdevreeze.tqa.relationship.DefaultRelationshipFactory
import eu.cdevreeze.tqa.relationship.DimensionalRelationship
import eu.cdevreeze.tqa.relationship.HasHypercubeRelationship
import eu.cdevreeze.tqa.relationship.HypercubeDimensionRelationship
import eu.cdevreeze.yaidom.core.EName
import net.sf.saxon.s9api.Processor

/**
 * Dimensional relationship target querying test case. It uses test data from the XBRL Dimensions conformance suite.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class DimensionalRelationshipTargetTest extends FunSuite {

  test("testTargetHypercubeDimensionIsItemInvalid") {
    val taxo = makeTestTaxonomy(Vector(
      "100-xbrldte/103-HypercubeDimensionTargetError/targetHypercubeDimensionIsItemInvalid.xsd",
      "100-xbrldte/103-HypercubeDimensionTargetError/targetHypercubeDimensionIsItemInvalid-definition.xml"))

    val invalidDimensionName = EName("{http://www.xbrl.org/dim/conf}ProdDim")
    val validDimensionName = EName("{http://www.xbrl.org/dim/conf}RegionDim")

    val hypercubeDimensions = taxo.findAllHypercubeDimensionRelationships

    assertResult(2) {
      hypercubeDimensions.size
    }

    assertResult(Set(invalidDimensionName, validDimensionName)) {
      hypercubeDimensions.map(_.targetConceptEName).toSet
    }
    assertResult(Set(invalidDimensionName, validDimensionName)) {
      hypercubeDimensions.map(_.dimension).toSet
    }

    assertResult(true) {
      taxo.findItemDeclaration(invalidDimensionName).isDefined
    }
    assertResult(true) {
      taxo.findItemDeclaration(validDimensionName).isDefined
    }

    // Not a dimension
    assertResult(false) {
      taxo.findDimensionDeclaration(invalidDimensionName).isDefined
    }
    assertResult(true) {
      taxo.findDimensionDeclaration(validDimensionName).isDefined
    }
    assertResult(true) {
      taxo.findExplicitDimensionDeclaration(validDimensionName).isDefined
    }
    assertResult(false) {
      taxo.findTypedDimensionDeclaration(validDimensionName).isDefined
    }

    // Not a dimension
    assertResult(true) {
      taxo.findPrimaryItemDeclaration(invalidDimensionName).isDefined
    }
    assertResult(false) {
      taxo.findPrimaryItemDeclaration(validDimensionName).isDefined
    }
  }

  test("testTargetHypercubeDimensionIsTupleInvalid") {
    val taxo = makeTestTaxonomy(Vector(
      "100-xbrldte/103-HypercubeDimensionTargetError/targetHypercubeDimensionIsTupleInvalid.xsd",
      "100-xbrldte/103-HypercubeDimensionTargetError/targetHypercubeDimensionIsTupleInvalid-definition.xml"))

    val invalidDimensionName = EName("{http://www.xbrl.org/dim/conf}ProdDim")
    val validDimensionName = EName("{http://www.xbrl.org/dim/conf}RegionDim")

    val hypercubeDimensions = taxo.findAllHypercubeDimensionRelationships

    assertResult(2) {
      hypercubeDimensions.size
    }

    assertResult(Set(invalidDimensionName, validDimensionName)) {
      hypercubeDimensions.map(_.targetConceptEName).toSet
    }
    assertResult(Set(invalidDimensionName, validDimensionName)) {
      hypercubeDimensions.map(_.dimension).toSet
    }

    assertResult(true) {
      taxo.findConceptDeclaration(invalidDimensionName).isDefined
    }
    assertResult(true) {
      taxo.findConceptDeclaration(validDimensionName).isDefined
    }

    // Not a dimension
    assertResult(false) {
      taxo.findDimensionDeclaration(invalidDimensionName).isDefined
    }
    assertResult(true) {
      taxo.findDimensionDeclaration(validDimensionName).isDefined
    }
    assertResult(true) {
      taxo.findExplicitDimensionDeclaration(validDimensionName).isDefined
    }
    assertResult(false) {
      taxo.findTypedDimensionDeclaration(validDimensionName).isDefined
    }

    // Not a dimension
    assertResult(true) {
      taxo.findTupleDeclaration(invalidDimensionName).isDefined
    }
    assertResult(false) {
      taxo.findTupleDeclaration(validDimensionName).isDefined
    }
  }

  test("testTargetHypercubeDimensionIsHypercubeInvalid") {
    val taxo = makeTestTaxonomy(Vector(
      "100-xbrldte/103-HypercubeDimensionTargetError/targetHypercubeDimensionIsHypercubeInvalid.xsd",
      "100-xbrldte/103-HypercubeDimensionTargetError/targetHypercubeDimensionIsHypercubeInvalid-definition.xml"))

    val invalidDimensionName = EName("{http://www.xbrl.org/dim/conf}ProdDim")

    val incomingHypercubeDimensions =
      taxo.findAllIncomingInterConceptRelationshipsOfType(invalidDimensionName, classTag[HypercubeDimensionRelationship])

    assertResult(1) {
      incomingHypercubeDimensions.size
    }

    assertResult(true) {
      taxo.findConceptDeclaration(invalidDimensionName).isDefined
    }

    // Not a dimension
    assertResult(false) {
      taxo.findDimensionDeclaration(invalidDimensionName).isDefined
    }
    assertResult(true) {
      taxo.findHypercubeDeclaration(invalidDimensionName).isDefined
    }
  }

  test("testTargetHasHypercubeIsItemInvalid") {
    val taxo = makeTestTaxonomy(Vector(
      "100-xbrldte/105-HasHypercubeTargetError/targetHasHypercubeIsItemInvalid.xsd",
      "100-xbrldte/105-HasHypercubeTargetError/targetHasHypercubeIsItemInvalid-definition.xml"))

    val targetName = EName("{http://www.example.com/new}Hypercube")

    val hasHypercubes =
      taxo.findAllIncomingInterConceptRelationshipsOfType(targetName, classTag[HasHypercubeRelationship])

    assertResult(1) {
      hasHypercubes.size
    }
    assertResult(Some(ENames.XbrliItemEName)) {
      taxo.findConceptDeclaration(targetName).flatMap(_.globalElementDeclaration.substitutionGroupOption)
    }
    assertResult(false) {
      taxo.findHypercubeDeclaration(targetName).isDefined
    }
    assertResult(true) {
      taxo.findPrimaryItemDeclaration(targetName).isDefined
    }
  }

  test("testTargetHasHypercubeIsDimensionInvalid") {
    val taxo = makeTestTaxonomy(Vector(
      "100-xbrldte/105-HasHypercubeTargetError/targetHasHypercubeIsDimensionInvalid.xsd",
      "100-xbrldte/105-HasHypercubeTargetError/targetHasHypercubeIsDimensionInvalid-definition.xml"))

    val targetName = EName("{http://www.example.com/new}Hypercube")

    val hasHypercubes = taxo.findAllHasHypercubeRelationships

    assertResult(1) {
      hasHypercubes.size
    }
    assertResult(false) {
      taxo.findHypercubeDeclaration(targetName).isDefined
    }
    assertResult(true) {
      taxo.findExplicitDimensionDeclaration(targetName).isDefined
    }
  }

  test("testTargetDimensionDomainIsDimensionInvalid") {
    val taxo = makeTestTaxonomy(Vector(
      "100-xbrldte/113-DimensionDomainTargetError/targetDimensionDomainIsDimensionInvalid.xsd",
      "100-xbrldte/113-DimensionDomainTargetError/targetDimensionDomainIsDimensionInvalid-definition.xml"))

    val dimensionDomains = taxo.findAllDimensionDomainRelationships

    assertResult(1) {
      dimensionDomains.size
    }

    assertResult(dimensionDomains.map(_.targetConceptEName)) {
      dimensionDomains.map(_.domain)
    }
    assertResult(false) {
      taxo.findPrimaryItemDeclaration(dimensionDomains.head.domain).nonEmpty
    }
    assertResult(true) {
      taxo.findDimensionDeclaration(dimensionDomains.head.domain).nonEmpty
    }
  }

  test("testTargetDimensionDomainIsHypercubeInvalid") {
    val taxo = makeTestTaxonomy(Vector(
      "100-xbrldte/113-DimensionDomainTargetError/targetDimensionDomainIsHypercubeInvalid.xsd",
      "100-xbrldte/113-DimensionDomainTargetError/targetDimensionDomainIsHypercubeInvalid-definition.xml"))

    val dimensionDomains = taxo.findAllDimensionDomainRelationships

    assertResult(1) {
      dimensionDomains.size
    }

    assertResult(false) {
      taxo.findPrimaryItemDeclaration(dimensionDomains.head.domain).nonEmpty
    }
    assertResult(true) {
      taxo.findHypercubeDeclaration(dimensionDomains.head.domain).nonEmpty
    }
  }

  private def makeTestTaxonomy(relativeDocPaths: immutable.IndexedSeq[String]): BasicTaxonomy = {
    val rootDir = new File(classOf[DimensionalRelationshipTargetTest].getResource("/conf-suite-dim").toURI)
    val docFiles = relativeDocPaths.map(relativePath => new File(rootDir, relativePath))

    val rootElems = docFiles.map(f => docBuilder.build(f.toURI))

    val taxoRootElems = rootElems.map(e => TaxonomyElem.build(e))

    val underlyingTaxo = TaxonomyBase.build(taxoRootElems)
    val richTaxo =
      BasicTaxonomy.build(underlyingTaxo, SubstitutionGroupMap.Empty, DefaultRelationshipFactory.LenientInstance)
    richTaxo
  }

  private val processor = new Processor(false)

  private val docBuilder = new SaxonDocumentBuilder(processor.newDocumentBuilder(), (uri => uri))
}
