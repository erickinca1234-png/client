package com.bi3l;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity; // Importación nueva
import net.minecraft.util.AxisAlignedBB; // Importación nueva
import net.minecraft.util.MovingObjectPosition; // Importación nueva
import net.minecraft.util.Vec3; // Importación nueva
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
import java.util.List;

@Mod(modid = "bi3l", name = "BI3L Client", version = "1.0.0", useMetadata = true,
        acceptableRemoteVersions = "*")
public class Bi3lClient {
    public static KeyBinding openMenu;

    // Reflexión para el clic IZQUIERDO (Combat Click / Autoclicker)
    private static Field leftClickCounterField;
    private static Method clickMouseMethod;

    // Reflexión para el Autoplace (elimina el delay de colocación sin dar clics falsos)
    private static Field rightClickDelayTimerField;

    // Distancia personalizada para pegar (Vanilla es 3.0)
    public static double reachDistance = 4.5; 

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        openMenu = new KeyBinding("Open BI3L menu", Keyboard.KEY_RSHIFT, "BI3L Client");
        ClientRegistry.registerKeyBinding(openMenu);
        MinecraftForge.EVENT_BUS.register(this);

        setupReflection();
    }

    private void setupReflection() {
        try {
            // Clic Izquierdo
            try { leftClickCounterField = Minecraft.class.getDeclaredField("leftClickCounter"); }
            catch (NoSuchFieldException e) { leftClickCounterField = Minecraft.class.getDeclaredField("field_71429_W"); }
            leftClickCounterField.setAccessible(true);

            try { clickMouseMethod = Minecraft.class.getDeclaredMethod("clickMouse"); }
            catch (NoSuchMethodException e) { clickMouseMethod = Minecraft.class.getDeclaredMethod("func_147116_af"); }
            clickMouseMethod.setAccessible(true);

            // Clic Derecho / Autoplace (rightClickDelayTimer)
            try { rightClickDelayTimerField = Minecraft.class.getDeclaredField("rightClickDelayTimer"); }
            catch (NoSuchFieldException e) { rightClickDelayTimerField = Minecraft.class.getDeclaredField("field_71467_ac"); }
            rightClickDelayTimerField.setAccessible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double accLeft = 0;

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
            
            // 1. Ejecutar Autoclicker izquierdo + Reach
            accLeft = runLeftClicker(mc, Modules.COMBAT_CLICK, mc.gameSettings.keyBindAttack, accLeft, sneaking);

            // 2. Ejecutar Autoplace puro
            handleAutoplace(mc, Modules.BUILD_CLICK, mc.gameSettings.keyBindUseItem, sneaking);

        } else {
            accLeft = 0;
        }
    }

    /** Autoclicker para el botón IZQUIERDO (combate) con Reach implementado */
    private static double runLeftClicker(Minecraft mc, Module m, KeyBinding kb, double acc, boolean sneaking) {
        if (!m.enabled || !kb.isKeyDown()) return 0;
        if (m.pauseSneak && sneaking) return 0;
        acc += m.cps / 20.0;
        while (acc >= 1.0) {
            try {
                if (leftClickCounterField != null) leftClickCounterField.set(mc, 0);
                
                // Lógica de REACH: Buscamos si hay una entidad en la mira dentro de nuestra distancia extendida
                Entity target = getEntityInCrosshair(mc, reachDistance);

                if (target != null) {
                    // Si hay una entidad lejos pero dentro del Reach, la atacamos directamente
                    mc.thePlayer.swingItem();
                    mc.playerController.attackEntity(mc.thePlayer, target);
                } else {
                    // Si no hay entidad (o golpea un bloque), usamos el clic normal
                    if (clickMouseMethod != null) clickMouseMethod.invoke(mc);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
            CpsTracker.addLeft();
            acc -= 1.0;
        }
        return acc;
    }

    /** 
     * Calcula manualmente si hay una entidad en la dirección donde miramos,
     * extendiendo el límite de distancia de ataque del juego.
     */
    private static Entity getEntityInCrosshair(Minecraft mc, double distance) {
        if (mc.getRenderViewEntity() == null || mc.theWorld == null) return null;

        Entity pointedEntity = null;
        Vec3 eyePos = mc.getRenderViewEntity().getPositionEyes(1.0F);
        Vec3 lookVec = mc.getRenderViewEntity().getLook(1.0F);
        Vec3 targetVec = eyePos.addVector(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance);

        // Creamos una caja de colisión para buscar entidades en esa dirección
        AxisAlignedBB searchBox = mc.getRenderViewEntity().getEntityBoundingBox()
                .addCoord(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance)
                .expand(1.0D, 1.0D, 1.0D);

        List<Entity> list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.getRenderViewEntity(), searchBox);
        double minDistance = distance;

        for (Entity entity : list) {
            if (entity.canBeCollidedWith()) {
                float borderSize = entity.getCollisionBorderSize();
                AxisAlignedBB aabb = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
                MovingObjectPosition intercept = aabb.calculateIntercept(eyePos, targetVec);

                if (aabb.isVecInside(eyePos)) {
                    if (minDistance >= 0.0D) {
                        pointedEntity = entity;
                        minDistance = 0.0D;
                    }
                } else if (intercept != null) {
                    double dist = eyePos.distanceTo(intercept.hitVec);
                    if (dist < minDistance || minDistance == 0.0D) {
                        pointedEntity = entity;
                        minDistance = dist;
                    }
                }
            }
        }
        return pointedEntity;
    }

    /** AUTOPLACE: Elimina el delay interno del juego para colocar bloques al instante */
    private static void handleAutoplace(Minecraft mc, Module m, KeyBinding kb, boolean sneaking) {
        if (!m.enabled || !kb.isKeyDown()) return;
        if (m.pauseSneak && sneaking) return;
        try {
            if (rightClickDelayTimerField != null) {
                rightClickDelayTimerField.set(mc, 0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Cuenta tus clics reales (solo lee el evento del mouse físico). */
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
