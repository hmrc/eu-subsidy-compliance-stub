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
import uk.gov.hmrc.eusubsidycompliancestub.services.DataGenerator
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future


@Singleton
class UndertakingController @Inject()(
  cc: ControllerComponents,
  generator: DataGenerator,
  authAndEnvAction: AuthAndEnvAction
) extends BackendController(cc) {

  def create(): Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis._
      val seed: Long = (json \ "retrieveUndertakingRequest" \ "requestDetail" \ "idValue").as[String].toLong
      val undertaking = generator.genUndertaking(seed)
      Future.successful(Ok(Json.toJson(undertaking)))
    }
  }


  def retrieve(): Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      ???
    }
  }
}

