package com.bi3l;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mod(modid = "bi3l", name = "BI3L Client", version = "1.0.0", useMetadata = true, acceptableRemoteVersions = "*")
public class Bi3lClient {
    public static KeyBinding openMenu;

    // Variables para Reflection
    private static Field leftClickCounterField;
    private static Field rightClickDelayTimerField;
    private static Method clickMouseMethod;
    private static Method rightClickMouseMethod;
    private static Field networkManagerChannelField;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        openMenu = new KeyBinding("Open BI3L menu", Keyboard.KEY_RSHIFT, "BI3L Client");
        ClientRegistry.registerKeyBinding(openMenu);
        
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);

        setupReflection();
    }

    private void setupReflection() {
        try {
            try { leftClickCounterField = Minecraft.class.getDeclaredField("leftClickCounter"); } 
            catch (NoSuchFieldException e) { leftClickCounterField = Minecraft.class.getDeclaredField("field_71429_W"); }
            leftClickCounterField.setAccessible(true);

            try { rightClickDelayTimerField = Minecraft.class.getDeclaredField("rightClickDelayTimer"); } 
            catch (NoSuchFieldException e) { rightClickDelayTimerField = Minecraft.class.getDeclaredField("field_71467_ac"); }
            rightClickDelayTimerField.setAccessible(true);

            try { clickMouseMethod = Minecraft.class.getDeclaredMethod("clickMouse"); } 
            catch (NoSuchMethodException e) { clickMouseMethod = Minecraft.class.getDeclaredMethod("func_147116_af"); }
            clickMouseMethod.setAccessible(true);

            try { rightClickMouseMethod = Minecraft.class.getDeclaredMethod("rightClickMouse"); } 
            catch (NoSuchMethodException e) { rightClickMouseMethod = Minecraft.class.getDeclaredMethod("func_147121_ag"); }
            rightClickMouseMethod.setAccessible(true);

            try { networkManagerChannelField = NetworkManager.class.getDeclaredField("channel"); } 
            catch (NoSuchFieldException e) { networkManagerChannelField = NetworkManager.class.getDeclaredField("field_150746_k"); }
            networkManagerChannelField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- TÉCNICA GHOST 1: CAMUFLAJE DE RED (SPOOFING) ---
    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        try {
            Channel channel = (Channel) networkManagerChannelField.get(event.manager);
            ChannelPipeline pipeline = channel.pipeline();
            
            pipeline.addBefore("packet_handler", "bi3l_ghost", new ChannelDuplexHandler() {
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    if (msg instanceof C17PacketCustomPayload) {
                        C17PacketCustomPayload packet = (C17PacketCustomPayload) msg;
                        
                        // Intercepta la etiqueta que Forge envía al servidor
                        if ("MC|Brand".equals(packet.getChannelName())) {
                            // Destruye la etiqueta real y envía un mensaje falso diciendo que somos "vanilla"
                            PacketBuffer buf = new PacketBuffer(Unpooled.buffer()).writeString("vanilla");
                            msg = new C17PacketCustomPayload("MC|Brand", buf);
                        }
                    }
                    super.write(ctx, msg, promise);
                }
            });
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

    private static double runClicker(Minecraft mc, Module m, KeyBinding kb, double acc, boolean left) {
        if (!m.enabled || !kb.isKeyDown()) return 0;
        
        // --- TÉCNICA GHOST 2: HUMANIZACIÓN DE CLICS ---
        // Genera un número aleatorio para que la velocidad fluctúe (entre el 70% y el 130% de los CPS que configuraste)
        // Ejemplo: Si pones 10 CPS, dará ráfagas irregulares de 7 a 13 clics simulando una mano real.
        double humanNoise = 0.7 + (Math.random() * 0.6); 
        acc += (m.cps * humanNoise) / 20.0;
        
        while (acc >= 1.0) {
            try {
                if (left) {
                    if (leftClickCounterField != null) leftClickCounterField.set(mc, 0);
                    if (clickMouseMethod != null) clickMouseMethod.invoke(mc);
                    CpsTracker.addLeft();
                } else {
                    if (rightClickDelayTimerField != null) rightClickDelayTimerField.set(mc, 0);
                    if (rightClickMouseMethod != null) rightClickMouseMethod.invoke(mc);
                    CpsTracker.addRight();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
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
            
            fr.drawString(t, 0f, 0f, 0xFF000000 | m.color, m.shadow); 
            GlStateManager.popMatrix();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
    }
}
