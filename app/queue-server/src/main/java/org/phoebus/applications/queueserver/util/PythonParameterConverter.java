package org.phoebus.applications.queueserver.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Python-based parameter type converter for Queue Server.
 *
 * This class delegates type conversion to a Python script (using Jython),
 * allowing us to use Python's ast.literal_eval for parsing parameter values
 * instead of implementing complex type conversions in Java.
 */
public class PythonParameterConverter {

    private static final Logger logger = Logger.getLogger(PythonParameterConverter.class.getPackageName());
    private static final String SCRIPT_RESOURCE = "/org/phoebus/applications/queueserver/scripts/type_converter.py";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JythonScriptExecutor executor;
    private final String scriptContent;

    /**
     * Create a new Python parameter converter.
     */
    public PythonParameterConverter() {
        this.executor = new JythonScriptExecutor();

        // Load the Python script from resources
        try (var stream = getClass().getResourceAsStream(SCRIPT_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Python converter script not found: " + SCRIPT_RESOURCE);
            }
            this.scriptContent = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            logger.log(Level.FINE, "Python type converter script loaded successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load Python converter script", e);
            throw new RuntimeException("Failed to initialize Python converter", e);
        }
    }

    /**
     * Parameter information for conversion.
     */
    public static class ParameterInfo {
        private String name;
        private String value;
        private boolean enabled;
        private boolean isOptional;
        private Object defaultValue;

        public ParameterInfo(String name, String value, boolean enabled, boolean isOptional, Object defaultValue) {
            this.name = name;
            this.value = value;
            this.enabled = enabled;
            this.isOptional = isOptional;
            this.defaultValue = defaultValue;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public boolean isOptional() { return isOptional; }
        public void setOptional(boolean optional) { isOptional = optional; }

        public Object getDefaultValue() { return defaultValue; }
        public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
    }

    /**
     * Convert a list of parameters from string values to typed objects using Python.
     *
     * @param parameters List of parameter information
     * @return Map of parameter names to their typed values
     */
    public Map<String, Object> convertParameters(List<ParameterInfo> parameters) {
        try {
            // Serialize parameters to JSON
            String parametersJson = objectMapper.writeValueAsString(parameters);

            // Prepare context for Python script
            Map<String, Object> context = new HashMap<>();
            context.put("parameters_json", parametersJson);

            // Execute the Python script
            Object result = executor.execute(scriptContent, context);

            // Parse result JSON back to Map
            if (result != null) {
                String resultJson = result.toString();

                // Check if result contains an error
                Map<String, Object> resultMap = objectMapper.readValue(resultJson,
                        new TypeReference<Map<String, Object>>() {});

                if (resultMap.containsKey("error")) {
                    String errorMsg = "Python type conversion failed: " + resultMap.get("error");
                    logger.log(Level.SEVERE, errorMsg);
                    throw new RuntimeException(errorMsg);
                }

                return resultMap;
            }

            return new HashMap<>();

        } catch (Exception e) {
            logger.log(Level.WARNING, "Python parameter conversion failed", e);
            throw new RuntimeException("Parameter conversion failed: " + e.getMessage(), e);
        }
    }


    /**
     * Validate a parameter value using Python.
     *
     * @param value String value to validate
     * @return true if the value can be parsed, false otherwise
     */
    public boolean validateValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        try {
            List<ParameterInfo> testParam = List.of(
                    new ParameterInfo("test", value, true, true, null)
            );
            convertParameters(testParam);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Normalize a value by parsing it if it's a string, then converting to Python repr.
     * This ensures that string representations of numbers like "1" become 1 (without quotes),
     * while actual strings like "hello" become 'hello' (with quotes).
     *
     * @param value The value to normalize
     * @return Python repr string with correct types
     */
    public String normalizeAndRepr(Object value) {
        if (value == null) {
            return "None";
        }

        // If it's already a proper Java type (not String), just repr it
        if (!(value instanceof String)) {
            return toPythonRepr(value);
        }

        // It's a string - try to parse it to see what type it really is
        String strValue = (String) value;
        if (strValue.trim().isEmpty()) {
            return "";
        }

        try {
            // Try to parse with Python to determine true type
            List<ParameterInfo> testParam = List.of(
                    new ParameterInfo("test", strValue, true, true, null)
            );
            Map<String, Object> parsed = convertParameters(testParam);

            if (parsed.containsKey("test")) {
                // Successfully parsed - use the parsed value's repr
                return toPythonRepr(parsed.get("test"));
            }
        } catch (Exception e) {
            // If parsing fails, treat as a string literal
        }

        // Fallback - just repr the string as-is
        return toPythonRepr(value);
    }

    /**
     * Convert a Java object to its Python string representation (repr).
     * This preserves Python syntax including quotes around strings in lists/dicts.
     *
     * @param value The Java object to convert
     * @return Python repr string
     */
    public static String toPythonRepr(Object value) {
        if (value == null) {
            return "None";
        }

        // Handle strings - add single quotes
        if (value instanceof String) {
            String str = (String) value;
            // Escape single quotes and backslashes
            str = str.replace("\\", "\\\\").replace("'", "\\'");
            return "'" + str + "'";
        }

        // Handle booleans - Python uses True/False
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "True" : "False";
        }

        // Handle numbers - convert to string directly
        if (value instanceof Number) {
            return value.toString();
        }

        // Handle lists
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(toPythonRepr(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }

        // Handle maps/dicts
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(toPythonRepr(entry.getKey()));
                sb.append(": ");
                sb.append(toPythonRepr(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }

        // Fallback - just use toString
        return value.toString();
    }

    /**
     * Close the converter and release resources.
     */
    public void close() {
        if (executor != null) {
            executor.close();
        }
    }
}
