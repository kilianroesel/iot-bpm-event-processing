package org.tum.bpm.sources;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.csv.CsvReaderFormat;
import org.tum.bpm.schemas.measurements.CSVMeasurement;

public class MeasurmentFileSource {

    private static final String DATA_PATH = "data.csv";

    public static FileSource<CSVMeasurement> createMeasurementSource() throws IOException {
        File dataFile = new File(DATA_PATH);

        CsvReaderFormat<CSVMeasurement> csvFormat = CsvReaderFormat.forPojo(CSVMeasurement.class);
        FileSource<CSVMeasurement> source = FileSource
                .forRecordStreamFormat(csvFormat, Path.fromLocalFile(dataFile)).build();

        return source;
    }

    public static WatermarkStrategy<CSVMeasurement> createWatermarkStrategy() {
        WatermarkStrategy<CSVMeasurement> watermarkStrategy = WatermarkStrategy
                // Handle data that is max 20 seconds out of order
                .<CSVMeasurement>forBoundedOutOfOrderness(Duration.ofSeconds(20))
                // Assign event Time Timestamps to each measurement
                .withTimestampAssigner((measurement, timestamp) -> measurement.getTime().toInstant().toEpochMilli());
        return watermarkStrategy;
    }
}
