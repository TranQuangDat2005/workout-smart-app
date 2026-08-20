package com.workoutsmart.exercise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExerciseTaxonomyTest {

    @Test
    void datasetCountsMatchIndexHtml() {
        assertEquals(10, ExerciseTaxonomy.CATEGORIES.size());
        assertEquals(28, ExerciseTaxonomy.EQUIPMENT.size());
        assertEquals(6, ExerciseTaxonomy.MUSCLE_GROUPS.size());
        assertTrue(ExerciseTaxonomy.isCategory("upper arms"));
        assertTrue(ExerciseTaxonomy.isEquipment("body_weight"));
        assertTrue(ExerciseTaxonomy.isMuscleGroup("core"));
    }
}
