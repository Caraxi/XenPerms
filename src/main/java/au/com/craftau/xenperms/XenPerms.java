package au.com.craftau.xenperms;

import au.com.craftau.xenperms.commands.RankCommand;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ProfileBanEntry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("xenperms")
public class XenPerms {
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public static XenPerms Instance;

    public static XenPermsConfig Config;
    private static ForgeConfigSpec ConfigSpec;

    private Map<String, Rank> ranks;

    public XenPerms() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        Instance = this;
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
        Config = new XenPermsConfig(configBuilder);
        ConfigSpec = configBuilder.build();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigSpec);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("XenPerms is starting...");
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        ranks = Rank.parseRanks(Config.Ranks.get());
        ConfigSpec.save();
        LOGGER.info("XenPerms is loaded");
        pingDatabase();
    }

    @SubscribeEvent
    public void onRegisterCommandEvent(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> commandDispatcher = event.getDispatcher();
        RankCommand.register(commandDispatcher);
    }

    @SubscribeEvent
    public void onPlayerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();
        UpdatePlayerRank(player);
    }

    private void pingDatabase() {
        String hostname = Config.DB_Hostname.get();
        String port = Config.DB_Port.get();
        String database = Config.DB_Database.get();
        String user = Config.DB_Username.get();
        String password = Config.DB_Password.get();

        LOGGER.debug(String.format("Using %s @ %s:%s", database, hostname, port));

        Connection conn = null;
        PreparedStatement q = null;
        ResultSet rs = null;

        ArrayList<String> list = new ArrayList<>();

        try {
            if (hostname == null || hostname.equals("")
                    || port == null || port.equals("")
                    || database == null || database.equals("")
                    || user == null || user.equals("")
                    || password == null || password.equals("") || password.equals("password")) {
                LOGGER.fatal("Please configure the database settings");
            } else {
                conn = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database, user, password);
                String query = "SELECT count(*) as c FROM xf_user";
                q = conn.prepareStatement(query);
                rs = q.executeQuery();
            }


        } catch (SQLException | UncheckedExecutionException e) {
            LOGGER.error(e.toString());
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (Exception e) {
            }
            try {
                if (q != null) q.close();
            } catch (Exception e) {
            }
            try {
                if (conn != null) conn.close();
            } catch (Exception e) {
            }
        }
    }

    private ArrayList<String> getUserGroups(PlayerEntity player) {

        String hostname = Config.DB_Hostname.get();
        String port = Config.DB_Port.get();
        String database = Config.DB_Database.get();
        String user = Config.DB_Username.get();
        String password = Config.DB_Password.get();

        LOGGER.debug(String.format("Using %s @ %s:%s", database, hostname, port));

        Connection conn = null;
        PreparedStatement usergroups = null;
        ResultSet rs = null;

        ArrayList<String> list = new ArrayList<>();

        try {
            if (hostname == null || hostname.equals("")
                    || port == null || port.equals("")
                    || database == null || database.equals("")
                    || user == null || user.equals("")
                    || password == null || password.equals("") || password.equals("password")) {
                LOGGER.fatal("Please configure the database settings");
            } else {

                conn = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database, user, password);

                String query = "SELECT `user_group_id`, `secondary_group_ids`, `xf_user_ban`.`ban_date` " +
                        "FROM `xf_user` " +
                        "LEFT JOIN `xf_user_ban` ON `xf_user`.`user_id` = `xf_user_ban`.`user_id` " +
                        "WHERE `xf_user`.`username` = ?";

                usergroups = conn.prepareStatement(query);
                usergroups.setString(1, player.getName().getString());
                rs = usergroups.executeQuery();

                // we have at least one row of results, we expect only 1 row
                if (rs.next()) {

                    // If not NULL, getInt returns 0 for NULL
                    if (rs.getInt("ban_date") != 0) {
                        LOGGER.warn("User has been banned on the forums");
                        banUser(player);
                    } else {

                        list.add(rs.getString("user_group_id"));

                        String groupids = rs.getString("secondary_group_ids");
                        if (!groupids.equals("")) {
                            list.addAll(Arrays.asList(groupids.split(",")));
                        }

                        Collections.reverse(list);
                    }
                }

            }


        } catch (SQLException | UncheckedExecutionException e) {
            LOGGER.error(e.toString());
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (Exception e) {
            }
            try {
                if (usergroups != null) usergroups.close();
            } catch (Exception e) {
            }
            try {
                if (conn != null) conn.close();
            } catch (Exception e) {
            }
        }

        return list;
    }

    public static String commandBuilder(String format, String playerName, String groupName) {
        return format
                .replace("%player%", playerName)
                .replace("%group%", groupName)
                .replace("%groupLower%", groupName.toLowerCase());
    }

    public void UpdatePlayerRank(PlayerEntity player) {
        String name = player.getName().getString();
        LOGGER.info("Fetching rank for " + name);

        ArrayList<String> userGroups = this.getUserGroups(player);

        if (userGroups != null) {
            Rank rank = null;
            for (String s : userGroups) {
                if (ranks.containsKey(s)) {
                    LOGGER.info(name + " is in group: " + ranks.get(s).name);
                    if (rank == null || ranks.get(s).index > rank.index) {
                        rank = ranks.get(s);
                    }
                }
            }

            if (rank != null) {
                LOGGER.info(name + "'s highest group is: " + rank.name);
                if (Config.RankChangedMessage.get().length() > 0) {
                    String updateMessage = commandBuilder(Config.RankChangedMessage.get(), name, rank.name);
                    player.sendStatusMessage(new StringTextComponent(updateMessage), true);
                }

                for (String cmd : Config.Commands.get()) {
                    String command = commandBuilder(cmd, name, rank.name);

                    LOGGER.info("Execute Command: " + command);
                    try {
                        player.getServer().getCommandManager().getDispatcher().execute(command, player.getServer().getCommandSource());
                    } catch (Exception ex) {
                        LOGGER.error(ex.getMessage());
                    }
                }
            } else {
                LOGGER.info(name + " is in no groups.");
            }

        } else {
            LOGGER.warn("Failed to retrieve user groups for '" + name + "'.");
        }
    }

    public void SendPlayerMessage(PlayerEntity player, String message) {
        player.sendMessage(new StringTextComponent(message), null);
    }

    public void banUser(PlayerEntity player) {

        MinecraftServer server = player.getServer();
        GameProfile gp = server.getPlayerProfileCache().getGameProfileForUsername(player.getName().getString());

        if (gp == null) {
            LOGGER.warn("Failed to apply ban");
        } else {
            ProfileBanEntry banEntry = new ProfileBanEntry(gp, (Date) null, "XenPerm", (Date) null, "Forum Ban");
            server.getPlayerList().getBannedPlayers().addEntry(banEntry);
        }

        for (ServerPlayerEntity p : server.getPlayerList().getPlayers()) {
            if (p.getName().equals(player.getName())) {
                p.connection.disconnect(new StringTextComponent("Forum Ban"));
            }
        }
    }
}
