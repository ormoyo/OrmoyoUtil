package com.ormoyo.ormoyoutil.client.model.obj;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.IOUtils;
import org.lwjgl.util.vector.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

@SideOnly(Side.CLIENT)
public class OBJLoader
{
    private static final Pattern WHITE_SPACE = Pattern.compile("\\s+");

    /**
     * Loads a file from the resource location
     *
     * @param file The {@link ResourceLocation} to point to the obj file
     */
    public static OBJModel loadOBJModel(ResourceLocation file)
    {
        return loadOBJModel(file, true, true);
    }

    /**
     * Loads a file from the resource location
     *
     * @param file                    The {@link ResourceLocation} to point to the obj file
     * @param removeDuplicateVertices If true every duplicate vertex will not get loaded
     */
    public static OBJModel loadOBJModel(ResourceLocation file, boolean removeDuplicateVertices, boolean hasJsonFile)
    {
        IResource iresource = null;
        try
        {
            OBJModel model = new OBJModel();
            InputStream stream;
            String loc = "";
            try
            {
                loc = file.toString();
                iresource = Minecraft.getMinecraft().getResourceManager().getResource(file);
                stream = iresource.getInputStream();
            }
            catch (IOException e)
            {
                OrmoyoUtil.LOGGER.error("Cannot find obj file at location " + loc);
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String mtlName = null;
            Shape shape = null;
            Material currentMaterial = null;
            String line;
            try
            {
                while ((line = reader.readLine()) != null)
                {
                    String[] fields = WHITE_SPACE.split(line, 2);
                    String key = fields[0];
                    String data = fields[1];
                    String[] splitData = WHITE_SPACE.split(data);
                    if (key.equalsIgnoreCase("mtllib"))
                    {
                        String[] arr = line.replaceFirst("mtllib ", "").split(" ");
                        mtlName = arr[0];
                        IResource mtlResource = null;
                        InputStream mtlStream = null;
                        try
                        {
                            mtlResource = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(file.getNamespace(), file.getPath().substring(0, file.getPath().lastIndexOf("/") + 1) + mtlName));
                            mtlStream = mtlResource.getInputStream();
                        }
                        catch (IOException i)
                        {
                            OrmoyoUtil.LOGGER.error("Cannot find obj file at location " + loc.substring(0, loc.length() - 4) + mtlName);
                            return null;
                        }
                        BufferedReader mtlReader = new BufferedReader(new InputStreamReader(mtlStream));
                        String materialName = null;
                        Vector3f color = null;
                        ResourceLocation texture = null;
                        float alpha = 1;
                        String mtlLine = null;
                        try
                        {
                            while ((mtlLine = mtlReader.readLine()) != null)
                            {
                                if (mtlLine.isEmpty())
                                {
                                    continue;
                                }
                                String[] mtlfields = WHITE_SPACE.split(mtlLine, 2);
                                String mtlkey = mtlfields[0];
                                String mtldata = mtlfields[1];
                                String[] mtlsplitData = WHITE_SPACE.split(mtldata);
                                if (mtlkey.equalsIgnoreCase("newmtl"))
                                {
                                    if (materialName != null && color != null)
                                    {
                                        Material material;
                                        if (texture != null)
                                        {
                                            material = new Material(materialName, texture, color.x, color.y, color.z, alpha);
                                        }
                                        else
                                        {
                                            material = new Material(materialName, color.x, color.y, color.z, alpha);
                                        }
                                        model.addMaterial(material);
                                        texture = null;
                                    }
                                    materialName = mtlsplitData[0];
                                }
                                else if (mtlkey.equalsIgnoreCase("Kd"))
                                {
                                    color = new Vector3f(Float.parseFloat(mtlsplitData[0]), Float.parseFloat(mtlsplitData[1]), Float.parseFloat(mtlsplitData[2]));
                                }
                                else if (mtlkey.equalsIgnoreCase("d"))
                                {
                                    alpha = Float.parseFloat(mtlsplitData[0]);
                                }
                                else if (mtlkey.equalsIgnoreCase("map_Kd"))
                                {
                                    String string = mtlsplitData[0];
                                    if (string.startsWith(file.getNamespace() + ":"))
                                    {
                                        texture = new ResourceLocation(string);
                                    }
                                    else if (string.lastIndexOf('/') != -1)
                                    {
                                        texture = new ResourceLocation(file.getNamespace(), "textures/obj/" + string.substring(0, string.lastIndexOf('/')));
                                    }
                                    else
                                    {
                                        texture = new ResourceLocation(file.getNamespace(), "textures/obj/" + string);
                                    }
                                }
                            }
                            if (materialName != null && color != null)
                            {
                                Material material;
                                if (texture != null)
                                {
                                    material = new Material(materialName, texture, color.x, color.y, color.z, alpha);
                                }
                                else
                                {
                                    material = new Material(materialName, color.x, color.y, color.z, alpha);
                                }
                                model.addMaterial(material);
                                texture = null;
                            }
                        }
                        catch (IOException ioe)
                        {
                            OrmoyoUtil.LOGGER.error("Failed to read mtl file at location " + loc.substring(0, loc.length() - 4) + mtlName);
                            return null;
                        }
                        finally
                        {
                            IOUtils.closeQuietly(mtlResource);
                        }

                    }
                    if (key.equalsIgnoreCase("o"))
                    {
                        shape = new Shape(model, splitData[0]);
                        model.addShape(shape);
                    }
                    if (shape != null)
                    {
                        if (key.equalsIgnoreCase("v"))
                        {
                            Vertex ver = new Vertex(Float.parseFloat(splitData[0]), Float.parseFloat(splitData[1]), Float.parseFloat(splitData[2]));
                            if (removeDuplicateVertices)
                            {
                                if (!shape.getVertices().contains(ver))
                                {
                                    shape.addVertex(ver);
                                }
                            }
                            else
                            {
                                shape.addVertex(ver);
                            }
                        }
                        else if (key.equalsIgnoreCase("vt"))
                        {
                            TextureCoords coord = new TextureCoords(Float.parseFloat(splitData[0]), Float.parseFloat(splitData[1]));
                            shape.addTexCoords(coord);
                        }
                        else if (key.equalsIgnoreCase("vn"))
                        {
                            Normal nor = new Normal(Float.parseFloat(splitData[0]), Float.parseFloat(splitData[1]), Float.parseFloat(splitData[2]));
                            shape.addNormal(nor);
                        }
                        else if (key.equalsIgnoreCase("usemtl"))
                        {
                            currentMaterial = model.getMaterialByName(splitData[0]);
                        }
                        else if (key.equalsIgnoreCase("f"))
                        {
                            Face face;
                            PolygonType isValid =
                                    splitData.length == 3 ? PolygonType.TRINGLE :
                                            splitData.length == 4 ? PolygonType.QUAD :
                                                    PolygonType.POLYGON;

                            if (currentMaterial != null)
                            {
                                face = new Face(shape,
                                        isValid,
                                        currentMaterial);
                            }
                            else
                            {
                                face = new Face(shape,
                                        isValid);
                            }

                            for (int i = 0; i < splitData.length; i++)
                            {
                                Vertex vertex = null;
                                TextureCoords coords = null;
                                Normal normal = null;
                                String[] split = splitData[i].split("/");
                                for (int y = 0; y < split.length; y++)
                                {
                                    switch (y)
                                    {
                                        case 0:
                                            for (Vertex v : shape.getVertices())
                                            {
                                                if (v.getIndex() == Integer.parseInt(split[y]))
                                                {
                                                    vertex = v;
                                                }
                                            }
                                            break;
                                        case 1:
                                            for (TextureCoords tex : shape.getTextureCoords())
                                            {
                                                if (tex.getIndex() == Integer.parseInt(split[y]))
                                                {
                                                    coords = tex;
                                                }
                                            }
                                            break;
                                        case 2:
                                            for (Normal n : shape.getNormals())
                                            {
                                                if (n.getIndex() == Integer.parseInt(split[y]))
                                                {
                                                    normal = n;
                                                }
                                            }
                                            break;
                                    }
                                }
                                if (vertex != null && coords != null && normal != null)
                                {
                                    face.appendWithoutParent(vertex, coords, normal);
                                }
                            }
                            shape.addFace(face);
                        }
                    }
                }
            }
            catch (IOException io)
            {
                OrmoyoUtil.LOGGER.error("Failed to read obj file at location " + loc);
                io.printStackTrace();
                return null;
            }
            InputStream jsonStream = null;
            if (hasJsonFile)
            {
                IResource jsonResource = null;
                try
                {
                    jsonResource = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(file.getNamespace(), file.getPath().substring(0, file.getPath().lastIndexOf(".")) + ".json"));
                    jsonStream = jsonResource.getInputStream();
                }
                catch (IOException io)
                {
                    OrmoyoUtil.LOGGER.error("Failed to read json file at location " + loc.substring(0, loc.length() - 4) + ".json");
                }
                finally
                {
                    IOUtils.closeQuietly(jsonResource);
                }
            }
            if (jsonStream != null)
            {
                InputStreamReader r = new InputStreamReader(jsonStream);
                JsonStreamParser parser = new JsonStreamParser(r);
                while (parser.hasNext())
                {
                    JsonElement e = parser.next();
                    if (e.isJsonObject())
                    {
                        JsonObject json = e.getAsJsonObject();
                        String name = json.get("name").getAsString();
                        String parent = json.get("parent").getAsString();
                        if (name != null && parent != null)
                        {
                            Shape s = model.getShapeByName(name);
                            s.addChildShape(shape);
                        }
                    }
                }
            }
            IOUtils.closeQuietly(jsonStream);
            return model;
        }
        finally
        {
            IOUtils.closeQuietly(iresource);
        }
    }
}
