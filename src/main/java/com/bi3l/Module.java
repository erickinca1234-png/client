package com.bi3l;

import java.util.function.Function;

/** Un modulo de HUD con sus ajustes. */
public class Module {
    public final String name;
    public final String category;
    public final String[] modes;              // null si no tiene modos
    public final Function<Module, String> text;

    public boolean enabled = true;
    public int mode = 0;
    public float scale = 1.0f;
    public int color = 0xFFFFFF;              // RGB
    public boolean shadow = true;
    public boolean background = true;
    public int bgOpacity = 60;                // 0-100
    public int x;
    public int y;

    // Para modulos que no son de HUD (autoclick)
    public boolean hud = true;
    public int cps = 10;                      // clics por segundo

    public Module(String name, String category, int x, int y, String[] modes,
                  Function<Module, String> text) {
        this.name = name;
        this.category = category;
        this.x = x;
        this.y = y;
        this.modes = modes;
        this.text = text;
    }

    /** Modulo de clics (apagado por defecto). */
    public static Module clicker(String name, String category) {
        Module m = new Module(name, category, 0, 0, null, null);
        m.hud = false;
        m.enabled = false;
        return m;
    }
}
