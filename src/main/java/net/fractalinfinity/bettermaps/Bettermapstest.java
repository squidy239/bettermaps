/*
package net.fractalinfinity.bettermaps;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Bettermaptest extends JavaPlugin implements Listener {
    static ScheduledExecutorService loader
            = Executors.newScheduledThreadPool( Runtime.getRuntime().availableProcessors()/3);
    public ConcurrentHashMap<Long,MapCanvas> canvasdict = new ConcurrentHashMap<>();
    public ConcurrentHashMap<Long, Boolean> fastupdatedict = new ConcurrentHashMap<>();

    public  ConcurrentHashMap<Long, List<Player>> playerlist = new ConcurrentHashMap<>();


    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getPlugin(Bettermaptest.class).getLogger().info("Bettermaps starting");
        try {
            playmedia(new File("mapimg/images/1.png"),new long[][]{{1}});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void mapevent(MapInitializeEvent mapp) {
        MapView mapview = mapp.getMap();
        long id = mapview.getId();
        mapview.addRenderer(new MapRenderer() {
            @Override
            public void render(@NotNull MapView mapView, @NotNull MapCanvas mapCanvas, @NotNull Player player) {
                canvasdict.putIfAbsent(id,mapCanvas);
                playerlist.compute(id, (key, val) -> {
                    if (val == null) {
                        val = new ArrayList<>();
                    }
                    val.add(player);
                    return val;
                });

            }
        });
    }
    public boolean putimgonmap(BufferedImage image,long id,int x , int y ) {
        if (canvasdict.containsKey(id)){(canvasdict.get(id)).drawImage(x,y,image);return true;}
        else return false;

    }
    public void fastupdatetoggle(long id,boolean toggle){
        fastupdatedict.put(id , toggle);
        if (toggle && toggle != fastupdatedict.get(id)) Thread.ofVirtual().start(() -> {
            while (fastupdatedict.get(id)) {
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                MapView mv = canvasdict.get(id).getMapView();
                List<Player> pl = playerlist.get(id);
                for (int i = 0; i < pl.size();i++){
                    pl.get(i).sendMap(mv);
                }
            }
        });


    }
    public void playmedia(File path,long[][] ids) throws IOException {
        if (!path.exists()) return;

        if (path.isDirectory()){

        }
        else if (path.isFile()){
            final BufferedImage image = ImageIO.read(path);
            for (int l = 0; l < ids.length; l++){
                for (int i = 0; i < ids[l].length; i++){
                    int finalI = i;
                    int finalL = l;
                    System.out.println(finalI);
                    System.out.println(finalL);
                    Thread.ofVirtual().start(()-> putimgonmap(image.getSubimage(finalI *128,finalL *128,128,128),ids[finalL][finalI],0,0));
                }
            }
        }
    }

}
 */
