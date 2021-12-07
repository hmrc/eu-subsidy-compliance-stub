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

package uk.gov.hmrc.eusubsidycompliancestub

import java.time.LocalDateTime

import cats.implicits._
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.{ErrorDetail, Params, ResponseCommon}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EisParamName, EisParamValue, EisStatus, EisStatusString, ErrorCode, ErrorMessage}
import uk.gov.hmrc.eusubsidycompliancestub.services.JsonSchemaChecker

package object controllers {

  val errorDetailFor500 =
    ErrorDetail(
      ErrorCode("500"),
      ErrorMessage("Error connecting to the server"),
      List("112233 - Send timeout")
    )

  def notOkCommonResponse(api :String, errorCode: String, errorText: String): JsObject = {
    Json.obj(
      api -> Json.obj(
        "responseCommon" -> badResponseCommon(
          errorCode,
          errorText
        )
      )
    )
  }

  def badResponseCommon(
    errorCode: String,
    errorText: String
  ): ResponseCommon = {
    ResponseCommon(
      EisStatus.NOT_OK,
      EisStatusString("String"), // taken verbatim from spec
      LocalDateTime.now,
      List(
        Params(
          EisParamName.ERRORCODE,
          EisParamValue(errorCode)
        ),
        Params(
          EisParamName.ERRORTEXT,
          EisParamValue(errorText)
        )
      ).some
    )
  }

  def processPayload(
    json: JsValue,
    schemaName: String,
    errorCode: ErrorCode = ErrorCode("403"),
    errorMessage: ErrorMessage = ErrorMessage("Invalid message : BEFORE TRANSFORMATION")
  ): Option[ErrorDetail] = {
    val processingReport = JsonSchemaChecker[JsValue](json, schemaName)
    if (processingReport.isSuccess) {
      none[ErrorDetail]
    } else {
      val sourceFaultDetailHead: String = processingReport.iterator().next().getMessage
      ErrorDetail(
        ErrorCode(errorCode),
        ErrorMessage(errorMessage),
        List(sourceFaultDetailHead)
      ).some
    }
  }

}
