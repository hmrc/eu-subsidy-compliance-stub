/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancestub.models.beneficiaryResponses

import play.api.libs.json.{Format, Json}

case class BeneficiaryDetail(
  eori: String,
  benName: Option[String],
  benIDType: Option[String],
  benIDValue: Option[String],
  validated: Boolean
)

object BeneficiaryDetail {
  implicit val format: Format[BeneficiaryDetail] = Json.format[BeneficiaryDetail]
}

case class BeneficiarySuccess(
  processingDate: String,
  beneficiaryInfo: Seq[BeneficiaryDetail]
)

object BeneficiarySuccess {
  implicit val format: Format[BeneficiarySuccess] = Json.format[BeneficiarySuccess]
}

case class BeneficiaryValidationSuccessResponse(success: BeneficiarySuccess)

object BeneficiaryValidationSuccessResponse {
  implicit val format: Format[BeneficiaryValidationSuccessResponse] =
    Json.format[BeneficiaryValidationSuccessResponse]
}

case class BeneficiaryErrorDetail(
  processingDate: String,
  code: String,
  text: String
)

object BeneficiaryErrorDetail {
  implicit val format: Format[BeneficiaryErrorDetail] = Json.format[BeneficiaryErrorDetail]
}

case class BeneficiaryValidationErrorResponse(errors: BeneficiaryErrorDetail)

object BeneficiaryValidationErrorResponse {
  implicit val format: Format[BeneficiaryValidationErrorResponse] =
    Json.format[BeneficiaryValidationErrorResponse]
}
