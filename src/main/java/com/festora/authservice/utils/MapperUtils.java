package com.festora.authservice.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MapperUtils {

    private static ObjectMapper objectMapper;
    public MapperUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ===============================
    // Object → JSON String
    // ===============================
    public static String convertObjectToString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert object to JSON string", e);
        }
    }

    // ===============================
    // JSON String → Object
    // ===============================
    public static  <T> T convertStringToObject(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert JSON string to object", e);
        }
    }
}