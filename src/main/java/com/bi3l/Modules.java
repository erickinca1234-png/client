package com.bi3l;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Modules {
    public static final List<String> CATEGORIES =
            Collections.unmodifiableList(Arrays.asList("Combat Display", "HUD"));
    public static final List<Module> ALL = new ArrayList<Module>();

    static {
        ALL.add(new Module("CPS", "Combat Display", 6, 6,
                new String[]{"Both", "Left only", "Right only"},
                m -> {
                    if (m.mode == 1) return "CPS: " + CpsTracker.left();
                    if (m.mode == 2) return "CPS: " + CpsTracker.right();
                    return "CPS: " + CpsTracker.left() + " | " + CpsTracker.right();
                }));

        ALL.add(new Module("FPS", "HUD", 6, 30, null,
                m -> "FPS: " + Minecraft.getDebugFPS()));

        ALL.add(new Module("Coordinates", "HUD", 6, 54, null, m -> {
            EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
            if (p == null) return "XYZ: -";
            return "XYZ: " + (int) Math.floor(p.posX) + " "
                    + (int) Math.floor(p.posY) + " " + (int) Math.floor(p.posZ);
        }));
    }

    private Modules() {}
}
