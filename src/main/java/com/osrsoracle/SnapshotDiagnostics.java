package com.osrsoracle;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

final class SnapshotDiagnostics
{
private SnapshotDiagnostics()
{
}

static String summarize(Gson gson, String json)
{
try
{
return summarizeValidPayload(gson, json);
}
catch (RuntimeException ignored)
{
return "payload=INVALID, bytes=" + byteLength(json);
}
}

private static String summarizeValidPayload(
Gson gson,
String json
)
{
JsonObject root =
gson.fromJson(
json,
JsonObject.class
);

if (root == null)
{
return "payload=INVALID, bytes=" + byteLength(json);
}

String reason =
root.has("snapshotReason") &&
!root.get("snapshotReason").isJsonNull()
? root.get("snapshotReason").getAsString()
: "<missing>";

StringJoiner fields =
new StringJoiner(",", "{", "}");

for (
Map.Entry<String, JsonElement> entry :
root.entrySet()
)
{
if ("snapshotReason".equals(entry.getKey()))
{
continue;
}

fields.add(
entry.getKey() +
"=" +
describe(entry.getValue())
);
}

String coverage =
describeCoverage(root);

return "reason=" + reason +
", bytes=" + byteLength(json) +
", fields=" + fields +
", coverage=" + coverage;
}

private static int byteLength(String json)
{
return json == null
? 0
: json.getBytes(StandardCharsets.UTF_8).length;
}

private static String describe(JsonElement value)
{
if (
value == null ||
value.isJsonNull()
)
{
return "NULL";
}

if (value.isJsonArray())
{
return value.getAsJsonArray().size() == 0
? "EMPTY_ARRAY"
: "ARRAY_PRESENT";
}

if (!value.isJsonObject())
{
return "SCALAR_PRESENT";
}

JsonObject object =
value.getAsJsonObject();

if (object.size() == 0)
{
return "EMPTY_OBJECT";
}

StringJoiner semantics =
new StringJoiner(
",",
"OBJECT_PRESENT[",
"]"
);

addPresence(
object,
"observedAt",
semantics
);

addChildState(
object,
"items",
semantics
);

addChildState(
object,
"entries",
semantics
);

addChildState(
object,
"contents",
semantics
);

addChildState(
object,
"units",
semantics
);

addChildState(
object,
"pages",
semantics
);

return semantics.length() ==
"OBJECT_PRESENT[]".length()
? "OBJECT_PRESENT"
: semantics.toString();
}

private static void addPresence(
JsonObject object,
String key,
StringJoiner semantics
)
{
if (!object.has(key))
{
return;
}

JsonElement value =
object.get(key);

semantics.add(
key +
"=" +
(
value == null ||
value.isJsonNull()
? "NULL"
: "PRESENT"
)
);
}

private static void addChildState(
JsonObject object,
String key,
StringJoiner semantics
)
{
if (object.has(key))
{
semantics.add(
key +
"=" +
describe(object.get(key))
);
}
}

private static String describeCoverage(
JsonObject root
)
{
if (
!root.has("evidence") ||
!root.get("evidence").isJsonObject()
)
{
return "MISSING";
}

JsonObject evidence =
root.getAsJsonObject("evidence");

if (
!evidence.has("coverage") ||
!evidence.get("coverage").isJsonObject()
)
{
return "MISSING";
}

JsonObject coverage =
evidence.getAsJsonObject("coverage");

StringJoiner summary =
new StringJoiner(",", "{", "}");

for (
Map.Entry<String, JsonElement> entry :
coverage.entrySet()
)
{
JsonElement value =
entry.getValue();

if (
value == null ||
value.isJsonNull() ||
!value.isJsonObject()
)
{
summary.add(
entry.getKey() +
"=" +
describe(value)
);

continue;
}

JsonObject observation =
value.getAsJsonObject();

String status =
observation.has("status") &&
!observation.get("status").isJsonNull()
? observation.get("status").getAsString()
: "OBJECT_PRESENT";

String freshness = "";

if (observation.has("observedAt"))
{
freshness =
observation.get("observedAt").isJsonNull()
? "@NULL"
: "@PRESENT";
}

summary.add(
entry.getKey() +
"=" +
status +
freshness
);
}

return summary.toString();
}
}