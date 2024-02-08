/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsJson, defaultAwaitTimeout}
import uk.gov.hmrc.eusubsidycompliancestub.BaseSpec
import uk.gov.hmrc.eusubsidycompliancestub.models.MonthlyExchangeRate

import java.time.LocalDate
import scala.math.Ordered.orderingToOrdered

class ExchangeRateControllerSpec extends BaseSpec {
  private val controller = new ExchangeRateController(app.injector.instanceOf[ControllerComponents])

  "ExchangeRateController" must {
    "return exchange rates for the last 12 months" in {
      val response = controller.retrieveExchangeRates()(FakeRequest(GET, "/budg/inforeuro/api/public/currencies/gbp"))
      val exchangeRates = contentAsJson(response).as[List[MonthlyExchangeRate]]
      val `1yearAgo` = LocalDate.now().withDayOfMonth(1).minusYears(1)
      exchangeRates.filter(_.dateStart > `1yearAgo`).distinctBy(_.dateStart).size mustBe 12
    }
  }
}
