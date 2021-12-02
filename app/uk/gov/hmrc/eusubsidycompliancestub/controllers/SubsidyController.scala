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
import uk.gov.hmrc.eusubsidycompliancestub.models.SubsidyUpdate
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.eisUpdateSubsidyUsageResponse
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, SubsidyRef, UndertakingRef}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future

@Singleton
class SubsidyController @Inject()(
  cc: ControllerComponents,
  authAndEnvAction: AuthAndEnvAction
) extends BackendController(cc) {

  def updateUsage: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "updateSubsidyUsageRequest") match {
        case Some(errorDetail) => // payload fails schema check
          Future.successful(Forbidden(Json.toJson(errorDetail)))
        case _ =>
          val update: SubsidyUpdate = json.as[SubsidyUpdate]
          val undertakingRef: String = (json \ "undertakingIdentifier").as[UndertakingRef]
          undertakingRef match {
            case a if a.endsWith("999") => // fake 500
              Future.successful(InternalServerError(Json.toJson(errorDetailFor500)))
            case b if b.endsWith("888") => // fake 004
              val dupeAckRef: JsValue = Json.obj(
                "amendUndertakingSubsidyUsageResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "004",
                    "Duplicate submission acknowledgment reference"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(dupeAckRef)))
            case c if c.endsWith("777") =>
              val dupeEori: JsValue = Json.obj(
                "amendUndertakingSubsidyUsageResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "107",
                    s"Undertaking reference in the API not Subscribed in ETMP"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(dupeEori)))
            case d if d.endsWith("666") =>
              val eori = (json \ "undertakingSubsidyAmendment" \ 0 \ "businessEntityIdentifier").as[EORI]
              val invalidEori: JsValue = Json.obj(
                "amendUndertakingSubsidyUsageResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "106",
                    s"EORI not Subscribed in ETMP $eori"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(invalidEori)))
            case e if e.endsWith("555") =>
              val eori = (json \ "undertakingSubsidyAmendment" \ 0 \ "businessEntityIdentifier").as[EORI]
              val invalidEori: JsValue = Json.obj(
                "amendUndertakingSubsidyUsageResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "112",
                    s"EORI $eori not linked with undertaking." // TODO check full stop
                  )
                )
              )
              Future.successful(Ok(Json.toJson(invalidEori)))
            case f if f.endsWith("444") =>
              val sutID = (json \ "undertakingSubsidyAmendment" \ 0 \ "subsidyUsageTransactionId").as[SubsidyRef]
              val invalidEori: JsValue = Json.obj(
                "amendUndertakingSubsidyUsageResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "111",
                    s"Subsidy allocation ID number $sutID or date is invalid is invalid" // TODO this string looks wrong
                  )
                )
              )
              Future.successful(Ok(Json.toJson(invalidEori)))

            case _ => // n.b. handles create, amend, delete and nil submission
              Future.successful(Ok(Json.toJson(update)(eisUpdateSubsidyUsageResponse)))
          }
      }
    }
  }

  def retrieveUsage: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "retrieveUndertakingSubsidiesRequest") match {
        case Some(errorDetail) => // payload fails schema check
          Future.successful(Forbidden(Json.toJson(errorDetail)))
        case _ => ???
      }
    }
  }
}
