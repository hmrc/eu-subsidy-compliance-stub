import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val bootStrapVersion = "10.7.0"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % bootStrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"    % "2.12.0",
    "org.typelevel" %% "cats-core"                 % "2.13.0",
    "com.github.java-json-tools" % "json-schema-validator"         % "2.2.14"
  )

  val test = Seq(
    "org.scalatestplus"      %% "scalacheck-1-15"        % "3.2.11.0"       % Test,
    "org.playframework"      %% "play-test"              % current          % Test,
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootStrapVersion % Test,
    "io.github.wolfendale"   %% "scalacheck-gen-regexp"  % "1.1.0"          % Test
  )
}
