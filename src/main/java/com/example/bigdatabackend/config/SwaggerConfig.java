package com.example.bigdatabackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger API文档配置 - 已禁用（Springfox与Spring Boot 4.x不兼容）
 * 如需API文档，请升级到SpringDoc OpenAPI
 */
// @Configuration
// @EnableSwagger2
// @Profile("dev")
public class SwaggerConfig {

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.example.bigdatabackend.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("大数据商城后端系统 API 文档")
                .description("基于HBase、Redis、HDFS的大数据商城后端API")
                .version("1.0")
                .contact(new Contact("开发团队", "", "dev@bigdata-mall.com"))
                .build();
    }
}
