package iti.jets.java.homenursing.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    private static final List<Locale> SUPPORTED = List.of(Locale.ENGLISH, Locale.forLanguageTag("ar"));

    private final AdminApiKeyInterceptor adminApiKeyInterceptor;

    @Autowired
    public LocaleConfig(AdminApiKeyInterceptor adminApiKeyInterceptor) {
        this.adminApiKeyInterceptor = adminApiKeyInterceptor;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(SUPPORTED);
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminApiKeyInterceptor)
                .addPathPatterns("/api/v1/admin/**");
    }
}
