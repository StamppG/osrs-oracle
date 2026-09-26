package com.osrsoracle;

import net.runelite.api.Item;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SnapshotItemSerializerTest
{
private final SnapshotItemSerializer serializer =
new SnapshotItemSerializer(
itemId ->
itemId == 995
? "Coins"
: "Item " + itemId
);

@Test
public void retainedUnknownAndEmptyRemainDistinct()
{
ObservedItemContainerState state =
new ObservedItemContainerState(
"gimStorage",
ObservedItemContainerState.Scope.GROUP,
true
);

assertEquals(
"{\"scope\":\"GROUP\",\"ownership\":\"SHARED\",\"items\":null}",
serializer.observedItemContainer(state)
);

state.observe(
new Item[0],
"2026-09-26T00:00:00Z"
);

assertEquals(
"{\"scope\":\"GROUP\",\"ownership\":\"SHARED\",\"items\":[]}",
serializer.observedItemContainer(state)
);
}

@Test
public void retainedContainerPreservesSlotsAndOwnership()
{
ObservedItemContainerState state =
new ObservedItemContainerState(
"bank",
ObservedItemContainerState.Scope.ACCOUNT,
false
);

state.observe(
new Item[] {
null,
new Item(995, 1234)
},
"2026-09-26T00:00:00Z"
);

assertEquals(
"{\"scope\":\"ACCOUNT\",\"ownership\":\"PERSONAL\",\"items\":[null,{\"slot\":1,\"name\":\"Coins\",\"id\":995,\"quantity\":1234}]}",
serializer.observedItemContainer(state)
);
}

@Test
public void bankPreservesNullSlots()
{
ObservedItemContainerState state =
new ObservedItemContainerState(
"bank",
ObservedItemContainerState.Scope.ACCOUNT,
false
);

state.observe(
new Item[] {
null,
new Item(995, 5)
},
"2026-09-26T00:00:00Z"
);

assertEquals(
"[null,{\"slot\":1,\"name\":\"Coins\",\"id\":995,\"quantity\":5}]",
serializer.bank(state)
);
}

@Test
public void seedVaultSkipsPlaceholderIdsWithoutRenumberingSlots()
{
ObservedItemContainerState state =
new ObservedItemContainerState(
"seedVault",
ObservedItemContainerState.Scope.ACCOUNT,
false
);

state.observe(
new Item[] {
new Item(-1, 1),
null,
new Item(995, 12)
},
"2026-09-26T00:00:00Z"
);

assertEquals(
"[{\"slot\":2,\"name\":\"Coins\",\"id\":995,\"quantity\":12}]",
serializer.seedVault(state)
);
}

@Test
public void unobservedSpecialStoresPreserveNullMeaning()
{
assertEquals(
"{\"scope\":\"ACTIVITY\",\"ownership\":\"PERSONAL\",\"quantity\":null}",
serializer.motherlodeSack(
new ObservedMotherlodeSackState()
)
);

assertEquals(
"{\"scope\":\"ACCOUNT\",\"ownership\":\"PERSONAL\",\"ammoItemIdRaw\":null,\"ammoQuantity\":null}",
serializer.quiverAmmo(
new ObservedQuiverAmmoState()
)
);
}
}