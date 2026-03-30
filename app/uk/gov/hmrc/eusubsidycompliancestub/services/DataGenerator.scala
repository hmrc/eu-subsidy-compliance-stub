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

import uk.gov.hmrc.eusubsidycompliancestub.models.types.{AmendmentType, EORI, IndustrySectorLimit, PhoneNumber, Sector, TraderRef, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.models.{types, _}

import java.time.LocalDate
import shapeless.tag.@@

import java.time.temporal.ChronoUnit
import java.util.regex.Pattern
import scala.util.Random

object DataGenerator {

  private def randomBetweenInclusive(min: Int, max: Int, random: Random): Int =
    random.between(min, max + 1)

  private def randomDateBetween(start: LocalDate, end: LocalDate, random: Random): LocalDate = {
    val days = ChronoUnit.DAYS.between(start, end).toInt
    start.plusDays(random.between(0, days + 1).toLong) // inclusive
  }

  private def maybe[A](make: => A, random: Random, noneProbability: Double = 0.5): Option[A] =
    if (random.nextDouble() < noneProbability) None else Some(make)

  private def lowerAlphaString(length: Int, random: Random): String =
    Iterator
      .continually(('a' + random.nextInt(26)).toChar)
      .take(length)
      .mkString

  private def variableLengthString(min: Int, max: Int, random: Random = new Random()): String = {
    val len = randomBetweenInclusive(min, max, random)
    lowerAlphaString(len, random)
  }

  private def genEORIPrefix(random: Random = new Random()): String =
    if (random.nextBoolean()) "GB" else "XI"

  private def genEORIDigits(random: Random = new Random()): String = {
    val length = if (random.nextBoolean()) 12 else 15
    Iterator.continually(random.nextInt(10)).take(length).mkString
  }

  def genEORI(random: Random = new Random()): EORI = {
    val prefix = genEORIPrefix(random)
    val rest = genEORIDigits(random)
    EORI(s"$prefix$rest")
  }

  def genUndertakingRef(random: Random = new Random(), maxRetries: Int = 20): UndertakingRef = {
    val pattern = Pattern.compile(UndertakingRef.regex)

    def candidate(): String =
      random.alphanumeric.filter(_.isLetterOrDigit).take(randomBetweenInclusive(1, 35, random)).mkString

    LazyList
      .continually(candidate())
      .find(s => pattern.matcher(s).matches())
      .map(UndertakingRef.apply)
      .getOrElse(throw new IllegalStateException(s"Could not generate UndertakingRef matching ${UndertakingRef.regex}"))
  }

  def genContactDetails(random: Random = new Random()): ContactDetails = {
    val phone = maybe(PhoneNumber(variableLengthString(1, 24, random)), random)
    val mobile = maybe(PhoneNumber(variableLengthString(1, 24, random)), random)
    ContactDetails(phone, mobile)
  }

  private def genBusinessEntity(random: Random = new Random()): BusinessEntity = {
    val e = genEORI(random)
    val contactDetails = maybe(genContactDetails(random), random)
    BusinessEntity(e, leadEORI = false, contactDetails)
  }

  def genBusinessEntityUpdate(random: Random = new Random()): BusinessEntityUpdate = {
    val amendmentType = List(AmendmentType.add, AmendmentType.amend, AmendmentType.delete)(random.nextInt(3))
    val amendmentEffectiveDate = randomDateBetween(LocalDate.of(2020, 1, 1), LocalDate.now(), random)
    val businessEntity = genBusinessEntity(random)

    BusinessEntityUpdate(amendmentType, amendmentEffectiveDate, businessEntity)
  }

  private def genIndustrySectorLimit(random: Random = new Random()): @@[BigDecimal, types.IndustrySectorLimit.Tag] = {
    val max = BigDecimal("99999999999.99")
    val raw = BigDecimal(random.nextDouble()) * max
    val scaled = raw.setScale(2, BigDecimal.RoundingMode.DOWN)

    IndustrySectorLimit(scaled.bigDecimal.stripTrailingZeros())
  }

  private def genLastSubsidyUsageUpdt(random: Random = new Random()): LocalDate =
    randomDateBetween(LocalDate.of(2020, 1, 1), LocalDate.now(), random)

  def genRetrievedUndertaking(eori: EORI, random: Random = new Random()): Undertaking = {
    val ref = variableLengthString(1, 17, random)
    val name = variableLengthString(1, 105, random)
    val industrySector = List(0, 1, 2, 3)(random.nextInt(4))
    val industrySectorLimit = genIndustrySectorLimit(random)
    val lastSubsidyUsageUpdt = genLastSubsidyUsageUpdt(random)
    val undertakingStatus = List(0, 1, 5, 9)(random.nextInt(4))
    val nBusinessEntities = randomBetweenInclusive(2, 25, random)
    val undertakingBusinessEntity = List.fill(nBusinessEntities)(genBusinessEntity(random))

    Undertaking(
      reference = UndertakingRef(ref),
      name = UndertakingName(name),
      industrySector = Sector(industrySector),
      industrySectorLimit = industrySectorLimit,
      lastSubsidyUsageUpdt = Some(lastSubsidyUsageUpdt),
      undertakingStatus = Some(undertakingStatus),
      undertakingBusinessEntity = undertakingBusinessEntity.head.copy(
        businessEntityIdentifier = eori,
        leadEORI = true
      ) :: undertakingBusinessEntity.tail
    )
  }

  def genTraderRef(random: Random = new Random()): TraderRef = {
    val length = random.between(0, 36) // 0..35 inclusive (matches your regex)
    val value = random.alphanumeric.filter(_.isLetterOrDigit).take(length).mkString
    TraderRef(value)
  }

  def genMonthlyExchangeRate(year: Int, month: Int, random: Random = new Random()): MonthlyExchangeRate = {
    val min = BigDecimal("0.8")
    val max = BigDecimal("0.95")
    val rawAmount = min + (max - min) * BigDecimal(random.nextDouble())
    val amount = rawAmount.setScale(4, BigDecimal.RoundingMode.DOWN)
    val dateStart = LocalDate.of(year, month, 1)

    MonthlyExchangeRate(
      currencyIso = "GBP",
      refCurrencyIso = "EUR",
      amount = amount,
      dateStart = dateStart,
      dateEnd = dateStart.plusMonths(1).minusDays(1)
    )
  }

  def getSampleValue[A](sample: => Option[A], retries: Int = 10): A =
    LazyList
      .continually(sample)
      .take(retries)
      .flatten
      .headOption
      .getOrElse(throw new Exception(s"Can't generate a sample after $retries retries"))

  def getSampleValue[A](sample: Random => Option[A], seed: Option[Long], retries: Int): A = {
    val rnd = seed.map(new Random(_)).getOrElse(new Random())
    LazyList
      .continually(sample(rnd))
      .take(retries)
      .flatten
      .headOption
      .getOrElse(throw new Exception(s"Can't generate a sample after $retries retries"))
  }
}
