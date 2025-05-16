package net.gudenau.cavegame.gui.drawing;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FontStyle {
    @SuppressWarnings("unchecked")
    private static final FontStyle DEFAULT = new FontStyle(Stream.of(
            FontAttribute.horizontalAlignment(FontAttribute.Alignment.NEG),
            FontAttribute.verticalAlignment(FontAttribute.Alignment.NEG),
            FontAttribute.color(0, 0, 0)
        ).collect(Collectors.toUnmodifiableMap(
            (attr) -> (Class<? extends FontAttribute>) attr.getClass(),
            Function.identity()
        )
    ));
    static {
        var attrs = Stream.of(FontAttribute.class.getClasses())
            .filter(FontAttribute.class::isAssignableFrom)
            .collect(Collectors.toSet());

        DEFAULT.attributes.forEach((k, _) -> attrs.remove(k));

        if(!attrs.isEmpty()) {
            throw new AssertionError(
                "Default FontStyle is missing attributes: " +
                    attrs.stream()
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", "))
            );
        }
    }

    private final Map<Class<? extends FontAttribute>, FontAttribute> attributes;

    private FontStyle(Map<Class<? extends FontAttribute>, FontAttribute> attributes) {
        this.attributes = Map.copyOf(attributes);
    }

    @NotNull
    public static FontStyle of() {
        return DEFAULT;
    }

    @NotNull
    public static FontStyle of(@NotNull FontAttribute attribute) {
        return DEFAULT.with(attribute);
    }

    @NotNull
    public static FontStyle of(@NotNull FontAttribute @NotNull ... attributes) {
        return DEFAULT.with(attributes);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T extends FontAttribute> T get(Class<T> attribute) {
        var value = (T) attributes.get(attribute);
        if(value == null) {
            throw new AssertionError("Somehow a FontStyle didn't have attribute " + attribute.getSimpleName());
        }
        return value;
    }

    @NotNull
    public FontStyle with(@NotNull FontAttribute attribute) {
        if(Objects.equals(attributes.get(attribute.getClass()), attribute)) {
            return this;
        }

        return new FontStyle(new HashMap<>(attributes) {{
            put(attribute.getClass(), attribute);
        }});
    }

    @NotNull
    public FontStyle with(@NotNull FontAttribute @NotNull ... attributes) {
        var map = new HashMap<>(this.attributes);

        for(var attribute : attributes) {
            map.put(attribute.getClass(), attribute);
        }

        if(map.equals(this.attributes)) {
            return this;
        } else {
            return new FontStyle(map);
        }
    }
}
