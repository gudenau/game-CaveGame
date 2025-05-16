package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.input.InputAction;
import net.gudenau.cavegame.gui.input.MouseButton;
import net.gudenau.cavegame.gui.drawing.Drawable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/// The base interface for GUI elements.
public interface Component extends Drawable {
    /// Gets the width of this component.
    ///
    /// @return The width of this component
    int width();

    /// Gets the height of this component.
    ///
    /// @return The height of this component
    int height();

    /// Sets the parent of this component.
    ///
    /// @param container The container this component will belong to
    /// @throws IllegalStateException If this component already had a parent
    void parent(@NotNull Component container);

    /// Gets the parent of this component, if it has been set.
    ///
    /// @return The parent of this component
    @NotNull Optional<Component> parent();

    /// Called when this component is clicked. The coordinates are relative to this component's location.
    ///
    /// @param x The X position of the cursor
    /// @param y The Y position of the cursor
    /// @param button The mouse button that was clicked
    default void onClick(int x, int y, @NotNull MouseButton button) {}

    /// Called when this the mouse is "dragged" over this component. The coordinates are relative to this component's
    /// location.
    ///
    /// @param x The X position of the cursor
    /// @param y The Y position of the cursor
    /// @param button The mouse button that was clicked
    /// @param action The "drag" action
    default void onDrag(int x, int y, @NotNull MouseButton button, @NotNull InputAction action) {}

    /// Called when the mouse wheel is used with the cursor hovering over this component. The coordinates are relative
    /// to this component's location. A positive amount is for scrolling up, a negative direction is for scrolling down.
    ///
    /// @param x The X position of the cursor
    /// @param y The Y position of the cursor
    /// @param amount The amount the wheel was scrolled
    default void onScroll(int x, int y, int amount) {}

    /// Marks this component as being dirty as well as its parent.
    default void invalidate() {
        parent().ifPresent(Component::invalidate);
    }
}
