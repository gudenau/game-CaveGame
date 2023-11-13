import net.gudenau.cavegame.renderer.RendererInfo;
import net.gudenau.cavegame.renderer.gl.GlRendererInfo;

module net.gudenau.cavegame.renderer.gl {
    provides RendererInfo with GlRendererInfo;

    requires net.gudenau.cavegame.renderer;

    requires static org.jetbrains.annotations;

    requires org.lwjgl;
    requires org.lwjgl.glfw;
    requires org.lwjgl.opengl;

    requires it.unimi.dsi.fastutil;

    requires net.gudenau.cavegame.logger;
    requires net.gudenau.cavegame.utilities;
}
