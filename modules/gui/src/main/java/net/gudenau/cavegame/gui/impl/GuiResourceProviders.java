package net.gudenau.cavegame.gui.impl;

import net.gudenau.cavegame.resource.ClassPathResourceProvider;
import net.gudenau.cavegame.resource.ResourceProvider;
import net.gudenau.cavegame.resource.ResourceProviderProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class GuiResourceProviders implements ResourceProviderProvider {
    @Override
    @NotNull
    public Map<String, ResourceProvider> providers() {
        return Map.of(
            "gui", ClassPathResourceProvider.of(GuiResourceProviders.class)
        );
    }
}
