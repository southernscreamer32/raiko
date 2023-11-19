/*
 * Copyright 2016-2018 John Grosh (jagrosh) & Kaidan Gustave (TheMonitorLizard)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hayashi.jdautilities.menu;

import java.awt.Color;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import hayashi.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.api.entities.channel.middleman.*;

public class Paginator extends Menu {
    private final BiFunction<Integer, Integer, Color> color;
    private final BiFunction<Integer, Integer, String> text;
    private final int columns, itemsPerPage, pages, bulkSkipNumber;
    private final boolean showPageNumbers, numberItems, wrapPageEnds, waitOnSinglePage, allowTextInput;
    private final List<String> strings;
    private final Consumer<Message> finalAction;
    private final String leftText, rightText;

    public static final Emoji BIG_LEFT = Emoji.fromUnicode("\u23EA");
    public static final Emoji LEFT = Emoji.fromUnicode("\u25C0");
    public static final Emoji STOP = Emoji.fromUnicode("\u23F9");
    public static final Emoji RIGHT = Emoji.fromUnicode("\u25B6");
    public static final Emoji BIG_RIGHT = Emoji.fromUnicode("\u23E9");

    Paginator(EventWaiter waiter, Set<User> users, Set<Role> roles, long timeout, TimeUnit unit,
              BiFunction<Integer, Integer, Color> c, BiFunction<Integer, Integer, String> function,
              Consumer<Message> consumer, int i, int i1, boolean b,
              boolean b1, List<String> items, boolean b2, int i2,
              boolean b3, String lt, String rt, boolean b4) {
        super(waiter, users, roles, timeout, unit);
        color = c;
        text = function;
        columns = i;
        itemsPerPage = i1;
        showPageNumbers = b;
        numberItems = b1;
        strings = items;
        pages = (int) Math.ceil((double) strings.size() / i1);
        finalAction = consumer;
        waitOnSinglePage = b2;
        bulkSkipNumber = i2;
        wrapPageEnds = b3;
        leftText = lt;
        rightText = rt;
        allowTextInput = b4;
    }

    @Override
    public void display(MessageChannel channel) {
        paginate(channel, 1);
    }

    @Override
    public void display(Message message) {
        paginate(message, 1);
    }

    public void paginate(MessageChannel channel, int pageNum) {
        if (pageNum < 1)
            pageNum = 1;
        else if (pageNum > pages)
            pageNum = pages;
        initialize(channel.sendMessage(MessageCreateData.fromEditData(renderPage(pageNum))), pageNum);
    }

    public void paginate(Message message, int pageNum) {
        if (pageNum < 1)
            pageNum = 1;
        else if (pageNum > pages)
            pageNum = pages;
        initialize(message.editMessage(renderPage(pageNum)), pageNum);
    }

    private void initialize(RestAction<Message> action, int pageNum) {
        action.queue(m -> {
            if (pages > 1) {
                if (bulkSkipNumber > 1)
                    m.addReaction(BIG_LEFT).queue();
                m.addReaction(LEFT).queue();
                m.addReaction(STOP).queue();
                if (bulkSkipNumber > 1)
                    m.addReaction(RIGHT).queue();
                m.addReaction(bulkSkipNumber > 1 ? BIG_RIGHT : RIGHT)
                    .queue(v -> pagination(m, pageNum), t -> pagination(m, pageNum));
            } else if (waitOnSinglePage) {
                // Go straight to without text-input because only one page is available
                m.addReaction(STOP).queue(
                    v -> paginationWithoutTextInput(m, pageNum),
                    t -> paginationWithoutTextInput(m, pageNum)
                );
            } else {
                finalAction.accept(m);
            }
        });
    }

    private void pagination(Message message, int pageNum) {
        if (allowTextInput || (leftText != null && rightText != null))
            paginationWithTextInput(message, pageNum);
        else
            paginationWithoutTextInput(message, pageNum);
    }

    private void paginationWithTextInput(Message message, int pageNum) {
        waiter.waitForEvent(GenericMessageEvent.class, event -> {
            if (event instanceof MessageReactionAddEvent e)
                return checkReaction(e, message.getIdLong());
            else if (event instanceof MessageReceivedEvent mre) {
                // Wrong channel
                if (!mre.getChannel().equals(message.getChannel()))
                    return false;
                String rawContent = mre.getMessage().getContentRaw().trim();
                if (rawContent.equalsIgnoreCase(leftText) || rawContent.equalsIgnoreCase(rightText))
                        return isValidUser(mre.getAuthor(), mre.isFromGuild() ? mre.getGuild() : null);

                if (allowTextInput) {
                    try {
                        int i = Integer.parseInt(rawContent);
                        // Minimum 1, Maximum the number of pages, never the current page number
                        if (1 <= i && i <= pages && i != pageNum)
                            return isValidUser(mre.getAuthor(), mre.isFromGuild() ? mre.getGuild() : null);
                    } catch (NumberFormatException ignored) {}
                }
            }
            // Default return false
            return false;
        }, event -> {
            if (event instanceof MessageReactionAddEvent e) {
                handleMessageReactionAddAction(e, message, pageNum);
            } else {
                MessageReceivedEvent mre = (MessageReceivedEvent) event;
                String rawContent = mre.getMessage().getContentRaw().trim();

                final int targetPage = (rawContent.equalsIgnoreCase(leftText) && (1 < pageNum || wrapPageEnds)) ?
                    (pageNum - 1 < 1 && wrapPageEnds ? pages : pageNum - 1) :
                    ((rawContent.equalsIgnoreCase(rightText) && (pageNum < pages || wrapPageEnds)) ?
                        pageNum + 1 > pages && wrapPageEnds ? 1 : pageNum + 1 :
                        Integer.parseInt(rawContent));

                message.editMessage(renderPage(targetPage)).queue(m -> pagination(m, targetPage));
                mre.getMessage().delete().queue(v -> {}, t -> {}); // delete the calling message so it doesn't get spammy
            }
        }, timeout, unit, () -> finalAction.accept(message));
    }

    private void paginationWithoutTextInput(Message message, int pageNum) {
        waiter.waitForEvent(MessageReactionAddEvent.class,
            event -> checkReaction(event, message.getIdLong()), // Check Reaction
            event -> handleMessageReactionAddAction(event, message, pageNum), // Handle Reaction
            timeout, unit, () -> finalAction.accept(message));
    }

    // Private method that checks MessageReactionAddEvents
    private boolean checkReaction(MessageReactionAddEvent event, long messageId) {
        if (event.getMessageIdLong() != messageId)
            return false;
        Emoji name = event.getEmoji();// LEFT, STOP, RIGHT, BIG_LEFT, BIG_RIGHT all fall-through to
// return if the User is valid or not. If none trip, this defaults
// and returns false.
        if (name.equals(LEFT) || name.equals(STOP) || name.equals(RIGHT)) {
            return isValidUser(event.getUser(), event.isFromGuild() ? event.getGuild() : null);
        }
        if (name.equals(BIG_LEFT) || name.equals(BIG_RIGHT)) {
            return bulkSkipNumber > 1 && isValidUser(event.getUser(), event.isFromGuild() ? event.getGuild() : null);
        }
        return false;
    }

    // Private method that handles MessageReactionAddEvents
    // this should also be doable
    private void handleMessageReactionAddAction(MessageReactionAddEvent event, Message message, int pageNum) {
        int newPageNum = pageNum;
        Emoji emoji = event.getReaction().getEmoji();
        if (emoji.equals(LEFT)) {
            if (newPageNum == 1 && wrapPageEnds)
                newPageNum = pages + 1;
            if (newPageNum > 1)
                newPageNum--;
        } else if (emoji.equals(RIGHT)) {
            if (newPageNum == pages && wrapPageEnds)
                newPageNum = 0;
            if (newPageNum < pages)
                newPageNum++;
        } else if (emoji.equals(BIG_LEFT)) {
            if (newPageNum > 1 || wrapPageEnds) {
                for (int i = 1; (newPageNum > 1 || wrapPageEnds) && i < bulkSkipNumber; i++) {
                    if (newPageNum == 1 && wrapPageEnds)
                        newPageNum = pages + 1;
                    newPageNum--;
                }
            }
        } else if (emoji.equals(BIG_RIGHT)) {
            if (newPageNum < pages || wrapPageEnds) {
                for (int i = 1; (newPageNum < pages || wrapPageEnds) && i < bulkSkipNumber; i++) {
                    if (newPageNum == pages && wrapPageEnds)
                        newPageNum = 0;
                    newPageNum++;
                }
            }
        } else if (emoji.equals(STOP)) {
            finalAction.accept(message);
            return;
        }

        try {
            event.getReaction().removeReaction(event.getUser()).queue();
        } catch (PermissionException ignored) {}

        int n = newPageNum;
        message.editMessage(renderPage(newPageNum)).queue(m -> pagination(m, n));
    }

    private MessageEditData renderPage(int pageNum) {
        MessageEditBuilder mbuilder = new MessageEditBuilder();
        EmbedBuilder ebuilder = new EmbedBuilder();
        int start = (pageNum - 1) * itemsPerPage;
        int end = Math.min(strings.size(), pageNum * itemsPerPage);
        if (columns == 1) {
            StringBuilder sbuilder = new StringBuilder();
            for (int i = start; i < end; i++)
                sbuilder.append("\n").append(numberItems ? "`" + (i + 1) + ".` " : "").append(strings.get(i));
            ebuilder.setDescription(sbuilder.toString());
        } else {
            int per = (int) Math.ceil((double) (end - start) / columns);
            for (int k = 0; k < columns; k++) {
                StringBuilder strbuilder = new StringBuilder();
                for (int i = start + k * per; i < end && i < start + (k + 1) * per; i++)
                    strbuilder.append("\n").append(numberItems ? (i + 1) + ". " : "").append(strings.get(i));
                ebuilder.addField("", strbuilder.toString(), true);
            }
        }

        ebuilder.setColor(color.apply(pageNum, pages));
        if (showPageNumbers)
            ebuilder.setFooter("Page " + pageNum + "/" + pages, null);
        mbuilder.setEmbeds(ebuilder.build());
        if (text != null)
            mbuilder.setContent(text.apply(pageNum, pages));
        return mbuilder.build();
    }

    public static class Builder extends Menu.Builder<Builder, Paginator> {
        private BiFunction<Integer, Integer, Color> color = (page, pages) -> null;
        private BiFunction<Integer, Integer, String> text = (page, pages) -> null;
        private Consumer<Message> finalAction = m -> m.delete().queue();
        private int columns = 1;
        private int itemsPerPage = 12;
        private boolean showPageNumbers = true;
        private boolean numberItems,waitOnSinglePage, wrapPageEnds, allowTextInput;
        private int bulkSkipNumber = 1;
        private String textToLeft, textToRight;

        private final List<String> strings = new LinkedList<>();

        @Override
        public Paginator build() {
            Checks.check(waiter != null, "Must set an EventWaiter");
            Checks.check(!strings.isEmpty(), "Must include at least one item to paginate");

            return new Paginator(waiter, users, roles, timeout, unit, color, text, finalAction,
                columns, itemsPerPage, showPageNumbers, numberItems, strings, waitOnSinglePage,
                bulkSkipNumber, wrapPageEnds, textToLeft, textToRight, allowTextInput);
        }

        public Builder setColor(Color c) {
            color = (i0, i1) -> c;
            return this;
        }

        public Builder setColor(BiFunction<Integer, Integer, Color> colorBiFunction) {
            color = colorBiFunction;
            return this;
        }

        public Builder setText(String t) {
            text = (i0, i1) -> t;
            return this;
        }

        public Builder setText(BiFunction<Integer, Integer, String> textBiFunction) {
            text = textBiFunction;
            return this;
        }

        public Builder setFinalAction(Consumer<Message> consumer) {
            finalAction = consumer;
            return this;
        }

        public Builder setColumns(int i) {
            if (i < 1 || i > 3)
                throw new IllegalArgumentException("Only 1, 2, or 3 columns are supported");
            columns = i;
            return this;
        }

        public Builder setItemsPerPage(int num) {
            if (num < 1)
                throw new IllegalArgumentException("There must be at least one item per page");
            itemsPerPage = num;
            return this;
        }

        public Builder showPageNumbers(boolean show) {
            showPageNumbers = show;
            return this;
        }

        public Builder useNumberedItems(boolean number) {
            numberItems = number;
            return this;
        }

        public Builder waitOnSinglePage(boolean wait) {
            waitOnSinglePage = wait;
            return this;
        }

        public Builder clearItems() {
            strings.clear();
            return this;
        }

        public Builder addItems(String... items) {
            strings.addAll(Arrays.asList(items));
            return this;
        }

        public Builder setItems(String... items) {
            strings.clear();
            strings.addAll(Arrays.asList(items));
            return this;
        }

        public Builder setBulkSkipNumber(int number) {
            bulkSkipNumber = Math.max(number, 1);
            return this;
        }

        public Builder wrapPageEnds(boolean b) {
            wrapPageEnds = b;
            return this;
        }

        public Builder allowTextInput(boolean b) {
            allowTextInput = b;
            return this;
        }

        public Builder setLeftRightText(String left, String right) {
            if (left == null || right == null) {
                textToLeft = null;
                textToRight = null;
            } else {
                textToLeft = left;
                textToRight = right;
            }
            return this;
        }
    }
}