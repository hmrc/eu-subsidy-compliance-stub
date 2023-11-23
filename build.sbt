import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings

val appName = "eu-subsidy-compliance-stub"

val silencerVersion = "1.7.8"

PlayKeys.playDefaultPort := 9095

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.13.8",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // ***************
    // Use the silencer plugin to suppress warnings
    scalacOptions += "-P:silencer:pathFilters=routes",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
    // ***************
  )
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

//Check both integration and normal scopes so formatAndTest can be applied when needed more easily.
Test / test := (Test / test)
  .dependsOn(scalafmtCheckAll)
  .value

IntegrationTest / test := (IntegrationTest / test)
  .dependsOn(scalafmtCheckAll)
  .value

addCommandAlias("precommit", ";scalafmt;test:scalafmt;it:test::scalafmt;coverage;test;it:test;coverageReport")

