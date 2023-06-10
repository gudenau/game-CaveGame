package net.gudenau.cavegame.launcher;

import net.gudenau.cavegame.logger.Logger;

public class Launcher {
    private static final Logger LOGGER = Logger.forName("launcher");

    public static void main(String[] args) {
        LOGGER.info("Hello world!");
        LOGGER.info(Launcher.class.getModule().getName());
    }
}
