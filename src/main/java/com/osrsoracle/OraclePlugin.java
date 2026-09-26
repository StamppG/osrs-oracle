package com.osrsoracle;

import com.google.gson.Gson;
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
import net.runelite.api.ParamID;
import net.runelite.api.Varbits;
import net.runelite.api.VarClientStr;
import net.runelite.api.MenuAction;
import net.runelite.api.SpriteID;
import net.runelite.api.FontID;

import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;

import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.ItemID;
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
import net.runelite.client.plugins.cluescrolls.clues.emote.STASHUnit;



import java.time.Instant;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

    @Inject
    private SnapshotTransport snapshotTransport;

    @Inject
    private Gson gson;

    @Inject
    private SnapshotItemSerializer snapshotItemSerializer;

    @Inject
    private SnapshotLiveStateCollector snapshotLiveStateCollector;

    @Inject
    private SnapshotCollectionLogSerializer snapshotCollectionLogSerializer;

    private static final long MIN_UPLOAD_INTERVAL_MS = 1000L;
    private static final int ENTRY_SNAPSHOT_SETTLE_TICKS = 3;
    private static final int UI_SNAPSHOT_SETTLE_TICKS = 1;
    private static final int STASH_SYNC_BATCH_SIZE = 8;

    private static final Set<Integer> MOTHERLODE_MAP_REGIONS = Set.of(
            14679,
            14680,
            14681,
            14935,
            14936,
            14937,
            15191,
            15192,
            15193
    );


	private long lastUploadTime = 0;
	private final DeferredSnapshotScheduler snapshotScheduler = new DeferredSnapshotScheduler();
	private final EntryObservationSettleGuard entryObservationSettleGuard = new EntryObservationSettleGuard();
	private String clientSessionId = null;
	private long snapshotSequence = 0;
	private final ObservedItemContainerState cachedBankState =
		new ObservedItemContainerState(
			"bank",
			ObservedItemContainerState.Scope.ACCOUNT,
			false
		);

	private final ObservedItemContainerState cachedSeedVaultState =
		new ObservedItemContainerState(
			"seedVault",
			ObservedItemContainerState.Scope.ACCOUNT,
			false
		);

	private final ObservedItemContainerState cachedGimStorageState =
		new ObservedItemContainerState(
			"gimStorage",
			ObservedItemContainerState.Scope.GROUP,
			true
		);

	private final ObservedItemContainerState cachedCoxPrivateStorageState =
	        new ObservedItemContainerState(
	                "coxPrivateStorage",
	                ObservedItemContainerState.Scope.ACCOUNT,
	                false
	        );

	private final ObservedItemContainerState cachedCoxSharedStorageState =
	        new ObservedItemContainerState(
	                "coxSharedStorage",
	                ObservedItemContainerState.Scope.ACTIVITY,
	                true
	        );
  private final ObservedItemContainerState cachedGravestoneStorageState =
          new ObservedItemContainerState(
                  "gravestoneStorage",
                  ObservedItemContainerState.Scope.ACCOUNT,
                  false
          );
  private final ObservedItemContainerState cachedDeathsOfficeStorageState =
          new ObservedItemContainerState(
                  "deathsOfficeStorage",
                  ObservedItemContainerState.Scope.ACCOUNT,
                  false
          );
  private final ObservedPotionStorageState cachedPotionStorageState =
          new ObservedPotionStorageState();
  private final ObservedMotherlodeSackState cachedMotherlodeSackState =
          new ObservedMotherlodeSackState();
  private final ObservedPlankSackState cachedPlankSackState =
          new ObservedPlankSackState();

  private final ObservedContextualStorageState cachedHerbSackState =
          new ObservedContextualStorageState();
  private final ObservedContextualStorageState cachedGemBagState =
          new ObservedContextualStorageState();
  private final ObservedContextualStorageState cachedGemSatchelState =
          new ObservedContextualStorageState();
  private final ObservedContextualStorageState cachedCoalBagState =
          new ObservedContextualStorageState();
  private final ObservedContextualStorageState cachedFishBarrelState =
          new ObservedContextualStorageState();
  private final ObservedContextualStorageState cachedLogBasketState =
          new ObservedContextualStorageState();

  private final ContextualStorageChatParser contextualStorageChatParser =
          new ContextualStorageChatParser();




	private final ObservedItemContainerState cachedLootingBagState =
	        new ObservedItemContainerState(
	                "lootingBag",
	                ObservedItemContainerState.Scope.ACCOUNT,
	                false
	        );

	private final ObservedItemContainerState cachedSeedBoxState =
	        new ObservedItemContainerState(
	                "seedBox",
	                ObservedItemContainerState.Scope.ACCOUNT,
	                false
	        );

	private final ObservedItemContainerState cachedTackleBoxState =
	        new ObservedItemContainerState(
	                "tackleBox",
	                ObservedItemContainerState.Scope.ACCOUNT,
	                false
	        );

	private final ObservedItemContainerState cachedForestryKitState =
	        new ObservedItemContainerState(
	                "forestryKit",
	                ObservedItemContainerState.Scope.ACCOUNT,
	                false
	        );

	private final ObservedItemContainerState cachedHuntsmansKitState =
	        new ObservedItemContainerState(
	                "huntsmansKit",
	                ObservedItemContainerState.Scope.ACCOUNT,
	                false
	        );

	private final ObservedItemContainerState cachedBarbarianKnapsackState =
	        new ObservedItemContainerState(
	                "barbarianKnapsack",
	                ObservedItemContainerState.Scope.ACCOUNT,
	                false
	        );

	private final ObservedStashState cachedStashState =
	        new ObservedStashState();

	private final List<ObservedStashState.Entry> pendingStashSyncEntries =
	        new ArrayList<>();

	private boolean manualStashSyncActive = false;
	private int manualStashSyncIndex = 0;
	private String manualStashSyncAccount = null;

	private final ObservedQuiverAmmoState cachedDizanasQuiverAmmoState =
	        new ObservedQuiverAmmoState();


	private String entryIntentReason = null;
	private String pendingEntrySnapshotReason = null;
	private boolean lootingBagWidgetWasVisible = false;


	private boolean collectionNotificationStarted = false;

	private long lastCollectionCaptureTime = 0;
	private String cachedCollectionLogCapturedAt = null;
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
		clientSessionId = SnapshotEvidence.newSessionId();
		snapshotSequence = 0;
		snapshotScheduler.clear();
		entryObservationSettleGuard.reset();
		lootingBagWidgetWasVisible = false;
		cachedBankState.reset();
		cachedSeedVaultState.reset();
		cachedGimStorageState.reset();
		cachedCoxPrivateStorageState.reset();
		cachedCoxSharedStorageState.reset();
		cachedGravestoneStorageState.reset();
		cachedDeathsOfficeStorageState.reset();
		cachedPotionStorageState.reset();
		cachedMotherlodeSackState.reset();
		cachedPlankSackState.reset();
        cachedHerbSackState.reset();
        cachedGemBagState.reset();
        cachedGemSatchelState.reset();
        cachedCoalBagState.reset();
        cachedFishBarrelState.reset();
        cachedLogBasketState.reset();
        contextualStorageChatParser.reset();
		cachedLootingBagState.reset();
		cachedSeedBoxState.reset();
		cachedTackleBoxState.reset();
		cachedForestryKitState.reset();
		cachedHuntsmansKitState.reset();
		cachedBarbarianKnapsackState.reset();
		cachedDizanasQuiverAmmoState.reset();
		cachedStashState.reset();
		pendingStashSyncEntries.clear();
		manualStashSyncActive = false;
		manualStashSyncIndex = 0;
		manualStashSyncAccount = null;
		lastCollectionCaptureTime = 0;
		cachedCollectionLogCapturedAt = null;
		cachedCollectionLogPages.clear();
		collectionInstantCapturedAt = null;
		instantCollectionLogItems.clear();
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
		 * Preserve the reason we are entering the game across LOADING.
		 *
		 * A real login normally does:
		 * LOGIN_SCREEN -> LOGGING_IN -> LOADING -> LOGGED_IN
		 *
		 * A world hop normally does:
		 * HOPPING -> LOADING -> LOGGED_IN
		 *
		 * Ordinary teleports and region changes can also do:
		 * LOGGED_IN -> LOADING -> LOGGED_IN
		 *
		 * so LOADING by itself must never arm an entry snapshot.
		 */
		if (newState == GameState.HOPPING)
		{
			entryIntentReason = "WORLD_HOP";
		}
		else if (
				newState == GameState.LOGIN_SCREEN ||
						newState == GameState.LOGGING_IN
		)
		{
			/*
			 * Do not overwrite a world-hop intent if RuneLite happens to pass
			 * through one of the login states during the hop sequence.
			 */
			if (!"WORLD_HOP".equals(entryIntentReason))
			{
				entryIntentReason = "LOGIN";
			}
		}
		else if (
				newState == GameState.LOGGED_IN &&
						entryIntentReason != null
		)
		{
			pendingEntrySnapshotReason = entryIntentReason;
			entryObservationSettleGuard.begin(
			        client.getTickCount(),
			        ENTRY_SNAPSHOT_SETTLE_TICKS
			);
			entryIntentReason = null;
		}

	}
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        if (!"Check".equalsIgnoreCase(
                stripTags(event.getMenuOption()).trim()
        ))
        {
            return;
        }

        contextualStorageChatParser.armCheck(
                stripTags(event.getMenuTarget()).trim()
        );
    }




	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		advanceManualStashSync();
		observeLootingBagWidgetIfOpened();
		ContextualStorageChatParser.Observation contextualTickObservation =
				contextualStorageChatParser.finishTick();

		if (contextualTickObservation != null)
		{
			observeContextualStorageObservation(contextualTickObservation);
		}

		/*
		 * BANK CACHE
		 */
		ItemContainer bank =
				client.getItemContainer(
						InventoryID.BANK
				);

		observeOwnedItemContainer(
				cachedBankState,
				bank,
				Instant.now().toString()
		);

		/*
		 * SEED VAULT CACHE
		 */
		ItemContainer seedVault =
				client.getItemContainer(
						InventoryID.SEED_VAULT
				);

		observeOwnedItemContainer(
				cachedSeedVaultState,
				seedVault,
				Instant.now().toString()
		);

		/*
		 * DEFERRED / RATE-LIMITED PENDING PUSH
		 *
		 * Events coalesce into one pending snapshot. Its settle deadline and
		 * the one-second upload interval must both be satisfied before sending.
		 * The payload is built at send time so state is fresh.
		 */
		schedulePendingEntrySnapshot();
		entryObservationSettleGuard.clearIfSettled(client.getTickCount());
		flushPendingSnapshotIfReady();

		/*
		 * LOGIN / WORLD-HOP PUSH
		 *
		 * The state-change handler records the entry reason at LOGGED_IN and
		 * arms the settle guard. The first valid game tick schedules that
		 * reason; sending waits until the entry settle deadline has passed.
		 */
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
			requestSnapshot("CLOG_MANUAL");
		}


		/*
		 * NORMAL 15-MINUTE HEARTBEAT
		 */
		long now =
				System.currentTimeMillis();

		if (
		        !snapshotScheduler.hasPending() &&
		                now - lastUploadTime >=
		                        15 * 60 * 1000
		)
		{
			requestSnapshot("HEARTBEAT");
		}
	}

	static boolean isIndividualDiaryTaskMessage(String lowerMessage)
	{
	        return lowerMessage.startsWith(
	                        "well done! you have completed "
	        ) &&
	                        lowerMessage.contains(
	                                        " task in the "
	                        ) &&
	                        lowerMessage.endsWith(
	                                        " area. your achievement diary has been updated."
	                        );
	}

	/*
	 * LEVEL-UP / QUEST / COLLECTION LOG PUSHES
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

		if (
				event.getType() != ChatMessageType.GAMEMESSAGE &&
						event.getType() != ChatMessageType.MESBOX
		)
		{
			return;
		}

		String message =
				stripTags(
						event.getMessage()
				).trim();

		String lowerMessage =
				message.toLowerCase();

		ContextualStorageChatParser.Observation contextualChatObservation =
				contextualStorageChatParser.accept(
						event.getType(),
						message
				);

		if (contextualChatObservation != null)
		{
			observeContextualStorageObservation(
					contextualChatObservation
			);
			return;
		}



		/*
		 * ACHIEVEMENT DIARY COMPLETION
		 *
		 * Confirmed live as a MESBOX message:
		 * "Congratulations! You have completed all of the easy tasks in the
		 * Lumbridge & Draynor area. Speak to Hatius Cosaintus outside of
		 * Lumbridge Castle to claim your reward."
		 *
		 * Only the full-tier MESBOX completion message should trigger this.
		 * Individual diary tasks arrive as GAMEMESSAGE and do not match here.
		 */
		if (
				event.getType() == ChatMessageType.MESBOX &&
						lowerMessage.contains(
								"you have completed all of the "
						) &&
						lowerMessage.contains(
								" tasks in the "
						) &&
						lowerMessage.contains(
								" area."
						)
		)
		{
			log.info(
					"PUSH TRIGGER: Achievement Diary completed"
			);

			requestSnapshot(
					"DIARY_COMPLETE"
			);

			return;
		}

		/*
		 * All remaining chat triggers are intentionally limited to the same
		 * GAMEMESSAGE path they used before the diary MESBOX fix.
		 */
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (isIndividualDiaryTaskMessage(lowerMessage))
		{
		        log.info(
		                        "PUSH TRIGGER: Achievement Diary task completed"
		        );

		        requestSnapshot(
		                        "DIARY_TASK"
		        );

		        return;
		}

		/*
		 * LEVEL UP
		 */
		java.util.regex.Matcher levelMatcher =
				java.util.regex.Pattern.compile(
						"Congratulations, you've just advanced your (.+?) level[.] You are now level ([0-9]+)[.]"
				).matcher(message);

		if (levelMatcher.matches())
		{
			String skillName =
					levelMatcher.group(1);

			log.info(
					"PUSH TRIGGER: level up {}",
					skillName
			);

			requestSnapshot(
					"LEVEL_UP:" +
							skillName.toUpperCase()
			);

			return;
		}

		/*
		 * QUEST COMPLETION
		 */
		if (
				message.contains(
						"Congratulations! Quest complete!"
				) ||
						lowerMessage.contains(
								"you've completed a quest"
						)
		)
		{
			log.info(
					"PUSH TRIGGER: quest completed"
			);

			requestSnapshot(
					"QUEST_COMPLETED"
			);

			return;
		}

		/*
		 * COMBAT ACHIEVEMENT COMPLETION
		 *
		 * Trigger from the player's local game message rather than tier
		 * varbit counters. The full Combat Achievement state is already
		 * included in the snapshot, so Oracle only needs the completion
		 * message as the event signal.
		 */
		if (
				message.startsWith(
						"CA_ID:"
				) &&
						lowerMessage.contains(
								"congratulations, you've completed"
						) &&
						lowerMessage.contains(
								"combat task"
						)
		)
		{
			log.info(
					"PUSH TRIGGER: Combat Achievement completed"
			);

			requestSnapshot(
					"COMBAT_ACHIEVEMENT"
			);

			return;
		}

		/*
		 * COLLECTION LOG ITEM GAIN
		 */
		final String collectionPrefix =
				"New item added to your collection log:";

		if (
				message.startsWith(
						collectionPrefix
				)
		)
		{
			String itemName =
					message.substring(
							collectionPrefix.length()
					).trim();

			log.info(
					"PUSH TRIGGER: collection log item gained '{}'",
					itemName
			);

			requestSnapshot(
					itemName.isEmpty()
							? "COLLECTION_LOG_ITEM"
							: "COLLECTION_LOG_ITEM:" +
							itemName
			);
		}
	}


	@Subscribe
	public void onScriptPreFired(
			ScriptPreFired event
	)
	{
		/*
		 * COLLECTION LOG POPUP FALLBACK
		 *
		 * RuneLite's Screenshot plugin watches the generic notification
		 * start/delay scripts and reads the notification top/bottom strings.
		 * We mirror only the Collection Log semantic signal here.
		 *
		 * The fallback is intentionally enabled only for popup-only mode
		 * (COLLECTION_LOG_NOTIFICATION == 2). If chat is also enabled, the
		 * already-proven GAMEMESSAGE path owns the event, preventing duplicate
		 * Oracle pushes when both notifications are enabled.
		 */
		if (event.getScriptId() == ScriptID.NOTIFICATION_START)
		{
			collectionNotificationStarted = true;
			return;
		}

		if (event.getScriptId() == ScriptID.NOTIFICATION_DELAY)
		{
			if (!collectionNotificationStarted)
			{
				return;
			}

			collectionNotificationStarted = false;

			if (client.getVarbitValue(
					Varbits.COLLECTION_LOG_NOTIFICATION
			) != 2)
			{
				return;
			}

			String topText =
					stripTags(
							client.getVarcStrValue(
									VarClientStr.NOTIFICATION_TOP_TEXT
							)
					).trim();

			String bottomText =
					stripTags(
							client.getVarcStrValue(
									VarClientStr.NOTIFICATION_BOTTOM_TEXT
							)
					).trim();

			if (!"Collection log".equalsIgnoreCase(topText))
			{
				return;
			}

			final String popupPrefix = "New item:";

			if (!bottomText.regionMatches(
					true,
					0,
					popupPrefix,
					0,
					popupPrefix.length()
			))
			{
				return;
			}

			String itemName =
					bottomText.substring(
							popupPrefix.length()
					).trim();

			log.info(
					"PUSH TRIGGER: collection log popup item gained '{}'",
					itemName
			);

			requestSnapshot(
					itemName.isEmpty()
							? "COLLECTION_LOG_ITEM"
							: "COLLECTION_LOG_ITEM:" +
							itemName
			);

			return;
		}

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

                    return;
            }
	}


	@Subscribe
	public void onScriptPostFired(
			ScriptPostFired event
	)
	{
          int scriptId =
                          event.getScriptId();

          if (
                          scriptId == ScriptID.POTIONSTORE_BUILD ||
                                          scriptId == ScriptID.POTIONSTORE_DOSE_CHANGE
          )
          {
                  observePotionStorage();
          }

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
		cachedCollectionLogCapturedAt = Instant.now().toString();

		log.info(
				"CLOG CAPTURE: page='{}' obtained={}/{} cachedPages={}",
				pageName,
				obtainedCount,
				totalCount,
				cachedCollectionLogPages.size()
		);

	}



  private void observePotionStorage()
  {
          Widget content =
                          client.getWidget(
                                          InterfaceID.Bankmain.POTIONSTORE_ITEMS
                          );

          if (content == null)
          {
                  return;
          }

          Widget[] children =
                          content.getDynamicChildren();

          if (children == null)
          {
                  return;
          }

          List<ObservedPotionStorageState.Entry> entries =
                          new ArrayList<>();

          for (
                          int i = 0;
                          i + 4 < children.length;
                          i += 5
          )
          {
                  Widget itemWidget =
                                  children[i + 1];

                  Widget amountWidget =
                                  children[i + 3];

                  if (
                                  itemWidget == null ||
                                                  amountWidget == null ||
                                                  itemWidget.getItemId() <= 0
                  )
                  {
                          continue;
                  }

                  String amountText =
                                  amountWidget.getText();

                  if (amountText == null)
                  {
                          return;
                  }

                  amountText =
                                  stripTags(amountText).trim();

                  String amountType;
                  String numericText;

                  if (amountText.startsWith("Doses: "))
                  {
                          amountType =
                                          ObservedPotionStorageState.AMOUNT_DOSES;

                          numericText =
                                          amountText.substring("Doses: ".length());
                  }
                  else if (amountText.startsWith("Quantity: "))
                  {
                          amountType =
                                          ObservedPotionStorageState.AMOUNT_QUANTITY;

                          numericText =
                                          amountText.substring("Quantity: ".length());
                  }
                  else
                  {
                          return;
                  }

                  int amount;

                  try
                  {
                          amount =
                                          Integer.parseInt(
                                                          numericText.replace(",", "").trim()
                                          );
                  }
                  catch (NumberFormatException ex)
                  {
                          return;
                  }

                  if (amount < 0)
                  {
                          return;
                  }

                  entries.add(
                                  new ObservedPotionStorageState.Entry(
                                                  itemWidget.getItemId(),
                                                  amount,
                                                  amountType
                                  )
                  );
          }

          boolean vialsAlreadyObserved =
                          false;

          for (ObservedPotionStorageState.Entry entry : entries)
          {
                  if (entry.getItemId() == ItemID.VIAL_EMPTY)
                  {
                          vialsAlreadyObserved = true;
                          break;
                  }
          }

          if (!vialsAlreadyObserved)
          {
                  int vialQuantity =
                                  client.getVarpValue(
                                                  VarPlayerID.POTIONSTORE_VIALS
                                  );

                  if (vialQuantity < 0)
                  {
                          return;
                  }

                  if (vialQuantity > 0)
                  {
                          entries.add(
                                          new ObservedPotionStorageState.Entry(
                                                          ItemID.VIAL_EMPTY,
                                                          vialQuantity,
                                                          ObservedPotionStorageState.AMOUNT_QUANTITY
                                          )
                          );
                  }
          }
          boolean accepted =
                          cachedPotionStorageState.observe(
                                          entries.toArray(
                                                          new ObservedPotionStorageState.Entry[0]
                                          ),
                                          Instant.now().toString()
                          );

          if (accepted)
          {
                  requestSnapshot(
                          "POTION_STORAGE",
                          UI_SNAPSHOT_SETTLE_TICKS
                  );
          }
  }

	boolean startManualStashSync()
	{
		if (
			manualStashSyncActive ||
			client.getLocalPlayer() == null
		)
		{
			return false;
		}

		String account =
			client.getLocalPlayer().getName();

		if (
			account == null ||
			account.isBlank()
		)
		{
			return false;
		}

		pendingStashSyncEntries.clear();
		manualStashSyncIndex = 0;
		manualStashSyncAccount = account;
		manualStashSyncActive = true;

		log.info(
			"STASH MANUAL START: account={} totalUnits={}",
			manualStashSyncAccount,
			STASHUnit.values().length
		);

		return true;
	}


	boolean isManualStashSyncActive()
	{
		return manualStashSyncActive;
	}


	int getManualStashSyncProgress()
	{
		return manualStashSyncIndex;
	}


	int getManualStashSyncTotal()
	{
		return STASHUnit.values().length;
	}


	String getStashLastSyncedAt()
	{
		return cachedStashState.getObservedAt();
	}


	private void advanceManualStashSync()
	{
		if (!manualStashSyncActive)
		{
			return;
		}

		if (client.getLocalPlayer() == null)
		{
			abortManualStashSync(
				"local player unavailable"
			);
			return;
		}

		String currentAccount =
			client.getLocalPlayer().getName();

		if (
			currentAccount == null ||
			!currentAccount.equals(manualStashSyncAccount)
		)
		{
			abortManualStashSync(
				"character changed during STASH sync"
			);
			return;
		}

		STASHUnit[] units =
			STASHUnit.values();

		int processedThisTick = 0;

		while (
			manualStashSyncIndex < units.length &&
			processedThisTick < STASH_SYNC_BATCH_SIZE
		)
		{
			STASHUnit unit =
				units[manualStashSyncIndex];

			try
			{
				client.runScript(
					ScriptID.WATSON_STASH_UNIT_CHECK,
					unit.getObjectId(),
					0,
					0,
					0
				);
			}
			catch (RuntimeException ex)
			{
				log.warn(
					"STASH MANUAL script failure: objectId={}",
					unit.getObjectId(),
					ex
				);

				abortManualStashSync(
					"RuneLite STASH check script failed"
				);

				return;
			}

			int[] intStack =
				client.getIntStack();

			int intStackSize =
				client.getIntStackSize();

			if (
				intStack == null ||
				intStackSize < 2 ||
				intStack.length < 2
			)
			{
				abortManualStashSync(
					"STASH check returned fewer than two values"
				);
				return;
			}

			int builtRaw =
				intStack[0];

			int filledRaw =
				intStack[1];

			if (
				(builtRaw != 0 && builtRaw != 1) ||
				(filledRaw != 0 && filledRaw != 1)
			)
			{
				abortManualStashSync(
					"STASH check returned non-boolean values"
				);
				return;
			}

			pendingStashSyncEntries.add(
				new ObservedStashState.Entry(
					unit.getObjectId(),
					builtRaw == 1,
					filledRaw == 1
				)
			);

			manualStashSyncIndex++;
			processedThisTick++;
		}

		if (manualStashSyncIndex < units.length)
		{
			return;
		}

		String observedAt =
			Instant.now().toString();

		boolean accepted =
			cachedStashState.observeComplete(
				pendingStashSyncEntries,
				observedAt
			);

		if (!accepted)
		{
			abortManualStashSync(
				"completed STASH observation was rejected"
			);
			return;
		}

		int syncedUnits =
			pendingStashSyncEntries.size();

		String syncedAccount =
			manualStashSyncAccount;

		pendingStashSyncEntries.clear();
		manualStashSyncIndex = 0;
		manualStashSyncAccount = null;
		manualStashSyncActive = false;

		log.info(
			"STASH MANUAL COMPLETE: account={} units={} observedAt={}",
			syncedAccount,
			syncedUnits,
			observedAt
		);

		requestSnapshot(
			"STASH_MANUAL"
		);
	}


	private void abortManualStashSync(
		String reason
	)
	{
		log.warn(
			"STASH MANUAL ABORTED: account={} progress={}/{} reason={}",
			manualStashSyncAccount,
			manualStashSyncIndex,
			STASHUnit.values().length,
			reason
		);

		pendingStashSyncEntries.clear();
		manualStashSyncIndex = 0;
		manualStashSyncAccount = null;
		manualStashSyncActive = false;
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
	public void onItemContainerChanged(
			ItemContainerChanged event
	)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		int containerId = event.getContainerId();

		if (containerId == InventoryID.WORN)
		{
		        observeDizanasQuiverAmmoIfContext(
		                "DIZANAS_QUIVER_CONTEXT",
		                true
		        );
		        return;
		}

		if (containerId == InventoryID.INV)
		{
			observePlankSackIfContext(
				"PLANK_SACK_CONTEXT",
				false
			);
			return;
		}

		ObservedItemContainerState state;
		String snapshotReason;

		if (containerId == InventoryID.INV_GROUP_TEMP)
		{
			state = cachedGimStorageState;
			snapshotReason = "GIM_STORAGE";
		}
		else if (
			containerId ==
				InventoryID.RAIDS_PRIVATESTORAGE
		)
		{
			state = cachedCoxPrivateStorageState;
			snapshotReason = "COX_PRIVATE_STORAGE";
		}
		else if (
			containerId ==
				InventoryID.RAIDS_SHAREDSTORAGE
		)
		{
			state = cachedCoxSharedStorageState;
			snapshotReason = "COX_SHARED_STORAGE";
		}
          else if (containerId == InventoryID.GRAVESTONE)
          {
                  state = cachedGravestoneStorageState;
                  snapshotReason = "GRAVESTONE_STORAGE";
          }
          else if (
                  containerId ==
                          InventoryID.DEATH_PERMANENT
          )
          {
                  state = cachedDeathsOfficeStorageState;
                  snapshotReason = "DEATHS_OFFICE_STORAGE";
          }
		else if (containerId == InventoryID.LOOTING_BAG)
		{
			state = cachedLootingBagState;
			snapshotReason = "PORTABLE_LOOTING_BAG";
		}
		else if (containerId == InventoryID.SEED_BOX)
		{
			state = cachedSeedBoxState;
			snapshotReason = "PORTABLE_SEED_BOX";
		}
		else if (containerId == InventoryID.TACKLE_BOX)
		{
			state = cachedTackleBoxState;
			snapshotReason = "PORTABLE_TACKLE_BOX";
		}
		else if (containerId == InventoryID.FORESTRY_KIT)
		{
			state = cachedForestryKitState;
			snapshotReason = "PORTABLE_FORESTRY_KIT";
		}
		else if (containerId == InventoryID.HUNTSMANS_KIT)
		{
			state = cachedHuntsmansKitState;
			snapshotReason = "PORTABLE_HUNTSMANS_KIT";
		}
		else if (
			containerId ==
				InventoryID.BARBARIAN_KNAPSACK
		)
		{
			state = cachedBarbarianKnapsackState;
			snapshotReason = "PORTABLE_BARBARIAN_KNAPSACK";
		}
		else
		{
			return;
		}

		/*
		 * An actual container event is authoritative. Later absence is not
		 * evidence of empty. Each retained state carries its own scope and
		 * ownership semantics.
		 */
		boolean accepted =
				state.observeIfPresent(
					event.getItemContainer(),
					Instant.now().toString()
				);

		if (accepted)
		{
			requestSnapshot(snapshotReason);
		}
	}

	@Subscribe
	public void onVarbitChanged(
	        VarbitChanged event
	)
	{
	        if (client.getLocalPlayer() == null)
	        {
	                return;
	        }

	        int varpId = event.getVarpId();

	        if (
	                varpId ==
	                        VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO ||
	                varpId ==
	                        VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT
	        )
	        {
	                observeDizanasQuiverAmmoIfContext(
	                        "DIZANAS_QUIVER_AMMO",
	                        true
	                );
	        }

          if (
                          event.getVarbitId() ==
                                          VarbitID.MOTHERLODE_SACK_TRANSMIT
          )
          {
                  observeMotherlodeSack(
                                  "MOTHERLODE_SACK",
                                  true
                  );
          }

		if (isPlankSackVarbit(event.getVarbitId()))
		{
			observePlankSackIfContext(
				"PLANK_SACK",
				true
			);
		}
  }

	private void observeLootingBagWidgetIfOpened()
	{
		Widget bagItems = client.getWidget(
				net.runelite.api.widgets.ComponentID.LOOTING_BAG_LOOTING_BAG_INVENTORY
		);

		boolean visible =
				bagItems != null &&
				!bagItems.isHidden();

		if (!visible)
		{
			lootingBagWidgetWasVisible = false;
			return;
		}

		if (lootingBagWidgetWasVisible)
		{
			return;
		}

		lootingBagWidgetWasVisible = true;

		Widget[] children = bagItems.getDynamicChildren();
		int[] itemIds = new int[children.length];

		for (int slot = 0; slot < children.length; slot++)
		{
			Widget child = children[slot];
			itemIds[slot] =
					child == null
							? -1
							: child.getItemId();
		}

		if (
				!LootingBagWidgetState.isAuthoritativeEmpty(
						true,
						itemIds
				)
		)
		{
			return;
		}

		boolean accepted =
				cachedLootingBagState.observe(
						new Item[0],
						Instant.now().toString()
				);

		if (accepted)
		{
			requestSnapshot(
					"PORTABLE_LOOTING_BAG",
					UI_SNAPSHOT_SETTLE_TICKS
			);
		}
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
					"Bank opened - scheduling snapshot after UI settle"
			);

			requestSnapshot(
			        "BANK_OPEN",
			        UI_SNAPSHOT_SETTLE_TICKS
			);
		}

		if (event.getGroupId() == 631)
		{
			log.info(
					"Seed Vault opened - scheduling snapshot after UI settle"
			);

			requestSnapshot(
			        "SEED_VAULT_OPEN",
			        UI_SNAPSHOT_SETTLE_TICKS
			);
		}
	}


  private boolean isInMotherlodeMine()
  {
          GameState gameState =
                          client.getGameState();

          if (
                          gameState != GameState.LOGGED_IN &&
                                          gameState != GameState.LOADING
          )
          {
                  return false;
          }

          int[] mapRegions =
                          client.getMapRegions();

          if (
                          mapRegions == null ||
                                          mapRegions.length == 0
          )
          {
                  return false;
          }

          for (int region : mapRegions)
          {
                  if (!MOTHERLODE_MAP_REGIONS.contains(region))
                  {
                          return false;
                  }
          }

          return true;
  }


  private void observeMotherlodeSack(
          String snapshotReason,
          boolean requestUpload
  )
  {
          if (!isInMotherlodeMine())
          {
                  return;
          }

          if (
                  entryObservationSettleGuard.isSettling(
                          client.getTickCount()
                  )
          )
          {
                  return;
          }

          int quantity =
                          client.getVarbitValue(
                                          VarbitID.MOTHERLODE_SACK_TRANSMIT
                          );

          if (quantity < 0)
          {
                  return;
          }

          boolean accepted =
                          cachedMotherlodeSackState.observe(
                                          quantity,
                                          Instant.now().toString()
                          );

          if (
                          accepted &&
                                          requestUpload
          )
          {
                  requestSnapshot(snapshotReason);
          }
  }

	private void observePlankSackIfContext(
        String snapshotReason,
        boolean requestUpload
	)
	{
        if (!containsItem(
                client.getItemContainer(InventoryID.INV),
                ItemID.PLANK_SACK
        ))
        {
                return;
        }

        boolean accepted =
                cachedPlankSackState.observe(
                        client.getVarbitValue(VarbitID.PLANK_SACK_PLAIN),
                        client.getVarbitValue(VarbitID.PLANK_SACK_OAK),
                        client.getVarbitValue(VarbitID.PLANK_SACK_TEAK),
                        client.getVarbitValue(VarbitID.PLANK_SACK_MAHOGANY),
                        client.getVarbitValue(VarbitID.PLANK_SACK_CAMPHOR),
                        client.getVarbitValue(VarbitID.PLANK_SACK_IRONWOOD),
                        client.getVarbitValue(VarbitID.PLANK_SACK_ROSEWOOD),
                        Instant.now().toString()
                );

        if (accepted && requestUpload)
        {
                requestSnapshot(snapshotReason);
        }
	}

	private boolean isPlankSackVarbit(int varbitId)
	{
        return varbitId == VarbitID.PLANK_SACK_PLAIN ||
                varbitId == VarbitID.PLANK_SACK_OAK ||
                varbitId == VarbitID.PLANK_SACK_TEAK ||
                varbitId == VarbitID.PLANK_SACK_MAHOGANY ||
                varbitId == VarbitID.PLANK_SACK_CAMPHOR ||
                varbitId == VarbitID.PLANK_SACK_IRONWOOD ||
                varbitId == VarbitID.PLANK_SACK_ROSEWOOD;
	}

	private boolean containsItem(
        ItemContainer container,
        int itemId
	)
	{
        if (container == null)
        {
                return false;
        }

        for (Item item : container.getItems())
        {
                if (item != null && item.getId() == itemId)
                {
                        return true;
                }
        }

        return false;
	}

	private void observeDizanasQuiverAmmoIfContext(
	        String snapshotReason,
	        boolean requestUpload
	)
	{
	        if (!hasDizanasQuiverContext())
	        {
	                return;
	        }

	        boolean accepted =
	                cachedDizanasQuiverAmmoState.observe(
	                        client.getVarpValue(
	                                VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO
	                        ),
	                        client.getVarpValue(
	                                VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT
	                        ),
	                        Instant.now().toString()
	                );

	        if (
	                accepted &&
	                requestUpload
	        )
	        {
	                requestSnapshot(snapshotReason);
	        }
	}

	private boolean hasDizanasQuiverContext()
	{
	        return containsQuiverAmmoCapableItem(
	                client.getItemContainer(InventoryID.INV)
	        ) ||
	                containsQuiverAmmoCapableItem(
	                        client.getItemContainer(InventoryID.WORN)
	                );
	}

	private boolean containsQuiverAmmoCapableItem(
	        ItemContainer container
	)
	{
	        if (container == null)
	        {
	                return false;
	        }

	        for (Item item : container.getItems())
	        {
	                if (
	                        item == null ||
	                        item.getId() <= 0
	                )
	                {
	                        continue;
	                }

	                if (
	                        client
	                                .getItemDefinition(item.getId())
	                                .getIntValue(
	                                        ParamID.QUIVER_AMMO_AVAILABLE
	                                ) == 1
	                )
	                {
	                        return true;
	                }
	        }

	        return false;
	}


	/*
	 * SNAPSHOT RATE LIMITER
	 *
	 * At most one payload may begin sending per second. Events that arrive
	 * inside that window do not form a queue: the newest reason overwrites
	 * the previous pending reason. When the window expires, onGameTick builds
	 * and sends one fresh snapshot for that newest reason.
	 */
	private void schedulePendingEntrySnapshot()
	{
	        if (pendingEntrySnapshotReason == null)
	        {
	                return;
	        }

	        String snapshotReason =
	                pendingEntrySnapshotReason;

	        pendingEntrySnapshotReason = null;

	        log.info(
	                "PUSH TRIGGER: {}",
	                snapshotReason
	        );

	        requestSnapshot(
	                snapshotReason,
	                entryObservationSettleGuard.remainingTicks(
	                        client.getTickCount()
	                )
	        );
	}


	private void requestSnapshot(
	        String snapshotReason
	)
	{
	        requestSnapshot(snapshotReason, 0);
	}


	private void requestSnapshot(
	        String snapshotReason,
	        int settleTicks
	)
	{
	        if (
	                snapshotReason == null ||
	                        snapshotReason.isBlank() ||
	                        client.getLocalPlayer() == null
	        )
	        {
	                return;
	        }

	        String previousReason =
	                snapshotScheduler.getPendingReason();

	        if (
	                previousReason != null &&
	                        !previousReason.equals(snapshotReason)
	        )
	        {
	                log.debug(
	                        "PUSH SCHEDULER: coalescing pending '{}' with newer '{}'",
	                        previousReason,
	                        snapshotReason
	                );
	        }

	        snapshotScheduler.schedule(
	                snapshotReason,
	                client.getTickCount(),
	                settleTicks
	        );
	}


	private void flushPendingSnapshotIfReady()
	{
	        if (
	                !snapshotScheduler.hasPending() ||
	                        client.getLocalPlayer() == null
	        )
	        {
	                return;
	        }

	        long currentTick =
	                client.getTickCount();

	        if (!snapshotScheduler.isDue(currentTick))
	        {
	                return;
	        }

	        long now =
	                System.currentTimeMillis();

	        if (
	                lastUploadTime != 0L &&
	                        now - lastUploadTime <
	                                MIN_UPLOAD_INTERVAL_MS
	        )
	        {
	                return;
	        }

	        String snapshotReason =
	                snapshotScheduler.takeIfDue(currentTick);

	        if (snapshotReason == null)
	        {
	                return;
	        }

	        log.debug(
	                "PUSH SCHEDULER: sending pending '{}'",
	                snapshotReason
	        );

	        sendSnapshot(snapshotReason);
	        lastUploadTime =
	                System.currentTimeMillis();
	}


	private boolean observeOwnedItemContainer(
			ObservedItemContainerState state,
			ItemContainer container,
			String observedAt
	)
	{
		if (container == null)
		{
			return false;
		}

		Item[] normalized =
				OwnedItemContainerNormalizer.withoutPlaceholders(
						container.getItems(),
						itemId ->
								client
										.getItemDefinition(itemId)
										.getPlaceholderTemplateId() != -1
				);

		return state.observe(
				normalized,
				observedAt
		);
	}

	private void observeContextualStorageObservation(
			ContextualStorageChatParser.Observation observation
	)
	{
		if (observation == null)
		{
			return;
		}

		ObservedContextualStorageState state;
		String snapshotReason;

		switch (observation.getDataset())
		{
			case HERB_SACK:
				state = cachedHerbSackState;
				snapshotReason = "PORTABLE_HERB_SACK";
				break;
			case GEM_BAG:
				state = cachedGemBagState;
				snapshotReason = "PORTABLE_GEM_BAG";
				break;
			case GEM_SATCHEL:
				state = cachedGemSatchelState;
				snapshotReason = "PORTABLE_GEM_SATCHEL";
				break;
			case COAL_BAG:
				state = cachedCoalBagState;
				snapshotReason = "PORTABLE_COAL_BAG";
				break;
			case FISH_BARREL:
				state = cachedFishBarrelState;
				snapshotReason = "PORTABLE_FISH_BARREL";
				break;
			case LOG_BASKET:
				state = cachedLogBasketState;
				snapshotReason = "PORTABLE_LOG_BASKET";
				break;
			default:
				return;
		}

		if (state.observe(
				observation.getEntries(),
				Instant.now().toString()
		))
		{
			requestSnapshot(snapshotReason);
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

		observeMotherlodeSack(
		        null,
		        false
		);

		observeDizanasQuiverAmmoIfContext(
		        null,
		        false
		);

		observePlankSackIfContext(
		        null,
		        false
		);


		ItemContainer seedVault =
				client.getItemContainer(
						InventoryID.SEED_VAULT
				);


		/*
		 * Refresh live bank cache if the bank is currently available.
		 */
		observeOwnedItemContainer(
				cachedBankState,
				bank,
				Instant.now().toString()
		);


		/*
		 * Refresh live Seed Vault cache if the Seed Vault is currently
		 * available.
		 */
		observeOwnedItemContainer(
				cachedSeedVaultState,
				seedVault,
				Instant.now().toString()
		);

        String bankJson =
                snapshotItemSerializer.bank(cachedBankState);

        String seedVaultJson =
                snapshotItemSerializer.seedVault(cachedSeedVaultState);

        String inventoryJson =
                snapshotItemSerializer.inventory(inventory);

        String equipmentJson =
                snapshotItemSerializer.equipment(equipment);




		String clientTime =
				Instant.now().toString();


		String evidenceJson =
				SnapshotEvidence.collect(
						clientTime,
						snapshotReason,
						clientSessionId,
						++snapshotSequence,
						inventory != null,
						equipment != null,
						cachedBankState.getObservedAt(),
						cachedSeedVaultState.getObservedAt(),
						cachedGimStorageState.getObservedAt(),
						cachedCoxPrivateStorageState.getObservedAt(),
						cachedCoxSharedStorageState.getObservedAt(),
						cachedGravestoneStorageState.getObservedAt(),
						cachedDeathsOfficeStorageState.getObservedAt(),
						cachedPotionStorageState.getObservedAt(),
						cachedMotherlodeSackState.getObservedAt(),
						cachedLootingBagState.getObservedAt(),
						cachedSeedBoxState.getObservedAt(),
						cachedTackleBoxState.getObservedAt(),
						cachedForestryKitState.getObservedAt(),
						cachedHuntsmansKitState.getObservedAt(),
						cachedBarbarianKnapsackState.getObservedAt(),
						cachedDizanasQuiverAmmoState.getObservedAt(),
						cachedCollectionLogCapturedAt,
						cachedCollectionLogPages.size(),
						collectionInstantCapturedAt,
						cachedStashState.getObservedAt(),
                        cachedPlankSackState.getObservedAt(),
                        cachedHerbSackState.getObservedAt(),
                        cachedGemBagState.getObservedAt(),
                        cachedGemSatchelState.getObservedAt(),
                        cachedCoalBagState.getObservedAt(),
                        cachedFishBarrelState.getObservedAt(),
                        cachedLogBasketState.getObservedAt()
				);

		String diaryTaskStateJson = AchievementDiaryState.collect(client);

		String globalResourceCapabilityStateJson =
				GlobalResourceCapabilityState.collect(client);

		String persistentStorageLiveItemStateJson =
				PersistentStorageEvidence.collectLiveItemState(client);

		String gimStorageJson =
				snapshotItemSerializer.observedItemContainer(cachedGimStorageState);

		String coxPrivateStorageJson =
				snapshotItemSerializer.observedItemContainer(cachedCoxPrivateStorageState);

		String coxSharedStorageJson =
				snapshotItemSerializer.observedItemContainer(cachedCoxSharedStorageState);
          String gravestoneStorageJson =
                          snapshotItemSerializer.observedItemContainer(cachedGravestoneStorageState);
          String deathsOfficeStorageJson =
                          snapshotItemSerializer.observedItemContainer(cachedDeathsOfficeStorageState);
          String potionStorageJson =
                          snapshotItemSerializer.potionStorage(cachedPotionStorageState);
          String motherlodeSackJson =
                          snapshotItemSerializer.motherlodeSack(cachedMotherlodeSackState);

		String plankSackJson =
				cachedPlankSackState.toJson();

        String herbSackJson =
                cachedHerbSackState.toJson();

        String gemBagJson =
                cachedGemBagState.toJson();

        String gemSatchelJson =
                cachedGemSatchelState.toJson();

        String coalBagJson =
                cachedCoalBagState.toJson();

        String fishBarrelJson =
                cachedFishBarrelState.toJson();

        String logBasketJson =
                cachedLogBasketState.toJson();




		String lootingBagJson =
				snapshotItemSerializer.observedItemContainer(cachedLootingBagState);

		String seedBoxJson =
				snapshotItemSerializer.observedItemContainer(cachedSeedBoxState);

		String tackleBoxJson =
				snapshotItemSerializer.observedItemContainer(cachedTackleBoxState);

		String forestryKitJson =
				snapshotItemSerializer.observedItemContainer(cachedForestryKitState);

		String huntsmansKitJson =
				snapshotItemSerializer.observedItemContainer(cachedHuntsmansKitState);

		String barbarianKnapsackJson =
				snapshotItemSerializer.observedItemContainer(cachedBarbarianKnapsackState);

		String dizanasQuiverAmmoJson =
				snapshotItemSerializer.quiverAmmo(
				        cachedDizanasQuiverAmmoState
				);

		String stashUnitsJson =
		        cachedStashState.toJson();

		SnapshotLiveStateCollector.State liveState =
                snapshotLiveStateCollector.collect();

                String collectionLogJson =
                snapshotCollectionLogSerializer.pages(
                        cachedCollectionLogPages
                );

        String instantCollectionLogJson =
                snapshotCollectionLogSerializer.instant(
                        collectionInstantCapturedAt,
                        instantCollectionLogItems
                );

        SnapshotEnvelopeSerializer.Parts envelopeParts =
                new SnapshotEnvelopeSerializer.Parts();

        envelopeParts.account = account;
        envelopeParts.clientTime = clientTime;
        envelopeParts.snapshotReason = snapshotReason;
        envelopeParts.evidenceJson = evidenceJson;
        envelopeParts.liveState = liveState;
        envelopeParts.diaryTaskStateJson = diaryTaskStateJson;
        envelopeParts.globalResourceCapabilityStateJson =
                globalResourceCapabilityStateJson;
        envelopeParts.persistentStorageLiveItemStateJson =
                persistentStorageLiveItemStateJson;
        envelopeParts.collectionLogJson = collectionLogJson;
        envelopeParts.instantCollectionLogJson =
                instantCollectionLogJson;
        envelopeParts.inventoryJson = inventoryJson;
        envelopeParts.bankJson = bankJson;
        envelopeParts.seedVaultJson = seedVaultJson;
        envelopeParts.gimStorageJson = gimStorageJson;
        envelopeParts.coxPrivateStorageJson = coxPrivateStorageJson;
        envelopeParts.coxSharedStorageJson = coxSharedStorageJson;
        envelopeParts.gravestoneStorageJson = gravestoneStorageJson;
        envelopeParts.deathsOfficeStorageJson = deathsOfficeStorageJson;
        envelopeParts.potionStorageJson = potionStorageJson;
        envelopeParts.motherlodeSackJson = motherlodeSackJson;
        envelopeParts.plankSackJson = plankSackJson;
        envelopeParts.herbSackJson = herbSackJson;
        envelopeParts.gemBagJson = gemBagJson;
        envelopeParts.gemSatchelJson = gemSatchelJson;
        envelopeParts.coalBagJson = coalBagJson;
        envelopeParts.fishBarrelJson = fishBarrelJson;
        envelopeParts.logBasketJson = logBasketJson;
        envelopeParts.lootingBagJson = lootingBagJson;
        envelopeParts.seedBoxJson = seedBoxJson;
        envelopeParts.tackleBoxJson = tackleBoxJson;
        envelopeParts.forestryKitJson = forestryKitJson;
        envelopeParts.huntsmansKitJson = huntsmansKitJson;
        envelopeParts.barbarianKnapsackJson =
                barbarianKnapsackJson;
        envelopeParts.dizanasQuiverAmmoJson =
                dizanasQuiverAmmoJson;
        envelopeParts.stashUnitsJson = stashUnitsJson;
        envelopeParts.equipmentJson = equipmentJson;

        String json =
                SnapshotEnvelopeSerializer.serialize(
                        envelopeParts
                );

if (log.isDebugEnabled())
        {
            log.debug(
                    "Outgoing snapshot diagnostic: {}",
                    SnapshotDiagnostics.summarize(gson, json)
            );
        }

        snapshotTransport.send(
                config.backendUrl(),
                config.writeToken(),
                json,
                snapshotReason
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
