package com.mk.esp;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.concurrent.ConcurrentHashMap;

public class MKESP implements ClientModInitializer {
    public static ConcurrentHashMap<Integer, Vec3d> positions = new ConcurrentHashMap<>();

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.LAST.register(ctx -> {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c.world == null) return;
            RenderSystem.disableDepthTest();
            // Используем стандартный шейдер вместо недоступного getPositionColorProgram
            RenderSystem.setShader(GameRenderer::getPositionProgram);
            
            Tessellator t = Tessellator.getInstance();
            BufferBuilder b = t.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            
            for (Entity e : c.world.getEntities()) {
                if (e == c.player || !e.isAlive()) continue;
                Vec3d pos = positions.getOrDefault(e.getId(), e.getPos()).subtract(ctx.camera().getPos());
                
                // Явно приводим double к float для совместимости
                b.vertex((float)pos.x, (float)pos.y, (float)pos.z).color(1.0f, 0.0f, 0.0f, 1.0f);
                b.vertex((float)pos.x, (float)pos.y + 2.0f, (float)pos.z).color(1.0f, 0.0f, 0.0f, 1.0f);
            }
            BufferRenderer.drawWithGlobalProgram(b.end());
            RenderSystem.enableDepthTest();
        });
    }
}

@Mixin(net.minecraft.client.network.ClientPlayNetworkHandler.class)
class MKNetworkMixin {
    @Inject(method = "onEntityPosition", at = @At("HEAD"))
    void onPos(EntityPositionS2CPacket p, CallbackInfo ci) {
        // Используем геттеры, так как поля защищены (private access)
        MKESP.positions.put(p.getId(), new Vec3d(p.getX(), p.getY(), p.getZ()));
    }
    
    @Inject(method = "onEntitiesDestroy", at = @At("HEAD"))
    void onDest(EntitiesDestroyS2CPacket p, CallbackInfo ci) {
        // Используем правильный метод доступа к массиву
        for(int i : p.getEntityIds()) {
            MKESP.positions.remove(i);
        }
    }
}
