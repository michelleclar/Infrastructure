package org.carl.domain.qdrant;

import java.util.List;

public class VectorQuery {
    String text;
    List<Float> vector;

    public boolean isText() {
        return text != null && !text.isEmpty();
    }

    public boolean isVector() {
        return vector != null && !vector.isEmpty();
    }

    public String getText() {
        return text;
    }

    public VectorQuery setText(String text) {
        this.text = text;
        return this;
    }

    public List<Float> getVector() {
        return vector;
    }

    public VectorQuery setVector(List<Float> vector) {
        this.vector = vector;
        return this;
    }
}
