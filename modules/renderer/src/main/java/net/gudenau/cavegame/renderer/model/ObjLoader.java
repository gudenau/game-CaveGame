package net.gudenau.cavegame.renderer.model;

import net.gudenau.cavegame.renderer.BufferBuilder;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// A crappy OBJ loader, should probably be replaced/deleted
public final class ObjLoader {
    private ObjLoader() {
        throw new AssertionError();
    }

    @NotNull
    @Contract("_, _ -> param1")
    public static BufferBuilder load(@NotNull BufferBuilder builder, @NotNull Identifier identifier) throws IOException {
        try(var reader = ResourceLoader.reader(identifier.normalize("model", ".obj"))) {
            List<Vector3f> vertices = new ArrayList<>();
            List<Vector2f> textureCoords = new ArrayList<>();
            //List<Vector3f> normals = new ArrayList<>();

            String line;
            while((line = reader.readLine()) != null) {
                int octothorpe = line.indexOf('#');
                if(octothorpe != -1) {
                    line = line.substring(0, octothorpe);
                }
                if(line.isBlank()) {
                    continue;
                }

                var split = line.split(" ");
                switch(split[0]) {
                    case "v" -> vertices.add(new Vector3f(
                        Float.parseFloat(split[1]),
                        Float.parseFloat(split[2]),
                        Float.parseFloat(split[3])
                    ));

                    case "vt" -> textureCoords.add(new Vector2f(
                        Float.parseFloat(split[1]),
                        1 - Float.parseFloat(split[2])
                    ));

                    /*
                    case "vn" -> normals.add(new Vector3f(
                        Float.parseFloat(split[1]),
                        Float.parseFloat(split[2]),
                        Float.parseFloat(split[3])
                    ));
                     */

                    case "f" -> {
                        for(int i = 1, length = split.length; i < length; i++) {
                            var part = split[i].split("/");
                            var vertex = vertices.get(Integer.parseInt(part[0]) - 1);
                            var texture = textureCoords.get(Integer.parseInt(part[1]) - 1);
                            //var normal = normals.get(Integer.parseInt(part[2]) - 1);

                            builder.position(vertex)
                                .textureCoord(texture)
                                .color(1, 1, 1)
                                .next();
                        }
                    }
                }
            }
        }

        return builder;
    }
}
