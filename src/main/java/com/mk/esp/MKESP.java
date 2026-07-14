package com.mk.esp;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
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
            Tessellator t = Tessellator.getInstance();
            BufferBuilder b = t.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            
            for (Entity e : c.world.getEntities()) {
                if (e == c.player || !e.isAlive()) continue;
                Vec3d pos = positions.getOrDefault(e.getId(), e.getPos()).subtract(ctx.camera().getPos());
                
                b.vertex(pos.x, pos.y, pos.z).color(1, 0, 0, 1);
                b.vertex(pos.x, pos.y + 2, pos.z).color(1, 0, 0, 1);
            }
            try { 
                BufferRenderer.drawWithGlobalProgram(b.end()); 
            } catch (Exception ignored) {}
            RenderSystem.enableDepthTest();
        });
    }
}

@Mixin(net.minecraft.client.network.ClientPlayNetworkHandler.class)
class MKNetworkMixin {
    @Inject(method = "onEntityPosition", at = @At("HEAD"))
    void onPos(EntityPositionS2CPacket p, CallbackInfo ci) {
        MKESP.positions.put(p.getEntityId(), new Vec3d(p.getX(), p.getY(), p.getZ()));
    }
    
    @Inject(method = "onEntitiesDestroy", at = @At("HEAD"))
    void onDest(EntitiesDestroyS2CPacket p, CallbackInfo ci) {
        for(int i : p.getEntityIds()) {
            MKESP.positions.remove(i);
        }
    }
}
