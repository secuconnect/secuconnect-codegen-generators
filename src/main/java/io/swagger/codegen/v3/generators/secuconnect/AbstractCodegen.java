package io.swagger.codegen.v3.generators.secuconnect;

import io.swagger.codegen.v3.generators.DefaultCodegenConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCodegen extends DefaultCodegenConfig {
    private static Logger LOGGER = LoggerFactory.getLogger(AbstractCodegen.class);

    @Override
    final public String toModelName(final String name) {
        // We need to check if import-mapping has a different model for this class, so we use it
        // instead of the auto-generated one.

        if (!getIgnoreImportMapping() && importMapping.containsKey(name)) {
            return importMapping.get(name);
        }

        String nameWithPrefixSuffix = sanitizeName(name);
        if (!StringUtils.isEmpty(modelNamePrefix)) {
            // add '_' so that model name can be camelized correctly
            nameWithPrefixSuffix = modelNamePrefix + "_" + nameWithPrefixSuffix;
        }

        if (!StringUtils.isEmpty(modelNameSuffix)) {
            // add '_' so that model name can be camelized correctly
            nameWithPrefixSuffix = nameWithPrefixSuffix + "_" + modelNameSuffix;
        }

        // camelize the model name
        // phone_number => PhoneNumber
        // if it's all upper case, do nothing
        String camelizedName = nameWithPrefixSuffix;
        if (!camelizedName.matches("^[A-Z]{3}_[A-Z].*$")) {
            camelizedName = camelize(nameWithPrefixSuffix);
        }

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(camelizedName)) {
            final String modelName = "Model" + camelizedName;
            LOGGER.warn(camelizedName + " (reserved word) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }

        // model name starts with number
        if (camelizedName.matches("^\\d.*")) {
            final String modelName = "Model" + camelizedName; // e.g. 200Response => Model200Response (after camelize)
            LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }

        return camelizedName;
    }

    @Override
    final public String toModelFilename(String name) {
        // should be the same as the model name
        return toModelName(name);
    }
}
