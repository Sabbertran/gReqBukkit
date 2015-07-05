package me.sabbertran.greqbukkit.commands;

import java.text.SimpleDateFormat;
import java.util.Date;
import me.sabbertran.greqbukkit.GReqBukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TicketsCommand implements CommandExecutor {

    private GReqBukkit main;

    public TicketsCommand(GReqBukkit main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length == 0) {
            if (sender.hasPermission("greq.tickets.list")) {
                main.sendTicketList(sender, 5, 1);
                return true;
            } else {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        } else if (args.length == 1) {
            if (sender.hasPermission("greq.tickets.read")) {
                try {
                    int id = Integer.parseInt(args[0]);
                    main.sendTicketInfo(sender, id);
                    return true;
                } catch (NumberFormatException ex) {
                    return false;
                }
            } else {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("page")) {
            if (sender.hasPermission("greq.tickets.page")) {
                try {
                    int page = Integer.parseInt(args[1]);
                    main.sendTicketList(sender, 5, page);
                    return true;
                } catch (NumberFormatException ex) {
                    return false;
                }
            } else {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            if (sender.hasPermission("greq.tickets.claim")) {
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    try {
                        int id = Integer.parseInt(args[1]);
                        main.claimTicket(p, id);
                        return true;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                } else {
//                    sender.sendMessage("You have to be a player to use this command.");
//                    sender.sendMessage(main.getMessages().get(33));
                    main.sendMessage(sender, main.getMessages().get(33), -1);
                    return true;
                }
            } else {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("unclaim")) {
            if (sender.hasPermission("greq.tickets.unclaim")) {
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    try {
                        int id = Integer.parseInt(args[1]);
                        main.unclaimTicket(p, id);
                        return true;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                } else {
//                    sender.sendMessage("You have to be a player to use this command.");
//                    sender.sendMessage(main.getMessages().get(33));
                    main.sendMessage(sender, main.getMessages().get(33), -1);
                    return true;
                }
            } else {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            if (sender.hasPermission("greq.tickets.tp")) {
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    try {
                        int id = Integer.parseInt(args[1]);
                        main.teleportToTicket(p, id);
                        return true;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                } else {
//                    sender.sendMessage("You have to be a player to use this command.");
//                    sender.sendMessage(main.getMessages().get(33));
                    main.sendMessage(sender, main.getMessages().get(33), -1);
                    return true;
                }
            } else {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("close")) {
            if (sender.hasPermission("greq.tickets.close")) {
                try {
                    int id = Integer.parseInt(args[1]);
                    main.closeTicket(sender, id, "");
                    return true;
                } catch (NumberFormatException ex) {
                    return false;
                }
            } else {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        } else if (args.length > 2 && args[0].equalsIgnoreCase("close")) {
            if (sender.hasPermission("greq.tickets.close")) {
                try {
                    int id = Integer.parseInt(args[1]);
                    String answer = "";
                    for (int i = 2; i < args.length; i++) {
                        answer = answer + args[i] + " ";
                    }
                    answer = answer.substring(0, answer.length() - 1);
                    main.closeTicket(sender, id, answer);
                    return true;
                } catch (NumberFormatException ex) {
                    return false;
                }
            } else {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("comments")) {
            if (sender.hasPermission("greq.comments")) {
                try {
                    int id = Integer.parseInt(args[1]);
                    main.sendCommentList(sender, id);
                    return true;
                } catch (NumberFormatException ex) {
                    return false;
                }
            } else {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        } else if (args.length >= 4 && args[0].equalsIgnoreCase("comments") && args[1].equalsIgnoreCase("add")) {
            if (sender.hasPermission("greq.comments.add")) {
                try {
                    int id = Integer.parseInt(args[2]);

                    String comment = "";
                    for (int i = 3; i < args.length; i++) {
                        comment = comment + args[i] + " ";
                    }
                    comment = comment.substring(0, comment.length() - 1);

                    main.addTicketComment(sender, id, comment);
                    return true;
                } catch (NumberFormatException ex) {
                    return false;
                }
            } else {
//                sender.sendMessage("You don't have permission to use this command.");
//                sender.sendMessage(main.getMessages().get(32));
                main.sendMessage(sender, main.getMessages().get(32), -1);
                return true;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("purge")) {
            if (args[1].equalsIgnoreCase("all")) {
                if (sender.hasPermission("greq.tickets.purge.all")) {
                    main.getPendingPurges().put(sender.getName(), 0);
                    main.sendMessage(sender, main.getMessages().get(44), -1);
                    return true;
                } else {
                    main.sendMessage(sender, main.getMessages().get(32), -1);
                    return true;
                }
            } else if (args[1].equalsIgnoreCase("closed")) {
                if (sender.hasPermission("greq.tickets.purge.closed")) {
                    main.getPendingPurges().put(sender.getName(), 1);
                    main.sendMessage(sender, main.getMessages().get(45), -1);
                    return true;
                } else {
                    main.sendMessage(sender, main.getMessages().get(32), -1);
                    return true;
                }
            } else {
                main.sendMessage(sender, main.getMessages().get(46), -1);
                return true;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("purge") && args[1].equalsIgnoreCase("confirm")) {
            if (sender.hasPermission("greq.tickets.purge.confirm")) {
                if (main.getPendingPurges().containsKey(sender.getName())) {
                    main.purgeTickets(main.getPendingPurges().get(sender.getName()));
                    main.getPendingPurges().remove(sender.getName());
                    main.sendMessage(sender, main.getMessages().get(43), -1);
                    return true;
                } else {
                    main.sendMessage(sender, main.getMessages().get(32), -1);
                    return true;
                }
            } else {
                main.sendMessage(sender, main.getMessages().get(47), -1);
                return true;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("block")) {
            OfflinePlayer p = main.getServer().getOfflinePlayer(args[1]);
            long time = 0;
            if (args[3].endsWith("s")) {
                time = Integer.parseInt(args[3].replace("s", "")) * 1000;
            } else if (args[3].endsWith("m")) {
                time = Integer.parseInt(args[3].replace("m", "")) * 60 * 1000;
            } else if (args[3].endsWith("h")) {
                time = Integer.parseInt(args[3].replace("h", "")) * 60 * 60 * 1000;
            } else if (args[3].endsWith("d")) {
                time = Integer.parseInt(args[3].replace("d", "")) * 24 * 60 * 60 * 1000;
            }
            Date until = new Date();
            until.setTime(until.getTime() + time);
            main.getBlockedUntil().put(p, until);
            main.sendMessage(sender, main.getMessages().get(49).replace("%name", p.getName()).replace("%date", new SimpleDateFormat(main.getMessages().get(30)).format(main.getBlockedUntil().get(p))), -1);
            return true;
        } else {
            return false;
        }
    }
}
