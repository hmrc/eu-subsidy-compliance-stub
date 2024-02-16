/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{AmendmentType, EORI, IndustrySectorLimit, PhoneNumber, Sector, TraderRef, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.models.{types, _}
import uk.gov.hmrc.smartstub._
import wolfendale.scalacheck.regexp.RegexpGen

import java.time.LocalDate
import shapeless.tag.@@

import scala.math.BigDecimal.RoundingMode

object DataGenerator {

  private def variableLengthString(min: Int, max: Int) =
    Gen.choose(min, max).flatMap(len => Gen.listOfN(len, Gen.alphaLowerChar)).map(_.mkString)

  private def genEORIPrefix: Gen[String] =
    Gen.oneOf(List("GB", "XI"))

  private def genEORIDigits: Gen[String] = {
    def short: Gen[String] = pattern"999999999999"
    def long: Gen[String] = pattern"999999999999999"
    Gen.oneOf(long, short)
  }

  def genEORI: Gen[EORI] =
    for {
      prefix <- genEORIPrefix
      rest <- genEORIDigits
    } yield EORI(s"$prefix$rest")

  def genUndertakingRef: Gen[UndertakingRef] =
    RegexpGen.from(UndertakingRef.regex).map(UndertakingRef.apply)

  def genContactDetails: Gen[ContactDetails] =
    for {
      phone <- Gen.option(variableLengthString(1, 24).map(PhoneNumber(_)))
      mobile <- Gen.option(variableLengthString(1, 24).map(PhoneNumber(_)))
    } yield ContactDetails(phone, mobile)

  private def genBusinessEntity: Gen[BusinessEntity] =
    for {
      e <- genEORI
      contactDetails <- Gen.option(genContactDetails)
    } yield BusinessEntity(e, leadEORI = false, contactDetails)

  def genBusinessEntityUpdate: Gen[BusinessEntityUpdate] =
    for {
      amendmentType <- Gen.oneOf(List(AmendmentType.add, AmendmentType.amend, AmendmentType.delete))
      amendmentEffectiveDate <- Gen.date(LocalDate.of(2020, 1, 1), LocalDate.now)
      businessEntity <- genBusinessEntity
    } yield BusinessEntityUpdate(amendmentType, amendmentEffectiveDate, businessEntity)

  private def genIndustrySectorLimit: Gen[@@[BigDecimal, types.IndustrySectorLimit.Tag]] =
    Gen
      .choose(BigDecimal(0), BigDecimal(99999999999.99f))
      .map(n => IndustrySectorLimit(n.setScale(2, RoundingMode.DOWN).bigDecimal.stripTrailingZeros()))

  private def genLastSubsidyUsageUpdt: Gen[LocalDate] =
    Gen.date(LocalDate.of(2020, 1, 1), LocalDate.now)

  def genRetrievedUndertaking(eori: EORI): Gen[Undertaking] =
    for {
      ref <- variableLengthString(1, 17)
      name <- variableLengthString(1, 105)
      industrySector <- Gen.oneOf(List(0, 1, 2, 3))
      industrySectorLimit <- genIndustrySectorLimit
      lastSubsidyUsageUpdt <- genLastSubsidyUsageUpdt
      undertakingStatus <- Gen.oneOf(List(0, 1, 5, 9))
      nBusinessEntities <- Gen.choose(2, 25)
      undertakingBusinessEntity <- Gen.listOfN(nBusinessEntities, genBusinessEntity)
    } yield Undertaking(
      reference = UndertakingRef(ref),
      name = UndertakingName(name),
      industrySector = Sector(industrySector),
      industrySectorLimit = industrySectorLimit,
      lastSubsidyUsageUpdt = lastSubsidyUsageUpdt.some,
      undertakingStatus = undertakingStatus.some,
      undertakingBusinessEntity = undertakingBusinessEntity.head
        .copy(businessEntityIdentifier = EORI(eori), leadEORI = true) :: undertakingBusinessEntity.tail
    )

  def genTraderRef: Gen[TraderRef] =
    RegexpGen.from(TraderRef.regex).map(TraderRef.apply)

  def genMonthlyExchangeRate(year: Int, month: Int): Gen[MonthlyExchangeRate] = {
    Gen.chooseNum(BigDecimal(0.8), BigDecimal(0.95)).map { amount =>
      val dateStart = LocalDate.of(year, month, 1)
      MonthlyExchangeRate(
        currencyIso = "GBP",
        refCurrencyIso = "EUR",
        amount = amount.setScale(4, RoundingMode.DOWN),
        dateStart = dateStart,
        dateEnd = dateStart.plusMonths(1).minusDays(1)
      )
    }
  }

  def getSampleValue[A](gen: Gen[A], seed: Option[Long] = None, retries: Int = 10): A = {
    LazyList
      .continually(seed.map(gen.seeded(_)(identity(_))).getOrElse(gen.sample))
      .take(retries)
      .flatten
      .headOption
      .getOrElse(throw new Exception(s"Can't generate a sample after $retries retries"))
  }

}
