package com.osrsoracle;

import java.util.StringJoiner;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;

@Slf4j
final class SnapshotLiveStateCollector
{
static final class State
{
final String accountTypeJson;
final String membershipActiveJson;
final String membershipDaysJson;
final int caEasy;
final int caMedium;
final int caHard;
final int caElite;
final int caMaster;
final int caGrandmaster;
final String caCompletedIdsJson;
final String diaryJson;
final String slayerTask;
final int slayerRemaining;
final String skillsJson;
final String questsJson;

State(
String accountTypeJson,
String membershipActiveJson,
String membershipDaysJson,
int caEasy,
int caMedium,
int caHard,
int caElite,
int caMaster,
int caGrandmaster,
String caCompletedIdsJson,
String diaryJson,
String slayerTask,
int slayerRemaining,
String skillsJson,
String questsJson
)
{
this.accountTypeJson = accountTypeJson;
this.membershipActiveJson = membershipActiveJson;
this.membershipDaysJson = membershipDaysJson;
this.caEasy = caEasy;
this.caMedium = caMedium;
this.caHard = caHard;
this.caElite = caElite;
this.caMaster = caMaster;
this.caGrandmaster = caGrandmaster;
this.caCompletedIdsJson = caCompletedIdsJson;
this.diaryJson = diaryJson;
this.slayerTask = slayerTask;
this.slayerRemaining = slayerRemaining;
this.skillsJson = skillsJson;
this.questsJson = questsJson;
}
}

private static final String[] ACCOUNT_TYPE_NAMES = {
"NORMAL",
"IRONMAN",
"ULTIMATE_IRONMAN",
"HARDCORE_IRONMAN",
"GROUP_IRONMAN",
"HARDCORE_GROUP_IRONMAN",
"UNRANKED_GROUP_IRONMAN"
};

private static final int[] CA_TASK_COMPLETION_VARPS = {
VarPlayerID.CA_TASK_COMPLETED_0,
VarPlayerID.CA_TASK_COMPLETED_1,
VarPlayerID.CA_TASK_COMPLETED_2,
VarPlayerID.CA_TASK_COMPLETED_3,
VarPlayerID.CA_TASK_COMPLETED_4,
VarPlayerID.CA_TASK_COMPLETED_5,
VarPlayerID.CA_TASK_COMPLETED_6,
VarPlayerID.CA_TASK_COMPLETED_7,
VarPlayerID.CA_TASK_COMPLETED_8,
VarPlayerID.CA_TASK_COMPLETED_9,
VarPlayerID.CA_TASK_COMPLETED_10,
VarPlayerID.CA_TASK_COMPLETED_11,
VarPlayerID.CA_TASK_COMPLETED_12,
VarPlayerID.CA_TASK_COMPLETED_13,
VarPlayerID.CA_TASK_COMPLETED_14,
VarPlayerID.CA_TASK_COMPLETED_15,
VarPlayerID.CA_TASK_COMPLETED_16,
VarPlayerID.CA_TASK_COMPLETED_17,
VarPlayerID.CA_TASK_COMPLETED_18,
VarPlayerID.CA_TASK_COMPLETED_19,
VarPlayerID.CA_TASK_COMPLETED_20
};

private final Client client;

@Inject
SnapshotLiveStateCollector(Client client)
{
this.client = client;
}

State collect()
{
int accountTypeCode =
				client.getVarbitValue(
						Varbits.ACCOUNT_TYPE
				);

		String accountTypeJson =
				accountTypeCode >= 0 &&
						accountTypeCode < ACCOUNT_TYPE_NAMES.length
						? "\"" + ACCOUNT_TYPE_NAMES[accountTypeCode] + "\""
						: "null";

		int membershipDays =
				client.getVarpValue(
						VarPlayerID.ACCOUNT_CREDIT
				);

		String membershipActiveJson =
				membershipDays >= 0
						? Boolean.toString(membershipDays > 0)
						: "null";

		String membershipDaysJson =
				membershipDays >= 0
						? Integer.toString(membershipDays)
						: "null";


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
				taskId < CA_TASK_COMPLETION_VARPS.length * 32;
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




return new State(
accountTypeJson,
membershipActiveJson,
membershipDaysJson,
caEasy,
caMedium,
caHard,
caElite,
caMaster,
caGrandmaster,
caCompletedIds.toString(),
diaryJson,
slayerTask,
slayerRemaining,
skillsJson.toString(),
questsJson.toString()
);
}

private boolean isCombatAchievementComplete(int taskId)
{
if (taskId < 0)
{
return false;
}

int block = taskId / 32;

if (block >= CA_TASK_COMPLETION_VARPS.length)
{
return false;
}

int bit = taskId % 32;
int packed =
client.getVarpValue(
CA_TASK_COMPLETION_VARPS[block]
);

return (
packed &
(1 << bit)
) != 0;
}

private static String escapeJson(String text)
{
if (text == null)
{
return "";
}

return text
.replace("\\", "\\\\")
.replace("\"", "\\\"")
.replace("\n", "\\n")
.replace("\r", "\\r")
.replace("\t", "\\t");
}
}
