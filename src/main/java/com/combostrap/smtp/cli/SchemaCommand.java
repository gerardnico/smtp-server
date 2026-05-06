package com.combostrap.smtp.cli;

import com.combostrap.smtp.SmtpConfigBean;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.*;

public class SchemaCommand {

    public static void main(String[] args) {

        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON);
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(SmtpConfigBean.class);

        System.out.println(jsonSchema.toString());
    }
}
