package com.combostrap.smtp.cli;

import com.combostrap.common.Fs;
import com.combostrap.smtp.SmtpConfigBean;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.*;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SchemaCommand {

    public static void main(String[] args) {

        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON);
        configBuilder
                .forTypesInGeneral()
                .withPatternPropertiesResolver(scope -> {
                    // Add integer pattern
                    if (scope.getType().isInstanceOf(Map.class)) {
                        ResolvedType keyType = scope.getTypeParameterFor(Map.class, 0);
                        if (keyType != null && (keyType.getErasedType() == Integer.class
                                || keyType.getErasedType() == int.class)) {
                            Map<String, Type> result = new HashMap<>();
                            result.put("^[0-9]+$", scope.getTypeParameterFor(Map.class, 1));
                            return result;
                        }
                    }
                    return null;
                });
        SchemaGeneratorConfig config = configBuilder
                .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES)
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(SmtpConfigBean.class);

        Path dir = Paths.get(".", ".smtp-server");
        Fs.createDirectoryIfNotExists(dir);

        Path resolve = dir.resolve("smtp-server-schema.json");
        Fs.write(resolve, jsonSchema.toString());
        System.err.println("File saved to " + resolve);

    }
}
