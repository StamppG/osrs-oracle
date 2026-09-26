package com.osrsoracle;

import java.util.Map;
import java.util.StringJoiner;
import javax.inject.Inject;

final class SnapshotCollectionLogSerializer
{
	@Inject
	SnapshotCollectionLogSerializer()
	{
	}

String pages(Map<String, String> cachedPages)
{
StringJoiner collectionPagesJson =
new StringJoiner(",", "{", "}");

for (
Map.Entry<String, String> entry :
cachedPages.entrySet()
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

return String.format(
"{\"pages\":%s}",
collectionPagesJson
);
}

String instant(
String capturedAt,
Map<Integer, Integer> items
)
{
if (capturedAt == null)
{
return "null";
}

StringJoiner instantItemsJson =
new StringJoiner(",", "[", "]");

for (
Map.Entry<Integer, Integer> entry :
items.entrySet()
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

return String.format(
"{\"capturedAt\":\"%s\",\"items\":%s}",
escapeJson(capturedAt),
instantItemsJson
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