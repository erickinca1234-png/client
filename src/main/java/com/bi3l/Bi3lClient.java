package com.bi3l;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mod(modid = "bi3l", name = "BI3L Client", version = "1.0.0", useMetadata = true,
        acceptableRemoteVersions = "*")
public class Bi3lClient {
    public static KeyBinding openMenu;

    // Reflexion solo para el clic IZQUIERDO
    private static Field leftClickCounterField;
    private static Method clickMouseMethod;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        openMenu = new KeyBinding("Open BI3L menu", Keyboard.KEY_RSHIFT, "BI3L Client");
        ClientRegistry.registerKeyBinding(openMenu);
        MinecraftForge.EVENT_BUS.register(this);

        setupReflection();
    }

    private void setupReflection() {
        try {
            try { leftClickCounterField = Minecraft.class.getDeclaredField("leftClickCounter"); }
            catch (NoSuchFieldException e) { leftClickCounterField = Minecraft.class.getDeclaredField("field_71429_W"); }
            leftClickCounterField.setAccessible(true);

            try { clickMouseMethod = Minecraft.class.getDeclaredMethod("clickMouse"); }
            catch (NoSuchMethodException e) { clickMouseMethod = Minecraft.class.getDeclaredMethod("func_147116_af"); }
            clickMouseMethod.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double accLeft = 0;
    private double accRight = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        while (openMenu.isPressed()) {
            if (mc.currentScreen == null) {
                mc.displayGuiScreen(new ClientScreen());
            }
        }

        if (mc.thePlayer != null && mc.currentScreen == null && mc.inGameHasFocus) {
            boolean sneaking = mc.thePlayer.isSneaking();
            accLeft = runClicker(mc, Modules.COMBAT_CLICK, mc.gameSettings.keyBindAttack, accLeft, true, sneaking);
            accRight = runClicker(mc, Modules.BUILD_CLICK, mc.gameSettings.keyBindUseItem, accRight, false, sneaking);
        } else {
            accLeft = 0;
            accRight = 0;
        }
    }

    /** Mientras mantienes el clic pulsado, repite el clic a los CPS elegidos. */
    private static double runClicker(Minecraft mc, Module m, KeyBinding kb, double acc, boolean left, boolean sneaking) {
        if (!m.enabled || !kb.isKeyDown()) return 0;
        if (m.pauseSneak && sneaking) return 0;   // agachado: solo el clic normal del juego
        acc += m.cps / 20.0;
        while (acc >= 1.0) {
            if (left) {
                // IZQUIERDO: metodo real de Minecraft, salta el candado leftClickCounter
                // y el bloqueo por isUsingItem() (puedes bloquear con espada y pegar a la vez).
                try {
                    if (leftClickCounterField != null) leftClickCounterField.set(mc, 0);
                    if (clickMouseMethod != null) clickMouseMethod.invoke(mc);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                CpsTracker.addLeft();
            } else {
                // DERECHO: igual que tu version que SI funcionaba. No lo toco.
                KeyBinding.onTick(kb.getKeyCode());
                CpsTracker.addRight();
            }
            acc -= 1.0;
        }
        return acc;
    }

    /** Cuenta tus clics reales (solo lee el evento del mouse, no genera clics). */
    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!e.buttonstate) return;
        if (Minecraft.getMinecraft().currentScreen != null) return;
        if (e.button == 0) CpsTracker.addLeft();
        else if (e.button == 1) CpsTracker.addRight();
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        FontRenderer fr = mc.fontRendererObj;

        for (Module m : Modules.ALL) {
            if (!m.enabled || !m.hud) continue;
            String t = m.text.apply(m);
            int w = fr.getStringWidth(t);
            int pad = 2;

            GlStateManager.pushMatrix();
            GlStateManager.translate((float) m.x, (float) m.y, 0f);
            GlStateManager.scale(m.scale, m.scale, 1f);
            if (m.background) {
                int a = (int) (m.bgOpacity * 2.55f);
                Gui.drawRect(-pad, -pad, w + pad, fr.FONT_HEIGHT + pad - 1, a << 24);
            }
            fr.drawString(t, 0, 0, 0xFF000000 | m.color, m.shadow);
            GlStateManager.popMatrix();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
    }
}
