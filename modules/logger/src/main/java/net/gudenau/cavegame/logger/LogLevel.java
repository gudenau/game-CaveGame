package net.gudenau.cavegame.logger;

public enum LogLevel {
    DEBUG(0x80_80_80),
    INFO(0xF0_F0_F0),
    WARN(0xF0_F0_00),
    ERROR(0xD0_00_00),
    FATAL(0xFF_00_00),
    ;

    final int color;

    LogLevel(int color) {
        this.color = color;
    }
}
