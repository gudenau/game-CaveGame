package net.gudenau.cavegame.gui.layout;

import net.gudenau.cavegame.gui.Direction;
import net.gudenau.cavegame.gui.component.Component;
import net.gudenau.cavegame.gui.component.Container;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

public final class LinearLayoutEngine implements LayoutEngine {
    private static final Map<Direction.Axis, LinearLayoutEngine> INSTANCES = Map.of(
        Direction.Axis.X, new LinearLayoutEngine(Direction.Axis.X),
        Direction.Axis.Y, new LinearLayoutEngine(Direction.Axis.Y)
    );

    @NotNull
    public static LinearLayoutEngine get(@NotNull Direction.Axis axis) {
        return INSTANCES.get(axis);
    }

    @NotNull
    private final Direction.Axis axis;

    private int minimumWidth;
    private int minimumHeight;

    private LinearLayoutEngine(@NotNull Direction.Axis axis) {
        this.axis = axis;
    }

    @Override
    public void minimumSize(int width, int height) {
        minimumWidth = width;
        minimumHeight = height;
    }

    @NotNull
    @Override
    public Layout layout(@NotNull Container parent) {
        var children = parent.children();

        var entries = new ArrayList<Layout.Entry>(children.size());
        int location = 0;

        ToIntFunction<Component> size = switch(axis) {
            case X -> Component::width;
            case Y -> Component::height;
        };
        BiFunction<Integer, Component, Layout.Entry> entryFactory = switch(axis) {
            case X -> (value, component) -> new Layout.Entry(value, 0, component);
            case Y -> (value, component) -> new Layout.Entry(0, value, component);
        };

        for(var child : children) {
            entries.add(entryFactory.apply(location, child));
            var childSize = size.applyAsInt(child);
            location += childSize;
        }

        var width = entries.stream()
            .mapToInt((entry) -> entry.x() + entry.component().width())
            .max()
            .orElse(0);
        var height = entries.stream()
            .mapToInt((entry) -> entry.y() + entry.component().height())
            .max()
            .orElse(0);

        return new Layout(Math.max(minimumWidth, width), Math.max(minimumHeight, height), List.copyOf(entries));
    }
}
