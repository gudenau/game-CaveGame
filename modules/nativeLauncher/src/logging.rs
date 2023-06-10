#![allow(unused)]

// This should mirror n.g.c.l.LogLevel
#[derive(Debug, Copy, Clone, PartialEq, Eq, PartialOrd)]
pub enum LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL,
}

static LEVEL: LogLevel = LogLevel::DEBUG;

impl LogLevel {
    fn color(self) -> u32 {
        return match(self) {
            LogLevel::DEBUG => 0x80_80_80,
            LogLevel::INFO => 0xF0_F0_F0,
            LogLevel::WARN => 0xF0_F0_00,
            LogLevel::ERROR => 0xD0_00_00,
            LogLevel::FATAL => 0xFF_00_00,
        };
    }

    fn name(self) -> &'static str {
        return match(self) {
            LogLevel::DEBUG => "debug",
            LogLevel::INFO => "info",
            LogLevel::WARN => "warn",
            LogLevel::ERROR => "error",
            LogLevel::FATAL => "fatal",
        };
    }
}

fn ansiColor(color: u32) -> String {
    let red = (color >> 16) & 0xFF;
    let green = (color >> 8) & 0xFF;
    let blue = color & 0xFF;

    return format!("[38;2;{};{};{}m", red, green, blue);
}

pub fn log(level: LogLevel, message: impl Into<String>) {
    if(level < LEVEL) {
        return;
    }

    let color = ansiColor(level.color());
    let name = level.name();
    let prefix = format!("{}[{}][RLaunch] ", color, name);
    let suffix = "[0m";

    message.into().lines().for_each(|line| {
        println!("{}{}{}", prefix, line, suffix);
    });
}

pub fn debug(message: impl Into<String>) {
    log(LogLevel::DEBUG, message);
}

pub fn info(message: impl Into<String>) {
    log(LogLevel::INFO, message);
}

pub fn warn(message: impl Into<String>) {
    log(LogLevel::WARN, message);
}

pub fn error(message: impl Into<String>) {
    log(LogLevel::ERROR, message);
}

pub fn fatal(message: impl Into<String>) -> ! {
    log(LogLevel::FATAL, message);
    std::process::exit(1);
}
