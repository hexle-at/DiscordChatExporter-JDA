/*
 * DiscordChatExporter-JDA
 * Copyright (C) Hexle development team and contributors.
 *
 * This program is free software and is free to redistribute
 * and/or modify under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is intended for the purpose of joy,
 * WITHOUT WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * INFORMATION:
 * If you need to update this class - just copy and replace everything in here!!!
 */
package hexle.at.api.chatexport;

import net.dv8tion.jda.api.entities.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Hexle
 * @version 1.0.0
 */
public class ChatExporter {

    private String version = "1.0.0";
    private BufferedWriter bufferedWriter;
    private TextChannel textChannel;
    private final int maxExportMessages = 2000;
    //Set the max amount of messages for one export; Theoretically you can set it to whatever you want
    //If there are more messages requested to export, only as much as the limit says will be exported.

    /**
     * This function exports a discord TextChannel, you just need to pass the parameters
     *
     * @param textChannel    The channel that should be exported
     * @param message        The reference message -> Most likely the command that the user entered. e.g: `!export 100` this message will be provided to this function
     * @param count          The amount of messages that should be exported. ^^maxExportMessages^^ if 0 then it will be set to 1
     * @param exportFileName The file name when the file is exported. Leave empty or null if you want to use the provided "Name-System": CHANNEL-NAME_DD_MM_YYYY_HH_MM_SS.html
     */
    public File exportChat(TextChannel textChannel, Message message, int count, String exportFileName) {
        this.textChannel = textChannel;
        if (count > maxExportMessages) {
            count = maxExportMessages;
        } else if (count < 1) {
            count = 1;
        }

        String fileName = textChannel.getName() + "_" + parseDateTime(message.getTimeCreated()).replaceAll(":", "_") + "_" + message.getTimeCreated().getSecond() + ".html";
        if (exportFileName != null && !exportFileName.equals("")) fileName = exportFileName;

        File file = new File(fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileWriter fileWriter = new FileWriter(file, true);
            bufferedWriter = new BufferedWriter(fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }

        printHtmlStart(textChannel.getName());
        long lastAuthorID = 0L;
        boolean reactionsSet, dayLine = false;
        String lastDate = "";
        List<Message> messageList = get(textChannel, count);
        Collections.reverse(messageList);

        for (Message message1 : messageList) {
            reactionsSet = false;
            boolean sameAuthor;
            boolean done = false;
            String sendDate = parseDate(message1.getTimeCreated());
            if (!lastDate.equalsIgnoreCase(sendDate) && lastAuthorID != 0L) {
                printEndMessage();
                printDayLine(sendDate);
                dayLine = true;
            }
            lastDate = sendDate;
            if (lastAuthorID == message1.getAuthor().getIdLong()) {
                sameAuthor = true;
            } else {
                lastAuthorID = message1.getAuthor().getIdLong();
                sameAuthor = false;
            }
            if (dayLine) {
                sameAuthor = false;
                dayLine = false;
            }

            String editDateTime = "";
            String avatarUrl = message1.getAuthor().getAvatarUrl();
            if (avatarUrl == null || avatarUrl.equals("")) {
                avatarUrl = "https://hexle.at/development/hexle/wikiimages/dc_no_avatar.png";
            }
            String name = message1.getAuthor().getName();
            if (message1.isWebhookMessage()) {
                name += "  <span class=\"badgeBot\">WEBHOOK</span>";
            } else if (message1.getAuthor().isBot()) {
                name += "  <span class=\"badgeBot\">BOT</span>";
            } else if (message1.getAuthor().isSystem()) { //N/A in JDA lower (inkl): 4.2
                name += " <span class=\"badgeBot\">SYSTEM</span>"; //N/A in JDA lower (inkl): 4.2
            } //N/A in JDA lower (inkl): 4.2
            if (message1.isEdited()) editDateTime = " (edited: " + parseDateTime(message1.getTimeEdited()) + ") ";
            String sendTime = parseTime(message1.getTimeCreated());
            List<Message.Attachment> attachments = message1.getAttachments();

            for (MessageEmbed embed : message1.getEmbeds()) {
                done = true;
                List<MessageReaction> reactionList1 = new ArrayList<>();
                if (!reactionsSet) {
                    reactionList1 = message1.getReactions();
                    reactionsSet = true;
                }
                String des = (embed.getDescription() == null) ? "" : mentionUser(styleText(embed.getDescription().replaceAll("<", "&lt;").replaceAll(">", "&gt;")));
                String auth = (embed.getAuthor() == null) ? "" : embed.getAuthor().getName();
                auth = (auth.equals("") && embed.getSiteProvider() != null) ? embed.getSiteProvider().getName() : "";
                String authUrl = (embed.getAuthor() == null) ? "" : embed.getAuthor().getUrl();
                String col = "0, 0, 0";
                try {
                    col = embed.getColor().getRed() + "," + embed.getColor().getGreen() + "," + embed.getColor().getBlue();
                } catch (Exception e) {
                    //THIS EXCEPTION CAN BE IGNORED
                }
                String title = (embed.getTitle() == null) ? "" : styleText(embed.getTitle()); //.replaceAll("<", "&lt;").replaceAll(">", "&gt;")
                String titleUrl = (embed.getUrl() == null) ? "" : embed.getUrl();
                String thumb = (embed.getThumbnail() == null) ? "" : embed.getThumbnail().getUrl();
                String img = (embed.getImage() == null) ? "" : embed.getImage().getUrl();
                String time = (parseDateTime(embed.getTimestamp()) == null) ? "" : parseDateTime(embed.getTimestamp());
                String footer = (embed.getFooter() == null) ? "" : styleText(embed.getFooter().getText().replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
                String footericon = (embed.getFooter() == null) ? "" : embed.getFooter().getIconUrl();

                if(embed.getType() == EmbedType.IMAGE){
                    String sendImage = (!img.startsWith("http")) ? thumb : "https://hexle.at/development/hexle/wikiimages/dc_no_avatar.png";
                    if(!sendImage.startsWith("http")) sendImage = "https://hexle.at/development/hexle/wikiimages/dc_no_avatar.png";
                    printStartImage(sameAuthor, sendTime, avatarUrl, name, sendTime, editDateTime, sendImage, reactionList1);
                }else {
                    printEmbed(sameAuthor, sendTime, avatarUrl, name, sendTime, editDateTime, reactionList1, auth, authUrl, des, col, title, titleUrl, thumb, img, time, footer, footericon, embed.getFields());
                }
            }

            if (done) {
                //LEAVE THIS EMPTY
            } else if (message1.getContentRaw() != null && !message1.getContentRaw().equals("") && message1.getEmbeds().isEmpty()) {
                String text = styleText(message1.getContentRaw()); //.replaceAll("<", "&lt;").replaceAll(">", "&gt;") TODO
                text = mentionUser(text);
                printStartMessage(sameAuthor, sendTime, text, avatarUrl, name, sendDate, editDateTime, message1.getReactions());
                reactionsSet = true;
            }
            //File
            if (!attachments.isEmpty()) {
                for (Message.Attachment attachment : attachments) {
                    List<MessageReaction> reactionList = new ArrayList<>();
                    if (!reactionsSet) {
                        reactionList = message1.getReactions();
                        reactionsSet = true;
                    }
                    if (attachment.isImage()) {
                        printStartImage(sameAuthor, sendTime, avatarUrl, name, sendDate, editDateTime, attachment.getProxyUrl(), reactionList);
                    } else {
                        printStartFile(sameAuthor, sendTime, avatarUrl, name, sendDate, editDateTime, attachment.getFileName(), attachment.getProxyUrl(), attachment.getSize(), reactionList);
                    }
                }
            }
        }
        printEndMessage();
        printEndFile();
        if (bufferedWriter != null) {
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    private void printEmbed(boolean append, String sendTime, String avatarUrl, String name, String created, String edited, List<MessageReaction> reactionList, String authornam, String authorUrl, String description, String color, String titlee, String titelUrl, String thumbnail, String image, String timestamp, String footer, String footericon, List<MessageEmbed.Field> fields) {
        String reactions = parseReactions(reactionList);
        String thumbnail1 = "", image1 = "", footericon1 = "", footerString = "", fieldString;
        String title = titlee;
        title = (titelUrl != "") ? "<a href=\""+titelUrl+"\" class=\"noDecoration\">"+titlee+"</a>" : titlee;
        String authorname = (authorUrl != null && !authorUrl.equals("")) ? "<a href=\""+authorUrl+"\" class=\"noDecoration\">"+authornam+"</a>" : authornam;
        if (thumbnail != null && !thumbnail.equals("")) thumbnail1 = "<img src=\"" + thumbnail + "\" >";
        if (footericon != null && !footericon.equals("")) footericon1 = "<img src=\"" + footericon + "\" >";
        if (image != null && !image.equals("")) image1 = "<img src=\"" + image + "\" >";

        if (!footer.equals("") || !footerString.equals("") || !Objects.equals(footericon, "")) {
            footerString = "                                        <div class=\"embedFooter\">\n" +
                    "                                            <div class=\"footerImg\">" + footericon1 + "</div>\n" +
                    "                                            <div class=\"footerText\">" + footer + "</div>\n" +
                    "                                            <div class=\"footerTime\">" + timestamp + "</div>\n" +
                    "                                        </div>\n";
        }

        StringBuilder fieldStringBuilder = new StringBuilder("<div class=\"embedFields\">\n");
        for (MessageEmbed.Field field : fields) {
            if (field.isInline()) {
                fieldStringBuilder.append("<div class=\"field inline\">\n");
            } else {
                fieldStringBuilder.append("<div class=\"field noinline\">\n");
            }
            fieldStringBuilder.append("<div class=\"fieldTitle\">").append(field.getName()).append("</div>\n").append("<div class=\"fieldContent\">").append(field.getValue()).append("</div>\n");
            fieldStringBuilder.append("</div>\n");
        }
        fieldString = fieldStringBuilder.toString();
        fieldString += "</div>\n";

        if (append) {
            writeFile("<tr>\n" +
                    "                            <td class=\"messageTime\">" + sendTime + "</td>\n" +
                    "                            <td class=\"embed\">\n" +
                    "                                <div class=\"messageEmbed\">\n" +
                    "                                    <div class=\"embedColorBar\" style=\"background-color: rgb(" + color + ")\"></div>\n" +
                    "                                    <div class=\"embedContent\">\n" +
                    "                                        <div class=\"embedThumbnail\">" + thumbnail1 + "</div>\n" +
                    "                                        <div class=\"embedAuthor\">" + authorname + "</div>\n" +
                    "                                        <div class=\"embedTitle\">" + title + "</div>\n" +
                    "                                        <div class=\"embedDescription\">\n" +
                    "                                            " + description + "\n" +
                    "                                        </div>\n " + fieldString +

                    "                                        <div class=\"embedImg\">" + image1 + "</div>\n" + footerString +
                    "                                    </div>\n" +
                    "                                </div>\n" +
                    "                                <div class=\"messageEditedDate\">" + edited + "</div> " + reactions + "</td>\n" +
                    "                            </td>\n" +
                    "                        </tr>");

        } else {
            writeFile("<div class=\"message\">\n" +
                    "                    <table>\n" +
                    "                        <tr>\n" +
                    "                            <th class=\"messageAvatarImg\"><img src=\"" + avatarUrl + "\"></th>\n" +
                    "                            <th class=\"messageInfo\">\n" +
                    "                                <div class=\"messageAuthorName\">" + name + "</div>\n" +
                    "                                <div class=\"messageCreateDate\">" + created + "</div>\n" +
                    "                            </th>\n" +
                    "                        </tr>\n" +
                    "                            <tr>\n" +
                    "                            <td class=\"messageTime\">" + sendTime + "</td>\n" +
                    "                            <td class=\"embed\">\n" +
                    "                                <div class=\"messageEmbed\">\n" +
                    "                                    <div class=\"embedColorBar\" style=\"background-color: rgb(" + color + ")\"></div>\n" +
                    "                                    <div class=\"embedContent\">\n" +
                    "                                        <div class=\"embedThumbnail\">" + thumbnail1 + "</div>\n" +
                    "                                        <div class=\"embedAuthor\">" + authorname + "</div>\n" +
                    "                                        <div class=\"embedTitle\">" + title + "</div>\n" +
                    "                                        <div class=\"embedDescription\">\n" +
                    "                                            " + description + "\n" +
                    "                                        </div>\n " + fieldString +
                    "                                        <div class=\"embedImg\">" + image1 + "</div>\n" + footerString +

                    "                                    </div>\n" +
                    "                                </div>\n" +
                    "                                <div class=\"messageEditedDate\">" + edited + "</div> " + reactions + "</td>\n" +
                    "                            </td>\n" +
                    "                        </tr>");

        }
    }

    private void printDayLine(String date) {
        writeFile("         <div class=\"dayLine\">\n" +
                "                    <hr class=\"hrDayLine\"> <span class=\"dayLineDate\">" + date + "</span> <hr class=\"hrDayLine\">\n" +
                "                </div>");
    }

    private void printStartMessage(boolean append, String sendTime, String text, String avatarUrl, String name, String created, String edited, List<MessageReaction> reactionList) {
        String reactions = parseReactions(reactionList);
        if (append) {
            writeFile("<tr>\n" +
                    "                            <td class=\"messageTime\">" + sendTime + "</td>\n" +
                    "                            <td class=\"messageText\">" + text + "" + "<div class=\"messageEditedDate\">" + edited + "</div>" + reactions + "</td>" +
                    "                        </tr>");
        } else {
            writeFile("<div class=\"message\">\n" +
                    "                    <table>\n" +
                    "                        <tr>\n" +
                    "                            <th class=\"messageAvatarImg\"><img src=\"" + avatarUrl + "\"></th>\n" +
                    "                            <th class=\"messageInfo\">\n" +
                    "                                <div class=\"messageAuthorName\">" + name + "</div>\n" +
                    "                                <div class=\"messageCreateDate\">" + created + "</div>\n" +
                    "                            </th>\n" +
                    "                        </tr>\n" +
                    "                        <tr>\n" +
                    "                            <td class=\"messageTime\">" + sendTime + "</td>\n" +
                    "                            <td class=\"messageText\">" + text + " " + "<div class=\"messageEditedDate\">" + edited + "</div> " + reactions + "</td>\n" +
                    "                        </tr>");
        }
    }

    private void printEndMessage() {
        writeFile("</table>\n" +
                "                </div>");
    }

    private void printStartImage(boolean append, String sendTime, String avatarUrl, String name, String created, String edited, String fileUrl, List<MessageReaction> reactionList) {
        String reactions = parseReactions(reactionList);
        fileUrl = fileUrl.replace("discordapp.net", "discordapp.com").replace("media.", "cdn.");

        if (append) {
            writeFile("<tr>\n" +
                    "                         <td class=\"messageTime\">" + sendTime + "</td>\n" +
                    "                            <td class=\"messageImage\">\n" +
                    "                                <img src=\"" + fileUrl + "\">\n" + reactions +
                    "                            </td>\n" +
                    "                        </tr>");
        } else {
            writeFile("<div class=\"message\">\n" +
                    "                    <table>\n" +
                    "                        <tr>\n" +
                    "                            <th class=\"messageAvatarImg\"><img src=\"" + avatarUrl + "\"></th>\n" +
                    "                            <th class=\"messageInfo\">\n" +
                    "                                <div class=\"messageAuthorName\">" + name + "</div>\n" +
                    "                                <div class=\"messageCreateDate\">" + created + "</div>\n" +
                    "                                <div class=\"messageEditedDate\">" + edited + "</div>\n" +
                    "                            </th>\n" +
                    "                        </tr>\n" +
                    "                        <tr>\n" +
                    "                         <td class=\"messageTime\">" + sendTime + "</td>\n" +
                    "                            <td class=\"messageImage\">\n" +
                    "                                <img src=\"" + fileUrl + "\">\n" + reactions +
                    "                            </td>\n" +
                    "                        </tr>");
        }


    }

    private void printStartFile(boolean append, String sendTime, String avatarUrl, String name, String created, String edited, String filename, String fileUrl, int filesize, List<MessageReaction> reactionList) {
        String reactions = "";
        if (!reactionList.isEmpty()) {
            reactions += "<div class=\"messageReactions\">\n";
            StringBuilder reactionsBuilder = new StringBuilder(reactions);
            for (MessageReaction reaction : reactionList) {
                reactionsBuilder.append("<div class=\"messageReaction\">").append(reaction.getReactionEmote().getEmoji()).append(" ").append(reaction.getCount()).append("</div>\n");
            }
            reactions = reactionsBuilder.toString();
            reactions += "</div>";
        }

        String size = "";
        if (filesize >= 1000 && filesize < 1000000) {
            size = filesize / 1000 + " KB";
        } else if (filesize >= 1000000 && filesize < 1000000000) {
            size = filesize / 1000000000 + " MB";
        } else if (filesize < 1000) {
            size = filesize + " B";
        }
        fileUrl = fileUrl.replace("discordapp.net", "discordapp.com").replace("media.", "cdn.");
        if (append) {
            writeFile("<tr>\n" +
                    "                         <td class=\"messageTime\">" + sendTime + "</td>\n" +
                    "                            <td class=\"messageFileBox\">\n" +
                    "                                <div class=\"messageFile\">\n" +
                    "                                    <div class=\"fileEmoji\">\uD83D\uDCC4</div>\n" +
                    "                                    <div class=\"fileInfo\">\n" +
                    "                                        <div class=\"fileText\">" + filename + "</div>\n" +
                    "                                        <div class=\"fileSize\">" + size + "</div>\n" +
                    "                                    </div>\n" +
                    "                                    \n" +
                    "                                    <div class=\"fileDownload\"><a href=\"" + fileUrl + "\" download><span class=\"hovertext\" data-hover=\"Download\">⬇️</span></a></div>\n" +
                    "                                </div>\n" + " <div class=\"messageEditedDate\">" + edited + "</div> " + reactions +
                    "                            </td>\n" +
                    "                        </tr>");
        } else {
            writeFile("<div class=\"message\">\n" +
                    "                    <table>\n" +
                    "                        <tr>\n" +
                    "                            <th class=\"messageAvatarImg\"><img src=\"" + avatarUrl + "\"></th>\n" +
                    "                            <th class=\"messageInfo\">\n" +
                    "                                <div class=\"messageAuthorName\">" + name + "</div>\n" +
                    "                                <div class=\"messageCreateDate\">" + created + "</div>\n" +
                    "                            </th>\n" +
                    "                        </tr>\n" +
                    "                        <tr>\n" +
                    "                         <td class=\"messageTime\">" + sendTime + "</td>\n" +
                    "                            <td class=\"messageFileBox\">\n" +
                    "                                <div class=\"messageFile\">\n" +
                    "                                    <div class=\"fileEmoji\">\uD83D\uDCC4</div>\n" +
                    "                                    <div class=\"fileInfo\">\n" +
                    "                                        <div class=\"fileText\">" + filename + "</div>\n" +
                    "                                        <div class=\"fileSize\">" + size + "</div>\n" +
                    "                                    </div>\n" +
                    "                                    \n" +
                    "                                    <div class=\"fileDownload\"><a href=\"" + fileUrl + "\" download><span class=\"hovertext\" data-hover=\"Download\">⬇️</span></a></div>\n" +
                    "                                </div>\n" + " <div class=\"messageEditedDate\">" + edited + "</div> " + reactions +
                    "                            </td>\n" +
                    "                        </tr>");
        }
    }

    //https://ticket-log.utopia-gaming.de/ticket/login.php
    private void printEndFile() {
        writeFile("         <div class=\"sendBox\">\n" + //PLEASE DO NOT REMOVE THIS LINE, WE REALLY APPRECIATE IT! :)
                "                    <div class=\"sendBoxText\">Channel-Exporter by Hexle: <a href=\"https://github.com/hexle-at/DiscordChatExporter-JDA\" target=\"_blank\">DiscordChatExporter-JDA</a></div>\n" +//PLEASE DO NOT REMOVE THIS LINE, WE REALLY APPRECIATE IT! :)
                "                </div>" +//PLEASE DO NOT REMOVE THIS LINE, WE REALLY APPRECIATE IT! :)
                "                <br>\n" +
                "                <div class=\"sendBox\">\n" +
                "                    <div class=\"sendBoxText\">Report an issue: <a href=\"https://github.com/hexle-at/DiscordChatExporter-JDA/issues\" target=\"_blank\">GitHub DiscordChatExporter</a></div>\n" +
                "                </div>" +
                "           </div>\n" +
                "        </div>\n" +
                "    </body>\n" +
                "</html>" +
                "<script>\n" +
                "\n" +
                "document.getElementById(\"latestStyle\").onclick = function() {latestStyle()};\n" +
                "\n" +
                "function latestStyle() {\n" +
                "document.getElementById(\"latestStyle\").innerHTML = \"<p class='styleButtonText'> Reload to use the old CSS </p>\";\n" +
                "\n" +
                "var e = document.getElementsByTagName('style')[0];\n" +
                "\n" +
                "var d = document.createElement('unuseable');\n" +
                "d.innerHTML = e.innerHTML;\n" +
                "e.parentNode.replaceChild(d, e);\n" +
                "document.getElementsByTagName(\"head\")[0].insertAdjacentHTML(\"beforeend\", \"<link rel=\\\"stylesheet\\\"href=\\\"https://hexle.at/api/discord_chat_exporter/latestStyle.css\\\" />\");\n" +
                "\n" +
                "  var links = document.getElementsByTagName(\"link\");\n" +
                "for (var cl in links)\n" +
                "{\n" +
                "var link = links[cl];\n" +
                "if (link.rel === \"stylesheet\")\n" +
                "link.href += \"\";\n" +
                "}\n" +
                "}\n" +
                "</script>");
    }

    private String mentionUser(String string) {
        int errorCount = 0;
        while (string.contains("<@!")) {
            String[] parts = string.split("<@!", 2);
            String[] parts1 = parts[1].split(">", 2);
            try {
                long userId = Long.parseLong(parts1[0].replaceAll("&", ""));
                User user = textChannel.getJDA().getUserById(userId);
                String userName = "N/A";
                if (user != null) {
                    userName = textChannel.getJDA().getUserById(userId).getName();
                }
                string = parts[0] + " <span class=\"mentionUser\">@" + userName + "</span> " + parts1[1];
                errorCount = 0;
            } catch (Exception e) {
                e.printStackTrace();
                if (errorCount > 0) {
                    string.replaceFirst("<@!", "");
                }
                errorCount++;
            }
        }
        errorCount = 0;
        while (string.contains("<@")) {
            String[] parts = string.split("<@", 2);
            String[] parts1 = parts[1].split(">", 2);
            try {
                long userId = Long.parseLong(parts1[0].replaceAll("&", ""));
                User user = textChannel.getJDA().getUserById(userId);
                String userName = "N/A";
                if (user != null) {
                    userName = textChannel.getJDA().getUserById(userId).getName();
                }
                string = parts[0] + " <span class=\"mentionUser\">@" + userName + "</span> " + parts1[1];
                errorCount = 0;
            } catch (Exception e) {
                e.printStackTrace();
                if (errorCount > 0) {
                    string.replaceFirst("<@", "");
                }
                errorCount++;
            }
        }
        return string;
    }

    private String styleText(String text) {
        String formattedText = text;
        boolean start = true;
        while (formattedText.contains("**")) { //Bold
            if (start) {
                formattedText = formattedText.replaceFirst("\\*\\*", "<b>");
                start = false;
            } else {
                formattedText = formattedText.replaceFirst("\\*\\*", "</b>");
                start = true;
            }
        }
        start = true;
        while (formattedText.contains("__")) { //Underlined
            if (start) {
                formattedText = formattedText.replaceFirst("__", "<u>");
                start = false;
            } else {
                formattedText = formattedText.replaceFirst("__", "</u>");
                start = true;
            }
        }
        start = true;
        while ((start && formattedText.contains("_")) || (!start && formattedText.contains("_ "))) { //Italic
            if (start) {
                formattedText = formattedText.replaceFirst("_", "<i>");
                start = false;
            } else {
                formattedText = formattedText.replaceFirst("_ ", "</i>");
                start = true;
            }
        }
        start = true;
        while (formattedText.contains("||")) { //Spoiler
            if (start) {
                formattedText = formattedText.replaceFirst("\\|\\|", "<span class=\"spoilerText\">");
                start = false;
            } else {
                formattedText = formattedText.replaceFirst("\\|\\|", "</span>");
                start = true;
            }
        }
        start = true;
        while (formattedText.contains("~~")) { //Strike
            if (start) {
                formattedText = formattedText.replaceFirst("~~", "<s>");
                start = false;
            } else {
                formattedText = formattedText.replaceFirst("~~", "</s>");
                start = true;
            }
        }
        start = true;
        while (formattedText.contains("```")) { //Code
            if (start) {
                formattedText = formattedText.replaceFirst("```", "<span class=\"codeText\">");
                start = false;
            } else {
                formattedText = formattedText.replaceFirst("```", "</span>");
                start = true;
            }
        }
        start = true;
        while (formattedText.contains("``")) { //Code
            if (start) {
                formattedText = formattedText.replaceFirst("``", "<span class=\"codeText\">");
                start = false;
            } else {
                formattedText = formattedText.replaceFirst("``", "</span>");
                start = true;
            }
        }
        start = true;
        while (formattedText.contains("`")) { //Code
            if (start) {
                formattedText = formattedText.replaceFirst("`", "<span class=\"codeText\">");
                start = false;
            } else {
                formattedText = formattedText.replaceFirst("`", "</span>");
                start = true;
            }
        }
        return formattedText;
    }

    private String parseReactions(List<MessageReaction> reactionList) {
        String reactions = "";
        if (!reactionList.isEmpty()) {
            reactions += "<div class=\"messageReactions\">\n";
            StringBuilder reactionsBuilder = new StringBuilder(reactions);
            for (MessageReaction reaction : reactionList) {
                reactionsBuilder.append("<div class=\"messageReaction\">").append(reaction.getReactionEmote().getEmoji()).append(" ").append(reaction.getCount()).append("</div>\n");
            }
            reactions = reactionsBuilder.toString();
            reactions += "</div>";
        }
        return reactions;
    }

    private void writeFile(String string) {
        try {
            bufferedWriter.write(string);
            bufferedWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printHtmlStart(String channelName) {
        writeFile("<!DOCTYPE html>\n" +
                "<html>\n" +
                "    <head>\n" +
                "        <meta charset=\"UTF-8\">\n" +
                "        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "    \n" +
                "    </head>\n" +
                "    <style>\n" +
                "        :root{\n" +
                "            --colBackground:  #36393f;\n" +
                "            --colDate: #72767d;\n" +
                "            --colDayLine: #71757c;\n" +
                "            --colScrollbar: #2e3338;\n" +
                "            --colScrollbarPart: #202225;\n" +
                "            --colAuthorName: #fff;\n" +
                "            --colNormalText: #dcddde;\n" +
                "            --colSpoiler: #202225;\n" +
                "            --colMentionUser: #404675;\n" +
                "            --colFileBack: #2f3136;\n" +
                "            --colFileText: #2a83e8;\n" +
                "            --colFileBorder: #292b2f;\n" +
                "            --colHrLine: #72767d;\n" +
                "            --colChannelHeaderBorder: #2a2c32;\n" +
                "            --colEmbedBackground: #2f3136;\n" +
                "            --colBotBadge: #5865f2;\n" +
                "            --fontPrimary: Whitney,\"Helvetica Neue\",Helvetica,Arial,sans-serif;\n" +
                "        }\n" +
                "\n" +
                "        th {\n" +
                "            text-align: left;\n" +
                "        }\n" +
                "\n" +
                "        body {\n" +
                "            background-color:  var(--colBackground);\n" +
                "            font-family: var(--fontPrimary);\n" +
                "        }\n" +
                "\n" +
                "        ::-webkit-scrollbar {\n" +
                "            width: 10px;\n" +
                "        }\n" +
                "        ::-webkit-scrollbar-track {\n" +
                "            background: var(--colScrollbar); \n" +
                "        }\n" +
                "        ::-webkit-scrollbar-thumb {\n" +
                "            background: var(--colScrollbarPart);\n" +
                "            border-radius: 10px\n" +
                "        }\n" +
                "\n" +
                "        .messageAvatarImg img {\n" +
                "            width: 40px;\n" +
                "            height: 40px;\n" +
                "            border-radius: 50%;\n" +
                "        }\n" +
                "\n" +
                "        .messageAuthorName {\n" +
                "            display: inline-block;\n" +
                "            font-size: 1rem;\n" +
                "            font-weight: 500;\n" +
                "            line-height: 1.375rem;\n" +
                "            color: var(--colAuthorName);\n" +
                "            padding-left: 5px;\n" +
                "        }\n" +
                "        .messageCreateDate {\n" +
                "            display: inline-block;\n" +
                "            font-size: 0.75rem;\n" +
                "            line-height: 1.375rem;\n" +
                "            color: var(--colDate);\n" +
                "        }\n" +
                "        .messageEditedDate {\n" +
                "            display: inline-block;\n" +
                "            font-size: 0.75rem;\n" +
                "            line-height: 1.375rem;\n" +
                "            color: var(--colDate);\n" +
                "        }\n" +
                "\n" +
                "        .messageText {\n" +
                "            color: var(--colNormalText);\n" +
                "            white-space: pre-wrap;\n" +
                "        }\n" +
                "        .messageTime {\n" +
                "            color: var(--colDate);\n" +
                "            font-size: 0.65rem;\n" +
                "            line-height: 1.375rem;\n" +
                "            vertical-align: top;\n" +
                "        }\n" +
                "        .mentionUser {\n" +
                "            background-color: var(--colMentionUser);\n" +
                "            padding: 2px;\n" +
                "            border-radius: 2px;\n" +
                "        }\n" +
                "        .mentionUser:hover {\n" +
                "            background-color: var(--colMentionUser);\n" +
                "            opacity: 0.9;\n" +
                "        }\n" +
                "        .spoilerText {\n" +
                "            background-color: var(--colSpoiler);\n" +
                "            color: var(--colSpoiler);\n" +
                "            padding: 2px;\n" +
                "            border-radius: 2px;\n" +
                "        }\n" +
                "        .spoilerText:hover {\n" +
                "            color: var(--colNormalText);\n" +
                "        }\n" +
                "        .codeText {\n" +
                "            background-color: var(--colEmbedBackground);\n" +
                "            opacity: 0.9;\n" +
                "            border-radius: 5px;\n" +
                "            padding: 3px;\n" +
                "        }\n" +
                "        .messageFile {\n" +
                "            background-color: var(--colFileBack);\n" +
                "            border-radius: 1px;\n" +
                "            border-color: var(--colFileBorder);\n" +
                "            width: fit-content;\n" +
                "            padding-right: 6px;\n" +
                "        }\n" +
                "        .fileEmoji {\n" +
                "            font-size: 30px;\n" +
                "            display: table-cell;\n" +
                "        }\n" +
                "        .fileText {\n" +
                "            color: var(--colFileText);\n" +
                "        }\n" +
                "        .fileInfo {\n" +
                "            display: table-cell;\n" +
                "            vertical-align: top;\n" +
                "            padding-left: 5px;\n" +
                "        }\n" +
                "        .fileSize{\n" +
                "            font-size: 0.75rem;\n" +
                "            line-height: 1.375rem;\n" +
                "            color: var(--colDate);\n" +
                "        }\n" +
                "        .fileDownload {\n" +
                "            display: table-cell;\n" +
                "            padding-left: 30px;\n" +
                "            vertical-align: middle;\n" +
                "        }\n" +
                "        .hovertext {\n" +
                "            position: relative;\n" +
                "            border-bottom: 1px dotted black;\n" +
                "        }\n" +
                "\n" +
                "        .hovertext:before {\n" +
                "            content: attr(data-hover);\n" +
                "            visibility: hidden;\n" +
                "            opacity: 0;\n" +
                "            width: 140px;\n" +
                "            background-color: black;\n" +
                "            color: #fff;\n" +
                "            text-align: center;\n" +
                "            border-radius: 5px;\n" +
                "            padding: 5px 0;\n" +
                "            transition: opacity 0.5s ease-in-out;\n" +
                "\n" +
                "            position: absolute;\n" +
                "            z-index: 1;\n" +
                "            left: 0;\n" +
                "            top: 110%;\n" +
                "        }\n" +
                "\n" +
                "        .hovertext:hover:before {\n" +
                "            opacity: 1;\n" +
                "            visibility: visible;\n" +
                "        }\n" +
                "        .embedFooter {" +
                "            margin-top: 10px;\n" +
                "            margin-left: -4px;\n" +
                "         }" +
                "\n" +
                "        .dayLine{\n" +
                "            margin-top: 10px;\n" +
                "            color: var(--colDayLine);\n" +
                "            font-size: 0.75rem;\n" +
                "            line-height: 1.375rem;\n" +
                "            display: inline-block;\n" +
                "            width: 100%;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .hrDayLine {\n" +
                "            width: 45%; \n" +
                "            display: inline-block;\n" +
                "            border-color: var(--colHrLine);\n" +
                "            \n" +
                "        }\n" +
                "        .dayLineDate {\n" +
                "            padding: 5px;\n" +
                "        }\n" +
                "        .messageImage img {\n" +
                "            max-width: 400px;\n" +
                "            max-height: 300px;\n" +
                "            border-radius: 5px;\n" +
                "        }\n" +
                "        .channelHeader {\n" +
                "            background-color: var(--colBackground);\n" +
                "            border-bottom: 1px solid;\n" +
                "            border-color: var(--colChannelHeaderBorder);\n" +
                "            position: fixed;\n" +
                "            left: 0;\n" +
                "            top: 0;\n" +
                "            width: 100%;\n" +
                "            z-index: 99;\n" +
                "            \n" +
                "        }\n" +
                "        .channelName {\n" +
                "            padding: 10px;\n" +
                "            color: var(--colNormalText);\n" +
                "            font-weight: bold;\n" +
                "            display: inline-block;\n" +
                "        }\n" +
                "        .pane {\n" +
                "            padding-top: 35px;\n" +
                "        }\n" +
                "        .embedColorBar {\n" +
                "            width: 5px;\n" +
                "            border-radius: 5px 0 0 5px;\n" +
                "        }\n" +
                "        .messageEmbed {\n" +
                "            display: flex;\n" +
                "            align-items: stretch;\n" +
                "        }\n" +
                "        .embedContent {\n" +
                "            width: 400px;\n" +
                "            background-color: var(--colEmbedBackground);\n" +
                "            border-radius: 0 5px 5px 0;    \n" +
                "            padding: 10px;\n" +
                "            padding-right: 0;\n" +
                "        }\n" +
                "        .embedThumbnail img {\n" +
                "            max-width: 100px;\n" +
                "            max-height: 50px;\n" +
                "            border-radius: 5px;\n" +
                "            float: right;\n" +
                "            margin-right: 10px;\n" +
                "        }\n" +
                "        .embedAuthor {\n" +
                "            color: var(--colNormalText);\n" +
                "            font-size: smaller;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .embedTitle {\n" +
                "            color: var(--colNormalText);\n" +
                "            padding-top: 10px;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .embedDescription {\n" +
                "            color: var(--colNormalText);\n" +
                "            padding-top: 5px;\n" +
                "            padding-bottom: 5px;\n" +
                "            font-size: smaller;\n" +
                "            white-space: pre-line;;\n" +
                "        }\n" +
                "        .embedFields {\n" +
                "            padding-top: 5px;\n" +
                "            display: flex;\n" +
                "            align-items: stretch;\n" +
                "            flex-wrap: wrap;\n" +
                "        }\n" +
                "        .fieldTitle {\n" +
                "            color: var(--colNormalText);\n" +
                "            padding-top: 10px;\n" +
                "            font-weight: bold;\n" +
                "            font-size: 14px;\n" +
                "            white-space: pre-wrap;\n" +
                "        }\n" +
                "        .fieldContent {\n" +
                "            color: var(--colNormalText);\n" +
                "            padding-top: 3px;\n" +
                "            font-size: smaller;\n" +
                "            white-space: pre-wrap;\n" +
                "        }\n" +
                "        .noinline {\n" +
                "            width: 100%;\n" +
                "        }\n" +
                "        .inline {\n" +
                "            width: 33.3%;\n" +
                "        }\n" +
                "        .embedImg img {\n" +
                "            padding-top: 10px;\n" +
                "            border-radius: 5px;\n" +
                "            width: 390px;\n" +
                "            height: 100%;\n" +
                "        }\n" +
                "\n" +
                "        .messageReactions {\n" +
                "            display: flex;\n" +
                "        }\n" +
                "        .messageReaction {\n" +
                "            margin-right: 5px;\n" +
                "            padding: 2px;\n" +
                "            border-radius: 5px;\n" +
                "            width: 40px;\n" +
                "            background-color: var(--colEmbedBackground);\n" +
                "        }\n" +
                "        .badgeBot {\n" +
                "            background-color: var(--colBotBadge);\n" +
                "            color: var(--colNormalText);\n" +
                "            border-radius: 5px;\n" +
                "            font-weight: bold;\n" +
                "            font-size: 11px;\n" +
                "            padding: 4px;\n" +
                "        }\n" +
                "        .footerImg{\n" +
                "            display: inline-block;\n" +
                "        }\n" +
                "\n" +
                "        .footerImg img {\n" +
                "            width: 20px;\n" +
                "            height: 20px;\n" +
                "            border-radius: 50%;\n" +
                "            margin-top: 10px;\n" +
                "        }\n" +
                "\n" +
                "        .footerText{\n" +
                "            display: inline-block;\n" +
                "            color: var(--colNormalText);\n" +
                "            font-size: 12px;\n" +
                "            vertical-align: super;\n" +
                "            white-space: pre-wrap;\n" +
                "        }\n" +
                "        .footerTime {\n" +
                "            display: inline-block;\n" +
                "            color: var(--colNormalText);\n" +
                "            font-size: 12px;\n" +
                "            float: right;\n" +
                "            margin-top: 10px;\n" +
                "            margin-right: 10px;\n" +
                "        }\n" +
                "        .sendBox{\n" +
                "            padding: 10px;\n" +
                "            background-color: var(--colEmbedBackground);\n" +
                "            border-radius: 20px;\n" +
                "            color: var(--colNormalText);\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .sendBoxText a{\n" +
                "            text-decoration:underline; \n" +
                "            color: var(--colNormalText);\n" +
                "        }" +
                "        .styleButton{\n" +
                "           display: inline-block;\n" +
                "           color: var(--colNormalText);\n" +
                "           float: right;\n" +
                "           margin-right: 20px;\n" +
                "           border: white 1px solid;\n" +
                "           background-color: var(--colBackground);\n" +
                "           margin-top: 4px;\n" +
                "           border-radius: 30px;\n" +
                "           }\n" +
                "           \n" +
                "       .styleButtonText {\n" +
                "           margin: 0;\n" +
                "           padding: 5px;\n" +
                "       }\n" +
                "        \n" +
                "        .noDecoration {\n" +
                "            text-decoration: none;\n" +
                "            color: var(--colFileText);\n" +
                "        }\n" +
                "\n" +
                "        /* Css for Mobile device */\n" +
                "        @media only screen and (max-width: 828px) {\n" +
                "            .hrDayLine {\n" +
                "                width: 40%; \n" +
                "                display: inline-block;\n" +
                "                border-color: var(--colHrLine);\n" +
                "                \n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        /* Css for Mobile device */\n" +
                "        @media only screen and (max-width: 600px) {\n" +
                "            table {\n" +
                "                border-collapse: collapse;\n" +
                "                border-spacing: 0;\n" +
                "            }\n" +
                "            .dayLine{\n" +
                "                color: var(--colDayLine);\n" +
                "                font-size: 0.75rem;\n" +
                "                line-height: 1.375rem;\n" +
                "                display: inline-block;\n" +
                "                width: 100%;\n" +
                "                text-align: center;\n" +
                "            }\n" +
                "            .hrDayLine {\n" +
                "                width: 35%; \n" +
                "                display: inline-block;\n" +
                "                border-color: var(--colHrLine);\n" +
                "                \n" +
                "            }\n" +
                "            .messageImage img {\n" +
                "                max-width: 300px;\n" +
                "                max-height: 180px;\n" +
                "                border-radius: 5px;\n" +
                "            }\n" +
                "            .embedContent {\n" +
                "                max-width: 300px;\n" +
                "                background-color: var(--colEmbedBackground);\n" +
                "                border-radius: 0 5px 5px 0;    \n" +
                "                padding-left: 10px;\n" +
                "                padding-top: 10px;\n" +
                "            }\n" +
                "            .embedImg img {\n" +
                "                padding-top: 10px;\n" +
                "                border-radius: 5px;\n" +
                "                width: 290px;\n" +
                "                height: 100%;\n" +
                "            }\n" +
                "\n" +
                "        }\n" +
                "\n" +
                "    </style>\n" +
                "    <body>\n" +
                "        <div class=\"channelHeader\">\n" +
                "            <div class=\"channelName\"># " + channelName + "</div>\n" +
                "            <div class=\"styleButton\" id=\"latestStyle\">\n" +
                "               <p class=\"styleButtonText\">Use latest CSS Version</p>\n" +
                "            </div>" +
                "            <div class=\"styleButton\">\n" +
                "               <p class=\"styleButtonText\">Version: "+version+"</p>\n" +
                "            </div>" +
                "        </div>\n" +
                "        <div class=\"pane\">\n" +
                "            <div class=\"panecontainer\"> ");
    }

    private List<Message> get(MessageChannel channel, int amount) {
        ArrayList<Message> messages = new ArrayList<>();
        int i = amount + 1;
        for (Message message : channel.getIterableHistory().cache(false)) {
            if (!message.isPinned()) {
                messages.add(message);
            }
            if (--i <= 0) break;
        }
        return messages;
    }

    private String parseDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) return "";
        LocalDateTime dateTime = offsetDateTime.toLocalDateTime();
        String newdate = "";
        newdate += String.format("%02d", dateTime.getDayOfMonth()) + "." + String.format("%02d", dateTime.getMonthValue()) + "." + dateTime.getYear() + " " + String.format("%02d", dateTime.getHour()) + ":" + String.format("%02d", dateTime.getMinute());
        return newdate;
    }

    private String parseDate(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) return "";
        LocalDateTime dateTime = offsetDateTime.toLocalDateTime();
        String newdate = "";
        newdate += String.format("%02d", dateTime.getDayOfMonth()) + "." + String.format("%02d", dateTime.getMonthValue()) + "." + dateTime.getYear();
        return newdate;
    }

    private String parseTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) return "";
        LocalDateTime dateTime = offsetDateTime.toLocalDateTime();
        String newdate = "";
        newdate += String.format("%02d", dateTime.getHour()) + ":" + String.format("%02d", dateTime.getMinute());
        return newdate;
    }

}
