package com.bi3l;

import java.util.function.Function;

public class Module {
    public final String name;
    public final String category;
    public final String[] modes;
    public final Function<Module, String> text;

    public boolean enabled = true;
    public int mode = 0;
    public double scale = 1.0;        // Cambiado a double
    public int color = 0xFFFFFF;
    public boolean shadow = true;
    public boolean background = true;
    public float bgOpacity = 0.23f;    // Cambiado a float (0.0f a 1.0f)
    public double x;                   // Cambiado a double
    public double y;                   // Cambiado a double

    public boolean hud = true;
    public int cps = 10;

    public Module(String name, String category, double x, double y, String[] modes,
                  Function<Module, String> text) {
        this.name = name;
        this.category = category;
        this.x = x;
        this.y = y;
        this.modes = modes;
        this.text = text;
    }

    public static Module clicker(String name, String category) {
        Module m = new Module(name, category, 0, 0, null, null);
        m.hud = false;
        m.enabled = false;
        return m;
    }
}
