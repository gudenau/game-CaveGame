package net.gudenau.cavegame.panama;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;

/**
 * A basic helper class to create structure layouts for the foreign memory API.
 */
@Deprecated(forRemoval = true)
public final class NativeLayout {
    /**
     * The layout of the created structure.
     */
    @NotNull
    private final StructLayout layout;

    private NativeLayout(@NotNull StructLayout layout) {
        this.layout = layout;
    }

    /**
     * Gets the layout used for the foreign memory API.
     *
     * @return The layout
     */
    @NotNull
    public StructLayout layout() {
        return layout;
    }

    /**
     * Gets a handle into the layout for accessing a member.
     *
     * @param name The name of the member
     * @return The handle to the member
     */
    @NotNull
    public VarHandle handle(@NotNull String name) {
        return layout.varHandle(MemoryLayout.PathElement.groupElement(name));
    }

    /**
     * Creates a new builder for a structure layout.
     *
     * @return The new builder
     */
    @Contract("-> new")
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A native layout builder to use with the foreign memory API.
     */
    public static final class Builder {
        /**
         * Current elements for this layout.
         */
        private final List<MemoryLayout> elements = new ArrayList<>();

        private Builder() {}

        /**
         * Creates a new layout instance after adding required padding to the structure.
         *
         * @return The new layout
         */
        @NotNull
        @Contract("-> new")
        public NativeLayout build() {
            long size = 0;
            long alignment = 0;
            long maxAlignment = 0;

            for(int i = 0, length = elements.size(); i < length; i++) {
                var element = elements.get(i);
                var requiredAlignment = element.byteAlignment();

                // Ensure things are correctly aligned
                if(requiredAlignment > alignment) {
                    long mask = requiredAlignment - 1;
                    long difference = (size + requiredAlignment) & mask;

                    if(difference != 0) {
                        size += difference;
                        elements.add(i, MemoryLayout.paddingLayout(difference));
                        // We added stuff to the list, ensure the loop doesn't iterate over the same thing multiple
                        // times
                        i++;
                        length++;
                    }
                }
                alignment = requiredAlignment;
                maxAlignment = Math.max(maxAlignment, alignment);

                size += element.byteSize();
            }

            // Extra padding to match C and allow for arrays
            long mask = maxAlignment - 1;
            long difference = (size + maxAlignment) & mask;
            if(difference != 0) {
                elements.add(MemoryLayout.paddingLayout(difference << 3));
            }

            return new NativeLayout(MemoryLayout.structLayout(elements.toArray(MemoryLayout[]::new)));
        }

        /**
         * Adds a new named 8 bit member to this layout.
         *
         * @param name The name of the member
         * @return This builder
         */
        @NotNull
        @Contract("_ -> this")
        public Builder u8(@NotNull String name) {
            elements.add(ValueLayout.JAVA_BYTE.withName(name));
            return this;
        }

        /**
         * Adds a new named 16 bit member to this layout.
         *
         * @param name The name of the member
         * @return This builder
         */
        @NotNull
        @Contract("_ -> this")
        public Builder u16(@NotNull String name) {
            elements.add(ValueLayout.JAVA_SHORT.withName(name));
            return this;
        }

        /**
         * Adds a new named 32 bit member to this layout.
         *
         * @param name The name of the member
         * @return This builder
         */
        @NotNull
        @Contract("_ -> this")
        public Builder u32(@NotNull String name) {
            elements.add(ValueLayout.JAVA_INT.withName(name));
            return this;
        }

        /**
         * Adds a new named 64 bit member to this layout.
         *
         * @param name The name of the member
         * @return This builder
         */
        @NotNull
        @Contract("_ -> this")
        public Builder u64(@NotNull String name) {
            elements.add(ValueLayout.JAVA_LONG.withName(name));
            return this;
        }

        /**
         * Adds a new named address member to this layout.
         *
         * @param name The name of the member
         * @return This builder
         */
        @NotNull
        @Contract("_ -> this")
        public Builder stringPointer(@NotNull String name) {
            elements.add(ValueLayout.ADDRESS.withName(name));
            return this;
        }

        /**
         * Adds a new named 32 bit member to this layout.
         *
         * @param name The name of the member
         * @return This builder
         */
        @NotNull
        @Contract("_ -> this")
        public Builder enumeration(@NotNull String name) {
            elements.add(ValueLayout.JAVA_INT.withName(name));
            return this;
        }
    }
}
