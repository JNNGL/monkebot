package com.jnngl;

import com.jnngl.reader.FrameReader;
import com.jnngl.util.FutureUtil;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
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
    private static final Font FONT;

    static {
        try {
            FONT = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream("font.ttf"));
        } catch (FontFormatException | IOException e) {
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

    public BufferedImage renderText(int width, int height, String text) {
        BufferedImage textImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        if (text.isEmpty()) {
            return textImage;
        }

        List<String> lines = Arrays.asList(text.split("\n"));
        Collections.reverse(lines);

        Graphics2D textGraphics = textImage.createGraphics();
        textGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        textGraphics.setFont(FONT);
        textGraphics.setColor(Color.WHITE);
        textGraphics.getFontMetrics().getHeight();

        Rectangle2D bounds = maxLineBounds(lines, textGraphics);

        Font font = FONT.deriveFont(FONT.getSize2D() * textImage.getWidth() / (float) bounds.getWidth());
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

    public String processMessage(Message message) throws Exception {
        String[] args = message.getContentRaw().split(" ", 2);
        args = mapMessageArguments(message, args);
        if (args == null) {
            throw new Exception("Ссылка не указана или указана неверно.");
        }

        String url = args[0];
        String text = args[1];

        System.out.println("Generating GIF:");
        System.out.println("> Issued by " + message.getAuthor().getName());
        System.out.println("> URL: " + url);
        System.out.println("> Text: " + text);

        String filename = System.currentTimeMillis() + ".gif";
        try (FileOutputStream outputStream = new FileOutputStream("data/monke/" + filename);
             FrameReader reader = FrameReader.createFrameReader(new URL(url))) {
            BufferedImage textImage = renderText(reader.getWidth(), reader.getHeight(), text);

            AnimatedGifEncoder encoder = new AnimatedGifEncoder();
            encoder.start(outputStream);
            encoder.setRepeat(0);
            encoder.setSize(textImage.getWidth(), textImage.getHeight());
            encoder.setFrameRate(reader.getFrameRate());

            BufferedImage frame;
            while ((frame = reader.readFrame()) != null) {
                Graphics2D graphics = frame.createGraphics();
                graphics.drawImage(textImage, 0, 0, null);
                graphics.dispose();
                encoder.addFrame(frame);
            }

            encoder.finish();
        }

        return "https://api.jnngl.me/monke/" + filename;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        String content = event.getMessage().getContentRaw();
        if (!content.startsWith("!monke ")) {
            if (content.startsWith("!monke")) {
                event.getMessage().reply("Использование: !monke [ссылка] <текст>\n" +
                        "Также можно ответить на сообщение с гифкой, в таком случае команда " +
                        "выглядит так: !monke <текст>").queue();
            }

            return;
        }

        CompletableFuture.supplyAsync(FutureUtil.withCompletionException(() -> processMessage(event.getMessage())))
                .orTimeout(1, TimeUnit.MINUTES)
                .whenComplete((url, exception) -> event.getMessage().reply(exception != null
                        ? "Не получилось добавить текст на говногифку: \n> " + exception.getMessage()
                        : url).queue());
    }
}
