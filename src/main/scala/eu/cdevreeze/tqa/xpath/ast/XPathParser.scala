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

package eu.cdevreeze.tqa.xpath.ast

import eu.cdevreeze.yaidom.core.QName
import fastparse.WhitespaceApi

/**
 * XPath 3.0 parsing support, using FastParse.
 *
 * Usage: XPathParser.xpathExpr.parse(xpathString)
 *
 * @author Chris de Vreeze
 */
object XPathParser {

  // TODO Improve, improve, improve. Study XPath spec more closely, use FastParse in a better way, make code complete and more robust, improve the AST class hierarchy, etc.

  import XPathExpressions._

  private val White = WhitespaceApi.Wrapper {
    import fastparse.all._

    NoTrace(CharPred(c => java.lang.Character.isWhitespace(c)).rep) // TODO Adapt. What about parsing of comments?
  }

  import White._
  import fastparse.noApi._

  val xpathExpr: P[XPathExpr] =
    P(expr ~ End) map (e => XPathExpr(e)) // TODO Make this work if there is more than one comma-separated ExprSingle

  private val expr: P[Expr] =
    P(exprSingle.rep(min = 1, sep = ",")) map {
      case (exprs) => Expr(exprs.toIndexedSeq)
    }

  private val enclosedExpr: P[EnclosedExpr] =
    P("{" ~ expr ~ "}") map {
      case exp => EnclosedExpr(exp)
    }

  private val exprSingle: P[ExprSingle] =
    P(forExpr | letExpr | quantifiedExpr | ifExpr | orExpr)

  private val forExpr: P[ForExpr] =
    P("for" ~ simpleForBinding.rep(min = 1, sep = ",") ~ "return" ~ exprSingle) map {
      case (bindings, returnExp) => ForExpr(bindings.toIndexedSeq, returnExp)
    }

  private val simpleForBinding: P[SimpleForBinding] =
    P("$" ~ eqName ~ "in" ~ exprSingle) map {
      case (eqn, exp) => SimpleForBinding(eqn, exp)
    }

  private val letExpr: P[LetExpr] =
    P("let" ~ simpleLetBinding.rep(min = 1, sep = ",") ~ "return" ~ exprSingle) map {
      case (bindings, returnExp) => LetExpr(bindings.toIndexedSeq, returnExp)
    }

  private val simpleLetBinding: P[SimpleLetBinding] =
    P("$" ~ eqName ~ ":=" ~ exprSingle) map {
      case (eqn, exp) => SimpleLetBinding(eqn, exp)
    }

  private val quantifiedExpr: P[QuantifiedExpr] =
    P(("some" | "every").! ~ simpleBindingInQuantifiedExpr.rep(min = 1, sep = ",") ~ "satisfies" ~ exprSingle) map {
      case (quant, bindings, satisfiesExp) => QuantifiedExpr(Quantifier.parse(quant), bindings.toIndexedSeq, satisfiesExp)
    }

  private val simpleBindingInQuantifiedExpr: P[SimpleBindingInQuantifiedExpr] =
    P("$" ~ eqName ~ "in" ~ exprSingle) map {
      case (eqn, exp) => SimpleBindingInQuantifiedExpr(eqn, exp)
    }

  private val ifExpr: P[IfExpr] =
    P("if" ~ "(" ~ expr ~ ")" ~ "then" ~ exprSingle ~ "else" ~ exprSingle) map {
      case (e1, e2, e3) => IfExpr(e1, e2, e3)
    }

  private val orExpr: P[OrExpr] =
    P(andExpr.rep(min = 1, sep = "or")) map {
      case exps => OrExpr(exps.toIndexedSeq)
    }

  private val andExpr: P[AndExpr] =
    P(comparisonExpr.rep(min = 1, sep = "and")) map {
      case exps => AndExpr(exps.toIndexedSeq)
    }

  private val comparisonExpr: P[ComparisonExpr] =
    P(stringConcatExpr ~ ((valueComp | generalComp | nodeComp) ~ stringConcatExpr).?) map {
      case (expr1, Some((op, expr2))) => CompoundComparisonExpr(expr1, op, expr2)
      case (expr, None)               => SimpleComparisonExpr(expr)
    }

  private val stringConcatExpr: P[StringConcatExpr] =
    P(rangeExpr.rep(min = 1, sep = "||")) map {
      case exps => StringConcatExpr(exps.toIndexedSeq)
    }

  private val rangeExpr: P[RangeExpr] =
    P(additiveExpr ~ ("to" ~ additiveExpr).?) map {
      case (additiveExp1, Some(additiveExp2)) => CompoundRangeExpr(additiveExp1, additiveExp2)
      case (additiveExp, None)                => SimpleRangeExpr(additiveExp)
    }

  private val additiveExpr: P[AdditiveExpr] =
    P(multiplicativeExpr ~ (("+" | "-").! ~ additiveExpr).?) map {
      case (expr, None)            => SimpleAdditiveExpr(expr)
      case (expr, Some(opAndExpr)) => CompoundAdditiveExpr(expr, AdditionOp.parse(opAndExpr._1), opAndExpr._2)
    }

  private val multiplicativeExpr: P[MultiplicativeExpr] =
    P(unionExpr ~ (("*" | "div" | "idiv" | "mod").! ~ multiplicativeExpr).?) map {
      case (expr, None)            => SimpleMultiplicativeExpr(expr)
      case (expr, Some(opAndExpr)) => CompoundMultiplicativeExpr(expr, MultiplicativeOp.parse(opAndExpr._1), opAndExpr._2)
    }

  private val unionExpr: P[UnionExpr] =
    P(intersectExceptExpr ~ (("union" | "|") ~ intersectExceptExpr).rep) map {
      case (expr, exprSeq) => UnionExpr(expr +: exprSeq.toIndexedSeq)
    }

  private val intersectExceptExpr: P[IntersectExceptExpr] =
    P(instanceOfExpr ~ (("intersect" | "except").! ~ intersectExceptExpr).?) map {
      case (expr, None)            => SimpleIntersectExceptExpr(expr)
      case (expr, Some(opAndExpr)) => CompoundIntersectExceptExpr(expr, IntersectExceptOp.parse(opAndExpr._1), opAndExpr._2)
    }

  private val instanceOfExpr: P[InstanceOfExpr] =
    P(treatExpr ~ ("instance" ~ "of" ~ sequenceType).?) map {
      case (expr, tpeOption) => InstanceOfExpr(expr, tpeOption)
    }

  private val treatExpr: P[TreatExpr] =
    P(castableExpr ~ ("treat" ~ "as" ~ sequenceType).?) map {
      case (expr, tpeOption) => TreatExpr(expr, tpeOption)
    }

  private val castableExpr: P[CastableExpr] =
    P(castExpr ~ ("castable" ~ "as" ~ singleType).?) map {
      case (expr, tpeOption) => CastableExpr(expr, tpeOption)
    }

  private val castExpr: P[CastExpr] =
    P(unaryExpr ~ ("cast" ~ "as" ~ singleType).?) map {
      case (expr, tpeOption) => CastExpr(expr, tpeOption)
    }

  private val unaryExpr: P[UnaryExpr] =
    P(("-" | "+").!.rep ~ valueExpr) map {
      case (ops, expr) => UnaryExpr(ops.toIndexedSeq.map(op => UnaryOp.parse(op)), expr)
    }

  private val valueExpr: P[ValueExpr] =
    P(simpleMapExpr) map {
      case expr => ValueExpr(expr)
    }

  private val simpleMapExpr: P[SimpleMapExpr] =
    P(pathExpr.rep(min = 1, sep = "|")) map {
      case exps => SimpleMapExpr(exps.toIndexedSeq)
    }

  private val pathExpr: P[PathExpr] =
    P(slashOnlyPathExpr | pathExprStartingWithSingleSlash | pathExprStartingWithDoubleSlash | relativePathExpr)

  // Lookahead parsers

  private val canStartRelativePathExpr: P[Unit] =
    P(canStartAxisStep | canStartPostfixExpr)

  private val canStartAxisStep: P[Unit] =
    P(forwardAxis | reverseAxis).map(_ => ())

  private val canStartPostfixExpr: P[Unit] =
    P(literal | varRef | "(" | contextItemExpr | eqName | "function").map(_ => ())

  // Looking ahead to distinguish single slash from double slash, and to recognize start of relativePathExpr.
  // See xgc:leading-lone-slash constraint.

  private val slashOnlyPathExpr: P[PathExpr] =
    P("/" ~ !("/" | canStartRelativePathExpr)) map {
      case _ => SlashOnlyPathExpr
    }

  // Looking ahead to distinguish single slash from double slash, and to recognize start of relativePathExpr.
  // See xgc:leading-lone-slash constraint. Note that canStartRelativePathExpr implies that the next token is not a slash!

  private val pathExprStartingWithSingleSlash: P[PathExpr] =
    P("/" ~ &(canStartRelativePathExpr) ~ relativePathExpr) map {
      case expr => PathExprStartingWithSingleSlash(expr)
    }

  private val pathExprStartingWithDoubleSlash: P[PathExpr] =
    P("//" ~ relativePathExpr) map {
      case expr => PathExprStartingWithDoubleSlash(expr)
    }

  private val relativePathExpr: P[RelativePathExpr] =
    P(stepExpr ~ (("/" | "//").! ~ relativePathExpr).?) map {
      case (expr, None)            => SimpleRelativePathExpr(expr)
      case (expr, Some(opAndExpr)) => CompoundRelativePathExpr(expr, StepOp.parse(opAndExpr._1), opAndExpr._2)
    }

  private val stepExpr: P[StepExpr] =
    P(postfixExpr | axisStep)

  private val axisStep: P[AxisStep] =
    P(forwardAxisStep | reverseAxisStep)

  private val forwardAxisStep: P[ForwardAxisStep] =
    P(forwardStep ~ predicate.rep) map {
      case (forwardStep, predicates) => ForwardAxisStep(forwardStep, predicates.toIndexedSeq)
    }

  private val reverseAxisStep: P[ReverseAxisStep] =
    P(reverseStep ~ predicate.rep) map {
      case (reverseStep, predicates) => ReverseAxisStep(reverseStep, predicates.toIndexedSeq)
    }

  private val forwardStep: P[ForwardStep] =
    P(nonAbbrevForwardStep | abbrevForwardStep)

  private val abbrevForwardStep: P[AbbrevForwardStep] =
    P(simpleAbbrevForwardStep | attributeAxisAbbrevForwardStep)

  private val simpleAbbrevForwardStep: P[SimpleAbbrevForwardStep] =
    P(nodeTest) map {
      case nodeTest => SimpleAbbrevForwardStep(nodeTest)
    }

  private val attributeAxisAbbrevForwardStep: P[AttributeAxisAbbrevForwardStep] =
    P("@" ~ nodeTest) map {
      case nodeTest => AttributeAxisAbbrevForwardStep(nodeTest)
    }

  private val nonAbbrevForwardStep: P[NonAbbrevForwardStep] =
    P(forwardAxis ~ nodeTest) map {
      case (axis, nodeTest) => NonAbbrevForwardStep(axis, nodeTest)
    }

  private val forwardAxis: P[ForwardAxis] =
    P(("child" | "descendant" | "attribute" | "self" | "descendant-or-self" | "following-sibling" | "following" | "namespace").! ~ "::") map {
      case "child"              => ForwardAxis.Child
      case "descendant"         => ForwardAxis.Descendant
      case "attribute"          => ForwardAxis.Attribute
      case "self"               => ForwardAxis.Self
      case "descendant-or-self" => ForwardAxis.DescendantOrSelf
      case "following-sibling"  => ForwardAxis.FollowingSibling
      case "following"          => ForwardAxis.Following
      case "namespace"          => ForwardAxis.Namespace
    }

  private val reverseStep: P[ReverseStep] =
    P(nonAbbrevReverseStep | abbrevReverseStep)

  private val abbrevReverseStep: P[AbbrevReverseStep.type] =
    P("..") map (_ => AbbrevReverseStep)

  private val nonAbbrevReverseStep: P[NonAbbrevReverseStep] =
    P(reverseAxis ~ nodeTest) map {
      case (axis, nodeTest) => NonAbbrevReverseStep(axis, nodeTest)
    }

  private val reverseAxis: P[ReverseAxis] =
    P(("parent" | "ancestor" | "preceding-sibling" | "preceding" | "ancestor-or-self").! ~ "::") map {
      case "parent"            => ReverseAxis.Parent
      case "ancestor"          => ReverseAxis.Ancestor
      case "preceding-sibling" => ReverseAxis.PrecedingSibling
      case "preceding"         => ReverseAxis.Preceding
      case "ancestor-or-self"  => ReverseAxis.AncestorOrSelf
    }

  private val nodeTest: P[NodeTest] =
    P(kindTest | nameTest)

  private val nameTest: P[NameTest] =
    P(simpleNameTest | wildcard)

  private val simpleNameTest: P[SimpleNameTest] =
    P(eqName) map {
      case name => SimpleNameTest(name)
    }

  // See ws:explicit constraint.

  private val wildcard: P[Wildcard] =
    P(anyWildcard | prefixWildcard | localNameWildcard | namespaceWildcard)

  private val anyWildcard: P[AnyWildcard.type] =
    P(CharsWhileIn("*:").!) filter (s => s == "*") map (_ => AnyWildcard)

  private val prefixWildcard: P[PrefixWildcard] =
    P(CharsWhile(isNCNameCharOrColonOrStar).!) filter (isPrefixWildcard) map (v => PrefixWildcard(NCName(v.dropRight(2))))

  private val localNameWildcard: P[LocalNameWildcard] =
    P(CharsWhile(isNCNameCharOrColonOrStar).!) filter (isLocalNameWildcard) map (v => LocalNameWildcard(NCName(v.drop(2))))

  private val namespaceWildcard: P[NamespaceWildcard] =
    P(CharsWhile(isNCNameCharOrBraceOrStar).!) filter (isNamespaceWildcard) map (v => NamespaceWildcard(BracedUriLiteral.parse(v.dropRight(1))))

  private val kindTest: P[KindTest] =
    P(documentTest | elementTest | attributeTest | schemaElementTest | schemaAttributeTest | piTest | commentTest | textTest | namespaceNodeTest | anyKindTest)

  private val documentTest: P[DocumentTest] =
    P(simpleDocumentTest | documentTestContainingElementTest | documentTestContainingSchemaElementTest)

  private val simpleDocumentTest: P[SimpleDocumentTest.type] =
    P("document-node" ~ "(" ~ ")") map (_ => SimpleDocumentTest)

  private val documentTestContainingElementTest: P[DocumentTestContainingElementTest] =
    P("document-node" ~ "(" ~ elementTest ~ ")") map {
      case elemTest => DocumentTestContainingElementTest(elemTest)
    }

  private val documentTestContainingSchemaElementTest: P[DocumentTestContainingSchemaElementTest] =
    P("document-node" ~ "(" ~ schemaElementTest ~ ")") map {
      case schemaElmTest => DocumentTestContainingSchemaElementTest(schemaElmTest)
    }

  private val elementTest: P[ElementTest] =
    P(anyElementTest | elementNameTest | elementNameAndTypeTest | nillableElementNameAndTypeTest | elementTypeTest | nillableElementTypeTest)

  // Losing some efficiency on parsing of element tests

  private val anyElementTest: P[AnyElementTest.type] =
    P("element" ~ "(" ~ "*".? ~ ")") map (_ => AnyElementTest)

  private val elementNameTest: P[ElementNameTest] =
    P("element" ~ "(" ~ eqName ~ ")") map {
      case name => ElementNameTest(name)
    }

  private val elementNameAndTypeTest: P[ElementNameAndTypeTest] =
    P("element" ~ "(" ~ eqName ~ "," ~ eqName ~ ")") map {
      case (name, tpe) => ElementNameAndTypeTest(name, tpe)
    }

  private val nillableElementNameAndTypeTest: P[NillableElementNameAndTypeTest] =
    P("element" ~ "(" ~ eqName ~ "," ~ eqName ~ "?" ~ ")") map {
      case (name, tpe) => NillableElementNameAndTypeTest(name, tpe)
    }

  private val elementTypeTest: P[ElementTypeTest] =
    P("element" ~ "(" ~ "*" ~ "," ~ eqName ~ ")") map {
      case tpe => ElementTypeTest(tpe)
    }

  private val nillableElementTypeTest: P[NillableElementTypeTest] =
    P("element" ~ "(" ~ "*" ~ "," ~ eqName ~ "?" ~ ")") map {
      case tpe => NillableElementTypeTest(tpe)
    }

  private val attributeTest: P[AttributeTest] =
    P(anyAttributeTest | attributeNameTest | attributeNameAndTypeTest | attributeTypeTest)

  // Losing some efficiency on parsing of attribute tests

  private val anyAttributeTest: P[AnyAttributeTest.type] =
    P("attribute" ~ "(" ~ "*".? ~ ")") map (_ => AnyAttributeTest)

  private val attributeNameTest: P[AttributeNameTest] =
    P("attribute" ~ "(" ~ eqName ~ ")") map {
      case name => AttributeNameTest(name)
    }

  private val attributeNameAndTypeTest: P[AttributeNameAndTypeTest] =
    P("attribute" ~ "(" ~ eqName ~ "," ~ eqName ~ ")") map {
      case (name, tpe) => AttributeNameAndTypeTest(name, tpe)
    }

  private val attributeTypeTest: P[AttributeTypeTest] =
    P("attribute" ~ "(" ~ "*" ~ "," ~ eqName ~ ")") map {
      case tpe => AttributeTypeTest(tpe)
    }

  private val schemaElementTest: P[SchemaElementTest] =
    P("schema-element" ~ "(" ~ eqName ~ ")") map {
      case name => SchemaElementTest(name)
    }

  private val schemaAttributeTest: P[SchemaAttributeTest] =
    P("schema-attribute" ~ "(" ~ eqName ~ ")") map {
      case name => SchemaAttributeTest(name)
    }

  private val piTest: P[PITest] =
    P(simplePiTest | targetPiTest | dataPiTest)

  private val simplePiTest: P[SimplePITest.type] =
    P("processing-instruction" ~ "(" ~ ")") map (_ => SimplePITest)

  private val targetPiTest: P[TargetPITest] =
    P("processing-instruction" ~ "(" ~ ncName ~ ")") map {
      case name => TargetPITest(name)
    }

  private val dataPiTest: P[DataPITest] =
    P("processing-instruction" ~ "(" ~ stringLiteral ~ ")") map {
      case stringLit => DataPITest(stringLit)
    }

  private val commentTest: P[CommentTest.type] =
    P("comment" ~ "(" ~ ")") map (_ => CommentTest)

  private val textTest: P[TextTest.type] =
    P("text" ~ "(" ~ ")") map (_ => TextTest)

  private val namespaceNodeTest: P[NamespaceNodeTest.type] =
    P("namespace-node" ~ "(" ~ ")") map (_ => NamespaceNodeTest)

  private val anyKindTest: P[AnyKindTest.type] =
    P("node" ~ "(" ~ ")") map (_ => AnyKindTest)

  private val postfixExpr: P[PostfixExpr] =
    P(primaryExpr ~ (predicate | argumentList).rep) map {
      case (primaryExp, predicateOrArgumentListSeq) => PostfixExpr(primaryExp, predicateOrArgumentListSeq.toIndexedSeq)
    }

  private val argumentList: P[ArgumentList] =
    P("(" ~ argument.rep(sep = ",") ~ ")") map {
      case args => ArgumentList(args.toIndexedSeq)
    }

  private val argument: P[Argument] =
    P(argumentPlaceholder | exprSingleArgument)

  private val argumentPlaceholder: P[ArgumentPlaceholder.type] =
    P("?") map (_ => ArgumentPlaceholder)

  private val exprSingleArgument: P[ExprSingleArgument] =
    P(exprSingle) map {
      case exp => ExprSingleArgument(exp)
    }

  private val paramList: P[ParamList] =
    P(param.rep(min = 1, sep = ",")) map {
      case pars => ParamList(pars.toIndexedSeq)
    }

  private val param: P[Param] =
    P("$" ~ eqName ~ ("as" ~ sequenceType).?) map {
      case (name, tpeOption) => Param(name, tpeOption.map(t => TypeDeclaration(t)))
    }

  private val predicate: P[Predicate] =
    P("[" ~ expr ~ "]") map {
      case exp => Predicate(exp)
    }

  // Primary expressions

  private val primaryExpr: P[PrimaryExpr] =
    P(literal | varRef | parenthesizedExpr | contextItemExpr | functionCall | functionItemExpr)

  private val literal: P[Literal] =
    P(stringLiteral | numericLiteral)

  // TODO Fix and improve string and numeric literals

  private val stringLiteral: P[StringLiteral] =
    P(CharsWhile(isStringLiteralChar).!) filter (isStringLiteral) map (v => StringLiteral(v.drop(1).dropRight(1)))

  private val numericLiteral: P[NumericLiteral] =
    P(integerLiteral | decimalLiteral | doubleLiteral)

  private val integerLiteral: P[IntegerLiteral] =
    P(CharsWhileIn("0123456789").!) filter (_.nonEmpty) map (v => IntegerLiteral(v.toInt))

  private val decimalLiteral: P[DecimalLiteral] =
    P(integerLiteral) map (v => DecimalLiteral(v.value)) // TODO

  private val doubleLiteral: P[DoubleLiteral] =
    P(integerLiteral) map (v => DoubleLiteral(v.value)) // TODO

  private val varRef: P[VarRef] =
    P("$" ~ eqName) map {
      name => VarRef(name)
    }

  private val parenthesizedExpr: P[ParenthesizedExpr] =
    P("(" ~ expr.? ~ ")") map {
      case expOption => ParenthesizedExpr(expOption)
    }

  private val contextItemExpr: P[ContextItemExpr.type] =
    P(".") map (_ => ContextItemExpr)

  // TODO xgc:reserved-function-names and gn:parens

  private val functionCall: P[FunctionCall] =
    P(eqName ~ argumentList) map {
      case (name, argList) => FunctionCall(name, argList)
    }

  private val functionItemExpr: P[FunctionItemExpr] =
    P(namedFunctionRef | inlineFunctionExpr)

  // TODO xgc:reserved-function-names

  private val namedFunctionRef: P[NamedFunctionRef] =
    P(eqName ~ "#" ~ integerLiteral) map {
      case (name, arity) => NamedFunctionRef(name, arity.value)
    }

  private val inlineFunctionExpr: P[InlineFunctionExpr] =
    P("function" ~ "(" ~ paramList.? ~ ")" ~ ("as" ~ sequenceType).? ~ enclosedExpr) map {
      case (parListOption, resultTpeOption, body) =>
        InlineFunctionExpr(parListOption, resultTpeOption, body)
    }

  // Types

  private val sequenceType: P[SequenceType] =
    P(emptySequenceType | nonEmptySequenceType)

  private val emptySequenceType: P[EmptySequenceType.type] =
    P("empty-sequence" ~ "(" ~ ")") map (_ => EmptySequenceType)

  // TODO xgc:occurrence-indicators

  private val nonEmptySequenceType: P[SequenceType] =
    P(itemType ~ ("?" | "*" | "+").!.?) map {
      case (tpe, None)      => ExactlyOneSequenceType(tpe)
      case (tpe, Some("?")) => ZeroOrOneSequenceType(tpe)
      case (tpe, Some("*")) => ZeroOrMoreSequenceType(tpe)
      case (tpe, Some("+")) => OneOrMoreSequenceType(tpe)
      case _                => EmptySequenceType
    }

  private val itemType: P[ItemType] =
    P(kindTestItemType | anyItemType | anyFunctionTest | typedFunctionTest | atomicOrUnionType | parenthesizedItemType)

  private val kindTestItemType: P[KindTestItemType] =
    P(kindTest) map {
      case kindTst => KindTestItemType(kindTst)
    }

  private val anyItemType: P[AnyItemType.type] =
    P("item" ~ "(" ~ ")") map (_ => AnyItemType)

  private val anyFunctionTest: P[AnyFunctionTest.type] =
    P("function" ~ "(" ~ "*" ~ ")") map (_ => AnyFunctionTest)

  private val typedFunctionTest: P[TypedFunctionTest] =
    P("function" ~ "(" ~ sequenceType.rep(sep = ",") ~ ")" ~ "as" ~ sequenceType) map {
      case (parTpes, resultTpe) => TypedFunctionTest(parTpes.toIndexedSeq, resultTpe)
    }

  private val atomicOrUnionType: P[AtomicOrUnionType] =
    P(eqName) map {
      case tpe => AtomicOrUnionType(tpe)
    }

  private val parenthesizedItemType: P[ParenthesizedItemType] =
    P("(" ~ itemType ~ ")") map {
      case tpe => ParenthesizedItemType(tpe)
    }

  private val singleType: P[SingleType] =
    P(eqName ~ "?".!.?) map {
      case (tpe, None)    => NonEmptySingleType(tpe)
      case (tpe, Some(_)) => PotentiallyEmptySingleType(tpe)
    }

  // Names (EQNames, NCNames etc.)

  private val ncName: P[NCName] =
    P(CharsWhile(c => NCName.canBePartOfNCName(c)).!) filter (s => NCName.canBeNCName(s)) map (s => NCName(s))

  private val eqName: P[EQName] =
    P(qName | uriQualifiedName)

  private val qName: P[QNameAsEQName] =
    P(CharsWhile(c => QNameAsEQName.canBePartOfQNameAsEQName(c)).!) filter (s => QNameAsEQName.canBeQNameAsEQName(s)) map (s => QNameAsEQName.parse(s))

  private val uriQualifiedName: P[URIQualifiedName] =
    P(CharsWhile(c => URIQualifiedName.canBePartOfURIQualifiedName(c)).!) filter (s => URIQualifiedName.canBeURIQualifiedName(s)) map (s => URIQualifiedName.parse(s))

  // Operators etc.

  private val valueComp: P[ValueComp] =
    P(("eq" | "ne" | "lt" | "le" | "gt" | "ge").!) map (s => ValueComp.parse(s))

  private val generalComp: P[GeneralComp] =
    P(("=" | "!=" | "<" | "<=" | ">" | ">=").!) map (s => GeneralComp.parse(s))

  private val nodeComp: P[NodeComp] =
    P(("is" | "<<" | ">>").!) map (s => NodeComp.parse(s))

  // Utility methods (and data)

  // TODO There are no keywords; it is the position of the word that counts. So we are cheating here.
  private val keywords: Set[String] = Set(
    "if",
    "in",
    "return",
    "and",
    "or",
    "not",
    "some",
    "every",
    "satisfies")

  private def isPrefixWildcard(s: String): Boolean = {
    s.endsWith(":*") && NCName.canBeNCName(s.dropRight(2))
  }

  private def isLocalNameWildcard(s: String): Boolean = {
    s.startsWith("*:") && NCName.canBeNCName(s.drop(2))
  }

  private def isNamespaceWildcard(s: String): Boolean = {
    s.startsWith("Q{") && s.endsWith("}*") && NCName.canBeNCName(s.drop(2).dropRight(2))
  }

  private def isNCNameCharOrColonOrStar(c: Char): Boolean = {
    NCName.canBePartOfNCName(c) || (c == ':') || (c == '*')
  }

  private def isNCNameCharOrBraceOrStar(c: Char): Boolean = {
    NCName.canBePartOfNCName(c) || (c == '{') || (c == '}') || (c == '*')
  }

  private def isStringLiteral(s: String): Boolean = {
    // TODO Improve, and mind escaping of quotes

    (s.startsWith("\"") && s.endsWith("\"") && isProbablyValidXmlName(s.drop(1).dropRight(1))) ||
      (s.startsWith("'") && s.endsWith("'") && isProbablyValidXmlName(s.drop(1).dropRight(1)))
  }

  private def isStringLiteralChar(c: Char): Boolean = {
    // TODO Improve
    (c == '"') || (c == '\'') || isProbableXmlNameChar(c)
  }

  /** Returns true if the name is probably a valid XML name (even if reserved or containing a colon) */
  private def isProbablyValidXmlName(s: String): Boolean = {
    require(s ne null) // scalastyle:off null
    (s.length > 0) && isProbableXmlNameStart(s(0)) && {
      s.drop(1) forall { c => isProbableXmlNameChar(c) }
    }
  }

  private def isProbableXmlNameStart(c: Char): Boolean = c match {
    case '-'                                 => false
    case '.'                                 => false
    case c if java.lang.Character.isDigit(c) => false
    case _                                   => isProbableXmlNameChar(c)
  }

  private def isProbableXmlNameChar(c: Char): Boolean = c match {
    case '_' => true
    case '-' => true
    case '.' => true
    case '$' => false
    case ':' => true
    case c if java.lang.Character.isWhitespace(c) => false
    case c if java.lang.Character.isJavaIdentifierPart(c) => true
    case _ => false
  }

  def main(args: Array[String]): Unit = {
    // TODO Remove main method!!!
    val exprString = args(0)

    val parseResult = xpathExpr.parse(exprString)
    println(parseResult)
  }
}
