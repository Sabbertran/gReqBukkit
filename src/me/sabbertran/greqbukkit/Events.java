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
                int status = rs.getInt("status");
                String status_extra = rs.getString("status");

//                p.sendMessage(status.split(":")[1] + " closed your ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ")");
                p.sendMessage(main.translateDatabaseVariables(main.getMessages().get(15), id).replace("%name", status_extra));
//                p.sendMessage("Answer: " + ChatColor.GRAY + answer);
                p.sendMessage(main.translateDatabaseVariables(main.getMessages().get(16), id));
                
                con.createStatement().execute("UPDATE greq_tickets SET status = '2' WHERE id = '" + id + "'");
            }
            
            ResultSet rs2 = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE author = '" + p.getName() + "' OR status_extra = '" + p.getName() + "'");
            while (rs2.next())
            {
                int id = rs2.getInt("id");
                int status = rs2.getInt("status");
                String status_extra = rs2.getString("status_extra");
                String author = rs2.getString("author");
                String comments = rs2.getString("comments");
                String[] comments_split = comments.split(";;");
                for (String s : comments_split)
                {
                    if (s.endsWith("#unseen#"))
                    {
                        s = s.substring(0, s.length() - 8);
                        String player = s.split(":")[0];
                        if (!player.equals(p.getName()))
                        {
//                            p.sendMessage("A new comment has been created for ticket #" + id + ". please check it using /tickets comments #" + id);
                            p.sendMessage(main.translateDatabaseVariables(main.getMessages().get(35), id));
                            String comment = "";
                            for (String s1 : comments_split)
                            {
                                if (s1.endsWith("#unseen#"))
                                {
                                    s1 = s1.substring(0, s1.length() - 8);
                                }
                                comment = comment + s1 + ";;";
                            }
                            comment = comment.substring(0, comment.length() - 2);
                            con.createStatement().execute("UPDATE greq_tickets SET comments = '" + comment + "' WHERE id = '" + id + "'");
                        }
                    }
                }
            }
            
            rs2.close();
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if (p.hasPermission("greq.notify"))
        {
            main.sendOpenTicketInfo(p, true);
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
