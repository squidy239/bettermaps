package net.fractalinfinity.bettermaps;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class Bettermaps extends JavaPlugin implements Listener {
    JSONObject bufferdict = new JSONObject();

    JSONObject framedict = new JSONObject();
    ScheduledExecutorService scheduler
            = Executors.newScheduledThreadPool(8);

    @Override
    public void onEnable() {
        try {
            Files.createDirectories(Paths.get("mapimg/images"));
            Files.createDirectories(Paths.get("mapimg/vids"));

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
        Player[] playr = {null};
        MapCanvas[] mc = {null};
        MapView[] mapview = {null};
        boolean[] onn = {true};
        String imgpath = "./mapimg";
        int[] m = {1};
        if (new File(imgpath + "/vids/" + id).exists()){m[0] = new File(imgpath + "/vids/" + id).listFiles().length;}
        Bukkit.getScheduler().runTaskTimer(this, () -> playr[0].sendMap(mapview[0]), 1, 1);
        scheduler.scheduleWithFixedDelay(()->{if (new File(imgpath + "/vids/" + id).exists()){onn[0] = true;m[0] = new File(imgpath + "/vids/" + id).listFiles().length;}},5000,5000,TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(() -> setiimgbufferfromfile(id, imgpath), 500, 500, TimeUnit.MILLISECONDS);
        mapp.getMap().getRenderers().clear();
        mapp.getMap().addRenderer(new MapRenderer() {
            @Override
            public void render(@NotNull MapView mapView, @NotNull MapCanvas mapCanvas, @NotNull Player player) {
                if (onn[0] == true) {
                    playr[0] = player;
                    mapview[0] = mapView;
                    onn[0] = false;
                    mc[0] = mapCanvas;
                }
            }
        });
        scheduler.scheduleAtFixedRate(() -> {
            if (Files.exists(Paths.get(imgpath + "/vids/" + id))) {
                try {
                    int f;
                    boolean t = true;
                    BufferedImage bb;
                    if (framedict.containsKey(id)) {
                        f = (int) framedict.get(id);
                    } else {
                        f = 0;
                    }
                    try{
                        bb = ImageIO.read(new File(imgpath + "/vids/" + id + "/" + f + ".png")).getSubimage(0, 0, 128, 128); }
                    catch (Exception e){t = false;getLogger().warning("error reading image, last frame: "+f+"max frame:"+m[0]);throw new RuntimeException(e);}
                    if(t){mc[0].drawImage(0, 0, bb);}
                    if (f >= m[0] - 1) {
                        //System.out.println("m[0]: "+m[0]);
                        f = 0;
                    }
                    framedict.put(id, f + 1);
                } catch (Exception e) {throw new RuntimeException(e);}
            } else if (bufferdict.containsKey(id)) {
                mc[0].drawImage(0, 0, (BufferedImage) bufferdict.get(id));
            }
        }, 1000, 50, TimeUnit.MILLISECONDS);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
