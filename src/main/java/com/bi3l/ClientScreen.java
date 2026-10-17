package com.bi3l;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientScreen extends GuiScreen {
    private static final int X0 = 10, Y0 = 10;
    private static final int SIDE_W = 110, MOD_W = 120, SET_W = 190;
    private static final int HEAD_H = 24, BODY_H = 190, ROW = 16;
    private static final int PURPLE = 0xFFAA66FF;
    private static final int BLUE = 0xFF55AAFF;
    private static final int[] PALETTE = {
            0xFFFFFF, 0xFF5555, 0x55FF55, 0x55AAFF, 0xFFFF55, 0xFF55FF, 0xAA66FF
    };

    private String category = Modules.CATEGORIES.get(0);
    private Module selected;

    private static class Row {
        final String label;
        final Supplier<String> value;
        final Runnable dec;
        final Runnable inc;

        Row(String label, Supplier<String> value, Runnable dec, Runnable inc) {
            this.label = label;
            this.value = value;
            this.dec = dec;
            this.inc = inc;
        }

        boolean arrows() { return dec != inc; }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    // ---------- layout ----------
    private int bodyY() { return Y0 + HEAD_H + 2; }
    private int catY(int i) { return bodyY() + 6 + i * 18; }
    private int modY(int i) { return bodyY() + 6 + i * 18; }
    private int rowY(int i) { return bodyY() + 24 + i * ROW; }

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private List<Module> inCategory() {
        List<Module> out = new ArrayList<Module>();
        for (Module m : Modules.ALL) if (m.category.equals(category)) out.add(m);
        return out;
    }

    private static int nextColor(int cur, int dir) {
        int idx = 0;
        for (int i = 0; i < PALETTE.length; i++) if (PALETTE[i] == (cur & 0xFFFFFF)) idx = i;
        int n = PALETTE.length;
        return PALETTE[((idx + dir) % n + n) % n];
    }

    private static int wrap(int v, int n) { return ((v % n) + n) % n; }

    private Row toggle(String label, final Supplier<Boolean> get, Runnable flip) {
        return new Row(label, () -> get.get() ? "ON" : "OFF", flip, flip);
    }

    private List<Row> rows(final Module m) {
        List<Row> r = new ArrayList<Row>();
        r.add(toggle("Enabled", () -> m.enabled, () -> m.enabled = !m.enabled));
        r.add(new Row("Scale", () -> String.format("%.2fx", m.scale),
                () -> m.scale = Math.max(0.5f, Math.round((m.scale - 0.05f) * 100) / 100f),
                () -> m.scale = Math.min(3f, Math.round((m.scale + 0.05f) * 100) / 100f)));
        r.add(new Row("Text Color", () -> String.format("#%06X", m.color),
                () -> m.color = nextColor(m.color, -1),
                () -> m.color = nextColor(m.color, 1)));
        r.add(toggle("Text Shadow", () -> m.shadow, () -> m.shadow = !m.shadow));
        r.add(toggle("Background", () -> m.background, () -> m.background = !m.background));
        r.add(new Row("Background Opacity", () -> m.bgOpacity + "%",
                () -> m.bgOpacity = Math.max(0, m.bgOpacity - 10),
                () -> m.bgOpacity = Math.min(100, m.bgOpacity + 10)));
        r.add(new Row("Position X", () -> String.valueOf(m.x),
                () -> m.x = Math.max(0, m.x - 5),
                () -> m.x = Math.min(2000, m.x + 5)));
        r.add(new Row("Position Y", () -> String.valueOf(m.y),
                () -> m.y = Math.max(0, m.y - 5),
                () -> m.y = Math.min(2000, m.y + 5)));
        if (m.modes != null) {
            r.add(new Row("Mode", () -> m.modes[m.mode],
                    () -> m.mode = wrap(m.mode - 1, m.modes.length),
                    () -> m.mode = wrap(m.mode + 1, m.modes.length)));
        }
        return r;
    }

    // ---------- render ----------
    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        drawRect(0, 0, width, height, 0x55000000);

        int total = SIDE_W + MOD_W + SET_W;
        int by = bodyY();
        int modX = X0 + SIDE_W;
        int setX = modX + MOD_W;

        // cabecera
        drawRect(X0, Y0, X0 + total, Y0 + HEAD_H, 0xE00C0C14);
        fontRendererObj.drawStringWithShadow("BI3L", X0 + 8, Y0 + 8, PURPLE);
        fontRendererObj.drawStringWithShadow("CLIENT",
                X0 + 8 + fontRendererObj.getStringWidth("BI3L "), Y0 + 8, 0xFFFFFFFF);
        String hint = "Right Shift = cerrar";
        fontRendererObj.drawString(hint,
                X0 + total - 8 - fontRendererObj.getStringWidth(hint), Y0 + 8, 0xFF888888);

        // paneles
        drawRect(X0, by, modX, by + BODY_H, 0xD0101018);
        drawRect(modX, by, setX, by + BODY_H, 0xD0161622);
        drawRect(setX, by, X0 + total, by + BODY_H, 0xD01C1C2A);

        // categorias
        int i = 0;
        for (String c : Modules.CATEGORIES) {
            int ry = catY(i);
            boolean sel = c.equals(category);
            if (sel) {
                drawRect(X0, ry, X0 + 2, ry + 16, PURPLE);
                drawRect(X0 + 2, ry, modX, ry + 16, 0x40AA66FF);
            } else if (hit(mx, my, X0, ry, SIDE_W, 16)) {
                drawRect(X0, ry, modX, ry + 16, 0x20FFFFFF);
            }
            fontRendererObj.drawString(c, X0 + 10, ry + 4, sel ? 0xFFFFFFFF : 0xFFAAAAAA);
            i++;
        }

        // modulos
        List<Module> mods = inCategory();
        for (int j = 0; j < mods.size(); j++) {
            Module m = mods.get(j);
            int ry = modY(j);
            if (m == selected) drawRect(modX, ry, setX, ry + 16, 0x40AA66FF);
            else if (hit(mx, my, modX, ry, MOD_W, 16)) drawRect(modX, ry, setX, ry + 16, 0x20FFFFFF);
            fontRendererObj.drawString(m.name, modX + 8, ry + 4, 0xFFFFFFFF);
            String st = m.enabled ? "ON" : "OFF";
            fontRendererObj.drawString(st, setX - 8 - fontRendererObj.getStringWidth(st), ry + 4,
                    m.enabled ? 0xFF55FF55 : 0xFFFF5555);
        }

        // ajustes
        if (selected == null) {
            fontRendererObj.drawString("Elige un modulo", setX + 10, by + 8, 0xFF888888);
        } else {
            fontRendererObj.drawStringWithShadow(selected.name, setX + 10, by + 6, PURPLE);
            List<Row> rows = rows(selected);
            for (int k = 0; k < rows.size(); k++) {
                Row row = rows.get(k);
                int ry = rowY(k);
                if (hit(mx, my, setX, ry, SET_W, ROW)) drawRect(setX, ry, X0 + total, ry + ROW, 0x20FFFFFF);
                fontRendererObj.drawString(row.label, setX + 10, ry + 4, 0xFFFFFFFF);
                String v = row.value.get();
                String shown = row.arrows() ? "< " + v + " >" : v;
                fontRendererObj.drawString(shown,
                        X0 + total - 8 - fontRendererObj.getStringWidth(shown), ry + 4, BLUE);
            }
        }
    }

    // ---------- input ----------
    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button == 0) {
            int modX = X0 + SIDE_W;
            int setX = modX + MOD_W;

            int i = 0;
            for (String c : Modules.CATEGORIES) {
                if (hit(mx, my, X0, catY(i), SIDE_W, 16)) {
                    category = c;
                    selected = null;
                    return;
                }
                i++;
            }

            List<Module> mods = inCategory();
            for (int j = 0; j < mods.size(); j++) {
                if (hit(mx, my, modX, modY(j), MOD_W, 16)) {
                    Module m = mods.get(j);
                    if (mx >= setX - 34) m.enabled = !m.enabled;   // clic en ON/OFF
                    else selected = m;
                    return;
                }
            }

            if (selected != null) {
                List<Row> rows = rows(selected);
                for (int k = 0; k < rows.size(); k++) {
                    if (hit(mx, my, setX, rowY(k), SET_W, ROW)) {
                        Row row = rows.get(k);
                        if (!row.arrows()) {
                            row.inc.run();
                        } else {
                            String shown = "< " + row.value.get() + " >";
                            double center = setX + SET_W - 8 - fontRendererObj.getStringWidth(shown) / 2.0;
                            if (mx < center) row.dec.run(); else row.inc.run();
                        }
                        return;
                    }
                }
            }
        }
        super.mouseClicked(mx, my, button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_RSHIFT || keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}
