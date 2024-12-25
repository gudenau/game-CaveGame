package net.gudenau.cavegame.renderer.font;

import org.lwjgl.system.Configuration;
import org.lwjgl.util.freetype.FreeType;

public final class HarfBuzzFont {
    static {
        Configuration.HARFBUZZ_LIBRARY_NAME.set(FreeType.getLibrary());
    }

    // Triggers the static block
    public static void staticInit() {}

    private HarfBuzzFont() {

    }
}
