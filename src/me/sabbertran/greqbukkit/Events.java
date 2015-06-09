package me.sabbertran.greqbukkit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Events implements Listener {

    private GReqBukkit main;

    public Events(GReqBukkit main) {
        this.main = main;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent ev) {
        final Player p = ev.getPlayer();

        try {
            PreparedStatement pst = main.getSqlhandler().getCurrentConnection().prepareStatement("SELECT * FROM greq_tickets WHERE author = ? AND status = '3'");
            pst.setString(1, p.getName());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                int status = rs.getInt("status");
                String status_extra = rs.getString("status");

//                p.sendMessage(status.split(":")[1] + " closed your ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ")");
//                p.sendMessage(main.translateDatabaseVariables(main.getMessages().get(15), id).replace("%name", status_extra));
                main.sendMessage(p, main.getMessages().get(15).replace("%name", status_extra), id);
//                p.sendMessage("Answer: " + ChatColor.GRAY + answer);
                main.sendMessage(p, main.getMessages().get(16).replace("%name", status_extra), id);

                PreparedStatement pst1 = main.getSqlhandler().getCurrentConnection().prepareStatement("UPDATE greq_tickets SET status = '2' WHERE id = ?");
                pst1.setInt(1, id);
                pst1.execute();
            }
            rs.close();

            PreparedStatement pst2 = main.getSqlhandler().getCurrentConnection().prepareStatement("SELECT * FROM greq_tickets WHERE author = ? OR status_extra = ?");
            pst2.setString(2, p.getName());
            pst2.setString(3, p.getName());
            ResultSet rs2 = pst2.executeQuery();
            while (rs2.next()) {
                int id = rs2.getInt("id");
                String comments = rs2.getString("comments");
                if (comments != null) {
                    String[] comments_split = comments.split(";;");
                    for (String s : comments_split) {
                        if (s.endsWith("#unseen#")) {
                            s = s.substring(0, s.length() - 8);
                            String player = s.split(":")[0];
                            if (!player.equals(p.getName())) {
//                            p.sendMessage("A new comment has been created for ticket #" + id + ". please check it using /tickets comments #" + id);
//                                p.sendMessage(main.translateDatabaseVariables(main.getMessages().get(35), id));
                                main.sendMessage(p, main.getMessages().get(35), id);
                                String comment = "";
                                for (String s1 : comments_split) {
                                    if (s1.endsWith("#unseen#")) {
                                        s1 = s1.substring(0, s1.length() - 8);
                                    }
                                    comment = comment + s1 + ";;";
                                }
                                comment = comment.substring(0, comment.length() - 2);
                                PreparedStatement pst3 = main.getSqlhandler().getCurrentConnection().prepareStatement("UPDATE greq_tickets SET comments = ? WHERE id = ?");
                                pst3.setString(1, comment);
                                pst3.setInt(2, id);
                                pst3.execute();
                            }
                        }
                    }
                }
            }

            rs2.close();
        } catch (SQLException ex) {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (p.hasPermission("greq.notify")) {
            main.sendOpenTicketInfo(p, true);
        }

        if (main.getPendingTeleports().containsKey(p.getName())) {
            main.getServer().getScheduler().scheduleSyncDelayedTask(main, new Runnable() {
                @Override
                public void run() {
                    p.teleport(main.getPendingTeleports().get(p.getName()));
                    main.getPendingTeleports().remove(p.getName());
                }
            }, 10L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent ev) {
        Player p = ev.getPlayer();
        if (main.getPendingPurges().containsKey(p.getName())) {
            main.getPendingPurges().remove(p.getName());
        }
    }
}
