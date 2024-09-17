package net.fractalinfinity.bettermaps;

import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ConcurrentHashMap;

public class EventListeners implements Listener {

    private static final Plugin plugin = Bettermaps.getPlugin(Bettermaps.class);
    public static ConcurrentHashMap<Integer, Location> maplocations = new ConcurrentHashMap<>();

    public static void initialize(ConcurrentHashMap<Integer, Location> maploc) {
        if (maploc != null) maplocations = maploc;
        plugin.getServer().getPluginManager().registerEvents(new EventListeners(), plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){

    }


    @EventHandler
    public void onHangingBreak(HangingBreakEvent event) {
        if (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof GlowItemFrame && ((ItemFrame) event.getEntity()).getItem().getType() == Material.FILLED_MAP) {
            MapMeta meta  = ((MapMeta)((ItemFrame) event.getEntity()).getItem().getItemMeta());
            if (meta != null) maplocations.remove(meta.getMapId());
        }
    }
    @EventHandler
    public void onItemFrameinteract(PlayerItemFrameChangeEvent e){
        if (e.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.PLACE && e.getItemStack().getType() == Material.FILLED_MAP){
            MapMeta mapmeta = ((MapMeta) e.getItemStack().getItemMeta());
            maplocations.put(mapmeta.getMapId(),e.getItemFrame().getLocation());
            //Screen.MapIdLocations.put(mapmeta.getMapId(),e.getItemFrame().getLocation());
        }
        else if (e.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.REMOVE && e.getItemStack().getType() == Material.FILLED_MAP){
            MapMeta mapmeta = ((MapMeta) e.getItemStack().getItemMeta());
            maplocations.remove(mapmeta.getMapId());
            //Screen.MapIdLocations.remove(mapmeta.getMapId());
        }

    }
}