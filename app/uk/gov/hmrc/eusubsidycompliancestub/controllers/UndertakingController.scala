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

import cats.implicits._
import com.github.fge.jsonschema.core.report.ProcessingMessage
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.eusubsidycompliancestub.services.{DataGenerator, EisService, JsonSchemaChecker}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.{ErrorDetail, ResponseCommon, eisRetrieveUndertakingResponse}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EisStatus, ErrorCode, ErrorMessage}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.smartstub._

import scala.concurrent.Future


@Singleton
class UndertakingController @Inject()(
  cc: ControllerComponents,
  authAndEnvAction: AuthAndEnvAction,
  eis: EisService
) extends BackendController(cc) {

  def create(): Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>

//      if (!JsonSchemaChecker[JsValue](json, "retrieveUndertakingRequest")) {
//        // this should ideally be a BadRequest but the API specifies forbidden
//        Future.successful(Forbidden(Json.toJson("TODO and error message"))) // TODO
//      } else {
//        val eori: String = (json \ "retrieveUndertakingRequest" \ "requestDetail" \ "idValue").as[String]
//
//        // TODO think about if the Gen is going to return the valid and invalid responses
//        val undertaking = eis.retrieveUndertaking(eori)
//
//        Future.successful(Ok(Json.toJson(undertaking)(eisRetrieveUndertakingResponse)))
//
//      }
      ???
    }
  }

  // we get 403 if the json schema is violated, 500 is they err or 200 is OK
  // we get 107 is they can't find an undertaking (should be 404)
  // two error codes that will be masked by 403
  // and 103 that will probably be masked too or mean something else...
  // think we should handle the 403 and 500 as UpstreamErrorResponse
  // sadly have to build a wrapper just for the 107 & 103
  def retrieve: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      val processingReport = JsonSchemaChecker[JsValue](json, "retrieveUndertakingRequest")

      if (!processingReport.isSuccess) {
        // this should ideally be a BadRequest but the API specifies forbidden
        val errorMsg: String = processingReport.iterator().next().getMessage
        val errorDetail:ErrorDetail =
          ErrorDetail(
            ErrorCode("403"),
            ErrorMessage("Invalid message : BEFORE TRANSFORMATION"),
            List(errorMsg)
          )

        Future.successful(Forbidden(Json.toJson(errorDetail)))
      } else {
        val eori: String = (json \ "retrieveUndertakingRequest" \ "requestDetail" \ "idValue").as[String]
        eori match {
          case a if a.endsWith("999") =>
            val errorDetail = ??? // TODO AD
            Future.successful(InternalServerError(Json.toJson(errorDetail)))
          case b if b.endsWith("888") => ??? // TODO look at the spec and send the 107, also ask what 003, 055 & 067 are for ALSO 004?! USE DIFFERENT WRITE
          case _ =>
            val undertaking = eis.retrieveUndertaking(eori)
            Future.successful(Ok(Json.toJson(undertaking)(eisRetrieveUndertakingResponse)))
        }
      }
    }
  }
}

