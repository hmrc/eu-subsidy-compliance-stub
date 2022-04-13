/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancestub

import cats.implicits._
import play.api.libs.json.JsValue
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.ErrorDetails
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{ErrorCode, ErrorMessage}
import uk.gov.hmrc.eusubsidycompliancestub.services.JsonSchemaChecker

package object controllers {

  val errorDetailFor500 =
    ErrorDetails(
      ErrorCode("500"),
      ErrorMessage("Error connecting to the server"),
      "112233 - Send timeout"
    )

  def processPayload(
    json: JsValue,
    schemaName: String,
    errorCode: ErrorCode = ErrorCode("403"),
    errorMessage: ErrorMessage = ErrorMessage("Invalid message : BEFORE TRANSFORMATION")
  ): Option[ErrorDetails] = {
    val processingReport = JsonSchemaChecker[JsValue](json, schemaName)
    if (processingReport.isSuccess) {
      none[ErrorDetails]
    } else {
      val sourceFaultDetailHead: String = processingReport.iterator().next().getMessage
      ErrorDetails(
        errorCode = ErrorCode(errorCode),
        errorMessage = ErrorMessage(errorMessage),
        sourceFaultDetail = sourceFaultDetailHead
      ).some
    }
  }

}
