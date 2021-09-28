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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.eusubsidycompliancestub.services.{DataGenerator, EisService, JsonSchemaChecker}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.eisRetrieveUndertakingResponse
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

  def retrieve: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      if (!JsonSchemaChecker[JsValue](json, "retrieveUndertakingRequest")) {
        // this should ideally be a BadRequest but the API specifies forbidden
        Future.successful(Forbidden(Json.toJson("TODO and error message"))) // TODO
      } else {
        val eori: String = (json \ "retrieveUndertakingRequest" \ "requestDetail" \ "idValue").as[String]

        // TODO think about if the Gen is going to return the valid and invalid responses
        val undertaking = eis.retrieveUndertaking(eori)
        Future.successful(Ok(Json.toJson(undertaking)(eisRetrieveUndertakingResponse)))

      }
    }
  }
}

