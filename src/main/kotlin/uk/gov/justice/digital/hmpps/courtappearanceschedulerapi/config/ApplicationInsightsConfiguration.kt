package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.opentelemetry.api.trace.Span
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.text.ParseException

@Configuration
@ConditionalOnExpression($$"T(org.springframework.util.StringUtils).hasText('${applicationinsights.connection.string:}')")
class ClientTrackingConfiguration(private val clientTrackingInterceptor: ClientTrackingInterceptor) : WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(clientTrackingInterceptor).addPathPatterns("/**").order(Ordered.HIGHEST_PRECEDENCE)
  }
}

@Configuration
class ClientTrackingInterceptor : HandlerInterceptor {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    val token = request.getHeader(HttpHeaders.AUTHORIZATION)
    if (token?.startsWith("Bearer ") == true) {
      try {
        val jwtBody = getClaimsFromJwt(token)
        val user = jwtBody.getClaim("user_name")?.toString()
        val client = jwtBody.getClaim("client_id")?.toString()
        val caseloadId = request.getHeader(CaseloadIdHeader.NAME)

        Span.current().also { span ->
          user?.also { span.setAttribute("username", it) }
          client?.also { span.setAttribute("clientId", it) }
          caseloadId?.also { span.setAttribute("caseloadId", it) }
        }
      } catch (_: ParseException) {
        // no-op - could not parse token
      }
    }
    return true
  }

  @Throws(ParseException::class)
  private fun getClaimsFromJwt(token: String): JWTClaimsSet = SignedJWT.parse(token.replace("Bearer ", "")).jwtClaimsSet
}
