package com.thomascup.mapper;

import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;

@Component
public class FlexibleJsonMapper {

    public <T> T mapToRecord(Map<String, Object> jsonBody, Class<T> recordClass) {
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException("Class must be a record type");
        }

        try {
            RecordComponent[] components = recordClass.getRecordComponents();
            Object[] args = new Object[components.length];

            // Extract and convert each field
            for (int i = 0; i < components.length; i++) {
                RecordComponent component = components[i];
                String fieldName = component.getName();
                Class<?> fieldType = component.getType();

                Object value = extractAndConvertField(jsonBody, fieldName, fieldType);

                // Validate mandatory fields
                if (value == null) {
                    throw new IllegalArgumentException(fieldName + " is mandatory");
                }

                args[i] = value;
            }

            Constructor<T> constructor = recordClass.getDeclaredConstructor(
                    Arrays.stream(components)
                            .map(RecordComponent::getType)
                            .toArray(Class[]::new)
            );

            return constructor.newInstance(args);

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create " + recordClass.getSimpleName(), e);
        }
    }

    private Object extractAndConvertField(Map<String, Object> jsonBody, String fieldName, Class<?> targetType) {
        Object rawValue = jsonBody.get(fieldName);

        if (rawValue == null) {
            return null;
        }

        // Handle String fields
        if (targetType == String.class) {
            String stringValue = String.valueOf(rawValue).trim();
            if (stringValue.isEmpty()) {
                throw new IllegalArgumentException(fieldName + " cannot be empty");
            }
            return stringValue;
        }

        // Handle Instant fields
        if (targetType == Instant.class) {
            return convertToInstant(rawValue, fieldName);
        }

        // Add more type conversions as needed
        return rawValue;
    }

    private Instant convertToInstant(Object value, String fieldName) {
        try {
            if (value instanceof String) {
                return Instant.parse((String) value);
            } else if (value instanceof Number) {
                return Instant.ofEpochMilli(((Number) value).longValue());
            } else {
                throw new IllegalArgumentException(fieldName + " must be a valid ISO timestamp string or epoch milliseconds");
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid ISO format timestamp", e);
        }
    }


}
