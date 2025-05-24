package ru.gesture.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class Shot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(nullable = false)
    private Session session;

    private String  animal;
    private Float   confidence;
    private Boolean userOk;

    @org.hibernate.annotations.CreationTimestamp
    private java.time.LocalDateTime shotTime;
}
