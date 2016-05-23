# Changelog
Changelog can generally be found here https://www.spigotmc.org/resources/17067/updates

## [Unreleased]
- Two separate list of channels that receive messages from Minecraft and channels that send messages to Discord
- /discord debug (discordmc.admin) for debug information printed in chat
- Replacement tag for channel
- /discord send <channel> <message>
- Strip color codes from chat messages sent to discord
- Always send messages and queue them if getting 429's
- /discord debug sendtest <channel>, sends a test payload
- Catching exception when token is invalid on first startup
- Color codes from Discord will no longer make the message colored
- Using #saveDefaultConfig, recommended to reset the configuration file due to major changes to the format
- Set the game of the Bot to "Minecraft" by default
- Support nicknames for message templates and mentions
- Performance improvements
