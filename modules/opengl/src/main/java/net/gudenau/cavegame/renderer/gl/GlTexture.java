package net.gudenau.cavegame.renderer.gl;

import net.gudenau.cavegame.renderer.texture.PngReader;
import net.gudenau.cavegame.renderer.texture.Texture;

import static org.lwjgl.opengl.GL44C.*;

public class GlTexture implements Texture {
    private final int handle;

    public GlTexture(GlState state, PngReader.Result result) {
        handle = glGenTextures();
        var boundTexture = state.boundTexture();
        state.bindTexture(handle);
        try {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            var glFormat = switch(result.format()) {
                case RGBA -> GL_RGBA;
                case RGB -> GL_RGB;
                case GRAYSCALE -> GL_ALPHA;
            };
            glTexImage2D(GL_TEXTURE_2D, 0, glFormat, result.width(), result.height(), 0, glFormat, GL_UNSIGNED_BYTE, result.pixels());
            glGenerateMipmap(GL_TEXTURE_2D);
        } finally {
            state.bindTexture(boundTexture);
        }
    }

    @Override
    public void close() {
        glDeleteTextures(handle);
    }

    public int handle() {
        return handle;
    }
}
