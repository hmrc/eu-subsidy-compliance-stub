/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.eusubsidycompliancestub.services

import cats.implicits._
import org.scalacheck.Gen
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{AmendmentType, DeclarationID, EORI, IndustrySectorLimit, PhoneNumber, Sector, SubsidyAmount, SubsidyRef, TaxType, TraderRef, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.models.{types, _}
import uk.gov.hmrc.smartstub._
import wolfendale.scalacheck.regexp.RegexpGen
import java.time.LocalDate

import shapeless.tag.@@

object DataGenerator {

  private def variableLengthString(min: Int, max: Int) = {
    Gen.choose(min, max).flatMap(len => Gen.listOfN(len, Gen.alphaLowerChar)).map(_.mkString)
  }

  def genEORIPrefix: Gen[String] =
    Gen.oneOf(List("GB", "XI"))

  def genEORIDigits: Gen[String] = {
    def short: Gen[String] = pattern"999999999999"
    def long: Gen[String] = pattern"999999999999999"
    Gen.oneOf(long, short)
  }

  def genEORI: Gen[EORI] = {
    for {
      prefix <- genEORIPrefix
      rest <- genEORIDigits
    } yield EORI(s"$prefix$rest")
  }

  def genUndertakingRef: Gen[UndertakingRef] =
    RegexpGen.from(UndertakingRef.regex).map(UndertakingRef.apply)

  def genContactDetails: Gen[ContactDetails] =
    for {
      phone <- Gen.option(variableLengthString(1,24).map(PhoneNumber(_)))
      mobile <- Gen.option(variableLengthString(1,24).map(PhoneNumber(_)))
    } yield ContactDetails(phone, mobile)

  def genBusinessEntity: Gen[BusinessEntity] =
    for {
      e <- genEORI
      contactDetails <- Gen.option(genContactDetails)
    } yield BusinessEntity(e, false, contactDetails)

 def genBusinessEntityUpdate : Gen[BusinessEntityUpdate] =
   for {
     amendmentType <- Gen.oneOf(List(AmendmentType.add, AmendmentType.amend, AmendmentType.delete))
     amendmentEffectiveDate <- Gen.date(LocalDate.of(2020,1,1), LocalDate.now)
     businessEntity <- genBusinessEntity
   } yield BusinessEntityUpdate(amendmentType, amendmentEffectiveDate, businessEntity)

  def genIndustrySectorLimit: Gen[@@[BigDecimal, types.IndustrySectorLimit.Tag]] =
    Gen.choose(1F, 9999999999999F).map(x => IndustrySectorLimit(x / 100))

  def genLastSubsidyUsageUpdt: Gen[LocalDate] =
    Gen.date(LocalDate.of(2020,1,1), LocalDate.now)

  def genRetrievedUndertaking(eori: EORI): Gen[Undertaking] =
    for {
      ref <- variableLengthString(1, 17)
      name <- variableLengthString(1, 105)
      industrySector <- Gen.oneOf(List(0,1,2,3))
      industrySectorLimit <- genIndustrySectorLimit
      lastSubsidyUsageUpdt <- genLastSubsidyUsageUpdt
      nBusinessEntities <- Gen.choose(1,25)
      undertakingBusinessEntity <- Gen.listOfN(nBusinessEntities,genBusinessEntity)
    } yield Undertaking(
      Some(UndertakingRef(ref)),
      UndertakingName(name),
      Sector(industrySector),
      industrySectorLimit.some,
      lastSubsidyUsageUpdt.some,
      undertakingBusinessEntity.head.copy(businessEntityIdentifier = EORI(eori), leadEORI = true) :: undertakingBusinessEntity.tail
    )

  def genSubsidyAmount: Gen[SubsidyAmount] = // n.b. dividing by 25 as the schema constraint is the same for the total as the subsidies
    Gen.choose(-9999999999999F, 9999999999999F).map{x =>
      SubsidyAmount((x / 25).round / 100)
    }

  def genSubsidyRef: Gen[SubsidyRef] =
    RegexpGen.from(SubsidyRef.regex).map(SubsidyRef.apply)

  def genTraderRef: Gen[TraderRef] =
    RegexpGen.from(TraderRef.regex).map(TraderRef.apply)

  private def start(r: SubsidyRetrieve): LocalDate =
    r.inDateRange.fold(LocalDate.of(2020, 1, 1)) {
      _._1
    }

  private def end(r: SubsidyRetrieve): LocalDate =
    r.inDateRange.fold(LocalDate.now) {
      _._2
    }

  def genNonHmrcSubsidy(r: SubsidyRetrieve): Gen[NonHmrcSubsidy] =
    for {
      subRef <- genSubsidyRef
      allocationDate <- Gen.date(start(r), end(r))
      submissionDate <- Gen.date(start(r), end(r))
      publicAuthority <- variableLengthString(1, 255)
      traderReference <- Gen.option(genTraderRef)
      nonHMRCSubsidyAmtEUR <- genSubsidyAmount
      businessEntityIdentifier <- Gen.option(genBusinessEntity.map(_.businessEntityIdentifier))
    } yield NonHmrcSubsidy(
      subRef.some,
      allocationDate,
      submissionDate,
      publicAuthority.some,
      traderReference,
      nonHMRCSubsidyAmtEUR,
      businessEntityIdentifier
    )

  def genDeclarationId: Gen[DeclarationID] =
    RegexpGen.from(DeclarationID.regex).map(DeclarationID.apply)

  def genTaxType: Gen[TaxType] =
    RegexpGen.from(TaxType.regex).map(TaxType.apply)

  def genHmrcSubsidies(r: SubsidyRetrieve): Gen[HmrcSubsidy] =
    for {
      declarationID <- genDeclarationId
      issueDate <- Gen.option(Gen.date(start(r), end(r)))
      acceptanceDate <- Gen.date(start(r), end(r))
      declarantEORI <- genEORI
      consigneeEORI <- genEORI
      taxType <- Gen.option(genTaxType)
      amount <- Gen.option(genSubsidyAmount)
      tradersOwnRefUCR <- Gen.option(genTraderRef)
    } yield HmrcSubsidy(
      declarationID,
      issueDate,
      acceptanceDate,
      declarantEORI,
      consigneeEORI,
      taxType,
      amount,
      tradersOwnRefUCR
    )


  def genSubsidies(r: SubsidyRetrieve): Gen[UndertakingSubsidies] =
    for {
      x <- Gen.choose(1,25)
      y <- Gen.choose(1,25)
      nonHmrcSubsidies <- Gen.listOfN(x, genNonHmrcSubsidy(r))
      hmrcSubsidies <- Gen.listOfN(y, genHmrcSubsidies(r))
    } yield {
      val nonHMRCTotal = SubsidyAmount(nonHmrcSubsidies.map(x => x.nonHMRCSubsidyAmtEUR).sum[BigDecimal])
      val hmrcTotal = SubsidyAmount(hmrcSubsidies.flatMap(x => x.amount).sum[BigDecimal])
      UndertakingSubsidies(
        r.undertakingIdentifier,
        nonHMRCTotal,
        nonHMRCTotal,
        hmrcTotal,
        hmrcTotal,
        nonHmrcSubsidies,
        hmrcSubsidies
      )
    }
}
