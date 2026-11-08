package id.co.bca.crosbatch.api;

import id.co.bca.crosbatch.config.CrosBatchProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI document (/v3/api-docs) and Swagger UI (/swagger-ui.html). Declares the API key
 * header so "Authorize" in Swagger UI sends it with every call.
 */
@Configuration
@ConditionalOnWebApplication
public class OpenApiConfig {

    static final String API_KEY_SCHEME = "apiKey";

    @Bean
    public OpenAPI crosBatchOpenApi(CrosBatchProperties props) {
        return new OpenAPI()
                .info(new Info()
                        .title("CROS Batch API")
                        .version("v1")
                        .description("Start and monitor the monthly CROS load (Spring Batch job crosMonthlyJob)."))
                .components(new Components().addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(props.getApi().getKeyHeader())))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME));
    }
}
