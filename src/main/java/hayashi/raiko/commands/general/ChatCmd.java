package hayashi.raiko.commands.general;
import hayashi.jdautilities.command.Command;
import hayashi.jdautilities.command.CommandEvent;
import hayashi.raiko.Bot;
import hayashi.raiko.ChatBot;
import okhttp3.*;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

public class ChatCmd extends Command {

    private final ChatBot chatBot;

    public ChatCmd(ChatBot chatBot, Bot bot) {
        this.chatBot = chatBot;
        this.name = "chat";
        this.help = "talk to raiko";
        this.arguments = "<text>";
        this.aliases = bot.getConfig().getAliases(this.name);
    }
    @Override
    protected void execute(CommandEvent event) {
        event.reply(chatBot.chat(event.getArgs()));
    }
}
