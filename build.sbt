import scoverage.ScoverageKeys

val appName = "eu-subsidy-compliance-stub"

PlayKeys.playDefaultPort := 9095

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.13.16",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += "-Wconf:src=routes/.*:s",
    ScoverageKeys.coverageExcludedPackages :=
      List(
        "<empty>",
        "Reverse.*",
        "app.Routes.*",
        "prod.*",
        "testOnlyDoNotUseInAppConf.*",
        "config.*"
      ).mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    Test / parallelExecution := false
  )
  .settings(resolvers += Resolver.jcenterRepo)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

//Check both integration and normal scopes so formatAndTest can be applied when needed more easily.
Test / test := (Test / test)
  .dependsOn(scalafmtCheckAll)
  .value

addCommandAlias("precommit", ";scalafmt;test:scalafmt;coverage;test;coverageReport")
