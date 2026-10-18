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
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mod(modid = "bi3l", name = "BI3L Client", version = "1.0.0", useMetadata = true, acceptableRemoteVersions = "*")
public class Bi3lClient {
    public static KeyBinding openMenu;

    // Variables para Reflection (saltar candados privados)
    private static Field leftClickCounterField;
    private static Method clickMouseMethod;
    private static Method rightClickMouseMethod;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        openMenu = new KeyBinding("Open BI3L menu", Keyboard.KEY_RSHIFT, "BI3L Client");
        ClientRegistry.registerKeyBinding(openMenu);
        
        // En 1.8.9, RenderGameOverlay y MouseEvent usan el bus de Forge
        MinecraftForge.EVENT_BUS.register(this);
        // Los TickEvents usan el bus de FML
        FMLCommonHandler.instance().bus().register(this);

        setupReflection();
    }

    private void setupReflection() {
        try {
            // leftClickCounter (Nombre de desarrollo y ofuscado)
            try { leftClickCounterField = Minecraft.class.getDeclaredField("leftClickCounter"); } 
            catch (NoSuchFieldException e) { leftClickCounterField = Minecraft.class.getDeclaredField("field_71429_W"); }
            leftClickCounterField.setAccessible(true);

            // clickMouse
            try { clickMouseMethod = Minecraft.class.getDeclaredMethod("clickMouse"); } 
            catch (NoSuchMethodException e) { clickMouseMethod = Minecraft.class.getDeclaredMethod("func_147116_af"); }
            clickMouseMethod.setAccessible(true);

            // rightClickMouse
            try { rightClickMouseMethod = Minecraft.class.getDeclaredMethod("rightClickMouse"); } 
            catch (NoSuchMethodException e) { rightClickMouseMethod = Minecraft.class.getDeclaredMethod("func_147121_ag"); }
            rightClickMouseMethod.setAccessible(true);
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
        
        if (openMenu.isPressed()) {
            if (mc.currentScreen == null) {
                mc.displayGuiScreen(new ClientScreen()); // Asegúrate de que ClientScreen.java exista
            }
        }

        if (mc.thePlayer != null && mc.currentScreen == null && mc.inGameHasFocus) {
            accLeft = runClicker(mc, Modules.COMBAT_CLICK, mc.gameSettings.keyBindAttack, accLeft, true);
            accRight = runClicker(mc, Modules.BUILD_CLICK, mc.gameSettings.keyBindUseItem, accRight, false);
        } else {
            accLeft = 0;
            accRight = 0;
        }
    }

    private static double runClicker(Minecraft mc, Module m, KeyBinding kb, double acc, boolean left) {
        if (!m.enabled || !kb.isKeyDown()) return 0;
        
        acc += m.cps / 20.0;
        while (acc >= 1.0) {
            try {
                if (left) {
                    if (leftClickCounterField != null) leftClickCounterField.set(mc, 0);
                    if (clickMouseMethod != null) clickMouseMethod.invoke(mc);
                    CpsTracker.addLeft();
                } else {
                    mc.rightClickDelayTimer = 0; // Este es público en 1.8.9, no requiere reflection
                    if (rightClickMouseMethod != null) rightClickMouseMethod.invoke(mc);
                    CpsTracker.addRight();
                }
            } catch (Exception ex) {
                ex.printStackTrace(); // Falla silenciosamente si no encuentra los métodos
            }
            acc -= 1.0;
        }
        return acc;
    }

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
            if (!m.enabled || !m.hud || m.text == null) continue;
            
            String t = m.text.apply(m);
            int w = fr.getStringWidth(t);
            int pad = 2;

            GlStateManager.pushMatrix();
            GlStateManager.translate((float) m.x, (float) m.y, 0f);
            GlStateManager.scale((float) m.scale, (float) m.scale, 1f);
            
            if (m.background) {
                int a = (int) (m.bgOpacity * 255f);
                Gui.drawRect(-pad, -pad, w + pad, fr.FONT_HEIGHT + pad - 1, a << 24);
            }
            
            // Usar floats (0f) previene errores de sobrecarga de métodos en Gradle
            fr.drawString(t, 0f, 0f, 0xFF000000 | m.color, m.shadow); 
            GlStateManager.popMatrix();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
    }
}
