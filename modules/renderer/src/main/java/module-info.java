import net.gudenau.cavegame.renderer.RendererInfo;

module net.gudenau.cavegame.renderer {
    exports net.gudenau.cavegame.renderer;
    exports net.gudenau.cavegame.renderer.shader;

    uses RendererInfo;

    requires org.jetbrains.annotations;

    requires it.unimi.dsi.fastutil;

    requires org.lwjgl;
    requires org.lwjgl.glfw;

    requires net.gudenau.cavegame.logger;
    requires net.gudenau.cavegame.utilities;
}
