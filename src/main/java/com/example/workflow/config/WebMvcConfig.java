package com.example.workflow.config;

import com.example.workflow.service.PermService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final PermService permService;

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jackson) {
                ObjectMapper mapper = jackson.getObjectMapper();
                mapper.findAndRegisterModules();
                mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                jackson.setObjectMapper(mapper);
                jackson.setDefaultCharset(StandardCharsets.UTF_8);
                return;
            }
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                      Object handler) throws Exception {
                try {
                    permService.initPermissionContext();
                    log.debug("权限上下文初始化完成: userId={}, orgId={}",
                            request.getHeader("X-User-Id"),
                            request.getHeader("X-Org-Id"));
                } catch (Exception e) {
                    log.error("权限初始化失败", e);
                    response.setStatus(401);
                    return false;
                }
                return true;
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                         Object handler, Exception ex) throws Exception {
                com.example.workflow.security.PermissionContext.clear();
            }
        }).addPathPatterns("/api/**");
    }
}
