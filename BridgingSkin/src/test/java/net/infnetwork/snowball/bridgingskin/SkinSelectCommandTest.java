package net.infnetwork.snowball.bridgingskin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class SkinSelectCommandTest {
    @Test
    void itemComponentPreservesUnspecifiedItalicDecoration() {
        Component component = Component.text("皮肤");

        Component result = SkinSelectCommand.itemComponent(component);

        assertEquals(TextDecoration.State.NOT_SET,
                result.decoration(TextDecoration.ITALIC));
    }

    @Test
    void materialNameUsesClientTranslationKey() {
        TranslatableComponent result = SkinSelectCommand.materialName(
                Material.STRIPPED_MANGROVE_WOOD, NamedTextColor.YELLOW);

        assertEquals("block.minecraft.stripped_mangrove_wood", result.key());
        assertEquals(NamedTextColor.YELLOW, result.color());
    }

    @Test
    void adminCatalogNameAlsoContainsNativeTranslation() {
        Component displayName = SkinItemComponents.adminCatalogName(Material.DEEPSLATE, true);

        TranslatableComponent translated = assertInstanceOf(
                TranslatableComponent.class, displayName.children().getLast());
        assertEquals("block.minecraft.deepslate", translated.key());
    }

    @Test
    void returnButtonKeepsExistingPaginationSlots() {
        assertEquals(45, SkinSelectHolder.RETURN_SLOT);
        assertTrue(SkinSelectHolder.RETURN_SLOT >= SkinSelectHolder.CONTENT_SIZE);
        assertNotEquals(SkinSelectHolder.PREVIOUS_SLOT, SkinSelectHolder.RETURN_SLOT);
        assertNotEquals(SkinSelectHolder.PAGE_SLOT, SkinSelectHolder.RETURN_SLOT);
        assertNotEquals(SkinSelectHolder.NEXT_SLOT, SkinSelectHolder.RETURN_SLOT);
        assertEquals(48, SkinSelectHolder.PREVIOUS_SLOT);
        assertEquals(49, SkinSelectHolder.PAGE_SLOT);
        assertEquals(50, SkinSelectHolder.NEXT_SLOT);
    }
}
