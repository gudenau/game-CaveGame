import net.gudenau.cavegame.renderer.RendererInfo;
import net.gudenau.cavegame.renderer.vk.VkRendererInfo;

module net.gudenau.cavegame.renderer.vulkan {
    provides RendererInfo with VkRendererInfo;

    requires net.gudenau.cavegame.renderer;
    requires net.gudenau.cavegame.logger;
    requires net.gudenau.cavegame.utilities;

    requires org.jetbrains.annotations;

    requires it.unimi.dsi.fastutil;

    requires org.joml;

    requires org.lwjgl;
    requires org.lwjgl.glfw;
    requires org.lwjgl.vulkan;
    requires org.lwjgl.shaderc;
    requires org.lwjgl.spvc;
}
