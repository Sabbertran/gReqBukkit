package me.sabbertran.greqbukkit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class Events implements Listener
{

    private GReqBukkit main;

    public Events(GReqBukkit main)
    {
        this.main = main;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent ev)
    {
        final Player p = ev.getPlayer();

        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + main.getSql().get(0) + ":" + main.getSql().get(1) + "/" + main.getSql().get(2);
            Connection con = DriverManager.getConnection(url, main.getSql().get(3), main.getSql().get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE author='" + p.getName() + "' AND status LIKE '%unseen%'");
            while (rs.next())
            {
                int id = rs.getInt("id");
                String status = rs.getString("status");

//                p.sendMessage(status.split(":")[1] + " closed your ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ")");
                p.sendMessage(main.translateDatabaseVariables(main.getMessages().get(15), id).replace("%name", status.split(":")[1]));
//                p.sendMessage("Answer: " + ChatColor.GRAY + answer);
                p.sendMessage(main.translateDatabaseVariables(main.getMessages().get(16), id));

                con.createStatement().execute("UPDATE greq_tickets SET status='" + status.split(":")[0] + ":" + status.split(":")[1] + "' WHERE id='" + id + "'");
            }
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (main.getPendingTeleports().containsKey(p.getName()))
        {
            main.getServer().getScheduler().scheduleSyncDelayedTask(main, new Runnable()
            {
                @Override
                public void run()
                {
                    p.teleport(main.getPendingTeleports().get(p.getName()));
                    main.getPendingTeleports().remove(p.getName());
                }
            }, 10L);
        }
    }
}
