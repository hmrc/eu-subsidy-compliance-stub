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

import play.api.mvc.{BaseController, ControllerComponents}

import javax.inject.{Inject, Singleton}

@Singleton
class ExchangeRateController @Inject() (
  override val controllerComponents: ControllerComponents,
  authAndEnvAction: AuthAndEnvAction
) extends BaseController {

  // This replicates the happy path response of the Europa currency API and returns a fixed response containing the
  // last two exchange rates for January 2021.
  //
  // No validation of the parameters or any other behaviour has been implemented.
  //
  // Example request
  //    GET curl -X GET  'https://sdw-wsrest.ecb.europa.eu/service/data/EXR/D.GBP.EUR.SP00.A?startPeriod=2021-01&endPeriod=2021-01&detail=dataonly&lastNObservations=2'

  def retrieveExchangeRate(
    startPeriod: String,
    endPeriod: String,
    detail: String,
    lastNObservations: String
  ) = Action { _ =>
    // For now we just return a hardcoded response with a canned valid response.
    val exchangeRateResponse =
      """{
        |    "header": {
        |        "id": "44b66645-7480-4ed4-8df2-df9e1b04b78f",
        |        "test": false,
        |        "prepared": "2022-06-29T12:56:33.455+02:00",
        |        "sender": {
        |            "id": "ECB"
        |        }
        |    },
        |    "dataSets": [
        |        {
        |            "action": "Replace",
        |            "validFrom": "2022-06-29T12:56:33.455+02:00",
        |            "series": {
        |                "0:0:0:0:0": {
        |                    "observations": {
        |                        "0": [
        |                            0.88603
        |                        ],
        |                        "1": [
        |                            0.88383
        |                        ]
        |                    }
        |                }
        |            }
        |        }
        |    ],
        |    "structure": {
        |        "links": [
        |            {
        |                "title": "Exchange Rates",
        |                "rel": "dataflow",
        |                "href": "https://sdw-wsrest.ecb.europa.eu:443/service/dataflow/ECB/EXR/1.0"
        |            }
        |        ],
        |        "name": "Exchange Rates",
        |        "dimensions": {
        |            "series": [
        |                {
        |                    "id": "FREQ",
        |                    "name": "Frequency",
        |                    "values": [
        |                        {
        |                            "id": "D",
        |                            "name": "Daily"
        |                        }
        |                    ]
        |                },
        |                {
        |                    "id": "CURRENCY",
        |                    "name": "Currency",
        |                    "values": [
        |                        {
        |                            "id": "GBP",
        |                            "name": "UK pound sterling"
        |                        }
        |                    ]
        |                },
        |                {
        |                    "id": "CURRENCY_DENOM",
        |                    "name": "Currency denominator",
        |                    "values": [
        |                        {
        |                            "id": "EUR",
        |                            "name": "Euro"
        |                        }
        |                    ]
        |                },
        |                {
        |                    "id": "EXR_TYPE",
        |                    "name": "Exchange rate type",
        |                    "values": [
        |                        {
        |                            "id": "SP00",
        |                            "name": "Spot"
        |                        }
        |                    ]
        |                },
        |                {
        |                    "id": "EXR_SUFFIX",
        |                    "name": "Series variation - EXR context",
        |                    "values": [
        |                        {
        |                            "id": "A",
        |                            "name": "Average"
        |                        }
        |                    ]
        |                }
        |            ],
        |            "observation": [
        |                {
        |                    "id": "TIME_PERIOD",
        |                    "name": "Time period or range",
        |                    "role": "time",
        |                    "values": [
        |                        {
        |                            "id": "2021-01-28",
        |                            "name": "2021-01-28",
        |                            "start": "2021-01-28T00:00:00.000+01:00",
        |                            "end": "2021-01-28T23:59:59.999+01:00"
        |                        },
        |                        {
        |                            "id": "2021-01-29",
        |                            "name": "2021-01-29",
        |                            "start": "2021-01-29T00:00:00.000+01:00",
        |                            "end": "2021-01-29T23:59:59.999+01:00"
        |                        }
        |                    ]
        |                }
        |            ]
        |        }
        |    }
        |}
        |""".stripMargin
    Ok(exchangeRateResponse).as("application/json")
  }

}
