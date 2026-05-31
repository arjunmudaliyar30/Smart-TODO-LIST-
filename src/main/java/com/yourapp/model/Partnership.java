package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "partnerships")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Partnership {

    @Id
    private String id;

    @Indexed
    private String userId1;

    @Indexed
    private String userId2;

    /** pending / active / declined */
    @Builder.Default
    private String status = "pending";

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
