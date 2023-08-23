package net.gudenau.cavegame.renderer.internal;

import net.gudenau.cavegame.renderer.RendererInfo;

import java.util.List;
import java.util.ServiceLoader;

public final class RendererInfoImpl {
    private static volatile List<RendererInfo> info = null;

    public static List<RendererInfo> availableRenderers() {
        if(info != null) {
            return info;
        }

        synchronized (RendererInfoImpl.class) {
            if(info == null) {
                info = ServiceLoader.load(RendererInfo.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                    .toList();
            }
        }

        return info;
    }
}
