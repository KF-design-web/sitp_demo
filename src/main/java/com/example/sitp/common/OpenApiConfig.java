package com.example.sitp.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Iterator;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String FORM_MEDIA_TYPE = org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

    @Bean
    public OpenApiCustomizer formDoorsFormBodyCustomizer() {
        return this::convertObjectQueryParamsToFormBodies;
    }

    private void convertObjectQueryParamsToFormBodies(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().forEach((path, pathItem) -> {
            convert(pathItem.getPost());
            convert(pathItem.getPut());
            convert(pathItem.getPatch());
        });
    }

    private void convert(Operation operation) {
        if (operation == null || operation.getRequestBody() != null || operation.getParameters() == null) {
            return;
        }
        List<Parameter> parameters = operation.getParameters();
        Iterator<Parameter> iterator = parameters.iterator();
        while (iterator.hasNext()) {
            Parameter parameter = iterator.next();
            Schema<?> schema = parameter.getSchema();
            if (schema == null || schema.get$ref() == null) {
                continue;
            }

            RequestBody formBody = new RequestBody()
                    .required(Boolean.TRUE.equals(parameter.getRequired()))
                    .content(new Content().addMediaType(FORM_MEDIA_TYPE,
                            new MediaType().schema(new Schema<>().$ref(schema.get$ref()))));

            operation.setRequestBody(formBody);
            iterator.remove();
            if (parameters.isEmpty()) {
                operation.setParameters(null);
            }
            return;
        }
    }
}
