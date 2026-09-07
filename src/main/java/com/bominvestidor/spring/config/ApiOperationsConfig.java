package com.bominvestidor.spring.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class ApiOperationsConfig {
	@Bean
	OpenAPI applicationOpenApi() {
		String scheme = "bearerAuth";
		return new OpenAPI()
				.info(new Info().title("Bom Investidor API").version("v1")
						.description("API REST para gestão privada de carteiras de investimentos."))
				.components(new Components().addSecuritySchemes(scheme, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(scheme));
	}

	@Bean
	OpenApiCustomizer publicContractCustomizer() {
		return openApi -> {
			Schema<?> error = new Schema<>().name("ApiError").type("object")
					.addProperty("timestamp", new Schema<>().type("string").format("date-time"))
					.addProperty("status", new Schema<>().type("integer"))
					.addProperty("code", new Schema<>().type("string"))
					.addProperty("message", new Schema<>().type("string"))
					.addProperty("path", new Schema<>().type("string"))
					.addProperty("fieldErrors", new Schema<>().type("array"));
			openApi.getComponents().addSchemas("ApiError", error);
			openApi.getPaths().forEach((path, item) -> item.readOperations().forEach(operation -> {
				if (path.equals("/api/auth/register") || path.equals("/api/auth/login")) {
					operation.setSecurity(List.of());
				} else if (path.startsWith("/api/")) {
					addError(operation, "400", "Request body, parameters or values are invalid");
					addError(operation, "401", "Authentication is required or invalid");
					addError(operation, "403", "Authenticated account is not authorized");
					addError(operation, "404", "Requested private resource was not found");
					addError(operation, "409", "Request conflicts with the current resource state");
					addError(operation, "422", "A business validation rejected the request");
					addError(operation, "503", "A required external provider is unavailable");
				}
			}));
		};
	}

	private static void addError(Operation operation, String status, String description) {
		operation.getResponses().addApiResponse(status, new ApiResponse().description(description)
				.content(new io.swagger.v3.oas.models.media.Content().addMediaType("application/json",
						new io.swagger.v3.oas.models.media.MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiError")))));
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		if (!properties.allowedOrigins().isEmpty()) {
			CorsConfiguration configuration = new CorsConfiguration();
			configuration.setAllowedOrigins(properties.allowedOrigins());
			configuration.setAllowedMethods(List.of(HttpMethod.GET.name(), HttpMethod.POST.name(),
					HttpMethod.PUT.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));
			configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
			configuration.setAllowCredentials(false);
			source.registerCorsConfiguration("/api/**", configuration);
		}
		return source;
	}
}
