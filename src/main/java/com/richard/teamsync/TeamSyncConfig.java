package com.richard.teamsync;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("teamsync")
public interface TeamSyncConfig extends Config
{
    @ConfigItem(
        keyName = "backendUrl",
        name = "Backend URL",
        description = "Sync API base URL"
    )
    default String backendUrl()
    {
        return "http://127.0.0.1:4317";
    }

    @ConfigItem(
        keyName = "teamId",
        name = "Team ID",
        description = "Logical team identifier"
    )
    default String teamId()
    {
        return "default-team";
    }

    @ConfigItem(
        keyName = "clientId",
        name = "Client ID",
        description = "Unique ID for this plugin install"
    )
    default String clientId()
    {
        return "client-1";
    }

    @ConfigItem(
        keyName = "profileLabel",
        name = "Profile Label",
        description = "Logical team label for this character/build"
    )
    default String profileLabel()
    {
        return "default";
    }

    @ConfigItem(
        keyName = "sendInventory",
        name = "Send inventory",
        description = "Send live inventory snapshots"
    )
    default boolean sendInventory()
    {
        return true;
    }

    @ConfigItem(
        keyName = "sendEquipment",
        name = "Send equipment",
        description = "Send live equipment snapshots"
    )
    default boolean sendEquipment()
    {
        return true;
    }

    @ConfigItem(
        keyName = "token",
        name = "Token",
        description = "Sync API token"
    )
    default String token()
    {
        return "change-me";
    }
}
