package net.fractalinfinity.bettermaps;


import net.coobird.thumbnailator.Thumbnails;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import spark.utils.IOUtils;

import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static spark.Spark.*;

public final class Bettermaps extends JavaPlugin implements Listener {
    static JSONObject mmconfigdict = new JSONObject();
    static ScheduledExecutorService scheduler
            = Executors.newScheduledThreadPool(8);
    JSONObject bufferdict = new JSONObject();
    JSONObject framedict = new JSONObject();

    private static void web() throws IOException {
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
            String vidpath = "mapimg/vids/";
            String imgpath = "mapimg/images/";
            if (!Objects.equals("", mmcfg)) {
                String[][] mmtable = table(mmcfg, "|", ",");
                System.out.println(Arrays.deepToString(mmtable));
                for (int i = 0; i < mmtable.length; i++) {
                    for (int n = 0; n < mmtable[i].length; n++) {
                        System.out.println(mmtable[i][n]);
                        mmconfigdict.put(mmtable[i][n], new int[]{128 * List.of(mmtable[i]).indexOf(mmtable[i][n]), 128 * i, Integer.parseInt(mmtable[0][0])});
                    }
                }
                System.out.println((((int[]) mmconfigdict.get("1"))[0]));
                Width = mmtable[0].length * 128;
                Height = mmtable.length * 128;
                vidpath = "mapimg/multimapvids/";
                imgpath = "mapimg/multimapimgs/";
            }
            Part uploadedFile = request.raw().getPart("media");
            if (uploadedFile.getContentType().contains("image")) {
                System.out.println("image Upload");

                ImageIO.write(Thumbnails.of(uploadedFile.getInputStream()).size(Width, Height).keepAspectRatio(false).outputQuality(1.0).outputFormat("png").asBufferedImage(), "png", new File(imgpath + id + ".png"));
                uploadedFile.delete();
                try {
                    for (File subfile : new File(vidpath + id).listFiles()) {
                        subfile.delete();
                    }
                    new File(vidpath + id).delete();
                } catch (Exception ignored) {
                }
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
            Files.createDirectories(Paths.get(vidpath + id));
            try {
                extractFramez("mapimg/temp/" + id + ".mp4", Width, Height, 20, id, vidpath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            multipartConfigElement = null;
            uploadedFile.delete();
            return "video upload OK";
        });
    }

    public static void extractFramez(String videoFilePath, int width, int height, double frameRate, String id, String path) throws
            IOException, JCodecException {
        try (SeekableByteChannel channel = NIOUtils.readableFileChannel(videoFilePath)) {
            FrameGrab grab = FrameGrab.createFrameGrab(channel);
            double videoFrameRate = grab.getVideoTrack().getMeta().getTotalFrames() / grab.getVideoTrack().getMeta().getTotalDuration();
            int frameInterval = (int) Math.round(videoFrameRate / frameRate);
            try {
                for (File subfile : new File(path + id).listFiles()) {
                    subfile.delete();
                }
            } catch (Exception ignored) {
            }
            int frameNumber = 0;
            int savedFrameNumber = 0;
            Picture f;
            int totalframenum = grab.getVideoTrack().getMeta().getTotalFrames();
            while (null != (f = grab.getNativeFrame())) {
                if (frameNumber % frameInterval == 0) {
                    int finalSavedFrameNumber = savedFrameNumber;
                    saveFrame(f, finalSavedFrameNumber, id, width, height, path);
                    savedFrameNumber++;
                }
                frameNumber++;
            }

        } finally {
            System.out.println("done");
            Files.deleteIfExists(Path.of(videoFilePath));
        }
    }

    private static void saveFrame(Picture picture, int frameNumber, String id, int width, int height, String outdir) {
        try {
            BufferedImage bufferedImage = Thumbnails.of(AWTUtil.toBufferedImage(picture))
                    .size(width, height)
                    .keepAspectRatio(false)
                    .outputQuality(1.0)
                    .outputFormat("png")
                    .asBufferedImage();

            File outputDir = new File(outdir + id);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            File outputFile = new File(outputDir, frameNumber + ".png");
            ImageIO.write(bufferedImage, "png", outputFile);
            System.out.println("Saved frame_" + frameNumber + ".png");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save frame_" + frameNumber + ".png", e);
        }
    }

    public static String[][] table(String source, String outerdelim, String innerdelim) {
        // outerdelim may be a group of characters
        String[] sOuter = source.split("[" + outerdelim + "]");
        int size = sOuter.length;
        // one dimension of the array has to be known on declaration:
        String[][] result = new String[size][];
        int count = 0;
        for (String line : sOuter) {
            result[count] = line.split(innerdelim);
            ++count;
        }
        return result;
    }

    @Override
    public void onEnable() {
        try {
            Files.createDirectories(Paths.get("mapimg/images"));
            Files.createDirectories(Paths.get("mapimg/vids"));
            Files.createDirectories(Paths.get("mapimg/multimapvids"));
            Files.createDirectories(Paths.get("mapimg/multimapimgs"));
            Files.createDirectories(Paths.get("mapimg/temp"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (new File("mapimg/mmconfigdict").exists()) {
            System.out.println("mmconfig exists");
            try {
                FileInputStream fileIn = new FileInputStream("mapimg/mmconfigdict");
                ObjectInputStream in = new ObjectInputStream(fileIn);
                mmconfigdict = (JSONObject) in.readObject();
                in.close();
                fileIn.close();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(e);
            }
        }
        System.out.println("started");
        try {
            web();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        getServer().getPluginManager().registerEvents(this, this);

    }

    private void setiimgbufferfromfile(int id, String path) {
        if (Files.exists(Paths.get(path + "/images/" + id + ".png")) && !Files.exists(Paths.get(path + "/vids/" + id))) {
            try {
                bufferdict.put(id, ImageIO.read(new File(path + "/images/" + id + ".png")));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @EventHandler
    public void mapevent(MapInitializeEvent mapp) {
        int id = mapp.getMap().getId();
        getLogger().info("map id " + id + " initalized");
        //Player[] playr = {null};
        MapCanvas[] mc = {null};
        //MapView[] mapview = {null};
        String imgpath = "./mapimg";
        int[] m = {1};
        int[] po1 = {0};
        int[] po2 = {0};
        int[] mmvid = {0};
        setiimgbufferfromfile(id, imgpath);
        if (mmconfigdict.containsKey(String.valueOf(id))) {
            m[0] = new File(imgpath + "/multimapvids/" + ((int[]) mmconfigdict.get(String.valueOf(id)))[2]).listFiles().length;
        } else if (new File(imgpath + "/vids/" + id).exists()) {
            m[0] = new File(imgpath + "/vids/" + id).listFiles().length;
        }
        //Bukkit.getScheduler().runTaskTimer(this, () -> playr[0].sendMap(mapview[0]), 1, 1);
        scheduler.scheduleWithFixedDelay(() -> {
            if (mmconfigdict.containsKey(String.valueOf(id))) {
                mmvid[0] = ((int[]) mmconfigdict.get(String.valueOf(id)))[2];
                m[0] = new File(imgpath + "/multimapvids/" + mmvid[0]).listFiles().length;
                po1[0] = ((int[]) mmconfigdict.get(String.valueOf(id)))[0];
                po2[0] = ((int[]) mmconfigdict.get(String.valueOf(id)))[1];
            } else if (new File(imgpath + "/vids/" + id).exists()) {
                m[0] = new File(imgpath + "/vids/" + id).listFiles().length;
            }
        }, 100, 1000, TimeUnit.MILLISECONDS);
        scheduler.scheduleWithFixedDelay(() -> setiimgbufferfromfile(id, imgpath), 20, 20, TimeUnit.SECONDS);
        mapp.getMap().getRenderers().clear();
        mapp.getMap().addRenderer(new MapRenderer() {
            @Override
            public void render(@NotNull MapView mapView, @NotNull MapCanvas mapCanvas, @NotNull Player player) {
                //playr[0] = player;
                //mapview[0] = mapView;
                mc[0] = mapCanvas;
            }
        });
        scheduler.scheduleWithFixedDelay(() -> {
            //System.out.println("mmvid: "+mmvid[0] + "  exists = "+ Files.exists(Paths.get(imgpath + "/multimapvids/" + mmvid[0])) + "   and map id:" + id);
            if (mmvid[0] != 0 && Files.exists(Paths.get(imgpath + "/multimapvids/" + mmvid[0]))) {

                try {
                    mc[0].drawImage(0, 0, ((BufferedImage) bufferdict.get(mmvid[0])).getSubimage(po1[0], po2[0], 128, 128));
                } catch (NullPointerException ignored) {
                }
                try {
                    if (id == mmvid[0]) {
                        int f;
                        if (framedict.containsKey(id)) {
                            f = (int) framedict.get(id);
                        } else {
                            f = 0;
                        }
                        if (f >= m[0] - 1) {
                            //System.out.println("m[0]: "+m[0]);
                            f = 0;
                        }
                        bufferdict.put(id, ImageIO.read(new File(imgpath + "/multimapvids/" + mmvid[0] + "/" + f + ".png")));
                        framedict.put(id, f + 1);
                    }
                } catch (Exception e) {
                    getLogger().warning(e.toString());
                }
            } else if (Files.exists(Paths.get(imgpath + "/vids/" + id))) {
                try {
                    int f;
                    BufferedImage bb = null;
                    if (framedict.containsKey(id)) {
                        f = (int) framedict.get(id);
                    } else {
                        f = 0;
                    }
                    try {
                        bb = ImageIO.read(new File(imgpath + "/vids/" + id + "/" + f + ".png"));
                        mc[0].drawImage(0, 0, bb);
                    } catch (Exception e) {
                        getLogger().warning("error reading image, last frame: " + f + "max frame:" + m[0] + " error : " + e);
                    }

                    if (f >= m[0] - 1) {
                        //System.out.println("m[0]: "+m[0]);
                        f = 0;
                    }
                    framedict.put(id, f + 1);
                } catch (Exception e) {
                    getLogger().warning(e.toString());
                }
            } else if (bufferdict.containsKey(id)) {
                mc[0].drawImage(0, 0, (BufferedImage) bufferdict.get(id));
            }
        }, 1000, 50, TimeUnit.MILLISECONDS);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try {
            FileOutputStream fileOut = new FileOutputStream("mapimg/mmconfigdict");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(mmconfigdict);
            out.close();
            fileOut.close();
            for (File subfile : new File("mapimg/temp").listFiles()) {
                subfile.delete();
            }
            new File("mapimg/temp").delete();
        } catch (Exception e) {
            System.out.println(e);
        }
    }


}

    /*
    public static void extractFrames(String videoFilePath,int width,int height, double frameRate,String id) throws IOException, JCodecException {
        SeekableByteChannel channel = null;
        try {
            channel = NIOUtils.readableFileChannel(videoFilePath);
            FrameGrab grab = FrameGrab.createFrameGrab(channel);
            double videoFrameRate = grab.getVideoTrack().getMeta().getTotalFrames() / grab.getVideoTrack().getMeta().getTotalDuration();
            int frameInterval = (int) Math.round(videoFrameRate / frameRate);

            Picture picture;
            int frameNumber = 0;
            int savedFrameNumber = 0;
            while (null != (picture = grab.getNativeFrame())) {
                if (frameNumber % frameInterval == 0) {

                    int finalSavedFrameNumber = savedFrameNumber;
                    Picture finalPicture = picture;
                    scheduler.execute(()-> {
                        BufferedImage bufferedImage = null;
                        try {
                            bufferedImage = Thumbnails.of(AWTUtil.toBufferedImage(finalPicture)).size(128, 128).keepAspectRatio(false).outputQuality(1.0).outputFormat("png").asBufferedImage();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        File outputfile = new File("mapimg/vids/" + id + "/", +finalSavedFrameNumber + ".png");
                                try {
                                    ImageIO.write(bufferedImage, "png", outputfile);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                System.out.println("Saved frame_" + finalSavedFrameNumber + ".png");
                            });
                        savedFrameNumber++;
                }
                frameNumber++;
            }
        } finally {
            System.out.println("done");
            if (channel != null) {
                channel.close();
                Files.delete(new File(videoFilePath).toPath());
            }
        }
    }
    private static void vid2img(@NotNull String videoFilestr, int width , int height, double targetFrameRate, String id) throws IOException {
        ArrayList<BufferedImage> images = new ArrayList<>();
        File videoFile = new File(videoFilestr);
        VideoCapture videoCapture = new VideoCapture(videoFile.getAbsolutePath());
        Files.delete(videoFile.toPath());

        if (!videoCapture.isOpened()) {
            System.out.println("Error: Could not open video file.");
            return;
        }

        double originalFrameRate = videoCapture.get(opencv_videoio.CAP_PROP_FPS);

        int frameSkipInterval = (int) (originalFrameRate / targetFrameRate);

        int frameCount = 0;

        while (true) {
            Mat frame = new Mat();
            boolean isRead = videoCapture.read(frame);

            if (!isRead || frame.empty()) {
                break;
            }
            if (frameCount % frameSkipInterval == 0) {
                Mat resizedFrame = new Mat();
                opencv_imgproc.resize(frame, resizedFrame, new Size(width, height));
                byte[] mob = new byte[(int) resizedFrame.arraySize()];
                opencv_imgcodecs.imencode(".png", resizedFrame, mob);
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(mob));
                images.add(image);
            }
            frameCount++;
        }

        videoCapture.release();
        System.out.println("images");
        Files.createDirectories(Paths.get("mapimg/vids/"+id));
        for (int i = 0; i < images.size(); i++) {
            try {ImageIO.write(images.get(i), "png", new File( "mapimg/vids/"+id+"/"+ i + ".png"));} catch (Exception e){System.out.println("error writing image");}

        }
    }
}
*/