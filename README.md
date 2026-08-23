# Oracle

Oracle is a RuneLite plugin that synchronizes Old School RuneScape account state with an external backend for use by AI assistants and other tools.

It can provide current account information such as:

- Skills and XP
- Quest states
- Combat Achievements
- Achievement Diaries
- Inventory
- Equipment
- Bank contents
- Seed Vault contents
- Collection Log data
- Slayer task information

Oracle is designed as a data synchronization layer rather than a gameplay assistant. The plugin collects current account facts, while external tools can interpret that data according to the user's goals.

## Backend Required

Oracle does not include or provide a hosted backend service.

You are responsible for providing and maintaining your own compatible backend endpoint for receiving and storing account data.

The plugin requires:

- A backend URL
- A write token

These are configured through the RuneLite plugin settings.

Oracle does not provide a preconfigured server, database, API, hosting service, or backend deployment.

## Privacy

Account data is only sent to the backend configured by the user.

Because you control the backend, you are also responsible for how that backend stores, protects, processes, and exposes the data it receives.