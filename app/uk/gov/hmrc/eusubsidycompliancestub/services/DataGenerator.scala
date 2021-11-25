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

import org.scalacheck.Gen
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, IndustrySectorLimit, PhoneNumber, Sector, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.models.{BusinessEntity, ContactDetails, Undertaking}
import uk.gov.hmrc.smartstub._
import wolfendale.scalacheck.regexp.RegexpGen

import java.time.LocalDate

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

  def genRetrievedUndertaking(eori: String): Gen[Undertaking] =
    for {
      ref <- variableLengthString(1, 17)
      name <- variableLengthString(1, 105)
      industrySector <- Gen.oneOf(List("0","1","2","3"))
      industrySectorLimit <- Gen.choose(1L, 9999999999999L).map(x => IndustrySectorLimit(x / 100))
      lastSubsidyUsageUpdt <- Gen.date(LocalDate.of(2020,1,1), LocalDate.now)
      nBusinessEntities <- Gen.choose(1,25)
      undertakingBusinessEntity <- Gen.listOfN(nBusinessEntities,genBusinessEntity)
    } yield Undertaking(
      Some(UndertakingRef(ref)),
      UndertakingName(name),
      Sector(industrySector),
      industrySectorLimit,
      lastSubsidyUsageUpdt,
      undertakingBusinessEntity.head.copy(businessEntityIdentifier = EORI(eori), leadEORI = true) :: undertakingBusinessEntity.tail
    )

}
