package com.derrick.wellnesscheck.model.data;

public class Resource {
    public String description, number, message;
    public boolean isSMS;
    public Resource(String description, String number, boolean isSMS){
        this.description = description; this.number = number; this.isSMS = isSMS;
    }

    public Resource setMessage(String message){ this.message = message; return this;}
}
