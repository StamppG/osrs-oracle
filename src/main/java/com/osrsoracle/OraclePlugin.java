package com.osrsoracle;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Skill;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Varbits;
import net.runelite.api.MenuAction;
import net.runelite.api.SpriteID;
import net.runelite.api.FontID;

import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;

import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.ScriptID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.JavaScriptCallback;


import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Instant;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;


@Slf4j
@PluginDescriptor(
		name = "Oracle"
)
public class OraclePlugin extends Plugin
{
	@Inject
	private Client client;


	@Inject
	private OracleConfig config;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	private long lastUploadTime = 0;

	private Item[] cachedBankItems = null;

	private Item[] cachedSeedVaultItems = null;


	/*
	 * PUSH-TRIGGER BASELINE
	 *
	 * These values are initialized once after login. From then on we only
	 * send an immediate snapshot when one of the explicitly approved
	 * progression events occurs.
	 */
	private boolean pendingLoginPush = false;
	private GameState previousGameState = GameState.UNKNOWN;
	private boolean pushTriggerBaselineReady = false;

	private final Map<Skill, Integer> lastKnownLevels =
			new LinkedHashMap<>();

	private final Map<Quest, Boolean> lastKnownQuestFinished =
			new LinkedHashMap<>();

	private int lastKnownCaEasy = 0;
	private int lastKnownCaMedium = 0;
	private int lastKnownCaHard = 0;
	private int lastKnownCaElite = 0;
	private int lastKnownCaMaster = 0;
	private int lastKnownCaGrandmaster = 0;


	private long lastCollectionCaptureTime = 0;
	private final Map<String, String> cachedCollectionLogPages =
			new LinkedHashMap<>();

	/*
	 * INSTANT COLLECTION LOG EXPERIMENT
	 *
	 * Mirrors the transport mechanism used by RuneProfile/WikiSync:
	 * opening Search causes the server to transmit obtained Collection Log
	 * item IDs and quantities via script 4100. Script 2240 then restores
	 * the normal Collection Log view.
	 *
	 * For this experiment we collect/log the stream only. We do NOT yet
	 * replace the proven 124-page JSON schema until we verify the stream.
	 */
	private static final int COLLECTION_DELAYED_TRANSMIT = 4100;
	private static final int COLLECTION_LOG_SETUP = 7797;
	private static final int COLLECTION_INIT_SCRIPT = 2240;


	private boolean collectionInstantRetrieval = false;
	private int collectionInstantLastTransmitTick = -1;
	private long collectionInstantStartedNanos = 0L;
	private String collectionInstantCapturedAt = null;

	private final Map<Integer, Integer> instantCollectionLogItems =
			new LinkedHashMap<>();

	/*
	 * WIKISYNC-STYLE MANUAL COLLECTION LOG BUTTON
	 *
	 * This deliberately mirrors WikiSync's native-looking 9-sprite
	 * metal button construction and top-right placement.
	 */
	private static final String COLLECTION_SYNC_LABEL =
			"Show Me Your ClogHole";

	private static final int[] COLLECTION_BUTTON_SPRITES_INACTIVE = {
			SpriteID.DIALOG_BACKGROUND,
			SpriteID.WORLD_MAP_BUTTON_METAL_CORNER_TOP_LEFT,
			SpriteID.WORLD_MAP_BUTTON_METAL_CORNER_TOP_RIGHT,
			SpriteID.WORLD_MAP_BUTTON_METAL_CORNER_BOTTOM_LEFT,
			SpriteID.WORLD_MAP_BUTTON_METAL_CORNER_BOTTOM_RIGHT,
			SpriteID.WORLD_MAP_BUTTON_EDGE_LEFT,
			SpriteID.WORLD_MAP_BUTTON_EDGE_TOP,
			SpriteID.WORLD_MAP_BUTTON_EDGE_RIGHT,
			SpriteID.WORLD_MAP_BUTTON_EDGE_BOTTOM
	};

	private static final int[] COLLECTION_BUTTON_SPRITES_ACTIVE = {
			SpriteID.RESIZEABLE_MODE_SIDE_PANEL_BACKGROUND,
			SpriteID.EQUIPMENT_BUTTON_METAL_CORNER_TOP_LEFT_HOVERED,
			SpriteID.EQUIPMENT_BUTTON_METAL_CORNER_TOP_RIGHT_HOVERED,
			SpriteID.EQUIPMENT_BUTTON_METAL_CORNER_BOTTOM_LEFT_HOVERED,
			SpriteID.EQUIPMENT_BUTTON_METAL_CORNER_BOTTOM_RIGHT_HOVERED,
			SpriteID.EQUIPMENT_BUTTON_EDGE_LEFT_HOVERED,
			SpriteID.EQUIPMENT_BUTTON_EDGE_TOP_HOVERED,
			SpriteID.EQUIPMENT_BUTTON_EDGE_RIGHT_HOVERED,
			SpriteID.EQUIPMENT_BUTTON_EDGE_BOTTOM_HOVERED
	};

	private static final int COLLECTION_BUTTON_FONT_INACTIVE = 0xD6D6D6;
	private static final int COLLECTION_BUTTON_FONT_ACTIVE = 0xFFFFFF;

	// Same right-edge anchor WikiSync uses.
	private static final int COLLECTION_CLOSE_BUTTON_OFFSET = 28;
	private static final int COLLECTION_BUTTON_OFFSET =
			COLLECTION_CLOSE_BUTTON_OFFSET + 5;

	// Wider than WikiSync's 71px because our label is intentionally longer.
	private static final int COLLECTION_BUTTON_WIDTH = 132;

	private final List<Widget> collectionSyncWidgets =
			new ArrayList<>();


	@Override
	protected void startUp() throws Exception
	{
		log.info("OSRS Account Sync plugin started");
	}


	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Oracle stopped!");
	}


	@Subscribe
	public void onGameStateChanged(
			GameStateChanged gameStateChanged
	)
	{
		GameState newState =
				gameStateChanged.getGameState();

		/*
		 * Only arm the LOGIN push after a genuine login/world-hop transition.
		 *
		 * Teleports and region changes commonly do:
		 * LOGGED_IN -> LOADING -> LOGGED_IN
		 *
		 * so LOADING -> LOGGED_IN must not count as a login.
		 */
		if (
				newState == GameState.LOGGED_IN &&
						(
								previousGameState == GameState.LOGIN_SCREEN ||
										previousGameState == GameState.LOGGING_IN ||
										previousGameState == GameState.HOPPING
						)
		)
		{
			pendingLoginPush = true;
			pushTriggerBaselineReady = false;
		}
		else if (
				newState == GameState.LOGIN_SCREEN ||
						newState == GameState.HOPPING
		)
		{
			pendingLoginPush = false;
			pushTriggerBaselineReady = false;
		}

		previousGameState = newState;
	}



	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		/*
		 * BANK CACHE
		 */
		ItemContainer bank =
				client.getItemContainer(
						InventoryID.BANK
				);

		if (bank != null)
		{
			cachedBankItems =
					bank.getItems().clone();
		}

		/*
		 * SEED VAULT CACHE
		 */
		ItemContainer seedVault =
				client.getItemContainer(
						InventoryID.SEED_VAULT
				);

		if (seedVault != null)
		{
			cachedSeedVaultItems =
					seedVault.getItems().clone();
		}

		/*
		 * LOGIN PUSH
		 *
		 * Initialize every progression baseline before sending the login
		 * snapshot. That prevents existing completed quests/diaries/CAs or
		 * current levels from being mistaken for brand-new events.
		 */
		if (pendingLoginPush)
		{
			initializePushTriggerBaseline();

			pendingLoginPush = false;
			pushTriggerBaselineReady = true;

			log.info(
					"PUSH TRIGGER: login"
			);

			sendSnapshot("LOGIN");
			lastUploadTime =
					System.currentTimeMillis();
		}
		else if (pushTriggerBaselineReady)
		{
			checkProgressionPushTriggers();
		}


		/*
		 * INSTANT COLLECTION LOG COMPLETION
		 *
		 * RuneProfile waits until two full game ticks have passed since the
		 * last delayed-transmit event. At that point the server-side dump is
		 * considered complete.
		 */
		if (
				collectionInstantRetrieval &&
						collectionInstantLastTransmitTick != -1 &&
						collectionInstantLastTransmitTick + 2 <
								client.getTickCount()
		)
		{
			long elapsedMs =
					collectionInstantStartedNanos > 0L
							? (System.nanoTime() -
							collectionInstantStartedNanos) / 1_000_000L
							: -1L;

			log.info(
					"CLOG INSTANT COMPLETE: uniqueObtainedItems={} elapsedMs={} lastTransmitTick={} currentTick={}",
					instantCollectionLogItems.size(),
					elapsedMs,
					collectionInstantLastTransmitTick,
					client.getTickCount()
			);

			collectionInstantCapturedAt =
					Instant.now().toString();

			collectionInstantRetrieval = false;
			collectionInstantLastTransmitTick = -1;
			collectionInstantStartedNanos = 0L;

			setCollectionSyncButtonLabel(
					COLLECTION_SYNC_LABEL
			);

			/*
			 * Upload exactly once after the complete server-side item stream.
			 * The Worker will combine this compact obtained-item map with the
			 * stored full page/item definition to rebuild our rich 124-page
			 * Collection Log representation.
			 */
			sendSnapshot("CLOG_MANUAL");
			lastUploadTime = System.currentTimeMillis();
		}


		/*
		 * NORMAL 15-MINUTE HEARTBEAT
		 */
		long now =
				System.currentTimeMillis();

		if (now - lastUploadTime >=
				15 * 60 * 1000)
		{
			sendSnapshot("HEARTBEAT");
			lastUploadTime = now;
		}
	}



	private void initializePushTriggerBaseline()
	{
		/*
		 * LEVELS
		 */
		lastKnownLevels.clear();

		for (Skill skill : Skill.values())
		{
			if (skill == Skill.OVERALL)
			{
				continue;
			}

			lastKnownLevels.put(
					skill,
					client.getRealSkillLevel(skill)
			);
		}


		/*
		 * QUESTS
		 */
		lastKnownQuestFinished.clear();

		for (Quest quest : Quest.values())
		{
			lastKnownQuestFinished.put(
					quest,
					quest.getState(client) ==
							QuestState.FINISHED
			);
		}

		/*
		 * COMBAT ACHIEVEMENTS
		 */
		lastKnownCaEasy =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_EASY
				);

		lastKnownCaMedium =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_MEDIUM
				);

		lastKnownCaHard =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_HARD
				);

		lastKnownCaElite =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_ELITE
				);

		lastKnownCaMaster =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_MASTER
				);

		lastKnownCaGrandmaster =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_GRANDMASTER
				);


	}

	/*
	 * LEVEL-UP PUSH
	 *
	 * RuneLite already emits the visible level-up game message. This is more
	 * reliable than polling XP/stat changes because the message is guaranteed
	 * at the moment the player actually levels.
	 */
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = event.getMessage();

		if (!message.startsWith("Congratulations, you've just advanced your "))
		{
			return;
		}

		java.util.regex.Matcher matcher =
				java.util.regex.Pattern.compile(
						"Congratulations, you've just advanced your (.+?) level\\. You are now level (\\d+)\\."
				).matcher(message);

		if (!matcher.matches())
		{
			return;
		}

		String skillName = matcher.group(1);

		log.info(
				"PUSH TRIGGER: level up {}",
				skillName
		);

		sendSnapshot(
				"LEVEL_UP:" +
						skillName.toUpperCase()
		);

		lastUploadTime =
				System.currentTimeMillis();
	}


	private void checkProgressionPushTriggers()
	{
		List<String> pushReasons =
				new ArrayList<>();

		/*
		 * COMBAT ACHIEVEMENT COMPLETED
		 *
		 * The six tier counters increase when a CA is completed. Comparing
		 * those counters is far cheaper than scanning every CA task each tick.
		 */
		int currentCaEasy =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_EASY
				);

		int currentCaMedium =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_MEDIUM
				);

		int currentCaHard =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_HARD
				);

		int currentCaElite =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_ELITE
				);

		int currentCaMaster =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_MASTER
				);

		int currentCaGrandmaster =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_GRANDMASTER
				);

		if (
				currentCaEasy > lastKnownCaEasy ||
						currentCaMedium > lastKnownCaMedium ||
						currentCaHard > lastKnownCaHard ||
						currentCaElite > lastKnownCaElite ||
						currentCaMaster > lastKnownCaMaster ||
						currentCaGrandmaster >
								lastKnownCaGrandmaster
		)
		{
			log.info(
					"PUSH TRIGGER: Combat Achievement completed"
			);

			pushReasons.add(
					"COMBAT_ACHIEVEMENT"
			);
		}

		lastKnownCaEasy = currentCaEasy;
		lastKnownCaMedium = currentCaMedium;
		lastKnownCaHard = currentCaHard;
		lastKnownCaElite = currentCaElite;
		lastKnownCaMaster = currentCaMaster;
		lastKnownCaGrandmaster =
				currentCaGrandmaster;



		/*
		 * QUEST COMPLETED
		 *
		 * Only NOT-FINISHED -> FINISHED transitions push. Quest starts and
		 * intermediate progress changes are deliberately ignored.
		 */
		for (Quest quest : Quest.values())
		{
			boolean currentFinished =
					quest.getState(client) ==
							QuestState.FINISHED;

			boolean previousFinished =
					lastKnownQuestFinished.getOrDefault(
							quest,
							currentFinished
					);

			if (
					!previousFinished &&
							currentFinished
			)
			{
				log.info(
						"PUSH TRIGGER: quest completed name='{}'",
						quest.getName()
				);

				pushReasons.add(
						"QUEST_COMPLETED:" +
								quest.getName()
				);
			}

			lastKnownQuestFinished.put(
					quest,
					currentFinished
			);
		}


		/*
		 * Coalesce simultaneous progression events into one upload per game
		 * tick while retaining every reason in the diagnostic label.
		 */
		if (!pushReasons.isEmpty())
		{
			sendSnapshot(
					String.join(
							"+",
							pushReasons
					)
			);

			lastUploadTime =
					System.currentTimeMillis();
		}
	}



	@Subscribe
	public void onScriptPreFired(
			ScriptPreFired event
	)
	{
		/*
		 * RuneProfile/WikiSync fast path.
		 *
		 * Each script-4100 event carries a Collection Log item ID and the
		 * obtained quantity in arguments 1 and 2.
		 */
		if (event.getScriptId() == COLLECTION_DELAYED_TRANSMIT)
		{
			/*
			 * Other installed plugins can trigger the same Collection Log
			 * server transmit. Only consume events while OUR retrieval is
			 * active, otherwise an unrelated sync could mutate our snapshot.
			 */
			if (!collectionInstantRetrieval)
			{
				return;
			}

			Object[] args =
					event.getScriptEvent().getArguments();

			if (
					args != null &&
							args.length >= 3 &&
							args[1] instanceof Integer &&
							args[2] instanceof Integer
			)
			{
				int itemId = (Integer) args[1];
				int quantity = (Integer) args[2];

				collectionInstantLastTransmitTick =
						client.getTickCount();

				instantCollectionLogItems.put(
						itemId,
						quantity
				);
			}
			else
			{
			}

			return;
		}

		/*
		 * Retain the earlier DRAW_LIST stack probe for comparison.
		 */
		if (event.getScriptId() != ScriptID.COLLECTION_DRAW_LIST)
		{
			return;
		}

		int size = client.getIntStackSize();
		int[] stack = client.getIntStack();

		int stackStart = Math.max(0, size - 12);

		StringJoiner values =
				new StringJoiner(", ", "[", "]");

		for (int i = stackStart; i < size; i++)
		{
			values.add(i + ":" + stack[i]);
		}
	}


	@Subscribe
	public void onScriptPostFired(
			ScriptPostFired event
	)
	{
		/*
		 * INSTANT COLLECTION LOG START
		 *
		 * When the Collection Log setup script fires, activate Search once.
		 * That requests the server-side Collection Log item stream. Then
		 * rerun the Collection Log init script to restore the normal view.
		 *
		 * The guard is essential because runScript(2240) causes setup to
		 * fire again.
		 */
		if (event.getScriptId() == COLLECTION_LOG_SETUP)
		{
			removeCollectionSyncButton();
			addCollectionSyncButton();
			return;
		}

		if (event.getScriptId() !=
				ScriptID.COLLECTION_DRAW_LIST)
		{
			return;
		}

		long now =
				System.currentTimeMillis();

		/*
		 * Passive Collection Log viewing is throttled to avoid duplicate
		 * redraw spam.
		 */
		if (now - lastCollectionCaptureTime < 500)
		{
			return;
		}


		lastCollectionCaptureTime = now;

		Widget header =
				client.getWidget(
						InterfaceID.Collection.HEADER_TEXT
				);

		Widget itemsContainer =
				client.getWidget(
						InterfaceID.Collection.ITEMS_CONTENTS
				);

		if (
				header == null ||
						header.getChildren() == null ||
						header.getChildren().length == 0 ||
						header.getChild(0) == null ||
						itemsContainer == null ||
						itemsContainer.getChildren() == null
		)
		{
			log.info(
					"CLOG CAPTURE: required widgets not ready"
			);
			return;
		}

		String pageName =
				stripTags(
						header.getChild(0).getText()
				).trim();

		if (pageName.isEmpty())
		{
			log.info(
					"CLOG CAPTURE: page name was empty"
			);
			return;
		}

		StringJoiner itemsJson =
				new StringJoiner(
						",",
						"[",
						"]"
				);

		int obtainedCount = 0;
		int totalCount = 0;

		Widget[] itemChildren =
				itemsContainer.getChildren();

		for (Widget child : itemChildren)
		{
			if (
					child == null ||
							child.isHidden() ||
							child.getItemId() <= 0
			)
			{
				continue;
			}

			String itemName =
					stripTags(
							child.getName()
					).trim();

			if (itemName.isEmpty())
			{
				itemName =
						client
								.getItemDefinition(
										child.getItemId()
								)
								.getName();
			}

			boolean obtained =
					child.getOpacity() == 0;

			int quantity =
					obtained
							? Math.max(
							1,
							child.getItemQuantity()
					)
							: 0;

			if (obtained)
			{
				obtainedCount++;
			}

			totalCount++;

			itemsJson.add(
					String.format(
							"{\"name\":\"%s\",\"id\":%d,\"quantity\":%d,\"obtained\":%b}",
							escapeJson(itemName),
							child.getItemId(),
							quantity,
							obtained
					)
			);
		}

		String pageJson =
				String.format(
						"{\"capturedAt\":\"%s\",\"obtained\":%d,\"total\":%d,\"items\":%s}",
						escapeJson(
								Instant.now().toString()
						),
						obtainedCount,
						totalCount,
						itemsJson.toString()
				);

		cachedCollectionLogPages.put(
				pageName,
				pageJson
		);

		log.info(
				"CLOG CAPTURE: page='{}' obtained={}/{} cachedPages={}",
				pageName,
				obtainedCount,
				totalCount,
				cachedCollectionLogPages.size()
		);

	}


	private void startInstantCollectionLogSync()
	{
		if (collectionInstantRetrieval)
		{
			return;
		}

		Widget searchButton =
				client.getWidget(
						InterfaceID.Collection.SEARCH_TOGGLE
				);

		if (
				searchButton == null ||
						searchButton.isHidden()
		)
		{
			return;
		}

		collectionInstantRetrieval = true;
		collectionInstantLastTransmitTick = -1;
		collectionInstantStartedNanos =
				System.nanoTime();

		instantCollectionLogItems.clear();
		collectionInstantCapturedAt = null;

		setCollectionSyncButtonLabel(
				"Syncing..."
		);

		log.info(
				"CLOG INSTANT START: tick={} manual button requested full server transmit",
				client.getTickCount()
		);

		client.menuAction(
				-1,
				InterfaceID.Collection.SEARCH_TOGGLE,
				MenuAction.CC_OP,
				1,
				-1,
				"Search",
				null
		);

		client.runScript(
				COLLECTION_INIT_SCRIPT
		);
	}


	private void addCollectionSyncButton()
	{
		Widget parent =
				client.getWidget(
						InterfaceID.Collection.UNIVERSE
				);

		Widget searchButton =
				client.getWidget(
						InterfaceID.Collection.SEARCH_TOGGLE
				);

		Widget collectionLogContainer =
				client.getWidget(
						InterfaceID.Collection.INFINITY
				);

		Widget[] containerChildren;
		Widget draggableTopbar;

		if (
				parent == null ||
						searchButton == null ||
						collectionLogContainer == null ||
						(containerChildren =
								collectionLogContainer.getChildren()) == null ||
						containerChildren.length == 0 ||
						(draggableTopbar = containerChildren[0]) == null
		)
		{
			return;
		}

		final int w = COLLECTION_BUTTON_WIDTH;
		final int h = searchButton.getOriginalHeight();

		/*
		 * This is exactly the same right-side anchor position WikiSync uses.
		 * Because our label is longer, only the LEFT edge extends farther.
		 */
		final int x = COLLECTION_BUTTON_OFFSET;
		final int y = searchButton.getOriginalY();
		final int cornerDim = 9;

		final Widget[] spriteWidgets =
				new Widget[9];

		spriteWidgets[0] =
				parent.createChild(
								-1,
								WidgetType.GRAPHIC
						)
						.setSpriteId(
								COLLECTION_BUTTON_SPRITES_INACTIVE[0]
						)
						.setPos(x, y)
						.setSize(w, h)
						.setXPositionMode(
								WidgetPositionMode.ABSOLUTE_RIGHT
						)
						.setYPositionMode(
								searchButton.getYPositionMode()
						);

		spriteWidgets[1] =
				parent.createChild(
								-1,
								WidgetType.GRAPHIC
						)
						.setSpriteId(
								COLLECTION_BUTTON_SPRITES_INACTIVE[1]
						)
						.setXPositionMode(
								WidgetPositionMode.ABSOLUTE_RIGHT
						)
						.setSize(cornerDim, cornerDim)
						.setPos(
								x + (w - cornerDim),
								y
						);

		spriteWidgets[2] =
				parent.createChild(
								-1,
								WidgetType.GRAPHIC
						)
						.setSpriteId(
								COLLECTION_BUTTON_SPRITES_INACTIVE[2]
						)
						.setXPositionMode(
								WidgetPositionMode.ABSOLUTE_RIGHT
						)
						.setSize(cornerDim, cornerDim)
						.setPos(x, y);

		spriteWidgets[3] =
				parent.createChild(
								-1,
								WidgetType.GRAPHIC
						)
						.setSpriteId(
								COLLECTION_BUTTON_SPRITES_INACTIVE[3]
						)
						.setXPositionMode(
								WidgetPositionMode.ABSOLUTE_RIGHT
						)
						.setSize(cornerDim, cornerDim)
						.setPos(
								x + (w - cornerDim),
								y + h - cornerDim
						);

		spriteWidgets[4] =
				parent.createChild(
								-1,
								WidgetType.GRAPHIC
						)
						.setSpriteId(
								COLLECTION_BUTTON_SPRITES_INACTIVE[4]
						)
						.setXPositionMode(
								WidgetPositionMode.ABSOLUTE_RIGHT
						)
						.setSize(cornerDim, cornerDim)
						.setPos(
								x,
								y + h - cornerDim
						);

		int sideWidth = 9;
		int sideHeight = 4;

		spriteWidgets[5] =
				parent.createChild(
								-1,
								WidgetType.GRAPHIC
						)
						.setSpriteId(
								COLLECTION_BUTTON_SPRITES_INACTIVE[5]
						)
						.setXPositionMode(
								WidgetPositionMode.ABSOLUTE_RIGHT
						)
						.setSize(sideWidth, sideHeight)
						.setPos(
								x + (w - sideWidth),
								y + cornerDim
						);

		spriteWidgets[7] =
				parent.createChild(
								-1,
								WidgetType.GRAPHIC
						)
						.setSpriteId(
								COLLECTION_BUTTON_SPRITES_INACTIVE[7]
						)
						.setXPositionMode(
								WidgetPositionMode.ABSOLUTE_RIGHT
						)
						.setSize(sideWidth, sideHeight)
						.setPos(
								x,
								y + cornerDim
						);

		int topWidth =
				w - (cornerDim * 2);

		int topHeight = 9;

		spriteWidgets[6] =
				parent.createChild(
								-1,
								WidgetType.GRAPHIC
						)
						.setSpriteId(
								COLLECTION_BUTTON_SPRITES_INACTIVE[6]
						)
						.setXPositionMode(
								WidgetPositionMode.ABSOLUTE_RIGHT
						)
						.setSize(topWidth, topHeight)
						.setPos(
								x + cornerDim,
								y
						);

		spriteWidgets[8] =
				parent.createChild(
								-1,
								WidgetType.GRAPHIC
						)
						.setSpriteId(
								COLLECTION_BUTTON_SPRITES_INACTIVE[8]
						)
						.setXPositionMode(
								WidgetPositionMode.ABSOLUTE_RIGHT
						)
						.setSize(topWidth, topHeight)
						.setPos(
								x + cornerDim,
								y + h - topHeight
						);

		for (Widget spriteWidget : spriteWidgets)
		{
			spriteWidget.revalidate();
			collectionSyncWidgets.add(
					spriteWidget
			);
		}

		final Widget text =
				parent.createChild(
								-1,
								WidgetType.TEXT
						)
						.setText(
								COLLECTION_SYNC_LABEL
						)
						.setTextColor(
								COLLECTION_BUTTON_FONT_INACTIVE
						)
						.setFontId(
								FontID.PLAIN_11
						)
						.setTextShadowed(true)
						.setXPositionMode(
								WidgetPositionMode.ABSOLUTE_RIGHT
						)
						.setXTextAlignment(
								WidgetTextAlignment.CENTER
						)
						.setYTextAlignment(
								WidgetTextAlignment.CENTER
						)
						.setPos(x, y)
						.setSize(w, h)
						.setYPositionMode(
								searchButton.getYPositionMode()
						);

		text.revalidate();
		text.setHasListener(true);

		text.setOnMouseOverListener(
				(JavaScriptCallback) ev ->
				{
					for (
							int i = 0;
							i < spriteWidgets.length;
							i++
					)
					{
						spriteWidgets[i].setSpriteId(
								COLLECTION_BUTTON_SPRITES_ACTIVE[i]
						);
					}

					text.setTextColor(
							COLLECTION_BUTTON_FONT_ACTIVE
					);
				}
		);

		text.setOnMouseLeaveListener(
				(JavaScriptCallback) ev ->
				{
					for (
							int i = 0;
							i < spriteWidgets.length;
							i++
					)
					{
						spriteWidgets[i].setSpriteId(
								COLLECTION_BUTTON_SPRITES_INACTIVE[i]
						);
					}

					text.setTextColor(
							COLLECTION_BUTTON_FONT_INACTIVE
					);
				}
		);

		text.setAction(
				0,
				"Show Me Your ClogHole"
		);

		text.setOnOpListener(
				(JavaScriptCallback) ev ->
						startInstantCollectionLogSync()
		);

		collectionSyncWidgets.add(text);

		/*
		 * WikiSync shortens the draggable top bar by the button width so
		 * the custom control does not overlap the draggable title region.
		 * Do the same here.
		 */
		draggableTopbar.setOriginalWidth(
				draggableTopbar.getOriginalWidth() -
						(
								w +
										(
												x -
														COLLECTION_CLOSE_BUTTON_OFFSET
										)
						)
		);

		draggableTopbar.revalidate();
		parent.revalidate();
	}


	private void setCollectionSyncButtonLabel(
			String label
	)
	{
		for (Widget widget : collectionSyncWidgets)
		{
			if (
					widget != null &&
							widget.getType() == WidgetType.TEXT
			)
			{
				widget.setText(label);
				widget.revalidate();
			}
		}
	}


	private void removeCollectionSyncButton()
	{
		for (Widget widget : collectionSyncWidgets)
		{
			if (widget != null)
			{
				widget.setHidden(true);
			}
		}

		collectionSyncWidgets.clear();
	}


	@Subscribe
	public void onWidgetLoaded(
			WidgetLoaded event
	)
	{
		if (event.getGroupId() ==
				InterfaceID.BANKMAIN)
		{
			log.info(
					"Bank opened - sending immediate snapshot"
			);

			sendSnapshot("BANK_OPEN");
		}

		if (event.getGroupId() == 631)
		{
			log.info(
					"Seed Vault opened - sending immediate snapshot"
			);

			sendSnapshot("SEED_VAULT_OPEN");
		}
	}


	private void sendSnapshot(
			String snapshotReason
	)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		String account =
				client.getLocalPlayer().getName();

		ItemContainer inventory =
				client.getItemContainer(
						InventoryID.INV
				);

		ItemContainer bank =
				client.getItemContainer(
						InventoryID.BANK
				);

		ItemContainer equipment =
				client.getItemContainer(
						InventoryID.WORN
				);


		ItemContainer seedVault =
				client.getItemContainer(
						InventoryID.SEED_VAULT
				);


		/*
		 * Refresh live bank cache if the bank is currently available.
		 */
		if (bank != null)
		{
			cachedBankItems =
					bank.getItems().clone();
		}


		/*
		 * Refresh live Seed Vault cache if the Seed Vault is currently
		 * available.
		 */
		if (seedVault != null)
		{
			cachedSeedVaultItems =
					seedVault.getItems().clone();
		}


		/*
		 * BANK JSON
		 */
		String bankJson = "null";

		if (cachedBankItems != null)
		{
			StringJoiner bankItems =
					new StringJoiner(
							",",
							"[",
							"]"
					);

			for (
					int slot = 0;
					slot < cachedBankItems.length;
					slot++
			)
			{
				Item item =
						cachedBankItems[slot];

				if (item == null)
				{
					bankItems.add("null");
				}
				else
				{
					String itemName =
							client
									.getItemDefinition(
											item.getId()
									)
									.getName();

					bankItems.add(
							String.format(
									"{\"slot\":%d,\"name\":\"%s\",\"id\":%d,\"quantity\":%d}",
									slot,
									escapeJson(itemName),
									item.getId(),
									item.getQuantity()
							)
					);
				}
			}

			bankJson =
					bankItems.toString();
		}


		/*
		 * SEED VAULT JSON
		 */
		String seedVaultJson = "null";

		if (cachedSeedVaultItems != null)
		{
			StringJoiner seedVaultItems =
					new StringJoiner(
							",",
							"[",
							"]"
					);

			for (
					int slot = 0;
					slot < cachedSeedVaultItems.length;
					slot++
			)
			{
				Item item =
						cachedSeedVaultItems[slot];

				if (
						item == null ||
								item.getId() <= 0
				)
				{
					continue;
				}

				String itemName =
						client
								.getItemDefinition(
										item.getId()
								)
								.getName();

				seedVaultItems.add(
						String.format(
								"{\"slot\":%d,\"name\":\"%s\",\"id\":%d,\"quantity\":%d}",
								slot,
								escapeJson(itemName),
								item.getId(),
								item.getQuantity()
						)
				);
			}

			seedVaultJson =
					seedVaultItems.toString();
		}


		/*
		 * INVENTORY JSON
		 */
		String inventoryJson = "null";

		if (inventory != null)
		{
			StringJoiner inventorySlots =
					new StringJoiner(
							",",
							"[",
							"]"
					);

			for (
					int slot = 0;
					slot < 28;
					slot++
			)
			{
				Item item =
						slot < inventory.size()
								? inventory.getItem(slot)
								: null;

				if (item == null)
				{
					inventorySlots.add("null");
				}
				else
				{
					String itemName =
							client
									.getItemDefinition(
											item.getId()
									)
									.getName();

					inventorySlots.add(
							String.format(
									"{\"slot\":%d,\"name\":\"%s\",\"id\":%d,\"quantity\":%d}",
									slot,
									escapeJson(itemName),
									item.getId(),
									item.getQuantity()
							)
					);
				}
			}

			inventoryJson =
					inventorySlots.toString();
		}


		/*
		 * EQUIPMENT JSON
		 */
		String equipmentJson = "null";

		if (equipment != null)
		{
			String head =
					getEquipmentItemJson(
							equipment,
							EquipmentInventorySlot.HEAD
					);

			String cape =
					getEquipmentItemJson(
							equipment,
							EquipmentInventorySlot.CAPE
					);

			String amulet =
					getEquipmentItemJson(
							equipment,
							EquipmentInventorySlot.AMULET
					);

			String weapon =
					getEquipmentItemJson(
							equipment,
							EquipmentInventorySlot.WEAPON
					);

			String body =
					getEquipmentItemJson(
							equipment,
							EquipmentInventorySlot.BODY
					);

			String shield =
					getEquipmentItemJson(
							equipment,
							EquipmentInventorySlot.SHIELD
					);

			String legs =
					getEquipmentItemJson(
							equipment,
							EquipmentInventorySlot.LEGS
					);

			String gloves =
					getEquipmentItemJson(
							equipment,
							EquipmentInventorySlot.GLOVES
					);

			String boots =
					getEquipmentItemJson(
							equipment,
							EquipmentInventorySlot.BOOTS
					);

			String ring =
					getEquipmentItemJson(
							equipment,
							EquipmentInventorySlot.RING
					);

			String ammo =
					getEquipmentItemJson(
							equipment,
							EquipmentInventorySlot.AMMO
					);

			equipmentJson =
					String.format(
							"{\"head\":%s,\"cape\":%s,\"amulet\":%s,\"weapon\":%s,\"body\":%s,\"shield\":%s,\"legs\":%s,\"gloves\":%s,\"boots\":%s,\"ring\":%s,\"ammo\":%s}",
							head,
							cape,
							amulet,
							weapon,
							body,
							shield,
							legs,
							gloves,
							boots,
							ring,
							ammo
					);
		}


		String clientTime =
				Instant.now().toString();


		/*
		 * COMBAT ACHIEVEMENT TIER COUNTS
		 */
		int caEasy =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_EASY
				);

		int caMedium =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_MEDIUM
				);

		int caHard =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_HARD
				);

		int caElite =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_ELITE
				);

		int caMaster =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_MASTER
				);

		int caGrandmaster =
				client.getVarbitValue(
						Varbits.COMBAT_TASK_GRANDMASTER
				);


		/*
		 * ACHIEVEMENT DIARY TIER COMPLETION
		 *
		 * Karamja intentionally uses == 2.
		 */
		String diaryJson =
				String.format(
						"{\"ardougne\":{\"easy\":%b,\"medium\":%b,\"hard\":%b,\"elite\":%b}," +
								"\"desert\":{\"easy\":%b,\"medium\":%b,\"hard\":%b,\"elite\":%b}," +
								"\"falador\":{\"easy\":%b,\"medium\":%b,\"hard\":%b,\"elite\":%b}," +
								"\"fremennik\":{\"easy\":%b,\"medium\":%b,\"hard\":%b,\"elite\":%b}," +
								"\"kandarin\":{\"easy\":%b,\"medium\":%b,\"hard\":%b,\"elite\":%b}," +
								"\"karamja\":{\"easy\":%b,\"medium\":%b,\"hard\":%b,\"elite\":%b}," +
								"\"kourend\":{\"easy\":%b,\"medium\":%b,\"hard\":%b,\"elite\":%b}," +
								"\"lumbridge\":{\"easy\":%b,\"medium\":%b,\"hard\":%b,\"elite\":%b}," +
								"\"morytania\":{\"easy\":%b,\"medium\":%b,\"hard\":%b,\"elite\":%b}," +
								"\"varrock\":{\"easy\":%b,\"medium\":%b,\"hard\":%b,\"elite\":%b}," +
								"\"western\":{\"easy\":%b,\"medium\":%b,\"hard\":%b,\"elite\":%b}," +
								"\"wilderness\":{\"easy\":%b,\"medium\":%b,\"hard\":%b,\"elite\":%b}}",

						client.getVarbitValue(
								Varbits.DIARY_ARDOUGNE_EASY
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_ARDOUGNE_MEDIUM
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_ARDOUGNE_HARD
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_ARDOUGNE_ELITE
						) == 1,


						client.getVarbitValue(
								Varbits.DIARY_DESERT_EASY
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_DESERT_MEDIUM
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_DESERT_HARD
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_DESERT_ELITE
						) == 1,


						client.getVarbitValue(
								Varbits.DIARY_FALADOR_EASY
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_FALADOR_MEDIUM
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_FALADOR_HARD
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_FALADOR_ELITE
						) == 1,


						client.getVarbitValue(
								Varbits.DIARY_FREMENNIK_EASY
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_FREMENNIK_MEDIUM
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_FREMENNIK_HARD
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_FREMENNIK_ELITE
						) == 1,


						client.getVarbitValue(
								Varbits.DIARY_KANDARIN_EASY
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_KANDARIN_MEDIUM
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_KANDARIN_HARD
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_KANDARIN_ELITE
						) == 1,


						client.getVarbitValue(
								Varbits.DIARY_KARAMJA_EASY
						) == 2,

						client.getVarbitValue(
								Varbits.DIARY_KARAMJA_MEDIUM
						) == 2,

						client.getVarbitValue(
								Varbits.DIARY_KARAMJA_HARD
						) == 2,

						client.getVarbitValue(
								Varbits.DIARY_KARAMJA_ELITE
						) == 2,


						client.getVarbitValue(
								Varbits.DIARY_KOUREND_EASY
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_KOUREND_MEDIUM
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_KOUREND_HARD
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_KOUREND_ELITE
						) == 1,


						client.getVarbitValue(
								Varbits.DIARY_LUMBRIDGE_EASY
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_LUMBRIDGE_MEDIUM
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_LUMBRIDGE_HARD
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_LUMBRIDGE_ELITE
						) == 1,


						client.getVarbitValue(
								Varbits.DIARY_MORYTANIA_EASY
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_MORYTANIA_MEDIUM
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_MORYTANIA_HARD
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_MORYTANIA_ELITE
						) == 1,


						client.getVarbitValue(
								Varbits.DIARY_VARROCK_EASY
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_VARROCK_MEDIUM
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_VARROCK_HARD
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_VARROCK_ELITE
						) == 1,


						client.getVarbitValue(
								Varbits.DIARY_WESTERN_EASY
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_WESTERN_MEDIUM
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_WESTERN_HARD
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_WESTERN_ELITE
						) == 1,


						client.getVarbitValue(
								Varbits.DIARY_WILDERNESS_EASY
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_WILDERNESS_MEDIUM
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_WILDERNESS_HARD
						) == 1,

						client.getVarbitValue(
								Varbits.DIARY_WILDERNESS_ELITE
						) == 1
				);


		/*
		 * COMBAT ACHIEVEMENT COMPLETION IDS
		 */
		StringJoiner caCompletedIds =
				new StringJoiner(
						",",
						"[",
						"]"
				);

		for (
				int taskId = 0;
				taskId < 640;
				taskId++
		)
		{
			if (
					isCombatAchievementComplete(
							taskId
					)
			)
			{
				caCompletedIds.add(
						String.valueOf(
								taskId
						)
				);
			}
		}


		/*
		 * SLAYER
		 */
		int slayerRemaining =
				client.getVarpValue(
						VarPlayerID.SLAYER_COUNT
				);

		int slayerTaskId =
				client.getVarpValue(
						VarPlayerID.SLAYER_TARGET
				);

		String slayerTask = "";

		try
		{
			var taskRows =
					client.getDBRowsByValue(
							DBTableID.SlayerTask.ID,
							DBTableID.SlayerTask.COL_ID,
							0,
							slayerTaskId
					);

			if (
					taskRows != null &&
							!taskRows.isEmpty()
			)
			{
				int taskDBRow =
						taskRows.get(0);

				Object[] taskFields =
						client.getDBTableField(
								taskDBRow,
								DBTableID.SlayerTask.COL_NAME_UPPERCASE,
								0
						);

				if (
						taskFields != null &&
								taskFields.length > 0 &&
								taskFields[0] != null
				)
				{
					slayerTask =
							String.valueOf(
									taskFields[0]
							);
				}
			}
		}
		catch (Exception e)
		{
			log.debug(
					"Unable to resolve Slayer task name",
					e
			);
		}


		/*
		 * SKILLS
		 */
		StringJoiner skillsJson =
				new StringJoiner(
						",",
						"{",
						"}"
				);

		for (
				Skill skill :
				Skill.values()
		)
		{
			if (skill == Skill.OVERALL)
			{
				continue;
			}

			int level =
					client.getRealSkillLevel(
							skill
					);

			int xp =
					client.getSkillExperience(
							skill
					);

			skillsJson.add(
					String.format(
							"\"%s\":{\"level\":%d,\"xp\":%d}",
							escapeJson(
									skill.getName()
							),
							level,
							xp
					)
			);
		}


		/*
		 * QUESTS
		 */
		StringJoiner questsJson =
				new StringJoiner(
						",",
						"{",
						"}"
				);

		for (
				Quest quest :
				Quest.values()
		)
		{
			questsJson.add(
					String.format(
							"\"%s\":\"%s\"",
							escapeJson(
									quest.getName()
							),
							escapeJson(
									quest
											.getState(client)
											.name()
							)
					)
			);
		}


		/*
		 * COLLECTION LOG JSON
		 *
		 * Pages are captured only when viewed in the Collection Log.
		 * This reads only the direct children exposed by the log interface.
		 */
		StringJoiner collectionPagesJson =
				new StringJoiner(
						",",
						"{",
						"}"
				);

		for (
				Map.Entry<String, String> entry :
				cachedCollectionLogPages.entrySet()
		)
		{
			collectionPagesJson.add(
					String.format(
							"\"%s\":%s",
							escapeJson(
									entry.getKey()
							),
							entry.getValue()
					)
			);
		}

		String collectionLogJson =
				String.format(
						"{\"pages\":%s}",
						collectionPagesJson.toString()
				);


		/*
		 * INSTANT COLLECTION LOG TRANSMIT JSON
		 *
		 * This is intentionally compact: only obtained item IDs and
		 * quantities are transmitted by the game. The Worker uses the
		 * existing full Collection Log page/item definition as a template
		 * and marks every absent ID as unobtained.
		 */
		String instantCollectionLogJson = "null";

		if (collectionInstantCapturedAt != null)
		{
			StringJoiner instantItemsJson =
					new StringJoiner(
							",",
							"[",
							"]"
					);

			for (
					Map.Entry<Integer, Integer> entry :
					instantCollectionLogItems.entrySet()
			)
			{
				instantItemsJson.add(
						String.format(
								"{\"id\":%d,\"quantity\":%d}",
								entry.getKey(),
								entry.getValue()
						)
				);
			}

			instantCollectionLogJson =
					String.format(
							"{\"capturedAt\":\"%s\",\"items\":%s}",
							escapeJson(
									collectionInstantCapturedAt
							),
							instantItemsJson.toString()
					);
		}


		/*
		 * FINAL SNAPSHOT JSON
		 */
		String json =
				String.format(
						"{\"account\":\"%s\"," +
								"\"clientTime\":\"%s\"," +
								"\"snapshotReason\":\"%s\"," +
								"\"slayerTask\":\"%s\"," +
								"\"slayerRemaining\":%d," +
								"\"combatAchievements\":{" +
								"\"easy\":%d," +
								"\"medium\":%d," +
								"\"hard\":%d," +
								"\"elite\":%d," +
								"\"master\":%d," +
								"\"grandmaster\":%d," +
								"\"completedTaskIds\":%s}," +
								"\"achievementDiaries\":%s," +
								"\"collectionLog\":%s," +
								"\"collectionLogInstant\":%s," +
								"\"skills\":%s," +
								"\"quests\":%s," +
								"\"inventory\":%s," +
								"\"bank\":%s," +
								"\"seedVault\":%s," +
								"\"equipment\":%s}",

						escapeJson(account),
						escapeJson(clientTime),
						escapeJson(snapshotReason),
						escapeJson(slayerTask),
						slayerRemaining,

						caEasy,
						caMedium,
						caHard,
						caElite,
						caMaster,
						caGrandmaster,
						caCompletedIds.toString(),

						diaryJson,
						collectionLogJson,
						instantCollectionLogJson,

						skillsJson.toString(),
						questsJson.toString(),

						inventoryJson,
						bankJson,
						seedVaultJson,
						equipmentJson
				);


		log.info(
				"Sending JSON: {}",
				json
		);


		/*
		 * SEND TO CLOUDFLARE
		 */
		try
		{
			HttpRequest request =
					HttpRequest.newBuilder()
							.uri(
									URI.create(
											config.backendUrl() +
													"/update"
									)
							)
							.header(
									"Authorization",
									"Bearer " +
											config.writeToken()
							)
							.header(
									"Content-Type",
									"application/json"
							)
							.POST(
									HttpRequest
											.BodyPublishers
											.ofString(json)
							)
							.build();


			httpClient
					.sendAsync(
							request,
							HttpResponse
									.BodyHandlers
									.ofString()
					)
					.thenAccept(
							response ->
									log.info(
											"Upload response: {} {}",
											response.statusCode(),
											response.body()
									)
					)
					.exceptionally(
							error ->
							{
								log.error(
										"Upload failed",
										error
								);

								return null;
							}
					);
		}
		catch (Exception e)
		{
			log.error(
					"Failed to send snapshot",
					e
			);
		}
	}


	private boolean isCombatAchievementComplete(
			int taskId
	)
	{
		int block =
				taskId / 32;

		int bit =
				taskId % 32;

		int packed;


		switch (block)
		{
			case 0:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_0
						);
				break;

			case 1:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_1
						);
				break;

			case 2:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_2
						);
				break;

			case 3:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_3
						);
				break;

			case 4:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_4
						);
				break;

			case 5:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_5
						);
				break;

			case 6:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_6
						);
				break;

			case 7:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_7
						);
				break;

			case 8:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_8
						);
				break;

			case 9:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_9
						);
				break;

			case 10:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_10
						);
				break;

			case 11:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_11
						);
				break;

			case 12:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_12
						);
				break;

			case 13:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_13
						);
				break;

			case 14:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_14
						);
				break;

			case 15:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_15
						);
				break;

			case 16:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_16
						);
				break;

			case 17:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_17
						);
				break;

			case 18:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_18
						);
				break;

			case 19:
				packed =
						client.getVarpValue(
								VarPlayerID.CA_TASK_COMPLETED_19
						);
				break;

			default:
				return false;
		}


		return (
				packed &
						(1 << bit)
		) != 0;
	}


	private String getEquipmentItemJson(
			ItemContainer equipment,
			EquipmentInventorySlot slot
	)
	{
		Item item =
				equipment.getItem(
						slot.getSlotIdx()
				);


		if (item == null)
		{
			return "null";
		}


		String itemName =
				client
						.getItemDefinition(
								item.getId()
						)
						.getName();


		return String.format(
				"{\"name\":\"%s\",\"id\":%d,\"quantity\":%d}",
				escapeJson(itemName),
				item.getId(),
				item.getQuantity()
		);
	}


	private String stripTags(
			String text
	)
	{
		if (text == null)
		{
			return "";
		}

		return text.replaceAll(
				"<[^>]*>",
				""
		);
	}


	private String escapeJson(
			String text
	)
	{
		if (text == null)
		{
			return "";
		}


		return text
				.replace(
						"\\",
						"\\\\"
				)
				.replace(
						"\"",
						"\\\""
				)
				.replace(
						"\n",
						"\\n"
				)
				.replace(
						"\r",
						"\\r"
				)
				.replace(
						"\t",
						"\\t"
				);
	}


	@Provides
	OracleConfig provideConfig(
			ConfigManager configManager
	)
	{
		return configManager.getConfig(
				OracleConfig.class
		);
	}
}