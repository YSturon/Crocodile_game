package ru.gesture.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "shots")
public class Shot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Session session;

    private String  animal;
    private Float   confidence;
    private Boolean userOk;

    @CreationTimestamp
    private LocalDateTime shotTime;

    public Shot() {}

    public Shot(Session session, String animal, Float confidence) {
        this.session    = session;
        this.animal     = animal;
        this.confidence = confidence;
    }


    public Long getId()                           { return id; }
    public void setId(Long id)                    { this.id = id; }

    public Session getSession()                   { return session; }
    public void setSession(Session session)       { this.session = session; }

    public String getAnimal()                     { return animal; }
    public void setAnimal(String animal)          { this.animal = animal; }

    public Float getConfidence()                  { return confidence; }
    public void setConfidence(Float confidence)   { this.confidence = confidence; }

    public Boolean getUserOk()                    { return userOk; }
    public void setUserOk(Boolean userOk)         { this.userOk = userOk; }

    public LocalDateTime getShotTime()            { return shotTime; }
    public void setShotTime(LocalDateTime time)   { this.shotTime = time; }
}
