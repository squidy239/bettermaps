package net.fractalinfinity.bettermaps;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class Bettermaps extends JavaPlugin implements Listener {
    JSONObject bufferdict = new JSONObject();
    JSONObject framedict = new JSONObject();

    boolean smoothtoggle = true;

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


    private void setiimgbufferfromvid(int id, String path) {
        if (Files.exists(Paths.get(path + "/vids/" + id))) {
            try {
                int f;
                if (framedict.containsKey(id)) {
                    f = (int) framedict.get(id);
                } else {
                    f = 0;
                }
                bufferdict.put(id, ImageIO.read(new File(path + "/vids/" + id + "/" + f + ".png")));
                int e = new File(path + "/vids/" + id).list().length;
                if (f >= e - 1) {
                    f = 0;
                }
                framedict.put(id, f + 1);
            } catch (IOException ignored) {
            }
        }

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
        System.out.println("map id " + id + " initalized");
        Player[] playr = {null};
        MapView[] mapview = {null};
        String imgpath = "./mapimg";
        Bukkit.getScheduler().runTaskTimer(this,()->{if(playr[0] == null){System.out.print("null");}else{playr[0].sendMap(mapview[0]);}},1,1);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> setiimgbufferfromvid(id, imgpath), 1, 1);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> setiimgbufferfromfile(id, imgpath), 1, 20);
        mapp.getMap().getRenderers().clear();
        mapp.getMap().addRenderer(new MapRenderer() {
            @Override
            public void render(@NotNull MapView mapView, @NotNull MapCanvas mapCanvas, @NotNull Player player) {
                if (bufferdict.containsKey(id) && framedict.containsKey(id)) {
                    try {
                        mapCanvas.drawImage(0, 0, (BufferedImage) bufferdict.get(id));
                    } catch (Exception e){
                        throw new RuntimeException(e);
                    }
                    playr[0] = player;
                    mapview[0] = mapView;
                } else if (bufferdict.containsKey(id)) {
                    mapCanvas.drawImage(0, 0, (BufferedImage) bufferdict.get(id));
                }
            }
        });


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
