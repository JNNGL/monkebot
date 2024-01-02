/*
 * All Rights Reserved
 *
 * Copyright (c) 2023 JNNGL
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.jnngl;

import com.jnngl.reader.FrameReader;
import com.jnngl.translator.MonkeTranslator;
import com.jnngl.translator.MonkeTranslatorV1;
import com.jnngl.translator.MonkeTranslatorV1_1;
import com.jnngl.translator.MonkeTranslatorV1_2;
import com.jnngl.util.FutureUtil;
import com.jnngl.util.Swapped;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MonkeBot extends ListenerAdapter {
    private static final Map<String, Pattern> KEYWORDS = Map.of(
            "//tenor.com/", Pattern.compile("meta itemProp=\"contentUrl\" content=\"(.+?)\""),
            "//imgur.com/", Pattern.compile("meta property=\"og:(?>video|image)\" data-react-helmet=\"true\" content=\"(.+?)(\\?fb)?\""),
            "//giphy.com/", Pattern.compile("meta property=\"og:video\" content=\"(.+?)\"")
    );

    private static final MonkeTranslator[] MONKE_TRANSLATORS = new MonkeTranslator[]{
            new MonkeTranslatorV1(),
            new MonkeTranslatorV1_1(),
            new MonkeTranslatorV1_2()
    };

    private static final int CURRENT_MONKELANG = MONKE_TRANSLATORS.length - 1;

    private static final ThreadLocal<Graphics2D> GRAPHICS_FONT_CONTEXT = ThreadLocal.withInitial(() ->
            new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY).createGraphics());

    private static final MethodHandle PROPERTIES_SETTER;

    private static final Font FONT;
    private static final Font FONT_FRAME;
    private static final long GUILD_ID;
    private static final long ROLE_ID;
    private static final long LIMITED_ROLE_ID;

    static {
        try {
            PROPERTIES_SETTER = MethodHandles.privateLookupIn(BufferedImage.class, MethodHandles.lookup())
                    .findSetter(BufferedImage.class, "properties", Hashtable.class);

            FONT = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream("font.ttf"));
            FONT_FRAME = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream("font-frame.ttf"));
            GUILD_ID = Long.parseLong(Files.readString(Path.of("guild.txt")).trim());
            List<Long> roles = Files.readAllLines(Path.of("role.txt"))
                    .stream().map(s -> Long.parseLong(s.trim()))
                    .toList();
            ROLE_ID = roles.size() > 0 ? roles.get(0) : 0;
            LIMITED_ROLE_ID = roles.size() > 1 ? roles.get(1) : 0;
        } catch (FontFormatException | IOException | ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public MonkeBot() throws IOException {
        JDABuilder.createDefault(Files.readString(Path.of("token.txt")).trim())
                .setActivity(Activity.of(Activity.ActivityType.WATCHING, "обезьянок"))
                .setStatus(OnlineStatus.IDLE)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(this)
                .build();
    }

    public static void main(String[] args) throws IOException {
        new MonkeBot();
    }

    public Optional<String> getAttachmentUrl(Message message) {
        for (Message.Attachment attachment : message.getAttachments()) {
            return Optional.of(attachment.getUrl());
        }

        return Optional.empty();
    }

    public String[] mapMessageArguments(Message message, String[] args) throws Exception {
        String text = args[1];
        String url;

        if (args[1].startsWith("https://") || args[1].startsWith("http://")) {
            args = args[1].split(" ", 2);
            url = args[0];
            text = args.length == 1 ? "" : args[1];
        } else if (message.getReferencedMessage() != null) {
            Message referenced = message.getReferencedMessage();
            url = getAttachmentUrl(referenced).orElseGet(referenced::getContentRaw);
        } else if (!message.getAttachments().isEmpty()) {
            url = getAttachmentUrl(message).orElse("");
        } else {
            List<Message> messages = message.getChannel().getHistory().retrievePast(6).complete();
            url = messages.stream()
                    .filter(m -> m.getAuthor().getIdLong() == message.getAuthor().getIdLong())
                    .dropWhile(m -> m.getAttachments().isEmpty() && !m.getContentRaw().startsWith("http"))
                    .findFirst().map(Message::getContentRaw).orElse("");
        }

        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw new Exception("Ссылка не указана или указана неверно.");
        }

        for (Map.Entry<String, Pattern> keyword : KEYWORDS.entrySet()) {
            if (url.contains(keyword.getKey())) {
                try {
                    String html = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
                    Matcher matcher = keyword.getValue().matcher(html);
                    if (!matcher.find()) {
                        continue;
                    }

                    url = matcher.group(1);
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        return new String[]{url, text};
    }

    public Rectangle2D maxLineBounds(List<String> lines, Graphics2D textGraphics) {
        Rectangle2D bounds = null;
        for (String line : lines) {
            Rectangle2D currentBounds = textGraphics.getFontMetrics().getStringBounds(line, textGraphics);
            if (bounds == null) {
                bounds = currentBounds;
                continue;
            }

            if (currentBounds.getWidth() > bounds.getWidth()) {
                bounds = currentBounds;
            }
        }

        return Objects.requireNonNullElseGet(bounds, Rectangle2D.Double::new);
    }

    public Font deriveFont(Font font, Rectangle2D bounds, int width, int height) {
        double heightSize = font.getSize2D() * height / (float) bounds.getHeight() / 4;
        double widthSize = font.getSize2D() * width / (float) bounds.getWidth();
        return font.deriveFont((float) Math.min(widthSize, heightSize));
    }

    public BufferedImage renderText(int width, int height, String text) {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("FrameX", 0);
        properties.put("FrameY", 0);

        BufferedImage textImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        try {
            PROPERTIES_SETTER.invoke(textImage, properties);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        if (text.isEmpty()) {
            return textImage;
        }

        List<String> lines = Arrays.asList(text.split("\n"));
        Collections.reverse(lines);

        Graphics2D textGraphics = textImage.createGraphics();
        textGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        textGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        textGraphics.setFont(FONT);
        textGraphics.setColor(Color.WHITE);

        Rectangle2D bounds = maxLineBounds(lines, textGraphics);
        Font font = deriveFont(FONT, bounds, width - width / 10, height);
        textGraphics.setFont(font);
        bounds = textGraphics.getFontMetrics().getStringBounds(text, textGraphics);
        AffineTransform transform = textGraphics.getTransform();
        transform.translate(0, textImage.getHeight() + (int) (bounds.getHeight() / 2.0D));
        for (String line : lines) {
            bounds = textGraphics.getFontMetrics().getStringBounds(line, textGraphics);
            int horizontalCenter = textImage.getWidth() / 2 - (int) bounds.getWidth() / 2;
            transform.translate(horizontalCenter, -bounds.getHeight());
            textGraphics.setTransform(transform);
            textGraphics.setColor(Color.BLACK);
            FontRenderContext frc = textGraphics.getFontRenderContext();
            TextLayout tl = new TextLayout(line, font, frc);
            Shape shape = tl.getOutline(null);
            textGraphics.setStroke(new BasicStroke(2.0F));
            textGraphics.draw(shape);
            textGraphics.setColor(Color.WHITE);
            textGraphics.fill(shape);
            transform.translate(-horizontalCenter, 0);
        }

        textGraphics.dispose();

        return textImage;
    }

    public BufferedImage renderFrame(int width, int height, String text) {
        List<String> lines = Arrays.asList(text.split("\n"));
        Collections.reverse(lines);

        Graphics2D context = GRAPHICS_FONT_CONTEXT.get();
        context.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        context.setFont(FONT_FRAME);
        Rectangle2D bounds = maxLineBounds(lines, context);
        Font font = deriveFont(FONT_FRAME, bounds, width, height);
        double fontHeight = context.getFontMetrics(font).getStringBounds(text, context).getHeight();

        int frameWidth = (int) (width / 7.5);
        int upHeight = frameWidth / 2;
        int outlineWidth = frameWidth / 20;
        int outlineOffset = outlineWidth * 2;
        int downHeight = (int) (fontHeight + fontHeight * lines.size() + outlineOffset);

        int totalWidth = frameWidth * 2 + width;
        int totalHeight = upHeight + height + downHeight;

        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("FrameX", frameWidth);
        properties.put("FrameY", upHeight);
        properties.put("RenderBuffer", new Swapped<>(() ->
                new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB)));

        BufferedImage frameImage = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
        try {
            PROPERTIES_SETTER.invoke(frameImage, properties);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        Graphics2D graphics = frameImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setStroke(new BasicStroke(outlineWidth));
        graphics.setFont(font);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, totalWidth, totalHeight);
        graphics.setColor(Color.WHITE);
        graphics.drawRect(frameWidth - outlineOffset, upHeight - outlineOffset,
                width + outlineOffset * 2, height + outlineOffset * 2);

        FontMetrics metrics = graphics.getFontMetrics();
        double currentY = totalHeight - fontHeight * 0.5 - metrics.getDescent();
        for (String line : lines) {
            int lineWidth = (int) metrics.getStringBounds(line, graphics).getWidth();
            graphics.drawString(line, (totalWidth - lineWidth) / 2, (int) currentY);
            currentY -= fontHeight;
        }

        graphics.dispose();

        return frameImage;
    }

    public BufferedImage renderOverlay(int width, int height, String text, boolean isFramed) {
        if (isFramed) {
            return renderFrame(width, height, text);
        } else {
            return renderText(width, height, text);
        }
    }

    public byte[] processMessage(Message message, boolean isFramed) throws Exception {
        String[] args = message.getContentRaw().split(" ", 2);
        args = mapMessageArguments(message, args);
        if (args == null) {
            throw new Exception("Ссылка не указана или указана неверно.");
        }

        String url = args[0];
        String text = args[1];

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             FrameReader reader = FrameReader.createFrameReader(new URL(url))) {
            BufferedImage overlay = renderOverlay(reader.getWidth(), reader.getHeight(), text, isFramed);

            int frameX = (Integer) overlay.getProperty("FrameX");
            int frameY = (Integer) overlay.getProperty("FrameY");

            Object renderBufferValue = overlay.getProperty("RenderBuffer");
            @SuppressWarnings("unchecked")
            Swapped<BufferedImage> renderBuffer = renderBufferValue != BufferedImage.UndefinedProperty
                    ? (Swapped<BufferedImage>) renderBufferValue : null;

            AnimatedGifEncoder encoder = new AnimatedGifEncoder();
            encoder.start(outputStream);
            encoder.setRepeat(0);
            encoder.setSize(overlay.getWidth(), overlay.getHeight());
            encoder.setFrameRate(reader.getFrameRate());

            BufferedImage frame;
            while ((frame = reader.readFrame()) != null) {
                if (renderBuffer != null) {
                    BufferedImage buffer = renderBuffer.getNext();
                    Graphics2D graphics = buffer.createGraphics();
                    graphics.drawImage(overlay, 0, 0, null);
                    graphics.drawImage(frame, frameX, frameY, null);
                    graphics.dispose();
                    encoder.addFrame(buffer);
                } else {
                    Graphics2D graphics = frame.createGraphics();
                    graphics.drawImage(overlay, 0, 0, null);
                    graphics.dispose();
                    encoder.addFrame(frame);
                }
            }

            encoder.finish();

            return outputStream.toByteArray();
        }
    }

    public static String[] monkeTranslate(String text) {
        for (;;) {
            if (!text.contains(" ")) {
                break;
            }

            String[] parts = text.split(" ", 2);
            parts[0] = parts[0]
                    .replace('у', '0')
                    .replace('У', '1')
                    .replace('а', '2')
                    .replace('А', '3');

            if (!parts[0].matches("^[0-3]+$")) {
                break;
            }

            int version = Integer.parseUnsignedInt(parts[0], 4);
            if (version >= MONKE_TRANSLATORS.length || MONKE_TRANSLATORS[version] == null) {
                break;
            }

            String result = MONKE_TRANSLATORS[version].translateFromMonke(parts[1].replace("\n", ""));
            if (result == null) {
                break;
            } else {
                return new String[]{"с обезьяньего языка (" + MONKE_TRANSLATORS[version].getName() + ")", result};
            }
        }

        return new String[]{
                "на обезьяний язык (" + MONKE_TRANSLATORS[CURRENT_MONKELANG].getName() + ")",
                Integer.toUnsignedString(CURRENT_MONKELANG, 4)
                    .replace('0', 'у')
                    .replace('1', 'У')
                    .replace('2', 'а')
                    .replace('3', 'А') +
                    ' ' +
                    MONKE_TRANSLATORS[CURRENT_MONKELANG].translateToMonke(text)
        };
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        String content = event.getMessage().getContentRaw();
        if (!content.startsWith("!monke")) {
            return;
        }

        if (content.startsWith("!monke") && !content.contains(" ")) {
            event.getMessage().replyEmbeds(new EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("Доступные команды:")
                    .addField(
                            "MonkeTranslate - обезьяний переводчик",
                            "Использование: !monketranslate <текст>",
                            false)
                    .addField(
                            "MonkeFrame - добавление текста на гифку в виде демотиватора",
                            "Использование: !monkeframe [ссылка] <текст>\n" +
                                    "Также можно ответить на сообещение с гифкой, " +
                                    "в таком случае команда выгядит так: !monkeframe <текст>",
                            false)
                    .addField(
                            "Monke - добавление текста на гифку> Использование: !monke [ссылка] <текст>",
                            "Использование: !monke [ссылка] <текст>\n" +
                                    "Также можно ответить на сообещение с гифкой, " +
                                    "в таком случае команда выгядит так: !monke <текст>",
                            false)
                    .setFooter("JNNGL © 2023 | All rights reserved.")
                    .build()).queue();

            return;
        }

        Guild guild = event.getJDA().getGuildById(GUILD_ID);
        Role role = guild != null ? guild.getRoleById(ROLE_ID) : null;
        Role limitedRole = guild != null ? guild.getRoleById(LIMITED_ROLE_ID) : null;
        Member member = role != null ? guild.retrieveMemberById(event.getAuthor().getIdLong()).complete() : null;

        if (member != null && member.getRoles().contains(limitedRole)
                && (!event.isFromGuild() || event.getGuild().getIdLong() != GUILD_ID)) {
            event.getMessage().reply("Доступ к боту в ЛС и на чужих серверах запрещен.").queue();
            return;
        }

        if (member != null && member.getRoles().contains(role)) {
            event.getMessage().reply("Доступ к боту обезьянам запрещен.").queue();
            return;
        }

        if (content.startsWith("!monketranslate ")) {
            String[] translated = monkeTranslate(content.substring(16).trim());
            if (translated[1].length() < 1900 - (translated[1].split("\n", -1).length - 1) * 2) {
                event.getMessage().reply("Переведено " + translated[0] + ":\n> " +
                        translated[1].replace("\n", "\n> ")).queue();
            } else {
                byte[] bytes = translated[1].getBytes(StandardCharsets.UTF_8);
                event.getMessage().reply(new MessageCreateBuilder()
                        .addContent("Переведено " + translated[0] + ":")
                        .addFiles(FileUpload.fromData(bytes, "translated.txt"))
                        .build()).queue();
            }
        } else {
            CompletableFuture.supplyAsync(FutureUtil.withCompletionException(() -> processMessage(event.getMessage(), content.startsWith("!monkeframe "))))
                    .orTimeout(3, TimeUnit.MINUTES)
                    .whenComplete((data, exception) -> {
                        if (exception != null) {
                            event.getMessage().reply("Не получилось добавить текст на говногифку: \n> " + exception.getMessage()).queue();
                        } else {
                            try {
                                event.getMessage().replyFiles(FileUpload.fromData(data, "monke.gif")).queue();
                            } catch (Throwable e) {
                                if (e.getMessage().contains("Request entity too large")) {
                                    event.getMessage().reply("ЭТА ГИФКА СЛИШКОМ БОЛЬШАЯ У ДС ЛИМИТ 25МБ У УА уа УА  УА У А У").queue();
                                }
                            }
                        }
                    });
        }
    }
}
