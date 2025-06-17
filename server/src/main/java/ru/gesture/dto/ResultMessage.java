package ru.gesture.dto;

import java.util.List;

/**
 * DTO, которое мы отправляем фронту после каждого кадра.
 * <p>Поле {@code finalShot} — true, когда накоплена «засчитанная»
 * серия одинаковых уверенных жестов и историю можно обновлять.
 */
public class ResultMessage {

    private String      animal;
    private float       confidence;
    private List<Float> probs;
    private boolean     finalShot;     // ← новое поле

    /** конструктор без аргументов нужен Jackson */
    public ResultMessage() { }

    public ResultMessage(String animal,
                         float confidence,
                         List<Float> probs,
                         boolean finalShot) {
        this.animal     = animal;
        this.confidence = confidence;
        this.probs      = probs;
        this.finalShot  = finalShot;
    }

    /* ---------- get / set ---------- */

    public String  getAnimal()                 { return animal; }
    public void    setAnimal(String animal)    { this.animal = animal; }

    public float   getConfidence()             { return confidence; }
    public void    setConfidence(float conf)   { this.confidence = conf; }

    public List<Float> getProbs()              { return probs; }
    public void       setProbs(List<Float> p)  { this.probs = p; }

    public boolean isFinalShot()               { return finalShot; }
    public void    setFinalShot(boolean flag)  { this.finalShot = flag; }
}