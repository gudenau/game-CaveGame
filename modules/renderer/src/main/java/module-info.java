import net.gudenau.cavegame.renderer.RendererInfo;

module net.gudenau.cavegame.renderer {
    exports net.gudenau.cavegame.renderer;
    exports net.gudenau.cavegame.renderer.model;
    exports net.gudenau.cavegame.renderer.shader;
    exports net.gudenau.cavegame.renderer.texture;
    exports net.gudenau.cavegame.renderer.font;

    uses RendererInfo;

    //FIXME Remove this once PngReader is no longer using AWT
    requires java.desktop;

    requires static org.jetbrains.annotations;

    requires it.unimi.dsi.fastutil;

    requires org.joml;

    requires org.lwjgl;
    requires org.lwjgl.freetype;
    requires org.lwjgl.glfw;
    requires org.lwjgl.harfbuzz;

    requires net.gudenau.cavegame.logger;
    requires net.gudenau.cavegame.utilities;
}
