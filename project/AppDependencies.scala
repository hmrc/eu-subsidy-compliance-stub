import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.13.0",
    "org.typelevel"           %% "cats-core"                  % "2.6.1",
    "com.chuusai"             %% "shapeless"                  % "2.3.7",
    "uk.gov.hmrc"             %% "stub-data-generator"        % "0.5.3",
    "com.github.fge"          %  "json-schema-validator"      % "2.2.6"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "5.13.0"             % Test,
    "org.scalatest"           %% "scalatest"                  % "3.0.8"                 % Test,
    "org.scalatestplus"       %% "mockito-3-4"                % "3.2.9.0" % "test",

    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8"            % "test, it"
  )
}
