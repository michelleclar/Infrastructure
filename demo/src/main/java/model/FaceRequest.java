package model;


import java.util.Map;

public class FaceRequest {
    String photo;
    Map<String,Object> payload;

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public Map<String,Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String,Object> payload) {
        this.payload = payload;
    }
}
