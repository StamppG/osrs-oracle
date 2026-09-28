package com.osrsoracle;

import java.util.Set;

final class AccountTypeResolution
{
    enum Status
    {
        UNRESOLVED_ONBOARDING,
        PROVISIONAL_ONBOARDING,
        RESOLVED,
        UNKNOWN
    }

    private static final Set<Integer> TUTORIAL_ISLAND_REGIONS = Set.of(
            12336,
            12335,
            12592,
            12080,
            12079,
            12436
    );

    private static final String[] ACCOUNT_TYPE_NAMES = {
            "NORMAL",
            "IRONMAN",
            "ULTIMATE_IRONMAN",
            "HARDCORE_IRONMAN",
            "GROUP_IRONMAN",
            "HARDCORE_GROUP_IRONMAN",
            "UNRANKED_GROUP_IRONMAN"
    };

    private final Status status;
    private final Integer rawAccountType;
    private final String officialAccountType;
    private final String provisionalAccountType;
    private final Boolean inTutorialIsland;
    private final String observedAt;

    private AccountTypeResolution(
            Status status,
            Integer rawAccountType,
            String officialAccountType,
            String provisionalAccountType,
            Boolean inTutorialIsland,
            String observedAt
    )
    {
        this.status = status;
        this.rawAccountType = rawAccountType;
        this.officialAccountType = officialAccountType;
        this.provisionalAccountType = provisionalAccountType;
        this.inTutorialIsland = inTutorialIsland;
        this.observedAt = observedAt;
    }

    static AccountTypeResolution resolve(
            Integer rawAccountType,
            Integer regionId,
            String observedAt
    )
    {
        if (observedAt == null || observedAt.isBlank())
        {
            throw new IllegalArgumentException("observedAt");
        }

        if (rawAccountType == null || regionId == null)
        {
            return unknown(
                    rawAccountType,
                    null,
                    observedAt
            );
        }

        String mappedType = mapAccountType(rawAccountType);

        if (mappedType == null)
        {
            return unknown(
                    rawAccountType,
                    TUTORIAL_ISLAND_REGIONS.contains(regionId),
                    observedAt
            );
        }

        boolean inTutorialIsland =
                TUTORIAL_ISLAND_REGIONS.contains(regionId);

        if (inTutorialIsland)
        {
            if (rawAccountType == 0)
            {
                return new AccountTypeResolution(
                        Status.UNRESOLVED_ONBOARDING,
                        rawAccountType,
                        null,
                        null,
                        true,
                        observedAt
                );
            }

            return new AccountTypeResolution(
                    Status.PROVISIONAL_ONBOARDING,
                    rawAccountType,
                    null,
                    mappedType,
                    true,
                    observedAt
            );
        }

        return new AccountTypeResolution(
                Status.RESOLVED,
                rawAccountType,
                mappedType,
                null,
                false,
                observedAt
        );
    }

    private static AccountTypeResolution unknown(
            Integer rawAccountType,
            Boolean inTutorialIsland,
            String observedAt
    )
    {
        return new AccountTypeResolution(
                Status.UNKNOWN,
                rawAccountType,
                null,
                null,
                inTutorialIsland,
                observedAt
        );
    }

    private static String mapAccountType(Integer rawAccountType)
    {
        if (
                rawAccountType == null ||
                        rawAccountType < 0 ||
                        rawAccountType >= ACCOUNT_TYPE_NAMES.length
        )
        {
            return null;
        }

        return ACCOUNT_TYPE_NAMES[rawAccountType];
    }

    String getOfficialAccountTypeJson()
    {
        return jsonString(officialAccountType);
    }

    String toJson()
    {
        return "{\"status\":" + jsonString(status.name()) +
                ",\"rawAccountType\":" +
                (rawAccountType == null
                        ? "null"
                        : rawAccountType) +
                ",\"provisionalAccountType\":" +
                jsonString(provisionalAccountType) +
                ",\"inTutorialIsland\":" +
                (inTutorialIsland == null
                        ? "null"
                        : inTutorialIsland.toString()) +
                ",\"observedAt\":" +
                jsonString(observedAt) +
                "}";
    }

    Status getStatus()
    {
        return status;
    }

    Integer getRawAccountType()
    {
        return rawAccountType;
    }

    String getOfficialAccountType()
    {
        return officialAccountType;
    }

    String getProvisionalAccountType()
    {
        return provisionalAccountType;
    }

    Boolean getInTutorialIsland()
    {
        return inTutorialIsland;
    }

    String getObservedAt()
    {
        return observedAt;
    }

    private static String jsonString(String value)
    {
        if (value == null)
        {
            return "null";
        }

        return "\"" +
                value
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r") +
                "\"";
    }
}