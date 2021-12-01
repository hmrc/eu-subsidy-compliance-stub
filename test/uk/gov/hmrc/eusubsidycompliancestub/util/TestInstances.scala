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

package uk.gov.hmrc.eusubsidycompliancestub.util

import cats.implicits._

import java.time._
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.eusubsidycompliancestub.models._
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.ErrorDetail
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{CorrelationID, EORI, EisSubsidyAmendmentType, ErrorCode, ErrorMessage, Source, SubsidyAmount, SubsidyRef}
import uk.gov.hmrc.eusubsidycompliancestub.services.DataGenerator.{genBusinessEntityUpdate, genContactDetails, genEORI, genRetrievedUndertaking, genUndertakingRef}
import uk.gov.hmrc.smartstub._
import wolfendale.scalacheck.regexp.RegexpGen

object TestInstances {

  implicit def arbSubsidyUpdate: Arbitrary[SubsidyUpdate] = {
    val su = for {
      undertakingRef <- genUndertakingRef
      n <- Gen.choose(1,25)
      undertakingSubsidyAmendment <- Gen.listOfN(n, arbSubsidy.arbitrary)
    } yield SubsidyUpdate(undertakingRef, UndertakingSubsidyAmendment(undertakingSubsidyAmendment))
    Arbitrary(su)
  }

  implicit def arbSubsidyUpdateNilReturn: Arbitrary[SubsidyUpdate] = {
    val su = for {
      undertakingRef <- genUndertakingRef
      nilSubmissionDate <-  Gen.date(LocalDate.of(2020,1,1), LocalDate.now)
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
      n <- Gen.choose(1,5)
      businessEntityUpdate <- Gen.listOfN(n, genBusinessEntityUpdate)
    } yield {
      UndertakingBusinessEntityUpdate(undertakingIdentifier, undertakingComplete, businessEntityUpdate)
    }
    Arbitrary(ubeu)
  }

  implicit def arbSubsidyRef: Arbitrary[SubsidyRef] = Arbitrary {
    RegexpGen.from(SubsidyRef.regex).map(SubsidyRef.apply)
  }

  implicit def arbSubsidy: Arbitrary[Subsidy] = {
    val a = for {
      amendmentType <- Gen.oneOf(Seq("1","2","3")).map(x => Some(EisSubsidyAmendmentType(x)))
      ref <- arbSubsidyRef.arbitrary.map(x => amendmentType.fold(Option.empty[SubsidyRef]){_ => Some(x)})
      allocationDate <- Gen.date(LocalDate.of(2020,1,1), LocalDate.now)
      submissionDate <- Gen.date(LocalDate.of(2020,1,1), LocalDate.now)
      publicAuthority <- arbString.arbitrary
      traderReference <- Gen.option(arbString.arbitrary)
      nonHMRCSubsidyAmount <- Gen.choose(1L, 9999999999999L).map(x => SubsidyAmount(x / 100))
      businessEntityIdentifier <- genEORI
    } yield
      Subsidy(
        ref,
        allocationDate,
        submissionDate,
        publicAuthority,
        traderReference,
        nonHMRCSubsidyAmount,
        businessEntityIdentifier.some,
        amendmentType
      )
    Arbitrary(a)
  }

  implicit def arbUndertaking: Arbitrary[Undertaking] = {
    val u = for {
      eori <- genEORI
      undertaking <- genRetrievedUndertaking(eori)
    } yield undertaking
    Arbitrary(u)
  }

  def arbContactDetails: Arbitrary[ContactDetails] =
    Arbitrary(genContactDetails.retryUntil(x => x.phone.nonEmpty || x.mobile.nonEmpty))

  def arbUndertakingForCreate: Arbitrary[Undertaking] = {
    Arbitrary(arbUndertaking.arbitrary.map { x =>
      x.copy(
        undertakingBusinessEntity =
          List(
            x.undertakingBusinessEntity.head.copy(
              contacts = arbContactDetails.arbitrary.sample.get.some
            )
          )
      )
    })
  }

  implicit def arbEori: Arbitrary[EORI] = {
    Arbitrary(genEORI)
  }

  implicit def arbErrorCode: Arbitrary[ErrorCode] = Arbitrary {
    RegexpGen.from(ErrorCode.regex).map(ErrorCode.apply)
  }

  implicit def arbErrorMessage: Arbitrary[ErrorMessage] = Arbitrary {
    RegexpGen.from(ErrorMessage.regex).map(ErrorMessage.apply)
  }

  implicit def arbCorrelationID: Arbitrary[CorrelationID] = Arbitrary {
    RegexpGen.from(CorrelationID.regex).map(CorrelationID.apply)
  }

  implicit val arbString: Arbitrary[String] = Arbitrary(
    Gen.alphaNumStr.map{_.take(255)}
  )

  implicit def arbSourceFaultDetail: Arbitrary[List[String]] = Arbitrary {
    for {
      num <- Gen.chooseNum(1, 5)
      list <- Gen.listOfN(num, arbString.arbitrary)
    } yield list
  }

  implicit def arbErrorDetail: Arbitrary[ErrorDetail] =  {
    val ed = for {
      errorCode <- arbErrorCode.arbitrary
      errorMessage <- arbErrorMessage.arbitrary
      sfd <- arbSourceFaultDetail.arbitrary
      corId <- arbCorrelationID.arbitrary
      t = LocalDateTime.now
      s = Source("EIS")
    } yield ErrorDetail(errorCode, errorMessage, sfd, s, LocalDateTime.now, corId)
    Arbitrary(ed)
  }

}
