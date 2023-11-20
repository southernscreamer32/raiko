# Raiko

[![Release](https://img.shields.io/github/release/a9lim/raiko.svg)](https://github.com/a9lim/raiko/releases/latest)
[![License](https://img.shields.io/github/license/a9lim/raiko.svg)](https://github.com/a9lim/raiko/blob/master/LICENSE)

Discord music + chatbot forked from [jagrosh's JMusicBot](https://github.com/jagrosh/MusicBot)

## Features
* (Relatively) easy to use 
* Supports all sources supported by [LavaPlayer](https://github.com/lavalink-devs/lavaplayer)
* OpenAI chatbot integrated with configurable prompt
* Improved performance

## Setup
1. [Install Java 21](https://www.oracle.com/java/technologies/downloads/#java21)
2. Download the .jar file in the [latest release](https://github.com/a9lim/Raiko/releases)
3. Get a [Discord bot token](https://github.com/jagrosh/MusicBot/wiki/Getting-a-Bot-Token),
your [Discord user ID](https://github.com/jagrosh/MusicBot/wiki/Finding-Your-User-ID),
and an [OpenAI API key](https://help.openai.com/en/articles/4936850-where-do-i-find-my-api-key)
4. Save a copy of [this file](https://github.com/a9lim/Raiko/blob/main/src/main/resources/reference.conf) as 'config.txt' in the same folder as the .jar file
5. Run the .jar file with `java -jar /path/to/raiko.jar`

## Main Changes
* Migrated to Java 21
* Reworked to use [JDA 5.0.0](https://github.com/discord-jda/JDA), [LavaPlayer 2.0.3](https://github.com/lavalink-devs/lavaplayer), and several other updated libraries
* [JDA-Utilities](https://github.com/JDA-Applications/JDA-Utilities) forked and directly incorporated into project
* ChatGPT-powered chatbot integrated 
* General improvements to code efficiency