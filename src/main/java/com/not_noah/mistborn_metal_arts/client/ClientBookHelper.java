package com.not_noah.mistborn_metal_arts.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ClientBookHelper {
    private ClientBookHelper() {
    }

    public static void openManuscript() {
        List<FormattedText> pages = new ArrayList<>();
        pages.add(Component.literal("Of the Art of Hemalurgic extraction, there are precise points on the human form where the soul is most vulnerable to spike placement.\n\nA spike must be driven with absolute accuracy to claim the desired attribute. These are the known optimal coordinates."));
        pages.add(Component.literal("§cPHYSICAL QUADRANT§r\n\n§lSteel Spike§r (Physical Allomancy)\n- Steel: Right Shoulder\n  (X: 0.25, Y: 1.35, Z: 0.00)\n- Iron: Left Shoulder\n  (X: -0.25, Y: 1.35, Z: 0.00)\n- Pewter: Upper Back\n  (X: 0.00, Y: 1.45, Z: 0.22)\n- Tin: Lower Back\n  (X: 0.00, Y: 1.00, Z: 0.22)"));
        pages.add(Component.literal("§cPHYSICAL QUADRANT§r\n\n§lPewter Spike§r (Physical Feruchemy)\n- Pewter: Lower Right Arm\n  (X: 0.28, Y: 0.85, Z: -0.10)\n- Tin: Lower Left Arm\n  (X: -0.28, Y: 0.85, Z: -0.10)\n- Iron: Right Chest\n  (X: 0.15, Y: 0.95, Z: -0.15)\n- Steel: Left Chest\n  (X: -0.15, Y: 0.95, Z: -0.15)"));
        pages.add(Component.literal("§cPHYSICAL QUADRANT§r\n\n§lIron Spike§r (Strength)\n- Iron Allomancy: Left Chest\n  (X: -0.20, Y: 1.30, Z: 0.10)\n- Iron Feruchemy: Right Chest\n  (X: 0.20, Y: 1.30, Z: 0.10)\n\n§lTin Spike§r (Senses)\n- Tin Allomancy: Left Temple\n  (X: -0.10, Y: 1.60, Z: 0.00)\n- Tin Feruchemy: Right Temple\n  (X: 0.10, Y: 1.60, Z: 0.00)"));
        pages.add(Component.literal("§9MENTAL QUADRANT§r\n\n§lBronze Spike§r (Mental Allomancy)\n- Zinc: Right Temple\n  (X: 0.12, Y: 1.62, Z: -0.12)\n- Brass: Left Temple\n  (X: -0.12, Y: 1.62, Z: -0.12)\n- Copper: Forehead\n  (X: 0.00, Y: 1.68, Z: -0.15)\n- Bronze: Crown of Head\n  (X: 0.00, Y: 1.75, Z: 0.00)"));
        pages.add(Component.literal("§9MENTAL QUADRANT§r\n\n§lBrass Spike§r (Cognitive Feruchemy)\n- Zinc: Right Brain\n  (X: 0.20, Y: 1.50, Z: -0.10)\n- Brass: Left Brain\n  (X: -0.20, Y: 1.50, Z: -0.10)\n- Copper: Back Left Brain\n  (X: 0.10, Y: 1.45, Z: 0.10)\n- Bronze: Back Right Brain\n  (X: -0.10, Y: 1.45, Z: 0.10)"));
        pages.add(Component.literal("§9MENTAL QUADRANT§r\n\n§lZinc Spike§r (Emotional Fortitude)\n- Zinc Allomancy: Right Brain\n  (X: 0.12, Y: 1.55, Z: -0.10)\n- Zinc Feruchemy: Left Brain\n  (X: -0.12, Y: 1.55, Z: -0.10)\n\n§lCopper Spike§r (Mental Fortitude)\n- Copper Allomancy: Forehead\n  (X: 0.00, Y: 1.65, Z: -0.12)\n- Copper Feruchemy: Back Head\n  (X: 0.00, Y: 1.60, Z: 0.12)"));
        pages.add(Component.literal("§aSPIRITUAL QUADRANT§r\n\n§lElectrum Spike§r (Enhancement Allomancy)\n- Chromium: Right Plexus\n  (X: 0.15, Y: 1.35, Z: -0.10)\n- Nicrosil: Left Plexus\n  (X: -0.15, Y: 1.35, Z: -0.10)\n- Duralumin: Back Spine\n  (X: 0.00, Y: 1.25, Z: 0.10)"));
        pages.add(Component.literal("§aSPIRITUAL QUADRANT§r\n\n§lBendalloy Spike§r (Spiritual Feruchemy)\n- Chromium: Upper Right Spine\n  (X: 0.15, Y: 1.10, Z: -0.10)\n- Nicrosil: Upper Left Spine\n  (X: -0.15, Y: 1.10, Z: -0.10)\n- Duralumin: Lower Spine\n  (X: 0.00, Y: 1.00, Z: 0.10)"));
        pages.add(Component.literal("§aSPIRITUAL QUADRANT§r\n\n§lChromium Spike§r (Destiny)\n- Chromium Allomancy: Front Spine\n  (X: 0.00, Y: 1.25, Z: -0.15)\n- Chromium Feruchemy: Back Spine\n  (X: 0.00, Y: 1.25, Z: 0.15)\n\n§lNicrosil Spike§r (Investiture)\n- Nicrosil Allomancy: Front Spine\n  (X: 0.00, Y: 1.30, Z: -0.10)\n- Nicrosil Feruchemy: Back Spine\n  (X: 0.00, Y: 1.30, Z: 0.10)"));
        pages.add(Component.literal("§aSPIRITUAL QUADRANT§r\n\n§lAluminum Spike§r (Power Removal)\n- Removes all powers: Plexus\n  (X: 0.00, Y: 1.20, Z: 0.00)\n\n§lDuralumin Spike§r (Connection)\n- Duralumin Allomancy: Heart\n  (X: 0.00, Y: 1.10, Z: -0.10)\n- Duralumin Feruchemy: Spine\n  (X: 0.00, Y: 1.10, Z: 0.10)"));
        pages.add(Component.literal("§eTEMPORAL QUADRANT§r\n\n§lCadmium Spike§r (Temporal Allomancy)\n- Cadmium: Lower Front Left\n  (X: 0.10, Y: 1.05, Z: -0.10)\n- Bendalloy: Lower Front Right\n  (X: -0.10, Y: 1.05, Z: -0.10)\n- Gold: Lower Back Left\n  (X: 0.10, Y: 0.95, Z: 0.10)\n- Electrum: Lower Back Right\n  (X: -0.10, Y: 0.95, Z: 0.10)"));
        pages.add(Component.literal("§eTEMPORAL QUADRANT§r\n\n§lGold Spike§r (Hybrid Feruchemy)\n- Gold: Left Chest (Heart)\n  (X: -0.10, Y: 1.25, Z: -0.12)\n- Electrum: Right Chest\n  (X: 0.10, Y: 1.25, Z: -0.12)\n- Cadmium: Back Left Chest\n  (X: -0.10, Y: 1.15, Z: 0.10)\n- Bendalloy: Back Right Chest\n  (X: 0.10, Y: 1.15, Z: 0.10)"));
        pages.add(Component.literal("§5GOD METALS§r\n\n§lAtium Spike§r (Steals Any Power)\n- Steals targeted power depending on aimed coordinates (Steel, Iron, Gold, Pewter bind points).\n\n§lLerasium Spike§r (Steals All Abilities)\n- Steals all abilities at once: Plexus\n  (X: 0.00, Y: 1.20, Z: 0.00)"));

        BookViewScreen.BookAccess bookAccess = new BookViewScreen.BookAccess() {
            @Override
            public int getPageCount() {
                return pages.size();
            }

            @Override
            public FormattedText getPageRaw(int index) {
                return pages.get(index);
            }
        };

        Minecraft.getInstance().setScreen(new HemalurgicBookScreen(bookAccess));
    }
}
