package com.not_noah.mistborn_metal_arts.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom book screen that renders the Hemalurgic spiking chart image
 * alongside the vanilla book GUI whenever the manuscript is open.
 */
@OnlyIn(Dist.CLIENT)
public class HemalurgicBookScreen extends BookViewScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(HemalurgicBookScreen.class);
    private static final ResourceLocation MAP_TEXTURE = new ResourceLocation("mistborn_metal_arts", "textures/gui/hemalurgic_map.png");

    private boolean loggedOnce = false;

    public HemalurgicBookScreen(BookViewScreen.BookAccess bookAccess) {
        super(bookAccess);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render vanilla book first (background, text, page buttons)
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // BookViewScreen centres the 192×192 book at ((width-192)/2, 2)
        int bookLeftX = (this.width - 192) / 2;
        int bookTopY = 2;

        // Calculate available space on left and right of book
        int spaceLeft = bookLeftX;
        int spaceRight = this.width - (bookLeftX + 192);

        int mapSize;
        int mapX;
        int mapY;

        if (spaceLeft >= 100) {
            // Render to the left of the book
            mapSize = Math.min(spaceLeft - 12, 192);
            mapX = bookLeftX - mapSize - 6;
            mapY = bookTopY;
        } else if (spaceRight >= 100) {
            // Render to the right of the book
            mapSize = Math.min(spaceRight - 12, 192);
            mapX = bookLeftX + 192 + 6;
            mapY = bookTopY;
        } else {
            // Small window — overlay a smaller version in bottom-right corner
            mapSize = 100;
            mapX = this.width - mapSize - 4;
            mapY = this.height - mapSize - 4;
        }

        if (!loggedOnce) {
            LOGGER.info("[HemalurgicBookScreen] Screen {}x{}, bookLeftX={}, spaceLeft={}, spaceRight={}, mapX={}, mapY={}, mapSize={}",
                    this.width, this.height, bookLeftX, spaceLeft, spaceRight, mapX, mapY, mapSize);
            loggedOnce = true;
        }

        // Explicitly bind the texture to GL texture slot 0
        RenderSystem.setShaderTexture(0, MAP_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Use the 9-param blit overload that works identically to the vignette rendering:
        // blit(ResourceLocation, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight)
        // This samples a width×height pixel region starting at UV (u,v) from a textureWidth×textureHeight texture.
        // Since we want to render the full 512×512 texture scaled down to mapSize×mapSize,
        // we render at mapSize on screen but tell it the "texture" is mapSize×mapSize,
        // which causes the entire PNG to be mapped/stretched to the render area.
        guiGraphics.blit(MAP_TEXTURE, mapX, mapY, 0.0F, 0.0F, mapSize, mapSize, mapSize, mapSize);

        RenderSystem.disableBlend();
    }
}
