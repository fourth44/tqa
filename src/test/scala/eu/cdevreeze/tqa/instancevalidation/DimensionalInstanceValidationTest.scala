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

package eu.cdevreeze.tqa.instancevalidation

import java.io.File
import java.net.URI

import scala.util.Success

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.tqa.backingelem.UriConverters
import eu.cdevreeze.tqa.backingelem.nodeinfo.SaxonDocumentBuilder
import eu.cdevreeze.tqa.base.common.ContextElement
import eu.cdevreeze.tqa.base.relationship.DefaultRelationshipFactory
import eu.cdevreeze.tqa.base.taxonomybuilder.DefaultDtsCollector
import eu.cdevreeze.tqa.base.taxonomybuilder.TaxonomyBuilder
import eu.cdevreeze.tqa.instance.XbrlInstance
import eu.cdevreeze.yaidom.core.EName
import net.sf.saxon.s9api.Processor

/**
 * Dimensional instance validation test case. It uses test data from the XBRL Dimensions conformance suite.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class DimensionalInstanceValidationTest extends FunSuite {

  import DimensionalContext.contextToDimensionalContext

  // 202-DefaultValueUsedInInstanceError

  test("testDefaultValueInInstanceOK") {
    val instance = makeTestInstance("200-xbrldie/202-DefaultValueUsedInInstanceError/defaultValueInInstanceOK.xbrl")
    val validator = makeValidator(instance)

    val tns = "http://xbrl.org/dims/conformance"
    val productTns = "http://www.xbrl.org/dim/conf/product"

    assertResult(true) {
      validator.dimensionDefaults.get(EName(tns, "ProductDim")).contains(EName(productTns, "AllProducts"))
    }

    assertResult(Set(EName(productTns, "Cars"), EName(productTns, "Wine"))) {
      instance.allContexts.flatMap(_.explicitDimensionMembers.get(EName(tns, "ProductDim"))).toSet
    }

    val dimContexts = instance.allContexts.map(contextToDimensionalContext)

    assertResult(3) {
      dimContexts.size
    }
    assertResult(3) {
      dimContexts.map(ctx => validator.validateDimensionalContext(ctx)).count(_.isSuccess)
    }
  }

  test("testDefaultValueInInstance") {
    val instance = makeTestInstance("200-xbrldie/202-DefaultValueUsedInInstanceError/defaultValueInInstance.xbrl")
    val validator = makeValidator(instance)

    val tns = "http://xbrl.org/dims/conformance"
    val productTns = "http://www.xbrl.org/dim/conf/product"

    assertResult(true) {
      validator.dimensionDefaults.get(EName(tns, "ProductDim")).contains(EName(productTns, "AllProducts"))
    }

    assertResult(Set(EName(productTns, "AllProducts"), EName(productTns, "Cars"), EName(productTns, "Wine"))) {
      instance.allContexts.flatMap(_.explicitDimensionMembers.get(EName(tns, "ProductDim"))).toSet
    }

    val dimContexts = instance.allContexts.map(contextToDimensionalContext)

    assertResult(3) {
      dimContexts.size
    }
    assertResult(1) {
      dimContexts.map(ctx => validator.validateDimensionalContext(ctx)).count(_.isFailure)
    }
    intercept[DefaultValueUsedInInstanceError] {
      dimContexts.map(ctx => validator.validateDimensionalContext(ctx)).find(_.isFailure).get.get
    }
  }

  // 203-PrimaryItemDimensionallyInvalidError

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-01.
   */
  test("testCombinationOfCubesCase1Segment") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/combinationOfCubesCase1Segment.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-02.
   */
  test("testCombinationOfCubesCase6Segment") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/combinationOfCubesCase6Segment.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-03.
   */
  test("testCombinationOfCubesCase2Segment") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/combinationOfCubesCase2Segment.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-04.
   */
  test("testCombinationOfCubesCase3Segment") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/combinationOfCubesCase3Segment.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-05.
   */
  test("testCombinationOfCubesCase4Segment") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/combinationOfCubesCase4Segment.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-06.
   */
  test("testCombinationOfCubesCase5Segment") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/combinationOfCubesCase5Segment.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-07.
   */
  test("testContextForbiddenExplicitDimInSegment") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/contextForbiddenExplicitDimInSegment.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(2) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true), Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-08.
   */
  test("testContextForbiddenExplicitDimMemberInScenario") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/contextForbiddenExplicitDimInScenario.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(2) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true), Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-09.
   */
  test("testContextImplicitDimNotInSegment") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/contextImplicitDimNotInSegment.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-10.
   */
  test("testContextImplicitDimNotInScenario") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/contextImplicitDimNotInScenario.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-11.
   */
  test("testContextExplicitDimNotInSegment") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/contextExplicitDimNotInSegment.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-12.
   */
  test("testContextExplicitDimNotInScenario") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/contextExplicitDimNotInScenario.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-13.
   */
  test("testCombinationOfCubesUnusableMemberInvalid") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/combinationOfCubesUnusableMemberInvalid.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-14.
   */
  test("testCombinationOfCubesUnusableDomainInvalid") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/combinationOfCubesUnusableDomainInvalid.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-15.
   */
  test("testClosedEmptyHypercubeIsValid") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/closedEmptyHypercubeIsValid.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-16.
   */
  test("testOpenEmptyHypercubeIsValid") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/openEmptyHypercubeIsValid.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-17.
   */
  test("testClosedEmptyHypercubeIsInvalid") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/closedEmptyHypercubeIsInvalid.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-18.
   */
  test("testClosedNotEmptyHypercubeIsValid") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/closedNotEmptyHypercubeIsValid.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-19.
   */
  test("testClosedNotEmptyHypercubeIsInvalid") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/closedNotEmptyHypercubeIsInvalid.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-20.
   */
  test("testOpenNotEmptyHypercubeIsValid") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/openNotEmptyHypercubeIsValid.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-21.
   */
  test("testInstanceValidAccordingToTwoDimensionalTaxonomies") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/InstanceValidAccordingToTwoDimensionalTaxonomies.xbrl").
        ensuring(_.findAllSchemaRefs.size == 3).ensuring(_.findAllLinkbaseRefs.size == 1)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-22.
   */
  test("testInstanceInvalidAccordingToTwoDimensionalTaxonomies") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/InstanceInvalidAccordingToTwoDimensionalTaxonomies.xbrl").
        ensuring(_.findAllSchemaRefs.size == 3).ensuring(_.findAllLinkbaseRefs.size == 1)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-23.
   */
  test("testInstanceInvalidTheValueIsNotADomainMember") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/InstanceInvalidTheValueIsNotADomainMember.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-24.
   */
  test("testInstanceInvalidTheValueIsNotADomainMemberCase2") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/InstanceInvalidTheValueIsNotADomainMemberCase2.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-25.
   */
  test("testComplexHypercubeInheritance") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/complexHypercubeInheritance_ValidLink1.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(4) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true), Success(true), Success(true), Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-26.
   */
  test("testComplexHypercubeInheritance2") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/complexHypercubeInheritance_ValidLink2.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(4) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true), Success(true), Success(true), Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-27.
   */
  test("testComplexHypercubeInheritance3") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/complexHypercubeInheritance_Invalid3.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(4) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false), Success(false), Success(false), Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-28.
   */
  test("testMemberExcludedFromValidDomain1") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/memberExcludedFromValidDomain1.xbrl").
        ensuring(_.findAllSchemaRefs.size == 2).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-29.
   */
  test("testMemberExcludedFromValidDomain2") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/memberExcludedFromValidDomain2.xbrl").
        ensuring(_.findAllSchemaRefs.size == 2).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-30.
   */
  test("testPrimaryItemValidContainsNoHypercubes") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/primaryItemValidContainsNoHypercubes.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-31.
   */
  test("testCombinationOfCubesCase7BothOpen") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/combinationOfCubesCase7BothOpen.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-32.
   */
  test("testCombinationOfCubesCase8NotAllClosed") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/combinationOfCubesCase8NotAllClosed.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-33.
   */
  test("testClosedHypercubeAndDefaultMembers") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/closedHypercubeAndDefaultMembers.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-34.
   */
  test("testInheritanceEdgeUseCase-p1-c1") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/inheritanceEdgeUseCase-p1-c1.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-35.
   */
  test("testInheritanceEdgeUseCase-p1-c2") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/inheritanceEdgeUseCase-p1-c2.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-36.
   */
  test("testInheritanceEdgeUseCase-p2-c1") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/inheritanceEdgeUseCase-p2-c1.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-37.
   */
  test("testInheritanceEdgeUseCase-p2-c2") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/inheritanceEdgeUseCase-p2-c2.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-38.
   */
  test("testInheritanceEdgeUseCase-p3-c1") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/inheritanceEdgeUseCase-p3-c1.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-39.
   */
  test("testInheritanceEdgeUseCase-p3-c2") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/inheritanceEdgeUseCase-p3-c2.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-40.
   */
  test("testInheritanceEdgeUseCase-p5-c1") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/inheritanceEdgeUseCase-p5-c1.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-41.
   */
  test("testInheritanceEdgeUseCase-p5-c2") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/inheritanceEdgeUseCase-p5-c2.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-42.
   */
  test("testInheritanceEdgeUseCase-p9-c1") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/inheritanceEdgeUseCase-p9-c1.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-43.
   */
  test("testInheritanceEdgeUseCase-p9-c2") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/inheritanceEdgeUseCase-p9-c2.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-44.
   */
  test("testInheritanceEdgeUseCase-p10-c1") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/inheritanceEdgeUseCase-p10-c1.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(false))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-45.
   */
  test("testInheritanceEdgeUseCase-p10-c2") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/inheritanceEdgeUseCase-p10-c2.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  /**
   * Test 203-TestCase-PrimaryItemDimensionallyInvalidError, variation V-46.
   */
  test("testInheritanceEdgeUseCase-p11-c1") {
    val instance =
      makeTestInstance("200-xbrldie/203-PrimaryItemDimensionallyInvalidError/inheritanceEdgeUseCase-p11-c1.xbrl").
        ensuring(_.findAllSchemaRefs.size == 1).ensuring(_.findAllLinkbaseRefs.size == 0)
    val validator = makeValidator(instance)

    assertResult(1) {
      instance.allTopLevelItems.size
    }
    assertResult(List(Success(true))) {
      instance.allTopLevelItems.map(fact => validator.validateDimensionally(fact, instance))
    }
  }

  // 204-RepeatedDimensionInInstanceError

  test("testContextContainsTypedDimensionValid") {
    val instance = makeTestInstance("200-xbrldie/204-RepeatedDimensionInInstanceError/contextContainsTypedDimensionValid.xbrl")
    val validator = makeValidator(instance)

    assertResult(List(ContextElement.Segment)) {
      validator.taxonomy.findAllHasHypercubeRelationships.map(_.contextElement)
    }

    val dimContexts = instance.allContexts.map(contextToDimensionalContext)

    assertResult(1) {
      dimContexts.size
    }
    assertResult(1) {
      instance.allContexts.flatMap(_.entity.segmentOption).flatMap(_.typedMembers).size
    }
    assertResult(1) {
      instance.allContexts.flatMap(_.entity.segmentOption).flatMap(_.typedMembers).map(_.dimension).distinct.size
    }
    assertResult(List(false)) {
      dimContexts.map(_.hasRepeatedDimensions)
    }

    assertResult(1) {
      dimContexts.map(ctx => validator.validateDimensionalContext(ctx)).count(_.isSuccess)
    }
  }

  test("testContextContainsRepeatedDimension") {
    val instance = makeTestInstance("200-xbrldie/204-RepeatedDimensionInInstanceError/contextContainsRepeatedDimension.xbrl")
    val validator = makeValidator(instance)

    val dimContexts = instance.allContexts.map(contextToDimensionalContext)

    assertResult(1) {
      dimContexts.size
    }
    assertResult(2) {
      instance.allContexts.flatMap(_.entity.segmentOption).flatMap(_.typedMembers).size
    }
    assertResult(1) {
      instance.allContexts.flatMap(_.entity.segmentOption).flatMap(_.typedMembers).map(_.dimension).distinct.size
    }
    assertResult(List(true)) {
      dimContexts.map(_.hasRepeatedDimensions)
    }

    assertResult(1) {
      dimContexts.map(ctx => validator.validateDimensionalContext(ctx)).count(_.isFailure)
    }
    intercept[RepeatedDimensionInInstanceError.type] {
      dimContexts.map(ctx => validator.validateDimensionalContext(ctx)).find(_.isFailure).get.get
    }
  }

  test("testBiLocatableExplicitDimInSeg") {
    val instance = makeTestInstance("200-xbrldie/204-RepeatedDimensionInInstanceError/bi-locational-seg-explicit-instance.xml")
    val validator = makeValidator(instance)

    assertResult(Set(ContextElement.Segment, ContextElement.Scenario)) {
      validator.taxonomy.findAllHasHypercubeRelationships.map(_.contextElement).toSet
    }

    assertResult(Set(ContextElement.Segment, ContextElement.Scenario)) {
      instance.allTopLevelItems.flatMap(fact => validator.taxonomy.findAllOwnOrInheritedHasHypercubes(fact.resolvedName)).
        map(_.contextElement).toSet
    }

    val dimContexts = instance.allContexts.map(contextToDimensionalContext)

    assertResult(1) {
      dimContexts.size
    }
    assertResult(1) {
      instance.allContexts.flatMap(_.entity.segmentOption).flatMap(_.explicitMembers).size
    }
    assertResult(List(false)) {
      dimContexts.map(_.hasRepeatedDimensions)
    }

    assertResult(1) {
      dimContexts.map(ctx => validator.validateDimensionalContext(ctx)).count(_.isSuccess)
    }
  }

  test("testBiLocatableExplicitDimInSegAndScen") {
    val instance = makeTestInstance("200-xbrldie/204-RepeatedDimensionInInstanceError/bi-locational-dual-explicit-instance.xml")
    val validator = makeValidator(instance)

    assertResult(Set(ContextElement.Segment, ContextElement.Scenario)) {
      validator.taxonomy.findAllHasHypercubeRelationships.map(_.contextElement).toSet
    }

    assertResult(Set(ContextElement.Segment, ContextElement.Scenario)) {
      instance.allTopLevelItems.flatMap(fact => validator.taxonomy.findAllOwnOrInheritedHasHypercubes(fact.resolvedName)).
        map(_.contextElement).toSet
    }

    val dimContexts = instance.allContexts.map(contextToDimensionalContext)

    assertResult(1) {
      dimContexts.size
    }
    assertResult(List(true)) {
      dimContexts.map(_.hasRepeatedDimensions)
    }

    assertResult(1) {
      dimContexts.map(ctx => validator.validateDimensionalContext(ctx)).count(_.isFailure)
    }
    intercept[RepeatedDimensionInInstanceError.type] {
      dimContexts.map(ctx => validator.validateDimensionalContext(ctx)).find(_.isFailure).get.get
    }
  }

  test("testRepeatedDimensionInScenario") {
    val instance = makeTestInstance("200-xbrldie/204-RepeatedDimensionInInstanceError/repeatedDimensionInScenario.xbrl")

    val dimContexts = instance.allContexts.map(contextToDimensionalContext)

    assertResult(1) {
      dimContexts.size
    }
    assertResult(List(true)) {
      dimContexts.map(_.hasRepeatedDimensions)
    }
    assertResult(List(true)) {
      dimContexts.map(_.dimensionalScenario).map(_.hasRepeatedDimensions)
    }
    assertResult(List(false)) {
      dimContexts.map(_.dimensionalSegment).map(_.hasRepeatedDimensions)
    }
  }

  // Helper methods

  private def makeValidator(xbrlInstance: XbrlInstance): DimensionalValidator = {
    val entrypointHrefs =
      xbrlInstance.findAllSchemaRefs.map(_.resolvedHref) ++ xbrlInstance.findAllLinkbaseRefs.map(_.resolvedHref)

    doMakeValidator(entrypointHrefs.toSet.filterNot(Set(URI.create("http://www.xbrl.org/2006/xbrldi-2006.xsd"))))
  }

  private def makeTestInstance(relativeDocPath: String): XbrlInstance = {
    val rootDir = new File(classOf[DimensionalInstanceValidationTest].getResource("/conf-suite-dim").toURI)
    val docFile = new File(rootDir, relativeDocPath)

    XbrlInstance(docBuilder.build(docFile.toURI))
  }

  private def doMakeValidator(entrypointUris: Set[URI]): DimensionalValidator = {
    val documentCollector = DefaultDtsCollector(entrypointUris)

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val taxoBuilder =
      TaxonomyBuilder.
        withDocumentBuilder(docBuilder).
        withDocumentCollector(documentCollector).
        withRelationshipFactory(relationshipFactory)

    val basicTaxo = taxoBuilder.build()
    val effectiveTaxo = basicTaxo.resolveProhibitionAndOverriding(relationshipFactory)

    DimensionalValidator.build(effectiveTaxo)
  }

  private val processor = new Processor(false)

  private val docBuilder = {
    val otherRootDir = new File(classOf[DimensionalInstanceValidationTest].getResource("/xbrl-and-w3").toURI)

    new SaxonDocumentBuilder(processor.newDocumentBuilder(), { uri =>
      if (uri.getScheme == "http" || uri.getScheme == "https") {
        UriConverters.uriToLocalUri(uri, otherRootDir)
      } else {
        uri
      }
    })
  }
}
