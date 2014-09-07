package me.sabbertran.greqbukkit;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class PMessageListener implements PluginMessageListener
{

    private GReqBukkit main;

    public PMessageListener(GReqBukkit main)
    {
        this.main = main;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message)
    {
        if (channel.equals("gReq"))
        {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            if (subchannel.equals("new_ticket"))
            {
                main.infoStaffLatest();
            } else if (subchannel.equals("claim"))
            {
                String pl = in.readUTF();
                String id = in.readUTF();
                try
                {
                    Class.forName("com.mysql.jdbc.Driver");
                    String url = "jdbc:mysql://" + main.getSql().get(0) + ":" + main.getSql().get(1) + "/" + main.getSql().get(2);
                    Connection con = DriverManager.getConnection(url, main.getSql().get(3), main.getSql().get(4));
                    ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id='" + id + "'");
                    if (rs.next())
                    {
                        String claimer = rs.getString("status").split(":")[1];

                        for (Player p : main.getServer().getOnlinePlayers())
                        {
                            if (pl.equals(p.getName()))
                            {
                                main.sendUserClaimInfo(p, Integer.parseInt(id));
                            }
                            if (p.hasPermission("greq.staff.claiminfo") && !p.getName().equals(claimer))
                            {
                                main.sendStaffClaimInfo(p, Integer.parseInt(id));
                            }
                        }
                    }
                    con.close();
                } catch (ClassNotFoundException | SQLException ex)
                {
                    Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (subchannel.equals("unclaim"))
            {
                String pl = in.readUTF();
                String id = in.readUTF();
                String unclaimer = in.readUTF();
                try
                {
                    Class.forName("com.mysql.jdbc.Driver");
                    String url = "jdbc:mysql://" + main.getSql().get(0) + ":" + main.getSql().get(1) + "/" + main.getSql().get(2);
                    Connection con = DriverManager.getConnection(url, main.getSql().get(3), main.getSql().get(4));
                    ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id='" + id + "'");
                    if (rs.next())
                    {
                        for (Player p : main.getServer().getOnlinePlayers())
                        {
                            if (pl.equals(p.getName()))
                            {
                                main.sendUserUnclaimInfo(p, Integer.parseInt(id), unclaimer);
                            }
                            if (p.hasPermission("greq.staff.unclaiminfo") && !p.getName().equals(unclaimer))
                            {
                                main.sendStaffUnclaimInfo(p, Integer.parseInt(id), unclaimer);
                            }
                        }
                    }
                    con.close();
                } catch (ClassNotFoundException | SQLException ex)
                {
                    Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (subchannel.equals("answer"))
            {
                String id = in.readUTF();
                String author = in.readUTF();
                for (Player p : main.getServer().getOnlinePlayers())
                {
                    if (p.getName().equals(author))
                    {
                        main.sendUserCloseInfo(p, Integer.parseInt(id));
                    }
                    if (p.hasPermission("greq.staff.closeinfo"))
                    {
                        main.sendStaffCloseInfo(p, Integer.parseInt(id));
                    }
                }
            } else if (subchannel.equals("join_teleport"))
            {
                String pl = in.readUTF();
                String server = in.readUTF();
                String loc = in.readUTF();
                String world = loc.split(":")[0];
                String coords = loc.split(":")[1];
                int x = Integer.parseInt(coords.split(",")[0]);
                int y = Integer.parseInt(coords.split(",")[1]);
                int z = Integer.parseInt(coords.split(",")[2]);
                Location l = new Location(main.getServer().getWorld(world), x, y, z);

                main.getPendingTeleports().put(pl, l);
            }
        }
    }
}
