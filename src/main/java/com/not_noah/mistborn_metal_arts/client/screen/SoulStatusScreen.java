package com.not_noah.mistborn_metal_arts.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.client.ClientMetalArtsData;
import com.not_noah.mistborn_metal_arts.client.keybind.MetalArtsKeyMappings;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SoulStatusScreen extends Screen {
    
    private float backgroundFade = 0.0F;

    private static final String[] PREFERRED_SLOTS = {
        "physical_quadrant", "mental_quadrant", "spiritual_quadrant", "temporal_quadrant",
        "head", "necklace", "back", "body", "belt", "ring", "hands", "bracelet", "charm"
    };

    public SoulStatusScreen() {
        super(Component.literal("Spiritual Status Diagnostic"));
    }

    @Override
    protected void init() {
        super.init();
        backgroundFade = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        backgroundFade = Math.min(1.0F, backgroundFade + 0.12F);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render beautiful premium slate-blue vignette screen backdrop
        int overlayColor = (int)(backgroundFade * 0xE5) << 24 | 0x06090F;
        graphics.fill(0, 0, width, height, overlayColor);

        MetalArtsData data = ClientMetalArtsData.data();
        Font font = Minecraft.getInstance().font;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        long time = System.currentTimeMillis();
        float cx = width / 2.0F;
        float cy = height / 2.0F;

        // ═══════════════════════════════════════════════
        // ── 1. CENTERPIECE: THE GREAT CONCENTRIC MANDALA ──
        // ═══════════════════════════════════════════════
        float stability = data.soulStability();
        float contamination = data.identityContamination();
        float bloat = data.spiritualBloat();

        int stabilityColor = stability < 30.0F ? (time % 500 < 250 ? 0xFFFF3D00 : 0xFFFF8A65) : 0xFF00E5FF;
        int contaminationColor = 0xFF7C4DFF;
        int bloatColor = 0xFFFF4081;

        // Draw Mandala backdrop plates
        drawDisc(graphics, cx, cy, 60.0F, 0xDD090D14);
        drawRingSegment(graphics, cx, cy, 59.2F, 60.0F, 0F, (float)(2 * Math.PI), 0x33FFFFFF);

        // Outer Stability Ring (radius 48.0 to 55.0)
        drawRingSegment(graphics, cx, cy, 48.0F, 55.0F, 0F, (float)(2 * Math.PI), 0x11FFFFFF);
        float stabPct = Math.max(0.0F, Math.min(1.0F, stability / 100.0F));
        if (stabPct > 0F) {
            float startAngle = -(float)Math.PI / 2F;
            float endAngle = startAngle + (float)(2 * Math.PI * stabPct);
            drawRingSegment(graphics, cx, cy, 48.0F, 55.0F, startAngle, endAngle, stabilityColor);
        }

        // Middle Contamination Ring (radius 37.0 to 44.0)
        drawRingSegment(graphics, cx, cy, 37.0F, 44.0F, 0F, (float)(2 * Math.PI), 0x11FFFFFF);
        float contPct = Math.max(0.0F, Math.min(1.0F, contamination / 100.0F));
        if (contPct > 0F) {
            float startAngle = -(float)Math.PI / 2F;
            float endAngle = startAngle + (float)(2 * Math.PI * contPct);
            drawRingSegment(graphics, cx, cy, 37.0F, 44.0F, startAngle, endAngle, contaminationColor);
        }

        // Inner Bloat Ring (radius 26.0 to 33.0)
        drawRingSegment(graphics, cx, cy, 26.0F, 33.0F, 0F, (float)(2 * Math.PI), 0x11FFFFFF);
        float bloatPct = Math.max(0.0F, Math.min(1.0F, bloat / 100.0F));
        if (bloatPct > 0F) {
            float startAngle = -(float)Math.PI / 2F;
            float endAngle = startAngle + (float)(2 * Math.PI * bloatPct);
            drawRingSegment(graphics, cx, cy, 26.0F, 33.0F, startAngle, endAngle, bloatColor);
        }

        // Central Spiritual Core (radius 23.0 to pulsing center)
        drawDisc(graphics, cx, cy, 23.0F, 0xEE05070A);
        float pulse = 0.35F + 0.18F * (float) Math.sin(time * 0.004F);
        int glowColor = (int)(pulse * 255) << 24 | (stabilityColor & 0xFFFFFF);
        drawDisc(graphics, cx, cy, 13.0F, glowColor);

        // Centered HUD details text inside the Mandala
        String titleStr = "SPIRIT";
        String stabStr = Math.round(stability) + "%";
        graphics.drawCenteredString(font, titleStr, (int)cx, (int)cy - 12, 0xFF8090A0);
        graphics.drawCenteredString(font, stabStr, (int)cx, (int)cy + 2, stabilityColor);

        // ═══════════════════════════════════════════════
        // ── 2. LEFT PANEL: HEMALURGIC QUADRANTS & SPIKES ──
        // ═══════════════════════════════════════════════
        int leftX = 20;
        int startY = 32;
        graphics.drawString(font, "SPIRITWEB BINDING POINTS", leftX, startY, 0xFFCFD8DC, true);
        graphics.fill(leftX, startY + 11, leftX + 130, startY + 12, 0x44FFFFFF);
        
        List<TempSpike> allSpikes = new ArrayList<>();
        // Installed permanent spikes
        List<MetalArtsData.InstalledSpike> installed = data.installedSpikes();
        for (int i = 0; i < installed.size(); i++) {
            MetalArtsData.InstalledSpike spike = installed.get(i);
            boolean isLinchpin = data.hasLinchpinSpike() && data.linchpinSpikeIndex() == i;
            allSpikes.add(new TempSpike(spike.spikeMetal(), spike.powerType(), spike.powerMetal(), spike.strength(), true, isLinchpin, ""));
        }
        // Equipped Curio spikes
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            for (String slotType : PREFERRED_SLOTS) {
                Optional<ICurioStacksHandler> stacksHandler = handler.getStacksHandler(slotType);
                if (stacksHandler.isEmpty()) continue;
                IItemHandlerModifiable stacks = stacksHandler.get().getStacks();
                for (int j = 0; j < stacks.getSlots(); j++) {
                    ItemStack stack = stacks.getStackInSlot(j);
                    if (stack.getItem() instanceof HemalurgicSpikeItem spike && spike.charged()) {
                        CompoundTag tag = stack.getOrCreateTag();
                        String powerType = tag.getString("PowerType");
                        if (powerType.isBlank()) {
                            powerType = spike.metal().isFeruchemical() ? "feruchemy" : "allomancy";
                        }
                        Metal powerMetal = Metal.byName(tag.getString("PowerMetal")).orElse(spike.metal());
                        float strength = tag.contains("Strength") ? tag.getFloat("Strength") : 1.0f;
                        allSpikes.add(new TempSpike(spike.metal(), powerType, powerMetal, strength, false, false, slotType));
                    }
                }
            }
        });

        // Split Spikes into classical Quadrants
        List<TempSpike> physical = new ArrayList<>();
        List<TempSpike> mental = new ArrayList<>();
        List<TempSpike> spiritual = new ArrayList<>();
        List<TempSpike> temporal = new ArrayList<>();

        for (TempSpike ts : allSpikes) {
            Metal m = ts.spikeMetal;
            if (m == Metal.IRON || m == Metal.STEEL || m == Metal.TIN || m == Metal.PEWTER) {
                physical.add(ts);
            } else if (m == Metal.ZINC || m == Metal.BRASS || m == Metal.COPPER || m == Metal.BRONZE) {
                mental.add(ts);
            } else if (m == Metal.GOLD || m == Metal.ELECTRUM || m == Metal.CHROMIUM || m == Metal.NICROSIL) {
                spiritual.add(ts);
            } else {
                temporal.add(ts);
            }
        }

        int currentY = startY + 20;

        // Draw Quadrant details
        currentY = drawQuadrantSection(graphics, font, "PHYSICAL QUADRANT", physical, leftX, currentY);
        currentY = drawQuadrantSection(graphics, font, "MENTAL QUADRANT", mental, leftX, currentY + 6);
        currentY = drawQuadrantSection(graphics, font, "SPIRITUAL QUADRANT", spiritual, leftX, currentY + 6);
        currentY = drawQuadrantSection(graphics, font, "TEMPORAL QUADRANT", temporal, leftX, currentY + 6);

        // ═══════════════════════════════════════════════
        // ── 3. RIGHT PANEL: SAVANTISM & SYSTEM STATUS ──
        // ═══════════════════════════════════════════════
        int rightX = width - 150;
        graphics.drawString(font, "SPIRITUAL ANOMALIES", rightX, startY, 0xFFCFD8DC, true);
        graphics.fill(rightX, startY + 11, rightX + 130, startY + 12, 0x44FFFFFF);

        int savantY = startY + 20;
        int activeSavantsCount = 0;

        for (Metal metal : Metal.cachedValues()) {
            float progress = data.savantProgress(metal);
            if (progress > 0.0F) {
                activeSavantsCount++;
                int stage = 1;
                if (progress >= 0.95F) stage = 4;
                else if (progress >= 0.75F) stage = 3;
                else if (progress >= 0.50F) stage = 2;

                int pct = Math.round(progress * 100.0F);
                
                // Draw metal title
                graphics.drawString(font, metal.displayName() + " Savant", rightX, savantY, 0xFFFFD54F, true);
                
                // Draw Stage label
                String stageText = "Stage " + stage + " (" + pct + "%)";
                graphics.drawString(font, stageText, rightX + 130 - font.width(stageText), savantY, 0xFFB0BEC5, false);

                // Draw tiny custom progress bar
                savantY += 10;
                graphics.fill(rightX, savantY, rightX + 130, savantY + 3, 0x22FFFFFF);
                int barWidth = (int)(130.0F * progress);
                graphics.fill(rightX, savantY, rightX + barWidth, savantY + 3, 0xFFFFB300);

                // Draw active warnings / cravings
                savantY += 5;
                if (data.isBurning(metal)) {
                    graphics.drawString(font, "• Stably burning (Active)", rightX + 4, savantY, 0xFF81C784, false);
                    savantY += 9;
                } else {
                    // Check withdrawal stages
                    long lastBurn = data.savantLastBurned(metal);
                    long timeSince = player.level().getGameTime() - lastBurn;
                    if (timeSince > 6000L) { // >5 minutes
                        int withdrawalStage = 1;
                        if (timeSince > 24000L) withdrawalStage = 4; // >20 min
                        else if (timeSince > 18000L) withdrawalStage = 3; // >15 min
                        else if (timeSince > 12000L) withdrawalStage = 2; // >10 min
                        
                        String alert = "• Craving: withdrawal III";
                        if (withdrawalStage == 1) alert = "• Urge: withdrawal I";
                        else if (withdrawalStage == 2) alert = "• urge: withdrawal II";
                        else if (withdrawalStage == 4) alert = "• CRITICAL: withdrawal IV";

                        graphics.drawString(font, alert, rightX + 4, savantY, 0xFFE57373, false);
                        savantY += 9;
                    } else {
                        graphics.drawString(font, "• Stored cravings (Resting)", rightX + 4, savantY, 0xFF90A4AE, false);
                        savantY += 9;
                    }
                }
                savantY += 3;
            }
        }

        if (activeSavantsCount == 0) {
            graphics.drawString(font, "No Savant anomalies", rightX + 6, savantY, 0xFF90A4AE, false);
            savantY += 12;
            graphics.drawString(font, "detected in spiritweb.", rightX + 6, savantY, 0xFF90A4AE, false);
        }

        // Draw general metrics summary under anomalies
        savantY += 10;
        graphics.drawString(font, "SOUL COMPOSITION", rightX, savantY, 0xFFCFD8DC, true);
        graphics.fill(rightX, savantY + 11, rightX + 130, savantY + 12, 0x44FFFFFF);
        savantY += 18;

        // Max Stability (dynamic maximum stability ceiling based on spiritual scarring)
        float baseMax = ServerConfig.VALUES.soulStabilityBaseMax.get().floatValue();
        float maxStability = baseMax - data.spiritualScarring();
        graphics.drawString(font, "Max Stability:", rightX + 4, savantY, 0xFFB0BEC5, false);
        String maxStabStr = Math.round(maxStability) + "%";
        graphics.drawString(font, maxStabStr, rightX + 130 - font.width(maxStabStr), savantY, 0xFF00E5FF, false);
        savantY += 10;

        // Spiritual Scarring
        graphics.drawString(font, "Spiritual Scars:", rightX + 4, savantY, 0xFFB0BEC5, false);
        String scarsStr = Math.round(data.spiritualScarring()) + "%";
        graphics.drawString(font, scarsStr, rightX + 130 - font.width(scarsStr), savantY, 0xFFE57373, false);
        savantY += 10;

        // Contamination details
        graphics.drawString(font, "Identity Floor:", rightX + 4, savantY, 0xFFB0BEC5, false);
        int totalSpikesCount = allSpikes.size();
        int floorVal = Math.round(totalSpikesCount * (float) ServerConfig.VALUES.contaminationPerSpike.get().doubleValue());
        graphics.drawString(font, floorVal + "%", rightX + 130 - font.width(floorVal + "%"), savantY, 0xFFB388FF, false);
        savantY += 10;

        // Bloat details
        graphics.drawString(font, "Forced Snaps:", rightX + 4, savantY, 0xFFB0BEC5, false);
        graphics.drawString(font, data.forcedSystemCount() + "", rightX + 130 - font.width(data.forcedSystemCount() + ""), savantY, 0xFFFF80AB, false);
    }

    private int drawQuadrantSection(GuiGraphics graphics, Font font, String title, List<TempSpike> list, int x, int y) {
        graphics.drawString(font, title, x + 2, y, 0xFF90A4AE, false);
        y += 10;
        if (list.isEmpty()) {
            graphics.drawString(font, "- No bindings active", x + 8, y, 0x55FFFFFF, false);
            y += 10;
        } else {
            for (TempSpike ts : list) {
                String prefix = ts.isLinchpin ? "[L] " : (ts.isAltar ? "[A] " : "[C] ");
                String powerShort = ts.powerType.startsWith("allo") ? "Allo" : "Feru";
                String name = prefix + ts.spikeMetal.displayName() + " (" + powerShort + ")";
                int color = ts.isLinchpin ? 0xFFFFB300 : (ts.isAltar ? 0xFFFF8A65 : 0xFF81C784);

                graphics.drawString(font, name, x + 8, y, color, false);
                
                String chargeStr = Math.round(ts.strength * 100.0F) + "%";
                graphics.drawString(font, chargeStr, x + 130 - font.width(chargeStr), y, 0xFFCFD8DC, false);
                y += 10;
            }
        }
        return y;
    }

    private String uppercaseFirst(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Escape closes the screen
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (MetalArtsKeyMappings.OPEN_SOUL_STATUS.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Helper rendering geometry from MetalArtsHudOverlay
    private static void drawDisc(GuiGraphics graphics, float centerX, float centerY, float radius, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
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
        
        int segments = 32;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            bufferBuilder.vertex(matrix, centerX + (float) Math.cos(angle) * radius, centerY + (float) Math.sin(angle) * radius, 0).color(r, g, b, a).endVertex();
        }
        tesselator.end();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    private static void drawRingSegment(GuiGraphics graphics, float centerX, float centerY, float innerRadius, float outerRadius, float startAngleRad, float endAngleRad, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        
        var matrix = graphics.pose().last().pose();
        int segments = (int) Math.max(16, Math.abs(endAngleRad - startAngleRad) * 20);
        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;
            float angle = startAngleRad + t * (endAngleRad - startAngleRad);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            bufferBuilder.vertex(matrix, centerX + innerRadius * cos, centerY + innerRadius * sin, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, centerX + outerRadius * cos, centerY + outerRadius * sin, 0).color(r, g, b, a).endVertex();
        }
        tesselator.end();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    private static class TempSpike {
        final Metal spikeMetal;
        final String powerType;
        final Metal powerMetal;
        final float strength;
        final boolean isAltar;
        final boolean isLinchpin;
        final String curiosSlot;

        TempSpike(Metal spikeMetal, String powerType, Metal powerMetal, float strength, boolean isAltar, boolean isLinchpin, String curiosSlot) {
            this.spikeMetal = spikeMetal;
            this.powerType = powerType;
            this.powerMetal = powerMetal;
            this.strength = strength;
            this.isAltar = isAltar;
            this.isLinchpin = isLinchpin;
            this.curiosSlot = curiosSlot;
        }
    }
}
