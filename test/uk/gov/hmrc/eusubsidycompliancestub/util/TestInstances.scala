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

package uk.gov.hmrc.eusubsidycompliancestub.util

import cats.implicits._

import java.time._
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.eusubsidycompliancestub.models._
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.{ErrorDetail, ErrorDetails, SourceFaultDetail}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{CorrelationID, EORI, EisSubsidyAmendmentType, ErrorCode, ErrorMessage, Source, SubsidyAmount, SubsidyRef, TraderRef, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.services.DataGenerator._
import uk.gov.hmrc.smartstub._
import wolfendale.scalacheck.regexp.RegexpGen

import scala.math.BigDecimal.RoundingMode

object TestInstances {

  implicit def arbSubsidyUpdate: Arbitrary[SubsidyUpdate] = {
    val su = for {
      undertakingRef <- genUndertakingRef
      n <- Gen.choose(1, 25)
      undertakingSubsidyAmendment <- Gen.listOfN(n, arbSubsidy.arbitrary)
    } yield SubsidyUpdate(undertakingRef, UndertakingSubsidyAmendment(undertakingSubsidyAmendment))
    Arbitrary(su)
  }

  implicit def arbSubsidyUpdateNilReturn: Arbitrary[SubsidyUpdate] = {
    val su = for {
      undertakingRef <- genUndertakingRef
      nilSubmissionDate <- Gen.date(LocalDate.of(2020, 1, 1), LocalDate.now)
    } yield SubsidyUpdate(undertakingRef, NilSubmissionDate(nilSubmissionDate))
    Arbitrary(su)
  }

  implicit def arbSubsidyUpdateWithSomeNilReturns: Arbitrary[SubsidyUpdate] = {
    val su = for {
      updateOrNilReturn <- Gen.oneOf(arbSubsidyUpdate.arbitrary, arbSubsidyUpdateNilReturn.arbitrary)
    } yield updateOrNilReturn
    Arbitrary(su)
  }

  implicit def arbUndertakingBusinessEntityUpdate: Arbitrary[UndertakingBusinessEntityUpdate] = {
    val ubeu = for {
      undertakingIdentifier <- genUndertakingRef
      undertakingComplete <- Gen.const(true)
      n <- Gen.choose(1, 5)
      businessEntityUpdate <- Gen.listOfN(n, genBusinessEntityUpdate)
    } yield UndertakingBusinessEntityUpdate(undertakingIdentifier, undertakingComplete, businessEntityUpdate)
    Arbitrary(ubeu)
  }

  implicit def arbSubsidyRef: Arbitrary[SubsidyRef] = Arbitrary {
    RegexpGen.from(SubsidyRef.regex).map(SubsidyRef.apply)
  }

  implicit def arbSubsidy: Arbitrary[NonHmrcSubsidy] = {
    val a = for {
      amendmentType <- Gen.oneOf(Seq("1", "2", "3")).map(x => Some(EisSubsidyAmendmentType(x)))
      ref <- arbSubsidyRef.arbitrary.map(x => amendmentType.fold(Option.empty[SubsidyRef])(_ => Some(x)))
      allocationDate <- Gen.date(LocalDate.of(2020, 1, 1), LocalDate.now)
      submissionDate <- Gen.date(LocalDate.of(2020, 1, 1), LocalDate.now)
      publicAuthority <- arbString.arbitrary
      traderReference <- Gen.option(arbTraderRef.arbitrary)
      nonHMRCSubsidyAmount <- Gen
        .choose(BigDecimal(0), BigDecimal(999999999.99f))
        .map(x => SubsidyAmount(x.setScale(2, RoundingMode.DOWN).bigDecimal.stripTrailingZeros()))
      businessEntityIdentifier <- genEORI
    } yield NonHmrcSubsidy(
      ref,
      allocationDate,
      submissionDate,
      publicAuthority.some,
      traderReference,
      nonHMRCSubsidyAmount,
      businessEntityIdentifier.some,
      amendmentType
    )
    Arbitrary(a)
  }

  implicit def arbSubsidies: Arbitrary[List[NonHmrcSubsidy]] = Arbitrary(Gen.nonEmptyListOf(arbSubsidy.arbitrary))

  implicit def arbUndertaking: Arbitrary[Undertaking] = {
    val u = for {
      eori <- genEORI
      undertaking <- genRetrievedUndertaking(eori)
    } yield undertaking
    Arbitrary(u)
  }

  implicit def arbUndertakings: Arbitrary[List[Undertaking]] =
    Arbitrary(
      Gen.listOf(arbUndertaking.arbitrary).map(_.distinctBy(_.reference))
    )

  def arbContactDetails: Arbitrary[ContactDetails] =
    Arbitrary(genContactDetails.retryUntil(x => x.phone.nonEmpty || x.mobile.nonEmpty))

  def arbUndertakingForCreate: Arbitrary[Undertaking] =
    Arbitrary(arbUndertaking.arbitrary.map { x =>
      x.copy(
        undertakingBusinessEntity = List(
          x.undertakingBusinessEntity.head.copy(contacts = getSampleValue(arbContactDetails.arbitrary).some)
        )
      )
    })

  implicit def arbEori: Arbitrary[EORI] =
    Arbitrary(genEORI)

  implicit def arbErrorCode: Arbitrary[ErrorCode] = Arbitrary {
    RegexpGen.from(ErrorCode.regex).map(ErrorCode.apply)
  }

  implicit def arbErrorMessage: Arbitrary[ErrorMessage] = Arbitrary {
    RegexpGen.from(ErrorMessage.regex).map(ErrorMessage.apply)
  }

  implicit def arbCorrelationID: Arbitrary[CorrelationID] = Arbitrary {
    RegexpGen.from(CorrelationID.regex).map(CorrelationID.apply)
  }

  implicit def arbTraderRef: Arbitrary[TraderRef] =
    Arbitrary(genTraderRef)

  implicit val arbString: Arbitrary[String] = Arbitrary(
    Gen.alphaNumStr.map(_.take(255))
  )

  implicit def arbSourceFaultDetail: Arbitrary[List[String]] = Arbitrary {
    for {
      num <- Gen.chooseNum(1, 5)
      list <- Gen.listOfN(num, arbString.arbitrary)
    } yield list
  }

  implicit def arbErrorDetail: Arbitrary[ErrorDetails] = {
    val ed = for {
      errorCode <- arbErrorCode.arbitrary
      errorMessage <- arbErrorMessage.arbitrary
      sfd <- arbSourceFaultDetail.arbitrary
      corId <- arbCorrelationID.arbitrary
      t = LocalDateTime.now
      s = Source("EIS")
    } yield ErrorDetails(ErrorDetail(t, corId, errorCode, errorMessage, s, SourceFaultDetail(sfd)))
    Arbitrary(ed)
  }

  implicit def arbUndertakingRef: Arbitrary[UndertakingRef] = Arbitrary {
    RegexpGen.from(UndertakingRef.regex).map(UndertakingRef.apply)
  }

  implicit def arbSubsidyRetrieve: Arbitrary[SubsidyRetrieve] = Arbitrary {
    for {
      ref <- arbUndertakingRef.arbitrary
      range <- Gen.option((LocalDate.now.minusMonths(6L), LocalDate.now))
    } yield SubsidyRetrieve(ref, range)
  }
}
