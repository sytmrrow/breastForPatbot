package com.example.activity;

public class TargetPoint {
    private int floor;
    private String id;
    private String NLP_name;
    private String Num_name;
    private float x;
    private float y;

    // Constructor
    public TargetPoint(int floor, String id, String NLP_name, String Num_name, float x, float y) {
        this.floor = floor;
        this.id = id;
        this.NLP_name = NLP_name;
        this.Num_name = Num_name;
        this.x = x;
        this.y = y;
    }

    // Getters and Setters
    public int getFloor() {
        return floor;
    }

    public String getId() {
        return id;
    }

    public String getNLP_name() {
        return NLP_name;
    }

    public String getNum_name() {
        return Num_name;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
