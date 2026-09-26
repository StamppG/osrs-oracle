package com.osrsoracle;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SnapshotDiagnosticsTest
{
@Test
public void redactsValuesButPreservesStateSemantics()
{
String json =
"{\"account\":\"BlueHelmSolo\"," +
"\"snapshotReason\":\"HEARTBEAT\"," +
"\"evidence\":{\"coverage\":{" +
"\"bank\":{\"status\":\"NOT_OBSERVED\",\"observedAt\":null}," +
"\"seedVault\":{\"status\":\"OBSERVED\",\"observedAt\":\"2026-09-26T00:00:00Z\"}}}," +
"\"bank\":null," +
"\"seedVault\":[]," +
"\"fishBarrel\":{\"scope\":\"ACCOUNT\",\"ownership\":\"PERSONAL\"," +
"\"observedAt\":\"2026-09-26T00:00:00Z\",\"items\":[]}," +
"\"inventory\":[{\"name\":\"Dragon scimitar\",\"id\":4587,\"quantity\":1}]}";

String summary =
SnapshotDiagnostics.summarize(
new Gson(),
json
);

assertTrue(summary.contains("reason=HEARTBEAT"));
assertTrue(summary.contains("bank=NULL"));
assertTrue(summary.contains("seedVault=EMPTY_ARRAY"));
assertTrue(summary.contains("items=EMPTY_ARRAY"));
assertTrue(summary.contains("bank=NOT_OBSERVED@NULL"));
assertTrue(summary.contains("seedVault=OBSERVED@PRESENT"));

assertFalse(summary.contains("BlueHelmSolo"));
assertFalse(summary.contains("Dragon scimitar"));
assertFalse(summary.contains("4587"));
assertFalse(summary.contains("2026-09-26T00:00:00Z"));
}

@Test
public void malformedPayloadCannotBreakDiagnosticPath()
{
String summary =
SnapshotDiagnostics.summarize(
new Gson(),
"{not-valid-json"
);

assertEquals(
"payload=INVALID, bytes=15",
summary
);
}
}