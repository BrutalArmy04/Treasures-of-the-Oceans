package com.oceans;

// One seat at the table. Maps directly to JSON from the frontend later.
public class PlayerConfig {
    private PlayerType type;
    private String name;

    public PlayerConfig() {}
    public PlayerConfig(PlayerType type, String name) {
        this.type = type;
        this.name = name;
    }
    public PlayerType getType() { return type; }
    public void setType(PlayerType type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
