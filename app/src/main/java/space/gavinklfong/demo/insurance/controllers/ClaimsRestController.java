package space.gavinklfong.demo.insurance.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import space.gavinklfong.demo.insurance.schema.InsuranceClaim;
import space.gavinklfong.demo.insurance.schema.InsuranceClaimKey;
import space.gavinklfong.demo.insurance.schema.Metadata;

import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
public class ClaimsRestController {

    private final KafkaTemplate<InsuranceClaimKey, InsuranceClaim> kafkaTemplate;
    private final KafkaProducer<InsuranceClaimKey, InsuranceClaim> kafkaProducer;

    public ClaimsRestController(KafkaTemplate<InsuranceClaimKey, InsuranceClaim> kafkaTemplate,
                                @Value("${spring.kafka.bootstrap-servers}") String kafkaServer) {
        this.kafkaTemplate = kafkaTemplate;
        kafkaProducer = createKafkaProducer(kafkaServer);
    }

    @PostMapping("/claim-2")
    public ResponseEntity<Void> generateClaimRequest2() throws ExecutionException, InterruptedException {
        InsuranceClaimKey key = generateAvroClaimRequestKey();
        InsuranceClaim value = generateAvroClaimRequest();
        kafkaTemplate.send("claim-submitted", key, value).get();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/claim-1")
    public ResponseEntity<Void> generateClaimRequest1() throws ExecutionException, InterruptedException, JsonProcessingException {
        InsuranceClaimKey key = generateAvroClaimRequestKey();
        InsuranceClaim value = generateAvroClaimRequest();
        ProducerRecord<InsuranceClaimKey, InsuranceClaim> producerRecord = new ProducerRecord<>("claim-submitted", key, value);
        kafkaProducer.send(producerRecord).get();
        return ResponseEntity.ok().build();
    }


    private KafkaProducer<InsuranceClaimKey, InsuranceClaim> createKafkaProducer(String kafkaServer) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put("schema.registry.url", "http://localhost:8081");
        return new KafkaProducer<>(props);
    }

    private InsuranceClaim generateAvroClaimRequest() {
        Metadata metadata = Metadata.newBuilder()
                .setCorrelationId(UUID.randomUUID().toString())
                .setTimestamp(Instant.now())
                .build();

        return InsuranceClaim.newBuilder()
                .setClaimAmount(RandomUtils.nextDouble(200, 7000))
                .setPriority(space.gavinklfong.demo.insurance.schema.Priority.HIGH)
                .setProduct(space.gavinklfong.demo.insurance.schema.Product.MEDICAL)
                .setMetadata(metadata)
                .build();
    }

    private InsuranceClaimKey generateAvroClaimRequestKey() {
        return InsuranceClaimKey.newBuilder()
                .setClaimId(UUID.randomUUID().toString())
                .setCustomerId(UUID.randomUUID().toString())
                .build();
    }
}
