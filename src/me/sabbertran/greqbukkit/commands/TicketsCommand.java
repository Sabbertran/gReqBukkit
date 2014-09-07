package me.sabbertran.greqbukkit.commands;

import me.sabbertran.greqbukkit.GReqBukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TicketsCommand implements CommandExecutor
{

    private GReqBukkit main;

    public TicketsCommand(GReqBukkit main)
    {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
        {
            if (sender.hasPermission("greq.tickets.list"))
            {
                main.sendTicketList(sender, 5, 1);
                return true;
            } else
            {
//                sender.sendMessage("You have to be a player to use this command.");
                sender.sendMessage(main.getMessages().get(33));
                return true;
            }
        } else if (args.length == 1)
        {
            if (sender.hasPermission("greq.tickets.read"))
            {
                try
                {
                    int id = Integer.parseInt(args[0]);
                    main.sendTicketInfo(sender, id);
                    return true;
                } catch (NumberFormatException ex)
                {
                    return false;
                }
            } else
            {
//                sender.sendMessage("You don't have permission to use this command.");
                sender.sendMessage(main.getMessages().get(32));
                return true;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("page"))
        {
            if (sender.hasPermission("greq.tickets.page"))
            {
                try
                {
                    int page = Integer.parseInt(args[1]);
                    main.sendTicketList(sender, 5, page);
                    return true;
                } catch (NumberFormatException ex)
                {
                    return false;
                }
            } else
            {
//                sender.sendMessage("You don't have permission to use this command.");
                sender.sendMessage(main.getMessages().get(32));
                return true;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("claim"))
        {
            if (sender.hasPermission("greq.tickets.claim"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player) sender;
                    try
                    {
                        int id = Integer.parseInt(args[1]);
                        main.claimTicket(p, id);
                        return true;
                    } catch (NumberFormatException ex)
                    {
                        return false;
                    }
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
        } else if (args.length == 2 && args[0].equalsIgnoreCase("unclaim"))
        {
            if (sender.hasPermission("greq.tickets.unclaim"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player) sender;
                    try
                    {
                        int id = Integer.parseInt(args[1]);
                        main.unclaimTicket(p, id);
                        return true;
                    } catch (NumberFormatException ex)
                    {
                        return false;
                    }
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
        } else if (args.length == 2 && args[0].equalsIgnoreCase("tp"))
        {
            if (sender.hasPermission("greq.tickets.tp"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player) sender;
                    try
                    {
                        int id = Integer.parseInt(args[1]);
                        main.teleportToTicket(p, id);
                        return true;
                    } catch (NumberFormatException ex)
                    {
                        return false;
                    }
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
        } else if (args.length > 2 && args[0].equalsIgnoreCase("close"))
        {
            if (sender.hasPermission("greq.tickets.close"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player) sender;
                    try
                    {
                        int id = Integer.parseInt(args[1]);
                        String answer = "";
                        for (int i = 2; i < args.length; i++)
                        {
                            answer = answer + args[i] + " ";
                        }
                        answer = answer.substring(0, answer.length() - 1);
                        main.closeTicket(p, id, answer);
                        return true;
                    } catch (NumberFormatException ex)
                    {
                        return false;
                    }
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
            return false;
        }
    }
}