package id.co.bca.crosbatch.api;

import id.co.bca.crosbatch.config.CrosBatchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.batch.autoconfigure.BatchTaskExecutor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/** Beans that only exist in API mode (web application). */
@Configuration
@ConditionalOnWebApplication
public class ApiConfig {

    /**
     * Makes Spring Batch's JobOperator run the job on its own thread, so the HTTP call returns
     * right away. Only one run at a time is allowed by {@code CrosJobService}.
     */
    @Bean
    @BatchTaskExecutor
    public TaskExecutor batchTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("cros-batch-");
        executor.setConcurrencyLimit(1);
        return executor;
    }

    @Bean
    public FilterRegistrationBean<ApiKeyFilter> apiKeyFilter(CrosBatchProperties props) {
        FilterRegistrationBean<ApiKeyFilter> registration =
                new FilterRegistrationBean<>(new ApiKeyFilter(props.getApi()));
        registration.addUrlPatterns("/api/*");
        return registration;
    }
}
