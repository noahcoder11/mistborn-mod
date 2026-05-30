package com.not_noah.mistborn_metal_arts.client.hud;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.client.ClientMetalArtsData;
import com.not_noah.mistborn_metal_arts.config.CommonConfig;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.ArrayList;
import java.util.List;

public final class MetalArtsHudOverlay {
    private static final List<Spark> SPARKS = new ArrayList<>();
    private static final java.util.Random RANDOM = new java.util.Random();

    private MetalArtsHudOverlay() {
    }

    private static class Spark {
        float x, y;
        float vx, vy;
        int age;
        int maxAge;
        int color;

        void tick() {
            x += vx;
            y += vy;
            vy -= 0.03F; // upward drift
            age++;
        }
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!CommonConfig.VALUES.showHud.get() || Minecraft.getInstance().options.hideGui) {
            return;
        }

        MetalArtsData data = ClientMetalArtsData.data();
        if (data.allomanticPowers().isEmpty() && data.feruchemicalPowers().isEmpty()) {
            return;
        }

        Font font = Minecraft.getInstance().font;

        // Tick and render the lightweight particle system
        tickAndRenderSparks(graphics);

        // Center-aligned hotbar coordinates
        int hotbarLeft = screenWidth / 2 - 91;
        int hotbarRight = screenWidth / 2 + 91;

        // Allomancy stack (left of hotbar)
        int allomancyX = hotbarLeft - 30;
        int allomancyY = screenHeight - 22;
        int allomancyCount = 0;
        Metal selected = ClientMetalArtsData.selectedMetal();

        // 1. Draw Allomancy Gauges
        for (Metal metal : Metal.cachedValues()) {
            if (!metal.isAllomantic()) continue;
            float reserve = data.getReserve(metal);
            boolean burning = data.isBurning(metal);
            boolean flaring = data.isFlaring(metal);
            boolean isSelected = (metal == selected);

            if (reserve > 0F || burning || (isSelected && data.hasAllomanticPower(metal))) {
                drawCircularGauge(graphics, font, allomancyX, allomancyY, metal, reserve, burning, flaring, isSelected, true, data);
                allomancyY -= 24;
                allomancyCount++;
                if (allomancyCount >= 7) break; // Limit vertical height
            }
        }

        // 2. Draw Feruchemy Gauges
        int feruchemyX = hotbarRight + 30;
        int feruchemyY = screenHeight - 22;
        int feruchemyCount = 0;

        for (Metal metal : Metal.cachedValues()) {
            if (!metal.isFeruchemical()) continue;
            float charge = data.getMetalmindCharge(metal);
            int modeVal = data.feruchemyMode(metal);
            boolean active = modeVal != 0;
            boolean isSelected = (metal == selected);

            if (charge > 0F || active || (isSelected && data.hasFeruchemicalPower(metal))) {
                drawCircularGauge(graphics, font, feruchemyX, feruchemyY, metal, charge, active, false, isSelected, false, data);
                feruchemyY -= 24;
                feruchemyCount++;
                if (feruchemyCount >= 7) break; // Limit vertical height
            }
        }

        // 3. Draw Bronze Pulses & Corruption if active
        int textY = screenHeight - 50;
        if (CommonConfig.VALUES.showBronzePulse.get() && data.bronzePulseStrength() > 0.01F) {
            float strength = data.bronzePulseStrength();
            float distance = data.bronzePulseDistance();
            String metalName = data.bronzePulseMetal().isEmpty() ? "unknown" : data.bronzePulseMetal();

            int cx = screenWidth / 2;
            int cy = screenHeight / 2 + 20;

            ResourceLocation arrowTex = new ResourceLocation("mistborn_metal_arts", "textures/gui/bronze_pulse_arrow.png");
            
            graphics.pose().pushPose();
            graphics.pose().translate(cx, cy, 0);
            
            // Calculate relative yaw: target absolute yaw minus player's current yaw
            float playerYaw = Minecraft.getInstance().player.getYRot();
            float relativeYaw = data.bronzePulseYaw() - playerYaw;
            
            graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(relativeYaw));
            
            // Pulsate compass size based on strength
            float scale = 0.8F + strength * 0.4F + 0.1F * (float) Math.sin(System.currentTimeMillis() * 0.012F);
            graphics.pose().scale(scale, scale, 1.0F);

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(0.84F, 0.65F, 0.41F, 0.35F + strength * 0.65F);
            
            // Draw 12x12 arrow centered
            graphics.blit(arrowTex, -6, -6, 0, 0, 12, 12, 12, 12);
            
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.pose().popPose();

            // Render distance text underneath the crosshair compass
            graphics.drawCenteredString(font, metalName + " (" + Math.round(distance) + "m)", cx, cy + 8, 0xD6A66A);
        }
        if (data.totalCorruption() > 0) {
            graphics.drawString(font, "Corruption: " + data.totalCorruption(), hotbarRight + 12, textY, 0xD47A7A, true);
        }

        // 4. Render Spiritweb Status Panel in the top-left corner
        int startX = 10;
        int startY = 10;
        long time = System.currentTimeMillis();

        // Title text
        graphics.drawString(font, "SPIRITWEB STATUS", startX, startY, 0xFFCFD8DC, true);

        // Mandala Center Coordinates
        float cx = startX + 20;
        float cy = startY + 28;

        float stability = data.soulStability();
        float contamination = data.identityContamination();
        float bloat = data.spiritualBloat();

        int stabilityColor = stability < 30.0F ? (time % 500 < 250 ? 0xFFFF3D00 : 0xFFFF8A65) : 0xFF00E5FF;
        int contaminationColor = 0xFF7C4DFF;
        int bloatColor = 0xFFFF4081;

        // Draw Mandala Backdrop Plate
        drawDisc(graphics, cx, cy, 19.5F, 0xCC0D121B);
        drawRingSegment(graphics, cx, cy, 19.0F, 19.5F, 0F, (float)(2 * Math.PI), 0x22FFFFFF);

        // Draw Outer Stability Ring
        drawRingSegment(graphics, cx, cy, 16.0F, 18.5F, 0F, (float)(2 * Math.PI), 0x15FFFFFF);
        float stabPct = Math.max(0.0F, Math.min(1.0F, stability / 100.0F));
        if (stabPct > 0F) {
            float startAngle = -(float)Math.PI / 2F;
            float endAngle = startAngle + (float)(2 * Math.PI * stabPct);
            drawRingSegment(graphics, cx, cy, 16.0F, 18.5F, startAngle, endAngle, stabilityColor);
        }

        // Draw Middle Contamination Ring
        drawRingSegment(graphics, cx, cy, 12.5F, 15.0F, 0F, (float)(2 * Math.PI), 0x15FFFFFF);
        float contPct = Math.max(0.0F, Math.min(1.0F, contamination / 100.0F));
        if (contPct > 0F) {
            float startAngle = -(float)Math.PI / 2F;
            float endAngle = startAngle + (float)(2 * Math.PI * contPct);
            drawRingSegment(graphics, cx, cy, 12.5F, 15.0F, startAngle, endAngle, contaminationColor);
        }

        // Draw Inner Bloat Ring
        drawRingSegment(graphics, cx, cy, 9.0F, 11.5F, 0F, (float)(2 * Math.PI), 0x15FFFFFF);
        float bloatPct = Math.max(0.0F, Math.min(1.0F, bloat / 100.0F));
        if (bloatPct > 0F) {
            float startAngle = -(float)Math.PI / 2F;
            float endAngle = startAngle + (float)(2 * Math.PI * bloatPct);
            drawRingSegment(graphics, cx, cy, 9.0F, 11.5F, startAngle, endAngle, bloatColor);
        }

        // Draw Pulsing Spiritual Center Core
        drawDisc(graphics, cx, cy, 8.0F, 0xCC080D14);
        float pulse = 0.35F + 0.2F * (float) Math.sin(time * 0.005F);
        int glowColor = (int)(pulse * 255) << 24 | (stabilityColor & 0xFFFFFF);
        drawDisc(graphics, cx, cy, 4.5F, glowColor);

        // Render Aligned Status Text Labels next to the Mandala
        int textX = startX + 46;
        
        // Stability line
        graphics.drawString(font, "Stability:", textX, (int)cy - 12, 0xFFCFD8DC, true);
        graphics.drawString(font, Math.round(stability) + "%", textX + font.width("Stability: "), (int)cy - 12, stabilityColor, true);

        // Contamination line
        graphics.drawString(font, "Contamination:", textX, (int)cy - 2, 0xFFCFD8DC, true);
        graphics.drawString(font, Math.round(contamination) + "%", textX + font.width("Contamination: "), (int)cy - 2, 0xFFB388FF, true);

        // Bloat line
        graphics.drawString(font, "Bloat:", textX, (int)cy + 8, 0xFFCFD8DC, true);
        graphics.drawString(font, Math.round(bloat) + "%", textX + font.width("Bloat: "), (int)cy + 8, 0xFFFF80AB, true);
    }

    private static void tickAndRenderSparks(GuiGraphics graphics) {
        // Ticking
        SPARKS.removeIf(spark -> {
            spark.tick();
            return spark.age >= spark.maxAge;
        });

        if (SPARKS.isEmpty()) return;

        // Rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        var matrix = graphics.pose().last().pose();
        for (Spark spark : SPARKS) {
            float lifePct = 1.0F - ((float) spark.age / spark.maxAge);
            float size = 0.8F * lifePct + 0.4F;
            
            float a = (float) (spark.color >> 24 & 255) / 255.0F * lifePct;
            float r = (float) (spark.color >> 16 & 255) / 255.0F;
            float g = (float) (spark.color >> 8 & 255) / 255.0F;
            float b = (float) (spark.color & 255) / 255.0F;
            
            bufferBuilder.vertex(matrix, spark.x - size, spark.y - size, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, spark.x - size, spark.y + size, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, spark.x + size, spark.y + size, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, spark.x + size, spark.y - size, 0).color(r, g, b, a).endVertex();
        }
        tesselator.end();
        RenderSystem.enableCull();
    }

    private static void spawnSparks(float cx, float cy, int color, int count) {
        for (int i = 0; i < count; i++) {
            Spark s = new Spark();
            s.x = cx + (RANDOM.nextFloat() - 0.5F) * 6F;
            s.y = cy + (RANDOM.nextFloat() - 0.5F) * 6F;
            s.vx = (RANDOM.nextFloat() - 0.5F) * 0.8F;
            s.vy = -RANDOM.nextFloat() * 1.2F - 0.2F;
            s.age = 0;
            s.maxAge = 10 + RANDOM.nextInt(15);
            s.color = color;
            SPARKS.add(s);
        }
    }

    private static void drawCircularGauge(GuiGraphics graphics, Font font, float cx, float cy, Metal metal, float reserve, boolean active, boolean flaring, boolean isSelected, boolean isAllomancy, MetalArtsData data) {
        float r_out = 9.5F;
        float r_in = 7.0F;

        // Base color system
        int metalColor = getMetalColor(metal);
        int border = isSelected ? 0xFFFFFFFF : 0xFF3B4655;

        // Soft pulsating active glow halo
        long time = System.currentTimeMillis();
        if (active) {
            float pulseScale = isAllomancy && flaring ? 0.015F : 0.005F;
            float haloAlpha = 0.22F + 0.14F * (float) Math.sin(time * pulseScale);
            int haloColor = (int)(haloAlpha * 255) << 24 | (metalColor & 0xFFFFFF);
            float haloRadius = isAllomancy && flaring ? 13.5F : 11.5F;
            drawDisc(graphics, cx, cy, haloRadius, haloColor);

            // Spawn sparks
            if (isAllomancy && flaring && RANDOM.nextFloat() < 0.18F) {
                spawnSparks(cx, cy, metalColor | 0xFF000000, 2);
            } else if (!isAllomancy && data.feruchemyMode(metal) != 0 && RANDOM.nextFloat() < 0.04F * Math.abs(data.feruchemyMode(metal))) {
                spawnSparks(cx, cy, metalColor | 0xFF000000, 1);
            }
        }

        // Draw solid dark backdrop
        drawDisc(graphics, cx, cy, 10.0F, 0xCC0D121B);

        // Draw selected ring outline if selected
        if (isSelected) {
            drawRingSegment(graphics, cx, cy, 9.7F, 10.0F, 0F, (float)(2 * Math.PI), border);
        } else {
            drawRingSegment(graphics, cx, cy, 9.7F, 10.0F, 0F, (float)(2 * Math.PI), 0x554A5666);
        }

        // Draw progress arc
        float fillPct = 0F;
        if (isAllomancy) {
            float cap = Math.max(1F, ServerConfig.reserveCapacity(metal));
            fillPct = Math.min(1.0F, reserve / cap);
        } else {
            // Default Feruchemical capacity representation
            fillPct = Math.min(1.0F, reserve / 5000F);
        }

        if (fillPct > 0F) {
            float startAngle = -(float)Math.PI / 2F; // Top of the circle
            float endAngle = startAngle + (float)(2 * Math.PI * fillPct);

            int arcColor = metalColor | 0xFF000000;
            if (isAllomancy && flaring) {
                arcColor = 0xFFFFFFFF; // Flaring is pure intense white-glow
            } else if (!isAllomancy) {
                int mode = data.feruchemyMode(metal);
                if (mode < 0) {
                    arcColor = 0xFF7CA1C4; // Storing is ice blue
                } else if (mode > 0) {
                    arcColor = 0xFFFFA044; // Tapping is burning orange
                }
            }
            drawRingSegment(graphics, cx, cy, r_in, r_out, startAngle, endAngle, arcColor);
        }

        // Draw icon centered inside the disc
        String prefix = isAllomancy ? "icon_" : "icon_feru_";
        ResourceLocation icon = new ResourceLocation(com.not_noah.mistborn_metal_arts.MistbornMetalArts.MOD_ID, "textures/gui/" + prefix + metal.id() + ".png");
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, active ? 1.0F : 0.45F);
        graphics.blit(icon, (int) cx - 6, (int) cy - 6, 0, 0, 12, 12, 12, 12);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw little numerical indicators if flaring/tapping
        if (!isAllomancy && data.feruchemyMode(metal) != 0) {
            int modeVal = data.feruchemyMode(metal);
            String txt = modeVal < 0 ? "S" : "x" + modeVal;
            int txtColor = modeVal < 0 ? 0xFF99BFE6 : 0xFFFFB366;
            graphics.pose().pushPose();
            graphics.pose().translate(cx + 6, cy + 2, 100);
            graphics.pose().scale(0.55F, 0.55F, 0.55F);
            graphics.drawString(font, txt, 0, 0, txtColor, true);
            graphics.pose().popPose();
        } else if (isAllomancy && flaring) {
            graphics.pose().pushPose();
            graphics.pose().translate(cx + 6, cy + 2, 100);
            graphics.pose().scale(0.55F, 0.55F, 0.55F);
            graphics.drawString(font, "F", 0, 0, 0xFFFFFFFF, true);
            graphics.pose().popPose();
        }
    }

    private static void drawDisc(GuiGraphics graphics, float centerX, float centerY, float radius, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        
        var matrix = graphics.pose().last().pose();
        bufferBuilder.vertex(matrix, centerX, centerY, 0).color(r, g, b, a).endVertex();
        
        int segments = 24;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            bufferBuilder.vertex(matrix, centerX + (float) Math.cos(angle) * radius, centerY + (float) Math.sin(angle) * radius, 0).color(r, g, b, a).endVertex();
        }
        tesselator.end();
    }

    private static void drawRingSegment(GuiGraphics graphics, float centerX, float centerY, float innerRadius, float outerRadius, float startAngleRad, float endAngleRad, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        
        var matrix = graphics.pose().last().pose();
        int segments = (int) Math.max(8, Math.abs(endAngleRad - startAngleRad) * 16);
        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;
            float angle = startAngleRad + t * (endAngleRad - startAngleRad);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            bufferBuilder.vertex(matrix, centerX + innerRadius * cos, centerY + innerRadius * sin, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, centerX + outerRadius * cos, centerY + outerRadius * sin, 0).color(r, g, b, a).endVertex();
        }
        tesselator.end();
    }

    private static int getMetalColor(Metal metal) {
        return switch (metal) {
            case IRON -> 0x5F758A;
            case STEEL -> 0x96C4EC;
            case TIN -> 0xD6C8A1;
            case PEWTER -> 0xAC8BA4;
            case ZINC -> 0xD48B6A;
            case BRASS -> 0xEAC675;
            case COPPER -> 0xC88261;
            case BRONZE -> 0x997A5C;
            case DURALUMIN -> 0xE6E6FA;
            case ALUMINUM -> 0xC0D6E4;
            case GOLD -> 0xE6C845;
            case ELECTRUM -> 0xE6E6AF;
            case CHROMIUM -> 0x5F9EA0;
            case NICROSIL -> 0x4682B4;
            case CADMIUM -> 0x66CDAA;
            case BENDALLOY -> 0xFFA500;
            case ATIUM -> 0x5B6B7C;
            case LERASIUM -> 0x8DF2FF;
            case TRELLIUM -> 0x4A0E3B;
            case RAYSIUM -> 0xFFD700;
            case TANAVASTIUM -> 0x00CED1;
            default -> 0xCCCCCC;
        };
    }
}
