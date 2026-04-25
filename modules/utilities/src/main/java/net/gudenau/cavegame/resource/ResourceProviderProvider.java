package net.gudenau.cavegame.resource;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface ResourceProviderProvider {
    @NotNull Map<@NotNull String, @NotNull ResourceProvider> providers();
}
