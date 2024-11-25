package xyz.nikitacartes.easyauth.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.nikitacartes.easyauth.utils.AuthHelper;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

public class AccountCommand implements CommandExecutor {

    public void registerCommand(JavaPlugin plugin) {
        plugin.getCommand("account").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage("Usage: /account <unregister|changePassword>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "unregister":
                if (player.hasPermission("easyauth.commands.account.unregister")) {
                    if (args.length < 2) {
                        player.sendMessage("Please enter your password.");
                        return true;
                    }
                    unregister(player, args[1]);
                } else {
                    player.sendMessage("You do not have permission to use this command.");
                }
                break;

            case "changepassword":
                if (player.hasPermission("easyauth.commands.account.changePassword")) {
                    if (args.length < 3) {
                        player.sendMessage("Usage: /account changePassword <old password> <new password>");
                        return true;
                    }
                    changePassword(player, args[1], args[2]);
                } else {
                    player.sendMessage("You do not have permission to use this command.");
                }
                break;

            default:
                player.sendMessage("Unknown subcommand. Usage: /account <unregister|changePassword>");
                break;
        }
        return true;
    }

    private void unregister(Player player, String pass) {
        Bukkit.getScheduler().runTaskAsynchronously(JavaPlugin.getProvidingPlugin(getClass()), () -> {
            String uuid = ((PlayerAuth) player).easyAuth$getFakeUuid();
            if (AuthHelper.checkPassword(uuid, pass.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
                DB.deleteUserData(uuid);
                player.sendMessage("Your account has been deleted.");
                ((PlayerAuth) player).easyAuth$setAuthenticated(false);
                ((PlayerAuth) player).easyAuth$saveLastLocation(true);
                player.kickPlayer("Your account has been deleted.");
                playerCacheMap.remove(uuid);
            } else {
                player.sendMessage("Incorrect password.");
            }
        });
    }

    private void changePassword(Player player, String oldPass, String newPass) {
        Bukkit.getScheduler().runTaskAsynchronously(JavaPlugin.getProvidingPlugin(getClass()), () -> {
            String uuid = ((PlayerAuth) player).easyAuth$getFakeUuid();
            if (AuthHelper.checkPassword(uuid, oldPass.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
                if (newPass.length() < extendedConfig.minPasswordLength) {
                    player.sendMessage("Password is too short.");
                } else if (newPass.length() > extendedConfig.maxPasswordLength && extendedConfig.maxPasswordLength != -1) {
                    player.sendMessage("Password is too long.");
                } else {
                    playerCacheMap.get(uuid).password = AuthHelper.hashPassword(newPass.toCharArray());
                    player.sendMessage("Password updated successfully.");
                }
            } else {
                player.sendMessage("Incorrect old password.");
            }
        });
    }
}
