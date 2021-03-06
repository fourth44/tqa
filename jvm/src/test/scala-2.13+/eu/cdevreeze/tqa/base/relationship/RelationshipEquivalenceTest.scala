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

package eu.cdevreeze.tqa.base.relationship

import java.io.File
import java.net.URI
import java.util.zip.ZipFile

import org.scalatest.funsuite.AnyFunSuite

import eu.cdevreeze.tqa.ENames.LinkLabelArcEName
import eu.cdevreeze.tqa.ENames.NameEName
import eu.cdevreeze.tqa.ENames.OrderEName
import eu.cdevreeze.tqa.ENames.WeightEName
import eu.cdevreeze.tqa.base.common.BaseSetKey
import eu.cdevreeze.tqa.base.common.Use
import eu.cdevreeze.tqa.base.dom.TaxonomyBase
import eu.cdevreeze.tqa.base.dom.TaxonomyDocument
import eu.cdevreeze.tqa.base.dom.XLinkLocator
import eu.cdevreeze.tqa.docbuilder.indexed.IndexedDocumentBuilder
import eu.cdevreeze.tqa.docbuilder.jvm.UriResolvers
import eu.cdevreeze.tqa.docbuilder.SimpleCatalog
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax

/**
 * Relationship equivalence test case. It uses test data from the XBRL Core Conformance Suite.
 *
 * @author Chris de Vreeze
 */
class RelationshipEquivalenceTest extends AnyFunSuite {

  test("testProhibition") {
    val docBuilder = getDocumentBuilder()

    val xsdDocUri = URI.create("file:///conf-suite/Common/200-linkbase/210-01-RelationshipEquivalence.xsd")
    val linkbaseDocUri1 = URI.create("file:///conf-suite/Common/200-linkbase/210-01-RelationshipEquivalence-calculation-1.xml")
    val linkbaseDocUri2 = URI.create("file:///conf-suite/Common/200-linkbase/210-01-RelationshipEquivalence-calculation-2.xml")

    val xsdBackingDoc = docBuilder.build(xsdDocUri)
    val linkbaseBackingDoc1 = docBuilder.build(linkbaseDocUri1)
    val linkbaseBackingDoc2 = docBuilder.build(linkbaseDocUri2)

    val xsdSchemaDoc = TaxonomyDocument.build(xsdBackingDoc)
    val linkbaseDoc1 = TaxonomyDocument.build(linkbaseBackingDoc1)
    val linkbaseDoc2 = TaxonomyDocument.build(linkbaseBackingDoc2)

    val tns = "http://mycompany.com/xbrl/taxonomy"

    val taxo = TaxonomyBase.build(Vector(xsdSchemaDoc, linkbaseDoc1, linkbaseDoc2))

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val relationships = relationshipFactory.extractRelationships(taxo, RelationshipFactory.AnyArc)

    val calcRelationships = relationships.collect { case rel: CalculationRelationship => rel }

    assertResult(2) {
      relationships.size
    }
    assertResult(relationships.map(_.arc)) {
      calcRelationships.map(_.arc)
    }

    val relationshipKeys = relationships.map(rel => relationshipFactory.getRelationshipKey(rel, taxo))

    assertResult(Set(BaseSetKey.forSummationItemArc(BaseSetKey.StandardElr))) {
      relationshipKeys.map(_.baseSetKey).toSet
    }
    assertResult(Set(taxo.findGlobalElementDeclarationByEName(EName(tns, "fixedAssets")).get.key)) {
      relationshipKeys.map(_.sourceKey).toSet
    }
    assertResult(Set(taxo.findGlobalElementDeclarationByEName(EName(tns, "changeInRetainedEarnings")).get.key)) {
      relationshipKeys.map(_.targetKey).toSet
    }
    assertResult(Set(NonExemptAttributeMap.from(Map(
      WeightEName -> DecimalAttributeValue(1),
      OrderEName -> DecimalAttributeValue(2))))) {

      relationshipKeys.map(_.nonExemptAttributes).toSet
    }

    // Both relationships are equivalent
    assertResult(1) {
      relationshipKeys.distinct.size
    }

    val networkMap = relationshipFactory.computeNetworks(relationships, taxo).view.mapValues(_.retainedRelationships).toMap

    assertResult(Map(BaseSetKey.forSummationItemArc(BaseSetKey.StandardElr) -> Vector())) {
      networkMap
    }
  }

  test("testFailingProhibition") {
    val docBuilder = getDocumentBuilder()

    val xsdDocUri = URI.create("file:///conf-suite/Common/200-linkbase/210-02-DifferentOrder.xsd")
    val linkbaseDocUri1 = URI.create("file:///conf-suite/Common/200-linkbase/210-02-DifferentOrder-calculation-1.xml")
    val linkbaseDocUri2 = URI.create("file:///conf-suite/Common/200-linkbase/210-02-DifferentOrder-calculation-2.xml")

    val xsdBackingDoc = docBuilder.build(xsdDocUri)
    val linkbaseBackingDoc1 = docBuilder.build(linkbaseDocUri1)
    val linkbaseBackingDoc2 = docBuilder.build(linkbaseDocUri2)

    val xsdSchemaDoc = TaxonomyDocument.build(xsdBackingDoc)
    val linkbaseDoc1 = TaxonomyDocument.build(linkbaseBackingDoc1)
    val linkbaseDoc2 = TaxonomyDocument.build(linkbaseBackingDoc2)

    val tns = "http://mycompany.com/xbrl/taxonomy"

    val taxo = TaxonomyBase.build(Vector(xsdSchemaDoc, linkbaseDoc1, linkbaseDoc2))

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val relationships = relationshipFactory.extractRelationships(taxo, RelationshipFactory.AnyArc)

    val calcRelationships = relationships.collect { case rel: CalculationRelationship => rel }

    assertResult(2) {
      relationships.size
    }
    assertResult(relationships.map(_.arc)) {
      calcRelationships.map(_.arc)
    }

    val relationshipKeys = relationships.map(rel => relationshipFactory.getRelationshipKey(rel, taxo))

    assertResult(Set(BaseSetKey.forSummationItemArc(BaseSetKey.StandardElr))) {
      relationshipKeys.map(_.baseSetKey).toSet
    }
    assertResult(Set(taxo.findGlobalElementDeclarationByEName(EName(tns, "fixedAssets")).get.key)) {
      relationshipKeys.map(_.sourceKey).toSet
    }
    assertResult(Set(taxo.findGlobalElementDeclarationByEName(EName(tns, "changeInRetainedEarnings")).get.key)) {
      relationshipKeys.map(_.targetKey).toSet
    }
    // Difference in the order attribute value, so no equivalent relationships
    assertResult(2) {
      relationshipKeys.map(_.nonExemptAttributes).toSet.size
    }

    // Both relationships are not equivalent
    assertResult(2) {
      relationshipKeys.distinct.size
    }

    val networkMap = relationshipFactory.computeNetworks(relationships, taxo).view.mapValues(_.retainedRelationships).toMap

    val nonProhibitedRelationships = relationships.filter(_.arc.use == Use.Optional)

    assertResult(1) {
      nonProhibitedRelationships.size
    }

    assertResult(Map(BaseSetKey.forSummationItemArc(BaseSetKey.StandardElr) -> nonProhibitedRelationships)) {
      networkMap
    }
  }

  test("testProhibitionWithImplicitOrder") {
    val docBuilder = getDocumentBuilder()

    val xsdDocUri = URI.create("file:///conf-suite/Common/200-linkbase/210-03-MissingOrder.xsd")
    val linkbaseDocUri1 = URI.create("file:///conf-suite/Common/200-linkbase/210-03-MissingOrder-calculation-1.xml")
    val linkbaseDocUri2 = URI.create("file:///conf-suite/Common/200-linkbase/210-03-MissingOrder-calculation-2.xml")

    val xsdBackingDoc = docBuilder.build(xsdDocUri)
    val linkbaseBackingDoc1 = docBuilder.build(linkbaseDocUri1)
    val linkbaseBackingDoc2 = docBuilder.build(linkbaseDocUri2)

    val xsdSchemaDoc = TaxonomyDocument.build(xsdBackingDoc)
    val linkbaseDoc1 = TaxonomyDocument.build(linkbaseBackingDoc1)
    val linkbaseDoc2 = TaxonomyDocument.build(linkbaseBackingDoc2)

    val tns = "http://mycompany.com/xbrl/taxonomy"

    val taxo = TaxonomyBase.build(Vector(xsdSchemaDoc, linkbaseDoc1, linkbaseDoc2))

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val relationships = relationshipFactory.extractRelationships(taxo, RelationshipFactory.AnyArc)

    val calcRelationships = relationships.collect { case rel: CalculationRelationship => rel }

    assertResult(2) {
      relationships.size
    }
    assertResult(relationships.map(_.arc)) {
      calcRelationships.map(_.arc)
    }

    val relationshipKeys = relationships.map(rel => relationshipFactory.getRelationshipKey(rel, taxo))

    assertResult(Set(BaseSetKey.forSummationItemArc(BaseSetKey.StandardElr))) {
      relationshipKeys.map(_.baseSetKey).toSet
    }
    assertResult(Set(taxo.findGlobalElementDeclarationByEName(EName(tns, "fixedAssets")).get.key)) {
      relationshipKeys.map(_.sourceKey).toSet
    }
    assertResult(Set(taxo.findGlobalElementDeclarationByEName(EName(tns, "changeInRetainedEarnings")).get.key)) {
      relationshipKeys.map(_.targetKey).toSet
    }
    // An implicit order has value 1
    assertResult(Set(NonExemptAttributeMap.from(Map(
      WeightEName -> DecimalAttributeValue(1),
      OrderEName -> DecimalAttributeValue(1))))) {

      relationshipKeys.map(_.nonExemptAttributes).toSet
    }

    // Both relationships are equivalent
    assertResult(1) {
      relationshipKeys.distinct.size
    }

    val networkMap = relationshipFactory.computeNetworks(relationships, taxo).view.mapValues(_.retainedRelationships).toMap

    assertResult(Map(BaseSetKey.forSummationItemArc(BaseSetKey.StandardElr) -> Vector())) {
      networkMap
    }
  }

  test("testProhibitionOfOneRelationship") {
    val docBuilder = getDocumentBuilder()

    val xsdDocUri = URI.create("file:///conf-suite/Common/200-linkbase/210-04-RelationshipEquivalence.xsd")
    val linkbaseDocUri1 = URI.create("file:///conf-suite/Common/200-linkbase/210-04-RelationshipEquivalence-calculation-1.xml")
    val linkbaseDocUri2 = URI.create("file:///conf-suite/Common/200-linkbase/210-04-RelationshipEquivalence-calculation-2.xml")

    val xsdBackingDoc = docBuilder.build(xsdDocUri)
    val linkbaseBackingDoc1 = docBuilder.build(linkbaseDocUri1)
    val linkbaseBackingDoc2 = docBuilder.build(linkbaseDocUri2)

    val xsdSchemaDoc = TaxonomyDocument.build(xsdBackingDoc)
    val linkbaseDoc1 = TaxonomyDocument.build(linkbaseBackingDoc1)
    val linkbaseDoc2 = TaxonomyDocument.build(linkbaseBackingDoc2)

    val tns = "http://mycompany.com/xbrl/taxonomy"

    val taxo = TaxonomyBase.build(Vector(xsdSchemaDoc, linkbaseDoc1, linkbaseDoc2))

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val relationships = relationshipFactory.extractRelationships(taxo, RelationshipFactory.AnyArc)

    val calcRelationships = relationships.collect { case rel: CalculationRelationship => rel }

    assertResult(3) {
      relationships.size
    }
    assertResult(relationships.map(_.arc)) {
      calcRelationships.map(_.arc)
    }

    val relationshipKeys = relationships.map(rel => relationshipFactory.getRelationshipKey(rel, taxo))

    assertResult(Set(BaseSetKey.forSummationItemArc(BaseSetKey.StandardElr))) {
      relationshipKeys.map(_.baseSetKey).toSet
    }
    assertResult(Set(taxo.findGlobalElementDeclarationByEName(EName(tns, "fixedAssets")).get.key)) {
      relationshipKeys.map(_.sourceKey).toSet
    }
    assertResult(Set(
      taxo.findGlobalElementDeclarationByEName(EName(tns, "changeInRetainedEarnings")).get.key,
      taxo.findGlobalElementDeclarationByEName(EName(tns, "floatingAssets")).get.key)) {

      relationshipKeys.map(_.targetKey).toSet
    }
    assertResult(Set(NonExemptAttributeMap.from(Map(
      WeightEName -> DecimalAttributeValue(1),
      OrderEName -> DecimalAttributeValue(1.0))))) {

      relationshipKeys.map(_.nonExemptAttributes).toSet
    }

    // Two of three relationships are equivalent
    assertResult(2) {
      relationshipKeys.distinct.size
    }

    val networkMap = relationshipFactory.computeNetworks(relationships, taxo).view.mapValues(_.retainedRelationships).toMap

    val floatingAssetsRels =
      calcRelationships.filter(_.targetConceptEName.localPart == "floatingAssets")

    assertResult(1) {
      floatingAssetsRels.size
    }

    assertResult(Map(BaseSetKey.forSummationItemArc(BaseSetKey.StandardElr) -> floatingAssetsRels)) {
      networkMap
    }
  }

  test("testProhibitionOfOneRelationshipAgain") {
    val docBuilder = getDocumentBuilder()

    val xsdDocUri = URI.create("file:///conf-suite/Common/200-linkbase/210-05-RelationshipEquivalence.xsd")
    val linkbaseDocUri1 = URI.create("file:///conf-suite/Common/200-linkbase/210-05-RelationshipEquivalence-calculation-1.xml")
    val linkbaseDocUri2 = URI.create("file:///conf-suite/Common/200-linkbase/210-05-RelationshipEquivalence-calculation-2.xml")

    val xsdBackingDoc = docBuilder.build(xsdDocUri)
    val linkbaseBackingDoc1 = docBuilder.build(linkbaseDocUri1)
    val linkbaseBackingDoc2 = docBuilder.build(linkbaseDocUri2)

    val xsdSchemaDoc = TaxonomyDocument.build(xsdBackingDoc)
    val linkbaseDoc1 = TaxonomyDocument.build(linkbaseBackingDoc1)
    val linkbaseDoc2 = TaxonomyDocument.build(linkbaseBackingDoc2)

    val tns = "http://mycompany.com/xbrl/taxonomy"

    val taxo = TaxonomyBase.build(Vector(xsdSchemaDoc, linkbaseDoc1, linkbaseDoc2))

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val relationships = relationshipFactory.extractRelationships(taxo, RelationshipFactory.AnyArc)

    val calcRelationships = relationships.collect { case rel: CalculationRelationship => rel }

    assertResult(3) {
      relationships.size
    }
    assertResult(relationships.map(_.arc)) {
      calcRelationships.map(_.arc)
    }

    val relationshipKeys = relationships.map(rel => relationshipFactory.getRelationshipKey(rel, taxo))

    assertResult(Set(BaseSetKey.forSummationItemArc(BaseSetKey.StandardElr))) {
      relationshipKeys.map(_.baseSetKey).toSet
    }
    assertResult(Set(taxo.findGlobalElementDeclarationByEName(EName(tns, "fixedAssets")).get.key)) {
      relationshipKeys.map(_.sourceKey).toSet
    }
    assertResult(Set(
      taxo.findGlobalElementDeclarationByEName(EName(tns, "changeInRetainedEarnings")).get.key,
      taxo.findGlobalElementDeclarationByEName(EName(tns, "floatingAssets")).get.key)) {

      relationshipKeys.map(_.targetKey).toSet
    }
    assertResult(Set(NonExemptAttributeMap.from(Map(
      WeightEName -> DecimalAttributeValue(1),
      OrderEName -> DecimalAttributeValue(1.0))))) {

      relationshipKeys.map(_.nonExemptAttributes).toSet
    }

    // Two of three relationships are equivalent
    assertResult(2) {
      relationshipKeys.distinct.size
    }

    val networkMap = relationshipFactory.computeNetworks(relationships, taxo).view.mapValues(_.retainedRelationships).toMap

    val floatingAssetsRels =
      calcRelationships.filter(_.targetConceptEName.localPart == "floatingAssets")

    assertResult(1) {
      floatingAssetsRels.size
    }

    assertResult(Map(BaseSetKey.forSummationItemArc(BaseSetKey.StandardElr) -> floatingAssetsRels)) {
      networkMap
    }
  }

  test("testCombineConceptLabelRelationships") {
    val docBuilder = getDocumentBuilder()

    val xsdDocUri = URI.create("file:///conf-suite/Common/200-linkbase/ArcOverrideDisjointLinkbases.xsd")
    val linkbaseDocUri1 = URI.create("file:///conf-suite/Common/200-linkbase/291-01-ArcOverrideDisjointLinkbases-1-label.xml")
    val linkbaseDocUri2 = URI.create("file:///conf-suite/Common/200-linkbase/291-01-ArcOverrideDisjointLinkbases-2-label.xml")

    val xsdBackingDoc = docBuilder.build(xsdDocUri)
    val linkbaseBackingDoc1 = docBuilder.build(linkbaseDocUri1)
    val linkbaseBackingDoc2 = docBuilder.build(linkbaseDocUri2)

    val xsdSchemaDoc = TaxonomyDocument.build(xsdBackingDoc)
    val linkbaseDoc1 = TaxonomyDocument.build(linkbaseBackingDoc1)
    val linkbaseDoc2 = TaxonomyDocument.build(linkbaseBackingDoc2)

    val taxo = TaxonomyBase.build(Vector(xsdSchemaDoc, linkbaseDoc1, linkbaseDoc2))

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val relationships = relationshipFactory.extractRelationships(taxo, RelationshipFactory.AnyArc)

    assertResult(2) {
      relationships.size
    }

    val relationshipKeys = relationships.map(rel => relationshipFactory.getRelationshipKey(rel, taxo))

    assertResult(2) {
      relationshipKeys.distinct.size
    }

    val networkMap = relationshipFactory.computeNetworks(relationships, taxo).view.mapValues(_.retainedRelationships).toMap

    assertResult(Map(BaseSetKey.forConceptLabelArc(BaseSetKey.StandardElr) -> relationships)) {
      networkMap
    }
  }

  test("testOverrideConceptLabelRelationships") {
    val docBuilder = getDocumentBuilder()

    val xsdDocUri = URI.create("file:///conf-suite/Common/200-linkbase/291-02-ArcOverrideLabelLinkbases.xsd")
    val linkbaseDocUri1 = URI.create("file:///conf-suite/Common/200-linkbase/291-02-ArcOverrideLabelLinkbases-1-label.xml")
    val linkbaseDocUri2 = URI.create("file:///conf-suite/Common/200-linkbase/291-02-ArcOverrideLabelLinkbases-2-label.xml")

    val xsdBackingDoc = docBuilder.build(xsdDocUri)
    val linkbaseBackingDoc1 = docBuilder.build(linkbaseDocUri1)
    val linkbaseBackingDoc2 = docBuilder.build(linkbaseDocUri2)

    val xsdSchemaDoc = TaxonomyDocument.build(xsdBackingDoc)
    val linkbaseDoc1 = TaxonomyDocument.build(linkbaseBackingDoc1)
    val linkbaseDoc2 = TaxonomyDocument.build(linkbaseBackingDoc2)

    val taxo = TaxonomyBase.build(Vector(xsdSchemaDoc, linkbaseDoc1, linkbaseDoc2))

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val relationships = relationshipFactory.extractRelationships(taxo, RelationshipFactory.AnyArc)

    assertResult(3) {
      relationships.size
    }

    val relationshipKeys = relationships.map(rel => relationshipFactory.getRelationshipKey(rel, taxo))

    assertResult(2) {
      relationshipKeys.distinct.size
    }

    val networkMap = relationshipFactory.computeNetworks(relationships, taxo).view.mapValues(_.retainedRelationships).toMap

    val filteredRelationships = relationships.filter(_.arc.priority == 2)

    assertResult(Map(BaseSetKey.forConceptLabelArc(BaseSetKey.StandardElr) -> filteredRelationships)) {
      networkMap
    }
  }

  test("testWrongOverrideConceptLabelRelationships") {
    val docBuilder = getDocumentBuilder()

    val xsdDocUri = URI.create("file:///conf-suite/Common/200-linkbase/291-03-ArcOverrideLabelLinkbases.xsd")
    val linkbaseDocUri1 = URI.create("file:///conf-suite/Common/200-linkbase/291-03-ArcOverrideLabelLinkbases-1-label.xml")
    val linkbaseDocUri2 = URI.create("file:///conf-suite/Common/200-linkbase/291-03-ArcOverrideLabelLinkbases-2-label.xml")
    val linkbaseDocUri3 = URI.create("file:///conf-suite/Common/200-linkbase/291-03-ArcOverrideLabelLinkbases-3-label.xml")

    val xsdBackingDoc = docBuilder.build(xsdDocUri)
    val linkbaseBackingDoc1 = docBuilder.build(linkbaseDocUri1)
    val linkbaseBackingDoc2 = docBuilder.build(linkbaseDocUri2)
    val linkbaseBackingDoc3 = docBuilder.build(linkbaseDocUri3)

    val xsdSchemaDoc = TaxonomyDocument.build(xsdBackingDoc)
    val linkbaseDoc1 = TaxonomyDocument.build(linkbaseBackingDoc1)
    val linkbaseDoc2 = TaxonomyDocument.build(linkbaseBackingDoc2)
    val linkbaseDoc3 = TaxonomyDocument.build(linkbaseBackingDoc3)

    val taxo = TaxonomyBase.build(Vector(xsdSchemaDoc, linkbaseDoc1, linkbaseDoc2, linkbaseDoc3))

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val relationships = relationshipFactory.extractRelationships(taxo, RelationshipFactory.AnyArc)

    assertResult(3) {
      relationships.size
    }

    val relationshipKeys = relationships.map(rel => relationshipFactory.getRelationshipKey(rel, taxo))

    assertResult(1) {
      relationshipKeys.distinct.size
    }

    // Assuming only concept-label relationships here

    val wrongRelationships =
      relationships.filter(rel => rel.resolvedTo.xlinkLocatorOrResource.isInstanceOf[XLinkLocator] && rel.arc.use == Use.Optional)

    assertResult(1) {
      wrongRelationships.size
    }

    // Useless network computation, because one arc is not allowed

    val networkMap = relationshipFactory.computeNetworks(relationships, taxo).view.mapValues(_.retainedRelationships).toMap

    assertResult(Map(BaseSetKey.forConceptLabelArc(BaseSetKey.StandardElr) -> relationships.filter(_.arc.priority == 2))) {
      networkMap
    }
  }

  test("testCombineDefinitionRelationships") {
    val docBuilder = getDocumentBuilder()

    val xsdDocUri = URI.create("file:///conf-suite/Common/200-linkbase/291-04-ArcOverrideDisjointLinkbases.xsd")
    val linkbaseDocUri1 = URI.create("file:///conf-suite/Common/200-linkbase/291-04-ArcOverrideLinkbases-1-def.xml")
    val linkbaseDocUri2 = URI.create("file:///conf-suite/Common/200-linkbase/291-04-ArcOverrideLinkbases-2-def.xml")

    val xsdBackingDoc = docBuilder.build(xsdDocUri)
    val linkbaseBackingDoc1 = docBuilder.build(linkbaseDocUri1)
    val linkbaseBackingDoc2 = docBuilder.build(linkbaseDocUri2)

    val xsdSchemaDoc = TaxonomyDocument.build(xsdBackingDoc)
    val linkbaseDoc1 = TaxonomyDocument.build(linkbaseBackingDoc1)
    val linkbaseDoc2 = TaxonomyDocument.build(linkbaseBackingDoc2)

    val taxo = TaxonomyBase.build(Vector(xsdSchemaDoc, linkbaseDoc1, linkbaseDoc2))

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val relationships = relationshipFactory.extractRelationships(taxo, RelationshipFactory.AnyArc)

    assertResult(2) {
      relationships.size
    }

    val relationshipKeys = relationships.map(rel => relationshipFactory.getRelationshipKey(rel, taxo))

    assertResult(2) {
      relationshipKeys.distinct.size
    }

    val networkMap = relationshipFactory.computeNetworks(relationships, taxo).view.mapValues(_.retainedRelationships).toMap

    assertResult(Map(BaseSetKey.forRequiresElementArc(BaseSetKey.StandardElr) -> relationships)) {
      networkMap
    }
  }

  test("testOverrideDefinitionRelationships") {
    val docBuilder = getDocumentBuilder()

    val xsdDocUri = URI.create("file:///conf-suite/Common/200-linkbase/291-05-ArcOverrideDisjointLinkbases.xsd")
    val linkbaseDocUri1 = URI.create("file:///conf-suite/Common/200-linkbase/291-05-ArcOverrideLinkbases-1-def.xml")
    val linkbaseDocUri2 = URI.create("file:///conf-suite/Common/200-linkbase/291-05-ArcOverrideLinkbases-2-def.xml")

    val xsdBackingDoc = docBuilder.build(xsdDocUri)
    val linkbaseBackingDoc1 = docBuilder.build(linkbaseDocUri1)
    val linkbaseBackingDoc2 = docBuilder.build(linkbaseDocUri2)

    val xsdSchemaDoc = TaxonomyDocument.build(xsdBackingDoc)
    val linkbaseDoc1 = TaxonomyDocument.build(linkbaseBackingDoc1)
    val linkbaseDoc2 = TaxonomyDocument.build(linkbaseBackingDoc2)

    val taxo = TaxonomyBase.build(Vector(xsdSchemaDoc, linkbaseDoc1, linkbaseDoc2))

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val relationships = relationshipFactory.extractRelationships(taxo, RelationshipFactory.AnyArc)

    assertResult(3) {
      relationships.size
    }

    val relationshipKeys = relationships.map(rel => relationshipFactory.getRelationshipKey(rel, taxo))

    assertResult(2) {
      relationshipKeys.distinct.size
    }

    val networkMap = relationshipFactory.computeNetworks(relationships, taxo).view.mapValues(_.retainedRelationships).toMap

    val filteredRelationships =
      relationships.filter(_.resolvedTo.resolvedElem.attributeOption(NameEName).contains("fixedAssets"))

    assertResult(Map(BaseSetKey.forRequiresElementArc(BaseSetKey.StandardElr) -> filteredRelationships)) {
      networkMap
    }
  }

  test("testProhibitAndInsertDefinitionRelationships") {
    val docBuilder = getDocumentBuilder()

    val xsdDocUri = URI.create("file:///conf-suite/Common/200-linkbase/291-06-ArcOverrideDisjointLinkbases.xsd")
    val linkbaseDocUri1 = URI.create("file:///conf-suite/Common/200-linkbase/291-06-ArcOverrideLinkbases-1-def.xml")
    val linkbaseDocUri2 = URI.create("file:///conf-suite/Common/200-linkbase/291-06-ArcOverrideLinkbases-2-def.xml")
    val linkbaseDocUri3 = URI.create("file:///conf-suite/Common/200-linkbase/291-06-ArcOverrideLinkbases-3-def.xml")

    val xsdBackingDoc = docBuilder.build(xsdDocUri)
    val linkbaseBackingDoc1 = docBuilder.build(linkbaseDocUri1)
    val linkbaseBackingDoc2 = docBuilder.build(linkbaseDocUri2)
    val linkbaseBackingDoc3 = docBuilder.build(linkbaseDocUri3)

    val xsdSchemaDoc = TaxonomyDocument.build(xsdBackingDoc)
    val linkbaseDoc1 = TaxonomyDocument.build(linkbaseBackingDoc1)
    val linkbaseDoc2 = TaxonomyDocument.build(linkbaseBackingDoc2)
    val linkbaseDoc3 = TaxonomyDocument.build(linkbaseBackingDoc3)

    val taxo = TaxonomyBase.build(Vector(xsdSchemaDoc, linkbaseDoc1, linkbaseDoc2, linkbaseDoc3))

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val relationships = relationshipFactory.extractRelationships(taxo, RelationshipFactory.AnyArc)

    assertResult(4) {
      relationships.size
    }

    val relationshipKeys = relationships.map(rel => relationshipFactory.getRelationshipKey(rel, taxo))

    // Mind the different arcroles
    assertResult(3) {
      relationshipKeys.distinct.size
    }

    val networkMap = relationshipFactory.computeNetworks(relationships, taxo).view.mapValues(_.retainedRelationships).toMap

    val filteredRequiresElementRelationships =
      relationships.collect {
        case rel: RequiresElementRelationship => rel
      }.filter(_.resolvedTo.resolvedElem.attributeOption(NameEName).contains("fixedAssets"))

    val filteredGeneralSpecialRelationships =
      relationships.collect { case rel: GeneralSpecialRelationship => rel }

    assertResult(
      Map(
        BaseSetKey.forRequiresElementArc(BaseSetKey.StandardElr) -> filteredRequiresElementRelationships,
        BaseSetKey.forGeneralSpecialArc(BaseSetKey.StandardElr) -> filteredGeneralSpecialRelationships)) {

      networkMap
    }
  }

  test("testWrongConceptLabelRelationship") {
    val docBuilder = getDocumentBuilder()

    val xsdDocUri = URI.create("file:///conf-suite/Common/200-linkbase/291-08-ArcOverrideLabelLinkbases.xsd")
    val linkbaseDocUri1 = URI.create("file:///conf-suite/Common/200-linkbase/291-08-ArcOverrideLabelLinkbases-1-label.xml")
    val linkbaseDocUri2 = URI.create("file:///conf-suite/Common/200-linkbase/291-08-ArcOverrideLabelLinkbases-2-label.xml")

    val xsdBackingDoc = docBuilder.build(xsdDocUri)
    val linkbaseBackingDoc1 = docBuilder.build(linkbaseDocUri1)
    val linkbaseBackingDoc2 = docBuilder.build(linkbaseDocUri2)

    val xsdSchemaDoc = TaxonomyDocument.build(xsdBackingDoc)
    val linkbaseDoc1 = TaxonomyDocument.build(linkbaseBackingDoc1)
    val linkbaseDoc2 = TaxonomyDocument.build(linkbaseBackingDoc2)

    val taxo = TaxonomyBase.build(Vector(xsdSchemaDoc, linkbaseDoc1, linkbaseDoc2))

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val relationships = relationshipFactory.extractRelationships(taxo, RelationshipFactory.AnyArc)

    assertResult(3) {
      relationships.size
    }
    assertResult(true) {
      relationships.forall(_.arc.resolvedName == LinkLabelArcEName)
    }
    assertResult(2) {
      (relationships.collect { case rel: ConceptLabelRelationship => rel }).size
    }
    // The corrupt concept-label relationship pointing to a target concept instead of label
    assertResult(1) {
      (relationships.collect { case rel: UnknownRelationship => rel }).size
    }

    val relationshipKeys = relationships.map(rel => relationshipFactory.getRelationshipKey(rel, taxo))

    assertResult(3) {
      relationshipKeys.distinct.size
    }
  }

  private val zipFile: ZipFile = {
    val uri = classOf[RelationshipEquivalenceTest].getResource("/XBRL-CONF-2014-12-10.zip").toURI
    new ZipFile(new File(uri))
  }

  private def getDocumentBuilder(): IndexedDocumentBuilder = {
    val docParser = DocumentParserUsingStax.newInstance()

    val catalog: SimpleCatalog =
      SimpleCatalog(
        None,
        Vector(
          SimpleCatalog.UriRewrite(None, "file:///conf-suite/", "XBRL-CONF-2014-12-10/")))

    val uriResolver = UriResolvers.forZipFileUsingCatalogWithFallback(zipFile, catalog)

    IndexedDocumentBuilder(docParser, uriResolver)
  }
}
