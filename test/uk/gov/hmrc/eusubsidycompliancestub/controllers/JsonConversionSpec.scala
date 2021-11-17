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
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.eusubsidycompliancestub.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.{ErrorDetail, eisRetrieveUndertakingResponse, retrieveUndertakingEORIWrites}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancestub.services.JsonSchemaChecker
import uk.gov.hmrc.eusubsidycompliancestub.util.TestInstances._

class JsonConversionSpec extends BaseControllerSpec {

  "Json Writes " must {
    "match retrieveUndertakingResponse.schema.json" in {
      forAll { undertaking: Undertaking =>
        checkWrites[Undertaking](undertaking,"retrieveUndertakingResponse")
      }
    }

    "match retrieveUndertakingRequest.schema.json" in {
      forAll { eori: EORI =>
        checkWrites[EORI](eori, "retrieveUndertakingRequest")
      }
    }

    "match errorDetailResponse.schema.json" in {
      forAll { errorDetail: ErrorDetail =>
        checkWrites[ErrorDetail](errorDetail, "errorDetailResponse")
      }
    }
  }

  def checkWrites[A](in: A, schema: String)(implicit writes: Writes[A]): Assertion = {
    val json = Json.toJson(in)
    JsonSchemaChecker[JsValue](
      json,
      schema
    ).isSuccess mustEqual true
  }
}
