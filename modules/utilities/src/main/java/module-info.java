import net.gudenau.cavegame.resource.ResourceProviderProvider;

module net.gudenau.cavegame.utilities {
    exports net.gudenau.cavegame.annotations;
    exports net.gudenau.cavegame.config;
    exports net.gudenau.cavegame.resource;
    exports net.gudenau.cavegame.util;
    exports net.gudenau.cavegame.util.collection;

    requires net.gudenau.cavegame.logger;

    requires jdk.unsupported;

    requires static org.jetbrains.annotations;

    requires it.unimi.dsi.fastutil;

    requires org.lwjgl;

    requires net.gudenau.codec;
    requires net.gudenau.codec.json;
    requires net.gudenau.json;

    uses ResourceProviderProvider;
}
