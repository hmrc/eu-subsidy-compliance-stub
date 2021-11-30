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

import play.api.libs.json.{JsString, JsValue}
import play.api.mvc.Action
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliancestub.models.{SubsidyUpdate}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.UndertakingRef
import uk.gov.hmrc.eusubsidycompliancestub.util.TestInstances

class SubsidyControllerSpec extends BaseControllerSpec {

  private val controller: SubsidyController =
    app.injector.instanceOf[SubsidyController]

  val subsidyUpdate: SubsidyUpdate = TestInstances.arbSubsidyUpdate.arbitrary.sample.get
  val nilReturn: SubsidyUpdate = TestInstances.arbSubsidyUpdateNilReturn.arbitrary.sample.get
  "update subsidy usage" must {
    implicit val path: String = "/scp/amendundertakingsubsidyusage/v1"
    implicit val action: Action[JsValue] = controller.updateUsage

    "return 200  and a valid response for a successful amend" in {
      testResponse[SubsidyUpdate](
        subsidyUpdate,
        "updateSubsidyUsageResponse",
        play.api.http.Status.OK
      )
    }

    "return 200  and a valid response for a nil return" in {
      testResponse[SubsidyUpdate](
        nilReturn,
        "updateSubsidyUsageResponse",
        play.api.http.Status.OK
      )
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 004 " +
      "if SubsidyUpdate.undertakingIdentifier ends in 888 (duplicate acknowledgementRef)" in {
      testResponse[SubsidyUpdate](
        subsidyUpdate.copy(undertakingIdentifier = UndertakingRef("888")),
        "updateSubsidyUsageResponse",
        play.api.http.Status.OK,
        List(
          contentAsJson(_) \\ "status" mustEqual
            List(JsString("NOT_OK")),
          contentAsJson(_) \\ "paramValue" mustEqual
            List(JsString("004"), JsString("Duplicate submission acknowledgment reference"))
        )
      )
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 107 " +
      "if SubsidyUpdate.undertakingIdentifier ends in 777" in {
      testResponse[SubsidyUpdate](
        subsidyUpdate.copy(undertakingIdentifier = UndertakingRef("777")),
        "updateSubsidyUsageResponse",
        play.api.http.Status.OK,
        List(
          contentAsJson(_) \\ "status" mustEqual
            List(JsString("NOT_OK")),
          contentAsJson(_) \\ "paramValue" mustEqual
            List(JsString("107"), JsString("Undertaking reference in the API not Subscribed in ETMP"))
        )
      )
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 106 " +
      "if SubsidyUpdate.undertakingIdentifier ends in 666" in {
      val eori = subsidyUpdate.undertakingSubsidyAmendment.get.updates.head.businessEntityIdentifier.get
      testResponse[SubsidyUpdate](
        subsidyUpdate.copy(undertakingIdentifier = UndertakingRef("666")),
        "updateSubsidyUsageResponse",
        play.api.http.Status.OK,
        List(
          contentAsJson(_) \\ "status" mustEqual
            List(JsString("NOT_OK")),
          contentAsJson(_) \\ "paramValue" mustEqual
            List(JsString("106"), JsString(s"EORI not Subscribed in ETMP $eori"))
        )
      )
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 112 " +
      "if SubsidyUpdate.undertakingIdentifier ends in 555" in {
      val eori = subsidyUpdate.undertakingSubsidyAmendment.get.updates.head.businessEntityIdentifier.get
      testResponse[SubsidyUpdate](
        subsidyUpdate.copy(undertakingIdentifier = UndertakingRef("555")),
        "updateSubsidyUsageResponse",
        play.api.http.Status.OK,
        List(
          contentAsJson(_) \\ "status" mustEqual
            List(JsString("NOT_OK")),
          contentAsJson(_) \\ "paramValue" mustEqual
            List(JsString("112"), JsString(s"EORI $eori not linked with undertaking."))
        )
      )
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 111 " +
      "if SubsidyUpdate.undertakingIdentifier ends in 444" in {
      val sutId = subsidyUpdate.undertakingSubsidyAmendment.get.updates.head.subsidyUsageTransactionId.get
      testResponse[SubsidyUpdate](
        subsidyUpdate.copy(undertakingIdentifier = UndertakingRef("444")),
        "updateSubsidyUsageResponse",
        play.api.http.Status.OK,
        List(
          contentAsJson(_) \\ "status" mustEqual
            List(JsString("NOT_OK")),
          contentAsJson(_) \\ "paramValue" mustEqual
            List(JsString("111"), JsString(s"Subsidy allocation ID number $sutId or date is invalid is invalid"))
        )
      )
    }
  }
}