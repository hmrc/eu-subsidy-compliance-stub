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

import javax.inject.Inject
import play.api.http.HeaderNames
import play.api.mvc.Results.{Forbidden, Unauthorized}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait ExtraActions extends ActionBuilder[Request,AnyContent] with ActionFilter[Request] {
  val controllerComponents: ControllerComponents
  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.defaultBodyParser
  override protected def executionContext: ExecutionContext = controllerComponents.executionContext
}

class AuthAndEnvAction @Inject()(cc: ControllerComponents) extends ExtraActions {

  override val controllerComponents: ControllerComponents = cc
  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    Future.successful(
      (request.headers.get(HeaderNames.AUTHORIZATION), request.headers.get("Environment")) match {
        case (None, _) =>
          Some(Unauthorized(""))
        case (_, None) =>
          Some(Forbidden(""))
        case (_, Some(x)) if !x.matches("^(ist0|clone|live)$") =>
          Some(Forbidden(""))
        case _ =>
          None
      }
    )
  }

}
