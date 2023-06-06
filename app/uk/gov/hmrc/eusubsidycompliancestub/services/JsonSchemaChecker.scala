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

package uk.gov.hmrc.eusubsidycompliancestub.services

import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchemaFactory
import play.api.Logger
import play.api.libs.json.{Format, Json}

object JsonSchemaChecker {

  val logger = Logger(getClass)

  def retrieveSchema(file: String): JsonNode = schema(s"/test/schema/$file.schema.json")

  private def schema(path: String): JsonNode = {
    val stream = getClass.getResourceAsStream(path)
    val schemaText = scala.io.Source.fromInputStream(stream).getLines.mkString
    stream.close()
    JsonLoader.fromString(schemaText)
  }

  def apply[A](model: A, file: String)(implicit format: Format[A]): ProcessingReport = {
    val schema = retrieveSchema(file)
    val validator = JsonSchemaFactory.byDefault.getValidator
    val json = JsonLoader.fromString(Json.prettyPrint(Json.toJson(model)))
    val processingReport: ProcessingReport = validator.validate(schema, json)
    if (!processingReport.isSuccess) processingReport.forEach { x =>
      x.getMessage
      logger.warn(
        s"json schema validation problem: ${x.getMessage}"
      )
    }
    processingReport
  }

}
