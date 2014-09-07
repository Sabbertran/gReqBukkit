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
        getConfig().addDefault("gReq.BungeeServerName", "SERVER-NAME");
        getConfig().addDefault("gReq.SQL", new String[]
        {
            "Adress", "Port", "Database", "User", "Password"
        });
        getConfig().options().copyDefaults(true);
        saveConfig();

        server_name = getConfig().getString("gReq.BungeeServerName");
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
                    + "`status` varchar(265) NOT NULL,\n"
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
        if (messages.size() != 34)
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

        try
        {
            messagesFile.delete();
            messagesFile.createNewFile();
            PrintWriter pw = new PrintWriter(new FileOutputStream(messagesFile));
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

    public String translateDatabaseVariables(String input, int id)
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
                String text = rs.getString("text");
                String server = rs.getString("location").split(":")[0];
                String world = rs.getString("location").split(":")[1];
                String coordinates = rs.getString("location").split(":")[2].replace(",", ", ");
                String date = rs.getString("date");
                String status = rs.getString("status");
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
                if (status.equals("open"))
                {
                    output = output.replace("%status", messages.get(27));
                } else if (status.startsWith("claimed"))
                {
                    String pl = status.split(":")[1];
                    output = output.replace("%status", messages.get(28).replace("%name", pl));
                } else if (status.startsWith("closed"))
                {
                    String pl = status.split(":")[1];
                    output = output.replace("%status", messages.get(29).replace("%name", pl));
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
        String date = new SimpleDateFormat("dd-MM-yyyy-HH-mm").format(new Date());
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
            Connection con = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            con.createStatement().execute("INSERT INTO greq_tickets (author, text, location, date, status) VALUES ('" + p.getName() + "', '" + text + "', '" + loc + "', '" + date + "', 'open')");
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
                        p.sendMessage(messages.get(6));
//                        p.sendMessage(author + " (" + server + "): " + ChatColor.GRAY + text);
                        p.sendMessage(translateDatabaseVariables(messages.get(7), id));
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
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE (status='open' OR status LIKE '%claimed%') ORDER BY id DESC LIMIT " + (amount * page - amount) + ", " + (amount * page));
            if (rs.next())
            {
                ResultSet rs_ = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE status='open' OR status LIKE '%claimed%'");
                rs_.last();
                int size = rs_.getRow();
                rs_.close();
//                p.sendMessage("There are currently " + size + " open tickets. Showing page " + page + "/" + (size / amount + 1));
                p.sendMessage(messages.get(8).replace("%tickets", "" + size).replace("%page", "" + page).replace("%maxpage", "" + (size / amount + 1)));
                do
                {
                    int id = rs.getInt("id");
//                    String author = rs.getString("author");
//                    String text = rs.getString("text");
//                    String server = rs.getString("location").split(":")[0];
//                    String world = rs.getString("location").split(":")[1];
//                    String coordinates = rs.getString("location").split(":")[2].replace(",", ", ");
//                    String date = rs.getString("date");
//                    String status = rs.getString("status");
//                    if (status.equals("open"))
//                    {
//                        p.sendMessage("[#" + id + " Open] " + author + " (" + server + "): " + ChatColor.GRAY + text);
//                    } else if (status.startsWith("claimed"))
//                    {
//                        String claimer = status.split(":")[1];
//                        p.sendMessage("[#" + id + " Claimed by " + claimer + "] " + author + " (" + server + "): " + ChatColor.GRAY + text);
//                    }
                    p.sendMessage(translateDatabaseVariables(messages.get(9), id));
                } while (rs.next());
            } else
            {
                if (amount == 5 && page == 1)
                {
//                    p.sendMessage("There are currently no open tickets.");
                    p.sendMessage(messages.get(10));
                } else
                {
//                    p.sendMessage("There are not enough open tickets to fill this page.");
                    p.sendMessage(messages.get(11));
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
//                String author = rs.getString("author");
//                String text = rs.getString("text");
//                String server = rs.getString("location").split(":")[0];
//                String world = rs.getString("location").split(":")[1];
//                String coordinates = rs.getString("location").split(":")[2];
//                String date = rs.getString("date");
//                String status = rs.getString("status");
//                SimpleDateFormat read = new SimpleDateFormat("dd-MM-yyyy-HH-mm");
//                Date parse = read.parse(date);
//                SimpleDateFormat write = new SimpleDateFormat("dd.MM.yyyy HH:mm");
//                if (status.equals("open"))
//                {
//                    p.sendMessage("[#" + id + "] - " + author + " - " + write.format(parse) + " - Status: Open");
//                } else if (status.startsWith("claimed"))
//                {
//                    String pl = status.split(":")[1];
//                    p.sendMessage("[#" + id + "] - " + author + " - " + write.format(parse) + " - Status: Claimed by " + pl);
//                } else if (status.startsWith("closed"))
//                {
//                    String pl = status.split(":")[1];
//                    p.sendMessage("[#" + id + "] - " + author + " - " + write.format(parse) + " - Status: Claimed by " + pl);
//                }
                p.sendMessage(translateDatabaseVariables(messages.get(12), id));
//                p.sendMessage("Server: " + server + ", World: " + world + ", Coordinates: " + coordinates.replace(",", ", "));
                p.sendMessage(translateDatabaseVariables(messages.get(13), id));
//                p.sendMessage(ChatColor.GRAY + text);
                p.sendMessage(translateDatabaseVariables(messages.get(14), id));
            } else
            {
//                p.sendMessage("The ticket #" + id + " does not exist.");
                p.sendMessage(translateDatabaseVariables(messages.get(5), id));
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
                String status = rs.getString("status");
                if (status.equals("open") || status.startsWith("claimed"))
                {
                    String author = rs.getString("author");
                    
                    con.createStatement().execute("UPDATE greq_tickets SET status = 'closed:" + p.getName() + ":unseen', answer = '" + answer + "' WHERE id='" + id + "'");
                    
//                    p.sendMessage("Closed ticket #" + id + " with answer: " + ChatColor.GRAY + answer);
                    p.sendMessage(translateDatabaseVariables(messages.get(17), id));
                    
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(b);
                    out.writeUTF("answer");
                    out.writeUTF("" + id);
                    out.writeUTF(author);
                    p.sendPluginMessage(this, "gReq", b.toByteArray());
                } else
                {
//                    p.sendMessage("The ticket #" + id + " is already closed");
                    p.sendMessage(translateDatabaseVariables(messages.get(26), id));
                }
            } else
            {
//                p.sendMessage("The ticket #" + id + " does not exist.");
                p.sendMessage(translateDatabaseVariables(messages.get(5), id));
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
                String status = rs.getString("status");

                if (status.split(":")[0].equals("closed"))
                {
//                    p.sendMessage(status.split(":")[1] + " closed your ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ")");
                    p.sendMessage(translateDatabaseVariables(messages.get(15), id).replace("%name", status.split(":")[1]));
//                    p.sendMessage("Answer: " + ChatColor.GRAY + answer);
                    p.sendMessage(translateDatabaseVariables(messages.get(16), id));

                    if (status.split(":").length == 3 && status.split(":")[2].equals("unseen"))
                    {
                        con.createStatement().execute("UPDATE greq_tickets SET status='closed:" + status.split(":")[1] + "' WHERE id='" + id + "'");
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
                String status = rs.getString("status");
                String answer = rs.getString("answer");

                if (status.split(":")[0].equals("closed") && !status.split(":")[1].equalsIgnoreCase(p.getName()))
                {
//                    p.sendMessage(status.split(":")[1] + " closed ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ") with answer: " + ChatColor.GRAY + answer);
                    p.sendMessage(translateDatabaseVariables(messages.get(18), id).replace("%name", status.split(":")[1]));
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
            p.sendMessage(translateDatabaseVariables(messages.get(19), id));

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
                String status = rs.getString("status");
                if (status.equals("open"))
                {

                    con.createStatement().execute("UPDATE greq_tickets SET status='claimed:" + p.getName() + "' WHERE id='" + id + "'");

                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("claim");
                    out.writeUTF(author);
                    out.writeUTF("" + id);
                    p.sendPluginMessage(this, "gReq", out.toByteArray());

//                    p.sendMessage("You are now handling ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ")");
                    p.sendMessage(translateDatabaseVariables(messages.get(20), id));
                } else
                {
//                    p.sendMessage("The ticket #" + id + " is not open");
                    p.sendMessage(translateDatabaseVariables(messages.get(22), id));
                }
            } else
            {
//                p.sendMessage("The ticket #" + id + " does not exist.");
                p.sendMessage(translateDatabaseVariables(messages.get(5), id));
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
                String status = rs.getString("status");
                if (status.contains("claimed:" + p.getName()))
                {

                    con.createStatement().execute("UPDATE greq_tickets SET status='open' WHERE id='" + id + "' AND status LIKE '%claimed:" + p.getName() + "%'");

                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("unclaim");
                    out.writeUTF(author);
                    out.writeUTF("" + id);
                    out.writeUTF(p.getName());
                    p.sendPluginMessage(this, "gReq", out.toByteArray());

//                    p.sendMessage("You are no longer handling ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ").");
                    p.sendMessage(translateDatabaseVariables(messages.get(23), id));
                } else
                {
//                    p.sendMessage("The ticket #" + id + " is not claimed by you");
                    p.sendMessage(translateDatabaseVariables(messages.get(25), id));
                }
            } else
            {
//                p.sendMessage("The ticket #" + id + " does not exist.");
                p.sendMessage(translateDatabaseVariables(messages.get(5), id));
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
                String status = rs.getString("status");

                if (status.split(":")[0].startsWith("claimed"))
                {
//                    p.sendMessage(status.split(":")[1] + " is now handling your request #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ")");
                    p.sendMessage(translateDatabaseVariables(messages.get(1), id).replace("%name", status.split(":")[1]));
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
                String status = rs.getString("status");

                if (status.split(":")[0].startsWith("claimed"))
                {
//                    p.sendMessage(status.split(":")[1] + " is now handling ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ")");
                    p.sendMessage(translateDatabaseVariables(messages.get(21), id).replace("%name", status.split(":")[1]));
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
                String status = rs.getString("status");

                if (status.split(":")[0].startsWith("open"))
                {
//                    p.sendMessage("Your request #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ") is no longer being handled");
                    p.sendMessage(translateDatabaseVariables(messages.get(2), id).replace("%name", unclaimer));
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
                String status = rs.getString("status");

                if (status.split(":")[0].startsWith("open"))
                {
//                    p.sendMessage(status.split(":")[1] + " is no longer handling ticket #" + id + " (" + ChatColor.GRAY + text + ChatColor.RESET + ")");
                    p.sendMessage(translateDatabaseVariables(messages.get(24), id).replace("%name", unclaimer));
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
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM greq_tickets WHERE author='" + p.getName() + "' AND (status='open' OR status LIKE '%claimed%')");
            if (rs.next())
            {
                rs.last();
                int size = rs.getRow();
                rs.first();
//                p.sendMessage("You currently have " + size + " open tickets:");
                p.sendMessage(messages.get(3).replace("%tickets", "" + size));
                do
                {
                    int id = rs.getInt("id");
//                    String text = rs.getString("text");
//                    String status = rs.getString("status");
//
//                    if (status.equals("open"))
//                    {
//                        p.sendMessage("[#" + id + "] " + ChatColor.GRAY + text + ChatColor.RESET + " (Status: Open)");
//                    } else if (status.startsWith("claimed"))
//                    {
//                        String claimer = status.split(":")[1];
//                        p.sendMessage("[#" + id + "] " + ChatColor.GRAY + text + ChatColor.RESET + " (Status: Claimed by " + claimer + ")");
//                    }
                    p.sendMessage(translateDatabaseVariables(messages.get(7), id));
                } while (rs.next());
            } else
            {
//                p.sendMessage("You have no open tickets.");
                p.sendMessage(messages.get(31));
            }
            con.close();
        } catch (ClassNotFoundException | SQLException ex)
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
