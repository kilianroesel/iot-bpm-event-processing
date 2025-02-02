package org.tum.configuration;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public final class KafkaConfiguration {
    private static volatile KafkaConfiguration instance;
    private static final String KAFKA_DEV_CONFIG_PATH = "src/main/resources/kafka.config";
    private static final String KAFKA_PROD_CONFIG_PATH = "/etc/iot-bpm-event-processing/kafka.config";

    private Properties properties;

    private KafkaConfiguration() {
        properties = new Properties();
        try {
            if (System.getenv("APP_ENVIRONMENT") != null && System.getenv("APP_ENVIRONMENT").equals("production")) {
                properties.load(new FileReader(KAFKA_PROD_CONFIG_PATH));
            } else {
                properties.load(new FileReader(KAFKA_DEV_CONFIG_PATH));
            }
        } catch (IOException ex) {
            System.err.println("Error loading configuration file: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static KafkaConfiguration getConfiguration() {
        KafkaConfiguration result = instance;
        if (result != null) {
            return result;
        }
        synchronized (KafkaConfiguration.class) {
            if (instance == null) {
                instance = new KafkaConfiguration();
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
