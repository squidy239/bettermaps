package net.fractalinfinity.bettermaps;

import net.coobird.thumbnailator.Thumbnails;
import org.bukkit.Bukkit;
import spark.utils.IOUtils;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static net.fractalinfinity.bettermaps.Bettermaps.playingmedia;
import static net.fractalinfinity.bettermaps.mapdraw.isoverlap;
import static net.fractalinfinity.bettermaps.videoprocessor.ExtractFrames;
import static spark.Spark.*;

public class web {

    public static void main(String[] args) throws IOException {
        runweb();
    }
    public static void runweb() throws IOException {
        System.out.println("web");
        port(4567);
        ClassLoader classloader = web.class.getClassLoader();
        InputStream is = classloader.getResourceAsStream("home.html");

        String htmlpage = new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining("\n"));
        get("/", (req, res) -> {
            res.type("text/html");
            return htmlpage;
        });
        before((request, response) -> {
            if (request.raw().getContentType() != null && request.raw().getContentType().contains("multipart/form-data")) {
                MultipartConfigElement multipartConfigElement = new MultipartConfigElement("mapimg/temp",
                        1024L * 1024 * 50000, // Max file size (5000 MB)
                        1024L * 1024 * 50000, // Max request size (5000 MB)
                        1024 * 1024 * 10000);
                request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
            }
        });

        // Route to handle file upload
        post("/upload", "multipart/form-data", (request, response) -> {
            try {
                int[][] mapconfig = table(IOUtils.toString(request.raw().getPart("mapconfig").getInputStream()), "|", ",");
                String name = IOUtils.toString(request.raw().getPart("name").getInputStream()).replaceAll("[^a-zA-Z0-9]", "");
                Part uploadedFile = request.raw().getPart("media");
                int Width = mapconfig[0].length * 128;
                int Height = mapconfig.length * 128;
                String mediapath = "mapimg/media/";
                if (uploadedFile.getContentType().contains("image")) {
                    System.out.println("image Upload");
                        try {

                            BufferedImage bufferedimage = Thumbnails.of(uploadedFile.getInputStream()).size(Width, Height).keepAspectRatio(false).outputQuality(1.0).outputFormat("png").asBufferedImage();
                            ImageUtils.ConvertAndWriteMinecraftImage(bufferedimage,new File(mediapath + name + ".png"));
                            //ImageIO.write(ImageUtils.imageToimageBytes(bufferedimage), "png", new File(mediapath + name + ".png"));
                        uploadedFile.delete();
                            Enumeration<int[][]> keys = playingmedia.keys();
                            while (keys.hasMoreElements()) {
                                int[][] key = keys.nextElement();
                                if (isoverlap(key, mapconfig)) {
                                    playingmedia.remove(key);
                                }
                            }
                            File[] files = new File(mediapath + name).listFiles();
                            if (files != null){
                            for (File subfile : files) {
                                subfile.delete();
                            }}
                            new File(mediapath + name).delete();
                        Bettermaps.PlayMedia(new File(mediapath + name + ".png"), mapconfig, 20F);
                        } catch (Exception e) {System.out.println(e+" imgupload");e.printStackTrace();};
                    return "image upload OK";
                }
                else if (uploadedFile.getContentType().contains("video")){
                    System.out.println("video Upload");
                        try {
                    Path out = Paths.get("mapimg/temp/" + name + ".mp4");
                    final InputStream in = uploadedFile.getInputStream();
                        Files.copy(in, out);
                        uploadedFile.delete();
                    Files.createDirectories(Paths.get(mediapath + name));
                            Enumeration<int[][]> keys = playingmedia.keys();
                            while (keys.hasMoreElements()) {
                                int[][] key = keys.nextElement();
                                if (isoverlap(key, mapconfig)) {
                                    playingmedia.remove(key);
                                }
                            }
                            new File(mediapath + name + ".png").delete();
                        ExtractFrames("mapimg/temp/" + name + ".mp4", Width, Height, 20, name, mediapath);
                        Bukkit.broadcastMessage("upload done, now playing:"+name);
                        Bettermaps.PlayMedia(new File(mediapath + name),mapconfig, 20F);
                    uploadedFile.delete();}catch (Exception e) {System.out.println("Error prossesing video"+e);};
                    return "video upload prossesed";
                }

                return name +" was not a image or video file";
            } catch (Exception e) {
                response.status(400);
                return "Error: " + e;
            }
    });


}
    public static int[][] table(String source, String outerdelim, String innerdelim) {
        String[] sOuter = source.split("[" + outerdelim + "]");
        int size = sOuter.length;
        int[][] result = new int[size][];
        int count = 0;
        for (String line : sOuter) {
            result[count] = Arrays.stream(line.split(innerdelim)).mapToInt(Integer::parseInt).toArray();
            ++count;
        }
        return result;
    }
}
