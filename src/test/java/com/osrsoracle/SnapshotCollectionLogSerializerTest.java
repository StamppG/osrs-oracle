package com.osrsoracle;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SnapshotCollectionLogSerializerTest
{
private final SnapshotCollectionLogSerializer serializer =
new SnapshotCollectionLogSerializer();

@Test
public void pagesPreserveNamesRawJsonAndIterationOrder()
{
Map<String, String> pages = new LinkedHashMap<>();

pages.put(
"Bosses \"A\"",
"{\"obtained\":1}"
);

pages.put(
"Raids",
"{\"obtained\":2}"
);

assertEquals(
"{\"pages\":{\"Bosses \\\"A\\\"\":{\"obtained\":1},\"Raids\":{\"obtained\":2}}}",
serializer.pages(pages)
);
}

@Test
public void instantUnobservedRemainsNull()
{
assertEquals(
"null",
serializer.instant(
null,
new LinkedHashMap<>()
)
);
}

@Test
public void instantObservedEmptyRemainsAuthoritativeEmpty()
{
assertEquals(
"{\"capturedAt\":\"2026-09-26T12:00:00Z\",\"items\":[]}",
serializer.instant(
"2026-09-26T12:00:00Z",
new LinkedHashMap<>()
)
);
}

@Test
public void instantItemsPreserveIdsQuantitiesAndOrder()
{
Map<Integer, Integer> items = new LinkedHashMap<>();

items.put(4151, 1);
items.put(11840, 3);

assertEquals(
"{\"capturedAt\":\"2026-09-26T12:00:00Z\",\"items\":[{\"id\":4151,\"quantity\":1},{\"id\":11840,\"quantity\":3}]}",
serializer.instant(
"2026-09-26T12:00:00Z",
items
)
);
}
}