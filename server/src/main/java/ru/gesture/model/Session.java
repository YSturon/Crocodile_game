package ru.gesture.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
public class Session {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(nullable = false)
    private User user;

    private String siteVer;

    @CreationTimestamp
    private LocalDateTime started;

    public Session() {}

    public Session(User user, String siteVer){
        this.user = user;
        this.siteVer = siteVer;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSiteVer() {
        return siteVer;
    }

    public void setSiteVer(String siteVer) {
        this.siteVer = siteVer;
    }

    public LocalDateTime getStarted() {
        return started;
    }

    public void setStarted(LocalDateTime started) {
        this.started = started;
    }
}
