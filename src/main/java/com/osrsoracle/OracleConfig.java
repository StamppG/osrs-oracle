package com.osrsoracle;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("oracle")
public interface OracleConfig extends Config
{
	@ConfigItem(
			keyName = "backendUrl",
			name = "Backend URL",
			description = "Backend URL used for account synchronization"
	)
	default String backendUrl()
	{
		return "";
	}

	@ConfigItem(
			keyName = "writeToken",
			name = "Write Token",
			description = "Secret token used to upload account data"
	)
	default String writeToken()
	{
		return "";
	}
}