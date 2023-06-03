import net.gudenau.cavegame.renderer.RendererInfo;

module net.gudenau.cavegame.renderer {
    exports net.gudenau.cavegame.renderer;

    uses RendererInfo;

    requires org.jetbrains.annotations;

    requires org.lwjgl;
    requires org.lwjgl.glfw;

    requires net.gudenau.cavegame.logger;
}
