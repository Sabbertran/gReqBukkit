package me.sabbertran.greqbukkit.commands;

import me.sabbertran.greqbukkit.GReqBukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TicketCommand implements CommandExecutor
{

    private GReqBukkit main;

    public TicketCommand(GReqBukkit main)
    {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 1 && args[0].equalsIgnoreCase("list"))
        {
            if (sender.hasPermission("greq.ticket.list"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player) sender;
                    main.sendOwnTicketList(p, false);
                    return true;
                } else
                {
//                    sender.sendMessage("You have to be a player to use this command.");
//                    sender.sendMessage(main.getMessages().get(33));
                    main.sendMessage(sender, main.getMessages().get(33), -1);
                    return true;
                }
            } else
            {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("list") && args[1].equalsIgnoreCase("closed"))
        {
            if (sender.hasPermission("greq.ticket.list"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player) sender;
                    main.sendOwnTicketList(p, true);
                    return true;
                } else
                {
//                    sender.sendMessage("You have to be a player to use this command.");
//                    sender.sendMessage(main.getMessages().get(33));
                    main.sendMessage(sender, main.getMessages().get(33), -1);
                    return true;
                }
            } else
            {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("comments"))
        {
            if (sender.hasPermission("greq.comments"))
            {
                try
                {
                    int id = Integer.parseInt(args[1]);
                    main.sendCommentList(sender, id);
                    return true;
                } catch (NumberFormatException ex)
                {
                    return false;
                }
            } else
            {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        } else if (args.length >= 4 && args[0].equalsIgnoreCase("comments") && args[1].equalsIgnoreCase("add"))
        {
            if (sender.hasPermission("greq.comments.add"))
            {
                try
                {
                    int id = Integer.parseInt(args[2]);

                    String comment = "";
                    for (int i = 3; i < args.length; i++)
                    {
                        comment = comment + args[i] + " ";
                    }
                    comment = comment.substring(0, comment.length() - 1);

                    main.addTicketComment(sender, id, comment);
                    return true;
                } catch (NumberFormatException ex)
                {
                    return false;
                }
            } else
            {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        } else
        {
            if (sender.hasPermission("greq.ticket"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player) sender;
                    if (args.length > 0)
                    {
                        String text = "";
                        for (int i = 0; i < args.length; i++)
                        {
                            text = text + args[i] + " ";
                        }
                        text = text.substring(0, text.length() - 1);
                        int id = main.createTicket(p, text);
//                        p.sendMessage("Ticket (ID: " + id + ") created. A staff member will have a look at your request as soon as possible.");
//                        p.sendMessage(main.translateDatabaseVariables(main.getMessages().get(0), id));
                        main.sendMessage(p, main.getMessages().get(0), id);
                        return true;
                    } else
                    {
                        return false;
                    }
                } else
                {
//                    sender.sendMessage("You have to be a player to use this command.");
//                    sender.sendMessage(main.getMessages().get(23));
                    main.sendMessage(sender, main.getMessages().get(23), -1);
                    return true;
                }
            } else
            {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        }
    }
}
