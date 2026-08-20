package com.workoutsmart.exercise.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.exercise.dto.CreateCustomExerciseRequest;
import com.workoutsmart.exercise.dto.ExerciseDetailResponse;
import com.workoutsmart.exercise.dto.ExerciseSearchResponse;
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    private ExerciseService service;

    @BeforeEach
    void setUp() {
        service = new ExerciseService(exerciseRepository);
    }

    private Exercise exercise() {
        return Exercise.builder()
                .id(1L).name("Push Up").category("strength").bodyPart("chest")
                .equipment("body_weight").target("chest").muscleGroup("chest")
                .image("img.png").gifUrl("gif.gif").instructions("[\"Bước 1\"]")
                .status("active").build();
    }

    @Test
    void findByIdReturnsDetail() {
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise()));

        ExerciseDetailResponse res = service.findById(1L);

        assertEquals("Push Up", res.name());
        assertEquals("body_weight", res.equipment());
    }

    @Test
    void findByIdUnknownThrows404() {
        when(exerciseRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.findById(99L));

        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void searchReturnsPagedResults() {
        when(exerciseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(exercise()), PageRequest.of(0, 20), 1));

        ExerciseSearchResponse res = service.search("body_weight", null, null, null, null, 0, 20);

        assertEquals(1, res.totalElements());
        assertEquals(1, res.content().size());
        assertEquals("Push Up", res.content().get(0).name());
    }

    @Test
    void searchAcceptsMultipleCategoriesAndEquipment() {
        when(exerciseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(exercise()), PageRequest.of(0, 20), 1));

        ExerciseSearchResponse res = service.search(
                List.of("body_weight", "dumbbell"), List.of("chest", "back"), null, null, null, 0, 20, null);

        assertEquals(1, res.totalElements());
    }

    @Test
    void findActiveByEquipmentDelegatesToRepository() {
        when(exerciseRepository.findByEquipmentInAndStatus(List.of("body_weight"), "active"))
                .thenReturn(List.of(exercise()));

        List<Exercise> res = service.findActiveByEquipment(List.of("body_weight"));

        assertEquals(1, res.size());
    }

    @Test
    void createCustomRejectsUnknownCategory() {
        CreateCustomExerciseRequest req = new CreateCustomExerciseRequest(
                "Cable row", "back", "cable", "strength", null, null, null, null, null);

        ApiException ex = assertThrows(ApiException.class, () -> service.createCustomExercise(1L, req));

        assertEquals(400, ex.getStatus().value());
    }
}
