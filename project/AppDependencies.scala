import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val bootStrapVersion = "9.13.0"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % bootStrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"    % "2.6.0",
    "org.typelevel" %% "cats-core"                 % "2.9.0",
    "com.chuusai"   %% "shapeless"                 % "2.3.10",
    "com.github.fge" % "json-schema-validator"     % "2.2.6",
    "uk.gov.hmrc"   %% "stub-data-generator"       % "1.4.0",
    "wolfendale"    %% "scalacheck-gen-regexp"     % "0.1.2"
  )

  val test = Seq(
    "org.scalatestplus"      %% "scalacheck-1-15"        % "3.2.11.0"       % Test,
    "org.playframework"      %% "play-test"              % current          % Test,
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootStrapVersion % Test,
    "wolfendale"             %% "scalacheck-gen-regexp"  % "0.1.2"          % Test,
    "uk.gov.hmrc"            %% "stub-data-generator"    % "1.4.0"          % Test
  )
}
