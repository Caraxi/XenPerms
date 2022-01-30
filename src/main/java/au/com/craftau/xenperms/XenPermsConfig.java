package au.com.craftau.xenperms;

import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class XenPermsConfig {

    public ForgeConfigSpec.ConfigValue<String> DB_Hostname;
    public ForgeConfigSpec.ConfigValue<String> DB_Port;
    public ForgeConfigSpec.ConfigValue<String> DB_Database;
    public ForgeConfigSpec.ConfigValue<String> DB_Username;
    public ForgeConfigSpec.ConfigValue<String> DB_Password;

    public ForgeConfigSpec.ConfigValue<ArrayList<String>> Ranks;

    public ForgeConfigSpec.ConfigValue<ArrayList<String>> Commands;

    public ForgeConfigSpec.ConfigValue<String> RankChangedMessage;


    public XenPermsConfig(ForgeConfigSpec.Builder builder) {
        builder.push("xenperms");

        DB_Hostname = builder.define("db_hostname", "localhost");
        DB_Port = builder.define("db_port", "3306");
        DB_Database = builder.define("db_database", "xenforodb");
        DB_Username = builder.define("db_username", "username");
        DB_Password = builder.define("db_password", "password");

        ArrayList<String> defaultRanks = new ArrayList<String>();
        defaultRanks.add("2:Registered");
        defaultRanks.add("3:Donator");
        defaultRanks.add("4:Emerald");
        defaultRanks.add("5:Supporter");

        ArrayList<String> defaultCommands = new ArrayList<String>();
        defaultCommands.add("perms user %player% parent set %group%");
        defaultCommands.add("ranks set %player% %groupLower%");

        Ranks = builder.define("ranks", defaultRanks);
        Commands = builder.define("commands", defaultCommands);

        RankChangedMessage = builder.define("rank_changed_message", TextFormatting.GREEN + "Your rank has been set to %group%.");

        builder.pop();
    }
}
