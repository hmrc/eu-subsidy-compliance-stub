import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val bootStrapVersion = "7.15.0"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-28" % bootStrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"    % "1.4.0",
    "org.typelevel" %% "cats-core"                 % "2.9.0",
    "com.chuusai"   %% "shapeless"                 % "2.3.10",
    "com.github.fge" % "json-schema-validator"     % "2.2.6",
    "uk.gov.hmrc"   %% "stub-data-generator"       % "1.1.0",
    "wolfendale"    %% "scalacheck-gen-regexp"     % "0.1.2"
  )

  val test = Seq(
    "org.scalatestplus"      %% "scalacheck-1-15"        % "3.2.11.0"       % Test,
    "com.typesafe.play"      %% "play-test"              % current          % Test,
    "org.scalacheck"         %% "scalacheck"             % "1.17.0"         % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"     % "5.1.0"          % Test,
    "uk.gov.hmrc"            %% "bootstrap-test-play-28" % bootStrapVersion % Test,
    "org.scalatestplus"      %% "mockito-3-4"            % "3.2.10.0"       % Test,
    "com.vladsch.flexmark"    % "flexmark-all"           % "0.64.6"         % Test,
    "wolfendale"             %% "scalacheck-gen-regexp"  % "0.1.2"          % Test
  )
}
