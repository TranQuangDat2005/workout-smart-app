package com.workoutsmart.exercise;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed thư viện bài tập từ exercises-dataset/data/exercises.json khi DB trống.
 * Bỏ qua khi path rỗng (test) hoặc file không tồn tại.
 */
@Component
public class ExerciseDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExerciseDataSeeder.class);

    private final ExerciseRepository exerciseRepository;
    private final ObjectMapper objectMapper;
    private final String datasetPath;

    public ExerciseDataSeeder(ExerciseRepository exerciseRepository,
                              ObjectMapper objectMapper,
                              @Value("${app.exercises-dataset-path:}") String datasetPath) {
        this.exerciseRepository = exerciseRepository;
        this.objectMapper = objectMapper;
        this.datasetPath = datasetPath;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (datasetPath == null || datasetPath.isBlank()) {
            return;
        }
        Path path = Path.of(datasetPath);
        if (!Files.exists(path)) {
            log.warn("Exercise dataset not found at {}", datasetPath);
            return;
        }
        if (exerciseRepository.count() > 0) {
            return;
        }

        List<DatasetExercise> items = objectMapper.readValue(path.toFile(), new TypeReference<>() {});
        List<Exercise> exercises = items.stream().map(this::toEntity).toList();
        exerciseRepository.saveAll(exercises);
        log.info("Seeded {} exercises from {}", exercises.size(), datasetPath);
    }

    private Exercise toEntity(DatasetExercise item) {
        return Exercise.builder()
                .name(item.name())
                .category(item.category())
                .bodyPart(item.bodyPart())
                .equipment(normalizeEquipment(item.equipment()))
                .target(item.target())
                .muscleGroup(coarseMuscleGroup(item.bodyPart()))
                .image(item.image())
                .gifUrl(item.gifUrl())
                .instructions(item.instructions() != null ? item.instructions().get("en") : null)
                .status("active")
                .build();
    }

    private String normalizeEquipment(String equipment) {
        if (equipment == null || equipment.isBlank()) {
            return "body_weight";
        }
        return equipment.trim().toLowerCase().replace(' ', '_');
    }

    /** Gộp body_part chi tiết về nhóm cơ thô để khớp Rule Engine v1. */
    private String coarseMuscleGroup(String bodyPart) {
        if (bodyPart == null) {
            return "core";
        }
        return switch (bodyPart) {
            case "chest" -> "chest";
            case "back" -> "back";
            case "shoulders" -> "shoulders";
            case "upper arms", "lower arms" -> "arms";
            case "upper legs", "lower legs" -> "legs";
            case "waist" -> "core";
            default -> "core";
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DatasetExercise(
            String id,
            String name,
            String category,
            @JsonProperty("body_part") String bodyPart,
            String equipment,
            Map<String, String> instructions,
            @JsonProperty("muscle_group") String muscleGroup,
            String target,
            String image,
            @JsonProperty("gif_url") String gifUrl) {
    }
}
