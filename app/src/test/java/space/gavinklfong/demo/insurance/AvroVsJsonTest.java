package space.gavinklfong.demo.insurance;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.math3.util.Precision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import space.gavinklfong.demo.insurance.schema.InsuranceClaim;
import space.gavinklfong.demo.insurance.schema.Metadata;
import space.gavinklfong.demo.insurance.schema.Priority;
import space.gavinklfong.demo.insurance.schema.Product;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
class AvroVsJsonTest {

    @ParameterizedTest
    @ValueSource(ints = {10, 100, 1000, 1000, 10000, 100000, 1000000})
    void runTestForAVRO(int size) throws IOException {
        DatumWriter<InsuranceClaim> writer = new SpecificDatumWriter<>(
                InsuranceClaim.class);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(stream, null);

        List<InsuranceClaim> records = generateInsuranceClaims(size);
        long start = System.currentTimeMillis();
        for (InsuranceClaim record : records) {
            writer.write(record, encoder);
        }
        encoder.flush();
        byte[] data = stream.toByteArray();
        long end = System.currentTimeMillis();

        log.info("{}, {}, {}ms", size, data.length, (end - start));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 100, 1000, 1000, 10000, 100000, 1000000})
    void runTestForJSON(int size) throws IOException {
        DatumWriter<InsuranceClaim> writer = new SpecificDatumWriter<>(
                InsuranceClaim.class);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().jsonEncoder(InsuranceClaim.getClassSchema(), stream);
        List<InsuranceClaim> records = generateInsuranceClaims(size);
        long start = System.currentTimeMillis();
        for (InsuranceClaim record : records) {
            writer.write(record, encoder);
        }
        encoder.flush();
        byte[] data = stream.toByteArray();
        long end = System.currentTimeMillis();

        log.info("{}, {}, {}ms", size, data.length, (end - start));
    }


    @Test
    void doTestJsonStringOuptut() throws IOException {
        DatumWriter<InsuranceClaim> writer = new SpecificDatumWriter<>(
                InsuranceClaim.class);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().jsonEncoder(InsuranceClaim.getClassSchema(), stream);

        Metadata metadata = Metadata.newBuilder()
                .setCorrelationId(UUID.randomUUID().toString())
                .setTimestamp(Instant.now())
                .build();

        InsuranceClaim record = InsuranceClaim.newBuilder()
                .setClaimAmount(Precision.round(RandomUtils.nextDouble(), 2))
                .setPriority(Priority.HIGH)
                .setProduct(Product.MEDICAL)
                .setMetadata(metadata)
                .build();

        writer.write(record, encoder);
        encoder.flush();

        log.info("data output: {}", stream);
    }


    private List<InsuranceClaim> generateInsuranceClaims(int size) {
        List<InsuranceClaim> data = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Metadata metadata = Metadata.newBuilder()
                    .setCorrelationId(UUID.randomUUID().toString())
                    .setTimestamp(Instant.now())
                    .build();

            data.add(InsuranceClaim.newBuilder()
                    .setClaimAmount(Precision.round(RandomUtils.nextDouble(), 2))
                    .setPriority(getRandomPriority())
                    .setProduct(getRandomProduct())
                    .setMetadata(metadata)
                    .build());
        }

        return data;
    }

    private Product getRandomProduct() {
        Product[] products = Product.values();
        return products[RandomUtils.nextInt(0, products.length)];
    }

    private Priority getRandomPriority() {
        Priority[] priorities = Priority.values();
        return priorities[RandomUtils.nextInt(0, priorities.length)];
    }
}
