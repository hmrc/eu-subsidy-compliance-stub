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

package uk.gov.hmrc.eusubsidycompliancestub.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.eusubsidycompliancestub.services.Store
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future


@Singleton
class StoreController @Inject()(
  cc: ControllerComponents,
  authAndEnvAction: AuthAndEnvAction,
) extends BackendController(cc) {

  def clearStore: Action[AnyContent] = Action.async { _ =>
    val isClear = Store.clear()
    Future.successful(Ok(Json.toJson(s"cleared: $isClear")))
  }

  def show: Action[AnyContent] = Action.async { _ =>
    val store = Store.undertakings.undertakingStore
    Future.successful(Ok(store.toString))
  }

}