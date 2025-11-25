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

    private static final Logger LOG = Logger.getLogger(PythonParameterConverter.class.getName());
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
            LOG.log(Level.INFO, "Python type converter script loaded successfully");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to load Python converter script", e);
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
                    LOG.log(Level.SEVERE, errorMsg);
                    throw new RuntimeException(errorMsg);
                }

                return resultMap;
            }

            return new HashMap<>();

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Python parameter conversion failed", e);
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
     * Close the converter and release resources.
     */
    public void close() {
        if (executor != null) {
            executor.close();
        }
    }
}
