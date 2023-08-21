module net.gudenau.cavegame.utilities {
    exports net.gudenau.cavegame.annotations;
    exports net.gudenau.cavegame.codec;
    exports net.gudenau.cavegame.codec.ops;
    exports net.gudenau.cavegame.config;
    exports net.gudenau.cavegame.resource;
    exports net.gudenau.cavegame.util;
    exports net.gudenau.cavegame.util.collection;

    requires jdk.unsupported;

    requires org.jetbrains.annotations;

    requires it.unimi.dsi.fastutil;

    requires org.lwjgl;

    requires com.google.gson;

    requires net.jodah.typetools;
}
