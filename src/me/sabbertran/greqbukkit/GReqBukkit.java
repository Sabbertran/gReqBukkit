package me.sabbertran.greqbukkit;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.sabbertran.greqbukkit.commands.TicketCommand;
import me.sabbertran.greqbukkit.commands.TicketsCommand;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class GReqBukkit extends JavaPlugin
{

    private Logger log = getLogger();

    private String server_name;
    private int notificationInterval;
    private ArrayList<String> sql;
    private ArrayList<String> messages;

    private File messagesFile;

    private HashMap<String, Location> pendingTeleports;

    @Override
    public void onDisable()
    {
        log.info("gReq disabled");
    }

    @Override
    public void onEnable()
    {
        getConfig().addDefault("gReq.BungeeServerName", getServer().getName());
        getConfig().addDefault("gReq.NotificationInterval", 120);
        getConfig().addDefault("gReq.SQL", new String[]
        {
            "Adress", "Port", "Database", "User", "Password"
        });
        getConfig().options().copyDefaults(true);
        saveConfig();

        server_name = getConfig().getString("gReq.BungeeServerName");
        notificationInterval = getConfig().getInt("gReq.NotificationInterval");
        sql = (ArrayList<String>) getConfig().getStringList("gReq.SQL");
        pendingTeleports = new HashMap<String, Location>();

        //Create SQL table
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            con.createStatement().execute("CREATE TABLE IF NOT EXISTS `greq_tickets` (\n"
                    + "`id` int(11) unsigned NOT NULL auto_increment,\n"
                    + "`author` varchar(265) NOT NULL,\n"
                    + "`text` text NOT NULL,\n"
                    + "`location` text NOT NULL,\n"
                    + "`date` varchar(265) NOT NULL,\n"
                    + "`status` int(11) NOT NULL,\n"
                    + "`status_extra` varchar(265),\n"
                    + "`comments` text,\n"
                    + "`answer` text,\n"
                    + "PRIMARY KEY  (`id`)\n"
                    + ")");
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }

        messages = new ArrayList<String>();
        messagesFile = new File("plugins/gReqBukkit/messages.yml");
        readMessagesFromFile();

        getServer().getPluginManager().registerEvents(new Events(this), this);

        getCommand("ticket").setExecutor(new TicketCommand(this));
        getCommand("tickets").setExecutor(new TicketsCommand(this));

        PMessageListener listener = new PMessageListener(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "gReq");
        getServer().getMessenger().registerIncomingPluginChannel(this, "gReq", listener);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
        {
            @Override
            public void run()
            {
                for (Player p : getServer().getOnlinePlayers())
                {
                    if (p.hasPermission("greq.notify"))
                    {
                        sendOpenTicketInfo(p, false);
                    }
                }
            }
        }, notificationInterval * 20, notificationInterval * 20);

        log.info("gReq enabled");
    }

    public void readMessagesFromFile()
    {
        if (messagesFile.exists())
        {
            messages.clear();
            try
            {
                BufferedReader read = new BufferedReader(new FileReader(messagesFile));
                String line;
                while ((line = read.readLine()) != null)
                {
                    if (!line.startsWith("#"))
                    {
                        messages.add(line);
                    }
                }
                read.close();
            } catch (IOException ex)
            {
                Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else
        {
            setupMessages();
        }
        if (messages.size() != 41)
        {
            setupMessages();
        }
    }

    public void setupMessages()
    {
        ArrayList<String> temp = new ArrayList<String>();

        //User messages
        temp.add("# Ticket creation user info #");
        temp.add("Ticket (ID: %id) created. A staff member will have a look at your request as soon as possible.");
        temp.add("# Ticket claim user info #");
        temp.add("%name is now handling your request #%id (§7%text§f)");
        temp.add("# Ticket unclaim user info #");
        temp.add("%name is no longer handling your request #%id (§7%text§f)");
        temp.add("# User ticket list #");
        temp.add("You currently have %tickets open tickets:");
        temp.add("[#%id] §7%text§f (%status)");
        //Staff messages
        temp.add("# Ticket Info #");
        temp.add("The ticket #%id does not exist.");
        temp.add("# Ticket creation staff info #");
        temp.add("A new Ticket has been created:");
        temp.add("[#%id] %author (%server): §7%text");
        temp.add("# Ticket list #");
        temp.add("There are currently %tickets open tickets. Showing page %page/%maxpage");
        temp.add("[#%id Status: %status] %author (%server, %world, %coordinates): §7%text");
        temp.add("There are currently no open tickets.");
        temp.add("There are not enough open tickets to fill this page");
        temp.add("# Ticket info #");
        temp.add("[#%id] - %author - %date - %status");
        temp.add("Server: %server, World: %world, Coordinates: %coordinates");
        temp.add("§7%text");
        temp.add("# Ticket closed user info #");
        temp.add("%name closed your ticket #%id (§7%text§f)");
        temp.add("Answer: §7%answer");
        temp.add("# Ticket closed staff info #");
        temp.add("Closed ticket #%id with answer: §7%answer");
        temp.add("%name closed ticket #%id (§7%text§f) with answer: §7%answer");
        temp.add("# Teleportation info #");
        temp.add("Teleporting to ticket #%id");
        temp.add("# Ticket claim info #");
        temp.add("You are now handling ticket #%id (§7%text§f)");
        temp.add("%name is now handling ticket #%id (§7%text§f)");
        temp.add("The ticket #%id is not open");
        temp.add("# Ticket unclaim info #");
        temp.add("You are no longer handling ticket #%id (§7%text§f)");
        temp.add("%name is no longer handling ticket #%id (§7%text§f)");
        temp.add("The ticket #%id is not claimed by you");
        temp.add("# Ticket close info #");
        temp.add("The ticket #%id is already closed");
        //Info
        temp.add("# Status messages #");
        temp.add("Open");
        temp.add("Claimed by %name");
        temp.add("Closed by %name");
        temp.add("# Date format #");
        temp.add("dd.MM.yyyy HH:mm");
        temp.add("# Info messages #");
        temp.add("You have no open tickets");
        temp.add("You don't have permission to use this command.");
        temp.add("You have to be a player to use this command.");
        //Comment messages
        temp.add("# Comment messages #");
        temp.add("A new comment has been created for ticket #%id. Please check it using /tickets comments #%id");
        temp.add("Your comment has been saved.");
        temp.add("Comments for ticket #%id:");
        temp.add("%player: %msg");
        temp.add("There are no comments for this ticket.");
        //Notify messages
        temp.add("# Notifications #");
        temp.add("There are currently no open tickets.");
        temp.add("There are currently %amount open tickets, check them with /tickets");

        try
        {
            messagesFile.delete();
            messagesFile.createNewFile();
            PrintWriter pw = new PrintWriter(new FileOutputStream(messagesFile), true);
            pw.println("# Do not change the order of the messages, otherwise they will be messed up ingame! #");
            pw.println("#####################################################################################");
            for (String m : temp)
            {
                pw.println(m);
            }
            pw.close();
        } catch (FileNotFoundException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }

        readMessagesFromFile();
    }

    public void sendMessage(CommandSender p, String msg, int id)
    {
        if (id != -1)
        {
            p.sendMessage(translateDatabaseVariables(msg, id).split("%n"));
        } else
        {
            p.sendMessage(msg.split("%n"));
        }
    }

    private String translateDatabaseVariables(String input, int id)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id = '" + id + "'");
            if (rs.next())
            {
                String author = rs.getString("author");
                String text = rs.getString("text");
                String server = rs.getString("location").split(":")[0];
                String world = rs.getString("location").split(":")[1];
                String coordinates = rs.getString("location").split(":")[2].replace(",", ", ");
                String date = rs.getString("date");
                int status = rs.getInt("status");
                String status_extra = rs.getString("status_extra");
                SimpleDateFormat read = new SimpleDateFormat("dd-MM-yyyy-HH-mm");
                Date parse = read.parse(date);
                SimpleDateFormat write = new SimpleDateFormat(messages.get(30));
                String date_translated = write.format(parse);
                String answer = rs.getString("answer");

                String output = input.replace("%id", "" + id).replace("%author", author).replace("%text", text).replace("%server", server).replace("%world", world).replace("%coordinates", coordinates).replace("%date", date_translated);
                if (answer != null)
                {
                    output = output.replace("%answer", answer);
                }
                if (status == 0)
                {
                    output = output.replace("%status", messages.get(27));
                } else if (status == 1)
                {
                    output = output.replace("%status", messages.get(28).replace("%name", status_extra));
                } else if (status == 2)
                {
                    output = output.replace("%status", messages.get(29).replace("%name", status_extra));
                }
                return output;
            }
            rs.close();
            con.close();
        } catch (ClassNotFoundException | SQLException | ParseException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public int createTicket(Player p, String text)
    {
        int id = -1;
        String loc = server_name + ":" + p.getWorld().getName() + ":" + p.getLocation().getBlockX() + "," + p.getLocation().getBlockY() + "," + p.getLocation().getBlockZ();
        String date = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss").format(new Date());
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            con.createStatement().execute("INSERT INTO greq_tickets (author, text, location, date, status) VALUES ('" + p.getName() + "', '" + text + "', '" + loc + "', '" + date + "', 0)");
            ResultSet rs = con.createStatement().executeQuery("SELECT LAST_INSERT_ID() AS last_id FROM greq_tickets");
            if (rs.next())
            {
                id = rs.getInt("last_id");
            }
            rs.close();
            con.close();

            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("new_ticket");
            p.sendPluginMessage(this, "gReq", b.toByteArray());
        } catch (ClassNotFoundException | SQLException | IOException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }

        return id;
    }

    public void infoStaffLatest()
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets ORDER BY id DESC LIMIT 0, 1");
            if (rs.next())
            {
                int id = rs.getInt("id");

                for (Player p : getServer().getOnlinePlayers())
                {
                    if (p.hasPermission("greq.notify"))
                    {
//                        p.sendMessage("A new ticket has been created.");
//                        p.sendMessage(messages.get(6));
                        sendMessage(p, messages.get(6), id);
//                        p.sendMessage(author + " (" + server + "): " + ChatColor.GRAY + text);
//                        p.sendMessage(translateDatabaseVariables(messages.get(7), id));
                        sendMessage(p, messages.get(7), id);
                    }
                }
            }
            rs.close();
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendTicketList(CommandSender p, int amount, int page)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE (status = '0' OR status = '1') ORDER BY id DESC LIMIT " + (amount * page - amount) + ", " + (amount * page));
            if (rs.next())
            {
                ResultSet rs_ = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE status = '0' OR status= '1'");
                rs_.last();
                int size = rs_.getRow();
                rs_.close();
//                p.sendMessage("There are currently " + size + " open tickets. Showing page " + page + "/" + (size / amount + 1));
//                p.sendMessage(messages.get(8).replace("%tickets", "" + size).replace("%page", "" + page).replace("%maxpage", "" + (size / amount + 1)));
                sendMessage(p, messages.get(8).replace("%tickets", "" + size).replace("%page", "" + page).replace("%maxpage", "" + (size / amount + 1)), -1);
                do
                {
                    int id = rs.getInt("id");
//                    p.sendMessage(translateDatabaseVariables(messages.get(9), id));
                    sendMessage(p, messages.get(9), id);
                } while (rs.next());
            } else
            {
                if (amount == 5 && page == 1)
                {
//                    p.sendMessage("There are currently no open tickets.");
//                    p.sendMessage(messages.get(10));
                    sendMessage(p, messages.get(10), -1);
                } else
                {
//                    p.sendMessage("There are not enough open tickets to fill this page.");
//                    p.sendMessage(messages.get(11));
                    sendMessage(p, messages.get(11), -1);
                }
            }
            rs.close();
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendTicketInfo(CommandSender p, int id)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id='" + id + "'");
            if (rs.next())
            {
//                p.sendMessage(translateDatabaseVariables(messages.get(12), id));
                sendMessage(p, messages.get(12), id);
//                p.sendMessage("Server: " + server + ", World: " + world + ", Coordinates: " + coordinates.replace(",", ", "));
//                p.sendMessage(translateDatabaseVariables(messages.get(13), id));
                sendMessage(p, messages.get(13), id);
//                p.sendMessage(ChatColor.GRAY + text);
//                p.sendMessage(translateDatabaseVariables(messages.get(14), id));
                sendMessage(p, messages.get(14), id);
            } else
            {
//                p.sendMessage("The ticket #" + id + " does not exist.");
//                p.sendMessage(translateDatabaseVariables(messages.get(5), id));
                sendMessage(p, messages.get(5), id);
            }
            rs.close();
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void closeTicket(Player p, int id, String answer)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id='" + id + "'");
            if (rs.next())
            {
                int status = rs.getInt("status");
                String status_extra = rs.getString("status_extra");
                if (status == 0 || status == 1)
                {
                    String author = rs.getString("author");

                    con.createStatement().execute("UPDATE greq_tickets SET status = '3', status_extra = '" + p.getName() + "' , answer = '" + answer + "' WHERE id='" + id + "'");

//                    p.sendMessage("Closed ticket #" + id + " with answer: " + ChatColor.GRAY + answer);
//                    p.sendMessage(translateDatabaseVariables(messages.get(17), id));
                    sendMessage(p, messages.get(17), id);

                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(b);
                    out.writeUTF("answer");
                    out.writeUTF("" + id);
                    out.writeUTF(author);
                    p.sendPluginMessage(this, "gReq", b.toByteArray());
                } else
                {
//                    p.sendMessage("The ticket #" + id + " is already closed");
//                    p.sendMessage(translateDatabaseVariables(messages.get(26), id));
                    sendMessage(p, messages.get(26), id);
                }
            } else
            {
//                p.sendMessage("The ticket #" + id + " does not exist.");
//                p.sendMessage(translateDatabaseVariables(messages.get(5), id));
                sendMessage(p, messages.get(5), id);
            }
            con.close();
        } catch (ClassNotFoundException | SQLException | IOException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendUserCloseInfo(Player p, int id)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id='" + id + "'");
            if (rs.next())
            {
                int status = rs.getInt("status");
                String status_extra = rs.getString("status_extra");

                if (status == 2 || status == 3)
                {
//                    p.sendMessage(status.split(":")[1] + " closed your ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ")");
//                    p.sendMessage(translateDatabaseVariables(messages.get(15), id).replace("%name", status_extra));
                    sendMessage(p, messages.get(15).replace("%name", status_extra), id);
//                    p.sendMessage("Answer: " + ChatColor.GRAY + answer);
//                    p.sendMessage(translateDatabaseVariables(messages.get(16), id));
                    sendMessage(p, messages.get(16), id);

                    if (status == 3)
                    {
                        con.createStatement().execute("UPDATE greq_tickets SET status = '2' WHERE id = '" + id + "'");
                    }
                }
            }
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendStaffCloseInfo(Player p, int id)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id='" + id + "'");
            if (rs.next())
            {
                String text = rs.getString("text");
                int status = rs.getInt("status");
                String status_extra = rs.getString("status_extra");
                String answer = rs.getString("answer");

                if (status == 2 && !status_extra.equals(p.getName()));
                {
//                    p.sendMessage(status.split(":")[1] + " closed ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ") with answer: " + ChatColor.GRAY + answer);
//                    p.sendMessage(translateDatabaseVariables(messages.get(18), id).replace("%name", status_extra));
                    sendMessage(p, messages.get(18).replace("%name", status_extra), id);
                }
            }
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void teleportToTicket(Player p, int id)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id='" + id + "'");
            String server = "";
            String world = "";
            String coordinates = "";
            if (rs.next())
            {
                server = rs.getString("location").split(":")[0];
                world = rs.getString("location").split(":")[1];
                coordinates = rs.getString("location").split(":")[2];
            }
            con.close();

//            p.sendMessage("Teleporting to ticket #" + id);
//            p.sendMessage(translateDatabaseVariables(messages.get(19), id));
            sendMessage(p, messages.get(19), id);

            if (server.equals(server_name))
            {
                p.teleport(new Location(getServer().getWorld(world), Integer.parseInt(coordinates.split(",")[0]), Integer.parseInt(coordinates.split(",")[1]), Integer.parseInt(coordinates.split(",")[2])));
            } else
            {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("join_teleport");
                out.writeUTF(p.getName());
                out.writeUTF(server);
                out.writeUTF(world + ":" + coordinates);
                p.sendPluginMessage(this, "gReq", out.toByteArray());

                ByteArrayDataOutput out_ = ByteStreams.newDataOutput();
                out_.writeUTF("Connect");
                out_.writeUTF(server);
                p.sendPluginMessage(this, "BungeeCord", out_.toByteArray());
            }
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void claimTicket(Player p, int id)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id='" + id + "'");
            if (rs.next())
            {
                String author = rs.getString("author");
                int status = rs.getInt("status");
                String status_extra = rs.getString("status_extra");
                if (status == 0)
                {
                    con.createStatement().execute("UPDATE greq_tickets SET status = '1', status_extra = '" + p.getName() + "' WHERE id='" + id + "'");

                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("claim");
                    out.writeUTF(author);
                    out.writeUTF("" + id);
                    p.sendPluginMessage(this, "gReq", out.toByteArray());

//                    p.sendMessage("You are now handling ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ")");
//                    p.sendMessage(translateDatabaseVariables(messages.get(20), id));
                    sendMessage(p, messages.get(20), id);
                } else
                {
//                    p.sendMessage("The ticket #" + id + " is not open");
//                    p.sendMessage(translateDatabaseVariables(messages.get(22), id));
                    sendMessage(p, messages.get(22), id);
                }
            } else
            {
//                p.sendMessage("The ticket #" + id + " does not exist.");
//                p.sendMessage(translateDatabaseVariables(messages.get(5), id));
                sendMessage(p, messages.get(5), id);
            }
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void unclaimTicket(Player p, int id)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id='" + id + "'");
            if (rs.next())
            {
                String author = rs.getString("author");
                int status = rs.getInt("status");
                String status_extra = rs.getString("status_extra");
                if (status == 1)
                {
                    con.createStatement().execute("UPDATE greq_tickets SET status = '0' WHERE id = '" + id + "' AND status = '1' AND status_extra = '" + p.getName() + "'");

                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("unclaim");
                    out.writeUTF(author);
                    out.writeUTF("" + id);
                    out.writeUTF(p.getName());
                    p.sendPluginMessage(this, "gReq", out.toByteArray());

//                    p.sendMessage("You are no longer handling ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ").");
//                    p.sendMessage(translateDatabaseVariables(messages.get(23), id));
                    sendMessage(p, messages.get(23), id);
                } else
                {
//                    p.sendMessage("The ticket #" + id + " is not claimed by you");
//                    p.sendMessage(translateDatabaseVariables(messages.get(25), id));
                    sendMessage(p, messages.get(25), id);
                }
            } else
            {
//                p.sendMessage("The ticket #" + id + " does not exist.");
                p.sendMessage(translateDatabaseVariables(messages.get(5), id));
                sendMessage(p, messages.get(5), id);
            }
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendUserClaimInfo(Player p, int id)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id='" + id + "'");
            if (rs.next())
            {
                String text = rs.getString("text");
                int status = rs.getInt("status");
                String status_extra = rs.getString("status_extra");

                if (status == 1)
                {
//                    p.sendMessage(status.split(":")[1] + " is now handling your request #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ")");
//                    p.sendMessage(translateDatabaseVariables(messages.get(1), id).replace("%name", status_extra));
                    sendMessage(p, messages.get(1).replace("%name", status_extra), id);
                }
            }
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendStaffClaimInfo(Player p, int id)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id='" + id + "'");
            if (rs.next())
            {
                int status = rs.getInt("status");
                String status_extra = rs.getString("status_extra");

                if (status == 1)
                {
//                    p.sendMessage(status.split(":")[1] + " is now handling ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ")");
//                    p.sendMessage(translateDatabaseVariables(messages.get(21), id).replace("%name", status_extra));
                    sendMessage(p, messages.get(21).replace("%name", status_extra), id);
                }
            }
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendUserUnclaimInfo(Player p, int id, String unclaimer)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id='" + id + "'");
            if (rs.next())
            {
                String text = rs.getString("text");
                int status = rs.getInt("status");
                String status_extra = rs.getString("status_extra");

                if (status == 0)
                {
//                    p.sendMessage("Your request #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ") is no longer being handled");
//                    p.sendMessage(translateDatabaseVariables(messages.get(2), id).replace("%name", unclaimer));
                    sendMessage(p, messages.get(2).replace("%name", unclaimer), id);
                }
            }
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendStaffUnclaimInfo(Player p, int id, String unclaimer)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id='" + id + "'");
            if (rs.next())
            {
                String text = rs.getString("text");
                int status = rs.getInt("status");
                String status_extra = rs.getString("status_extra");

                if (status == 0)
                {
//                    p.sendMessage(status.split(":")[1] + " is no longer handling ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ")");
//                    p.sendMessage(translateDatabaseVariables(messages.get(24), id).replace("%name", unclaimer));
                    sendMessage(p, messages.get(24).replace("%name", unclaimer), id);
                }
            }
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendOwnTicketList(Player p)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE author='" + p.getName() + "' AND (status = '0' OR status = '1')");
            if (rs.next())
            {
                rs.last();
                int size = rs.getRow();
                rs.first();
//                p.sendMessage("You currently have " + size + " open tickets:");
//                p.sendMessage(messages.get(3).replace("%tickets", "" + size));
                sendMessage(p, messages.get(3).replace("%tickets", "" + size), -1);
                do
                {
                    int id = rs.getInt("id");
//                    p.sendMessage(translateDatabaseVariables(messages.get(7), id));
                    sendMessage(p, messages.get(7), id);
                } while (rs.next());
            } else
            {
//                p.sendMessage("You have no open tickets.");
//                p.sendMessage(messages.get(31));
                sendMessage(p, messages.get(31), -1);
            }
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void addTicketComment(CommandSender p, int id, String text)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id = '" + id + "'");
            if (rs.next())
            {
                int status = rs.getInt("status");
                String status_extra = rs.getString("status_extra");
                String author = rs.getString("author");

                if (author.equals(p.getName()) || (status == 1 && status_extra.equals(p.getName())))
                {
                    String comments = rs.getString("comments");
                    if (comments == null || comments.equals(""))
                    {
                        comments = p.getName() + ":" + text + "#unseen#";
                    } else
                    {
                        comments = comments + ";;" + p.getName() + ":" + text + "#unseen#";
                    }
                    con.createStatement().execute("UPDATE greq_tickets SET comments = '" + comments + "' WHERE id = '" + id + "'");
//                    p.sendMessage("Your comment has been saved.");
//                    p.sendMessage(translateDatabaseVariables(messages.get(34), id));
                    sendMessage(p, messages.get(34), id);

                    String receiver;
                    boolean staff;

                    if (p.getName().equals(author))
                    {
                        newCommentNotifyStaff(id);
                        receiver = status_extra;
                        staff = true;
                    } else
                    {
                        newCommentNotifyUser(id);
                        receiver = author;
                        staff = false;
                    }

                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(b);
                    out.writeUTF("new_comment");
                    out.writeUTF(receiver);
                    out.writeUTF("" + staff);
                    out.writeUTF("" + id);
                    getServer().sendPluginMessage(this, "gReq", b.toByteArray());
                }

                rs.close();
                con.close();
            } else
            {
//                p.sendMessage(translateDatabaseVariables(messages.get(5), id));
                sendMessage(p, messages.get(5), id);
            }
        } catch (ClassNotFoundException | SQLException | IOException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void newCommentNotifyStaff(int id)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id = '" + id + "'");
            if (rs.next())
            {
                int status = rs.getInt("status");
                String status_extra = rs.getString("status_extra");
                String author = rs.getString("author");
                String comments = rs.getString("comments");
                String[] comments_split = comments.split(";;");
                for (String s : comments_split)
                {
                    if (s.endsWith("#unseen#"))
                    {
                        s = s.substring(0, s.length() - 8);
                        String player = s.split(":")[0];
                        if (author.equals(player) && status_extra != null)
                        {
                            Player pl = getServer().getPlayer(status_extra);
                            if (pl != null)
                            {
//                            pl.sendMessage("A new comment has been created for ticket #" + id + ". please check it using /tickets comments #" + id);
//                                pl.sendMessage(translateDatabaseVariables(messages.get(35), id));
                                sendMessage(pl, messages.get(34), id);
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
                                break;
                            }
                        }
                    }
                }
            }

        } catch (SQLException | ClassNotFoundException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void newCommentNotifyUser(int id)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id = '" + id + "'");
            if (rs.next())
            {
                int status = rs.getInt("status");
                String status_extra = rs.getString("status_extra");
                String author = rs.getString("author");
                String comments = rs.getString("comments");
                String[] comments_split = comments.split(";;");
                for (String s : comments_split)
                {
                    if (s.endsWith("#unseen#"))
                    {
                        s = s.substring(0, s.length() - 8);
                        String player = s.split(":")[0];
                        if (status_extra.equals(player))
                        {
                            Player pl = getServer().getPlayer(author);
                            if (pl != null)
                            {
//                            pl.sendMessage("A new comment has been created for ticket #" + id + ". please check it using /tickets comments #" + id);
//                                pl.sendMessage(translateDatabaseVariables(messages.get(35), id));
                                sendMessage(pl, messages.get(34), id);
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
                                break;
                            }
                        }
                    }
                }
            }

        } catch (SQLException | ClassNotFoundException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendCommentList(CommandSender p, int id)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE id = '" + id + "'");
            if (rs.next())
            {
                String comments = rs.getString("comments");
                if (comments != null)
                {
                    String[] comments_split = comments.split(";;");
//                    p.sendMessage(translateDatabaseVariables(messages.get(36), id));
                    sendMessage(p, messages.get(36), id);
                    for (String s : comments_split)
                    {
                        String player = s.split(":")[0];
                        String msg = s.split(":")[1];
//                            p.sendMessage("%player: %msg");
//                            p.sendMessage(messages.get(37).replace("%player", player).replace("%msg", msg));
                        sendMessage(p, messages.get(37).replace("%player", player).replace("%msg", msg), id);
                    }
                } else
                {
//                    p.sendMessage(messages.get(38));
                    sendMessage(p, messages.get(38), id);
                }
            } else
            {
//                p.sendMessage(translateDatabaseVariables(messages.get(5), id));
                sendMessage(p, messages.get(5), id);
            }

        } catch (SQLException | ClassNotFoundException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendOpenTicketInfo(Player p, boolean includeZero)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE status = '0' OR status = '1'");
            rs.last();
            int count = rs.getRow();
            if (count != 0)
            {
//                p.sendMessage(messages.get(40).replace("%amount", "" + count));
                sendMessage(p, messages.get(40).replace("%amount", "" + count), -1);
            } else if (count == 0 && includeZero)
            {
//                p.sendMessage(messages.get(39));
                sendMessage(p, messages.get(39), -1);
            }

            rs.close();

        } catch (SQLException | ClassNotFoundException ex)
        {
            Logger.getLogger(GReqBukkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ArrayList<String> getSql()
    {
        return sql;
    }

    public HashMap<String, Location> getPendingTeleports()
    {
        return pendingTeleports;
    }

    public ArrayList<String> getMessages()
    {
        return messages;
    }
}
