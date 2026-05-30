package com.not_noah.mistborn_metal_arts.client.screen;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.client.ClientMetalArtsData;
import com.not_noah.mistborn_metal_arts.client.keybind.MetalArtsKeyMappings;
import com.not_noah.mistborn_metal_arts.network.MetalAction;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import com.not_noah.mistborn_metal_arts.network.ServerboundMetalActionPacket;
import com.not_noah.mistborn_metal_arts.network.ServerboundSetFeruchemyModePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MetalArtsRadialScreen extends Screen {
    // Official Allomancy Outer Ring (External Metals)
    private static final Metal[] ALLOMANCY_OUTER = {
        Metal.IRON,      // Sector 0 (Top-Left Pull): -135 to -90
        Metal.ZINC,      // Sector 1 (Top-Right Pull): -90 to -45
        Metal.BRASS,     // Sector 2 (Top-Right Push): -45 to 0
        Metal.CADMIUM,   // Sector 3 (Bottom-Right Pull): 0 to 45
        Metal.BENDALLOY, // Sector 4 (Bottom-Right Push): 45 to 90
        Metal.NICROSIL,  // Sector 5 (Bottom-Left Push): 90 to 135
        Metal.CHROMIUM,  // Sector 6 (Bottom-Left Pull): 135 to 180
        Metal.STEEL      // Sector 7 (Top-Left Push): -180 to -135
    };

    // Official Allomancy Inner Ring (Internal Metals)
    private static final Metal[] ALLOMANCY_INNER = {
        Metal.TIN,       // Sector 0
        Metal.COPPER,    // Sector 1
        Metal.BRONZE,    // Sector 2
        Metal.GOLD,      // Sector 3
        Metal.ELECTRUM,  // Sector 4
        Metal.DURALUMIN, // Sector 5
        Metal.ALUMINUM,  // Sector 6
        Metal.PEWTER     // Sector 7
    };

    // Official Feruchemy Rotated Diamond Grid Mapping
    private static final Metal[][] GRID_METALS = {
        { Metal.IRON,      Metal.TIN,      Metal.CHROMIUM, Metal.GOLD },
        { Metal.STEEL,     Metal.PEWTER,   Metal.NICROSIL, Metal.ELECTRUM },
        { Metal.ZINC,      Metal.COPPER,   Metal.CADMIUM,  Metal.ALUMINUM },
        { Metal.BRASS,     Metal.BRONZE,   Metal.BENDALLOY,Metal.DURALUMIN }
    };

    private boolean feruchemyTab = false;
    private final Map<Metal, Float> metalAnimations = new HashMap<>();
    private float backgroundFade = 0.0F;

    // GUI coordinate tracking for key-release selection
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    public MetalArtsRadialScreen() {
        super(Component.translatable("screen.mistborn_metal_arts.metal_arts"));
    }

    @Override
    protected void init() {
        MetalArtsData data = ClientMetalArtsData.data();
        boolean hasAllo = false;
        for (Metal m : Metal.cachedValues()) {
            if (data.allomanticPowersRaw().contains(m)) {
                hasAllo = true;
                break;
            }
        }
        boolean hasFeru = false;
        for (Metal m : Metal.cachedValues()) {
            if (data.hasFeruchemicalPower(m)) {
                hasFeru = true;
                break;
            }
        }
        if (hasAllo && !hasFeru) {
            feruchemyTab = false;
        } else if (!hasAllo && hasFeru) {
            feruchemyTab = true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        backgroundFade = Math.min(1.0F, backgroundFade + 0.15F);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        // Dark premium deep slate blue vignette overlay
        int overlayColor = (int)(backgroundFade * 0xDA) << 24 | 0x060A12;
        graphics.fill(0, 0, width, height, overlayColor);

        float cx = width / 2.0F;
        float cy = height / 2.0F;

        // Draw tab headers
        drawTabHeaders(graphics);

        // Identify hovered metal
        Metal hoveredMetal = feruchemyTab ? getHoveredFeruchemy(mouseX, mouseY, cx, cy) : getHoveredAllomancy(mouseX, mouseY, cx, cy);

        // Update interpolation animation scales for premium responsive transitions
        for (Metal m : Metal.cachedValues()) {
            float current = metalAnimations.getOrDefault(m, 0.0F);
            if (m == hoveredMetal) {
                metalAnimations.put(m, Math.min(1.0F, current + 0.16F));
            } else {
                metalAnimations.put(m, Math.max(0.0F, current - 0.16F));
            }
        }

        MetalArtsData data = ClientMetalArtsData.data();

        if (!feruchemyTab) {
            // ALLOMANCY CONCENTRIC CIRCLE RENDERING
            for (int i = 0; i < 8; i++) {
                float startAngle = -3.0F * (float)Math.PI / 4.0F + i * ((float)Math.PI / 4.0F);
                float endAngle = startAngle + ((float)Math.PI / 4.0F);

                // --- 1. Inner Ring (Internal Metals) ---
                Metal innerMetal = ALLOMANCY_INNER[i];
                if (data.allomanticPowersRaw().contains(innerMetal)) {
                    float innerAnim = metalAnimations.getOrDefault(innerMetal, 0.0F);
                    float innerRin = 35.0F + innerAnim * 3.0F;
                    float innerRout = 75.0F + innerAnim * 5.0F;
                    int innerColor = getDynamicColor(innerMetal, data, innerAnim, false);

                    drawSlice(graphics, cx, cy, innerRin, innerRout, startAngle, endAngle, innerColor);
                    
                    int borderColor = 0x558090A0;
                    int sliceColor = 0x1A8090A0;
                    if (data.isBurning(innerMetal)) {
                        borderColor = data.isFlaring(innerMetal) ? 0xFFFFFFFF : 0xFF358AE6;
                        sliceColor = data.isFlaring(innerMetal) ? 0x88FFFFFF : 0x88358AE6;
                    } else if (innerAnim > 0) {
                        int alphaBorder = 0x55 + (int)(innerAnim * 0x55);
                        int alphaSlice = 0x1A + (int)(innerAnim * 0x3B);
                        borderColor = alphaBorder << 24 | 0x7095B5;
                        sliceColor = alphaSlice << 24 | 0x7095B5;
                    }

                    drawRingSegment(graphics, cx, cy, innerRin, innerRin + 1.2F, startAngle, endAngle, innerMetal == hoveredMetal || data.isBurning(innerMetal) ? borderColor : 0xFF3E546C);
                    drawRingSegment(graphics, cx, cy, innerRout - 1.2F, innerRout, startAngle, endAngle, innerMetal == hoveredMetal || data.isBurning(innerMetal) ? borderColor : 0x558090A0);
                    drawSliceBorder(graphics, cx, cy, innerRin, innerRout, startAngle, sliceColor);
                    drawSliceBorder(graphics, cx, cy, innerRin, innerRout, endAngle, sliceColor);

                    // Render Inner Metal Icon
                    float innerRIcon = innerRin + (innerRout - innerRin) * 0.52F;
                    float midAngle = (startAngle + endAngle) / 2.0F;
                    float innerX = cx + innerRIcon * (float)Math.cos(midAngle);
                    float innerY = cy + innerRIcon * (float)Math.sin(midAngle);
                    drawMetalIcon(graphics, innerMetal, innerX, innerY, data, false);
                }

                // --- 2. Outer Ring (External Metals) ---
                Metal outerMetal = ALLOMANCY_OUTER[i];
                if (data.allomanticPowersRaw().contains(outerMetal)) {
                    float outerAnim = metalAnimations.getOrDefault(outerMetal, 0.0F);
                    float outerRin = 75.0F + outerAnim * 4.0F;
                    float outerRout = 115.0F + outerAnim * 8.0F;
                    int outerColor = getDynamicColor(outerMetal, data, outerAnim, false);

                    drawSlice(graphics, cx, cy, outerRin, outerRout, startAngle, endAngle, outerColor);
                    
                    int borderColor = 0x558090A0;
                    int sliceColor = 0x1A8090A0;
                    if (data.isBurning(outerMetal)) {
                        borderColor = data.isFlaring(outerMetal) ? 0xFFFFFFFF : 0xFF358AE6;
                        sliceColor = data.isFlaring(outerMetal) ? 0x88FFFFFF : 0x88358AE6;
                    } else if (outerAnim > 0) {
                        int alphaBorder = 0x55 + (int)(outerAnim * 0x55);
                        int alphaSlice = 0x1A + (int)(outerAnim * 0x3B);
                        borderColor = alphaBorder << 24 | 0x7095B5;
                        sliceColor = alphaSlice << 24 | 0x7095B5;
                    }

                    drawRingSegment(graphics, cx, cy, outerRin, outerRin + 1.2F, startAngle, endAngle, outerMetal == hoveredMetal || data.isBurning(outerMetal) ? borderColor : 0x558090A0);
                    drawRingSegment(graphics, cx, cy, outerRout - 1.2F, outerRout, startAngle, endAngle, outerMetal == hoveredMetal || data.isBurning(outerMetal) ? borderColor : 0xFF3E546C);
                    drawSliceBorder(graphics, cx, cy, outerRin, outerRout, startAngle, sliceColor);
                    drawSliceBorder(graphics, cx, cy, outerRin, outerRout, endAngle, sliceColor);

                    // Render Outer Metal Icon
                    float outerRIcon = outerRin + (outerRout - outerRin) * 0.55F;
                    float midAngle = (startAngle + endAngle) / 2.0F;
                    float outerX = cx + outerRIcon * (float)Math.cos(midAngle);
                    float outerY = cy + outerRIcon * (float)Math.sin(midAngle);
                    drawMetalIcon(graphics, outerMetal, outerX, outerY, data, false);
                }
            }

            // Draw central deadzone circle with a steel border
            drawDisc(graphics, cx, cy, 33.0F, 0xEE080C14);
            drawRingSegment(graphics, cx, cy, 33.0F, 34.5F, 0F, (float)(2 * Math.PI), 0xFF3E546C);

            // --- 3. God Metals (Atium, Lerasium, Trellium, Raysium, Tanavastium) Medallions ---
            float godMetalY = Math.min(cy + 135.0F, height - 25.0F);
            if (data.allomanticPowersRaw().contains(Metal.ATIUM)) {
                renderGodMedallion(graphics, Metal.ATIUM, cx - 72.0F, godMetalY, data);
            }
            if (data.allomanticPowersRaw().contains(Metal.LERASIUM)) {
                renderGodMedallion(graphics, Metal.LERASIUM, cx - 36.0F, godMetalY, data);
            }
            if (data.allomanticPowersRaw().contains(Metal.TRELLIUM)) {
                renderGodMedallion(graphics, Metal.TRELLIUM, cx, godMetalY, data);
            }
            if (data.allomanticPowersRaw().contains(Metal.RAYSIUM)) {
                renderGodMedallion(graphics, Metal.RAYSIUM, cx + 36.0F, godMetalY, data);
            }
            if (data.allomanticPowersRaw().contains(Metal.TANAVASTIUM)) {
                renderGodMedallion(graphics, Metal.TANAVASTIUM, cx + 72.0F, godMetalY, data);
            }

        } else {
            // FERUCHEMY RUSTIC DIAMOND GRID RENDERING
            float U = 32.0F;
            float W_render = 0.7071F * U - 1.5F;

            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 4; y++) {
                    Metal metal = GRID_METALS[x][y];
                    if (data.hasFeruchemicalPower(metal)) {
                        float anim = metalAnimations.getOrDefault(metal, 0.0F);

                        float cellX = cx + (x - y) * 0.7071F * U;
                        float cellY = cy + (x + y - 3.0F) * 0.7071F * U;

                        float currentW = W_render + anim * 2.0F;
                        int cellColor = getDynamicColor(metal, data, anim, true);

                        // Draw individual cell structural shadow and border first
                        drawDiamond(graphics, cellX, cellY, currentW + 2.0F, 0x5D04070B);
                        drawDiamondBorder(graphics, cellX, cellY, currentW + 2.0F, 0xFF2A3644);

                        // Draw the diamond tile
                        drawDiamond(graphics, cellX, cellY, currentW, cellColor);

                        // Glowing dynamic border based on active mode (storing = blue, tapping = orange/yellow)
                        int borderColor = 0x2E4A5E78;
                        int mode = data.feruchemyMode(metal);
                        if (mode < 0) {
                            borderColor = 0xFF358AE6; // Storing cyan glow
                        } else if (mode > 0) {
                            borderColor = 0xFFFFA033; // Tapping gold glow
                        } else if (metal == hoveredMetal) {
                            borderColor = 0xAA7095B5; // Unlocked hovered highlight
                        } else {
                            borderColor = 0x66405266; // Unlocked idle border
                        }
                        drawDiamondBorder(graphics, cellX, cellY, currentW, borderColor);

                        // Draw Metal Icon
                        drawMetalIcon(graphics, metal, cellX, cellY, data, true);

                        // Storing/Tapping indicators badges
                        if (data.feruchemyMode(metal) != 0) {
                            int modeVal = data.feruchemyMode(metal);
                            String badge = modeVal < 0 ? "S" : "x" + modeVal;
                            int badgeColor = modeVal < 0 ? 0xFF85B5E6 : 0xFFFF9433;
                            graphics.pose().pushPose();
                            graphics.pose().translate(cellX + 5, cellY + 3, 10);
                            graphics.pose().scale(0.55F, 0.55F, 0.55F);
                            graphics.drawString(font, badge, 0, 0, badgeColor, true);
                            graphics.pose().popPose();
                        }
                    }
                }
            }

            // --- 3. God Metals (Atium, Lerasium, Trellium, Raysium, Tanavastium) Medallions for Feruchemy ---
            float godMetalY = Math.min(cy + 135.0F, height - 25.0F);
            if (data.hasFeruchemicalPower(Metal.ATIUM)) {
                renderGodMedallion(graphics, Metal.ATIUM, cx - 72.0F, godMetalY, data);
            }
            if (data.hasFeruchemicalPower(Metal.LERASIUM)) {
                renderGodMedallion(graphics, Metal.LERASIUM, cx - 36.0F, godMetalY, data);
            }
            if (data.hasFeruchemicalPower(Metal.TRELLIUM)) {
                renderGodMedallion(graphics, Metal.TRELLIUM, cx, godMetalY, data);
            }
            if (data.hasFeruchemicalPower(Metal.RAYSIUM)) {
                renderGodMedallion(graphics, Metal.RAYSIUM, cx + 36.0F, godMetalY, data);
            }
            if (data.hasFeruchemicalPower(Metal.TANAVASTIUM)) {
                renderGodMedallion(graphics, Metal.TANAVASTIUM, cx + 72.0F, godMetalY, data);
            }
        }

        // Draw Central/Side description lore card if a metal is hovered
        if (hoveredMetal != null) {
            float cardX = cx;
            float cardY = cy;
            
            if (feruchemyTab) {
                if (width >= 380) {
                    // Place card on the same side as the mouse!
                    if (mouseX < cx) {
                        cardX = cx - 145.0F;
                    } else {
                        cardX = cx + 145.0F;
                    }
                    // Clamp cardX to stay inside screen safe bounds (5px padding)
                    float halfWidth = 135.0F / 2.0F;
                    cardX = Math.max(halfWidth + 5.0F, Math.min(width - halfWidth - 5.0F, cardX));
                } else {
                    // Extremely tiny screen: place at the top of the screen in a clean banner
                    cardY = 32.0F;
                }
            }
            
            drawLoreCard(graphics, hoveredMetal, cardX, cardY, data);
        } else {
            graphics.pose().pushPose();
            graphics.pose().translate(cx, cy, 0);
            graphics.drawCenteredString(font, feruchemyTab ? "FERUCHEMY DIAGRAM" : "ALLOMANCY DIAGRAM", 0, -12, 0x5D7B9C);
            graphics.drawCenteredString(font, "Hover a metal to burn/tap", 0, 2, 0x3C4F66);
            graphics.pose().popPose();
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderGodMedallion(GuiGraphics graphics, Metal metal, float x, float y, MetalArtsData data) {
        float anim = metalAnimations.getOrDefault(metal, 0.0F);
        float r = 18.0F + anim * 2.0F;

        int discColor = getDynamicColor(metal, data, anim, feruchemyTab);
        drawDisc(graphics, x, y, r, discColor);

        int borderColor;
        if (feruchemyTab) {
            borderColor = data.hasFeruchemicalPower(metal) ? (data.feruchemyMode(metal) != 0 ? 0xFFFFFFFF : 0xFF5D7B9C) : 0x223C4F66;
        } else {
            borderColor = data.allomanticPowersRaw().contains(metal) ? (data.isBurning(metal) ? 0xFFFFFFFF : 0xFF5D7B9C) : 0x223C4F66;
        }
        if (anim > 0) {
            borderColor = getMetalColor(metal) | 0xFF000000;
        }
        drawRingSegment(graphics, x, y, r - 1.5F, r, 0, (float)(2 * Math.PI), borderColor);

        drawMetalIcon(graphics, metal, x, y, data, feruchemyTab);
    }

    private void drawMetalIcon(GuiGraphics graphics, Metal metal, float x, float y, MetalArtsData data, boolean feru) {
        boolean hasPower = feru ? data.hasFeruchemicalPower(metal) : data.allomanticPowersRaw().contains(metal);
        boolean active = feru ? (data.feruchemyMode(metal) != 0) : data.isBurning(metal);

        String prefix = feru ? "icon_feru_" : "icon_";
        ResourceLocation icon = new ResourceLocation("mistborn_metal_arts", "textures/gui/" + prefix + metal.id() + ".png");
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, hasPower ? (active ? 1.0F : 0.65F) : 0.12F);
        graphics.blit(icon, (int) x - 8, (int) y - 8, 0, 0, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private int getDynamicColor(Metal metal, MetalArtsData data, float anim, boolean feru) {
        boolean hasPower = feru ? data.hasFeruchemicalPower(metal) : data.allomanticPowersRaw().contains(metal);
        int baseCol = getMetalColor(metal);

        if (!hasPower) {
            // Locked / unlearned metallic shell (12% opacity)
            return (int)(0x1E + anim * 0x1A) << 24 | (baseCol & 0xFFFFFF);
        }

        boolean active = feru ? (data.feruchemyMode(metal) != 0) : data.isBurning(metal);
        if (active) {
            if (feru) {
                // Tapping (warm gold-orange) or Storing (cool teal-blue)
                int mode = data.feruchemyMode(metal);
                return mode < 0 ? 0xDD28425F : 0xDDE68A30;
            } else {
                // Burning standard blue or Flaring white
                return data.isFlaring(metal) ? 0xFFFFFFFF : 0xDD3B8AE0;
            }
        }

        // Unlocked but idle: dynamic translucent base metal color
        int alpha = (int)(0x48 + anim * 0x62);
        return alpha << 24 | (baseCol & 0xFFFFFF);
    }

    private void drawTabHeaders(GuiGraphics graphics) {
        MetalArtsData data = ClientMetalArtsData.data();
        boolean hasAllo = false;
        for (Metal m : Metal.cachedValues()) {
            if (data.allomanticPowersRaw().contains(m)) {
                hasAllo = true;
                break;
            }
        }
        boolean hasFeru = false;
        for (Metal m : Metal.cachedValues()) {
            if (data.hasFeruchemicalPower(m)) {
                hasFeru = true;
                break;
            }
        }
        if (!hasAllo || !hasFeru) {
            return;
        }

        int tabY = height / 2 - 128;
        int cx = width / 2;

        int alloColor = feruchemyTab ? 0x4D8EA2B5 : 0xFFFFFFFF;
        int feruColor = feruchemyTab ? 0xFFFFFFFF : 0x4D8EA2B5;

        // ALLOMANCY BUTTON
        graphics.drawString(font, "ALLOMANCY", cx - 85, tabY, alloColor, true);
        if (!feruchemyTab) {
            graphics.fill(cx - 87, tabY + 11, cx - 25, tabY + 13, 0xFF3E8AE6);
        }

        // FERUCHEMY BUTTON
        graphics.drawString(font, "FERUCHEMY", cx + 25, tabY, feruColor, true);
        if (feruchemyTab) {
            graphics.fill(cx + 23, tabY + 11, cx + 85, tabY + 13, 0xFFFFA044);
        }

        graphics.drawCenteredString(font, "[TAB] Swap Charts", cx, tabY + 22, 0x3E526B);
    }

    private void drawLoreCard(GuiGraphics graphics, Metal metal, float cx, float cy, MetalArtsData data) {
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);

        // 1. Metal Title
        String name = metal.displayName().toUpperCase();
        int titleColor = getMetalColor(metal);
        graphics.pose().scale(0.92F, 0.92F, 0.92F);
        graphics.drawCenteredString(font, name, 0, -28, titleColor);
        graphics.pose().scale(1.0F/0.92F, 1.0F/0.92F, 1.0F/0.92F);

        // 2. Power Role Status
        boolean hasPower = feruchemyTab ? data.hasFeruchemicalPower(metal) : data.allomanticPowersRaw().contains(metal);
        String roleText = "LOCKED";
        int roleColor = 0xFF5A616A;

        if (hasPower) {
            if (feruchemyTab) {
                roleText = "Ferring: " + getFeruchemicalRole(metal);
                roleColor = 0xFFFFA533;
            } else {
                roleText = "Misting: " + getAllomanticRole(metal);
                roleColor = 0xFF4DA6FF;
            }
        }
        graphics.pose().scale(0.62F, 0.62F, 0.62F);
        graphics.drawCenteredString(font, roleText, 0, -26, roleColor);

        // 3. Status Metric Value
        String status = "Inactive";
        int statusColor = 0xFF8A95A5;
        if (hasPower) {
            if (feruchemyTab) {
                int mode = data.feruchemyMode(metal);
                if (mode < 0) {
                    status = "Storing";
                    statusColor = 0xFF6EA5E6;
                } else if (mode > 0) {
                    status = "Tapping x" + mode;
                    statusColor = 0xFFFF8533;
                } else {
                    status = "Idle [Charge: " + Math.round(data.getMetalmindCharge(metal)) + "]";
                }
            } else {
                if (data.isFlaring(metal)) {
                    status = "Flaring";
                    statusColor = 0xFFFFFFFF;
                } else if (data.isBurning(metal)) {
                    status = "Burning [Res: " + Math.round(data.getReserve(metal)) + "%]";
                    statusColor = 0xFF70D680;
                } else {
                    status = "Idle [Reserve: " + Math.round(data.getReserve(metal)) + "%]";
                }
            }
        }
        graphics.drawCenteredString(font, status, 0, -14, statusColor);
        graphics.pose().scale(1.0F/0.62F, 1.0F/0.62F, 1.0F/0.62F);

        // 4. Wrapped Lore Description
        graphics.pose().scale(0.50F, 0.50F, 0.50F);
        String lore = getMetalLore(metal, feruchemyTab);
        List<Component> lines = wrapLore(lore, 125);
        int ly = 0;
        for (Component line : lines) {
            graphics.drawCenteredString(font, line, 0, ly, 0xFF9EACBC);
            ly += 9;
        }

        graphics.pose().popPose();
    }

    private List<Component> wrapLore(String lore, int maxWidth) {
        List<Component> list = new ArrayList<>();
        String[] words = lore.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (font.width(currentLine + " " + word) < maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                list.add(Component.literal(currentLine.toString()));
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            list.add(Component.literal(currentLine.toString()));
        }
        return list;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tabY = height / 2 - 128;
        int cx = width / 2;

        MetalArtsData data = ClientMetalArtsData.data();
        boolean hasAllo = false;
        for (Metal m : Metal.cachedValues()) {
            if (data.allomanticPowersRaw().contains(m)) {
                hasAllo = true;
                break;
            }
        }
        boolean hasFeru = false;
        for (Metal m : Metal.cachedValues()) {
            if (data.hasFeruchemicalPower(m)) {
                hasFeru = true;
                break;
            }
        }

        // Toggle tabs via header clicks
        if (hasAllo && hasFeru && mouseY >= tabY - 10 && mouseY <= tabY + 18) {
            if (mouseX >= cx - 95 && mouseX <= cx - 15) {
                if (feruchemyTab) {
                    feruchemyTab = false;
                    playClickSound();
                }
                return true;
            } else if (mouseX >= cx + 15 && mouseX <= cx + 95) {
                if (!feruchemyTab) {
                    feruchemyTab = true;
                    playClickSound();
                }
                return true;
            }
        }

        // Mouse click triggers fallback select for absolute user compatibility
        if (button == 0) {
            Metal metal = feruchemyTab ? getHoveredFeruchemy(mouseX, mouseY, cx, height / 2.0F) : getHoveredAllomancy(mouseX, mouseY, cx, height / 2.0F);
            if (metal != null) {
                selectMetal(metal);
                return true;
            } else {
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void selectMetal(Metal metal) {
        ClientMetalArtsData.setLocalSelected(metal);
        // Direct Allomancy toggle on selection
        if (!feruchemyTab) {
            MetalArtsData data = ClientMetalArtsData.data();
            if (data.hasAllomanticPower(metal)) {
                if (data.isBurning(metal)) {
                    if (Screen.hasShiftDown()) {
                        MetalArtsNetwork.sendToServer(new ServerboundMetalActionPacket(MetalAction.TOGGLE_FLARE, metal));
                    } else {
                        MetalArtsNetwork.sendToServer(new ServerboundMetalActionPacket(MetalAction.STOP_BURN, metal));
                    }
                } else {
                    MetalArtsNetwork.sendToServer(new ServerboundMetalActionPacket(MetalAction.START_BURN, metal));
                }
            }
        } else {
            // Selected Feruchemy: sync selected metal to server
            MetalArtsNetwork.sendToServer(new ServerboundMetalActionPacket(MetalAction.SELECT, metal));
        }
        playClickSound();
        onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (feruchemyTab) {
            float cx = width / 2.0F;
            float cy = height / 2.0F;
            Metal metal = getHoveredFeruchemy(mouseX, mouseY, cx, cy);
            if (metal != null) {
                MetalArtsData data = ClientMetalArtsData.data();
                if (metal.isFeruchemical() && data.hasFeruchemicalPower(metal)) {
                    int currentMode = data.feruchemyMode(metal);
                    int nextMode = getNextFeruchemicalMode(currentMode, delta);

                    data.setFeruchemyMode(metal, nextMode);
                    MetalArtsNetwork.sendToServer(new ServerboundSetFeruchemyModePacket(metal, nextMode));

                    playClickSound();
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int getNextFeruchemicalMode(int current, double scrollDelta) {
        int[] sequence = {-1, 0, 1, 2, 4, 8};
        int index = 1; // Default Idle (0)
        for (int i = 0; i < sequence.length; i++) {
            if (sequence[i] == current) {
                index = i;
                break;
            }
        }
        if (scrollDelta > 0) {
            index = Math.min(sequence.length - 1, index + 1);
        } else {
            index = Math.max(0, index - 1);
        }
        return sequence[index];
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // TAB toggles tabs inside GUI
        if (keyCode == 258) { // TAB GLFW
            MetalArtsData data = ClientMetalArtsData.data();
            boolean hasAllo = false;
            for (Metal m : Metal.cachedValues()) {
                if (data.allomanticPowersRaw().contains(m)) {
                    hasAllo = true;
                    break;
                }
            }
            boolean hasFeru = false;
            for (Metal m : Metal.cachedValues()) {
                if (data.hasFeruchemicalPower(m)) {
                    hasFeru = true;
                    break;
                }
            }
            if (hasAllo && hasFeru) {
                feruchemyTab = !feruchemyTab;
                playClickSound();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (MetalArtsKeyMappings.OPEN_MENU.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            selectHoveredMetalAndClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (MetalArtsKeyMappings.OPEN_MENU.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(button))) {
            selectHoveredMetalAndClose();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void selectHoveredMetalAndClose() {
        float cx = width / 2.0F;
        float cy = height / 2.0F;
        Metal metal = feruchemyTab ? getHoveredFeruchemy(lastMouseX, lastMouseY, cx, cy) : getHoveredAllomancy(lastMouseX, lastMouseY, cx, cy);
        if (metal != null) {
            selectMetal(metal);
        } else {
            onClose();
        }
    }

    private Metal getHoveredAllomancy(double mouseX, double mouseY, float cx, float cy) {
        MetalArtsData data = ClientMetalArtsData.data();
        
        float godMetalY = Math.min(cy + 135.0F, height - 25.0F);

        // God Metals centers and radius check
        double dxAtium = mouseX - (cx - 72.0F);
        double dyAtium = mouseY - godMetalY;
        if (dxAtium * dxAtium + dyAtium * dyAtium <= 20.0F * 20.0F && data.allomanticPowersRaw().contains(Metal.ATIUM)) {
            return Metal.ATIUM;
        }

        double dxLerasium = mouseX - (cx - 36.0F);
        double dyLerasium = mouseY - godMetalY;
        if (dxLerasium * dxLerasium + dyLerasium * dyLerasium <= 20.0F * 20.0F && data.allomanticPowersRaw().contains(Metal.LERASIUM)) {
            return Metal.LERASIUM;
        }

        double dxTrellium = mouseX - cx;
        double dyTrellium = mouseY - godMetalY;
        if (dxTrellium * dxTrellium + dyTrellium * dyTrellium <= 20.0F * 20.0F && data.allomanticPowersRaw().contains(Metal.TRELLIUM)) {
            return Metal.TRELLIUM;
        }

        double dxRaysium = mouseX - (cx + 36.0F);
        double dyRaysium = mouseY - godMetalY;
        if (dxRaysium * dxRaysium + dyRaysium * dyRaysium <= 20.0F * 20.0F && data.allomanticPowersRaw().contains(Metal.RAYSIUM)) {
            return Metal.RAYSIUM;
        }

        double dxTanavastium = mouseX - (cx + 72.0F);
        double dyTanavastium = mouseY - godMetalY;
        if (dxTanavastium * dxTanavastium + dyTanavastium * dyTanavastium <= 20.0F * 20.0F && data.allomanticPowersRaw().contains(Metal.TANAVASTIUM)) {
            return Metal.TANAVASTIUM;
        }

        // Concentric wheel math
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 35.0D || dist > 115.0D) {
            return null;
        }

        double angle = Math.atan2(dy, dx);
        int sector = 0;
        if (angle >= -Math.PI && angle < -3 * Math.PI / 4) sector = 7;
        else if (angle >= -3 * Math.PI / 4 && angle < -Math.PI / 2) sector = 0;
        else if (angle >= -Math.PI / 2 && angle < -Math.PI / 4) sector = 1;
        else if (angle >= -Math.PI / 4 && angle < 0) sector = 2;
        else if (angle >= 0 && angle < Math.PI / 4) sector = 3;
        else if (angle >= Math.PI / 4 && angle < Math.PI / 2) sector = 4;
        else if (angle >= Math.PI / 2 && angle < 3 * Math.PI / 4) sector = 5;
        else sector = 6;

        if (dist < 75.0D) {
            Metal inner = ALLOMANCY_INNER[sector];
            return data.allomanticPowersRaw().contains(inner) ? inner : null;
        } else {
            Metal outer = ALLOMANCY_OUTER[sector];
            return data.allomanticPowersRaw().contains(outer) ? outer : null;
        }
    }

    private Metal getHoveredFeruchemy(double mouseX, double mouseY, float cx, float cy) {
        MetalArtsData data = ClientMetalArtsData.data();
        
        float godMetalY = Math.min(cy + 135.0F, height - 25.0F);

        // God Metals centers and radius check for Feruchemy
        double dxAtium = mouseX - (cx - 72.0F);
        double dyAtium = mouseY - godMetalY;
        if (dxAtium * dxAtium + dyAtium * dyAtium <= 20.0F * 20.0F && data.hasFeruchemicalPower(Metal.ATIUM)) {
            return Metal.ATIUM;
        }

        double dxLerasium = mouseX - (cx - 36.0F);
        double dyLerasium = mouseY - godMetalY;
        if (dxLerasium * dxLerasium + dyLerasium * dyLerasium <= 20.0F * 20.0F && data.hasFeruchemicalPower(Metal.LERASIUM)) {
            return Metal.LERASIUM;
        }

        double dxTrellium = mouseX - cx;
        double dyTrellium = mouseY - godMetalY;
        if (dxTrellium * dxTrellium + dyTrellium * dyTrellium <= 20.0F * 20.0F && data.hasFeruchemicalPower(Metal.TRELLIUM)) {
            return Metal.TRELLIUM;
        }

        double dxRaysium = mouseX - (cx + 36.0F);
        double dyRaysium = mouseY - godMetalY;
        if (dxRaysium * dxRaysium + dyRaysium * dyRaysium <= 20.0F * 20.0F && data.hasFeruchemicalPower(Metal.RAYSIUM)) {
            return Metal.RAYSIUM;
        }

        double dxTanavastium = mouseX - (cx + 72.0F);
        double dyTanavastium = mouseY - godMetalY;
        if (dxTanavastium * dxTanavastium + dyTanavastium * dyTanavastium <= 20.0F * 20.0F && data.hasFeruchemicalPower(Metal.TANAVASTIUM)) {
            return Metal.TANAVASTIUM;
        }

        float U = 32.0F;
        float W = 0.7071F * U;
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                float cellX = cx + (x - y) * 0.7071F * U;
                float cellY = cy + (x + y - 3.0F) * 0.7071F * U;
                if (Math.abs(mouseX - cellX) + Math.abs(mouseY - cellY) <= W) {
                    Metal m = GRID_METALS[x][y];
                    return data.hasFeruchemicalPower(m) ? m : null;
                }
            }
        }
        return null;
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F
        ));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- Premium Rendering Geometry Utilities ---

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
        
        int segments = 45;
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
        int segments = 45;
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

    private void drawSlice(GuiGraphics graphics, float cx, float cy, float rin, float rout, float startAngle, float endAngle, int color) {
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
        int segments = 15;
        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;
            float angle = startAngle + t * (endAngle - startAngle);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            bufferBuilder.vertex(matrix, cx + rin * cos, cy + rin * sin, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, cx + rout * cos, cy + rout * sin, 0).color(r, g, b, a).endVertex();
        }
        tesselator.end();
    }

    private void drawSliceBorder(GuiGraphics graphics, float cx, float cy, float rin, float rout, float angle, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        
        var matrix = graphics.pose().last().pose();
        bufferBuilder.vertex(matrix, cx + rin * cos, cy + rin * sin, 0).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, cx + rout * cos, cy + rout * sin, 0).color(r, g, b, a).endVertex();
        
        tesselator.end();
    }

    private static void drawDiamond(GuiGraphics graphics, float cx, float cy, float r, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r_col = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        
        var matrix = graphics.pose().last().pose();
        bufferBuilder.vertex(matrix, cx, cy, 0).color(r_col, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, cx, cy - r, 0).color(r_col, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, cx + r, cy, 0).color(r_col, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, cx, cy + r, 0).color(r_col, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, cx - r, cy, 0).color(r_col, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, cx, cy - r, 0).color(r_col, g, b, a).endVertex();
        tesselator.end();
    }

    private static void drawDiamondBorder(GuiGraphics graphics, float cx, float cy, float r, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r_col = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        
        var matrix = graphics.pose().last().pose();
        bufferBuilder.vertex(matrix, cx, cy - r, 0).color(r_col, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, cx + r, cy, 0).color(r_col, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, cx, cy + r, 0).color(r_col, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, cx - r, cy, 0).color(r_col, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, cx, cy - r, 0).color(r_col, g, b, a).endVertex();
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
            case GOLD -> 0xE6C845;
            case ELECTRUM -> 0xE6E6AF;
            case CHROMIUM -> 0x5F9EA0;
            case NICROSIL -> 0x4682B4;
            case CADMIUM -> 0x66CDAA;
            case BENDALLOY -> 0xFFA500;
            case ALUMINUM -> 0xB0B5B3;
            case DURALUMIN -> 0x8C92AC;
            case ATIUM -> 0x5B6B7C;
            case LERASIUM -> 0x8DF2FF;
            case TRELLIUM -> 0x4A0E3B;   // Deep spiritual purple
            case RAYSIUM -> 0xFFD700;    // Golden radiance
            case TANAVASTIUM -> 0x00CED1; // Divine teal
            default -> 0xCCCCCC;
        };
    }

    private String getAllomanticRole(Metal metal) {
        return switch (metal) {
            case IRON -> "Lurcher";
            case STEEL -> "Coinshot";
            case TIN -> "Tineye";
            case PEWTER -> "Thug / Pewterarm";
            case ZINC -> "Rioter";
            case BRASS -> "Soother";
            case COPPER -> "Smoker";
            case BRONZE -> "Seeker";
            case GOLD -> "Gold Misting";
            case ELECTRUM -> "Oracle";
            case CHROMIUM -> "Leecher";
            case NICROSIL -> "Nicroburst";
            case CADMIUM -> "Pulser";
            case BENDALLOY -> "Slider";
            case ALUMINUM -> "Aluminum Gnat";
            case DURALUMIN -> "Duralumin Gnat";
            case ATIUM -> "Atium Misting";
            case LERASIUM -> "Mistborn Creator";
            case TRELLIUM -> "Phantom";
            case RAYSIUM -> "Siphoner";
            case TANAVASTIUM -> "Anchor";
            default -> "Misting";
        };
    }

    private String getFeruchemicalRole(Metal metal) {
        return switch (metal) {
            case IRON -> "Skimmer";
            case STEEL -> "Steelrunner";
            case TIN -> "Windwhisperer";
            case PEWTER -> "Brute";
            case ZINC -> "Sparker";
            case BRASS -> "Firesoul";
            case COPPER -> "Archivist";
            case BRONZE -> "Sentry";
            case GOLD -> "Bloodmaker";
            case ELECTRUM -> "Spinner";
            case CHROMIUM -> "Spinner";
            case NICROSIL -> "Soulbearer";
            case CADMIUM -> "Gasper";
            case BENDALLOY -> "Subsumer";
            case ALUMINUM -> "Trueself";
            case DURALUMIN -> "Connector";
            case ATIUM -> "Atium Ferring";
            case LERASIUM -> "Mistborn";
            case TRELLIUM -> "Ghostblood";
            case RAYSIUM -> "Drainer";
            case TANAVASTIUM -> "Pillar";
            default -> "Ferring";
        };
    }

    private String getMetalLore(Metal metal, boolean feru) {
        if (!feru) {
            return switch (metal) {
                case IRON -> "Iron Allomancy: Pulls on nearby metallic objects, blocks, or entities. Essential for vertical traversal.";
                case STEEL -> "Steel Allomancy: Pushes away from nearby metallic objects. Launch coins at enemies or launch yourself into the sky.";
                case TIN -> "Tin Allomancy: Enhances the senses. Grants continuous Night Vision and reveals hidden, active targets through walls.";
                case PEWTER -> "Pewter Allomancy: Increases physical capabilities. Take less damage, run faster, mine faster, and survive death blows.";
                case ZINC -> "Zinc Allomancy: Inflames the emotions of nearby entities, drawing their absolute hostility and aggression.";
                case BRASS -> "Brass Allomancy: Soothes the emotions of nearby entities, rendering them completely neutral and passive.";
                case COPPER -> "Copper Allomancy: Emits a coppercloud that completely hides Allomantic pulses from bronze seekers.";
                case BRONZE -> "Bronze Allomancy: Allows hearing the rhythmic, melodic pulses of other active metal burners in the area.";
                case GOLD -> "Gold Allomancy: Reveals a shadowy vision of your past choices, showing what you could have been.";
                case ELECTRUM -> "Electrum Allomancy: Reveals a brief path of your immediate future, granting speed and clear vision.";
                case CHROMIUM -> "Chromium Allomancy: Leeches a target's metal reserves completely dry upon contact.";
                case NICROSIL -> "Nicrosil Allomancy: Instantly flares and hyper-burns another burner's metal reserves in a singular burst.";
                case CADMIUM -> "Cadmium Allomancy: Slows down time in a bubble, freezing block-ticks and projectiles.";
                case BENDALLOY -> "Bendalloy Allomancy: Creates a high-speed time bubble, speeding up ticks inside.";
                case ALUMINUM -> "Aluminum Allomancy: Instantly purges and drains all of your own active metal reserves.";
                case DURALUMIN -> "Duralumin Allomancy: Causes an explosive, instant burn of your active metals, exhausting their reserves for massive power.";
                case ATIUM -> "Atium Allomancy: Extrapolates immediate shadows of future movement. Evasion and combat intuition.";
                case LERASIUM -> "Lerasium Allomancy: Pure divine metal. Consuming it completely rewires the spirit, creating a Mistborn.";
                case TRELLIUM -> "Trellium Allomancy: Burning grants absolute spiritual stealth. Undetectable by seekers, copperclouds, and all spiritual detection. Duralumin burst grants extended total invisibility.";
                case RAYSIUM -> "Raysium Allomancy: Burns to passively siphon life and energy from nearby entities. Duralumin burst unleashes a devastating drain wave, siphoning 30% health from all nearby targets.";
                case TANAVASTIUM -> "Tanavastium Allomancy: Burns to reinforce the spiritweb, granting +40 Soul Stability. Duralumin burst instantly restores Soul Stability to maximum and grants divine protection.";
                default -> "Pure metal arts power.";
            };
        } else {
            return switch (metal) {
                case IRON -> "Iron Feruchemy: Stores physical weight. Storing grants featherlight weight and low fall damage. Tapping sinks in water and triggers ground slams.";
                case STEEL -> "Steel Feruchemy: Stores physical speed. Storing makes you extremely sluggish. Tapping scales speed, attack speed, and mining haste.";
                case TIN -> "Tin Feruchemy: Stores physical senses. Storing causes blindness. Tapping grants Night Vision and glows moving entities.";
                case PEWTER -> "Pewter Feruchemy: Stores strength. Storing makes you weak. Tapping scales attack damage and allows bare-fist mining.";
                case ZINC -> "Zinc Feruchemy: Stores mental speed. Storing causes slowness. Tapping slows projectiles and speeds up active furnace/brewing ticks.";
                case BRASS -> "Brass Feruchemy: Stores heat. Storing freezes yourself and provides fire resistance. Tapping ignites nearby enemies and melts ice.";
                case COPPER -> "Copper Feruchemy: Stores experience. Storing drains XP points losslessly into copper metalminds. Tapping retrieves stored XP.";
                case BRONZE -> "Bronze Feruchemy: Stores wakefulness. Storing causes slowness/sleep. Tapping cleanses phantoms and fatigue.";
                case GOLD -> "Gold Feruchemy: Stores health. Storing halves max health pool. Tapping rapidly heals wounds and purges poison/wither.";
                case ELECTRUM -> "Electrum Feruchemy: Stores determination. Storing weakens you. Tapping grants absorption shield and cures debuffs.";
                case CHROMIUM -> "Chromium Feruchemy: Stores fortune. Storing grants bad luck. Tapping scales looting levels and forces guaranteed critical hits.";
                case NICROSIL -> "Nicrosil Feruchemy: Stores Investiture. Storing locks out all Allomancy. Tapping amplifies other metal reserve burn rates.";
                case CADMIUM -> "Cadmium Feruchemy: Stores breath. Allows holding breath indefinitely.";
                case BENDALLOY -> "Bendalloy Feruchemy: Stores calories. Storing digests food quickly. Tapping restores hunger bars instantly.";
                case ALUMINUM -> "Aluminum Feruchemy: Stores identity. Storing allows creating unkeyed metalminds that other Feruchemists can tap.";
                case DURALUMIN -> "Duralumin Feruchemy: Stores connection. Storing makes you socially isolated. Tapping grants friendly relations with villagers and mobs.";
                case ATIUM -> "Atium Feruchemy: Stores physical youth and age.";
                case LERASIUM -> "Pure Investiture Feruchemy: Pure metalmind capability.";
                case TRELLIUM -> "Trellium Feruchemy: Stores spiritual presence. Storing grants invisibility but slows movement. Tapping reveals all entities through walls in a massive radius, bypassing even copperclouds.";
                case RAYSIUM -> "Raysium Feruchemy: Stores siphoned energy. Storing drains active metal reserves. Tapping unleashes siphoning damage to nearby entities, converting their life force into healing and reserve restoration.";
                case TANAVASTIUM -> "Tanavastium Feruchemy: Stores spiritual integrity. Tapping grants massive Soul Stability bonus, pauses all spike decay, and negates wither/instability penalties.";
                default -> "Pure feruchemical capability.";
            };
        }
    }
}
