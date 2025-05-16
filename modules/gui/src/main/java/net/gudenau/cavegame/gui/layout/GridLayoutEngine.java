package net.gudenau.cavegame.gui.layout;

import net.gudenau.cavegame.gui.component.Component;
import net.gudenau.cavegame.gui.component.Container;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.SequencedCollection;
import java.util.stream.IntStream;

/// A simple grid {@link GridLayoutEngine}.
//FIXME Make this immutable
public final class GridLayoutEngine implements LayoutEngine {
    public static final int UNLIMITED = -1;

    private final int columns;
    private final int rows;

    private int minimumWidth;
    private int minimumHeight;

    public GridLayoutEngine(int columns, int rows) {
        if(columns == UNLIMITED && rows == UNLIMITED) {
            throw new IllegalArgumentException("Columns and rows can't both be unlimited");
        }

        this.columns = columns;
        this.rows = rows;
    }

    @Override
    public void minimumSize(int width, int height) {
        minimumWidth = width;
        minimumHeight = height;
    }

    @Override
    @NotNull
    public Layout layout(@NotNull Container parent) {
        var children = parent.children();

        final int columns, rows;
        solver: {
            if(this.columns != UNLIMITED && this.rows != UNLIMITED) {
                columns = this.columns;
                rows = this.rows;
                break solver;
            }

            if(this.columns == UNLIMITED) {
                columns = Math.floorDiv(children.size(), this.rows);
                rows = this.rows;
            } else {
                columns = this.columns;
                rows = Math.floorDiv(children.size(), this.columns);
            }
        }
        var limit = (long) columns * rows;

        int minimumWidth = this.minimumWidth / columns;
        int minimumHeight = this.minimumHeight / rows;

        var state = new Object() {
            int x = 0;
            int y = 0;
            final int[] widths = new int[columns];
            final int[] heights = new int[rows];

            void size(int width, int height) {
                widths[x] = Math.max(widths[x], Math.max(minimumWidth, width));
                heights[y] = Math.max(heights[y], Math.max(minimumHeight, height));
                advance();
            }

            int width() {
                return widths[x];
            }

            int height() {
                return heights[y];
            }

            int x() {
                int value = 0;
                for(int i = 0; i < x; i++) {
                    value += widths[i];
                }
                return value;
            }

            int y() {
                int value = 0;
                for(int i = 0; i < y; i++) {
                    value += heights[i];
                }
                return value;
            }

            int totalWidth() {
                return IntStream.of(widths).sum();
            }

            int totalHeight() {
                return IntStream.of(heights).sum();
            }

            void advance() {
                if(++x >= columns) {
                    x = 0;
                    y++;
                }
            }

            void reset() {
                x = 0;
                y = 0;
            }
        };

        SequencedCollection<Component> sequencedChildren;
        if(children instanceof SequencedCollection<Component> sc) {
            sequencedChildren = sc;
        } else {
            sequencedChildren = List.copyOf(children);
        }

        sequencedChildren.stream()
            .limit(limit)
            .forEachOrdered((child) -> state.size(child.width(), child.height()));

        state.reset();

        var entries = sequencedChildren.stream()
            .limit(limit)
            .map((child) -> {
                var entry = new Layout.Entry(state.x(), state.y(), state.width(), state.height(), child);
                state.advance();
                return entry;
            })
            .toList();

        return new Layout(state.totalWidth(), state.totalHeight(), entries);
    }
}
