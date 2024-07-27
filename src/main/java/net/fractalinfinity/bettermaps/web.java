package net.fractalinfinity.bettermaps;

import net.coobird.thumbnailator.Thumbnails;
import spark.utils.IOUtils;

import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.fractalinfinity.bettermaps.videoprocessor.extractFramez;
import static spark.Spark.*;

public class web {
    public void runweb() throws IOException {
        System.out.println("web");
        port(4567);
        // Serve static files (home.html)
        // Route to serve home.html
        get("/", (req, res) -> {
            res.type("text/html");
            ClassLoader classloader = Bettermaps.class.getClassLoader();
            InputStream is = classloader.getResourceAsStream("home.html");

            return new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.joining("\n"));
        });

        // Route to handle file upload
        post("/upload", "multipart/form-data", (request, response) -> {


            String location = "mapimg/temp";          // the directory location where files will be stored

            MultipartConfigElement multipartConfigElement = new MultipartConfigElement(location);
            request.raw().setAttribute("org.eclipse.jetty.multipartConfig",
                    multipartConfigElement);
            String id = IOUtils.toString(request.raw().getPart("id").getInputStream());
            System.out.println(id);
            String mmcfg = IOUtils.toString(request.raw().getPart("multimapconfig").getInputStream());
            int Width = 128;
            int Height = 128;
            long[][] mmtable = {{Long.parseLong(id)}};
            String mediapath = "mapimg/media/";
            if (!Objects.equals("", mmcfg)) {
                 mmtable = table(mmcfg, "|", ",");
                System.out.println(Arrays.deepToString(mmtable));

                //System.out.println((((int[]) mmconfigdict.get("1"))[0]));
                Width = mmtable[0].length * 128;
                Height = mmtable.length * 128;
            }
            Part uploadedFile = request.raw().getPart("media");
            if (uploadedFile.getContentType().contains("image")) {
                System.out.println("image Upload");

                ImageIO.write(Thumbnails.of(uploadedFile.getInputStream()).size(Width, Height).keepAspectRatio(false).outputQuality(1.0).outputFormat("png").asBufferedImage(), "png", new File(mediapath + id + ".png"));
                uploadedFile.delete();
                try {
                    for (File subfile : new File(mediapath + id).listFiles()) {
                        subfile.delete();
                    }
                    new File(mediapath + id).delete();
                } catch (Exception ignored) {
                }
                ArrayList<Object> arr = new ArrayList<>(1);
                arr.addFirst(true);
                arr.add(1, mediapath + id + ".png");
                Bettermaps.playingmedia.put(mmtable, arr);
                Bettermaps.playmedia(new File(mediapath + id + ".png"),mmtable);
                return "image upload OK";
            }
            if (!uploadedFile.getContentType().contains("video")) {
                return "not a video or image upload";
            }
            Path out = Paths.get("mapimg/temp/" + id + ".mp4");
            try (final InputStream in = uploadedFile.getInputStream()) {
                Files.copy(in, out);
                uploadedFile.delete();
            }
            Files.createDirectories(Paths.get(mediapath + id));
            try {
                extractFramez("mapimg/temp/" + id + ".mp4", Width, Height, 20, id, mediapath);
                ArrayList<Object> arr = new ArrayList<>(1);
                arr.addFirst(true);
                arr.add(1, mediapath + id);
                Bettermaps.playingmedia.put(mmtable, arr);
                Bettermaps.playmedia(new File(mediapath + id),mmtable);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            multipartConfigElement = null;
            uploadedFile.delete();
            return "video upload OK";
        });
    }
    public static long[][] table(String source, String outerdelim, String innerdelim) {
        // outerdelim may be a group of characters
        String[] sOuter = source.split("[" + outerdelim + "]");
        int size = sOuter.length;
        // one dimension of the array has to be known on declaration:
        long[][] result = new long[size][];
        int count = 0;
        for (String line : sOuter) {
            result[count] = Arrays.stream(line.split(innerdelim)).mapToLong( Long::parseLong).toArray();
            ++count;
        }
        return result;
    }
}
