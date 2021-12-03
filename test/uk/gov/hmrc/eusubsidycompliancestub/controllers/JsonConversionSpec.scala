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

package uk.gov.hmrc.eusubsidycompliancestub.controllers

import org.scalatest.Assertion
import play.api.libs.json.{Format, Json, Writes}
import uk.gov.hmrc.eusubsidycompliancestub.models._
import uk.gov.hmrc.eusubsidycompliancestub.models.json.digital.{amendUndertakingMemberDataWrites, retrieveUndertakingEORIWrites}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.{ErrorDetail, eisRetrieveUndertakingResponse}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, EisAmendmentType}
import uk.gov.hmrc.eusubsidycompliancestub.util.TestInstances._

class JsonConversionSpec extends BaseControllerSpec {

  "Json Writes " must {
    "match retrieveUndertakingResponse.schema.json" in {
      forAll { undertaking: Undertaking =>
        checkWrites[Undertaking](undertaking,"retrieveUndertakingResponse")
      }
    }

    "match createUndertakingRequest.schema.json" in {
      implicit val format: Format[Undertaking] = json.digital.undertakingFormat
      forAll { undertaking: Undertaking =>
        checkWrites[Undertaking](undertaking,"createUndertakingRequest")
      }(implicitly, arbUndertakingForCreate, implicitly, implicitly, implicitly, implicitly)
    }

    "match retrieveUndertakingRequest.schema.json" in {
      forAll { eori: EORI =>
        checkWrites[EORI](eori, "retrieveUndertakingRequest")
      }
    }

    "match updateUndertakingRequest.schema.json for disable" in {
      val writes = json.digital.updateUndertakingWrites(EisAmendmentType.D)
      forAll { undertaking: Undertaking =>
        checkWrites[Undertaking](undertaking,"updateUndertakingRequest")(writes)
      }(implicitly, arbUndertaking, implicitly, implicitly, implicitly, implicitly)
    }

    "match updateUndertakingRequest.schema.json for amend" in {
      val writes = json.digital.updateUndertakingWrites(EisAmendmentType.A)
      forAll { undertaking: Undertaking =>
        checkWrites[Undertaking](undertaking,"updateUndertakingRequest")(writes)
      }(implicitly, arbUndertaking, implicitly, implicitly, implicitly, implicitly)
    }

    "match errorDetailResponse.schema.json" in {
      forAll { errorDetail: ErrorDetail =>
        checkWrites[ErrorDetail](errorDetail, "errorDetailResponse")
      }
    }

    "match updateSubsidyUsageRequest.schema.json" in {
      forAll { subsidyUpdate: SubsidyUpdate =>
        checkWrites[SubsidyUpdate](subsidyUpdate, "updateSubsidyUsageRequest")
      }(implicitly, arbSubsidyUpdateWithSomeNilReturns, implicitly, implicitly, implicitly, implicitly)
    }

    "match amendUndertakingMemberDataRequest.schema.json" in {
      forAll { undertakingBusinessEntityUpdate: UndertakingBusinessEntityUpdate =>
        checkWrites[UndertakingBusinessEntityUpdate](undertakingBusinessEntityUpdate, "amendUndertakingMemberDataRequest")
      }
    }

    "match retrieveUndertakingSubsidiesRequest.schema.json" in {
      forAll { subsidyRetrieve: SubsidyRetrieve =>
        checkWrites[SubsidyRetrieve](subsidyRetrieve, "retrieveUndertakingSubsidiesRequest")
      }
    }
  }

  def checkWrites[A](in: A, schemaName: String)(implicit writes: Writes[A]): Assertion = {
    checkJson(Json.toJson(in), schemaName)
  }
}
