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

@Mod(modid = "bi3l", name = "BI3L Client", version = "1.0.0", useMetadata = true,
        acceptableRemoteVersions = "*")
public class Bi3lClient {
    public static KeyBinding openMenu;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        openMenu = new KeyBinding("Open BI3L menu", Keyboard.KEY_RSHIFT, "BI3L Client");
        ClientRegistry.registerKeyBinding(openMenu);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private double accLeft = 0;
    private double accRight = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        
        // Usamos 'if' en lugar de 'while' para evitar que el juego se congele
        if (openMenu.isPressed()) {
            if (mc.currentScreen == null) {
                mc.displayGuiScreen(new ClientScreen());
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

    /** Clic real, saltando los candados de vanilla. */
    private static double runClicker(Minecraft mc, Module m, KeyBinding kb, double acc, boolean left) {
        if (!m.enabled || !kb.isKeyDown()) return 0;
        
        acc += m.cps / 20.0;
        while (acc >= 1.0) {
            if (left) {
                mc.leftClickCounter = 0;      // Fix: Quita el candado del clic izquierdo
                mc.clickMouse();              // Ejecuta el ataque
                CpsTracker.addLeft();
            } else {
                mc.rightClickMouse();         // Ejecuta el uso de item
                mc.rightClickDelayTimer = 0;   // Fix: Evita el delay de vanilla
                CpsTracker.addRight();
            }
            acc -= 1.0;
        }
        return acc;
    }

    /** Cuenta los clics reales del mouse. */
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
            
            // Evitar error si la función de texto es nula
            if (m.text == null) continue;
            
            String t = m.text.apply(m);
            int w = fr.getStringWidth(t);
            int pad = 2;

            GlStateManager.pushMatrix();
            // Convertimos x e y a float para GlStateManager
            GlStateManager.translate((float) m.x, (float) m.y, 0f);
            GlStateManager.scale((float) m.scale, (float) m.scale, 1f);
            
            if (m.background) {
                // Calculamos la opacidad basándonos en el float de Module.java
                int a = (int) (m.bgOpacity * 255f);
                Gui.drawRect(-pad, -pad, w + pad, fr.FONT_HEIGHT + pad - 1, a << 24);
            }
            
            fr.drawString(t, 0, 0, 0xFF000000 | m.color, m.shadow);
            GlStateManager.popMatrix();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
    }
}
