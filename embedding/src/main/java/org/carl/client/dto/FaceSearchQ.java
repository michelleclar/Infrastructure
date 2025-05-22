package org.carl.client.dto;

import org.carl.component.dto.Query;

public class FaceSearchQ extends Query {
    String photo;

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}
