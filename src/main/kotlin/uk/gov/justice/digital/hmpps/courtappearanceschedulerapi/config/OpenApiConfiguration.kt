package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config

import io.swagger.v3.core.util.PrimitiveType
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.expression.BeanFactoryResolver
import org.springframework.expression.spel.SpelEvaluationException
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.method.HandlerMethod
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.OpenApiTags.INTEGRATIONS
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.OpenApiTags.SYNC
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.OpenApiTags.UI
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object OpenApiTags {
  const val INTEGRATIONS = "Integrations"
  const val SYNC = "Sync"
  const val UI = "UI"
}

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties, private val context: ApplicationContext) {
  private val version: String? = buildProperties.version

  @Bean
  fun localDateTimeCustomiser(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
    openApi.components.schemas.values.forEach { schema ->
      val props = schema.properties ?: return@forEach
      props.forEach { (name, property) ->
        if (property.type == "string" && property.format == "date-time") {
          props[name] = Schema<String>().apply {
            example = "${LocalDate.now()}T${DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now())}"
            type = property.type
            format = property.format
            nullable = property.nullable
            deprecated = property.deprecated
            readOnly = property.readOnly
            writeOnly = property.writeOnly
            extensions = property.extensions
            title = property.title
          }
        }
      }
    }
  }

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://court-appearance-scheduler-api-dev.hmpps.service.justice.gov.uk").description("Development"),
        Server().url("https://court-appearance-scheduler-api-preprod.hmpps.service.justice.gov.uk").description("Pre-Production"),
        Server().url("https://court-appearance-scheduler-api.hmpps.service.justice.gov.uk").description("Production"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .tags(
      listOf(
        Tag().name(INTEGRATIONS).description("DPS integration endpoints"),
        Tag().name(UI).description("UI endpoints - not to be use by any other client"),
        Tag().name(SYNC).description("Legacy sync endpoints - not to be use by any other client"),
      ),
    )
    .info(
      Info().title("HMPPS Court Appearance Scheduler Api").version(version)
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")),
    ).components(
      Components().addSecuritySchemes(
        "bearer-jwt",
        SecurityScheme()
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")
          .`in`(SecurityScheme.In.HEADER)
          .name("Authorization"),
      ),
    )
    .addSecurityItem(SecurityRequirement().addList("bearer-jwt", listOf("read", "write")))
    .also { PrimitiveType.enablePartialTime() }

  @Bean
  fun preAuthorizeCustomizer(): OperationCustomizer = OperationCustomizer { operation: Operation, handlerMethod: HandlerMethod ->
    handlerMethod.preAuthorizeForMethodOrClass()?.let {
      val preAuthExp = SpelExpressionParser().parseExpression(it)
      val evalContext = StandardEvaluationContext()
      evalContext.beanResolver = BeanFactoryResolver(context)
      evalContext.setRootObject(
        @Suppress("unused")
        object {
          fun hasRole(role: String) = listOf(role)
          fun hasAnyRole(vararg roles: String) = roles.toList()
        },
      )

      val roles = try {
        (preAuthExp.getValue(evalContext) as List<*>).filterIsInstance<String>()
      } catch (e: SpelEvaluationException) {
        emptyList()
      }
      if (roles.isNotEmpty()) {
        operation.description = "${operation.description ?: ""}\n\n" +
          "Requires one of the following roles:\n" +
          roles.joinToString(prefix = "* ", separator = "\n* ")
      }
    }

    operation
  }

  private fun HandlerMethod.preAuthorizeForMethodOrClass() = getMethodAnnotation(PreAuthorize::class.java)?.value
    ?: beanType.getAnnotation(PreAuthorize::class.java)?.value
}
