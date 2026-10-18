package com.bi3l;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Modules {
    public static final List<String> CATEGORIES =
            Collections.unmodifiableList(Arrays.asList("Combat Display", "HUD", "Combat", "Build"));
    public static final List<Module> ALL = new ArrayList<Module>();

    public static final Module COMBAT_CLICK = Module.clicker("Auto Click", "Combat");
    public static final Module BUILD_CLICK = Module.clicker("Auto Place", "Build");

    static {
        ALL.add(new Module("CPS", "Combat Display", 6.0, 6.0,
                new String[]{"Both", "Left only", "Right only"},
                m -> {
                    if (m.mode == 1) return "CPS: " + CpsTracker.left();
                    if (m.mode == 2) return "CPS: " + CpsTracker.right();
                    return "CPS: " + CpsTracker.left() + " | " + CpsTracker.right();
                }));

        ALL.add(new Module("FPS", "HUD", 6.0, 30.0, null,
                m -> "FPS: " + Minecraft.getDebugFPS()));

        ALL.add(new Module("Coordinates", "HUD", 6.0, 54.0, null, m -> {
            EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
            if (p == null) return "XYZ: -";
            return "XYZ: " + (int) Math.floor(p.posX) + " "
                    + (int) Math.floor(p.posY) + " " + (int) Math.floor(p.posZ);
        }));

        ALL.add(COMBAT_CLICK);
        ALL.add(BUILD_CLICK);
    }
}
