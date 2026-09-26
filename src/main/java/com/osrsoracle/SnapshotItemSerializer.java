package com.osrsoracle;

import java.util.StringJoiner;
import java.util.function.IntFunction;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

final class SnapshotItemSerializer
{
private final IntFunction<String> itemNameResolver;

@Inject
SnapshotItemSerializer(Client client)
{
this(
itemId ->
client
.getItemDefinition(itemId)
.getName()
);
}

SnapshotItemSerializer(IntFunction<String> itemNameResolver)
{
this.itemNameResolver = itemNameResolver;
}

String bank(ObservedItemContainerState state)
{
Item[] items = state.getItems();

if (items == null)
{
return "null";
}

StringJoiner json =
new StringJoiner(",", "[", "]");

for (int slot = 0; slot < items.length; slot++)
{
Item item = items[slot];

if (item == null)
{
json.add("null");
continue;
}

json.add(
itemWithSlot(
slot,
item
)
);
}

return json.toString();
}

String seedVault(ObservedItemContainerState state)
{
Item[] items = state.getItems();

if (items == null)
{
return "null";
}

StringJoiner json =
new StringJoiner(",", "[", "]");

for (int slot = 0; slot < items.length; slot++)
{
Item item = items[slot];

if (
item == null ||
item.getId() <= 0
)
{
continue;
}

json.add(
itemWithSlot(
slot,
item
)
);
}

return json.toString();
}

String inventory(ItemContainer inventory)
{
if (inventory == null)
{
return "null";
}

StringJoiner json =
new StringJoiner(",", "[", "]");

for (int slot = 0; slot < 28; slot++)
{
Item item =
slot < inventory.size()
? inventory.getItem(slot)
: null;

if (item == null)
{
json.add("null");
}
else
{
json.add(
itemWithSlot(
slot,
item
)
);
}
}

return json.toString();
}

String equipment(ItemContainer equipment)
{
if (equipment == null)
{
return "null";
}

return String.format(
"{\"head\":%s,\"cape\":%s,\"amulet\":%s,\"weapon\":%s,\"body\":%s,\"shield\":%s,\"legs\":%s,\"gloves\":%s,\"boots\":%s,\"ring\":%s,\"ammo\":%s}",
equipmentItem(
equipment,
EquipmentInventorySlot.HEAD
),
equipmentItem(
equipment,
EquipmentInventorySlot.CAPE
),
equipmentItem(
equipment,
EquipmentInventorySlot.AMULET
),
equipmentItem(
equipment,
EquipmentInventorySlot.WEAPON
),
equipmentItem(
equipment,
EquipmentInventorySlot.BODY
),
equipmentItem(
equipment,
EquipmentInventorySlot.SHIELD
),
equipmentItem(
equipment,
EquipmentInventorySlot.LEGS
),
equipmentItem(
equipment,
EquipmentInventorySlot.GLOVES
),
equipmentItem(
equipment,
EquipmentInventorySlot.BOOTS
),
equipmentItem(
equipment,
EquipmentInventorySlot.RING
),
equipmentItem(
equipment,
EquipmentInventorySlot.AMMO
)
);
}

String observedItemContainer(
ObservedItemContainerState state
)
{
Item[] observedItems = state.getItems();

String scope =
state.getScope().name();

String ownership =
state.isShared()
? "SHARED"
: "PERSONAL";

if (observedItems == null)
{
return String.format(
"{\"scope\":\"%s\",\"ownership\":\"%s\",\"items\":null}",
scope,
ownership
);
}

StringJoiner items =
new StringJoiner(",", "[", "]");

for (
int slot = 0;
slot < observedItems.length;
slot++
)
{
Item item = observedItems[slot];

if (
item == null ||
item.getId() <= 0
)
{
items.add("null");
continue;
}

items.add(
itemWithSlot(
slot,
item
)
);
}

return String.format(
"{\"scope\":\"%s\",\"ownership\":\"%s\",\"items\":%s}",
scope,
ownership,
items
);
}

String potionStorage(
ObservedPotionStorageState state
)
{
ObservedPotionStorageState.Entry[] entries =
state.getEntries();

if (entries == null)
{
return "{\"scope\":\"ACCOUNT\",\"ownership\":\"PERSONAL\",\"entries\":null}";
}

StringJoiner entryJson =
new StringJoiner(",", "[", "]");

for (ObservedPotionStorageState.Entry entry : entries)
{
entryJson.add(
String.format(
"{\"name\":\"%s\",\"id\":%d,\"amount\":%d,\"amountType\":\"%s\"}",
escapeJson(
itemNameResolver.apply(
entry.getItemId()
)
),
entry.getItemId(),
entry.getAmount(),
entry.getAmountType()
)
);
}

return String.format(
"{\"scope\":\"ACCOUNT\",\"ownership\":\"PERSONAL\",\"entries\":%s}",
entryJson
);
}

String motherlodeSack(
ObservedMotherlodeSackState state
)
{
if (!state.hasObservation())
{
return "{\"scope\":\"ACTIVITY\",\"ownership\":\"PERSONAL\",\"quantity\":null}";
}

return String.format(
"{\"scope\":\"ACTIVITY\",\"ownership\":\"PERSONAL\",\"quantity\":%d}",
state.getQuantity()
);
}

String quiverAmmo(
ObservedQuiverAmmoState state
)
{
if (!state.hasObservation())
{
return "{\"scope\":\"ACCOUNT\",\"ownership\":\"PERSONAL\",\"ammoItemIdRaw\":null,\"ammoQuantity\":null}";
}

return String.format(
"{\"scope\":\"ACCOUNT\",\"ownership\":\"PERSONAL\",\"ammoItemIdRaw\":%d,\"ammoQuantity\":%d}",
state.getAmmoItemIdRaw(),
state.getAmmoQuantity()
);
}

private String itemWithSlot(
int slot,
Item item
)
{
return String.format(
"{\"slot\":%d,\"name\":\"%s\",\"id\":%d,\"quantity\":%d}",
slot,
escapeJson(
itemNameResolver.apply(
item.getId()
)
),
item.getId(),
item.getQuantity()
);
}

private String equipmentItem(
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

return String.format(
"{\"name\":\"%s\",\"id\":%d,\"quantity\":%d}",
escapeJson(
itemNameResolver.apply(
item.getId()
)
),
item.getId(),
item.getQuantity()
);
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