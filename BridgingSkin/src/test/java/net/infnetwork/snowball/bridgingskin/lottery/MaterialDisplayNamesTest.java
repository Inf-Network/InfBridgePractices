package net.infnetwork.snowball.bridgingskin.lottery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class MaterialDisplayNamesTest {
    @Test
    void usesMinecraftTranslationKeyForStrippedMangroveWood() {
        TranslatableComponent name = MaterialDisplayNames.translated(
                Material.STRIPPED_MANGROVE_WOOD, NamedTextColor.GREEN);

        assertEquals("block.minecraft.stripped_mangrove_wood", name.key());
        assertEquals(NamedTextColor.GREEN, name.color());
    }
}
