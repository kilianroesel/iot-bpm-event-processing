package org.tum.configuration;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public final class MongoConfiguration {
    private static volatile MongoConfiguration instance;
    private static final String MONGO_DEV_CONFIG_PATH = "src/main/resources/mongodb.config";
    private static final String MONGO_PROD_CONFIG_PATH = "/etc/iot-bpm-event-processing/mongodb.config";

    private Properties properties;

    private MongoConfiguration() {
        properties = new Properties();
        try {
            if (System.getenv("APP_ENVIRONMENT") != null && System.getenv("APP_ENVIRONMENT").equals("production")) {
                properties.load(new FileReader(MONGO_PROD_CONFIG_PATH));
            } else {
                properties.load(new FileReader(MONGO_DEV_CONFIG_PATH));
            }
        } catch (IOException ex) {
            System.err.println("Error loading configuration file: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static MongoConfiguration getConfiguration() {
        MongoConfiguration result = instance;
        if (result != null) {
            return result;
        }
        synchronized (MongoConfiguration.class) {
            if (instance == null) {
                instance = new MongoConfiguration();
            }
            return instance;
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public Properties getProperties() {
        return properties;
    }
}
