package net.gudenau.cavegame.gui.input;

/// The "action" of an input.
public enum InputAction {
    /// For when an input started being pressed.
    START,
    /// For when an input is continuing to be held.
    CONTINUE,
    /// For when an input was released.
    STOP,
}
