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
                    main.sendOwnTicketList(p);
                    return true;
                } else
                {
//                    sender.sendMessage("You have to be a player to use this command.");
                    sender.sendMessage(main.getMessages().get(33));
                    return true;
                }
            } else
            {
//                sender.sendMessage("You don't have permission to use this command.");
                sender.sendMessage(main.getMessages().get(32));
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
                        p.sendMessage(main.translateDatabaseVariables(main.getMessages().get(0), id));
                        return true;
                    } else
                    {
                        return false;
                    }
                } else
                {
//                    sender.sendMessage("You have to be a player to use this command.");
                    sender.sendMessage(main.getMessages().get(23));
                    return true;
                }
            } else
            {
//                sender.sendMessage("You don't have permission to use this command.");
                sender.sendMessage(main.getMessages().get(32));
                return true;
            }
        }
    }
}
