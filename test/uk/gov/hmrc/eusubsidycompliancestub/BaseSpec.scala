/*
 * Copyright 2023 HM Revenue & Customs
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

import com.github.fge.jsonschema.core.report.AbstractProcessingReport
import org.scalatest.Assertion
import org.scalatestplus.mockito._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.{Application, Logging}
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, Result, Results}
import play.api.test.Helpers.{contentAsJson, status, _}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.eusubsidycompliancestub.services.JsonSchemaChecker

import scala.concurrent.Future

abstract class BaseSpec
    extends PlaySpec
    with MockitoSugar
    with Results
    with GuiceOneAppPerSuite
    with ScalaCheckDrivenPropertyChecks
    with Logging {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false, "microservice.metrics.graphite.enabled" -> false)
      .build()

  val fakeHeaders =
    FakeHeaders(
      Seq(
        "Content-type" -> "application/json",
        HeaderNames.AUTHORIZATION -> s"Bearer FOOBAR",
        "Environment" -> "ist0"
      )
    )

  def checkJson(json: JsValue, schemaName: String): Assertion = {
    withClue("checking schema") {
      JsonSchemaChecker[JsValue](json, schemaName) match {
        case _: AbstractProcessingReport => succeed
        case error => fail(error.toString)
      }
    }
  }

  def fakePost(body: JsValue)(implicit path: String): FakeRequest[JsValue] =
    FakeRequest(POST, path, fakeHeaders, body)

  def testResponse[A](
    model: A,
    responseSchemaName: String,
    expectedStatus: Int,
    extraChecks: List[Future[Result] => Assertion] = List.empty,
    debug: Boolean = false
  )(implicit action: Action[JsValue], writes: Writes[A], path: String): Future[Result] = {
    val json = Json.toJson(model)
    if (debug) logger.debug(Json.prettyPrint(json))
    val result = action.apply(fakePost(json))
    if (debug) logger.debug(Json.prettyPrint(contentAsJson(result)))
    checkJson(contentAsJson(result), responseSchemaName)
    status(result) mustEqual expectedStatus
    extraChecks.map(f => f(result))
    result
  }
}
