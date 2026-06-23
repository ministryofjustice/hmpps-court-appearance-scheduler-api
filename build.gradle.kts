import com.google.cloud.tools.jib.gradle.BuildImageTask
import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.5.0"
  kotlin("plugin.spring") version "2.4.0"
  kotlin("plugin.jpa") version "2.4.0"
  id("com.google.cloud.tools.jib") version "3.5.3"
  id("de.undercouch.download") version "5.7.0"
}

val hmppsKotlinVersion = "2.5.0"
val sentryVersion = "8.44.1"
val springDocVersion = "3.0.3"
val sqsStarterVersion = "7.4.0"
val swaggerParserVersion = "2.1.44"
val testContainersVersion = "1.21.4"
val uuidGeneratorVersion = "5.2.0"
val wiremockVersion = "3.13.2"

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:$hmppsKotlinVersion")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:$sqsStarterVersion")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
  implementation("com.fasterxml.uuid:java-uuid-generator:$uuidGeneratorVersion")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.data:spring-data-envers")
  implementation("io.sentry:sentry-spring-boot-4:$sentryVersion")

  runtimeOnly("org.springframework.boot:spring-boot-starter-flyway")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")

  testImplementation("org.testcontainers:postgresql:$testContainersVersion")
  testImplementation("org.testcontainers:localstack:$testContainersVersion")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:$hmppsKotlinVersion")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
  testImplementation("io.swagger.parser.v3:swagger-parser:$swaggerParserVersion") {
    exclude(group = "io.swagger.core.v3")
  }
}

kotlin {
  jvmToolchain(25)
}

tasks {
  withType<KotlinCompile> {
    compilerOptions {
      jvmTarget = JVM_25
    }
  }
  test {
    if (project.hasProperty("init-db")) {
      include("**/InitialiseDatabase.class")
    } else {
      exclude("**/InitialiseDatabase.class")
    }
  }

  val downloadDbCerts by registering(Download::class) {
    src("https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem")
    dest(layout.buildDirectory.file("root.crt"))
  }

  val copyAgentJar by registering(Copy::class) {
    from(layout.buildDirectory.dir("libs"))
    include("applicationinsights-agent*.jar")
    into(layout.buildDirectory.dir("agent"))
    rename("applicationinsights-agent(.+).jar", "agent.jar")
    dependsOn("assemble")
  }

  val jibBuildTar by getting {
    dependsOn.addAll(listOf(copyAgentJar, downloadDbCerts))
  }

  val jibDockerBuild by getting {
    dependsOn.addAll(listOf(copyAgentJar, downloadDbCerts))
  }

  withType<BuildImageTask>().named("jib") {
    doFirst {
      jib!!.to {
        tags = setOf(System.getenv("BUILD_NUMBER") ?: "dev")
        auth {
          username = System.getenv("GITHUB_USERNAME")
          password = System.getenv("GITHUB_PASSWORD")
        }
      }
    }
    dependsOn.addAll(listOf(copyAgentJar, downloadDbCerts))
  }
}

jib {
  container {
    creationTime.set("USE_CURRENT_TIMESTAMP")
    jvmFlags = mutableListOf("-Duser.timezone=Europe/London")
    mainClass = "uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.CourtAppearanceSchedulerApiKt"
    user = "2000:2000"
    environment = mapOf("BUILD_NUMBER" to (System.getenv("BUILD_NUMBER") ?: "dev"))
  }
  from {
    image = "eclipse-temurin:25-jre-jammy"
    platforms {
      platform {
        architecture = "amd64"
        os = "linux"
      }
      platform {
        architecture = "arm64"
        os = "linux"
      }
    }
  }
  to {
    image = "ghcr.io/ministryofjustice/hmpps-court-appearance-scheduler-api"
  }
  extraDirectories {
    paths {
      path {
        setFrom(layout.buildDirectory.dir("agent").get().asFile)
        includes.add("agent.jar")
        into = "/agent"
      }
      path {
        setFrom(layout.projectDirectory.asFile)
        includes.add("applicationinsights.json")
        into = "/agent"
      }
      path {
        setFrom(layout.buildDirectory)
        includes.add("root.crt")
        into = "/.postgresql"
      }
    }
  }
}
