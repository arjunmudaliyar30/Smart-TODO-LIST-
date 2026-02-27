package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a fitness category (e.g. CALORIES, STRENGTH, COMBAT or a user-created custom one).
 * System defaults have userId = null and editable = false.
 * User-created categories carry the owner's userId and editable = true.
 */
@Document(collection = "fitness_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FitnessCategory {

    @Id
    private String id;

    /** Category name — stored uppercase (e.g. "CALORIES", "STRENGTH", "COMBAT"). */
    private String name;

    /**
     * Owner user ID.
     *  null  → system default (visible to everyone, not editable)
     *  value → user-created (visible only to that user, editable)
     */
    @Indexed
    private String userId;

    /** false for system defaults, true for user-created custom categories. */
    @Builder.Default
    private boolean editable = true;
}
