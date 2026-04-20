package com.richard.teamsync;

import com.google.inject.Provides;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
    name = "Team Sync",
    description = "Sync live RuneLite account state to team backend",
    tags = {"sync", "team", "richard", "discord"}
)
public class TeamSyncPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private TeamSyncConfig config;

    @Provides
    TeamSyncConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TeamSyncConfig.class);
    }

    @Override
    protected void startUp()
    {
        sendProfileSnapshot();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN || event.getGameState() == GameState.HOPPING)
        {
            sendHeartbeat(event.getGameState());
            sendProfileSnapshot();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (event.getSkill() != Skill.OVERALL)
        {
            sendProfileSnapshot();
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() == InventoryID.INVENTORY.getId())
        {
            if (config.sendInventory())
            {
                sendContainerSnapshot("inventory", client.getItemContainer(InventoryID.INVENTORY));
            }
        }
        else if (event.getContainerId() == InventoryID.EQUIPMENT.getId())
        {
            if (config.sendEquipment())
            {
                sendContainerSnapshot("equipment", client.getItemContainer(InventoryID.EQUIPMENT));
            }
        }
    }

    private String accountName()
    {
        return client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : "unknown";
    }

    private int totalLevel()
    {
        int total = 0;
        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL)
            {
                continue;
            }
            total += client.getRealSkillLevel(skill);
        }
        return total;
    }

    private Map<String, Object> basePayload()
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put("teamId", config.teamId());
        payload.put("clientId", config.clientId());
        payload.put("accountName", accountName());
        payload.put("world", client.getWorld());
        payload.put("combatLevel", client.getLocalPlayer() != null ? client.getLocalPlayer().getCombatLevel() : null);
        payload.put("totalLevel", totalLevel());
        payload.put("questPoints", client.getVarpValue(101));
        payload.put("observedAt", Instant.now().toString());
        return payload;
    }

    private void sendHeartbeat(GameState state)
    {
        Map<String, Object> payload = basePayload();
        payload.put("status", state == GameState.HOPPING ? "world-hopping" : "online");
        postJson("/v1/plugin/heartbeat", Json.stringify(payload));
    }

    private void sendProfileSnapshot()
    {
        Map<String, Object> payload = basePayload();
        payload.put("gameMode", detectMode().name().toLowerCase());
        payload.put("profileLabel", config.profileLabel());
        postJson("/v1/snapshot/profile", Json.stringify(payload));
    }

    private AccountMode detectMode()
    {
        // Placeholder for richer RuneLite/Jagex account-mode detection later.
        // For now, let profile labeling carry the operational meaning if exact detection is unavailable.
        return AccountMode.UNKNOWN;
    }

    private void sendContainerSnapshot(String containerName, ItemContainer container)
    {
        if (container == null)
        {
            return;
        }
        Map<String, Object> payload = basePayload();
        payload.put("container", containerName);

        List<Map<String, Object>> items = new ArrayList<>();
        Item[] raw = container.getItems();
        for (int i = 0; i < raw.length; i++)
        {
            Item item = raw[i];
            if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
            {
                continue;
            }
            Map<String, Object> itemRow = new HashMap<>();
            itemRow.put("itemId", item.getId());
            itemRow.put("quantity", item.getQuantity());
            itemRow.put("slot", i);
            items.add(itemRow);
        }
        payload.put("items", items);
        postJson("/v1/snapshot/" + containerName, Json.stringify(payload));
    }

    private void postJson(String path, String body)
    {
        HttpURLConnection conn = null;
        try
        {
            URL url = new URL(config.backendUrl() + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Team-Sync-Token", config.token());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream())
            {
                os.write(bytes);
            }
            conn.getResponseCode();
        }
        catch (IOException ignored)
        {
        }
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }
        }
    }
}
