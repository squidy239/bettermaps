package net.fractalinfinity.bettermaps;

import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ConcurrentHashMap;

public class mapidlocations implements Listener {

    private static final Plugin plugin = Bettermaps.getPlugin(Bettermaps.class);
    public static ConcurrentHashMap<Long, Location> maplocations = new ConcurrentHashMap<>();

    public static void initialize(ConcurrentHashMap<Long, Location> maploc) {
        if (maploc != null) maplocations = maploc;
        plugin.getServer().getPluginManager().registerEvents(new mapidlocations(), plugin);
    }

    @EventHandler
    public void onHangingBreak(HangingBreakEvent event) {
        if (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof GlowItemFrame && ((ItemFrame) event.getEntity()).getItem().getType() == Material.FILLED_MAP) {
            maplocations.remove((long) ((MapMeta)((ItemFrame) event.getEntity()).getItem().getItemMeta()).getMapId());
        }
    }
    @EventHandler
    public void onItemframeinteract(PlayerItemFrameChangeEvent e){
        if (e.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.PLACE && e.getItemStack().getType() == Material.FILLED_MAP){
            MapMeta mapmeta = ((MapMeta) e.getItemStack().getItemMeta());
            maplocations.put((long) mapmeta.getMapId(),e.getItemFrame().getLocation());
        }
        else if (e.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.REMOVE && e.getItemStack().getType() == Material.FILLED_MAP){
            MapMeta mapmeta = ((MapMeta) e.getItemStack().getItemMeta());
            maplocations.remove((long) mapmeta.getMapId());
        }

    }
}