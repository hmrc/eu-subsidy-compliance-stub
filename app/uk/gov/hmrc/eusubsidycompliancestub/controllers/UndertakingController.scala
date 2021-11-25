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
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.{eisCreateUndertakingResponse, eisRetrieveUndertakingResponse}
import uk.gov.hmrc.eusubsidycompliancestub.services.EisService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future

@Singleton
class UndertakingController @Inject()(
  cc: ControllerComponents,
  authAndEnvAction: AuthAndEnvAction,
  eis: EisService
) extends BackendController(cc) {

  def create: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "createUndertakingRequest") match {
        case Some(errorDetail) => // payload schema check failed
          Future.successful(Forbidden(Json.toJson(errorDetail)))
        case _ =>
          val eori: String = (json \ "createUndertakingRequest" \ "requestDetail" \ "businessEntity" \ "idValue").as[String]
          eori match {
            case a if a.endsWith("999") => // fake 500
              Future.successful(InternalServerError(Json.toJson(errorDetailFor500)))
            case b if b.endsWith("888") => // fake 004
              val dupeAckRef: JsValue = Json.obj(
                "createUndertakingResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "004",
                    "Duplicate submission acknowledgment reference"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(dupeAckRef)))
            case c if c.endsWith("777") =>
              val dupeEori: JsValue = Json.obj(
                "createUndertakingResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "101",
                    s"EORI $eori already associated with another Undertaking $eori"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(dupeEori)))
            case d if d.endsWith("666") =>
              val invalidEori: JsValue = Json.obj(
                "createUndertakingResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "102",
                    s"Invalid EORI number $eori"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(invalidEori)))
            case e if e.endsWith("555") =>
              val missingPostcode: JsValue = Json.obj(
                "createUndertakingResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "113",
                    s"Postcode missing for the address"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(missingPostcode)))
            case _ =>
              val undertakingRef = eis.undertakingRef(eori)
              Future.successful(Ok(Json.toJson(undertakingRef)))
          }
      }
    }
  }

  def retrieve: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "retrieveUndertakingRequest") match {
        case Some(errorDetail) => // payload fails schema check
          Future.successful(Forbidden(Json.toJson(errorDetail)))
        case _ =>
          val eori: String = (json \ "retrieveUndertakingRequest" \ "requestDetail" \ "idValue").as[String]
          eori match {
            case a if a.endsWith("999") => // fake 500
              Future.successful(InternalServerError(Json.toJson(errorDetailFor500)))
            case b if b.endsWith("888") => // fake not found (ideally should have been 404)
              val noUndertakingFoundResponse: JsValue = Json.obj(
                "retrieveUndertakingResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "107",
                    "Undertaking reference in the API not Subscribed in ETMP"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(noUndertakingFoundResponse)))
            case _ => // successful retrieval
              val undertaking = eis.retrieveUndertaking(eori)
              Future.successful(Ok(Json.toJson(undertaking)(eisRetrieveUndertakingResponse)))
          }
      }
    }
  }
}
