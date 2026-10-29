package com.infotact.venuevista.model;

public enum Tier {
    VIP("VIP"), PREMIUM("Premium"), STANDARD("Standard");

    private final String label;

    Tier(String label) { this.label = label; }

    public String getLabel() { return label; }

    public String getCss() { return name().toLowerCase(); }
}
