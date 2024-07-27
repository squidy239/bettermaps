
package net.fractalinfinity.bettermaps;


import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static net.fractalinfinity.bettermaps.web.runweb;

public final class Bettermaps extends JavaPlugin implements Listener {
    public static ConcurrentHashMap<Long, MapView> mapviewdict = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<long[][], List<Object>> playingmedia = new ConcurrentHashMap<>();
    static ImageUtils imageutils = new ImageUtils();


    @Override
    public void onEnable() {
        try {
            Files.createDirectories(Paths.get("mapimg/media"));
            Files.createDirectories(Paths.get("mapimg/temp"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (new File("mapimg/playingmedia.dat").exists()) {
            System.out.println("media exists");
            try {
                FileInputStream fileIn = new FileInputStream("mapimg/playingmedia.dat");
                ObjectInputStream in = new ObjectInputStream(fileIn);
                playingmedia = (ConcurrentHashMap<long[][], List<Object>>) in.readObject();
                in.close();
                fileIn.close();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(e);
            }
            playingmedia.forEach((k, v)->{List<Object> data = v;if ((Boolean) data.getFirst()){try {System.out.println(Arrays.deepToString(k));playmedia((File)data.get(1),k);} catch (IOException e) {JavaPlugin.getPlugin(this.getClass()).getLogger().warning(data.get(1).toString()+" does not exist!");playingmedia.remove(k);}}});
        }
        //try {playmedia(new File("mapimg/vids/128"), new long[][]{{172, 173,174,175,176,177,178,179}, {180, 181,182,183,184,185,186,187},{188,189,190,191,192,193,194,195},{196,197,198,199,200,201,202,203},{204,205,206,207,208,209,210,211}});} catch (IOException e) {throw new RuntimeException(e);}
        System.out.println("started");
        try {
            runweb();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        getServer().getPluginManager().registerEvents(this, this);

    }



    @EventHandler
    public void mapevent(MapInitializeEvent mapp) {mapviewdict.put((long) mapp.getMap().getId(),mapp.getMap());}

    private static boolean removemaprenderers(long ids[][]){
        for (long[] ii:ids){
            for (long id: ii){ MapView mv = mapviewdict.get(id);
                if (mv != null){
                    List<MapRenderer> mvrendererlist = mv.getRenderers();
                    for (MapRenderer i : mvrendererlist) mv.removeRenderer(i);}
                else return true;}

        }
        return false;
    }

    public static void setmapimg(BufferedImage image, long id, int x, int y) {
        List<Player> pl = (List<Player>) Bukkit.getOnlinePlayers();
        boolean islocked = true;
        for (int i = 0; i < pl.size(); i++) {
            if (((CraftPlayer) pl.get(i)).getHandle().connection != null) {
                Collection<MapDecoration> icons = new ArrayList();
                byte[] renderedimage = imageutils.imageToBytes(image);
                ClientboundMapItemDataPacket packet = new ClientboundMapItemDataPacket(new MapId((int) id), MapView.Scale.valueOf("CLOSEST").getValue(), islocked, icons, new MapItemSavedData.MapPatch(x, y, 128, 128, renderedimage));
                ((CraftPlayer) pl.get(i)).getHandle().connection.send(packet);
            }
        }
    }


    public static void putimageonmaps(BufferedImage image, long[][] ids) {
        Thread.ofVirtual().start(() -> {
            for (int l = 0; l < ids.length; l++) {
                int finalL = l;
                IntStream.range(0,ids[finalL].length).parallel().forEach(i->{
                    setmapimg(image.getSubimage(i * 128, finalL * 128, 128, 128), ids[finalL][i], 0, 0);// System.out.println(ids[finalL][finalI] + " dosent exist");

                });
            }
        });
    }
    private static boolean isoverlap(long[][] first, long[][] second) {
        if (first == null || second == null || first.length == 0 || second.length == 0) {
            return false;
        }

        int firstRows = first.length;
        int firstCols = first[0].length;
        int secondRows = second.length;
        int secondCols = second[0].length;

        for (int i = 0; i < firstRows; i++) {
            for (int j = 0; j < firstCols; j++) {
                for (int k = 0; k < secondRows; k++) {
                    for (int l = 0; l < secondCols; l++) {
                        if (first[i][j] == second[k][l]) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public static void playmedia(File path, long[][] ids) throws IOException {
        if (!path.exists()) throw new IOException("path " + path + " does not exist");
        ArrayList<Object> arr = new ArrayList<>(1);
        // find if any overlap in ids and playingmedia and stop playing other if there is
        Enumeration<long[][]> keys = playingmedia.keys();
        while (keys.hasMoreElements()) {
            long[][] key = keys.nextElement();
            if (isoverlap(key, ids)) {
                playingmedia.remove(key);
            }
        }
        arr.addFirst(true);
        arr.add(1, path);
        playingmedia.put(ids, arr);
        AtomicBoolean hasrenderers = new AtomicBoolean(true);
        if (path.isDirectory()) {
            Thread.ofVirtual().start(() -> {
                long frame = 0;
                while (playingmedia.containsKey(ids) && (boolean) playingmedia.get(ids).getFirst()) {
                    if (hasrenderers.get()) {hasrenderers.set(removemaprenderers(ids));};
                    try {
                        File file = new File(path, "/" + frame + ".png");
                        if (!file.exists()) {
                            frame = 0;
                            file = new File(path, "/" + frame + ".png");
                        }
                        putimageonmaps(Scalr.resize(ImageIO.read(file), Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_EXACT, ids[0].length * 128, ids.length * 128), ids);
                        Thread.sleep(40);
                        frame++;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } else if (path.isFile()) {
            // is image
            Thread.ofVirtual().start(() -> {
                long imgmodified;
                long lastimgmodified = 0;
                BufferedImage scaledimage = null;
                while (playingmedia.containsKey(ids) && (boolean) playingmedia.get(ids).getFirst()) {
                    if (hasrenderers.get()) {hasrenderers.set(removemaprenderers(ids));};
                    try {
                        imgmodified = path.lastModified();
                        if (imgmodified != lastimgmodified) putimageonmaps( Scalr.resize(ImageIO.read(path), Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_EXACT, ids[0].length * 128, ids.length * 128), ids);
                        lastimgmodified = imgmodified;
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

        }
    }



    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try {
            FileOutputStream fileOut = new FileOutputStream("mapimg/playingmedia.dat");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(playingmedia);
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