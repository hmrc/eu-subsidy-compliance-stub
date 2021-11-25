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
import uk.gov.hmrc.eusubsidycompliancestub.models.{ContactDetails, Undertaking}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.ErrorDetail
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{CorrelationID, EORI, ErrorCode, ErrorMessage, Source}
import uk.gov.hmrc.eusubsidycompliancestub.services.DataGenerator.{genContactDetails, genEORI, genRetrievedUndertaking}
import wolfendale.scalacheck.regexp.RegexpGen

object TestInstances {

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
