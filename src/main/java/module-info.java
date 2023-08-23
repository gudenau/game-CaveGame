module net.gudenau.cavegame {
    exports net.gudenau.cavegame.actor;
    exports net.gudenau.cavegame.ai;
    exports net.gudenau.cavegame.api;
    exports net.gudenau.cavegame.input;
    exports net.gudenau.cavegame.material;
    exports net.gudenau.cavegame.tile;
    exports net.gudenau.cavegame.level;

    requires jdk.unsupported;
    
    requires org.jetbrains.annotations;
    
    requires org.joml;
    
    requires it.unimi.dsi.fastutil;

    requires com.google.gson;

    requires net.jodah.typetools;

    requires org.lwjgl;

    requires transitive net.gudenau.cavegame.logger;
    requires transitive net.gudenau.cavegame.utilities;
    requires net.gudenau.cavegame.wooting;
    requires net.gudenau.cavegame.renderer;
}
