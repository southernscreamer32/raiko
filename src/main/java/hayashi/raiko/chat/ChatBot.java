// Copyright 2023 Aidan Lim (southernscreamer32) <aidanlim192@gmail.com>.
//
// This file is part of Raiko.
//
// Raiko is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// Raiko is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with Raiko. If not, see <http://www.gnu.org/licenses/>.

package hayashi.raiko.chat;

import hayashi.raiko.queue.DoubleDealingQueue;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class ChatBot {
    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.MINUTES)
                .readTimeout(10, TimeUnit.MINUTES)
                .writeTimeout(10, TimeUnit.MINUTES)
                .build();
    private static final String preprompt = "You are Raiko Horikawa, fun-loving, free spirited drum tsukumogami. You're having a chat with several humans!";
    private String head;
    private static final int capacity = 10;
    private final DoubleDealingQueue<QueuedChat> chathist = new DoubleDealingQueue<>(capacity);
    private String jsonhead = "";
    private boolean cheap;
    private final String apiKey;
    private final MediaType mediaType = MediaType.parse("application/json");
    public ChatBot(String apik, boolean c) {
        apiKey = apik;
        cheap = c;
        clearHead();
    }

    public String chat(String s, long l) {
        jsonhead += "{\"role\": \"user\", \"content\": \"" + s.replace("\n","\\n").replace("\"", "\\\"") + "\"}";
        if(chathist.size() == capacity)
            chathist.pop();
        chathist.add(new QueuedChat(jsonhead,l));
        try {
            String reply = (new JSONObject(client.newCall(new Request.Builder()
                            .url("https://api.openai.com/v1/chat/completions")
                            .post(RequestBody.create(head + chathist + "]}",mediaType))
                            .addHeader("Authorization", "Bearer " + apiKey)
                            .addHeader("Content-Type", "application/json")
                            .build())
                    .execute().body().string())
                    .getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content"));
            jsonhead = ", {\"role\": \"assistant\", \"content\": \"" + reply.replace("\n","\\n").replace("\"", "\\\"") + "\"}, ";
            return reply;
        } catch (Exception e){
            System.out.println(e);
            clear();
            return "Huh?";
        }
    }
    public void clear(){
        chathist.clear();
    }

    public void remove(int i){
        chathist.remove(capacity - i);
    }

    public void rewind(int i){
        chathist.backskip(i);
    }

    public void toggleModel(){
        cheap = !cheap;
        clearHead();
    }

    public void clearHead(){
        head = "{\"model\": \"" + (cheap ? "gpt-3.5-turbo-1106" : "gpt-4-1106-preview") +
                "\", \"messages\": [{\"role\": \"system\", \"content\": \"" + preprompt + "\"}, ";
    }
}
