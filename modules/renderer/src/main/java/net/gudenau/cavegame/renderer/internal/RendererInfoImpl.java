package net.gudenau.cavegame.renderer.internal;

import net.gudenau.cavegame.renderer.RendererInfo;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    public static RendererInfo of(@NotNull List<@NotNull RendererInfo> rendererInfo) {
        var filteredInfo = rendererInfo.stream()
            .filter(RendererInfo::supported)
            .toList();

        if(filteredInfo.isEmpty()) {
            throw new IllegalArgumentException("None of the passed RenderInfo are supported on this platform");
        } else if(filteredInfo.size() == 1) {
            return filteredInfo.get(0);
        }

        var nameBuilder = new StringBuilder("MergedInfo{");
        filteredInfo.forEach((info) -> nameBuilder.append(info.name()).append(','));
        nameBuilder.setCharAt(nameBuilder.length() - 1, '}');

        return new MergedRendererInfo(nameBuilder.toString(), filteredInfo);
    }
}
