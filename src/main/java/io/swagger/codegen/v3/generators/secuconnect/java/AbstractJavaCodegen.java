package io.swagger.codegen.v3.generators.secuconnect.java;

import com.github.jknack.handlebars.Handlebars;
import io.swagger.codegen.v3.*;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;
import io.swagger.codegen.v3.generators.handlebars.HandlebarsHelpers;
import io.swagger.codegen.v3.generators.handlebars.java.JavaHelper;
import io.swagger.codegen.v3.generators.secuconnect.AbstractCodegen;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.util.SchemaTypeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import static io.swagger.codegen.v3.CodegenConstants.HAS_ENUMS_EXT_NAME;
import static io.swagger.codegen.v3.CodegenConstants.IS_ENUM_EXT_NAME;
import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;

public abstract class AbstractJavaCodegen extends AbstractCodegen {
    private static Logger LOGGER = LoggerFactory.getLogger(AbstractJavaCodegen.class);
    public static final String JAVA8_MODE = "java8";
    public static final String JAVA11_MODE = "java11";

    protected String dateLibrary = "java11";
    protected boolean java8Mode = true;
    protected boolean java11Mode = true;
    protected String invokerPackage = "io.swagger";
    protected String artifactId = "swagger-java";
    protected String artifactVersion = "1.0.0";
    protected String artifactUrl = "https://github.com/swagger-api/swagger-codegen";
    protected String artifactDescription = "Swagger Java";
    protected String developerName = "Swagger";
    protected String developerEmail = "apiteam@swagger.io";
    protected String developerOrganization = "Swagger";
    protected String developerOrganizationUrl = "http://swagger.io";
    protected String scmConnection = "scm:git:git@github.com:swagger-api/swagger-codegen.git";
    protected String scmDeveloperConnection = "scm:git:git@github.com:swagger-api/swagger-codegen.git";
    protected String scmUrl = "https://github.com/swagger-api/swagger-codegen";
    protected String licenseName = "Unlicense";
    protected String licenseUrl = "http://unlicense.org";
    protected String projectFolder = "src" + File.separator + "main";
    protected String projectTestFolder = "src" + File.separator + "test";
    protected String sourceFolder = projectFolder + File.separator + "java";
    protected String testFolder = projectTestFolder + File.separator + "java";
    protected boolean serializeBigDecimalAsString = false;
    protected String apiDocPath = "docs/";
    protected String modelDocPath = "docs/";

    public AbstractJavaCodegen() {
        super();
        hideGenerationTimestamp = true;
        supportsInheritance = true;

        setReservedWordsLowerCase(
                Arrays.asList(
                        // used as internal variables, can collide with parameter names
                        "localVarPath", "localVarQueryParams", "localVarCollectionQueryParams",
                        "localVarHeaderParams", "localVarFormParams", "localVarPostBody",
                        "localVarAccepts", "localVarAccept", "localVarContentTypes",
                        "localVarContentType", "localVarAuthNames", "localReturnType",
                        "ApiClient", "ApiException", "ApiResponse", "Configuration", "StringUtil",

                        // language reserved words
                        "abstract", "continue", "for", "new", "switch", "assert",
                        "default", "if", "package", "synchronized", "boolean", "do", "goto", "private",
                        "this", "break", "double", "implements", "protected", "throw", "byte", "else",
                        "import", "public", "throws", "case", "enum", "instanceof", "return", "transient",
                        "catch", "extends", "int", "short", "try", "char", "final", "interface", "static",
                        "void", "class", "finally", "long", "strictfp", "volatile", "const", "float",
                        "native", "super", "while", "null")
        );

        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList(
                        "String",
                        "boolean",
                        "Boolean",
                        "Double",
                        "Integer",
                        "Long",
                        "Float",
                        "Object",
                        "byte[]")
        );
        instantiationTypes.put("array", "ArrayList");
        instantiationTypes.put("map", "HashMap");
        typeMapping.put("date", "Date");
        typeMapping.put("file", "File");
        typeMapping.put("binary", "File");

        cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_ID, CodegenConstants.ARTIFACT_ID_DESC));
        cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_VERSION, CodegenConstants.ARTIFACT_VERSION_DESC));
    }

    @Override
    public void processOpts() {
        if (additionalProperties.containsKey(CodegenConstants.INVOKER_PACKAGE)) {
            this.setInvokerPackage((String) additionalProperties.get(CodegenConstants.INVOKER_PACKAGE));
        } else if (additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
            // guess from api package
            String derivedInvokerPackage = deriveInvokerPackageName((String)additionalProperties.get(CodegenConstants.API_PACKAGE));
            this.additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, derivedInvokerPackage);
            this.setInvokerPackage((String) additionalProperties.get(CodegenConstants.INVOKER_PACKAGE));
            LOGGER.info("Invoker Package Name, originally not set, is now derived from api package name: " + derivedInvokerPackage);
        } else if (additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
            // guess from model package
            String derivedInvokerPackage = deriveInvokerPackageName((String)additionalProperties.get(CodegenConstants.MODEL_PACKAGE));
            this.additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, derivedInvokerPackage);
            this.setInvokerPackage((String) additionalProperties.get(CodegenConstants.INVOKER_PACKAGE));
            LOGGER.info("Invoker Package Name, originally not set, is now derived from model package name: " + derivedInvokerPackage);
        } else if (StringUtils.isNotEmpty(invokerPackage)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage);
        }

        super.processOpts();

        modelTemplateFiles.put("model.mustache", ".java");
        apiTemplateFiles.put("api.mustache", ".java");
        //apiTestTemplateFiles.put("api_test.mustache", ".java");

        if (additionalProperties.containsKey(CodegenConstants.ARTIFACT_ID)) {
            this.setArtifactId((String) additionalProperties.get(CodegenConstants.ARTIFACT_ID));
        } else if(StringUtils.isNotEmpty(artifactId)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            additionalProperties.put(CodegenConstants.ARTIFACT_ID, artifactId);
        }

        if (additionalProperties.containsKey(CodegenConstants.ARTIFACT_VERSION)) {
            this.setArtifactVersion((String) additionalProperties.get(CodegenConstants.ARTIFACT_VERSION));
        } else if(StringUtils.isNotEmpty(artifactVersion)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            additionalProperties.put(CodegenConstants.ARTIFACT_VERSION, artifactVersion);
        }

        importMapping.put("List", "java.util.List");

        this.sanitizeConfig();

        // optional jackson mappings for BigDecimal support
        importMapping.put("ToStringSerializer", "com.fasterxml.jackson.databind.ser.std.ToStringSerializer");
        importMapping.put("JsonSerialize", "com.fasterxml.jackson.databind.annotation.JsonSerialize");
        importMapping.put("Schema", "io.swagger.v3.oas.annotations.media.Schema");
        importMapping.put("JsonProperty", "com.fasterxml.jackson.annotation.JsonProperty");
        importMapping.put("JsonSubTypes", "com.fasterxml.jackson.annotation.JsonSubTypes");
        importMapping.put("JsonTypeInfo", "com.fasterxml.jackson.annotation.JsonTypeInfo");
        importMapping.put("JsonCreator", "com.fasterxml.jackson.annotation.JsonCreator");
        importMapping.put("JsonValue", "com.fasterxml.jackson.annotation.JsonValue");
        importMapping.put("JsonTypeId", "com.fasterxml.jackson.annotation.JsonTypeId");
        importMapping.put("SerializedName", "com.google.gson.annotations.SerializedName");
        importMapping.put("TypeAdapter", "com.google.gson.TypeAdapter");
        importMapping.put("JsonAdapter", "com.google.gson.annotations.JsonAdapter");
        importMapping.put("JsonReader", "com.google.gson.stream.JsonReader");
        importMapping.put("JsonWriter", "com.google.gson.stream.JsonWriter");
        importMapping.put("IOException", "java.io.IOException");
        importMapping.put("Object", "java.lang.Object"); // TODO check if needed
        importMapping.put("Objects", "java.util.Objects");
        importMapping.put("StringUtil", invokerPackage + ".StringUtil");
        // import JsonCreator if JsonProperty is imported
        // used later in recursive import in postProcessingModels
        importMapping.put("com.fasterxml.jackson.annotation.JsonProperty", "com.fasterxml.jackson.annotation.JsonCreator");
    }

    private void sanitizeConfig() {
        // Sanitize any config options here. We also have to update the additionalProperties because
        // the whole additionalProperties object is injected into the main object passed to the mustache layer

        this.setApiPackage(sanitizePackageName(apiPackage));
        if (additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
            this.additionalProperties.put(CodegenConstants.API_PACKAGE, apiPackage);
        }

        this.setModelPackage(sanitizePackageName(modelPackage));
        if (additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
            this.additionalProperties.put(CodegenConstants.MODEL_PACKAGE, modelPackage);
        }

        this.setInvokerPackage(sanitizePackageName(invokerPackage));
        if (additionalProperties.containsKey(CodegenConstants.INVOKER_PACKAGE)) {
            this.additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage);
        }
    }

    protected String escapeUnderscore(String name) {
        // Java 8 discourages naming things _, but Java 9 does not allow it.
        if("_".equals(name)) {
            return "_u";
        } else {
            return name;
        }
    }

    @Override
    public String escapeReservedWord(String name) {
        if(this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return "_" + name;
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', '/');
    }

    @Override
    public String apiTestFileFolder() {
        return outputFolder + "/" + testFolder + "/" + apiPackage().replace('.', '/');
    }

    @Override
    public String modelFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', '/');
    }

    @Override
    public String apiDocFileFolder() {
        return (outputFolder + "/" + apiDocPath).replace('/', File.separatorChar);
    }

    @Override
    public String modelDocFileFolder() {
        return (outputFolder + "/" + modelDocPath).replace('/', File.separatorChar);
    }

    @Override
    public String toApiDocFilename(String name) {
        return toApiName(name);
    }

    @Override
    public String toModelDocFilename(String name) {
        return toModelName(name);
    }

    @Override
    public String toApiTestFilename(String name) {
        return toApiName(name) + "Test";
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultApi";
        }
        return camelize(name) + "Api";
    }

    @Override
    public String toApiFilename(String name) {
        return toApiName(name);
    }

    @Override
    public String toVarName(String name) {
        // sanitize name
        name = sanitizeVarName(name); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        if (name.toLowerCase().matches("^_*class$")) {
            return "propertyClass";
        }

        name = escapeUnderscore(name);

        // if it's all upper case, do nothing
        if (name.matches("^[A-Z_]*$")) {
            return name;
        }

        if(startsWithTwoUppercaseLetters(name)){
            name = name.substring(0, 2).toLowerCase() + name.substring(2);
        }

        // camelize (lower first character) the variable name
        // pet_id => petId
        name = camelizeVarName(name, true);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    public String camelizeVarName(String word, boolean lowercaseFirstLetter) {
        if (word.startsWith("_") && word.length() > 1 && !word.equals("_u") ) {
            word = "_" + DefaultCodegenConfig.camelize(word, lowercaseFirstLetter);
        } else {
            word  = DefaultCodegenConfig.camelize(word, lowercaseFirstLetter);
        }

        if (!word.startsWith("$") || word.length() <= 1) {
            return word;
        }
        final String letter = String.valueOf(word.charAt(1));
        if (!StringUtils.isAllUpperCase(letter)) {
            return word;
        }
        word = word.replaceFirst(letter, letter.toLowerCase());
        return word;
    }

    private boolean startsWithTwoUppercaseLetters(String name) {
        boolean startsWithTwoUppercaseLetters = false;
        if(name.length() > 1) {
            startsWithTwoUppercaseLetters = name.substring(0, 2).equals(name.substring(0, 2).toUpperCase());
        }
        return startsWithTwoUppercaseLetters;
    }

    @Override
    public String toParamName(String name) {
        // to avoid conflicts with 'callback' parameter for async call
        if ("callback".equals(name)) {
            return "paramCallback";
        }

        // should be the same as variable name
        return toVarName(name);
    }

    @Override
    public String getTypeDeclaration(Schema propertySchema) {
        if (propertySchema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) propertySchema;
            Schema inner = arraySchema.getItems();
            if (inner == null) {
                LOGGER.warn(arraySchema.getName() + "(array property) does not have a proper inner type defined");
                // TODO maybe better defaulting to StringProperty than returning null
                return null;
            }
            return String.format("%s<%s>", getSchemaType(propertySchema), getTypeDeclaration(inner));
            // return getSwaggerType(propertySchema) + "<" + getTypeDeclaration(inner) + ">";
        } else if (propertySchema instanceof MapSchema && hasSchemaProperties(propertySchema)) {
            Schema inner = (Schema) propertySchema.getAdditionalProperties();
            if (inner == null) {
                LOGGER.warn(propertySchema.getName() + "(map property) does not have a proper inner type defined");
                // TODO maybe better defaulting to StringProperty than returning null
                return null;
            }
            return getSchemaType(propertySchema) + "<String, " + getTypeDeclaration(inner) + ">";
        } else if (propertySchema instanceof MapSchema && hasTrueAdditionalProperties(propertySchema)) {
            Schema inner = new ObjectSchema();
            return getSchemaType(propertySchema) + "<String, " + getTypeDeclaration(inner) + ">";
        }
        return super.getTypeDeclaration(propertySchema);
    }

    @Override
    public String getAlias(String name) {
        if (typeAliases != null && typeAliases.containsKey(name)) {
            return typeAliases.get(name);
        }
        return name;
    }

    @Override
    public String toDefaultValue(Schema schema) {
        if (schema instanceof ArraySchema) {
            final ArraySchema arraySchema = (ArraySchema) schema;
            final String pattern;
            pattern = "new ArrayList<%s>()";
            if (arraySchema.getItems() == null) {
                return null;
            }

            String typeDeclaration = getTypeDeclaration(arraySchema.getItems());
            Object java8obj = additionalProperties.get("java8");
            if (java8obj != null) {
                Boolean java8 = Boolean.valueOf(java8obj.toString());
                if (java8 != null && java8) {
                    typeDeclaration = "";
                }
            }

            return String.format(pattern, typeDeclaration);
        } else if (schema instanceof MapSchema && hasSchemaProperties(schema)) {
            final String pattern;
            pattern = "new HashMap<%s>()";
            if (schema.getAdditionalProperties() == null) {
                return null;
            }

            String typeDeclaration = String.format("String, %s", getTypeDeclaration((Schema) schema.getAdditionalProperties()));
            Object java8obj = additionalProperties.get("java8");
            if (java8obj != null) {
                Boolean java8 = Boolean.valueOf(java8obj.toString());
                if (java8 != null && java8) {
                    typeDeclaration = "";
                }
            }

            return String.format(pattern, typeDeclaration);
        } else if (schema instanceof MapSchema && hasTrueAdditionalProperties(schema)) {
            final String pattern;
            pattern = "new HashMap<%s>()";
            if (schema.getAdditionalProperties() == null) {
                return null;
            }
            Schema inner = new ObjectSchema();
            String typeDeclaration = String.format("String, %s", getTypeDeclaration(inner));
            Object java8obj = additionalProperties.get("java8");
            if (java8obj != null) {
                Boolean java8 = Boolean.valueOf(java8obj.toString());
                if (java8 != null && java8) {
                    typeDeclaration = "";
                }
            }

            return String.format(pattern, typeDeclaration);
        } else if (schema instanceof IntegerSchema) {
            if (schema.getDefault() != null && SchemaTypeUtil.INTEGER64_FORMAT.equals(schema.getFormat())) {
                return String.format("%sl", schema.getDefault().toString());
            }
        } else if (schema instanceof NumberSchema) {
            if (schema.getDefault() != null) {
                if (schema.getDefault() != null && SchemaTypeUtil.FLOAT_FORMAT.equals(schema.getFormat())) {
                    return String.format("%sf", schema.getDefault().toString());
                } else if (schema.getDefault() != null && SchemaTypeUtil.DOUBLE_FORMAT.equals(schema.getFormat())) {
                    return String.format("%sd", schema.getDefault().toString());
                } else {
                    return String.format("new BigDecimal(%s)", schema.getDefault().toString());
                }
            }
        } else if (schema instanceof StringSchema) {
            if (schema.getDefault() != null) {
                String _default = schema.getDefault().toString();
                if (schema.getEnum() == null) {
                    return String.format("\"%s\"", escapeText(_default));
                } else {
                    // convert to enum var name later in postProcessModels
                    return _default;
                }
            }
        }
        return super.toDefaultValue(schema);
    }

    @Override
    public void setParameterExampleValue(CodegenParameter p) {
        String example;

        if (p.defaultValue == null) {
            example = p.example;
        } else {
            example = p.defaultValue;
        }

        String type = p.baseType;
        if (type == null) {
            type = p.dataType;
        }

        if ("String".equals(type)) {
            if (example == null) {
                example = p.paramName + "_example";
            }
            p.testExample = example;
            example = "\"" + escapeText(example) + "\"";
        } else if ("Integer".equals(type) || "Short".equals(type)) {
            if (example == null) {
                example = "56";
            }
        } else if ("Long".equals(type)) {
            if (example == null) {
                example = "56";
            }
            p.testExample = example;
            example = example + "L";
        } else if ("Float".equals(type)) {
            if (example == null) {
                example = "3.4";
            }
            p.testExample = example;
            example = example + "F";
        } else if ("Double".equals(type)) {
            example = "3.4";
            p.testExample = example;
            example = example + "D";
        } else if ("Boolean".equals(type)) {
            if (example == null) {
                example = "true";
            }
        } else if ("File".equals(type)) {
            if (example == null) {
                example = "/path/to/file";
            }
            example = "new File(\"" + escapeText(example) + "\")";
        } else if ("Date".equals(type)) {
            example = "new Date()";
        } else if (!languageSpecificPrimitives.contains(type)) {
            // type is a model class, e.g. User
            example = "new " + type + "()";
        }

        if (p.testExample == null) {
            p.testExample = example;
        }

        if (example == null) {
            example = "null";
        } else if (getBooleanValue(p, CodegenConstants.IS_LIST_CONTAINER_EXT_NAME)) {
            example = "Arrays.asList(" + example + ")";
        } else if (getBooleanValue(p, CodegenConstants.IS_MAP_CONTAINER_EXT_NAME)) {
            example = "new HashMap()";
        }

        p.example = example;
    }

    @Override
    public String toExampleValue(Schema schemaProperty) {
        if(schemaProperty.getExample() != null) {
            return escapeText(schemaProperty.getExample().toString());
        } else {
            return super.toExampleValue(schemaProperty);
        }
    }

    @Override
    public String getSchemaType(Schema schema) {
        String schemaType = super.getSchemaType(schema);

        schemaType = getAlias(schemaType);

        // don't apply renaming on types from the typeMapping
        if (typeMapping.containsKey(schemaType)) {
            return typeMapping.get(schemaType);
        }

        if (null == schemaType) {
            if (schema.getName() != null) {
                LOGGER.warn("No Type defined for Property " + schema.getName());
            } else {
                // LOGGER.error("No Type defined.", new Exception());
            }
        }
        return toModelName(schemaType);
    }

    @Override
    public String toOperationId(String operationId) {
        // throw exception if method name is empty
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method/operation name (operationId) not allowed");
        }

        operationId = camelize(sanitizeName(operationId), true);

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            String newOperationId = camelize("call_" + operationId, true);
            LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to " + newOperationId);
            return newOperationId;
        }

        return operationId;
    }

    @Override
    public CodegenModel fromModel(String name, Schema schema, Map<String, Schema> allSchemas) {
        CodegenModel codegenModel = super.fromModel(name, schema, allSchemas);
//        if(codegenModel.description != null) {
//            codegenModel.imports.add("Schema");
//        }
        if (codegenModel.discriminator != null && additionalProperties.containsKey("jackson")) {
            codegenModel.imports.add("JsonSubTypes");
            codegenModel.imports.add("JsonTypeInfo");
        }
        boolean hasEnums = getBooleanValue(codegenModel, HAS_ENUMS_EXT_NAME);
        if (allSchemas != null && codegenModel.parentSchema != null && hasEnums) {
            final Schema parentModel = allSchemas.get(codegenModel.parentSchema);
            final CodegenModel parentCodegenModel = super.fromModel(codegenModel.parent, parentModel, allSchemas);
            codegenModel = AbstractJavaCodegen.reconcileInlineEnums(codegenModel, parentCodegenModel);
        }
        return codegenModel;
    }

    @Override
    protected void addAdditionPropertiesToCodeGenModel(CodegenModel codegenModel, Schema schema) {
        super.addAdditionPropertiesToCodeGenModel(codegenModel, schema);
        addVars(codegenModel, schema.getProperties(), schema.getRequired());
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        if(serializeBigDecimalAsString) {
            if (property.baseType.equals("BigDecimal")) {
                // we serialize BigDecimal as `string` to avoid precision loss
                property.vendorExtensions.put("extraAnnotation", "@JsonSerialize(using = ToStringSerializer.class)");

                // this requires some more imports to be added for this model...
                model.imports.add("ToStringSerializer");
                model.imports.add("JsonSerialize");
            }
        }

        if ("array".equals(property.containerType)) {
            model.imports.add("ArrayList");
        } else if ("map".equals(property.containerType)) {
            model.imports.add("HashMap");
        }

//        boolean isEnum = getBooleanValue(model, IS_ENUM_EXT_NAME);
//        if(!BooleanUtils.toBoolean(isEnum)) {
            // needed by all pojos, but not enums
//            model.imports.add("Schema");
//        }
        if (model.discriminator != null && model.discriminator.getPropertyName().equals(property.baseName)) {
            property.vendorExtensions.put("x-is-discriminator-property", true);
            if (additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonTypeId");
            }
        }
    }

    @Override
    protected void fixUpParentAndInterfaces(CodegenModel codegenModel, Map<String, CodegenModel> allModels) {
        super.fixUpParentAndInterfaces(codegenModel, allModels);
        if (codegenModel.vars == null || codegenModel.vars.isEmpty() || codegenModel.parentModel == null) {
            return;
        }

        for (CodegenProperty codegenProperty : codegenModel.vars) {
            CodegenModel parentModel = codegenModel.parentModel;

            while (parentModel != null) {
                if (parentModel.vars == null || parentModel.vars.isEmpty()) {
                    parentModel = parentModel.parentModel;
                    continue;
                }
                boolean hasConflict = parentModel.vars.stream()
                    .anyMatch(parentProperty -> parentProperty.name.equals(codegenProperty.name) && !parentProperty.datatype.equals(codegenProperty.datatype));
//
//                    .anyMatch(parentProperty ->
//                        (parentProperty.name.equals(codegenProperty.name) ||
//                            parentProperty.getGetter().equals(codegenProperty.getGetter()) ||
//                            parentProperty.getSetter().equals(codegenProperty.getSetter()) &&
//                        !parentProperty.datatype.equals(codegenProperty.datatype)));
                if (hasConflict) {
                    codegenProperty.name = toVarName(codegenModel.name + "_" + codegenProperty.name);
                    codegenProperty.nameInCamelCase = camelize(codegenProperty.name, false);
                    codegenProperty.getter = toGetter(codegenProperty.name);
                    codegenProperty.setter = toSetter(codegenProperty.name);
                    break;
                }
                parentModel = parentModel.parentModel;
            }
        }
    }

    @Override
    public void postProcessParameter(CodegenParameter parameter) { }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        // recursively add import for mapping one type to multiple imports
        List<Map<String, String>> recursiveImports = (List<Map<String, String>>) objs.get("imports");
        if (recursiveImports == null)
            return objs;

        ListIterator<Map<String, String>> listIterator = recursiveImports.listIterator();
        while (listIterator.hasNext()) {
            String _import = listIterator.next().get("import");
            // if the import package happens to be found in the importMapping (key)
            // add the corresponding import package to the list
            if (importMapping.containsKey(_import)) {
                Map<String, String> newImportMap= new HashMap<String, String>();
                newImportMap.put("import", importMapping.get(_import));
                listIterator.add(newImportMap);
            }
        }

        return postProcessModelsEnum(objs);
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        // Remove imports of List, ArrayList, Map and HashMap as they are
        // imported in the template already.
        List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
        Pattern pattern = Pattern.compile("java\\.util\\.(List|ArrayList|Map|HashMap)");
        for (Iterator<Map<String, String>> itr = imports.iterator(); itr.hasNext();) {
            String _import = itr.next().get("import");
            if (pattern.matcher(_import).matches()) {
                itr.remove();
            }
        }
        return objs;
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        super.preprocessOpenAPI(openAPI);
        if (openAPI == null || openAPI.getPaths() == null){
            return;
        }
        this.checkDuplicatedModelNameIgnoringCase(openAPI);

        for (String pathname : openAPI.getPaths().keySet()) {
            PathItem pathItem = openAPI.getPaths().get(pathname);

            for (Operation operation : pathItem.readOperations()) {
                if (operation == null) {
                    continue;
                }
                //only add content-Type if its no a GET-Method
                if (!operation.equals(pathItem.getGet())) {
                    String contentType = getContentType(operation.getRequestBody());
                    if (StringUtils.isBlank(contentType)) {
                        contentType = DEFAULT_CONTENT_TYPE;
                    }
                    operation.addExtension("x-contentType", contentType);
                }
                String accepts = getAccept(operation);
                operation.addExtension("x-accepts", accepts);
            }
        }
    }

    private static String getAccept(Operation operation) {
        String accepts = null;
        if (operation != null && operation.getResponses() != null && !operation.getResponses().isEmpty()) {
            StringBuilder mediaTypeBuilder = new StringBuilder();

            responseLoop:
            for (ApiResponse response : operation.getResponses().values()) {
                if(response.getContent() == null || response.getContent().isEmpty()) {
                    continue;
                }

                mediaTypeLoop:
                for (String mediaTypeKey : response.getContent().keySet()) {
                    if (DEFAULT_CONTENT_TYPE.equalsIgnoreCase(mediaTypeKey)) {
                        accepts = DEFAULT_CONTENT_TYPE;
                        break responseLoop;
                    } else {
                        if (mediaTypeBuilder.length() > 0) {
                            mediaTypeBuilder.append(",");
                        }
                        mediaTypeBuilder.append(mediaTypeKey);
                    }
                }
            }
            if (accepts == null) {
                accepts = mediaTypeBuilder.toString();
            }
        } else {
            accepts = DEFAULT_CONTENT_TYPE;
        }
        return accepts;
    }

    @Override
    protected boolean needToImport(String type) {
        return super.needToImport(type) && type.indexOf(".") < 0;
    }

    protected void checkDuplicatedModelNameIgnoringCase(OpenAPI openAPI) {
        final Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        final Map<String, Map<String, Schema>> schemasRepeated = new HashMap<>();

        for (String schemaKey : schemas.keySet()) {
            final Schema schema = schemas.get(schemaKey);
            final String lowerKeyDefinition = schemaKey.toLowerCase();

            if (schemasRepeated.containsKey(lowerKeyDefinition)) {
                Map<String, Schema> modelMap = schemasRepeated.get(lowerKeyDefinition);
                if (modelMap == null) {
                    modelMap = new HashMap<>();
                    schemasRepeated.put(lowerKeyDefinition, modelMap);
                }
                modelMap.put(schemaKey, schema);
            } else {
                schemasRepeated.put(lowerKeyDefinition, null);
            }
        }
        for (String lowerKeyDefinition : schemasRepeated.keySet()) {
            final Map<String, Schema> modelMap = schemasRepeated.get(lowerKeyDefinition);
            if (modelMap == null) {
                continue;
            }
            int index = 1;
            for (String name : modelMap.keySet()) {
                final Schema schema = modelMap.get(name);
                final String newModelName = name + index;
                schemas.put(newModelName, schema);
                replaceDuplicatedInPaths(openAPI.getPaths(), name, newModelName);
                replaceDuplicatedInModelProperties(schemas, name, newModelName);
                schemas.remove(name);
                index++;
            }
        }
    }

    protected void replaceDuplicatedInPaths(Paths paths, String modelName, String newModelName) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        paths.values().stream()
            .flatMap(pathItem -> pathItem.readOperations().stream())
            .filter(operation -> {
                final RequestBody requestBody = operation.getRequestBody();
                if (requestBody == null || requestBody.getContent() == null || requestBody.getContent().isEmpty()) {
                    return false;
                }
                final Optional<MediaType> mediaTypeOptional = requestBody.getContent().values().stream().findAny();
                if (!mediaTypeOptional.isPresent()) {
                    return false;
                }
                final MediaType mediaType = mediaTypeOptional.get();
                final Schema schema = mediaType.getSchema();
                if (schema.get$ref() != null) {
                    return true;
                }
                return false;
            })
            .forEach(operation -> {
                Schema schema = this.getSchemaFromBody(operation.getRequestBody());
                schema.set$ref(schema.get$ref().replace(modelName, newModelName));
            });
        paths.values().stream()
            .flatMap(path -> path.readOperations().stream())
            .flatMap(operation -> operation.getResponses().values().stream())
            .filter(response -> {
                if (response.getContent() == null || response.getContent().isEmpty()) {
                    return false;
                }
                final Optional<MediaType> mediaTypeOptional = response.getContent().values().stream().findFirst();
                if (!mediaTypeOptional.isPresent()) {
                    return false;
                }
                final MediaType mediaType = mediaTypeOptional.get();
                final Schema schema = mediaType.getSchema();
                if (schema.get$ref() != null) {
                    return true;
                }
                return false;
            }).forEach(response -> {
                final Optional<MediaType> mediaTypeOptional = response.getContent().values().stream().findFirst();
                final Schema schema = mediaTypeOptional.get().getSchema();
                schema.set$ref(schema.get$ref().replace(modelName, newModelName));
            });
    }

    protected void replaceDuplicatedInModelProperties(Map<String, Schema> definitions, String modelName, String newModelName) {
        definitions.values().stream()
            .flatMap(model -> model.getProperties().values().stream())
            .filter(property -> ((Schema) property).get$ref() != null)
            .forEach(property -> {
                final Schema schema = (Schema) property;
                schema.set$ref(schema.get$ref().replace(modelName, newModelName));
            });
    }

    @Override
    public String toEnumName(CodegenProperty property) {
        return sanitizeName(camelize(property.name)) + "Enum";
    }

    @Override
    public String toEnumVarName(String value, String datatype) {
        if (value.length() == 0) {
            return "EMPTY";
        }

        // for symbol, e.g. $, #
        if (getSymbolName(value) != null) {
            return getSymbolName(value).toUpperCase();
        }

        // number
        if ("Integer".equals(datatype) || "Long".equals(datatype) || "Float".equals(datatype) || "Double".equals(datatype) || "BigDecimal".equals(datatype)) {
            String varName = "NUMBER_" + value;
            varName = varName.replaceAll("-", "MINUS_");
            varName = varName.replaceAll("\\+", "PLUS_");
            varName = varName.replaceAll("\\.", "_DOT_");
            return varName;
        }

        // string
        String var = value.replaceAll("\\W+", "_").toUpperCase();
        if (var.matches("\\d.*")) {
            return "_" + var;
        } else {
            return escapeUnderscore(var).toUpperCase();
        }
    }

    @Override
    public String toEnumValue(String value, String datatype) {
        if (value == null) {
            return null;
        }
        if ("Integer".equals(datatype) || "Double".equals(datatype) || "Boolean".equals(datatype)) {
            return value;
        } else if ("Long".equals(datatype)) {
            // add l to number, e.g. 2048 => 2048l
            return value + "l";
        } else if ("Float".equals(datatype)) {
            // add f to number, e.g. 3.14 => 3.14f
            return value + "f";
        } else if ("BigDecimal".equals(datatype)) {
            return "new BigDecimal(" + escapeText(value) + ")";
        } else {
            return "\"" + escapeText(value) + "\"";
        }
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, Map<String, Schema> schemas, OpenAPI openAPI) {
        CodegenOperation op = super.fromOperation(path, httpMethod, operation, schemas, openAPI);
        op.path = sanitizePath(op.path);
        return op;
    }

    public String sanitizeVarName(String name) {
        if (name == null) {
            LOGGER.warn("String to be sanitized is null. Default to " + Object.class.getSimpleName());
            return Object.class.getSimpleName();
        }
        if ("$".equals(name)) {
            return "value";
        }
        name = name.replaceAll("\\[\\]", StringUtils.EMPTY);
        name = name.replaceAll("\\[", "_")
                .replaceAll("\\]", "")
                .replaceAll("\\(", "_")
                .replaceAll("\\)", StringUtils.EMPTY)
                .replaceAll("\\.", "_")
                .replaceAll("@", "_at_")
                .replaceAll("-", "_")
                .replaceAll(" ", "_");

        // remove everything else other than word, number and _
        // $php_variable => php_variable
        if (allowUnicodeIdentifiers) { //could be converted to a single line with ?: operator
            name = Pattern.compile("[\\W&&[^$]]", Pattern.UNICODE_CHARACTER_CLASS).matcher(name).replaceAll(StringUtils.EMPTY);
        }
        else {
            name = name.replaceAll("[\\W&&[^$]]", StringUtils.EMPTY);
        }
        return name;
    }

    private static CodegenModel reconcileInlineEnums(CodegenModel codegenModel, CodegenModel parentCodegenModel) {
        // This generator uses inline classes to define enums, which breaks when
        // dealing with models that have subTypes. To clean this up, we will analyze
        // the parent and child models, look for enums that match, and remove
        // them from the child models and leave them in the parent.
        // Because the child models extend the parents, the enums will be available via the parent.

        // Only bother with reconciliation if the parent model has enums.
        boolean hasEnums = getBooleanValue(parentCodegenModel, HAS_ENUMS_EXT_NAME);
        if  (!hasEnums) {
            return codegenModel;
        }

        // Get the properties for the parent and child models
        final List<CodegenProperty> parentModelCodegenProperties = parentCodegenModel.vars;
        List<CodegenProperty> codegenProperties = codegenModel.vars;

        // Iterate over all of the parent model properties
        boolean removedChildEnum = false;
        for (CodegenProperty parentModelCodegenPropery : parentModelCodegenProperties) {
            // Look for enums
            boolean isEnum = getBooleanValue(parentModelCodegenPropery, IS_ENUM_EXT_NAME);
            if (isEnum) {
                // Now that we have found an enum in the parent class,
                // and search the child class for the same enum.
                Iterator<CodegenProperty> iterator = codegenProperties.iterator();
                while (iterator.hasNext()) {
                    CodegenProperty codegenProperty = iterator.next();
                    isEnum = getBooleanValue(codegenProperty, IS_ENUM_EXT_NAME);
                    // we don't check for the full set of properties as they could be overridden
                    // e.g. in the child; if we used codegenProperty.equals, the result in this
                    // case would be `false` resulting on 2 different enums created on parent and
                    // child classes, used in same method. This means that the child class will use
                    // the enum defined in the parent, loosing any overridden property
                    if (isEnum && isSameEnum(codegenProperty, parentModelCodegenPropery)) {
                        // We found an enum in the child class that is
                        // a duplicate of the one in the parent, so remove it.
                        iterator.remove();
                        removedChildEnum = true;
                    }
                }
            }
        }

        if(removedChildEnum) {
            // If we removed an entry from this model's vars, we need to ensure hasMore is updated
            int count = 0, numVars = codegenProperties.size();
            for(CodegenProperty codegenProperty : codegenProperties) {
                count += 1;
                codegenProperty.getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, (count < numVars) ? true : false);
            }

            if (!codegenProperties.isEmpty()) {
                codegenModel.getVendorExtensions().put(CodegenConstants.HAS_VARS_EXT_NAME, true);
                codegenModel.getVendorExtensions().put(CodegenConstants.HAS_ENUMS_EXT_NAME, false);
            } else {
                codegenModel.emptyVars = true;
                codegenModel.getVendorExtensions().put(CodegenConstants.HAS_VARS_EXT_NAME, false);
                codegenModel.getVendorExtensions().put(CodegenConstants.HAS_ENUMS_EXT_NAME, false);
            }


            codegenModel.vars = codegenProperties;
        }
        return codegenModel;


    }

    protected static boolean isSameEnum(CodegenProperty actual, CodegenProperty other) {
        if (actual == null && other == null) {
            return true;
        }
        if ((actual.name == null) ? (other.name != null) : !actual.name.equals(other.name)) {
            return false;
        }
        if ((actual.baseName == null) ? (other.baseName != null) : !actual.baseName.equals(other.baseName)) {
            return false;
        }
        if ((actual.datatype == null) ? (other.datatype != null) : !actual.datatype.equals(other.datatype)) {
            return false;
        }
        if ((actual.datatypeWithEnum == null) ? (other.datatypeWithEnum != null) : !actual.datatypeWithEnum.equals(other.datatypeWithEnum)) {
            return false;
        }
        if ((actual.baseType == null) ? (other.baseType != null) : !actual.baseType.equals(other.baseType)) {
            return false;
        }
        if (!Objects.equals(actual.enumName, other.enumName)) {
            return false;
        }
        return true;
    }
    private static String sanitizePackageName(String packageName) {
        packageName = packageName.trim(); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
        packageName = packageName.replaceAll("[^a-zA-Z0-9_\\.]", "_");
        if(packageName == null || packageName.isEmpty()) {
            return "invalidPackageName";
        }
        return packageName;
    }

    public void setInvokerPackage(String invokerPackage) {
        this.invokerPackage = invokerPackage;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    private String sanitizePath(String p) {
        //prefer replace a ", instead of a fuLL URL encode for readability
        return p.replaceAll("\"", "%22");
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    /*
     * Derive invoker package name based on the input
     * e.g. foo.bar.model => foo.bar
     *
     * @param input API package/model name
     * @return Derived invoker package name based on API package/model name
     */
    private String deriveInvokerPackageName(String input) {
        String[] parts = input.split(Pattern.quote(".")); // Split on period.

        StringBuilder sb = new StringBuilder();
        String delim = "";
        for (String p : Arrays.copyOf(parts, parts.length-1)) {
            sb.append(delim).append(p);
            delim = ".";
        }
        return sb.toString();
    }

    public String toRegularExpression(String pattern) {
        return escapeText(pattern);
    }

    public boolean convertPropertyToBoolean(String propertyKey) {
        boolean booleanValue = false;
        if (additionalProperties.containsKey(propertyKey)) {
            booleanValue = Boolean.valueOf(additionalProperties.get(propertyKey).toString());
        }

        return booleanValue;
    }

    public void writePropertyBack(String propertyKey, boolean value) {
        additionalProperties.put(propertyKey, value);
    }

    /**
     * Output the Getter name for boolean property, e.g. isActive
     *
     * @param name the name of the property
     * @return getter name based on naming convention
     */
    public String toBooleanGetter(String name) {
        return "get" + getterAndSetterCapitalize(name);
    }

    @Override
    public String sanitizeTag(String tag) {
        tag = camelize(underscore(sanitizeName(tag)));

        // tag starts with numbers
        if (tag.matches("^\\d.*")) {
            tag = "Class" + tag;
        }
        return tag;
    }

    @Override
    public void addHandlebarHelpers(Handlebars handlebars) {
        super.addHandlebarHelpers(handlebars);
        handlebars.registerHelpers(new JavaHelper());
        handlebars.registerHelpers(new HandlebarsHelpers());
    }

    @Override
    public boolean defaultIgnoreImportMappingOption() {
        return true;
    }

    @Override
    public boolean checkAliasModel() {
        return true;
    }
}
